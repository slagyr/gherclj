Feature: Airlock exit cycle

  To let crew leave the station without dying in vacuum
  As the airlock control system
  I enforce authorization and suit checks before opening the outer door

  Scenario: Authorized crew member exits through the airlock
    Given Commander Vega is inside the airlock
    And Commander Vega is wearing a suit
    And Commander Vega has a valid badge
    And the inner door is closed
    And the outer door is closed
    And the chamber is pressurized
    When Commander Vega requests exit
    Then the chamber should depressurize
    And the outer door should unlock
    And the inner door should remain locked
    And the airlock status should be "cycle complete"

  Scenario: Crew member without a suit cannot exit
    Given Engineer Moss is inside the airlock
    And Engineer Moss has a valid badge
    But Engineer Moss is not wearing a suit
    And the chamber is pressurized
    When Engineer Moss requests exit
    Then the request should be denied
    And the chamber should remain pressurized
    And the outer door should remain locked
    And the system should display "Suit required"

  Scenario: Unauthorized visitor cannot cycle the airlock
    Given a visitor is inside the airlock
    And the visitor does not have a valid badge
    And the chamber is pressurized
    When the visitor requests exit
    Then the request should be denied
    And the outer door should remain locked
    And the system should display "Authorization required"
