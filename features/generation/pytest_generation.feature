Feature: Pytest code generation

  The pytest adapter produces native Python test files. Helper imports
  declared via helper! become import lines; pytest/scenario-setup!
  contributions land inside each test function; step refs of the form
  sut.method-name render as sut.method_name(args) calls.

  Background:
    Given step namespace "gherclj.sample.pytest-app-steps"

  @smoke
  Scenario: Generate a pytest test module with sut method calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                              |
      | given | a Python user "alice"            |
      | when  | the Python user logs in           |
      | then  | the Python response should be 200 |
    When generating the spec with framework :python/pytest
    Then the generated code should be:
      """
      # generated from login.feature
      import pytest
      from sample_app import SampleApp

      def test_valid_credentials():
          sut = SampleApp()
          sut.create_adventurer('alice')
          sut.enter_the_realm()
          sut.verify_outcome(200)
      """

  Scenario: Unrecognized steps generate a skipped pytest test function
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :python/pytest
    Then the output should contain "pytest.skip('not yet implemented')"

  Scenario: sut.method-name helper-refs render as sut.method_name(args)
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                  |
      | given | a Python user "alice" |
    When generating the spec with framework :python/pytest
    Then the output should contain "sut.create_adventurer('alice')"

  Scenario: helper! with a string value emits a Python import
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                  |
      | given | a Python user "alice" |
    When generating the spec with framework :python/pytest
    Then the output should contain "from sample_app import SampleApp"

  Scenario: scenario-setup! contributions appear inside each test function
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                  |
      | given | a Python user "alice" |
    When generating the spec with framework :python/pytest
    Then the output should contain "    sut = SampleApp()"
