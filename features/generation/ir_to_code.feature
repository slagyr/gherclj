Feature: IR to code generation

  The generator converts parsed feature IR into executable spec files.
  It resolves steps to qualified function calls and delegates formatting
  to framework-specific multimethods.

  @smoke
  Scenario: Generate a speclj spec
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                        |
      | given | a user "alice"              |
      | when  | the user logs in            |
      | then  | the response should be 200  |
    When generating the spec with framework :speclj
    Then the generated code should be:
      """
      (ns login-spec
        (:require [speclj.core :refer :all]
                  [gherclj.core :as g]
                  [gherclj.features.steps.sample-app :as sample-app]))

      (describe "Login"

        (before-all (g/run-before-feature-hooks!))
        (before (g/reset!) (g/run-before-scenario-hooks!))
        (after (g/run-after-scenario-hooks!))
        (after-all (g/run-after-feature-hooks!))

        (it "Valid credentials"
          (sample-app/create-user "alice")
          (sample-app/user-logs-in)
          (sample-app/response-should-be 200)))
      """

  Scenario: Generate a clojure.test spec
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                        |
      | given | a user "alice"              |
      | when  | the user logs in            |
      | then  | the response should be 200  |
    When generating the spec with framework :clojure.test
    Then the generated code should be:
      """
      (ns login-test
        (:require [clojure.test :refer :all]
                  [gherclj.core :as g]
                  [gherclj.features.steps.sample-app :as sample-app]))

      (defn ^:private feature-fixture [f]
        (g/run-before-feature-hooks!)
        (try
          (f)
          (finally
            (g/run-after-feature-hooks!))))

      (defn ^:private scenario-fixture [f]
        (g/reset!)
        (g/run-before-scenario-hooks!)
        (try
          (f)
          (finally
            (g/run-after-scenario-hooks!))))

      (use-fixtures :once feature-fixture)
      (use-fixtures :each scenario-fixture)

      (deftest valid-credentials
        (testing "Valid credentials"
          (sample-app/create-user "alice")
          (sample-app/user-logs-in)
          (sample-app/response-should-be 200)))
      """

  Scenario: clojure.test with multiple scenarios
    Given a feature named "Login" from source "login.feature"
    And a scenario "Valid credentials" with steps:
      | type  | text                       |
      | given | a user "alice"             |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    And a scenario "Invalid credentials" with steps:
      | type  | text                       |
      | given | a user "nobody"            |
      | when  | the user logs in           |
      | then  | the response should be 401 |
    When generating the spec with framework :clojure.test
    Then the generated code should be:
      """
      (ns login-test
        (:require [clojure.test :refer :all]
                  [gherclj.core :as g]
                  [gherclj.features.steps.sample-app :as sample-app]))

      (defn ^:private feature-fixture [f]
        (g/run-before-feature-hooks!)
        (try
          (f)
          (finally
            (g/run-after-feature-hooks!))))

      (defn ^:private scenario-fixture [f]
        (g/reset!)
        (g/run-before-scenario-hooks!)
        (try
          (f)
          (finally
            (g/run-after-scenario-hooks!))))

      (use-fixtures :once feature-fixture)
      (use-fixtures :each scenario-fixture)

      (deftest valid-credentials
        (testing "Valid credentials"
          (sample-app/create-user "alice")
          (sample-app/user-logs-in)
          (sample-app/response-should-be 200)))

      (deftest invalid-credentials
        (testing "Invalid credentials"
          (sample-app/create-user "nobody")
          (sample-app/user-logs-in)
          (sample-app/response-should-be 401)))
      """

  Scenario: clojure.test with background
    Given a feature named "Login" from source "login.feature"
    And a background with steps:
      | type  | text           |
      | given | a user "alice" |
    And a scenario "Check response" with steps:
      | type  | text                       |
      | when  | the user logs in           |
      | then  | the response should be 200 |
    When generating the spec with framework :clojure.test
    Then the generated code should be:
      """
      (ns login-test
        (:require [clojure.test :refer :all]
                  [gherclj.core :as g]
                  [gherclj.features.steps.sample-app :as sample-app]))

      (defn ^:private feature-fixture [f]
        (g/run-before-feature-hooks!)
        (try
          (f)
          (finally
            (g/run-after-feature-hooks!))))

      (defn ^:private scenario-fixture [f]
        (g/reset!)
        (g/run-before-scenario-hooks!)
        (try
          (f)
          (finally
            (g/run-after-scenario-hooks!))))

      (use-fixtures :once feature-fixture)
      (use-fixtures :each scenario-fixture)

      (deftest check-response
        (testing "Check response"
          (sample-app/create-user "alice")
          (sample-app/user-logs-in)
          (sample-app/response-should-be 200)))
      """

  Scenario: clojure.test pending scenario
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                  |
      | given | something undefined   |
      | when  | doing unknown things  |
    When generating the spec with framework :clojure.test
    Then the generated code should be:
      """
      (ns login-test
        (:require [clojure.test :refer :all]
                  [gherclj.core :as g]))

      (defn ^:private feature-fixture [f]
        (g/run-before-feature-hooks!)
        (try
          (f)
          (finally
            (g/run-after-feature-hooks!))))

      (defn ^:private scenario-fixture [f]
        (g/reset!)
        (g/run-before-scenario-hooks!)
        (try
          (f)
          (finally
            (g/run-after-scenario-hooks!))))

      (use-fixtures :once feature-fixture)
      (use-fixtures :each scenario-fixture)

      (deftest not-implemented
        (testing "Not implemented"
          ;; TODO: not yet implemented
          ))
      """

  Scenario: Background steps are included in every speclj scenario
    Given a feature named "Login" from source "login.feature"
    And a background with steps:
      | type  | text           |
      | given | a user "alice" |
    And a scenario "First" with steps:
      | type  | text             |
      | when  | the user logs in |
    And a scenario "Second" with steps:
      | type  | text                       |
      | then  | the response should be 200 |
    When generating the spec with framework :speclj
    Then the output should contain "(sample-app/create-user"
    And the output should contain "(sample-app/user-logs-in)"
    And the output should contain "(sample-app/response-should-be 200)"

  Scenario: State is reset before each scenario
    Given a feature named "Login" from source "login.feature"
    And a scenario "First" with steps:
      | type  | text             |
      | when  | the user logs in |
    And a scenario "Second" with steps:
      | type  | text             |
      | when  | the user logs in |
    When generating the spec with framework :speclj
    Then the output should contain "(g/reset!)"

  Scenario: Unrecognized steps generate pending speclj scenarios
    Given a feature named "Login" from source "login.feature"
    And a scenario "Not implemented" with steps:
      | type  | text                  |
      | given | something undefined   |
      | when  | doing unknown things  |
    When generating the spec with framework :speclj
    Then the output should contain "pending"
    And the output should contain ";; given something undefined"

  Scenario: WIP scenarios are excluded from generation
    Given a feature named "Login" from source "login.feature"
    And a scenario "Normal" with steps:
      | type  | text             |
      | when  | the user logs in |
    And a wip scenario "Skipped" with steps:
      | type  | text                |
      | given | something undefined |
    When generating the spec with framework :speclj
    Then the output should contain "Normal"
    And the output should not contain "Skipped"
