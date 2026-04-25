Feature: Safety interlocks

  The airlock must refuse impossible door operations and unsafe cycles.

  Scenario: Outer door cannot open while the chamber is pressurized
    Given the chamber is pressurized
    And the outer door is closed
    When the outer door is commanded open
    Then the outer door should remain locked
    And the system should display "Depressurize first"

  Scenario: Inner door cannot open while the chamber is depressurized
    Given the chamber is depressurized
    And the inner door is closed
    When the inner door is commanded open
    Then the inner door should remain locked
    And the system should display "Repressurize first"

  Scenario: The airlock cannot depressurize while the inner door is open
    Given the chamber is pressurized
    And the inner door is open
    When depressurization is commanded
    Then the chamber should remain pressurized
    And the system should display "Close inner door"

  Scenario: The airlock cannot repressurize while the outer door is open
    Given the chamber is depressurized
    And the outer door is open
    When repressurization is commanded
    Then the chamber should remain depressurized
    And the system should display "Close outer door"
