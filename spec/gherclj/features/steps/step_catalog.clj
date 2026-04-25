(ns gherclj.features.steps.step-catalog
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defthen helper!]]))

(helper! gherclj.features.steps.step-catalog)

(def ansi-code-pattern #"\[[0-9;]*m")

(defn- trim-blank-lines [lines]
  (->> lines
       (drop-while str/blank?)
       reverse
       (drop-while str/blank?)
       reverse
       vec))

(defn- adjacent-lines-present? [output expected]
  (let [actual-lines (str/split-lines output)
        expected-lines (-> expected str/split-lines trim-blank-lines)
        window-size (count expected-lines)]
    (boolean
     (some (fn [window]
             (every? true?
                     (map str/includes? window expected-lines)))
           (partition window-size 1 actual-lines)))))

(defn catalog-output-should-include [doc-string]
  (g/should (adjacent-lines-present? (g/get :cli-output "") doc-string)))

(defn output-should-have-no-color-codes []
  (g/should-not (re-find ansi-code-pattern (g/get :cli-output ""))))

(defn output-should-have-color-codes []
  (g/should (re-find ansi-code-pattern (g/get :cli-output ""))))

(defn output-should-contain-lines [table]
  (let [output (g/get :cli-output "")
        lines (concat (:headers table) (mapcat identity (:rows table)))]
    (doseq [line lines]
      (g/should (str/includes? output line)))))

(defthen "the catalog output should include:" step-catalog/catalog-output-should-include
  "Checks that expected lines appear adjacent and in order in :cli-output. Leading/trailing blank lines in expected are trimmed.")

(defthen "the output should have no color codes" step-catalog/output-should-have-no-color-codes)

(defthen "the output should have color codes" step-catalog/output-should-have-color-codes)

(defthen "the output should contain lines:" step-catalog/output-should-contain-lines
  "Each table row is a substring check against :cli-output. All rows must be present; order is not checked.")
