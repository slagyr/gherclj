(ns step-patterns-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.step-definitions]
            [gherclj.features.steps.step-patterns]))

(describe "Step patterns"

  (context "Quoted string capture"
    (it "Quoted string capture"
      ;; given a given step named "greet" with template "hello \"{name}\""
      ;; then the step "greet" should match "hello \"Alice\""
      ;; then the match args should be ["Alice"]
      (pending "not yet implemented")))

  (context "Float capture"
    (it "Float capture"
      (h/reset!)
      (gherclj.features.steps.step-definitions/define-then-step "check-price" "the price is {amount:float}")
      (gherclj.features.steps.step-definitions/step-should-match "check-price" "the price is 19.99")
      (gherclj.features.steps.step-definitions/match-args-should-be "[19.99]")))

  (context "Multiple captures of different types"
    (it "Multiple captures of different types"
      ;; given a given step named "set-config" with template "set \"{key}\" to {value:int}"
      ;; then the step "set-config" should match "set \"timeout\" to 300"
      ;; and the match args should be ["timeout" 300]
      (pending "not yet implemented")))

  (context "Template escapes regex special characters"
    (it "Template escapes regex special characters"
      (h/reset!)
      (gherclj.features.steps.step-definitions/define-given-step "parens" "call foo() with {n:int} args")
      (gherclj.features.steps.step-definitions/step-should-match "parens" "call foo() with 3 args")
      (gherclj.features.steps.step-definitions/match-args-should-be "[3]")))

  (context "Macro accepts a raw regex pattern"
    (it "Macro accepts a raw regex pattern"
      ;; given the registered step "raw-output-match"
      ;; then the step "raw-output-match" should match "the output contains hello world"
      ;; and the match args should be ["hello world"]
      (pending "not yet implemented")))

  (context "Regex captures are strings, not coerced"
    (it "Regex captures are strings, not coerced"
      (h/reset!)
      (gherclj.features.steps.step-patterns/lookup-registered-step "raw-digit-match")
      (gherclj.features.steps.step-definitions/step-should-match "raw-digit-match" "count is 42")
      (gherclj.features.steps.step-definitions/match-args-should-be "[\"42\"]"))))
