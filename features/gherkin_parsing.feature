@wip
Feature: Gherkin parsing

  The parser converts .feature file text into an EDN intermediate
  representation that drives code generation.

  Scenario: Parse a minimal feature
    Given a feature file containing:
      """
      Feature: Login

        Scenario: Successful login
          Given a valid user
          When the user logs in
          Then the user sees the dashboard
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Login"
       :scenarios [{:scenario "Successful login"
                    :steps [{:type :given :text "a valid user"}
                            {:type :when :text "the user logs in"}
                            {:type :then :text "the user sees the dashboard"}]}]}
      """

  Scenario: And and But preserve their keyword type
    Given a feature file containing:
      """
      Feature: Continuations

        Scenario: Mixed
          Given first condition
          And second condition
          But not third
          When an action
          Then a result
          And another result
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Continuations"
       :scenarios [{:scenario "Mixed"
                    :steps [{:type :given :text "first condition"}
                            {:type :and :text "second condition"}
                            {:type :but :text "not third"}
                            {:type :when :text "an action"}
                            {:type :then :text "a result"}
                            {:type :and :text "another result"}]}]}
      """

  Scenario: Parse multiple scenarios
    Given a feature file containing:
      """
      Feature: Multi

        Scenario: First
          Given setup one

        Scenario: Second
          Given setup two
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Multi"
       :scenarios [{:scenario "First"
                    :steps [{:type :given :text "setup one"}]}
                   {:scenario "Second"
                    :steps [{:type :given :text "setup two"}]}]}
      """

  Scenario: Parse a Background section
    Given a feature file containing:
      """
      Feature: With background

        Background:
          Given a common precondition
          And another precondition

        Scenario: Uses background
          When an action
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "With background"
       :background {:steps [{:type :given :text "a common precondition"}
                            {:type :and :text "another precondition"}]}
       :scenarios [{:scenario "Uses background"
                    :steps [{:type :when :text "an action"}]}]}
      """

  Scenario: Parse a step with a data table
    Given a feature file containing:
      """
      Feature: Tables

        Scenario: With table
          Given users:
            | name  | role  |
            | alice | admin |
            | bob   | guest |
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Tables"
       :scenarios [{:scenario "With table"
                    :steps [{:type :given
                             :text "users:"
                             :table {:headers ["name" "role"]
                                     :rows [["alice" "admin"]
                                            ["bob" "guest"]]}}]}]}
      """

  Scenario: WIP-tagged scenarios are marked
    Given a feature file containing:
      """
      Feature: WIP

        @wip
        Scenario: Not ready
          Given something

        Scenario: Ready
          Given something else
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "WIP"
       :scenarios [{:scenario "Not ready"
                    :wip true
                    :steps [{:type :given :text "something"}]}
                   {:scenario "Ready"
                    :steps [{:type :given :text "something else"}]}]}
      """

  Scenario: Feature description is captured
    Given a feature file containing:
      """
      Feature: Described

        This feature has a multi-line
        description block.

        Scenario: Only one
          Given something
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Described"
       :description "This feature has a multi-line\ndescription block."
       :scenarios [{:scenario "Only one"
                    :steps [{:type :given :text "something"}]}]}
      """
