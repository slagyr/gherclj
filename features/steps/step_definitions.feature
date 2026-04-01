Feature: Step definitions

  Steps are the building blocks of Gherkin scenarios. Each step
  is defined with defgiven, defwhen, or defthen and maps a
  template string to a callable function.

  @smoke
  Scenario: Define a given step with integer capture
    Given a given step named "add-timeout" with template "timeout is {seconds:int}"
    Then the step "add-timeout" should be registered as a :given step
    And the step "add-timeout" should match "timeout is 300"
    And the match args should be [300]

  Scenario: Define a when step with no captures
    Given a when step named "run-action" with template "running the action"
    Then the step "run-action" should be registered as a :when step
    And the step "run-action" should match "running the action"
    And the match args should be []

  Scenario: Define a then step with word capture
    Given a then step named "check-status" with template "the status should be {status}"
    Then the step "check-status" should be registered as a :then step
    And the step "check-status" should match "the status should be active"
    And the match args should be ["active"]

  Scenario: Unrecognized step text returns no match
    Given a given step named "add-timeout" with template "timeout is {seconds:int}"
    When classifying "something completely unrecognized"
    Then no step should match
