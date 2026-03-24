(ns gherclj.features.harness
  (:refer-clojure :exclude [reset!])
  (:require [gherclj.template :as template]
            [gherclj.core :as core]
            [gherclj.generator :as gen]
            [gherclj.frameworks.speclj]))

(def ^:private state (atom nil))

(defn reset! []
  (clojure.core/reset! state
    {:steps []
     :compiled nil
     :match-result nil
     :classify-result nil
     :feature-ir nil
     :generated-output nil}))

;; --- Template state ---

(defn set-template! [t]
  (swap! state assoc :template-str t))

(defn compile-template! []
  (swap! state assoc :compiled (template/compile-template (:template-str @state))))

(defn compiled [] (:compiled @state))

(defn match-text! [text]
  (swap! state assoc :match-result (template/match-step (compiled) text)))

(defn match-result [] (:match-result @state))

(defn regex-str [] (str (:regex (compiled))))

(defn binding-count [] (count (:bindings (compiled))))

;; --- Step registration state ---

(defn registered-steps [] (:steps @state))

(defn register-test-step! [step-type step-name template]
  (let [compiled (template/compile-template template)
        entry {:name step-name
               :type step-type
               :ns 'gherclj.features.harness
               :template template
               :regex (:regex compiled)
               :bindings (:bindings compiled)}]
    (swap! state update :steps conj entry)))

(defn find-step [step-name]
  (first (filter #(= step-name (:name %)) (registered-steps))))

(defn register-real-step!
  "Look up a step by name from the real core registry and add it to harness state."
  [ns-sym step-name]
  (let [steps (core/steps-in-ns ns-sym)
        step (first (filter #(= step-name (:name %)) steps))]
    (when step
      (swap! state update :steps conj step))))

(defn classify-text! [text]
  (swap! state assoc :classify-result (core/classify-step (registered-steps) text)))

(defn classify-result [] (:classify-result @state))

;; --- Code generation state ---

(defn set-feature! [feature-name source]
  (swap! state assoc :feature-ir {:feature feature-name :source source :scenarios []}))

(defn add-scenario! [title steps]
  (swap! state update-in [:feature-ir :scenarios] conj
         {:scenario title :steps steps}))

(defn generate-spec! [framework step-namespaces]
  (let [config {:step-namespaces step-namespaces
                :harness-ns 'gherclj.features.harness
                :test-framework framework}]
    (swap! state assoc :generated-output (gen/generate-spec config (:feature-ir @state)))))

(defn generated-output [] (:generated-output @state))
