@wip
Feature: Shared state

  gherclj provides a global state atom for step definitions
  to share data across steps within a scenario. State is
  reset before each scenario.

  Scenario: State starts empty after reset
    When the state is reset
    Then the state should be:
      """
      {}
      """

  Scenario: Swap updates the state
    When the state is reset
    And the state is swapped with assoc :user "alice"
    Then the state should be:
      """
      {:user "alice"}
      """

  Scenario: Get retrieves the current state
    When the state is reset
    And the state is swapped with assoc :user "alice"
    Then getting :user should return "alice"

  Scenario: Reset clears user state
    When the state is reset
    And the state is swapped with assoc :user "alice"
    And the state is reset
    Then the state should be:
      """
      {}
      """

  Scenario: gherclj internal state is namespaced
    When the state is reset
    And gherclj stores internal data
    Then the state should have a :_gherclj key
    And getting :_gherclj should not be nil

  Scenario: Reset preserves nothing
    When the state is reset
    And the state is swapped with assoc :user "alice"
    And gherclj stores internal data
    And the state is reset
    Then the state should be:
      """
      {}
      """
