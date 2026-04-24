(ns gherclj.sample.dragon-steps
  (:require [gherclj.core :refer [defgiven defwhen defthen]]))

(defgiven summon-dragon "a dragon named {name:string}"
  [name]
  :summoned)

(defgiven dragon-hoards "the dragon hoards {item:string}"
  "Adds to hoard without duplicate checking."
  [item]
  :hoarded)

(defwhen dragon-breathes "the dragon breathes fire"
  "Raises cave temperature by exactly 300 degrees."
  []
  :breathed)

(defthen treasure-check "the hoard should include {item:string}"
  "Checks visible hoard only — buried treasure excluded."
  [item]
  :checked)

(defgiven cave-contains #"^the cave contains (.+)$"
  [contents]
  :cave)

(defgiven dragon-count #"^the dragon has (\d+) treasures$"
  [n]
  :counted)
