(ns gherclj.sample.app-steps
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]))

(helper! gherclj.sample.app-steps)

(defn create-adventurer [name] :created)
(defn enter-the-realm [] :logged-in)
(defn verify-outcome [status] :checked)

(defgiven "a user {name:string}" app-steps/create-adventurer)
(defwhen "the user logs in" app-steps/enter-the-realm)
(defthen "the response should be {status:int}" app-steps/verify-outcome)
