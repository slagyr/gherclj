Feature: Step patterns

  Step definitions accept either a template string or a raw regex.
  Templates offer convenience with typed captures; regexes offer
  full control without coercion.

  Scenario: String capture
    Given a given step named "greet" with template "hello {name:string}"
    Then the step "greet" should match "hello Alice"
    And the match args should be ["Alice"]

  Scenario: Float capture
    Given a then step named "check-price" with template "the price is {amount:float}"
    Then the step "check-price" should match "the price is 19.99"
    And the match args should be [19.99]

  Scenario: Multiple captures of different types
    Given a given step named "set-config" with template "set {key:string} to {value:int}"
    Then the step "set-config" should match "set timeout to 300"
    And the match args should be ["timeout" 300]

  Scenario: Template escapes regex special characters
    Given a given step named "parens" with template "call foo() with {n:int} args"
    Then the step "parens" should match "call foo() with 3 args"
    And the match args should be [3]

  Scenario: Macro accepts a raw regex pattern
    Given the registered step "cave-contains"
    Then the step "cave-contains" should match "the cave contains hello world"
    And the match args should be ["hello world"]

  Scenario: Regex captures are strings, not coerced
    Given the registered step "dragon-count"
    Then the step "dragon-count" should match "the dragon has 42 treasures"
    And the match args should be ["42"]
