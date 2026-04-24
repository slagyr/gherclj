(ns gherclj.features.steps.step-patterns
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven defwhen defthen]]))

;; --- Regex-based test subjects ---

(defgiven raw-output-match #"^the output contains (.+)$"
  [text]
  text)

(defgiven raw-digit-match #"^count is (\d+)$"
  [digits]
  digits)

;; --- Steps ---

(defgiven lookup-registered-step "the registered step {name}"
  [name]
  (let [normalized-name (str/replace name #"^\"|\"$" "")
        steps (g/steps-in-ns 'gherclj.features.steps.step-patterns)
        step (first (filter #(= normalized-name (:name %)) steps))]
    (when step
      (g/update! :steps (fnil conj []) step))))
