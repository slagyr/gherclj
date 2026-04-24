Feature: Step docstrings

  defgiven, defwhen, and defthen accept an optional docstring between
  the template and arg vector. The docstring is stored in the step
  registry alongside the phrase, so the step catalog can explain
  each step's contract without reading source.

  Scenario: Step without a docstring has nil doc in the registry
    Given the registered step "summon-dragon" from dragon suite
    Then the step should have no docstring

  Scenario: Given step with docstring stores it in the registry
    Given the registered step "dragon-hoards" from dragon suite
    Then the step should have docstring "Adds to hoard without duplicate checking."

  Scenario: When step stores its docstring
    Given the registered step "dragon-breathes" from dragon suite
    Then the step should have docstring "Raises cave temperature by exactly 300 degrees."

  Scenario: Then step stores its docstring
    Given the registered step "treasure-check" from dragon suite
    Then the step should have docstring "Checks visible hoard only — buried treasure excluded."
