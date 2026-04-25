(ns gherclj.features.steps.gherkin-parsing
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.parser :as parser]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(helper! gherclj.features.steps.gherkin-parsing)

(defn feature-file-containing! [doc-string]
  (g/assoc! :raw-feature doc-string))

(defn parse-feature! []
  (try
    (g/assoc! :parsed-ir (parser/parse-feature (g/get :raw-feature)))
    (catch Exception e
      (g/assoc! :error (.getMessage e)))))

(defn ir-should-be [doc-string]
  (g/should= (edn/read-string doc-string) (g/get :parsed-ir)))

(defn parsing-should-fail [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))

(defgiven "a feature file containing:" gherkin-parsing/feature-file-containing!)

(defwhen "the feature is parsed" gherkin-parsing/parse-feature!)

(defthen "the IR should be:" gherkin-parsing/ir-should-be)

(defthen "parsing should fail with message {text:string}" gherkin-parsing/parsing-should-fail)
