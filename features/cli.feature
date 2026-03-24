@wip
Feature: CLI

  gherclj can be invoked from the command line. CLI flags
  override config file values which override defaults.

  Scenario: Display usage message
    When running gherclj with "--help"
    Then the output should contain "Usage: gherclj [options]"
    And the output should contain "-f, --features-dir DIR"
    And the output should contain "-e, --edn-dir DIR"
    And the output should contain "-o, --output-dir DIR"
    And the output should contain "-s, --step-namespaces NS"
    And the output should contain "-t, --test-framework FRAMEWORK"
    And the output should contain "-v, --verbose"
    And the output should contain "-h, --help"

  Scenario: Short flag for help
    When running gherclj with "-h"
    Then the output should contain "Usage: gherclj [options]"

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
    When running gherclj with "-t clojure.test -v"
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

  Scenario: Unknown flag is rejected
    When running gherclj with "--turbo-mode"
    Then the output should contain "Unknown option"
    And the output should contain "turbo-mode"
