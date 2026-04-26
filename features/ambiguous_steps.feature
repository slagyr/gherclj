Feature: Step ambiguity detection

  `gherclj ambiguity` walks the project's feature files and reports any
  step phrase that matches more than one registered step definition —
  the static analog of the runtime "ambiguous step match" error. Each
  finding shows the phrase, the feature-file:line where it appears, and
  every registered step it matches. A phrase appearing in multiple
  scenarios is reported once per occurrence.

  Scenario: ambiguity --help describes the subcommand and its options
    When running gherclj with "ambiguity --help"
    Then the output should contain lines:
      | gherclj ambiguity |
      | --features-dir    |
      | --step-namespaces |
      | --tag             |
      | --json            |
      | --edn             |
      | --help            |

  Scenario: No ambiguous phrases — clean report
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
    When running gherclj with "-f features -s gherclj.sample.app-steps ambiguity"
    Then the output should contain lines:
      | Scanned 1 scenario. No tag filtering applied. |
      | No ambiguous step phrases found.              |
    And the exit code should be zero

  Scenario: A phrase matching two registered steps is reported
    Given a features directory containing:
      | file              |
      | ambiguous.feature |
    And the feature "ambiguous.feature" contains:
      """
      Feature: Login
        Scenario: Greet
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.ambiguous-steps ambiguity"
    Then the output should contain lines:
      | Ambiguous step phrases:                       |
      | a user "alice"  (ambiguous.feature:3)         |
      | Matches:                                      |
      | a user {name:string}    (ambiguous_steps.clj: |
      | a user {handle:string}    (ambiguous_steps.clj: |
    And the exit code should be non-zero

  Scenario: A regex that overlaps with a template is reported
    Given a features directory containing:
      | file              |
      | ambiguous.feature |
    And the feature "ambiguous.feature" contains:
      """
      Feature: Login
        Scenario: Login
          When the user logs in
      """
    When running gherclj with "-f features -s gherclj.sample.ambiguous-steps ambiguity"
    Then the output should contain lines:
      | the user logs in  (ambiguous.feature:3) |
      | the user logs in                        |
      | ^the user .+$                           |
    And the exit code should be non-zero

  Scenario: A phrase repeated across scenarios is reported once per occurrence
    Given a features directory containing:
      | file              |
      | ambiguous.feature |
    And the feature "ambiguous.feature" contains:
      """
      Feature: Login
        Scenario: First
          Given a user "alice"

        Scenario: Second
          Given a user "bob"
      """
    When running gherclj with "-f features -s gherclj.sample.ambiguous-steps ambiguity"
    Then the output should contain lines:
      | a user "alice"  (ambiguous.feature:3) |
      | a user "bob"  (ambiguous.feature:6)   |

  Scenario: Tag filter — phrase only in excluded scenario is not reported
    Given a features directory containing:
      | file              |
      | ambiguous.feature |
    And the feature "ambiguous.feature" contains:
      """
      Feature: Login
        Scenario: Plain login
          When the user logs in

        @slow
        Scenario: Greet
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.ambiguous-steps ambiguity -t ~slow"
    Then the output should contain "1 scenario filtered out by tags: ~slow"
    And the output should not contain "a user \"alice\""

  # --- Machine-readable output ---

  Scenario: gherclj ambiguity --edn emits a structured report
    Given a features directory containing:
      | file              |
      | ambiguous.feature |
    And the feature "ambiguous.feature" contains:
      """
      Feature: Login
        Scenario: Greet
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.ambiguous-steps ambiguity --edn"
    Then the output should be valid EDN
    And the output should span multiple lines
    And the EDN report should include:
      | field             | value |
      | scenarios-scanned | 1     |
      | ambiguous-count   | 1     |
    And the :ambiguities list should contain an entry with phrase "a user \"alice\""

  Scenario: gherclj ambiguity --json emits a structured report
    Given a features directory containing:
      | file              |
      | ambiguous.feature |
    And the feature "ambiguous.feature" contains:
      """
      Feature: Login
        Scenario: Greet
          Given a user "alice"
      """
    When running gherclj with "-f features -s gherclj.sample.ambiguous-steps ambiguity --json"
    Then the output should be valid JSON
    And the output should span multiple lines
    And the JSON report should include:
      | field             | value |
      | scenarios-scanned | 1     |
      | ambiguous-count   | 1     |

  Scenario: --json and --edn together is an error
    When running gherclj with "-s gherclj.sample.ambiguous-steps ambiguity --json --edn"
    Then the exit code should be non-zero
    And the error output should mention "--json and --edn are mutually exclusive"
