Feature: Failure provenance

  When a scenario fails, gherclj reports which Gherkin step failed,
  the feature file location, and for data tables which cell mismatched.
  Generated specs carry that provenance so humans and agents can jump
  straight from a failure to the .feature line that broke.

  # --- IR line numbers (parser) ---

  Scenario: Parsed scenarios and steps include 1-based line numbers
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
                    :line 3
                    :steps [{:type :given :text "a valid user" :line 4}
                            {:type :when :text "the user logs in" :line 5}
                            {:type :then :text "the user sees the dashboard" :line 6}]}]}
      """

  Scenario: Parsed data tables include header and row line numbers
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
                    :line 3
                    :steps [{:type :given
                             :text "users:"
                             :line 4
                             :table {:headers ["name" "role"]
                                     :rows [["alice" "admin"]
                                            ["bob" "guest"]]
                                     :header-line 5
                                     :row-lines [6 7]}}]}]}
      """

  Scenario: Background steps include line numbers
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
       :background {:steps [{:type :given :text "a common precondition" :line 4}
                            {:type :and :text "another precondition" :line 5}]}
       :scenarios [{:scenario "Uses background"
                    :line 7
                    :steps [{:type :when :text "an action" :line 8}]}]}
      """

  # --- Generated code annotations ---

  Scenario: Generated speclj annotates each step with feature path and line
    Given a feature file containing:
      """
      Feature: Login

        Scenario: Valid credentials
          Given a user "alice"
          When the user logs in
          Then the response should be 200
      """
    When the feature is parsed
    And the parsed IR is sourced as "auth.feature"
    And generating a speclj spec from the parsed IR using step namespace "gherclj.sample.app-steps"
    Then the output should contain ";; Given a user"
    And the output should contain "(auth.feature:4)"
    And the output should contain ";; When the user logs in  (auth.feature:5)"
    And the output should contain ";; Then the response should be 200  (auth.feature:6)"

  Scenario: Generated speclj wraps each step in with-step* for runtime context
    Given a feature file containing:
      """
      Feature: Login

        Scenario: Valid credentials
          Given a user "alice"
          When the user logs in
          Then the response should be 200
      """
    When the feature is parsed
    And the parsed IR is sourced as "auth.feature"
    And generating a speclj spec from the parsed IR using step namespace "gherclj.sample.app-steps"
    Then the output should contain "(g/with-step*"
    And the output should contain "auth.feature"
    And the output should contain "app-steps/create-adventurer"
    And the output should contain "app-steps/enter-the-realm"
    And the output should contain "app-steps/verify-outcome"
    And the generated with-step wrappers should reference lines 4, 5, and 6

  Scenario: Provenance comments for table steps include each row's feature line
    Given a table step labeled "Given users:" at line 4 of "users.feature":
      | name  | role  |
      | alice | admin |
      | bob   | guest |
    And the table header is at line 5 and data rows at lines 6 and 7
    When formatting provenance comments for framework :clojure/speclj
    Then the provenance comments should contain ";; Given users:  (users.feature:4)"
    And the provenance comments should contain ";;   | name | role |  (users.feature:5)"
    And the provenance comments should contain ";;   | alice | admin |  (users.feature:6)"
    And the provenance comments should contain ";;   | bob | guest |  (users.feature:7)"

  Scenario: Non-Clojure frameworks use language-appropriate comment syntax
    Given a plain step labeled "Given a user" at line 3 of "login.feature"
    When formatting provenance comments for framework :ruby/rspec
    Then the provenance comments should contain "# Given a user  (login.feature:3)"
    When formatting provenance comments for framework :javascript/node-test
    Then the provenance comments should contain "// Given a user  (login.feature:3)"

  # --- Runtime failure messages ---

  Scenario: with-step* prefixes assertion failures with step text and location
    When a step "Then the status is 200" at "features/auth.feature" line 12 fails with:
      """
      Expected: 200
           got: 401
      """
    Then the failure message should include "Then the status is 200"
    And the failure message should include "features/auth.feature:12"
    And the failure message should include "Expected: 200"
    And the failure message should include "got: 401"

  Scenario: should-table= reports row, column, and feature line on cell mismatch
    Given an expected table with row lines 14 and 15:
      | name  | role  |
      | alice | admin |
      | bob   | guest |
    And an actual table:
      | name  | role   |
      | alice | admin  |
      | bob   | member |
    When comparing the tables with should-table=
    Then the failure message should include "row 2"
    And the failure message should include "role"
    And the failure message should include "line 15"
    And the failure message should include "guest"
    And the failure message should include "member"

  Scenario: each-row names the failing row and its feature line
    Given a table with row lines 14 and 15:
      | name  | role  |
      | alice | admin |
      | bob   | guest |
    When each-row asserts role "admin" for every row
    Then the failure message should include "row 2"
    And the failure message should include "line 15"
    And the failure message should include "admin"
    And the failure message should include "guest"

  Scenario: Step context and table context compose in one failure message
    Given an expected table with row lines 14 and 15:
      | name  | role  |
      | alice | admin |
      | bob   | guest |
    And an actual table:
      | name  | role   |
      | alice | admin  |
      | bob   | member |
    When comparing the tables with should-table= inside step "Then the users should be:" at "features/auth.feature" line 12
    Then the failure message should include "Then the users should be:"
    And the failure message should include "features/auth.feature:12"
    And the failure message should include "row 2"
    And the failure message should include "role"
    And the failure message should include "line 15"
