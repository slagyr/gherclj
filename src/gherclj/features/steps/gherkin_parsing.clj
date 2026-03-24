(ns gherclj.features.steps.gherkin-parsing
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.parser :as parser]
            [clojure.edn :as edn]
            [speclj.core :refer [should=]]))

(defgiven feature-file-containing "a feature file containing:"
  [doc-string]
  (g/assoc! :raw-feature doc-string))

(defwhen parse-feature "the feature is parsed"
  []
  (g/assoc! :parsed-ir (parser/parse-feature (g/get :raw-feature))))

(defthen ir-should-be "the IR should be:"
  [doc-string]
  (should= (edn/read-string doc-string) (g/get :parsed-ir)))
