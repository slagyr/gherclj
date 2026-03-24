(ns code-generation-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.code-generation]))

(describe "Code generation"

  (context "Generate a spec from a simple feature"
    (it "Generate a spec from a simple feature"
      (h/reset!)
      (gherclj.features.steps.code-generation/setup-feature "Authentication" "auth.feature")
      (gherclj.features.steps.code-generation/add-scenario "User can log in" {:headers ["type" "text"], :rows [["given" "timeout is 300"] ["when" "running the action"] ["then" "the status should be active"]]})
      (gherclj.features.steps.code-generation/generate-spec ":speclj")
      (gherclj.features.steps.code-generation/output-should-contain "Authentication")
      (gherclj.features.steps.code-generation/output-should-contain "User can log in")
      (gherclj.features.steps.code-generation/output-should-contain "add-timeout 300")
      (gherclj.features.steps.code-generation/output-should-contain "run-action")))

  (context "Unrecognized steps generate pending scenarios"
    (it "Unrecognized steps generate pending scenarios"
      (h/reset!)
      (gherclj.features.steps.code-generation/setup-feature "Pending" "pending.feature")
      (gherclj.features.steps.code-generation/add-scenario "Not yet done" {:headers ["type" "text"], :rows [["given" "something not yet defined"] ["when" "doing the undefined thing"]]})
      (gherclj.features.steps.code-generation/generate-spec ":speclj")
      (gherclj.features.steps.code-generation/output-should-contain "pending"))))
