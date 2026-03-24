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
        (should-be-nil (:doc-string (first (:steps (first (:scenarios ir))))))))))
