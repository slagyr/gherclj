(ns gherclj.features.steps.template-compilation
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]))

(defgiven set-template "a template \"{template}\""
  [template]
  (h/set-template! template))

(defwhen compile-template "the template is compiled"
  []
  (h/compile-template!))

(defthen regex-should-be "the regex should be \"{expected}\""
  [expected]
  (h/regex-str))

(defthen regex-should-match "the regex should match \"{text}\""
  [text]
  (h/match-text! text))

(defthen binding-count "there should be {count:int} bindings"
  [count]
  (h/binding-count))

(defthen captured-value-string "the captured value should be \"{expected}\""
  [expected]
  (h/match-result))

(defthen captured-value-int "the captured value should be {expected:int}"
  [expected]
  (h/match-result))
