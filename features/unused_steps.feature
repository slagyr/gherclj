@wip
Feature: Unused step detection

  `gherclj unused` compares registered step definitions against all
  step texts found in feature files and reports any steps never
  referenced. By default all scenarios are searched. Tag filters
  narrow which scenarios count as "used"; the output always states
  exactly what was searched.

  Scenario: All steps referenced — reports none unused
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth
        Scenario: Login
          Given a user "alice"
          When the user logs in
          Then the response should be 200
      """
    When running gherclj with "-f features -s gherclj.sample.app-steps unused"
    Then the output should contain lines:
      | Scanned 1 scenario. No tag filtering applied. |
      | 3 of 3 registered steps are in use.           |
      | No unused steps found.                        |

  Scenario: Step never referenced in any feature file is reported
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth
        Scenario: Login
          Given a user "alice"
          When the user logs in
      """
    When running gherclj with "-f features -s gherclj.sample.app-steps unused"
    Then the output should contain lines:
      | Scanned 1 scenario. No tag filtering applied.         |
      | 2 of 3 registered steps are in use.                   |
      | 1 unused step found.                                  |
      | the response should be {status:int}  (app_steps.clj: |
    And the output should not contain "a user {name:string}"
    And the output should not contain "the user logs in"

  Scenario: Unused steps grouped under type headers
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth
        Scenario: Login
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.app-steps unused"
    Then the output should contain lines:
      | Scanned 1 scenario. No tag filtering applied. |
      | 1 of 3 registered steps are in use.           |
      | 2 unused steps found.                         |
      | When:                                         |
      | the user logs in  (app_steps.clj:             |
      | Then:                                         |
      | the response should be {status:int}  (app_steps.clj: |
    And the output should not contain "Given:"

  Scenario: Tag filter — step only in excluded scenario counts as unused
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth
        Scenario: Login
          Given a user "alice"
          When the user logs in

        @slow
        Scenario: Slow check
          Then the response should be 200
      """
    When running gherclj with "-f features -s gherclj.sample.app-steps unused -t ~slow"
    Then the output should contain lines:
      | Scanned 1 scenario. 1 scenario unscanned due to tag filters: ~slow. |
      | 2 of 3 registered steps are in use.                                  |
      | 1 unused step found.                                                 |
      | the response should be {status:int}  (app_steps.clj:                |
    And the output should not contain "a user {name:string}"
    And the output should not contain "the user logs in"
