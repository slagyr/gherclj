@wip
Feature: Step catalog

  `gherclj steps` lists all registered step definitions grouped by type.
  Each entry shows the phrase and source location on one line, with an
  optional docstring indented on the next. An optional keyword argument
  filters by phrase or docstring content. --given, --when, and --then
  are additive: each flag includes that type; no flags means all types.

  # --- Output format ---

  Scenario: Catalog groups steps under Given, When, Then headers
    When running gherclj with "-s gherclj.sample.app-steps steps"
    Then the output should contain "Given:"
    And the output should contain "When:"
    And the output should contain "Then:"

  Scenario: Each entry shows phrase and source location on one line
    When running gherclj with "-s gherclj.sample.app-steps steps"
    Then the output should contain "a user {name:string}  (app_steps.clj:"
    And the output should contain "the user logs in  (app_steps.clj:"
    And the output should contain "the response should be {status:int}  (app_steps.clj:"

  Scenario: Entry with docstring shows it on the line after the phrase
    When running gherclj with "-s gherclj.sample.dragon-steps steps"
    Then the catalog output should include:
      """
      the dragon hoards {item:string}  (dragon_steps.clj:
        Adds to hoard without duplicate checking.
      """

  Scenario: Entry without docstring has no extra line before the next entry
    When running gherclj with "-s gherclj.sample.dragon-steps steps"
    Then the catalog output should include:
      """
      a dragon named {name:string}  (dragon_steps.clj:
      the dragon hoards {item:string}  (dragon_steps.clj:
      """

  # --- Keyword filter ---

  Scenario: Keyword filter shows only steps with matching phrase
    When running gherclj with "-s gherclj.sample.app-steps steps user"
    Then the output should contain "a user {name:string}"
    And the output should contain "the user logs in"
    And the output should not contain "the response should be {status:int}"

  Scenario: Keyword filter matches against docstring content
    When running gherclj with "-s gherclj.sample.dragon-steps steps buried"
    Then the output should contain "the hoard should include {item:string}"
    And the output should not contain "Raises cave temperature"

  # --- Type filters ---

  Scenario: --given shows only Given steps
    When running gherclj with "-s gherclj.sample.app-steps steps --given"
    Then the output should contain "Given:"
    And the output should contain "a user {name:string}"
    And the output should not contain "When:"
    And the output should not contain "Then:"

  Scenario: --when shows only When steps
    When running gherclj with "-s gherclj.sample.app-steps steps --when"
    Then the output should contain "When:"
    And the output should contain "the user logs in"
    And the output should not contain "Given:"
    And the output should not contain "Then:"

  Scenario: --given and --when together show both types but not Then
    When running gherclj with "-s gherclj.sample.app-steps steps --given --when"
    Then the output should contain "Given:"
    And the output should contain "When:"
    And the output should not contain "Then:"

  Scenario: --given --when --then shows all steps
    When running gherclj with "-s gherclj.sample.app-steps steps --given --when --then"
    Then the output should contain "Given:"
    And the output should contain "When:"
    And the output should contain "Then:"

  # --- Color ---

  Scenario: Output is colorized by default
    When running gherclj with "-s gherclj.sample.app-steps steps"
    Then the output should have color codes

  Scenario: --no-color suppresses ANSI escape sequences
    When running gherclj with "-s gherclj.sample.app-steps steps --no-color"
    Then the output should not contain "Unknown option"
    And the output should have no color codes

  # --- Help ---

  Scenario: steps --help describes the subcommand and its options
    When running gherclj with "steps --help"
    Then the output should contain "gherclj steps"
    And the output should contain "--given"
    And the output should contain "--when"
    And the output should contain "--then"
    And the output should contain "--no-color"
    And the output should contain "-s, --step-namespaces"
