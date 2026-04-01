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

  Scenario: assoc! sets a key
    When the state is reset
    And assoc! :user "alice"
    Then the state should be:
      """
      {:user "alice"}
      """

  Scenario: assoc! sets multiple keys
    When the state is reset
    And assoc! :user "alice" :role "admin"
    Then the state should be:
      """
      {:user "alice" :role "admin"}
      """

  Scenario: assoc-in! sets a nested key
    When the state is reset
    And assoc-in! [:user :name] "alice"
    Then the state should be:
      """
      {:user {:name "alice"}}
      """

  Scenario: dissoc! removes a key
    When the state is reset
    And assoc! :user "alice" :role "admin"
    And dissoc! :role
    Then the state should be:
      """
      {:user "alice"}
      """

  Scenario: get returns the full state
    When the state is reset
    And assoc! :user "alice"
    Then get should return:
      """
      {:user "alice"}
      """

  Scenario: get returns a single key
    When the state is reset
    And assoc! :user "alice"
    Then get :user should return "alice"

  Scenario: get returns default for missing key
    When the state is reset
    Then get :user "nobody" should return "nobody"

  Scenario: get-in returns a nested value
    When the state is reset
    And assoc-in! [:user :name] "alice"
    Then get-in [:user :name] should return "alice"

  Scenario: swap! applies a function
    When the state is reset
    And assoc! :count 0
    And swap! update :count inc
    Then get :count should return 1

  Scenario: update! updates a key with a function
    When the state is reset
    And assoc! :count 0
    And update! :count inc
    Then get :count should return 1

  Scenario: update-in! updates a nested key
    When the state is reset
    And assoc-in! [:user :roles] []
    And update-in! [:user :roles] conj "admin"
    Then get-in [:user :roles] should return:
      """
      ["admin"]
      """

  Scenario: reset clears all state
    When the state is reset
    And assoc! :user "alice"
    And the state is reset
    Then the state should be:
      """
      {}
      """

  Scenario: gherclj internal state is namespaced under :_gherclj
    When the state is reset
    And gherclj stores internal data
    Then get :_gherclj should not be nil
    And get :user should be nil
