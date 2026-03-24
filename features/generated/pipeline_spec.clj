(ns pipeline-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.code-generation :as code-generation]))

(describe "Pipeline"

  (context "Parse stage writes EDN IR files"
    (it "Parse stage writes EDN IR files"
      ;; given a features directory containing:
      ;; and the feature "login.feature" contains:
      (pending "not yet implemented")))

  (context "Valid credentials"
    (it "Valid credentials"
      ;; given a valid user
      ;; when the user logs in
      ;; then the user sees the dashboard
      ;; and the feature "checkout.feature" contains:
      (pending "not yet implemented")))

  (context "Empty cart"
    (it "Empty cart"
      ;; given an empty cart
      ;; when the user checks out
      ;; then an error is shown
      ;; when the parse stage runs
      ;; then "target/gherclj/edn/login.edn" should exist
      ;; and "target/gherclj/edn/checkout.edn" should exist
      ;; and "target/gherclj/edn/login.edn" should contain IR:
      (pending "not yet implemented")))

  (context "Pipeline is silent by default"
    (it "Pipeline is silent by default"
      ;; given a features directory containing:
      ;; and the feature "auth.feature" contains:
      (pending "not yet implemented")))

  (context "Login"
    (it "Login"
      ;; given a user
      ;; when the full pipeline runs with framework :speclj
      ;; then the output should be empty
      (pending "not yet implemented")))

  (context "Parse stage reports progress when verbose"
    (it "Parse stage reports progress when verbose"
      ;; given a features directory containing:
      ;; and the feature "auth.feature" contains:
      (pending "not yet implemented")))

  (context "Login"
    (it "Login"
      ;; given a user
      (pending "not yet implemented")))

  (context "Logout"
    (it "Logout"
      ;; given a session
      ;; when the parse stage runs with :verbose
      ;; then the output should contain "Parsing auth.feature -> target/gherclj/edn/auth.edn"
      ;; then the output should contain "2 scenarios parsed"
      (pending "not yet implemented")))

  (context "Generate stage writes spec files from EDN"
    (it "Generate stage writes spec files from EDN"
      ;; given a features directory containing:
      ;; and the feature "auth.feature" contains:
      (pending "not yet implemented")))

  (context "Login"
    (it "Login"
      ;; given a valid user
      ;; and the parse stage has run
      ;; when the generate stage runs with framework :speclj
      ;; then "target/gherclj/generated/auth_spec.clj" should exist
      ;; and "target/gherclj/generated/auth_spec.clj" should contain "(describe \"Auth\""
      (pending "not yet implemented")))

  (context "Generate stage reports progress when verbose"
    (it "Generate stage reports progress when verbose"
      ;; given a features directory containing:
      ;; and the feature "auth.feature" contains:
      (pending "not yet implemented")))

  (context "Login"
    (it "Login"
      ;; given a valid user
      ;; and the parse stage has run
      ;; when the generate stage runs with framework :speclj and :verbose
      ;; then the output should contain "Generating target/gherclj/generated/auth_spec.clj from auth.edn"
      ;; then the output should contain "1 scenarios generated"
      (pending "not yet implemented")))

  (context "Full pipeline runs both stages"
    (it "Full pipeline runs both stages"
      ;; given a features directory containing:
      ;; and the feature "auth.feature" contains:
      (pending "not yet implemented")))

  (context "Login"
    (it "Login"
      ;; given a valid user
      ;; when the full pipeline runs with framework :speclj
      ;; then "target/gherclj/edn/auth.edn" should exist
      ;; and "target/gherclj/generated/auth_spec.clj" should exist
      (pending "not yet implemented")))

  (context "WIP scenarios are parsed but not generated"
    (it "WIP scenarios are parsed but not generated"
      ;; given a features directory containing:
      ;; and the feature "auth.feature" contains:
      (pending "not yet implemented")))

  (context "Ready"
    (it "Ready"
      ;; given a valid user
      ;; when the full pipeline runs with framework :speclj
      ;; then "target/gherclj/edn/auth.edn" should contain IR with 2 scenarios
      ;; and "target/gherclj/generated/auth_spec.clj" should contain "Ready"
      ;; and "target/gherclj/generated/auth_spec.clj" should not contain "Not ready"
      (pending "not yet implemented"))))
