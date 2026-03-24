(ns gherclj.features.steps.gherkin-parsing
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]))

(defgiven feature-file-containing "a feature file containing:"
  [doc-string]
  (h/set-raw-feature! doc-string))

(defwhen parse-feature "the feature is parsed"
  []
  (h/parse-raw-feature!))

(defthen ir-should-be "the IR should be:"
  [doc-string]
  (h/parsed-ir))
