Feature: Tag CLI flag

  Tags can be specified on the command line with -t. Prefix
  with ~ to exclude. Repeatable for multiple tags.

  Scenario: Include by tag
    When running gherclj with "-t smoke"
    Then the resolved config should contain:
      """
      {:include-tags ["smoke"]}
      """

  Scenario: Exclude by tag with tilde prefix
    When running gherclj with "-t ~slow"
    Then the resolved config should contain:
      """
      {:exclude-tags ["slow"]}
      """

  Scenario: Multiple tags
    When running gherclj with "-t smoke -t ~slow -t ~wip"
    Then the resolved config should contain:
      """
      {:include-tags ["smoke"]
       :exclude-tags ["slow" "wip"]}
      """

  Scenario: Updated help shows new flags
    When running gherclj with "--help"
    Then the output should contain "-t, --tag TAG"
    And the output should contain "-F, --framework FRAMEWORK"
