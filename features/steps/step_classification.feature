@wip
Feature: Step type-aware classification

  Classification of a step phrase against the registered step set honors
  the Gherkin keyword (Given / When / Then) on the step. Two stepdefs
  with the same phrase but different types coexist without triggering
  an ambiguity error; same-type duplicates remain ambiguous.

  Scenario: Same phrase as Given and When is not ambiguous
    Given a given step named "login-state" with template "the user logs in"
    And a when step named "perform-login" with template "the user logs in"
    Then matching "the user logs in" as Given finds "login-state"
    And matching "the user logs in" as When finds "perform-login"

  Scenario: Same phrase registered twice within one type is still ambiguous
    Given a given step named "login-a" with template "the user logs in"
    And a given step named "login-b" with template "the user logs in"
    Then matching "the user logs in" as Given is ambiguous

  Scenario: A phrase classifies to nothing when no step of that type matches
    Given a given step named "login-state" with template "the user logs in"
    Then matching "the user logs in" as When finds nothing
