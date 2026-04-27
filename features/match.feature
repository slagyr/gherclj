@wip
Feature: Step matching

  `gherclj match "<phrase>"` reports how a step phrase classifies
  against the registered step set: matched / no match / ambiguous,
  with the matched step's source location, helper-ref, docstring, and
  args paired with binding names and types.

  When the phrase begins with Given / When / Then, matches are filtered
  to that type. With And, But, or no leading keyword, matches are
  reported across all three types — same-phrase pairs across types are
  not ambiguous.

  Scenario: match --help describes the subcommand and its options
    When running gherclj with "match --help"
    Then the output should contain lines:
      | gherclj match         |
      | -s, --step-namespaces |
      | --json                |
      | --edn                 |
      | --no-color            |
      | --help                |

  # --- Typed match ---

  Scenario: A Given phrase with a single matching step shows full detail
    When running gherclj with "-s gherclj.sample.app-steps match Given a user \"alice\""
    Then the output should contain lines:
      | Phrase: a user "alice"  (Given)      |
      | Matched step:                        |
      | Pattern: a user {name:string}        |
      | Source:  app_steps.clj:              |
      | Helper:  app-steps/create-adventurer |
      | Args:                                |
      | name (string) = "alice"              |

  Scenario: A typed phrase with no matching step reports no match
    When running gherclj with "-s gherclj.sample.app-steps match Given some imaginary thing"
    Then the output should contain lines:
      | Phrase: some imaginary thing  (Given) |
      | No matching step.                     |

  Scenario: A typed phrase matching two same-type steps is ambiguous
    When running gherclj with "-s gherclj.sample.ambiguous-steps match Given a user \"alice\""
    Then the output should contain lines:
      | Phrase: a user "alice"  (Given)               |
      | Ambiguous — 2 matching Given steps:           |
      | a user {name:string}    (ambiguous_steps.clj: |
      | a user {handle:string}  (ambiguous_steps.clj: |

  Scenario: Type filter excludes matches of other types
    # "the user logs in" is registered as When in app-steps;
    # querying as Given must find nothing.
    When running gherclj with "-s gherclj.sample.app-steps match Given the user logs in"
    Then the output should contain "No matching step."

  # --- Untyped match (no keyword, And, But) ---

  Scenario: A phrase with no leading keyword matches across all types
    When running gherclj with "-s gherclj.sample.same-phrase-steps match the user logs in"
    Then the output should contain lines:
      | Phrase: the user logs in  (any type) |
      | Matched in Given:                    |
      | Matched in When:                     |

  Scenario: And or But prefixes are treated the same as no keyword
    When running gherclj with "-s gherclj.sample.same-phrase-steps match And the user logs in"
    Then the output should contain "(any type)"

  # --- Machine-readable output ---

  Scenario: gherclj match --edn emits a structured report
    When running gherclj with "-s gherclj.sample.app-steps --edn match Given a user \"alice\""
    Then the output should be valid EDN
    And the output should span multiple lines
    And the EDN report should include:
      | field          | value          |
      | match-status   | :matched       |
      | requested-type | :given         |
      | phrase         | a user "alice" |
    And the :matches list should contain an entry with phrase "a user {name:string}"

  Scenario: gherclj match --json emits a structured report
    When running gherclj with "-s gherclj.sample.app-steps --json match Given a user \"alice\""
    Then the output should be valid JSON
    And the output should span multiple lines
    And the JSON report should include:
      | field          | value          |
      | match-status   | matched        |
      | requested-type | given          |
      | phrase         | a user "alice" |

  Scenario: --json and --edn together is an error
    When running gherclj with "-s gherclj.sample.app-steps --json --edn match Given a user \"alice\""
    Then the exit code should be non-zero
    And the error output should mention "--json and --edn are mutually exclusive"
