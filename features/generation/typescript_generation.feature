Feature: TypeScript node:test code generation

  The TypeScript adapter produces native node:test files. Helper imports
  declared via helper! become TypeScript import lines; describe-setup!
  contributions land inside the describe block; step refs of the form
  subject.method-name render as subject.methodName(args) calls.

  Background:
    Given step namespace "gherclj.sample.typescript-app-steps"

  @smoke
  Scenario: Generate a node:test spec with subject method calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                                    |
      | given | a TypeScript user "alice"              |
      | when  | the TypeScript user logs in             |
      | then  | the TypeScript response should be 200   |
    When generating the spec with framework :typescript/node-test
    Then the generated code should be:
      """
      // generated from login.feature
      import { beforeEach, describe, test } from 'node:test'
      import * as typescript_app_steps from '../../../lib/typescript_app_steps'

      describe('Login', () => {
        let subject: typescript_app_steps.SampleAppSteps
        beforeEach(() => {
          subject = new typescript_app_steps.SampleAppSteps()
        })


        test('Valid credentials', () => {
          subject.createAdventurer('alice')
          subject.enterTheRealm()
          subject.verifyOutcome(200)
        })
      })
      """

  Scenario: Unrecognized steps generate a pending node:test block
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :typescript/node-test
    Then the output should contain "skip: 'not yet implemented'"

  Scenario: subject.method-name helper-refs render as subject.methodName(args)
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                      |
      | given | a TypeScript user "alice" |
    When generating the spec with framework :typescript/node-test
    Then the output should contain "subject.createAdventurer('alice')"

  Scenario: helper! with a string value emits a path-relative import
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                      |
      | given | a TypeScript user "alice" |
    When generating the spec with framework :typescript/node-test
    Then the output should contain "import * as typescript_app_steps from '../../../lib/typescript_app_steps'"

  Scenario: describe-setup! contributions appear inside the describe block
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                      |
      | given | a TypeScript user "alice" |
    When generating the spec with framework :typescript/node-test
    Then the output should contain "let subject: typescript_app_steps.SampleAppSteps"
    And the output should contain "beforeEach(() => {"
