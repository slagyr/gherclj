(ns gherclj.airlock-steps
  "Step routing for the Java implementation of SpaceAirlock.
   Each entry maps a Gherkin phrase to a method call on a per-scenario
   `SpaceAirlockHelper`, which delegates Givens/Whens to a SpaceAirlock
   and turns Thens into JUnit assertions. The local `airlock` variable
   is created per-scenario via junit5/scenario-setup!."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.java.junit5 :as junit5]))

;; Import the test-side helper. The value is emitted verbatim as a Java
;; `import` line, so use the fully-qualified class name.
(helper! "airlock.SpaceAirlockHelper")

;; Per-scenario setup: every @Test method body starts with this line.
(junit5/scenario-setup! "SpaceAirlockHelper airlock = new SpaceAirlockHelper();")

;; --- Step routing (phrase → method call on airlock) ---

(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$"     airlock.crewMemberInside)
(defgiven "a visitor is inside the airlock"                                   airlock.visitorInside)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$"         airlock.wearingSuit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$"     airlock.notWearingSuit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$"         airlock.validBadge)
(defgiven "the visitor does not have a valid badge"                           airlock.visitorInvalidBadge)
(defgiven "the {door} door is {state}"                                        airlock.doorState)
(defgiven "the chamber is {pressure}"                                         airlock.chamberPressure)
(defgiven "the emergency override is engaged"                                 airlock.emergencyOverrideEngaged)

(defwhen  #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$" airlock.requestExit)
(defwhen  "the {door} door is commanded open"                                 airlock.openDoor)
(defwhen  "depressurization is commanded"                                     airlock.depressurizationCommanded)
(defwhen  "repressurization is commanded"                                     airlock.repressurizationCommanded)

(defthen  "the chamber should depressurize"                                   airlock.chamberShouldDepressurize)
(defthen  "the chamber should remain {pressure}"                              airlock.chamberShouldRemain)
(defthen  "the {door} door should unlock"                                     airlock.doorShouldUnlock)
(defthen  "the {door} door should remain locked"                              airlock.doorShouldRemainLocked)
(defthen  "the request should be denied"                                      airlock.requestShouldBeDenied)
(defthen  "the system should display {message:string}"                        airlock.systemShouldDisplay)
(defthen  "the airlock status should be {status:string}"                      airlock.airlockStatusShouldBe)
