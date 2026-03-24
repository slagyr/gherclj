(ns gherclj.features.steps.sample-app
  (:require [gherclj.core :refer [defgiven defwhen defthen]]))

(defgiven create-user "a user \"{name}\""
  [name]
  :created)

(defwhen user-logs-in "the user logs in"
  []
  :logged-in)

(defthen response-should-be "the response should be {status:int}"
  [status]
  :checked)
