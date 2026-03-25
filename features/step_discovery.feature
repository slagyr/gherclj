Feature: Step namespace discovery

  Step namespaces can be specified as concrete symbols or
  glob pattern strings. Patterns are matched against namespaces
  found on the classpath.

  Scenario: Concrete namespace symbol
    Given a config:
      """
      {:step-namespaces [myapp.steps.auth]}
      """
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      [myapp.steps.auth]
      """

  Scenario: Glob pattern discovers namespaces
    Given a config:
      """
      {:step-namespaces ["myapp.features.steps.*"]}
      """
    And namespaces on the classpath:
      | namespace                     |
      | myapp.features.steps.auth     |
      | myapp.features.steps.cart     |
      | myapp.features.harness        |
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      [myapp.features.steps.auth myapp.features.steps.cart]
      """

  Scenario: Glob in the middle of a pattern
    Given a config:
      """
      {:step-namespaces ["myapp.*-steps"]}
      """
    And namespaces on the classpath:
      | namespace            |
      | myapp.auth-steps     |
      | myapp.cart-steps     |
      | myapp.auth-helpers   |
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      [myapp.auth-steps myapp.cart-steps]
      """

  Scenario: Mix of concrete symbols and patterns
    Given a config:
      """
      {:step-namespaces [myapp.manual.steps "myapp.features.steps.*"]}
      """
    And namespaces on the classpath:
      | namespace                     |
      | myapp.features.steps.auth     |
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      [myapp.manual.steps myapp.features.steps.auth]
      """

  Scenario: No step namespaces configured
    Given a config:
      """
      {}
      """
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      []
      """
