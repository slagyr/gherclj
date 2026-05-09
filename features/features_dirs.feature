Feature: Multi-root features directories

  gherclj accepts multiple -f flags and supports glob patterns, letting
  projects compose feature files from a base directory and per-module
  directories. When more than one root contributes features, each IR's
  :source is prefixed with its root path so cross-root collisions never
  overwrite each other in generated output. With a single root, :source
  remains bare (back-compat). The legacy singular :features-dir config
  key is removed; configs must use :features-dirs (a list of strings).

  # --- Multi-root semantics ---

  Scenario: Multi-root pipeline qualifies each :source with its root
    Given features directories containing:
      | root        | file               |
      | features    | auth/login.feature |
      | modules/sso | auth/login.feature |
    And the feature "features/auth/login.feature" contains:
      """
      Feature: Auth base

        Scenario: Login
          Given a user
      """
    And the feature "modules/sso/auth/login.feature" contains:
      """
      Feature: Auth SSO

        Scenario: SSO login
          Given an SSO user
      """
    When the full pipeline runs with framework :clojure/speclj
    Then "target/gherclj/generated/features/auth/login_spec.clj" should exist and:
      | check    | value     |
      | contains | Auth base |
      | contains | Login     |
    And "target/gherclj/generated/modules/sso/auth/login_spec.clj" should exist and:
      | check    | value     |
      | contains | Auth SSO  |
      | contains | SSO login |

  # --- Glob expansion ---

  Scenario: Glob -f expands to all matching directories
    Given features directories containing:
      | root            | file            |
      | modules/sso     | login.feature   |
      | modules/billing | invoice.feature |
    And the feature "modules/sso/login.feature" contains:
      """
      Feature: SSO

        Scenario: Login
          Given an SSO user
      """
    And the feature "modules/billing/invoice.feature" contains:
      """
      Feature: Billing

        Scenario: Issue invoice
          Given an invoice
      """
    When the full pipeline runs with options:
      | option        | value           |
      | framework     | :clojure/speclj |
      | features-dirs | modules/*       |
    Then "target/gherclj/generated/modules/sso/login_spec.clj" should exist
    And "target/gherclj/generated/modules/billing/invoice_spec.clj" should exist

  Scenario: CLI -f with glob pattern expands to matching directories
    Given features directories containing:
      | root            | file            |
      | modules/sso     | login.feature   |
      | modules/billing | invoice.feature |
    And the feature "modules/sso/login.feature" contains:
      """
      Feature: SSO

        Scenario: Login
          Given a user
      """
    And the feature "modules/billing/invoice.feature" contains:
      """
      Feature: Billing

        Scenario: Invoice
          Given an invoice
      """
    When running gherclj with "-f modules/* --verbose"
    Then the exit code should be zero
    And "target/gherclj/generated/modules/sso/login_spec.clj" should exist
    And "target/gherclj/generated/modules/billing/invoice_spec.clj" should exist

  Scenario: Glob patterns expand at config-resolve time
    Given features directories containing:
      | root            | file            |
      | modules/sso     | login.feature   |
      | modules/billing | invoice.feature |
    When running gherclj with "-f modules/* --verbose"
    Then the resolved config should contain:
      """
      {:features-dirs ["modules/billing" "modules/sso"]}
      """

  Scenario: Glob with no matching directories errors with the pattern
    Given a features directory containing:
      | file      |
      | x.feature |
    When the full pipeline runs with options:
      | option        | value           |
      | framework     | :clojure/speclj |
      | features-dirs | nonexistent/*   |
    Then the run should fail
    And the error output should mention "nonexistent/*"
    And the error output should mention "no directories matched"

  # --- Selector resolution ---

  Scenario: Bare selector resolves to its single matching root
    Given features directories containing:
      | root        | file          |
      | features    | core.feature  |
      | modules/sso | login.feature |
    And the feature "features/core.feature" contains:
      """
      Feature: Core

        Scenario: Bootstrap
          Given a user
      """
    And the feature "modules/sso/login.feature" contains:
      """
      Feature: SSO

        Scenario: Login
          Given an SSO user
      """
    When the full pipeline runs with framework :clojure/speclj and locations:
      | selector      |
      | login.feature |
    Then "target/gherclj/generated/modules/sso/login_spec.clj" should exist and:
      | check    | value |
      | contains | Login |
    And "target/gherclj/generated/features/core_spec.clj" should not exist

  Scenario: Qualified selector with root prefix picks the qualified root
    Given features directories containing:
      | root      | file           |
      | features  | shared.feature |
      | overrides | shared.feature |
    And the feature "features/shared.feature" contains:
      """
      Feature: Shared base

        Scenario: Base
          Given baseline
      """
    And the feature "overrides/shared.feature" contains:
      """
      Feature: Shared override

        Scenario: Override
          Given an override
      """
    When the full pipeline runs with framework :clojure/speclj and locations:
      | selector                 |
      | overrides/shared.feature |
    Then "target/gherclj/generated/overrides/shared_spec.clj" should exist and:
      | check    | value           |
      | contains | Shared override |
    And "target/gherclj/generated/features/shared_spec.clj" should not exist

  Scenario: Bare selector that matches multiple roots is ambiguous
    Given features directories containing:
      | root        | file         |
      | features    | auth.feature |
      | modules/sso | auth.feature |
    And the feature "features/auth.feature" contains:
      """
      Feature: Auth base

        Scenario: Login
          Given a user
      """
    And the feature "modules/sso/auth.feature" contains:
      """
      Feature: Auth SSO

        Scenario: Login
          Given a user
      """
    When running gherclj with "-f features -f modules/sso auth.feature"
    Then the exit code should be non-zero
    And the error output should mention "Ambiguous selector"
    And the error output should mention "auth.feature"
    And the error output should mention "features/auth.feature"
    And the error output should mention "modules/sso/auth.feature"
    And the error output should mention "Qualify with the root path"

  Scenario: Selector that exists under no root errors with the path
    Given features directories containing:
      | root        | file          |
      | features    | core.feature  |
      | modules/sso | login.feature |
    And the feature "features/core.feature" contains:
      """
      Feature: Core

        Scenario: Bootstrap
          Given a user
      """
    And the feature "modules/sso/login.feature" contains:
      """
      Feature: SSO

        Scenario: Login
          Given an SSO user
      """
    When running gherclj with "-f features -f modules/sso bogus.feature"
    Then the exit code should be non-zero
    And the error output should mention "Feature file not found"
    And the error output should mention "bogus.feature"

  # --- Config schema ---

  Scenario: Config :features-dirs accepts a list of roots
    Given a config file:
      """
      {:features-dirs ["features" "modules/sso"]
       :framework :clojure/speclj}
      """
    When running gherclj with "--verbose"
    Then the resolved config should contain:
      """
      {:features-dirs ["features" "modules/sso"]
       :framework :clojure/speclj}
      """

  Scenario: Legacy :features-dir is rejected with a migration message
    Given a config file:
      """
      {:features-dir "features"
       :framework :clojure/speclj}
      """
    When running gherclj with "--verbose"
    Then the exit code should be non-zero
    And the error output should mention ":features-dir is no longer supported"
    And the error output should mention ":features-dirs"

  Scenario: CLI -f replaces config :features-dirs entirely
    Given a config file:
      """
      {:features-dirs ["features" "modules/*"]
       :framework :clojure/speclj}
      """
    When running gherclj with "-f experiments --verbose"
    Then the resolved config should contain:
      """
      {:features-dirs ["experiments"]}
      """

  Scenario: Empty :features-dirs is rejected
    Given a config file:
      """
      {:features-dirs []
       :framework :clojure/speclj}
      """
    When running gherclj with "--verbose"
    Then the exit code should be non-zero
    And the error output should mention "no feature roots resolved"
