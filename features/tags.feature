Feature: Tags

  Scenarios can be tagged for filtering. Tags are preserved
  in the IR and used to select which scenarios are generated.

  Scenario: Tags are captured in the IR
    Given a feature file containing:
      """
      Feature: Tagged

        @smoke
        Scenario: Fast test
          Given something

        @slow
        Scenario: Slow test
          Given something else

        Scenario: Untagged
          Given another thing
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Tagged"
       :scenarios [{:scenario "Fast test"
                    :tags ["smoke"]
                    :steps [{:type :given :text "something"}]}
                   {:scenario "Slow test"
                    :tags ["slow"]
                    :steps [{:type :given :text "something else"}]}
                   {:scenario "Untagged"
                    :steps [{:type :given :text "another thing"}]}]}
      """

  Scenario: Multiple tags on a scenario
    Given a feature file containing:
      """
      Feature: Multi-tag

        @smoke @fast
        Scenario: Quick check
          Given something
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Multi-tag"
       :scenarios [{:scenario "Quick check"
                    :tags ["smoke" "fast"]
                    :steps [{:type :given :text "something"}]}]}
      """

  Scenario: Feature-level tags apply to all scenarios
    Given a feature file containing:
      """
      @api
      Feature: API tests

        Scenario: Endpoint A
          Given something

        @slow
        Scenario: Endpoint B
          Given something else
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "API tests"
       :tags ["api"]
       :scenarios [{:scenario "Endpoint A"
                    :tags ["api"]
                    :steps [{:type :given :text "something"}]}
                   {:scenario "Endpoint B"
                    :tags ["api" "slow"]
                    :steps [{:type :given :text "something else"}]}]}
      """

  Scenario: Include by tag
    Given a feature with tagged scenarios:
      | scenario    | tags       |
      | Fast test   | smoke      |
      | Slow test   | slow       |
      | Both        | smoke,slow |
      | Untagged    |            |
    When generating with include tags "smoke"
    Then the generated scenarios should be "Fast test, Both"

  Scenario: Exclude by tag
    Given a feature with tagged scenarios:
      | scenario    | tags       |
      | Fast test   | smoke      |
      | Slow test   | slow       |
      | Both        | smoke,slow |
      | Untagged    |            |
    When generating with exclude tags "slow"
    Then the generated scenarios should be "Fast test, Untagged"

  Scenario: WIP is just a tag
    Given a feature file containing:
      """
      Feature: WIP as tag

        @wip
        Scenario: Not ready
          Given something
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "WIP as tag"
       :scenarios [{:scenario "Not ready"
                    :tags ["wip"]
                    :steps [{:type :given :text "something"}]}]}
      """
