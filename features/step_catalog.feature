@wip
Feature: Step catalog

  `gherclj steps` lists all registered step definitions grouped by type.
  Each entry shows the phrase and source location on one line, with an
  optional docstring indented on the next. An optional keyword argument
  filters by phrase or docstring content. --given, --when, and --then
  are additive: each flag includes that type; no flags means all types.

  # --- Output format ---

  Scenario: Catalog groups steps under Given, When, Then headers
    When running gherclj with "-s gherclj.features.steps.sample-app steps"
    Then the output should contain "Given:"
    And the output should contain "When:"
    And the output should contain "Then:"

  Scenario: Each entry shows phrase and source location on one line
    When running gherclj with "-s gherclj.features.steps.sample-app steps"
    Then the output should contain "a user {name:string}  (sample_app.clj:"
    And the output should contain "the user logs in  (sample_app.clj:"
    And the output should contain "the response should be {status:int}  (sample_app.clj:"

  Scenario: Entry with docstring shows it on the line after the phrase
    When running gherclj with "-s gherclj.features.steps.step-docstrings steps"
    Then the catalog output should include:
      """
      a documented step  (step_docstrings.clj:
        Sets :crew atom — does NOT write disk.
      """

  Scenario: Entry without docstring has no extra line before the next entry
    When running gherclj with "-s gherclj.features.steps.step-docstrings steps"
    Then the catalog output should include:
      """
      a bare step with no doc  (step_docstrings.clj:
      a documented step  (step_docstrings.clj:
      """

  # --- Keyword filter ---

  Scenario: Keyword filter shows only steps with matching phrase
    When running gherclj with "-s gherclj.features.steps.sample-app steps name"
    Then the output should contain "a user {name:string}"
    And the output should not contain "the user logs in"
    And the output should not contain "the response should be {status:int}"

  Scenario: Keyword filter matches against docstring content
    When running gherclj with "-s gherclj.features.steps.step-docstrings steps disk"
    Then the output should contain "a documented step"
    And the output should not contain "Polls for up to 2s."

  # --- Type filters ---

  Scenario: --given shows only Given steps
    When running gherclj with "-s gherclj.features.steps.sample-app steps --given"
    Then the output should contain "Given:"
    And the output should contain "a user {name:string}"
    And the output should not contain "When:"
    And the output should not contain "Then:"

  Scenario: --when shows only When steps
    When running gherclj with "-s gherclj.features.steps.sample-app steps --when"
    Then the output should contain "When:"
    And the output should contain "the user logs in"
    And the output should not contain "Given:"
    And the output should not contain "Then:"

  Scenario: --given and --when together show both types but not Then
    When running gherclj with "-s gherclj.features.steps.sample-app steps --given --when"
    Then the output should contain "Given:"
    And the output should contain "When:"
    And the output should not contain "Then:"

  Scenario: --given --when --then shows all steps
    When running gherclj with "-s gherclj.features.steps.sample-app steps --given --when --then"
    Then the output should contain "Given:"
    And the output should contain "When:"
    And the output should contain "Then:"

  # --- Color ---

  Scenario: --no-color produces output without ANSI escape sequences
    When running gherclj with "-s gherclj.features.steps.sample-app steps --no-color"
    Then the output should have no color codes

  Scenario: --color forces ANSI color codes in output
    When running gherclj with "-s gherclj.features.steps.sample-app steps --color"
    Then the output should have color codes

  # --- Help ---

  Scenario: steps --help describes the subcommand and its options
    When running gherclj with "steps --help"
    Then the output should contain "gherclj steps"
    And the output should contain "--given"
    And the output should contain "--when"
    And the output should contain "--then"
    And the output should contain "--color"
    And the output should contain "--no-color"
    And the output should contain "-s, --step-namespaces"
