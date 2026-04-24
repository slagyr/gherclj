(ns gherclj.sample.app-steps
  (:require [gherclj.core :refer [defgiven defwhen defthen]]))

(defgiven create-adventurer "a user {name:string}"
  [name]
  :created)

(defwhen enter-the-realm "the user logs in"
  []
  :logged-in)

(defthen verify-outcome "the response should be {status:int}"
  [status]
  :checked)
