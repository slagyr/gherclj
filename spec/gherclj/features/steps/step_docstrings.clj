(ns gherclj.features.steps.step-docstrings
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven defthen helper!]]
            [gherclj.sample.dragon-steps]))

(helper! gherclj.features.steps.step-docstrings)

(defn lookup-registered-step-from-dragon-suite! [name]
  (let [normalized-name (str/replace name #"^\"|\"$" "")
        steps (g/steps-in-ns 'gherclj.sample.dragon-steps)
        step (first (filter #(= normalized-name (:name %)) steps))]
    (g/assoc! :registered-step step)))

(defn step-should-have-docstring [doc]
  (let [step (g/get :registered-step)]
    (g/should-not-be-nil step)
    (g/should= doc (:doc step))))

(defn step-should-have-no-docstring []
  (let [step (g/get :registered-step)]
    (g/should-not-be-nil step)
    (g/should-be-nil (:doc step))))

(defgiven "the registered step {name} from dragon suite" step-docstrings/lookup-registered-step-from-dragon-suite!
  "Looks up by function name from gherclj.sample.dragon-steps namespace. Stores the registry entry in :registered-step.")

(defthen "the step should have docstring {doc:string}" step-docstrings/step-should-have-docstring)

(defthen "the step should have no docstring" step-docstrings/step-should-have-no-docstring)
