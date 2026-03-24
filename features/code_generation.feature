Feature: Code generation

  The generator takes parsed feature IR and registered steps
  and produces runnable spec files with qualified function calls.

  Scenario: Generate a spec from a simple feature
    Given a feature named "Authentication" from source "auth.feature"
    And a scenario "User can log in" with steps:
      | type  | text                       |
      | given | a user "alice"             |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    When generating the spec with framework :speclj
    Then the output should contain "Authentication"
    And the output should contain "User can log in"
    And the output should contain "create-user"
    And the output should contain "user-logs-in"

  Scenario: Unrecognized steps generate pending scenarios
    Given a feature named "Pending" from source "pending.feature"
    And a scenario "Not yet done" with steps:
      | type  | text                        |
      | given | something not yet defined   |
      | when  | doing the undefined thing   |
    When generating the spec with framework :speclj
    Then the output should contain "pending"
