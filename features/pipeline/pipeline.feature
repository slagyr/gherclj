@slow
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

  @smoke
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

  @wip
  Scenario: Generated speclj output can be verified with grouped assertions
    Given a features directory containing:
      | file         |
      | auth.feature |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a user "alice" with role "admin"
          When the user logs in
          Then the response status should be 200
      """
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/generated/auth_spec.clj" should exist and:
      | check        | value                          |
      | contains     | describe "Auth"              |
      | contains     | pipeline-spec/setup-user      |
      | contains     | pipeline-spec/user-logs-in    |
      | contains     | pipeline-spec/response-status |
      | not-contains | pending                       |

  @wip
  Scenario: Grouped assertions can verify filtered generated output
    Given a features directory containing:
      | file            |
      | logging.feature |
    And the feature "logging.feature" contains:
      """
      Feature: Logging

        @slow
        Scenario: Slow logging check
          Given a user "alice"
          When the user logs in
          Then the response should be 200

        @smoke
        Scenario: Fast logging check
          Given a user "bob"
          When the user logs in
          Then the response should be 200
      """
    When the full pipeline runs with framework :speclj and tags:
      | tag   |
      | smoke |
    Then "target/gherclj/generated/logging_spec.clj" should exist and:
      | check        | value              |
      | contains     | Fast logging check |
      | not-contains | Slow logging check |

  @wip
  Scenario: Full pipeline resolves globbed step namespaces from classpath roots
    Given a features directory containing:
      | file          |
      | auth.feature  |
    And the feature "auth.feature" contains:
      """
      Feature: Auth

        Scenario: Login
          Given a user "alice" with role "admin"
          When the user logs in
          Then the response status should be 200
      """
    And step namespaces include pattern "gherclj.pipeline-*"
    When the full pipeline runs with framework :speclj
    Then "target/gherclj/generated/auth_spec.clj" should exist and:
      | check        | value                         |
      | contains     | pipeline-spec/setup-user      |
      | contains     | pipeline-spec/user-logs-in    |
      | contains     | pipeline-spec/response-status |
      | not-contains | pending                       |

  Scenario: WIP scenarios are parsed and generated when unfiltered
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
    And "target/gherclj/generated/auth_spec.clj" should contain "Not ready"

  Scenario: Excluding all scenarios omits the generated Speclj file
    Given a features directory containing:
      | file             |
      | logging.feature  |
    And the feature "logging.feature" contains:
      """
      @slow
      Feature: Logging

        Scenario: Slow logging check
          Given a user "alice"
          When the user logs in
          Then the response should be 200
      """
    When the full pipeline runs with framework :speclj and tags:
      | tag   |
      | ~slow |
    Then "target/gherclj/generated/logging_spec.clj" should not exist

  Scenario: Keeping one matching scenario still generates the Speclj file
    Given a features directory containing:
      | file             |
      | logging.feature  |
    And the feature "logging.feature" contains:
      """
      Feature: Logging

        @slow
        Scenario: Slow logging check
          Given a user "alice"
          When the user logs in
          Then the response should be 200

        @smoke
        Scenario: Fast logging check
          Given a user "bob"
          When the user logs in
          Then the response should be 200
      """
    When the full pipeline runs with framework :speclj and tags:
      | tag   |
      | smoke |
    Then "target/gherclj/generated/logging_spec.clj" should exist
    And "target/gherclj/generated/logging_spec.clj" should contain "Fast logging check"
    And "target/gherclj/generated/logging_spec.clj" should not contain "Slow logging check"
