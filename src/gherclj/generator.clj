(ns gherclj.generator
  (:require [clojure.string :as str]
            [gherclj.core :as core]))

;; --- Framework multimethods ---

(defmulti generate-ns-form
  "Generate the ns declaration string for a feature spec."
  (fn [config & _] (:test-framework config)))

(defmulti wrap-feature
  "Wrap scenario blocks in a feature-level form."
  (fn [config & _] (:test-framework config)))

(defmulti wrap-scenario
  "Wrap step code in a scenario-level form."
  (fn [config & _] (:test-framework config)))

(defmulti wrap-pending
  "Generate a pending/skipped scenario."
  (fn [config & _] (:test-framework config)))

;; --- Step classification ---

(defn classify-scenario
  "Classify all steps in a scenario against registered steps.
   Returns the scenario with each step augmented with classification data."
  [steps scenario]
  (update scenario :steps
          (fn [step-nodes]
            (mapv (fn [node]
                    (if-let [classified (core/classify-step steps (:text node))]
                      (merge node classified {:classified? true})
                      (assoc node :classified? false)))
                  step-nodes))))

(defn- all-classified? [scenario]
  (every? :classified? (:steps scenario)))

;; --- Code generation ---

(defn generate-step-call
  "Generate a string of a qualified function call for a classified step."
  [{:keys [ns name args]}]
  (let [fn-sym (str ns "/" name)
        args-str (str/join " " (map pr-str args))]
    (if (seq args)
      (str "(" fn-sym " " args-str ")")
      (str "(" fn-sym ")"))))

(defn- generate-step-call-with-table
  "Generate a function call, appending the table as the last arg if present."
  [{:keys [ns name args table] :as step}]
  (if table
    (let [fn-sym (str ns "/" name)
          all-args (concat (map pr-str args) [(pr-str table)])
          args-str (str/join " " all-args)]
      (str "(" fn-sym " " args-str ")"))
    (generate-step-call step)))

(defn- step-ns-requires
  "Compute the set of step namespace symbols used in a feature's scenarios."
  [steps scenarios]
  (->> scenarios
       (mapcat :steps)
       (keep (fn [node]
               (when-let [classified (core/classify-step steps (:text node))]
                 (:ns classified))))
       (into #{})))

(defn- source->ns-name
  "Convert a feature source filename to a namespace name."
  [source suffix]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str/replace #"_" "-")
      (str suffix)))

;; --- Public generation ---

(defn generate-spec
  "Generate a complete spec file string from a config and feature IR."
  [config ir]
  (let [{:keys [step-namespaces harness-ns]} config
        {:keys [source feature scenarios background]} ir
        steps (core/collect-steps step-namespaces)
        non-wip (remove :wip scenarios)
        classified-scenarios (mapv #(classify-scenario steps %) non-wip)
        classified-bg (when background (classify-scenario steps background))
        ns-form (generate-ns-form config source
                                  (step-ns-requires steps non-wip)
                                  harness-ns)
        scenario-blocks (->> classified-scenarios
                             (map (fn [scenario]
                                    (if (all-classified? scenario)
                                      (wrap-scenario config scenario classified-bg)
                                      (wrap-pending config scenario classified-bg))))
                             (str/join "\n\n"))]
    (str ns-form "\n\n"
         (wrap-feature config feature scenario-blocks))))
