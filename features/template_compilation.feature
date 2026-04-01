Feature: Template compilation

  Templates are the human-readable patterns in step definitions.
  The compiler converts them to regexes with named captures
  and type coercions.

  Scenario: Compile a plain template with no captures
    Given a template "checking for zombies"
    When the template is compiled
    Then the regex should be "^checking for zombies$"
    And there should be 0 bindings

  Scenario: Compile an integer capture
    Given a template "timeout is {seconds:int}"
    When the template is compiled
    Then the regex should match "timeout is 300"
    And the captured value should be 300

  Scenario: Compile a word capture
    Given a template "status is {status}"
    When the template is compiled
    Then the regex should match "status is active"
    And the captured value should be "active"

  Scenario: Compile multiple captures
    Given a template "set {key} to {value}"
    When the template is compiled
    Then the regex should match "set timeout to 300"
    And there should be 2 bindings
