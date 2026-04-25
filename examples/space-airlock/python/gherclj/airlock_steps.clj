(ns gherclj.airlock-steps
  "Step routing for the Python implementation of SpaceAirlock.
   Each entry maps a Gherkin phrase to a method call on sut."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.python.pytest :as pytest]))

(helper! "from space_airlock import AirlockChecks, SpaceAirlock")

(pytest/scenario-setup! "airlock = SpaceAirlock()")
(pytest/scenario-setup! "checks = AirlockChecks(airlock)")

(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$"     airlock.crew-member-inside)
(defgiven "a visitor is inside the airlock"                                   airlock.visitor-inside)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$"         airlock.wearing-suit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$"     airlock.not-wearing-suit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$"         airlock.valid-badge)
(defgiven "the visitor does not have a valid badge"                           airlock.visitor-invalid-badge)
(defgiven "the {door} door is {state}"                                        airlock.door-state)
(defgiven "the chamber is {pressure}"                                         airlock.chamber-pressure)
(defgiven "the emergency override is engaged"                                 airlock.emergency-override-engaged)

(defwhen  #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$" airlock.request-exit)
(defwhen  "the {door} door is commanded open"                                 airlock.open-door)
(defwhen  "depressurization is commanded"                                     airlock.depressurization-commanded)
(defwhen  "repressurization is commanded"                                     airlock.repressurization-commanded)

(defthen  "the chamber should depressurize"                                   checks.chamber-is-depressurized)
(defthen  "the chamber should remain {pressure}"                              checks.chamber-remains)
(defthen  "the {door} door should unlock"                                     checks.door-is-unlocked)
(defthen  "the {door} door should remain locked"                              checks.door-remains-locked)
(defthen  "the request should be denied"                                      checks.request-is-denied)
(defthen  "the system should display {message:string}"                        checks.displays-message)
(defthen  "the airlock status should be {status:string}"                      checks.status-is)
