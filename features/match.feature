@wip
Feature: Step matching

  `gherclj match "<phrase>"` classifies a step phrase against the
  registered step set: matched / no match / ambiguous, with the matched
  step's source location, helper-ref, docstring, and args paired with
  binding name and type.

  Matching is type-blind, in line with Cucumber semantics. The leading
  Gherkin keyword (Given / When / Then / And / But) is stripped from the
  input but does not constrain the search — a stepdef registered as
  `defwhen` will match a phrase pasted with a leading `Given`, and so
  on. The `:type` shown in output is the registration intent, not a
  filter.

  Scenario: match --help describes the subcommand and its options
    When running gherclj with "match --help"
    Then the output should contain lines:
      | gherclj match         |
      | -s, --step-namespaces |
      | --json                |
      | --edn                 |
      | --no-color            |
      | --help                |

  Scenario: A phrase with a single matching step shows full detail
    When running gherclj with "-s gherclj.sample.app-steps match Given a user \"alice\""
    Then the output should contain lines:
      | Phrase: a user "alice"               |
      | Matched step:                        |
      | Type:    Given                       |
      | Pattern: a user {name:string}        |
      | Source:  app_steps.clj:              |
      | Helper:  app-steps/create-adventurer |
      | Args:                                |
      | name (string) = "alice"              |

  Scenario: A phrase with no matching step reports no match
    When running gherclj with "-s gherclj.sample.app-steps match Given some imaginary thing"
    Then the output should contain lines:
      | Phrase: some imaginary thing |
      | No matching step.            |

  Scenario: A leading keyword that doesn't match the registration is fine
    # "the user logs in" is registered as defwhen in app-steps;
    # querying with "Given" still matches because matching is type-blind.
    When running gherclj with "-s gherclj.sample.app-steps match Given the user logs in"
    Then the output should contain lines:
      | Phrase: the user logs in |
      | Matched step:            |
      | Type:    When            |

  Scenario: A phrase matching two registrations is reported as ambiguous
    When running gherclj with "-s gherclj.sample.ambiguous-steps match a user \"alice\""
    Then the output should contain lines:
      | Phrase: a user "alice"          |
      | Ambiguous — 2 matching steps:   |

  # --- Machine-readable output ---

  Scenario: gherclj match --edn emits a structured report
    When running gherclj with "-s gherclj.sample.app-steps --edn match Given a user \"alice\""
    Then the output should be valid EDN
    And the output should span multiple lines
    And the EDN report should include:
      | field        | value          |
      | match-status | :matched       |
      | phrase       | a user "alice" |
    And the :matches list should contain an entry with phrase "a user {name:string}"

  Scenario: gherclj match --json emits a structured report
    When running gherclj with "-s gherclj.sample.app-steps --json match Given a user \"alice\""
    Then the output should be valid JSON
    And the output should span multiple lines
    And the JSON report should include:
      | field        | value          |
      | match-status | matched        |
      | phrase       | a user "alice" |

  Scenario: --json and --edn together is an error
    When running gherclj with "-s gherclj.sample.app-steps --json --edn match Given a user \"alice\""
    Then the exit code should be non-zero
    And the error output should mention "--json and --edn are mutually exclusive"
