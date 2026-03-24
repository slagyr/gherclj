@wip
Feature: Doc-string step arguments

  Gherkin steps can have a doc-string argument, delimited by
  triple quotes. The doc-string is attached to the step in
  the IR, similar to data tables.

  Scenario: Step with a doc-string
    Given a feature file containing:
      """
      Feature: Docs

        Scenario: With doc-string
          Given a document containing:
            \"\"\"
            Hello, world!
            This is a test.
            \"\"\"
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Docs"
       :scenarios [{:scenario "With doc-string"
                    :steps [{:type :given
                             :text "a document containing:"
                             :doc-string "Hello, world!\nThis is a test."}]}]}
      """

  Scenario: Doc-string preserves indentation relative to delimiter
    Given a feature file containing:
      """
      Feature: Indentation

        Scenario: Indented content
          Given a code block:
            \"\"\"
            def foo():
              return 42
            \"\"\"
      """
    When the feature is parsed
    Then the IR should be:
      """
      {:feature "Indentation"
       :scenarios [{:scenario "Indented content"
                    :steps [{:type :given
                             :text "a code block:"
                             :doc-string "def foo():\n  return 42"}]}]}
      """
