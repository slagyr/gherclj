Feature: Scenario Outline

  Scenario Outlines are parameterized scenarios. Each row in
  the Examples table produces a concrete scenario with
  placeholders substituted. Expansion happens at parse time
  so the IR contains only concrete scenarios.

  Scenario: Outline expands to concrete scenarios
    Given a feature file containing:
      """
      Feature: Login

        Scenario Outline: Login with role
          Given a user "<name>" with role "<role>"
          When the user logs in
          Then the response should be <status>

          Examples:
            | name  | role  | status |
            | alice | admin | 200    |
            | bob   | guest | 401    |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Login"
       :scenarios [{:scenario "Login with role — alice, admin, 200"
                    :steps [{:type :given :text "a user \"alice\" with role \"admin\""}
                            {:type :when :text "the user logs in"}
                            {:type :then :text "the response should be 200"}]}
                   {:scenario "Login with role — bob, guest, 401"
                    :steps [{:type :given :text "a user \"bob\" with role \"guest\""}
                            {:type :when :text "the user logs in"}
                            {:type :then :text "the response should be 401"}]}]}
      """

  Scenario: Outline with a single example row
    Given a feature file containing:
      """
      Feature: Single

        Scenario Outline: Check status
          Then the status should be <status>

          Examples:
            | status |
            | active |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Single"
       :scenarios [{:scenario "Check status — active"
                    :steps [{:type :then :text "the status should be active"}]}]}
      """

  Scenario: Outline mixed with regular scenarios
    Given a feature file containing:
      """
      Feature: Mixed

        Scenario: Regular
          Given something

        Scenario Outline: Parameterized
          Given value is <val>

          Examples:
            | val |
            | one |
            | two |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Mixed"
       :scenarios [{:scenario "Regular"
                    :steps [{:type :given :text "something"}]}
                   {:scenario "Parameterized — one"
                    :steps [{:type :given :text "value is one"}]}
                   {:scenario "Parameterized — two"
                    :steps [{:type :given :text "value is two"}]}]}
      """

  Scenario: WIP tag on outline marks all expanded scenarios
    Given a feature file containing:
      """
      Feature: WIP Outline

        @wip
        Scenario Outline: Not ready
          Given value is <val>

          Examples:
            | val |
            | one |
            | two |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "WIP Outline"
       :scenarios [{:scenario "Not ready — one"
                    :tags ["wip"]
                    :steps [{:type :given :text "value is one"}]}
                   {:scenario "Not ready — two"
                    :tags ["wip"]
                    :steps [{:type :given :text "value is two"}]}]}
      """

  Scenario: Placeholders are substituted in a step's data table
    Given a feature file containing:
      """
      Feature: Tables

        Scenario Outline: Check the <part>
          Then the report matches:
            | key     | <column> |
            | <field> | <value>  |

          Examples:
            | part | column | field  | value |
            | hull | actual | status | sound |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Tables"
       :scenarios [{:scenario "Check the <part> — hull, actual, status, sound"
                    :steps [{:type :then
                             :text "the report matches:"
                             :table {:headers ["key" "actual"]
                                     :rows [["status" "sound"]]}}]}]}
      """

  Scenario: Placeholders are substituted in a step's doc string
    Given a feature file containing:
      """
      Feature: Docs

        Scenario Outline: Send <part>
          When the payload is sent:
            \"\"\"
            {"part": "<part>"}
            \"\"\"

          Examples:
            | part |
            | hull |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Docs"
       :scenarios [{:scenario "Send <part> — hull"
                    :steps [{:type :when
                             :text "the payload is sent:"
                             :doc-string "{\"part\": \"hull\"}"}]}]}
      """
