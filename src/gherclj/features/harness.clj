(ns gherclj.features.harness
  (:refer-clojure :exclude [reset!])
  (:require [gherclj.template :as template]
            [gherclj.core :as core]
            [gherclj.generator :as gen]
            [gherclj.frameworks.speclj]
            [gherclj.frameworks.clojure-test]
            [gherclj.pipeline :as pipeline]
            [gherclj.parser :as parser]
            [clojure.java.io :as io]))

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

(defn add-wip-scenario! [title steps]
  (swap! state update-in [:feature-ir :scenarios] conj
         {:scenario title :steps steps :wip true}))

(defn set-background! [steps]
  (swap! state assoc-in [:feature-ir :background]
         {:steps steps}))

(defn generate-spec! [framework step-namespaces]
  (let [config {:step-namespaces step-namespaces
                :test-framework framework}]
    (swap! state assoc :generated-output (gen/generate-spec config (:feature-ir @state)))))

(defn generated-output [] (:generated-output @state))

;; --- Parsing state ---

(defn set-raw-feature! [text]
  (swap! state assoc :raw-feature text))

(defn parse-raw-feature! []
  (swap! state assoc :parsed-ir (parser/parse-feature (:raw-feature @state))))

(defn parsed-ir [] (:parsed-ir @state))

;; --- Pipeline state ---

(defn set-pipeline-dir! [dir]
  (swap! state assoc :pipeline-dir dir))

(defn pipeline-dir [] (:pipeline-dir @state))

(defn pipeline-base-dir []
  (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test"))

(defn- pipeline-edn-dir []
  (str (pipeline-base-dir) "/target/gherclj/edn"))

(defn- pipeline-output-dir []
  (str (pipeline-base-dir) "/target/gherclj/generated"))

(defn- pipeline-config [& {:keys [verbose framework]}]
  (cond-> {:features-dir (:pipeline-dir @state)
           :edn-dir (pipeline-edn-dir)
           :output-dir (pipeline-output-dir)}
    verbose (assoc :verbose true)
    framework (assoc :test-framework framework
                     :step-namespaces [])))

(defn run-parse-stage! []
  (let [output (with-out-str (pipeline/parse! (pipeline-config)))]
    (swap! state assoc :pipeline-output output)))

(defn run-parse-stage-verbose! []
  (let [output (with-out-str (pipeline/parse! (pipeline-config :verbose true)))]
    (swap! state assoc :pipeline-output output)))

(defn run-generate-stage! [framework]
  (let [output (with-out-str (pipeline/generate! (pipeline-config :framework framework)))]
    (swap! state assoc :pipeline-output output)))

(defn run-generate-stage-verbose! [framework]
  (let [output (with-out-str (pipeline/generate! (pipeline-config :framework framework :verbose true)))]
    (swap! state assoc :pipeline-output output)))

(defn run-full-pipeline! [framework]
  (let [output (with-out-str (pipeline/run! (pipeline-config :framework framework)))]
    (swap! state assoc :pipeline-output output)))

(defn pipeline-output [] (:pipeline-output @state))

(defn cleanup-pipeline! []
  (let [base (io/file (pipeline-base-dir))]
    (when (.exists base)
      (doseq [f (reverse (file-seq base))]
        (.delete f)))))
