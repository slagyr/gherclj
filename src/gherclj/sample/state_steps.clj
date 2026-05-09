(ns gherclj.sample.state-steps
  (:require [gherclj.core :as g :refer [defwhen defthen helper!]]))

(helper! gherclj.sample.state-steps)

(defn set-shared-value! [value]
  (g/assoc! :shared value))

(defn shared-value-should-be [value]
  (g/should= value (g/get :shared)))

(defn shared-value-should-be-absent []
  (g/should-be-nil (g/get :shared)))

(defwhen "the shared value becomes {value:string}" state-steps/set-shared-value!)

(defthen #"^the shared value should be \"([^\"]+)\"$" state-steps/shared-value-should-be)

(defthen "the shared value should be absent" state-steps/shared-value-should-be-absent)
