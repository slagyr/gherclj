Feature: Unused step detection

  `gherclj unused` compares registered step definitions against all
  step texts found in feature files and reports any steps never
  referenced. By default all scenarios are searched. Tag filters
  narrow which scenarios count as "used"; the output always states
  exactly what was searched.

  Scenario: unused --help describes the subcommand and its options
    When running gherclj with "unused --help"
    Then the output should contain lines:
      | gherclj unused         |
      | --features-dir         |
      | --step-namespaces      |
      | --tag                  |
      | --help                 |

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
      | 2 of 3 registered steps are in use (1 unused).        |
      | Unused steps:                                         |
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
      | Scanned 1 scenario. No tag filtering applied.         |
      | 1 of 3 registered steps are in use (2 unused).        |
      | Unused steps:                                         |
      | When:                                                 |
      | the user logs in  (app_steps.clj:                     |
      | Then:                                                 |
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
      | Scanned 1 of 2 scenarios. 1 scenario filtered out by tags: ~slow.   |
      | 2 of 3 registered steps are in use (1 unused).                       |
      | Unused steps:                                                        |
      | the response should be {status:int}  (app_steps.clj:                |
    And the output should not contain "a user {name:string}"
    And the output should not contain "the user logs in"

  Scenario: gherclj unused --edn emits a structured report
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth
        Scenario: Login
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.app-steps unused --edn"
    Then the output should be valid EDN
    And the output should span multiple lines
    And the EDN report should include:
      | field             | value |
      | scenarios-scanned | 1     |
    And the :unused-steps list should contain a step with phrase "the user logs in"
    And the :unused-steps list should contain a step with phrase "the response should be {status:int}"

  Scenario: gherclj unused --json emits a structured report
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth
        Scenario: Login
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.app-steps unused --json"
    Then the output should be valid JSON
    And the output should span multiple lines
    And the JSON report should include:
      | field             | value |
      | scenarios-scanned | 1     |
