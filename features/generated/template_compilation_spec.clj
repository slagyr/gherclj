(ns template-compilation-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.template-compilation]))

(describe "Template compilation"

  (context "Compile a plain template with no captures"
    (it "Compile a plain template with no captures"
      (h/reset!)
      (gherclj.features.steps.template-compilation/set-template "checking for zombies")
      (gherclj.features.steps.template-compilation/compile-template)
      (gherclj.features.steps.template-compilation/regex-should-be "^checking for zombies$")
      (gherclj.features.steps.template-compilation/binding-count 0)))

  (context "Compile an integer capture"
    (it "Compile an integer capture"
      (h/reset!)
      (gherclj.features.steps.template-compilation/set-template "timeout is {seconds:int}")
      (gherclj.features.steps.template-compilation/compile-template)
      (gherclj.features.steps.template-compilation/regex-should-match "timeout is 300")
      (gherclj.features.steps.template-compilation/captured-value-int 300)))

  (context "Compile a word capture"
    (it "Compile a word capture"
      (h/reset!)
      (gherclj.features.steps.template-compilation/set-template "status is {status}")
      (gherclj.features.steps.template-compilation/compile-template)
      (gherclj.features.steps.template-compilation/regex-should-match "status is active")
      (gherclj.features.steps.template-compilation/captured-value-string "active")))

  (context "Compile multiple captures"
    (it "Compile multiple captures"
      (h/reset!)
      (gherclj.features.steps.template-compilation/set-template "set {key} to {value}")
      (gherclj.features.steps.template-compilation/compile-template)
      (gherclj.features.steps.template-compilation/regex-should-match "set timeout to 300")
      (gherclj.features.steps.template-compilation/binding-count 2))))
