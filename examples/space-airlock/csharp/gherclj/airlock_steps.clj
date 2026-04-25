(ns gherclj.airlock-steps
  "Step routing for the C# implementation of SpaceAirlock.
   Each entry maps a Gherkin phrase to a method call on subject."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.csharp.xunit :as xunit]))

(helper! "SpaceAirlock.Steps")
(xunit/project-reference! "src/SpaceAirlock.Steps/SpaceAirlock.Steps.csproj")
(xunit/scenario-setup! "var subject = new SpaceAirlockSteps();")


(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$" subject.crewMemberInside)
(defgiven "a visitor is inside the airlock" subject.visitorInside)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$" subject.wearingSuit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$" subject.notWearingSuit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$" subject.validBadge)
(defgiven "the visitor does not have a valid badge" subject.visitorInvalidBadge)
(defgiven "the {door} door is {state}" subject.doorState)
(defgiven "the chamber is {pressure}" subject.chamberPressure)
(defgiven "the emergency override is engaged" subject.emergencyOverrideEngaged)

(defwhen #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$" subject.requestExit)
(defwhen "the {door} door is commanded open" subject.openDoor)
(defwhen "depressurization is commanded" subject.depressurizationCommanded)
(defwhen "repressurization is commanded" subject.repressurizationCommanded)

(defthen "the chamber should depressurize" subject.chamberShouldDepressurize)
(defthen "the chamber should remain {pressure}" subject.chamberShouldRemain)
(defthen "the {door} door should unlock" subject.doorShouldUnlock)
(defthen "the {door} door should remain locked" subject.doorShouldRemainLocked)
(defthen "the request should be denied" subject.requestShouldBeDenied)
(defthen "the system should display {message:string}" subject.systemShouldDisplay)
(defthen "the airlock status should be {status:string}" subject.airlockStatusShouldBe)
