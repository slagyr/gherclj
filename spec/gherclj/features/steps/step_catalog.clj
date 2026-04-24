(ns gherclj.features.steps.step-catalog
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defthen]]))

(def ansi-code-pattern #"\u001b\[[0-9;]*m")

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

(defthen catalog-output-should-include "the catalog output should include:"
  "Checks that expected lines appear adjacent and in order in :cli-output. Leading/trailing blank lines in expected are trimmed."
  [doc-string]
  (g/should (adjacent-lines-present? (g/get :cli-output "") doc-string)))

(defthen output-should-have-no-color-codes "the output should have no color codes"
  []
  (g/should-not (re-find ansi-code-pattern (g/get :cli-output ""))))

(defthen output-should-have-color-codes "the output should have color codes"
  []
  (g/should (re-find ansi-code-pattern (g/get :cli-output ""))))

(defthen output-should-contain-lines "the output should contain lines:"
  "Each table row is a substring check against :cli-output. All rows must be present; order is not checked."
  [table]
  (let [output (g/get :cli-output "")
        lines (concat (:headers table) (mapcat identity (:rows table)))]
    (doseq [line lines]
      (g/should (str/includes? output line)))))
