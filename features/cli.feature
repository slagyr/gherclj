Feature: CLI

  gherclj can be invoked from the command line. CLI flags
  override config file values which override defaults.

  @smoke
  Scenario: Display usage message
    When running gherclj with "--help"
    Then the output should contain "Gherclj"
    And the output should contain "Gherkin -> test code transducer"
    And the output should contain "Copyright (c) 2026 Micah Martin under The MIT License."
    And the output should contain "-f, --features-dir DIR"
    And the output should contain "-e, --edn-dir DIR"
    And the output should contain "-o, --output-dir DIR"
    And the output should contain "-s, --step-namespaces NS"
    And the output should contain "-t, --tag TAG"
    And the output should contain "Use ~ prefix to exclude tags"
    And the output should contain "-T, --test-framework FRAMEWORK"
    And the output should contain "-v, --verbose"
    And the output should contain "-h, --help"

  Scenario: Short flag for help
    When running gherclj with "-h"
    Then the output should contain "Gherclj"

  Scenario: CLI flags override config file
    Given a config file:
      """
      {:features-dir "features"
       :test-framework :speclj}
      """
    When running gherclj with "--test-framework clojure.test --verbose"
    Then the resolved config should contain:
      """
      {:test-framework :clojure.test
       :verbose true}
      """

  Scenario: Short flags work
    When running gherclj with "-T clojure.test -v"
    Then the resolved config should contain:
      """
      {:test-framework :clojure.test
       :verbose true}
      """

  Scenario: Boolean flag presence means true
    When running gherclj with "--verbose"
    Then the resolved config should contain:
      """
      {:verbose true}
      """

  Scenario: Step namespaces can be specified multiple times
    When running gherclj with "-s myapp.steps.auth -s myapp.steps.cart"
    Then the resolved config should contain:
      """
      {:step-namespaces [myapp.steps.auth myapp.steps.cart]}
      """

  Scenario: Step namespace glob patterns remain strings
    When running gherclj with "-s carnival.dragon_steps.*"
    Then the resolved config should contain:
      """
      {:step-namespaces ["carnival.dragon_steps.*"]}
      """

  Scenario: Concrete step namespaces remain symbols
    When running gherclj with "-s carnival.dragon_steps.fire -s carnival.dragon_steps.treasure"
    Then the resolved config should contain:
      """
      {:step-namespaces [carnival.dragon_steps.fire carnival.dragon_steps.treasure]}
      """

  Scenario: CLI step namespace glob patterns are resolved during the pipeline
    Given a features directory containing:
      | file           |
      | dragon.feature |
    And the feature "dragon.feature" contains:
      """
      Feature: Dragon academy

        Scenario: Wake the librarian dragon
          Given a user "alice" with role "admin"
          When the user logs in
          Then the response status should be 200
      """
    And step namespaces include pattern "gherclj.pipeline-*"
    When running gherclj with "-f features -o target/gherclj/generated -e target/gherclj/edn -T speclj -s gherclj.pipeline-*"
    Then "target/gherclj/generated/dragon_spec.clj" should exist and:
      | check        | value                         |
      | contains     | Wake the librarian dragon     |
      | contains     | pipeline-spec/summon-hero     |
      | contains     | pipeline-spec/enter-the-realm |
      | contains     | pipeline-spec/check-the-gate  |
      | not-contains | pending                       |

  Scenario: Unknown flag is rejected
    When running gherclj with "--turbo-mode"
    Then the output should contain "Unknown option"
    And the output should contain "turbo-mode"

  Scenario: Help mentions the steps subcommand
    When running gherclj with "--help"
    Then the output should contain "gherclj steps"
    And the output should contain "gherclj steps --help"
