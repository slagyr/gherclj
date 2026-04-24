(ns gherclj.features.steps.step-definitions
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.template :as template]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defgiven define-given-step "a given step named {name:string} with template {template:string}"
  "Creates a step entry in :steps state (not the global registry). Uses a fake ns symbol."
  [name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type :given :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defgiven define-when-step "a when step named {name:string} with template {template:string}"
  "Creates a step entry in :steps state (not the global registry). Uses a fake ns symbol."
  [name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type :when :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defgiven define-then-step "a then step named {name:string} with template {template:string}"
  "Creates a step entry in :steps state (not the global registry). Uses a fake ns symbol."
  [name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type :then :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defthen step-registered-as "the step {name:string} should be registered as a {type} step"
  [name type]
  (let [step (first (filter #(= name (:name %)) (g/get :steps)))]
    (g/should-not-be-nil step)
    (g/should= (keyword (str/replace type #"^:" "")) (:type step))))

(defthen step-should-match "the step {name:string} should match {text:string}"
  [name text]
  (let [result (g/classify-step (g/get :steps) text)]
    (g/assoc! :classify-result result)
    (g/should-not-be-nil result)
    (g/should= name (:name result))))

(defthen match-args-should-be "the match args should be {args:string}"
  [args]
  (g/should= (edn/read-string args) (:args (g/get :classify-result))))

(defwhen classify-text "classifying {text:string}"
  "Catches ambiguous-match exceptions and stores the error message to :error."
  [text]
  (try
    (g/assoc! :classify-result (g/classify-step (g/get :steps) text))
    (catch Exception e
      (g/assoc! :error (.getMessage e)))))

(defthen no-step-should-match "no step should match"
  []
  (g/should-be-nil (g/get :classify-result)))
