(ns gherclj.airlock
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.frameworks.rspec :as rspec]))

(rspec/file-setup! "require File.expand_path('lib/space_airlock', Dir.pwd)")
(rspec/describe-setup! "subject { SpaceAirlock.new }")

(defn- rspec-only [] (throw (UnsupportedOperationException. "rspec step — not executable in Clojure")))

(defgiven crew-member-inside #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$"
  [name] (rspec-only))

(defgiven visitor-inside "a visitor is inside the airlock"
  [] (rspec-only))

(defgiven wearing-suit #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$"
  [name] (rspec-only))

(defgiven not-wearing-suit #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$"
  [name] (rspec-only))

(defgiven valid-badge #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$"
  [name] (rspec-only))

(defgiven visitor-invalid-badge "the visitor does not have a valid badge"
  [] (rspec-only))

(defgiven door-state "the {door} door is {state}"
  [door state] (rspec-only))

(defgiven chamber-pressure "the chamber is {pressure}"
  [pressure] (rspec-only))

(defgiven emergency-override-engaged "the emergency override is engaged"
  [] (rspec-only))

(defwhen request-exit #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$"
  [name] (rspec-only))

(defwhen open-door "the {door} door is commanded open"
  [door] (rspec-only))

(defwhen depressurization-commanded "depressurization is commanded"
  [] (rspec-only))

(defwhen repressurization-commanded "repressurization is commanded"
  [] (rspec-only))

(defthen chamber-should-depressurize "the chamber should depressurize"
  [] (rspec-only))

(defthen chamber-should-remain "the chamber should remain {pressure}"
  [pressure] (rspec-only))

(defthen door-should-unlock "the {door} door should unlock"
  [door] (rspec-only))

(defthen door-should-remain-locked "the {door} door should remain locked"
  [door] (rspec-only))

(defthen request-should-be-denied "the request should be denied"
  [] (rspec-only))

(defthen system-should-display "the system should display {message:string}"
  [message] (rspec-only))

(defthen airlock-status-should-be "the airlock status should be {status:string}"
  [status] (rspec-only))
