(ns gherclj.airlock
  "Step routing for the Ruby implementation of SpaceAirlock.
   Each entry maps a Gherkin phrase to the matching Ruby method name.
   The actual logic lives in lib/space_airlock.rb (the production code)
   and lib/space_airlock_steps.rb (a thin module that delegates each
   method to subject)."
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.frameworks.rspec :as rspec]))

;; Ruby file-level setup
(rspec/file-setup! "require File.expand_path('lib/space_airlock_steps', Dir.pwd)")

;; describe-block setup — define the subject and bring step methods into scope
(rspec/describe-setup! "subject { SpaceAirlock.new }")
(rspec/describe-setup! "include SpaceAirlockSteps")

;; --- Step routing (phrase → Ruby method name) ---

(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$"     crew-member-inside)
(defgiven "a visitor is inside the airlock"                                   visitor-inside)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$"         wearing-suit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$"     not-wearing-suit)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$"         valid-badge)
(defgiven "the visitor does not have a valid badge"                           visitor-invalid-badge)
(defgiven "the {door} door is {state}"                                        door-state)
(defgiven "the chamber is {pressure}"                                         chamber-pressure)
(defgiven "the emergency override is engaged"                                 emergency-override-engaged)

(defwhen  #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$" request-exit)
(defwhen  "the {door} door is commanded open"                                 open-door)
(defwhen  "depressurization is commanded"                                     depressurization-commanded)
(defwhen  "repressurization is commanded"                                     repressurization-commanded)

(defthen  "the chamber should depressurize"                                   chamber-should-depressurize)
(defthen  "the chamber should remain {pressure}"                              chamber-should-remain)
(defthen  "the {door} door should unlock"                                     door-should-unlock)
(defthen  "the {door} door should remain locked"                              door-should-remain-locked)
(defthen  "the request should be denied"                                      request-should-be-denied)
(defthen  "the system should display {message:string}"                        system-should-display)
(defthen  "the airlock status should be {status:string}"                      airlock-status-should-be)
