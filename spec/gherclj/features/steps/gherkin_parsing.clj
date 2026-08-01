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

(defn matches-expected?
  "True when expected equals actual, treating expected maps as a subset of
   actual (so additive IR fields like :line do not break older fixtures)."
  [expected actual]
  (cond
    (and (map? expected) (map? actual))
    (every? (fn [[k v]] (matches-expected? v (get actual k))) expected)

    (and (sequential? expected) (not (string? expected))
         (sequential? actual) (not (string? actual)))
    (and (= (count expected) (count actual))
         (every? true? (map matches-expected? expected actual)))

    :else
    (= expected actual)))

(defn ir-should-be [doc-string]
  (let [expected (edn/read-string doc-string)
        actual (g/get :parsed-ir)]
    (g/should (matches-expected? expected actual))))

(defn parsing-should-fail [text]
  (let [error (g/get :error)]
    (g/should-not-be-nil error)
    (g/should (str/includes? error text))))

(defgiven "a feature file containing:" gherkin-parsing/feature-file-containing!)

(defwhen "the feature is parsed" gherkin-parsing/parse-feature!)

(defthen "the IR should be:" gherkin-parsing/ir-should-be)

(defthen "parsing should fail with message {text:string}" gherkin-parsing/parsing-should-fail)
