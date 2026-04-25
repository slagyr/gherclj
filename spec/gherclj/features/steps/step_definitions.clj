(ns gherclj.features.steps.step-definitions
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.template :as template]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(helper! gherclj.features.steps.step-definitions)

(defn- compile-and-store! [step-type name template]
  (let [compiled (template/compile-template template)
        entry {:name name :type step-type :ns 'gherclj.features.steps.step-definitions
               :template template :regex (:regex compiled) :bindings (:bindings compiled)}]
    (g/update! :steps (fnil conj []) entry)))

(defn define-given-step! [name template]
  (compile-and-store! :given name template))

(defn define-when-step! [name template]
  (compile-and-store! :when name template))

(defn define-then-step! [name template]
  (compile-and-store! :then name template))

(defn step-registered-as [name type]
  (let [step (first (filter #(= name (:name %)) (g/get :steps)))]
    (g/should-not-be-nil step)
    (g/should= (keyword (str/replace type #"^:" "")) (:type step))))

(defn step-should-match [name text]
  (let [result (g/classify-step (g/get :steps) text)]
    (g/assoc! :classify-result result)
    (g/should-not-be-nil result)
    (g/should= name (:name result))))

(defn match-args-should-be [args]
  (g/should= (edn/read-string args) (:args (g/get :classify-result))))

(defn classify-text! [text]
  (try
    (g/assoc! :classify-result (g/classify-step (g/get :steps) text))
    (catch Exception e
      (g/assoc! :error (.getMessage e)))))

(defn no-step-should-match []
  (g/should-be-nil (g/get :classify-result)))

(defgiven "a given step named {name:string} with template {template:string}" step-definitions/define-given-step!
  "Creates a step entry in :steps state (not the global registry). Uses a fake ns symbol.")

(defgiven "a when step named {name:string} with template {template:string}" step-definitions/define-when-step!
  "Creates a step entry in :steps state (not the global registry). Uses a fake ns symbol.")

(defgiven "a then step named {name:string} with template {template:string}" step-definitions/define-then-step!
  "Creates a step entry in :steps state (not the global registry). Uses a fake ns symbol.")

(defthen "the step {name:string} should be registered as a {type} step" step-definitions/step-registered-as)

(defthen "the step {name:string} should match {text:string}" step-definitions/step-should-match)

(defthen "the match args should be {args:string}" step-definitions/match-args-should-be)

(defwhen "classifying {text:string}" step-definitions/classify-text!
  "Catches ambiguous-match exceptions and stores the error message to :error.")

(defthen "no step should match" step-definitions/no-step-should-match)
