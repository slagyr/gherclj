Feature: Code generation

  The generator takes parsed feature IR and registered steps
  and produces runnable spec files with qualified function calls.

  Scenario: Generate a spec from a simple feature
    Given a feature named "Authentication" from source "auth.feature"
    And a scenario "User can log in" with steps:
      | type  | text                        |
      | given | timeout is 300              |
      | when  | running the action          |
      | then  | the status should be active |
    When generating the spec with framework :speclj
    Then the output should contain "Authentication"
    And the output should contain "User can log in"
    And the output should contain "add-timeout 300"
    And the output should contain "run-action"

  Scenario: Unrecognized steps generate pending scenarios
    Given a feature named "Pending" from source "pending.feature"
    And a scenario "Not yet done" with steps:
      | type  | text                        |
      | given | something not yet defined   |
      | when  | doing the undefined thing   |
    When generating the spec with framework :speclj
    Then the output should contain "pending"
