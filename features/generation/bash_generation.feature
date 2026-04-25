Feature: Bash test code generation

  The Bash adapter produces native Bash test scripts. Helper imports
  declared via helper! become source lines; bash/scenario-setup!
  contributions land inside each generated scenario function; step refs of the form
  subject.method-name render as subject_method_name args calls.

  Background:
    Given step namespace "gherclj.sample.bash-app-steps"

  @smoke
  Scenario: Generate a Bash test script with function calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                            |
      | given | a Bash user "alice"            |
      | when  | the Bash user logs in           |
      | then  | the Bash response should be 200 |
    When generating the spec with framework :bash/testing
    Then the generated code should be:
      """
      #!/usr/bin/env bash
      # generated from login.feature
      set -uo pipefail
      SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
      source "$SCRIPT_DIR/../../../lib/sample_app.sh"

      failures=0

      run_test() {
        local name="$1"
        shift
        if "$@"; then
          printf 'ok - %s\n' "$name"
        else
          printf 'not ok - %s\n' "$name" >&2
          failures=$((failures + 1))
        fi
      }

      valid_credentials() {
        subject_new
        subject_create_adventurer 'alice'
        subject_enter_the_realm
        subject_verify_outcome 200
      }

      run_test 'Valid credentials' valid_credentials
      exit "$failures"
      """

  Scenario: Unrecognized steps generate a skipped Bash test entry
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :bash/testing
    Then the output should contain "skip - Not implemented # not yet implemented"

  Scenario: subject.method-name helper-refs render as subject_method_name args
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                 |
      | given | a Bash user "alice" |
    When generating the spec with framework :bash/testing
    Then the output should contain "subject_create_adventurer 'alice'"

  Scenario: helper! with a string value emits a path-relative source line
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                 |
      | given | a Bash user "alice" |
    When generating the spec with framework :bash/testing
    Then the output should contain source "$SCRIPT_DIR/../../../lib/sample_app.sh"

  Scenario: scenario-setup! contributions appear inside each generated function
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                 |
      | given | a Bash user "alice" |
    When generating the spec with framework :bash/testing
    Then the output should contain "  subject_new"
