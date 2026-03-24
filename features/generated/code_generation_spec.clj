(ns code-generation-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.code-generation :as code-generation]))

(describe "Code generation"

  (context "Generate a spec from a simple feature"
    (it "Generate a spec from a simple feature"
      (h/reset!)
      (code-generation/setup-feature "Authentication" "auth.feature")
      (code-generation/add-scenario "User can log in" {:headers ["type" "text"], :rows [["given" "timeout is 300"] ["when" "running the action"] ["then" "the status should be active"]]})
      (code-generation/generate-spec ":speclj")
      (code-generation/output-should-contain "Authentication")
      (code-generation/output-should-contain "User can log in")
      (code-generation/output-should-contain "add-timeout 300")
      (code-generation/output-should-contain "run-action")))

  (context "Unrecognized steps generate pending scenarios"
    (it "Unrecognized steps generate pending scenarios"
      (h/reset!)
      (code-generation/setup-feature "Pending" "pending.feature")
      (code-generation/add-scenario "Not yet done" {:headers ["type" "text"], :rows [["given" "something not yet defined"] ["when" "doing the undefined thing"]]})
      (code-generation/generate-spec ":speclj")
      (code-generation/output-should-contain "pending"))))
