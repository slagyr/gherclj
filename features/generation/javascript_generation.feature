Feature: JavaScript node:test code generation

  The JavaScript adapter produces native node:test files. Helper imports
  declared via helper! become JavaScript import lines; js/scenario-setup!
  contributions land inside each test function; step refs of the form
  subject.method-name render as subject.methodName(args) calls.

  Background:
    Given step namespace "gherclj.sample.javascript-app-steps"

  @smoke
  Scenario: Generate a node:test module with subject method calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                                    |
      | given | a JavaScript user "alice"             |
      | when  | the JavaScript user logs in             |
      | then  | the JavaScript response should be 200   |
    When generating the spec with framework :javascript/node-test
    Then the generated code should be:
      """
      // generated from login.feature
      import test from 'node:test'
      import * as javascript_app_steps from '../../../lib/javascript_app_steps.js'

      test('Valid credentials', () => {
        const subject = new javascript_app_steps.SampleAppSteps()
        subject.createAdventurer('alice')
        subject.enterTheRealm()
        subject.verifyOutcome(200)
      })
      """

  Scenario: Unrecognized steps generate a pending node:test block
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :javascript/node-test
    Then the output should contain "skip: 'not yet implemented'"

  Scenario: subject.method-name helper-refs render as subject.methodName(args)
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                      |
      | given | a JavaScript user "alice" |
    When generating the spec with framework :javascript/node-test
    Then the output should contain "subject.createAdventurer('alice')"

  Scenario: helper! with a string value emits a path-relative import
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                      |
      | given | a JavaScript user "alice" |
    When generating the spec with framework :javascript/node-test
    Then the output should contain "import * as javascript_app_steps from '../../../lib/javascript_app_steps.js'"

  Scenario: scenario-setup! contributions appear inside each test function
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                      |
      | given | a JavaScript user "alice" |
    When generating the spec with framework :javascript/node-test
    Then the output should contain "  const subject = new javascript_app_steps.SampleAppSteps()"
