Feature: Gherkin Rule and nested Background

  A Feature may group scenarios under Rule blocks. Each Rule can have its
  own Background. Feature-level Background still applies first; then the
  Rule Background; then the scenario steps.

  Scenario: Parse a Rule with scenarios
    Given a feature file containing:
      """
      Feature: Highlander

        Rule: There can be only One

          Scenario: Only One
            Given there is only 1 ninja alive
            Then they will live forever
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Highlander"
       :scenarios [{:scenario "Only One"
                    :rule "There can be only One"
                    :line 5
                    :rule-line 3
                    :steps [{:type :given :text "there is only 1 ninja alive" :line 6}
                            {:type :then :text "they will live forever" :line 7}]}]}
      """

  Scenario: Rule Background is attached to scenarios in that rule
    Given a feature file containing:
      """
      Feature: Overdue tasks

        Rule: Users are notified about overdue tasks

          Background:
            Given I have overdue tasks

          Scenario: First use of the day
            Given I last used the app yesterday
            When I use the app
            Then I am notified about overdue tasks
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Overdue tasks"
       :scenarios [{:scenario "First use of the day"
                    :rule "Users are notified about overdue tasks"
                    :rule-line 3
                    :line 8
                    :rule-background {:steps [{:type :given :text "I have overdue tasks" :line 6}]}
                    :steps [{:type :given :text "I last used the app yesterday" :line 9}
                            {:type :when :text "I use the app" :line 10}
                            {:type :then :text "I am notified about overdue tasks" :line 11}]}]}
      """

  Scenario: Feature Background and Rule Background both appear on the scenario
    Given a feature file containing:
      """
      Feature: Nested backgrounds

        Background:
          Given a global admin

        Rule: Client blogs

          Background:
            Given a client blog

          Scenario: Post to client blog
            When I try to post
            Then it is published
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Nested backgrounds"
       :background {:steps [{:type :given :text "a global admin" :line 4}]}
       :scenarios [{:scenario "Post to client blog"
                    :rule "Client blogs"
                    :rule-line 6
                    :rule-background {:steps [{:type :given :text "a client blog" :line 9}]}
                    :steps [{:type :when :text "I try to post"}
                            {:type :then :text "it is published"}]}]}
      """

  Scenario: Multiple rules isolate their backgrounds
    Given a feature file containing:
      """
      Feature: Two rules

        Rule: First rule

          Background:
            Given first setup

          Scenario: Under first
            When action one

        Rule: Second rule

          Background:
            Given second setup

          Scenario: Under second
            When action two
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Two rules"
       :scenarios [{:scenario "Under first"
                    :rule "First rule"
                    :rule-background {:steps [{:type :given :text "first setup"}]}
                    :steps [{:type :when :text "action one"}]}
                   {:scenario "Under second"
                    :rule "Second rule"
                    :rule-background {:steps [{:type :given :text "second setup"}]}
                    :steps [{:type :when :text "action two"}]}]}
      """

  Scenario: Rule tags merge onto scenarios
    Given a feature file containing:
      """
      Feature: Tagged rules

        @billing
        Rule: Invoices

          @wip
          Scenario: Draft invoice
            Given a draft
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Tagged rules"
       :scenarios [{:scenario "Draft invoice"
                    :rule "Invoices"
                    :tags ["billing" "wip"]
                    :steps [{:type :given :text "a draft"}]}]}
      """

  Scenario: Top-level scenarios before rules stay without a rule
    Given a feature file containing:
      """
      Feature: Mixed

        Scenario: Top level
          Given standalone

        Rule: Grouped

          Scenario: Nested
            Given under a rule
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Mixed"
       :scenarios [{:scenario "Top level"
                    :steps [{:type :given :text "standalone"}]}
                   {:scenario "Nested"
                    :rule "Grouped"
                    :steps [{:type :given :text "under a rule"}]}]}
      """
