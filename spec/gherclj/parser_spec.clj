(ns gherclj.parser-spec
  (:require [speclj.core :refer :all]
            [gherclj.parser :as parser]))

(describe "Parser"

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
        (should-be-nil (:wip (first (:scenarios ir))))))))
