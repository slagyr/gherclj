(ns gherclj.features.steps.step-docstrings
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven defwhen defthen]]))

;; --- Fixture steps ---

(defgiven fixture-no-doc "a bare step with no doc"
  []
  :bare)

(defgiven fixture-with-doc "a documented step"
  "Sets :crew atom — does NOT write disk."
  []
  :documented)

(defwhen fixture-async "the async fixture runs"
  "Polls for up to 2s."
  []
  :async)

(defthen fixture-check "the async fixture should match"
  "Matches within 2s timeout."
  []
  (g/should true))

;; --- Acceptance steps ---

(defgiven lookup-registered-step-from-docstring-suite "the registered step {name} from docstring suite"
  [name]
  (let [normalized-name (str/replace name #"^\"|\"$" "")
        steps (g/steps-in-ns 'gherclj.features.steps.step-docstrings)
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
