(ns gherclj.features.steps.step-definitions
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.template :as template]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [speclj.core :refer [should= should-not-be-nil should-be-nil]]))

(defgiven define-given-step "a given step named {name:string} with template {template:string}"
  [name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type :given :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defgiven define-when-step "a when step named {name:string} with template {template:string}"
  [name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type :when :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defgiven define-then-step "a then step named {name:string} with template {template:string}"
  [name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type :then :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defthen step-registered-as "the step {name:string} should be registered as a {type} step"
  [name type]
  (let [step (first (filter #(= name (:name %)) (g/get :steps)))]
    (should-not-be-nil step)
    (should= (keyword (str/replace type #"^:" "")) (:type step))))

(defthen step-should-match "the step {name:string} should match {text:string}"
  [name text]
  (let [result (g/classify-step (g/get :steps) text)]
    (g/assoc! :classify-result result)
    (should-not-be-nil result)
    (should= name (:name result))))

(defthen match-args-should-be "the match args should be {args:string}"
  [args]
  (should= (edn/read-string args) (:args (g/get :classify-result))))

(defwhen classify-text "classifying {text:string}"
  [text]
  (g/assoc! :classify-result (g/classify-step (g/get :steps) text)))

(defthen no-step-should-match "no step should match"
  []
  (should-be-nil (g/get :classify-result)))
