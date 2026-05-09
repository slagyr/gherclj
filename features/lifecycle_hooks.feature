Feature: Lifecycle hooks

  Step namespaces can register hooks around the generated run, feature,
  and scenario lifecycle, and those hooks run in a predictable order.

  Scenario: Speclj generation includes feature and scenario hook runners
    Given a feature named "Hooks" from source "hooks.feature"
    And a scenario "Passes" with steps:
      | type  | text                       |
      | given | a user "alice"             |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    When generating the spec with framework :clojure/speclj and lifecycle hooks enabled
    Then the output should contain "(around [it]"
    And the output should contain "(binding [g/*state* (atom {})]"
    And the output should contain "(lifecycle/run-before-feature-hooks!)"
    And the output should contain "(binding [g/*state* (atom @g/*state*)]"
    And the output should contain "(lifecycle/run-before-scenario-hooks!)"
    And the output should contain "(lifecycle/run-after-scenario-hooks!)"
    And the output should contain "(lifecycle/run-after-feature-hooks!)"

  Scenario: clojure.test generation includes feature and scenario hook runners
    Given a feature named "Hooks" from source "hooks.feature"
    And a scenario "Passes" with steps:
      | type  | text                       |
      | given | a user "alice"             |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    When generating the spec with framework :clojure/test and lifecycle hooks enabled
    Then the output should contain "(use-fixtures :once"
    And the output should contain "(binding [g/*state* (atom {})]"
    And the output should contain "(lifecycle/run-before-feature-hooks!)"
    And the output should contain "(lifecycle/run-after-feature-hooks!)"
    And the output should contain "(use-fixtures :each"
    And the output should contain "(binding [g/*state* (atom @g/*state*)]"
    And the output should contain "(lifecycle/run-before-scenario-hooks!)"
    And the output should contain "(lifecycle/run-after-scenario-hooks!)"

  Scenario: Scenario writes do not leak to sibling scenarios
    Given lifecycle event recording is enabled
    And a feature named "State isolation" from source "state_isolation.feature"
    And a scenario "First keeps its own value" with steps:
      | type | text                                 |
      | when | the shared value becomes "first"    |
      | then | the shared value should be "first"  |
    And a scenario "Second starts empty and keeps its own value" with steps:
      | type | text                                  |
      | then | the shared value should be absent     |
      | when | the shared value becomes "second"    |
      | then | the shared value should be "second"  |
    When the generated scenarios run directly with framework :clojure/test
    Then the run should succeed

  Scenario: Before-feature state is visible to every scenario without leaking overrides back
    Given lifecycle event recording is enabled
    And a before-feature hook seeds the shared value "seed"
    And a feature named "State inheritance" from source "state_inheritance.feature"
    And a scenario "First sees the feature seed and overrides locally" with steps:
      | type | text                                     |
      | then | the shared value should be "seed"       |
      | when | the shared value becomes "override"     |
      | then | the shared value should be "override"   |
    And a scenario "Second still sees the feature seed" with steps:
      | type | text                               |
      | then | the shared value should be "seed" |
    When the generated scenarios run directly with framework :clojure/test
    Then the run should succeed

  Scenario: Direct generated runs record feature and scenario lifecycle events
    Given lifecycle event recording is enabled
    And a before-feature hook records "before-feature"
    And a before-scenario hook records "before-scenario"
    And an after-scenario hook records "after-scenario"
    And an after-feature hook records "after-feature"
    And a feature named "Hooks" from source "hooks.feature"
    And a scenario "Passes first" with steps:
      | type  | text                       |
      | given | a user "alice"             |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    And a scenario "Passes second" with steps:
      | type  | text                       |
      | given | a user "bob"               |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    When the generated scenarios run directly with framework :clojure/test
    Then the recorded lifecycle events should be:
      | event           |
      | before-feature  |
      | before-scenario |
      | after-scenario  |
      | before-scenario |
      | after-scenario  |
      | after-feature   |

  Scenario: After-scenario hook fires even when a step fails
    Given lifecycle event recording is enabled
    And an after-scenario hook records "after-scenario"
    And a feature named "Failure" from source "failure.feature"
    And a scenario "Crashes" with steps:
      | type | text                          |
      | then | the dragon vanishes unexpectedly |
    When the generated scenarios run directly with framework :clojure/test
    Then the run should fail
    And the recorded lifecycle events should be:
      | event          |
      | after-scenario |

  Scenario: Gherclj runs record all lifecycle events
    Given lifecycle event recording is enabled
    And a before-all hook records "before-all"
    And a before-feature hook records "before-feature"
    And a before-scenario hook records "before-scenario"
    And an after-scenario hook records "after-scenario"
    And an after-feature hook records "after-feature"
    And an after-all hook records "after-all"
    And a feature named "Hooks" from source "hooks.feature"
    And a scenario "Passes" with steps:
      | type  | text                       |
      | given | a user "alice"             |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    When the generated scenarios run through gherclj with framework :clojure/test
    Then the recorded lifecycle events should be:
      | event           |
      | before-all      |
      | before-feature  |
      | before-scenario |
      | after-scenario  |
      | after-feature   |
      | after-all       |
