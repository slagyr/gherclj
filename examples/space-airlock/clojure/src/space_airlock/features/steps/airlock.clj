(ns space-airlock.features.steps.airlock
  "Step routing for airlock feature scenarios. Each entry maps a step
   phrase to a helper function — no logic here. The actual test logic
   lives in space-airlock.features.helpers.airlock."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [space-airlock.features.helpers.airlock]))

(helper! space-airlock.features.helpers.airlock)

(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is inside the airlock$"     airlock/crew-member-inside!)
(defgiven "a visitor is inside the airlock"                                   airlock/visitor-inside!)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is wearing a suit$"         airlock/wearing-suit!)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) is not wearing a suit$"     airlock/not-wearing-suit!)
(defgiven #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*) has a valid badge$"         airlock/valid-badge!)
(defgiven "the visitor does not have a valid badge"                           airlock/visitor-invalid-badge!)
(defgiven "the {door} door is {state}"                                        airlock/door-state!)
(defgiven "the chamber is {pressure}"                                         airlock/chamber-pressure!)
(defgiven "the emergency override is engaged"                                 airlock/emergency-override-engaged!)

(defwhen  #"^([A-Z][A-Za-z]+(?: [A-Z][A-Za-z]+)*|the visitor) requests exit$" airlock/request-exit!)
(defwhen  "the {door} door is commanded open"                                 airlock/open-door!)
(defwhen  "depressurization is commanded"                                     airlock/depressurization-commanded!)
(defwhen  "repressurization is commanded"                                     airlock/repressurization-commanded!)

(defthen  "the chamber should depressurize"                                   airlock/chamber-should-depressurize)
(defthen  "the chamber should remain {pressure}"                              airlock/chamber-should-remain)
(defthen  "the {door} door should unlock"                                     airlock/door-should-unlock)
(defthen  "the {door} door should remain locked"                              airlock/door-should-remain-locked)
(defthen  "the request should be denied"                                      airlock/request-should-be-denied)
(defthen  "the system should display {message:string}"                        airlock/system-should-display)
(defthen  "the airlock status should be {status:string}"                      airlock/airlock-status-should-be)
