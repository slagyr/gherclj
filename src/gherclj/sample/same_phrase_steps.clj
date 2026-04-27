(ns gherclj.sample.same-phrase-steps
  "Fixture namespace with the same phrase registered under multiple step types."
  (:require [gherclj.core :refer [defgiven defwhen helper!]]))

(helper! gherclj.sample.same-phrase-steps)

(defn login-state [])
(defn perform-login [])

(defgiven "the user logs in" same-phrase-steps/login-state)
(defwhen "the user logs in" same-phrase-steps/perform-login)
