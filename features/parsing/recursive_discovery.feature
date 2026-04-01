@slow
Feature: Recursive feature discovery

  The parser finds .feature files recursively in subdirectories,
  preserving directory structure in output paths.

  Scenario: Discover features in subdirectories
    Given a features directory containing:
      | file                          |
      | session/keys.feature          |
      | cli/cli.feature               |
      | auth.feature                  |
    And the feature "session/keys.feature" contains:
      """
      Feature: Session keys

        Scenario: Generate key
          Given a session
      """
    And the feature "cli/cli.feature" contains:
      """
      Feature: CLI

        Scenario: Help flag
          Given the --help flag
      """
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    When the parse stage runs
    Then "target/gherclj/edn/session/keys.edn" should exist
    And "target/gherclj/edn/cli/cli.edn" should exist
    And "target/gherclj/edn/auth.edn" should exist

  Scenario: Generate specs preserving directory structure
    Given a features directory containing:
      | file                          |
      | session/keys.feature          |
      | auth.feature                  |
    And the feature "session/keys.feature" contains:
      """
      Feature: Session keys

        Scenario: Generate key
          Given a valid user
      """
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/generated/session/keys_spec.clj" should exist
    And "target/gherclj/generated/session/keys_spec.clj" should contain "(ns session.keys-spec"
    And "target/gherclj/generated/auth_spec.clj" should exist
    And "target/gherclj/generated/auth_spec.clj" should contain "(ns auth-spec"

  Scenario: Single run with multiple step namespaces across subdirectories
    Given a features directory containing:
      | file                          |
      | session/keys.feature          |
      | cli/cli.feature               |
    And the feature "session/keys.feature" contains:
      """
      Feature: Session keys

        Scenario: Generate key
          Given a valid user
      """
    And the feature "cli/cli.feature" contains:
      """
      Feature: CLI

        Scenario: Help flag
          Given a valid user
      """
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/generated/session/keys_spec.clj" should exist
    And "target/gherclj/generated/cli/cli_spec.clj" should exist

  Scenario: clojure.test finds specs in subdirectories
    Given a features directory containing:
      | file                          |
      | session/keys.feature          |
      | auth.feature                  |
    And the feature "session/keys.feature" contains:
      """
      Feature: Session keys

        Scenario: Generate key
          Given a valid user
      """
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    When the full pipeline runs with framework :clojure.test
    Then "target/gherclj/generated/session/keys_test.clj" should exist
    And "target/gherclj/generated/session/keys_test.clj" should contain "(ns session.keys-test"
    And "target/gherclj/generated/auth_test.clj" should exist
