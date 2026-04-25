Feature: Scenario location selection

  gherclj can run specific scenarios by source location using
  file:line arguments. A location selects the scenario whose
  declaration contains that line in the feature file. A bare
  .feature path (no line number) selects every scenario in the
  file, and bare and file:line selectors may be mixed freely.

  Scenario: CLI parses a single file:line selector
    When running gherclj with "features/adventure/dragon_cave.feature:42"
    Then the resolved config should contain:
      """
      {:locations [{:source "features/adventure/dragon_cave.feature"
                    :line 42}]}
      """

  Scenario: CLI parses multiple file:line selectors
    When running gherclj with "features/adventure/dragon_cave.feature:42 features/adventure/moon_castle.feature:73"
    Then the resolved config should contain:
      """
      {:locations [{:source "features/adventure/dragon_cave.feature"
                    :line 42}
                   {:source "features/adventure/moon_castle.feature"
                    :line 73}]}
      """

  Scenario: Full pipeline generates only the scenario at the selected location
    Given a features directory containing:
      | file                         |
      | adventure/dragon_cave.feature |
    And the feature "adventure/dragon_cave.feature" contains:
      """
      Feature: Dragon cave

        Scenario: Wake the dragon
          Given a sleepy dragon
          When the bell is rung
          Then the dragon opens one eye

        Scenario: Negotiate for treasure
          Given an awake dragon
          When the bard offers a song
          Then the dragon shares one coin
      """
    When the full pipeline runs with framework :clojure/speclj and locations:
      | location                         |
      | adventure/dragon_cave.feature:4 |
    Then "target/gherclj/generated/adventure/dragon_cave_spec.clj" should exist and:
      | check        | value                  |
      | contains     | Wake the dragon        |
      | not-contains | Negotiate for treasure |

  Scenario: A line inside a scenario body selects that scenario
    Given a features directory containing:
      | file                        |
      | adventure/moon_castle.feature |
    And the feature "adventure/moon_castle.feature" contains:
      """
      Feature: Moon castle

        Scenario: Launch the kite bridge
          Given a windy courtyard
          When the silver kite is released
          Then the bridge reaches the tower

        Scenario: Crown the raccoon king
          Given a raccoon in a velvet cape
          When the trumpet sounds
          Then the court bows politely
      """
    When the full pipeline runs with framework :clojure/speclj and locations:
      | location                        |
      | adventure/moon_castle.feature:5 |
    Then "target/gherclj/generated/adventure/moon_castle_spec.clj" should exist and:
      | check        | value                  |
      | contains     | Launch the kite bridge |
      | not-contains | Crown the raccoon king |

  Scenario: CLI runs multiple selected scenarios in one invocation
    Given a features directory containing:
      | file                         |
      | adventure/dragon_cave.feature |
      | adventure/moon_castle.feature |
    And the feature "adventure/dragon_cave.feature" contains:
      """
      Feature: Dragon cave

        Scenario: Wake the dragon
          Given a sleepy dragon
      """
    And the feature "adventure/moon_castle.feature" contains:
      """
      Feature: Moon castle

        Scenario: Crown the raccoon king
          Given a raccoon in a velvet cape
      """
    When running gherclj with "-f features -o target/gherclj/generated -e target/gherclj/edn adventure/dragon_cave.feature:3 adventure/moon_castle.feature:3"
    Then "target/gherclj/generated/adventure/dragon_cave_spec.clj" should exist and:
      | check    | value           |
      | contains | Wake the dragon |
    And "target/gherclj/generated/adventure/moon_castle_spec.clj" should exist and:
      | check    | value                  |
      | contains | Crown the raccoon king |

  Scenario: CLI parses a bare feature path as a location
    When running gherclj with "features/adventure/dragon_cave.feature"
    Then the resolved config should contain:
      """
      {:locations [{:source "features/adventure/dragon_cave.feature"}]}
      """

  Scenario: CLI parses a bare feature path mixed with a file:line selector
    When running gherclj with "features/adventure/dragon_cave.feature features/adventure/moon_castle.feature:73"
    Then the resolved config should contain:
      """
      {:locations [{:source "features/adventure/dragon_cave.feature"}
                   {:source "features/adventure/moon_castle.feature"
                    :line 73}]}
      """

  Scenario: Full pipeline runs every scenario in a bare feature location
    Given a features directory containing:
      | file                          |
      | adventure/dragon_cave.feature |
    And the feature "adventure/dragon_cave.feature" contains:
      """
      Feature: Dragon cave

        Scenario: Wake the dragon
          Given a sleepy dragon

        Scenario: Negotiate for treasure
          Given an awake dragon
      """
    When the full pipeline runs with framework :clojure/speclj and locations:
      | location                      |
      | adventure/dragon_cave.feature |
    Then "target/gherclj/generated/adventure/dragon_cave_spec.clj" should exist and:
      | check    | value                  |
      | contains | Wake the dragon        |
      | contains | Negotiate for treasure |

  Scenario: CLI mixes a bare feature and a file:line across two files
    Given a features directory containing:
      | file                          |
      | adventure/dragon_cave.feature |
      | adventure/moon_castle.feature |
    And the feature "adventure/dragon_cave.feature" contains:
      """
      Feature: Dragon cave

        Scenario: Wake the dragon
          Given a sleepy dragon

        Scenario: Negotiate for treasure
          Given an awake dragon
      """
    And the feature "adventure/moon_castle.feature" contains:
      """
      Feature: Moon castle

        Scenario: Launch the kite bridge
          Given a windy courtyard

        Scenario: Crown the raccoon king
          Given a raccoon in a velvet cape
      """
    When running gherclj with "-f features -o target/gherclj/generated -e target/gherclj/edn adventure/dragon_cave.feature adventure/moon_castle.feature:6"
    Then "target/gherclj/generated/adventure/dragon_cave_spec.clj" should exist and:
      | check    | value                  |
      | contains | Wake the dragon        |
      | contains | Negotiate for treasure |
    And "target/gherclj/generated/adventure/moon_castle_spec.clj" should exist and:
      | check        | value                  |
      | contains     | Crown the raccoon king |
      | not-contains | Launch the kite bridge |

  Scenario: Unknown bare feature path is rejected
    Given a features directory containing:
      | file                          |
      | adventure/dragon_cave.feature |
    And the feature "adventure/dragon_cave.feature" contains:
      """
      Feature: Dragon cave

        Scenario: Wake the dragon
          Given a sleepy dragon
      """
    When running gherclj with "adventure/ghost.feature"
    Then the output should contain "Feature file not found"
    And the output should contain "adventure/ghost.feature"

  Scenario: Unknown file:line selector is rejected
    Given a features directory containing:
      | file                         |
      | adventure/dragon_cave.feature |
    And the feature "adventure/dragon_cave.feature" contains:
      """
      Feature: Dragon cave

        Scenario: Wake the dragon
          Given a sleepy dragon
      """
    When running gherclj with "adventure/dragon_cave.feature:99"
    Then the output should contain "No scenario found for location"
    And the output should contain "adventure/dragon_cave.feature:99"
