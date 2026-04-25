(ns gherclj.sample.dragon-steps
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]))

(helper! gherclj.sample.dragon-steps)

(defn summon-dragon [name] :summoned)
(defn dragon-hoards [item] :hoarded)
(defn dragon-breathes [] :breathed)
(defn treasure-check [item] :checked)
(defn dragon-vanishes [] (g/should= :present :gone))
(defn cave-contains [contents] :cave)
(defn dragon-count [n] :counted)

(defgiven "a dragon named {name:string}" dragon-steps/summon-dragon)
(defgiven "the dragon hoards {item:string}" dragon-steps/dragon-hoards
  "Adds to hoard without duplicate checking.")
(defwhen "the dragon breathes fire" dragon-steps/dragon-breathes
  "Raises cave temperature by exactly 300 degrees.")
(defthen "the hoard should include {item:string}" dragon-steps/treasure-check
  "Checks visible hoard only — buried treasure excluded.")
(defthen "the dragon vanishes unexpectedly" dragon-steps/dragon-vanishes
  "Always fails — use in lifecycle scenarios to verify hooks fire on step failure.")
(defgiven #"^the cave contains (.+)$" dragon-steps/cave-contains)
(defgiven #"^the dragon has (\d+) treasures$" dragon-steps/dragon-count)
