(ns gherclj.features.steps.step-definitions
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]))

(defgiven define-given-step "a given step named \"{name}\" with template \"{template}\""
  [name template]
  (h/register-test-step! :given name template))

(defgiven define-when-step "a when step named \"{name}\" with template \"{template}\""
  [name template]
  (h/register-test-step! :when name template))

(defgiven define-then-step "a then step named \"{name}\" with template \"{template}\""
  [name template]
  (h/register-test-step! :then name template))

(defthen step-registered-as "the step \"{name}\" should be registered as a {type} step"
  [name type]
  (h/find-step name))

(defthen step-should-match "the step \"{name}\" should match \"{text}\""
  [name text]
  (h/classify-text! text))

(defthen match-args-should-be "the match args should be {args}"
  [args]
  (h/classify-result))

(defwhen classify-text "classifying \"{text}\""
  [text]
  (h/classify-text! text))

(defthen no-step-should-match "no step should match"
  []
  (h/classify-result))
