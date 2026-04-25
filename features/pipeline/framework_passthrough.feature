Feature: Framework passthrough options

  Framework-specific options can be passed to the test runner
  via the CLI (after --) or via :framework-opts in config.

  Scenario: Pass options to speclj via double-dash
    When running gherclj with "-t speclj -- -c spec/myapp -s src --no-color"
    Then the framework should receive options ["-c" "spec/myapp" "-s" "src" "--no-color"]

  Scenario: Pass options via config
    Given a config:
      """
      {:framework :clojure/speclj
       :framework-opts ["-c" "spec/myapp" "-s" "src"]}
      """
    When the config is resolved
    Then the resolved config should contain:
      """
      {:framework-opts ["-c" "spec/myapp" "-s" "src"]}
      """

  Scenario: CLI options override config options
    Given a config:
      """
      {:framework :clojure/speclj
       :framework-opts ["-c" "spec/myapp"]}
      """
    When running gherclj with "-- -c spec/other -s src"
    Then the framework should receive options ["-c" "spec/other" "-s" "src"]

  Scenario: No passthrough options by default
    Given a config:
      """
      {:framework :clojure/speclj}
      """
    When the config is resolved
    Then the resolved config should contain:
      """
      {:framework-opts []}
      """

  Scenario: Speclj passthrough options are appended to generated spec defaults
    Given generated specs in "target/gherclj/generated"
    When speclj runs with framework options ["-f" "documentation" "-c" "-P"]
    Then speclj should receive args ["-c" "target/gherclj/generated" "-s" "src" "-f" "documentation" "-c" "-P"]
