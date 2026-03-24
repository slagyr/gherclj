(ns gherkin-parsing-spec
  (:require [speclj.core :refer :all]
            [gherclj.features.harness :as h]))

(describe "Gherkin parsing"

  (context "Parse a minimal feature"
    (it "Parse a minimal feature"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "Successful login"
    (it "Successful login"
      ;; given a common precondition
      ;; and another precondition
      ;; given a valid user
      ;; when the user logs in
      ;; then the user sees the dashboard
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented")))

  (context "And and But preserve their keyword type"
    (it "And and But preserve their keyword type"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "Mixed"
    (it "Mixed"
      ;; given a common precondition
      ;; and another precondition
      ;; given first condition
      ;; and second condition
      ;; but not third
      ;; when an action
      ;; then a result
      ;; and another result
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented")))

  (context "Parse multiple scenarios"
    (it "Parse multiple scenarios"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "First"
    (it "First"
      ;; given a common precondition
      ;; and another precondition
      ;; given setup one
      (pending "not yet implemented")))

  (context "Second"
    (it "Second"
      ;; given a common precondition
      ;; and another precondition
      ;; given setup two
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented")))

  (context "Parse a Background section"
    (it "Parse a Background section"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "Uses background"
    (it "Uses background"
      ;; given a common precondition
      ;; and another precondition
      ;; when an action
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented")))

  (context "Parse a step with a data table"
    (it "Parse a step with a data table"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "With table"
    (it "With table"
      ;; given a common precondition
      ;; and another precondition
      ;; given users:
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented")))

  (context "WIP-tagged scenarios are marked"
    (it "WIP-tagged scenarios are marked"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "Ready"
    (it "Ready"
      ;; given a common precondition
      ;; and another precondition
      ;; given something else
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented")))

  (context "Feature description is captured"
    (it "Feature description is captured"
      ;; given a common precondition
      ;; and another precondition
      ;; given a feature file containing:
      (pending "not yet implemented")))

  (context "Only one"
    (it "Only one"
      ;; given a common precondition
      ;; and another precondition
      ;; given something
      ;; when the feature is parsed
      ;; then the IR should be:
      (pending "not yet implemented"))))
