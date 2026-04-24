;; mutation-tested: 2026-03-25
(ns gherclj.generator
  (:require [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.framework :as fw]))

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

(defn ns->alias
  "Extract the last segment of a namespace symbol as an alias string."
  [ns-sym]
  (last (str/split (str ns-sym) #"\.")))

(defn generate-step-call
  "Generate a string of an aliased function call for a classified step."
  [{:keys [ns name args]}]
  (let [fn-sym (str (ns->alias ns) "/" name)
        args-str (str/join " " (map pr-str args))]
    (if (seq args)
      (str "(" fn-sym " " args-str ")")
      (str "(" fn-sym ")"))))

(defn generate-step-call-with-extras
  "Generate a function call, appending table or doc-string as the last arg if present."
  [{:keys [ns name args table doc-string] :as step}]
  (if (or table doc-string)
    (let [fn-sym (str (ns->alias ns) "/" name)
          extra (if table (pr-str table) (pr-str doc-string))
          all-args (concat (map pr-str args) [extra])
          args-str (str/join " " all-args)]
      (str "(" fn-sym " " args-str ")"))
    (generate-step-call step)))

(defn- step-ns-requires
  "Compute the set of step namespace symbols used in a feature's background and scenarios."
  [steps background scenarios]
  (->> (concat (when background (:steps background))
               (mapcat :steps scenarios))
       (keep (fn [node]
               (when-let [classified (core/classify-step steps (:text node))]
                 (:ns classified))))
       (into #{})))

(defn source->ns-name
  "Convert a feature source path to a namespace name."
  [source suffix]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str/replace "/" ".")
      (str/replace "_" "-")
      (str suffix)))

;; --- Public generation ---

(defn generate-spec
  "Generate a complete spec file string from a config and feature IR."
  [config ir]
  (let [{:keys [step-namespaces extra-steps exclude-tags include-tags]} config
        {:keys [source feature scenarios background]} ir
        effective-excludes (vec (or exclude-tags []))
        effective-includes (vec (or include-tags []))
        steps (into (core/collect-steps step-namespaces) extra-steps)
        filtered (cond->> scenarios
                   (seq effective-excludes) (remove #(some (set effective-excludes) (:tags %)))
                   (seq effective-includes) (filter #(some (set effective-includes) (:tags %))))
        classified-scenarios (mapv #(classify-scenario steps %) filtered)]
    (when (seq classified-scenarios)
      (let [classified-bg (when background (classify-scenario steps background))
            preamble (fw/generate-preamble config source
                                           (step-ns-requires steps classified-bg filtered))
            scenario-blocks (->> classified-scenarios
                                 (map (fn [scenario]
                                        (if (all-classified? scenario)
                                          (fw/wrap-scenario config scenario classified-bg)
                                          (fw/wrap-pending config scenario classified-bg))))
                                 (str/join "\n\n"))]
        (str preamble "\n\n"
             (fw/wrap-feature config feature scenario-blocks))))))
