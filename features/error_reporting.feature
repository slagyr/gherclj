Feature: Error reporting

  gherclj provides clear error messages when it encounters
  problems in feature files or step definitions.

  Scenario: Feature file missing Feature keyword
    Given a feature file containing:
      """
      Scenario: No feature
        Given something
      """
    When the feature is parsed
    Then parsing should fail with message "Feature"

  Scenario: Empty feature file
    Given a feature file containing:
      """
      """
    When the feature is parsed
    Then parsing should fail with message "empty"

  Scenario: Step text matches multiple registered steps
    Given a given step named "greet-any" with template "hello {name}"
    And a given step named "greet-world" with template "hello world"
    When classifying "hello world"
    Then classification should fail with message "ambiguous"
    And the error should mention "greet-any"
    And the error should mention "greet-world"

  Scenario: Ambiguous match reports the step text
    Given a given step named "greet-any" with template "hello {name}"
    And a given step named "greet-world" with template "hello world"
    When classifying "hello world"
    Then the error should mention "hello world"
