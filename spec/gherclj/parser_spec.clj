(ns gherclj.parser-spec
  (:require [speclj.core :refer :all]
            [gherclj.parser :as parser]))

(describe "Parser"

  (context "basic parsing"

    (it "parses all step keyword types"
      (let [ir (parser/parse-feature
                 (str "Feature: Keywords\n"
                      "\n"
                      "  Scenario: All types\n"
                      "    Given a precondition\n"
                      "    And another precondition\n"
                      "    But not this one\n"
                      "    When an action\n"
                      "    Then a result\n"
                      "    And another result\n"))]
        (should= 6 (count (:steps (first (:scenarios ir)))))
        (should= :given (:type (nth (:steps (first (:scenarios ir))) 0)))
        (should= :and (:type (nth (:steps (first (:scenarios ir))) 1)))
        (should= :but (:type (nth (:steps (first (:scenarios ir))) 2)))
        (should= :when (:type (nth (:steps (first (:scenarios ir))) 3)))
        (should= :then (:type (nth (:steps (first (:scenarios ir))) 4)))
        (should= :and (:type (nth (:steps (first (:scenarios ir))) 5)))))

    (it "parses a data table attached to a step"
      (let [ir (parser/parse-feature
                 (str "Feature: Tables\n"
                      "\n"
                      "  Scenario: With table\n"
                      "    Given users:\n"
                      "      | name  | role  |\n"
                      "      | alice | admin |\n"
                      "      | bob   | guest |\n"))]
        (should= {:headers ["name" "role"]
                  :rows [["alice" "admin"] ["bob" "guest"]]}
                 (:table (first (:steps (first (:scenarios ir))))))))

    (it "parses a background section"
      (let [ir (parser/parse-feature
                 (str "Feature: BG\n"
                      "\n"
                      "  Background:\n"
                      "    Given setup\n"
                      "\n"
                      "  Scenario: Test\n"
                      "    When action\n"))]
        (should-not-be-nil (:background ir))
        (should= 1 (count (:steps (:background ir))))
        (should= "setup" (:text (first (:steps (:background ir)))))))

    (it "parses a feature description"
      (let [ir (parser/parse-feature
                 (str "Feature: Described\n"
                      "\n"
                      "  This is a description.\n"
                      "  Second line.\n"
                      "\n"
                      "  Scenario: Test\n"
                      "    Given something\n"))]
        (should= "This is a description.\nSecond line." (:description ir))))

    (it "parses multiple scenarios"
      (let [ir (parser/parse-feature
                 (str "Feature: Multi\n"
                      "\n"
                      "  Scenario: First\n"
                      "    Given one\n"
                      "\n"
                      "  Scenario: Second\n"
                      "    Given two\n"))]
        (should= 2 (count (:scenarios ir)))
        (should= "First" (:scenario (first (:scenarios ir))))
        (should= "Second" (:scenario (second (:scenarios ir)))))))

  (context "doc-strings"

    (it "attaches a doc-string to a step"
      (let [ir (parser/parse-feature
                 (str "Feature: Docs\n"
                      "\n"
                      "  Scenario: Has doc-string\n"
                      "    Given a feature file containing:\n"
                      "      \"\"\"\n"
                      "      hello world\n"
                      "      \"\"\"\n"
                      "    Then something happens\n"))]
        (should= "hello world"
                 (:doc-string (first (:steps (first (:scenarios ir))))))))

    (it "preserves multi-line doc-string content"
      (let [ir (parser/parse-feature
                 (str "Feature: Docs\n"
                      "\n"
                      "  Scenario: Multi-line\n"
                      "    Given input:\n"
                      "      \"\"\"\n"
                      "      line one\n"
                      "      line two\n"
                      "      line three\n"
                      "      \"\"\"\n"))]
        (should= "line one\nline two\nline three"
                 (:doc-string (first (:steps (first (:scenarios ir))))))))

    (it "does not add doc-string to steps without one"
      (let [ir (parser/parse-feature
                 (str "Feature: No docs\n"
                      "\n"
                      "  Scenario: Plain\n"
                      "    Given something\n"
                      "    Then something else\n"))]
        (should-be-nil (:doc-string (first (:steps (first (:scenarios ir)))))))))

  (context "tags"

    (it "parses scenario tags into :tags vector"
      (let [ir (parser/parse-feature
                 (str "Feature: Tagged\n"
                      "\n"
                      "  @smoke\n"
                      "  Scenario: Fast\n"
                      "    Given something\n"))]
        (should= ["smoke"] (:tags (first (:scenarios ir))))))

    (it "parses multiple tags on a scenario"
      (let [ir (parser/parse-feature
                 (str "Feature: Multi\n"
                      "\n"
                      "  @smoke @fast\n"
                      "  Scenario: Quick\n"
                      "    Given something\n"))]
        (should= ["smoke" "fast"] (:tags (first (:scenarios ir))))))

    (it "does not add :tags to untagged scenarios"
      (let [ir (parser/parse-feature
                 (str "Feature: Plain\n"
                      "\n"
                      "  Scenario: No tags\n"
                      "    Given something\n"))]
        (should-be-nil (:tags (first (:scenarios ir))))))

    (it "parses feature-level tags"
      (let [ir (parser/parse-feature
                 (str "@api\n"
                      "Feature: API\n"
                      "\n"
                      "  Scenario: Test\n"
                      "    Given something\n"))]
        (should= ["api"] (:tags ir))
        (should= ["api"] (:tags (first (:scenarios ir))))))

    (it "merges feature and scenario tags"
      (let [ir (parser/parse-feature
                 (str "@api\n"
                      "Feature: API\n"
                      "\n"
                      "  @slow\n"
                      "  Scenario: Test\n"
                      "    Given something\n"))]
        (should= ["api" "slow"] (:tags (first (:scenarios ir))))))

    (it "uses :tags instead of :wip"
      (let [ir (parser/parse-feature
                 (str "Feature: WIP\n"
                      "\n"
                      "  @wip\n"
                      "  Scenario: Not ready\n"
                      "    Given something\n"))]
        (should= ["wip"] (:tags (first (:scenarios ir))))
        (should-be-nil (:wip (first (:scenarios ir)))))))

  (context "error reporting"

    (it "throws on missing Feature keyword"
      (should-throw RuntimeException
        (parser/parse-feature "Scenario: No feature\n  Given something\n")))

    (it "throws on empty input"
      (should-throw RuntimeException
        (parser/parse-feature ""))))

  (context "scenario outlines"

    (it "expands outline to concrete scenarios"
      (let [ir (parser/parse-feature
                 (str "Feature: Login\n"
                      "\n"
                      "  Scenario Outline: Login with role\n"
                      "    Given a user \"<name>\" with role \"<role>\"\n"
                      "\n"
                      "    Examples:\n"
                      "      | name  | role  |\n"
                      "      | alice | admin |\n"
                      "      | bob   | guest |\n"))]
        (should= 2 (count (:scenarios ir)))
        (should= "Login with role — alice, admin" (:scenario (first (:scenarios ir))))
        (should= "Login with role — bob, guest" (:scenario (second (:scenarios ir))))
        (should= "a user \"alice\" with role \"admin\""
                 (:text (first (:steps (first (:scenarios ir))))))))

    (it "applies tags to all expanded scenarios"
      (let [ir (parser/parse-feature
                 (str "Feature: WIP\n"
                      "\n"
                      "  @wip\n"
                      "  Scenario Outline: Not ready\n"
                      "    Given value is <val>\n"
                      "\n"
                      "    Examples:\n"
                      "      | val |\n"
                      "      | one |\n"
                      "      | two |\n"))]
        (should= 2 (count (:scenarios ir)))
        (should= ["wip"] (:tags (first (:scenarios ir))))
        (should= ["wip"] (:tags (second (:scenarios ir))))))))
