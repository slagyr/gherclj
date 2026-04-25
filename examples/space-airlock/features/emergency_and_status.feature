Feature: Emergency override and status reporting

  The station needs clear state reporting and one-way emergency recovery.

  Scenario: Airlock reports ready for boarding
    Given the chamber is pressurized
    And the inner door is unlocked
    And the outer door is locked
    Then the airlock status should be "ready for boarding"

  Scenario: Airlock reports ready for exit
    Given the chamber is depressurized
    And the inner door is locked
    And the outer door is unlocked
    Then the airlock status should be "ready for exit"

  Scenario: Emergency override unlocks the inner door only
    Given the chamber is pressurized
    And the outer door is closed
    And the emergency override is engaged
    When the inner door is commanded open
    Then the inner door should unlock
    And the outer door should remain locked
    And the system should display "Emergency override active"

  Scenario: Emergency override does not open the outer door into vacuum
    Given the chamber is pressurized
    And the emergency override is engaged
    When the outer door is commanded open
    Then the outer door should remain locked
    And the system should display "Outer door lock preserved"
