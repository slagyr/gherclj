(ns template-compilation-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.template-compilation :as template-compilation]))

(describe "Template compilation"

  (context "Compile a plain template with no captures"
    (it "Compile a plain template with no captures"
      (h/reset!)
      (template-compilation/set-template "checking for zombies")
      (template-compilation/compile-template)
      (template-compilation/regex-should-be "^checking for zombies$")
      (template-compilation/binding-count 0)))

  (context "Compile an integer capture"
    (it "Compile an integer capture"
      (h/reset!)
      (template-compilation/set-template "timeout is {seconds:int}")
      (template-compilation/compile-template)
      (template-compilation/regex-should-match "timeout is 300")
      (template-compilation/captured-value-int 300)))

  (context "Compile a word capture"
    (it "Compile a word capture"
      (h/reset!)
      (template-compilation/set-template "status is {status}")
      (template-compilation/compile-template)
      (template-compilation/regex-should-match "status is active")
      (template-compilation/captured-value-string "active")))

  (context "Compile multiple captures"
    (it "Compile multiple captures"
      (h/reset!)
      (template-compilation/set-template "set {key} to {value}")
      (template-compilation/compile-template)
      (template-compilation/regex-should-match "set timeout to 300")
      (template-compilation/binding-count 2))))
