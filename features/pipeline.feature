Feature: Pipeline

  The pipeline orchestrates parsing and code generation as a
  two-stage flow, writing intermediate and final artifacts to
  the target directory.

  Scenario: Parse stage writes EDN IR files
    Given a features directory containing:
      | file              |
      | login.feature     |
      | checkout.feature  |
    And the feature "login.feature" contains:
      """
      Feature: Login

        Scenario: Valid credentials
          Given a valid user
          When the user logs in
          Then the user sees the dashboard
      """
    And the feature "checkout.feature" contains:
      """
      Feature: Checkout

        Scenario: Empty cart
          Given an empty cart
          When the user checks out
          Then an error is shown
      """
    When the parse stage runs
    Then "target/gherclj/edn/login.edn" should exist
    And "target/gherclj/edn/checkout.edn" should exist
    And "target/gherclj/edn/login.edn" should contain IR:
      """
      {:feature "Login"
       :source "login.feature"
       :scenarios [{:scenario "Valid credentials"
                    :steps [{:type :given :text "a valid user"}
                            {:type :when :text "the user logs in"}
                            {:type :then :text "the user sees the dashboard"}]}]}
      """

  Scenario: Pipeline is silent by default
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a user
      """
    When the full pipeline runs with framework :speclj
    Then the output should be empty

  Scenario: Parse stage reports progress when verbose
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a user

        Scenario: Logout
          Given a session
      """
    When the parse stage runs with :verbose
    Then the output should contain "Parsing auth.feature -> target/gherclj/edn/auth.edn"
    And the output should contain "2 scenarios parsed"

  Scenario: Generate stage writes spec files from EDN
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    And the parse stage has run
    When the generate stage runs with framework :speclj
    Then "target/gherclj/generated/auth_spec.clj" should exist
    And "target/gherclj/generated/auth_spec.clj" should contain "(describe"

  Scenario: Generate stage reports progress when verbose
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    And the parse stage has run
    When the generate stage runs with framework :speclj and :verbose
    Then the output should contain "Generating target/gherclj/generated/auth_spec.clj from auth.edn"
    And the output should contain "1 scenarios generated"

  Scenario: Full pipeline runs both stages
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/edn/auth.edn" should exist
    And "target/gherclj/generated/auth_spec.clj" should exist

  Scenario: Framework selection controls generated output
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/generated/auth_spec.clj" should contain "describe"
    And "target/gherclj/generated/auth_spec.clj" should contain "speclj.core"
    When the full pipeline runs with framework :clojure.test
    Then "target/gherclj/generated/auth_test.clj" should contain "deftest"
    And "target/gherclj/generated/auth_test.clj" should contain "clojure.test"

  Scenario: clojure.test generates _test files
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a valid user
      """
    When the full pipeline runs with framework :clojure.test
    Then "target/gherclj/edn/auth.edn" should exist
    And "target/gherclj/generated/auth_test.clj" should exist

  Scenario: WIP scenarios are parsed but not generated
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        @wip
        Scenario: Not ready
          Given something

        Scenario: Ready
          Given a valid user
      """
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/edn/auth.edn" should contain IR with 2 scenarios
    And "target/gherclj/generated/auth_spec.clj" should contain "Ready"
    And "target/gherclj/generated/auth_spec.clj" should not contain "Not ready"
