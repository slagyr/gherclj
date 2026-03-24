@wip
Feature: Step namespace discovery

  Step namespaces can be discovered automatically using glob
  patterns instead of listing every namespace explicitly.

  Scenario: Discover step namespaces by pattern
    Given a config:
      """
      {:step-ns-patterns ["myapp.features.steps.*"]}
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

  Scenario: Pattern with glob in the middle
    Given a config:
      """
      {:step-ns-patterns ["myapp.*-steps"]}
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

  Scenario: Multiple patterns
    Given a config:
      """
      {:step-ns-patterns ["myapp.steps.*" "myapp.extra.*"]}
      """
    And namespaces on the classpath:
      | namespace            |
      | myapp.steps.auth     |
      | myapp.extra.billing  |
      | myapp.core           |
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      [myapp.extra.billing myapp.steps.auth]
      """

  Scenario: Explicit namespaces combine with patterns
    Given a config:
      """
      {:step-ns-patterns ["myapp.steps.*"]
       :step-namespaces [myapp.manual.steps]}
      """
    And namespaces on the classpath:
      | namespace            |
      | myapp.steps.auth     |
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      [myapp.manual.steps myapp.steps.auth]
      """

  Scenario: No pattern and no explicit namespaces
    Given a config:
      """
      {}
      """
    When step namespaces are resolved
    Then the resolved step namespaces should be:
      """
      []
      """
