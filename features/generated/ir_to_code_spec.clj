(ns ir-to-code-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.code-generation :as code-generation]))

(describe "IR to code generation"

  (context "Generate a speclj spec"
    (it "Generate a speclj spec"
      ;; given a feature named "Login" from source "login.feature"
      ;; given a scenario "Valid credentials" with steps:
      ;; when generating the spec with framework :speclj
      ;; then the generated code should be:
      (pending "not yet implemented")))

  (context "Generate a clojure.test spec"
    (it "Generate a clojure.test spec"
      ;; given a feature named "Login" from source "login.feature"
      ;; given a scenario "Valid credentials" with steps:
      ;; when generating the spec with framework :clojure.test
      ;; then the generated code should be:
      (pending "not yet implemented")))

  (context "Background steps are included in every scenario"
    (it "Background steps are included in every scenario"
      ;; given a feature named "Login" from source "login.feature"
      ;; and a background with steps:
      ;; given a scenario "First" with steps:
      ;; given a scenario "Second" with steps:
      ;; when generating the spec with framework :speclj
      ;; then the output should contain "(sample-app/create-user \"alice\")"
      ;; then the output should contain "(sample-app/user-logs-in)"
      ;; then the output should contain "(sample-app/response-should-be 200)"
      (pending "not yet implemented")))

  (context "Harness reset is called before each scenario"
    (it "Harness reset is called before each scenario"
      (h/reset!)
      (code-generation/setup-feature "Login" "login.feature")
      (code-generation/add-scenario "First" {:headers ["type" "text"], :rows [["when" "the user logs in"]]})
      (code-generation/add-scenario "Second" {:headers ["type" "text"], :rows [["when" "the user logs in"]]})
      (code-generation/generate-spec ":speclj")
      (code-generation/output-should-contain "(h/reset!)")))

  (context "Unrecognized steps generate pending scenarios"
    (it "Unrecognized steps generate pending scenarios"
      (h/reset!)
      (code-generation/setup-feature "Login" "login.feature")
      (code-generation/add-scenario "Not implemented" {:headers ["type" "text"], :rows [["given" "something undefined"] ["when" "doing unknown things"]]})
      (code-generation/generate-spec ":speclj")
      (code-generation/output-should-contain "pending")
      (code-generation/output-should-contain ";; given something undefined")))

  (context "WIP scenarios are excluded from generation"
    (it "WIP scenarios are excluded from generation"
      ;; given a feature named "Login" from source "login.feature"
      ;; given a scenario "Normal" with steps:
      ;; and a wip scenario "Skipped" with steps:
      ;; when generating the spec with framework :speclj
      ;; then the output should contain "Normal"
      ;; and the output should not contain "Skipped"
      (pending "not yet implemented"))))
