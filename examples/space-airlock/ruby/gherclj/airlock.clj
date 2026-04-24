(ns gherclj.airlock
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.frameworks.rspec :as rspec]))

(rspec/file-setup! "require File.expand_path('lib/space_airlock', Dir.pwd)")
(rspec/describe-setup! "subject { SpaceAirlock.new }")

(defgiven crew-member-inside #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$"
  [name])

(defgiven visitor-inside "a visitor is inside the airlock"
  [])

(defgiven wearing-suit #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$"
  [name])

(defgiven not-wearing-suit #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$"
  [name])

(defgiven valid-badge #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$"
  [name])

(defgiven visitor-invalid-badge "the visitor does not have a valid badge"
  [])

(defgiven door-state "the {door} door is {state}"
  [door state])

(defgiven chamber-pressure "the chamber is {pressure}"
  [pressure])

(defgiven emergency-override-engaged "the emergency override is engaged"
  [])

(defwhen request-exit #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$"
  [name])

(defwhen open-door "the {door} door is commanded open"
  [door])

(defwhen depressurization-commanded "depressurization is commanded"
  [])

(defwhen repressurization-commanded "repressurization is commanded"
  [])

(defthen chamber-should-depressurize "the chamber should depressurize"
  [])

(defthen chamber-should-remain "the chamber should remain {pressure}"
  [pressure])

(defthen door-should-unlock "the {door} door should unlock"
  [door])

(defthen door-should-remain-locked "the {door} door should remain locked"
  [door])

(defthen request-should-be-denied "the request should be denied"
  [])

(defthen system-should-display "the system should display {message:string}"
  [message])

(defthen airlock-status-should-be "the airlock status should be {status:string}"
  [status])
