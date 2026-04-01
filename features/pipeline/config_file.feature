Feature: Config file loading

  gherclj reads configuration from a gherclj.edn file.
  It checks the project root first, then the classpath.

  Scenario: Load config from project root
    Given a gherclj.edn file at the project root:
      """
      {:features-dir "my-features"
       :test-framework :clojure.test}
      """
    When the config is loaded
    Then the resolved config should contain:
      """
      {:features-dir "my-features"
       :test-framework :clojure.test}
      """

  Scenario: Load config from classpath
    Given a gherclj.edn file on the classpath:
      """
      {:test-framework :clojure.test}
      """
    When the config is loaded
    Then the resolved config should contain:
      """
      {:test-framework :clojure.test}
      """

  Scenario: Project root takes precedence over classpath
    Given a gherclj.edn file at the project root:
      """
      {:test-framework :speclj}
      """
    And a gherclj.edn file on the classpath:
      """
      {:test-framework :clojure.test}
      """
    When the config is loaded
    Then the resolved config should contain:
      """
      {:test-framework :speclj}
      """

  Scenario: No config file uses defaults
    Given no gherclj.edn file exists
    When the config is loaded
    Then the resolved config should be:
      """
      {:features-dir "features"
       :edn-dir "target/gherclj/edn"
       :output-dir "target/gherclj/generated"
       :step-namespaces []
       :test-framework :speclj
       :verbose false
       :framework-opts []
       :include-tags []
       :exclude-tags ["wip"]}
      """

  Scenario: Config file is validated
    Given a gherclj.edn file at the project root:
      """
      {:turbo-mode true}
      """
    When the config is loaded
    Then the config should be invalid with message "turbo-mode"
