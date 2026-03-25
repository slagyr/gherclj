(ns gherclj.features.steps.gherkin-parsing
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.parser :as parser]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defgiven feature-file-containing "a feature file containing:"
  [doc-string]
  (g/assoc! :raw-feature doc-string))

(defwhen parse-feature "the feature is parsed"
  []
  (try
    (g/assoc! :parsed-ir (parser/parse-feature (g/get :raw-feature)))
    (catch Exception e
      (g/assoc! :error (.getMessage e)))))

(defthen ir-should-be "the IR should be:"
  [doc-string]
  (g/should= (edn/read-string doc-string) (g/get :parsed-ir)))

(defthen parsing-should-fail "parsing should fail with message {text:string}"
  [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))
