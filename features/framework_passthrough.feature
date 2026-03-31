Feature: Framework passthrough options

  Framework-specific options can be passed to the test runner
  via the CLI (after --) or via :framework-opts in config.

  Scenario: Pass options to speclj via double-dash
    When running gherclj with "-t speclj -- -c spec/myapp -s src --no-color"
    Then the framework should receive options ["-c" "spec/myapp" "-s" "src" "--no-color"]

  Scenario: Pass options via config
    Given a config:
      """
      {:test-framework :speclj
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
      {:test-framework :speclj
       :framework-opts ["-c" "spec/myapp"]}
      """
    When running gherclj with "-- -c spec/other -s src"
    Then the framework should receive options ["-c" "spec/other" "-s" "src"]

  Scenario: No passthrough options by default
    Given a config:
      """
      {:test-framework :speclj}
      """
    When the config is resolved
    Then the resolved config should contain:
      """
      {:framework-opts []}
      """
