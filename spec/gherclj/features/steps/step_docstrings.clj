(ns gherclj.features.steps.step-docstrings
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven defthen]]
            [gherclj.sample.dragon-steps]))

(defgiven lookup-registered-step-from-dragon-suite "the registered step {name} from dragon suite"
  "Looks up by function name from gherclj.sample.dragon-steps namespace. Stores the registry entry in :registered-step."
  [name]
  (let [normalized-name (str/replace name #"^\"|\"$" "")
        steps (g/steps-in-ns 'gherclj.sample.dragon-steps)
        step (first (filter #(= normalized-name (:name %)) steps))]
    (g/assoc! :registered-step step)))

(defthen step-should-have-docstring "the step should have docstring {doc:string}"
  [doc]
  (let [step (g/get :registered-step)]
    (g/should-not-be-nil step)
    (g/should= doc (:doc step))))

(defthen step-should-have-no-docstring "the step should have no docstring"
  []
  (let [step (g/get :registered-step)]
    (g/should-not-be-nil step)
    (g/should-be-nil (:doc step))))
