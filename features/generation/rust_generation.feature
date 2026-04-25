Feature: Rust rustc --test code generation

  The Rust adapter produces native throwaway Rust test files in target.
  Helper imports declared via helper! become #[path] mod lines; rust/scenario-setup!
  contributions land inside each generated #[test] function; step refs of the form
  subject.method-name render as subject.method_name(args) calls.

  Background:
    Given step namespace "gherclj.sample.rust-app-steps"

  @smoke
  Scenario: Generate a Rust test module with subject method calls
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                         |
      | given | a Rust user "alice"         |
      | when  | the Rust user logs in        |
      | then  | the Rust response should be 200 |
    When generating the spec with framework :rust/rustc-test
    Then the generated code should be:
      """
      // generated from login.feature
      #[path = "../../../lib/sample_app.rs"]
      mod sample_app;

      #[test]
      fn valid_credentials() {
          let mut subject = sample_app::SampleAppSteps::new();
          subject.create_adventurer("alice");
          subject.enter_the_realm();
          subject.verify_outcome(200);
      }
      """

  Scenario: Unrecognized steps generate an ignored Rust test
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :rust/rustc-test
    Then the output should contain "#[ignore = "
    And the output should contain "not yet implemented"

  Scenario: subject.method-name helper-refs render as subject.method_name(args)
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                |
      | given | a Rust user "alice" |
    When generating the spec with framework :rust/rustc-test
    Then the output should contain "subject.create_adventurer("
    And the output should contain "alice"

  Scenario: helper! with a string value emits a path-based module import
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                |
      | given | a Rust user "alice" |
    When generating the spec with framework :rust/rustc-test
    Then the output should contain "sample_app.rs"
    And the output should contain "mod sample_app;"

  Scenario: scenario-setup! contributions appear inside each test function
    Given a feature named "Login" from source "login.feature"
    And a scenario "Login" with steps:
      | type  | text                |
      | given | a Rust user "alice" |
    When generating the spec with framework :rust/rustc-test
    Then the output should contain "let mut subject = sample_app::SampleAppSteps::new();"
