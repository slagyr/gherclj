(ns gherclj.sample.ambiguous-steps
  "Fixture namespace with intentional step collisions for ambiguity reporting."
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]))

(helper! gherclj.sample.ambiguous-steps)

(defn user-by-name [_name])
(defn user-by-handle [_handle])
(defn login-template [])
(defn login-regex [_suffix])
(defn duplicate-regex-one [_value])
(defn duplicate-regex-two [_value])

(defgiven "a user {name:string}" ambiguous-steps/user-by-name)
(defgiven "a user {handle:string}" ambiguous-steps/user-by-handle)
(defwhen "the user logs in" ambiguous-steps/login-template)
(defwhen #"^the user .+$" ambiguous-steps/login-regex)
(defthen #"^danger (.+)$" ambiguous-steps/duplicate-regex-one)
(defthen #"^danger (.+)$" ambiguous-steps/duplicate-regex-two)
