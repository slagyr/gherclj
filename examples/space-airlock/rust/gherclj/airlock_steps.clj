(ns gherclj.airlock-steps
  "Step routing for the Rust implementation of SpaceAirlock.
   Each entry maps a Gherkin phrase to a method call on subject."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [gherclj.frameworks.rust.rustc-test :as rust]))

(helper! "src/space_airlock.rs")
(helper! "src/space_airlock_steps.rs")

(rust/scenario-setup! "let mut subject = space_airlock_steps::SpaceAirlockSteps::new();")

(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$" subject.crew-member-inside)
(defgiven "a visitor is inside the airlock" subject.visitor-inside)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$" subject.wearing-suit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$" subject.not-wearing-suit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$" subject.valid-badge)
(defgiven "the visitor does not have a valid badge" subject.visitor-invalid-badge)
(defgiven "the {door} door is {state}" subject.door-state)
(defgiven "the chamber is {pressure}" subject.chamber-pressure)
(defgiven "the emergency override is engaged" subject.emergency-override-engaged)

(defwhen #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$" subject.request-exit)
(defwhen "the {door} door is commanded open" subject.open-door)
(defwhen "depressurization is commanded" subject.depressurization-commanded)
(defwhen "repressurization is commanded" subject.repressurization-commanded)

(defthen "the chamber should depressurize" subject.chamber-should-depressurize)
(defthen "the chamber should remain {pressure}" subject.chamber-should-remain)
(defthen "the {door} door should unlock" subject.door-should-unlock)
(defthen "the {door} door should remain locked" subject.door-should-remain-locked)
(defthen "the request should be denied" subject.request-should-be-denied)
(defthen "the system should display {message:string}" subject.system-should-display)
(defthen "the airlock status should be {status:string}" subject.airlock-status-should-be)
