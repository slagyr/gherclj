Feature: Pipeline configuration

  The pipeline accepts a configuration map with options for
  parsing, generation, and test execution. Options have sensible
  defaults and are validated before the pipeline runs.

  Scenario: Minimal config uses all defaults
    Given an empty config
    When the config is resolved
    Then the resolved config should be:
      """
      {:features-dir "features"
       :edn-dir "target/gherclj/edn"
       :output-dir "target/gherclj/generated"
       :step-namespaces []
       :framework :speclj
       :verbose false
       :framework-opts []
       :rspec-requires []
       :include-tags []
       :exclude-tags []}
      """

  Scenario: Explicit values override defaults
    Given a config:
      """
      {:features-dir "specs/features"
       :framework :clojure.test
       :verbose true}
      """
    When the config is resolved
    Then the resolved config should be:
      """
      {:features-dir "specs/features"
       :edn-dir "target/gherclj/edn"
       :output-dir "target/gherclj/generated"
       :step-namespaces []
       :framework :clojure.test
       :verbose true
       :framework-opts []
       :rspec-requires []
       :include-tags []
       :exclude-tags []}
      """

  Scenario: Invalid test framework is rejected
    Given a config:
      """
      {:framework :banana}
      """
    When the config is resolved
    Then the config should be invalid with message "banana"

  Scenario: features-dir must be a string
    Given a config:
      """
      {:features-dir 42}
      """
    When the config is resolved
    Then the config should be invalid with message "features-dir"

  Scenario: Unrecognized keys are rejected
    Given a config:
      """
      {:features-dir "features"
       :turbo-mode true}
      """
    When the config is resolved
    Then the config should be invalid with message "turbo-mode"
