Feature: Step docstrings

  defgiven, defwhen, and defthen accept an optional docstring between
  the template and arg vector. The docstring is stored in the step
  registry alongside the phrase, so the step catalog can explain
  each step's contract without reading source.

  Scenario: Step without a docstring has nil doc in the registry
    Given the registered step "fixture-no-doc" from docstring suite
    Then the step should have no docstring

  Scenario: Given step with docstring stores it in the registry
    Given the registered step "fixture-with-doc" from docstring suite
    Then the step should have docstring "Sets :crew atom — does NOT write disk."

  Scenario: When step stores its docstring
    Given the registered step "fixture-async" from docstring suite
    Then the step should have docstring "Polls for up to 2s."

  Scenario: Then step stores its docstring
    Given the registered step "fixture-check" from docstring suite
    Then the step should have docstring "Matches within 2s timeout."
