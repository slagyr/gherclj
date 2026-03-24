(ns step-definitions-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.step-definitions]))

(describe "Step definitions"

  (context "Define a given step with integer capture"
    (it "Define a given step with integer capture"
      (h/reset!)
      (gherclj.features.steps.step-definitions/define-given-step "add-timeout" "timeout is {seconds:int}")
      (gherclj.features.steps.step-definitions/step-registered-as "add-timeout" ":given")
      (gherclj.features.steps.step-definitions/step-should-match "add-timeout" "timeout is 300")
      (gherclj.features.steps.step-definitions/match-args-should-be "[300]")))

  (context "Define a when step with no captures"
    (it "Define a when step with no captures"
      (h/reset!)
      (gherclj.features.steps.step-definitions/define-when-step "run-action" "running the action")
      (gherclj.features.steps.step-definitions/step-registered-as "run-action" ":when")
      (gherclj.features.steps.step-definitions/step-should-match "run-action" "running the action")
      (gherclj.features.steps.step-definitions/match-args-should-be "[]")))

  (context "Define a then step with word capture"
    (it "Define a then step with word capture"
      (h/reset!)
      (gherclj.features.steps.step-definitions/define-then-step "check-status" "the status should be {status}")
      (gherclj.features.steps.step-definitions/step-registered-as "check-status" ":then")
      (gherclj.features.steps.step-definitions/step-should-match "check-status" "the status should be active")
      (gherclj.features.steps.step-definitions/match-args-should-be "[\"active\"]")))

  (context "Unrecognized step text returns no match"
    (it "Unrecognized step text returns no match"
      (h/reset!)
      (gherclj.features.steps.step-definitions/define-given-step "add-timeout" "timeout is {seconds:int}")
      (gherclj.features.steps.step-definitions/classify-text "something completely unrecognized")
      (gherclj.features.steps.step-definitions/no-step-should-match))))
