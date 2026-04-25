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

(defn call-step-renderer
  "Invoke a classified step's renderer with its args (plus optional table or
   doc-string appended) and return whatever the renderer produces — typically
   a Clojure form for Clojure targets, or a target-language string."
  [{:keys [renderer args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))]
    (apply renderer all-args)))

(defn code->string
  "Stringify a step renderer's return value. Forms get pr-str'd; strings pass through."
  [result]
  (if (string? result) result (pr-str result)))

(defmethod fw/render-step :default [_config step]
  (code->string (call-step-renderer step)))

(defn- render-background [config background]
  (when background
    (assoc background :rendered-steps (->> (:steps background)
                                           (filter :classified?)
                                           (mapv #(fw/render-step config %))))))

(defn- render-scenario [config scenario]
  (assoc scenario :rendered-steps (mapv #(fw/render-step config %) (:steps scenario))))

(defn- step-namespaces-used
  "Return the set of step namespace symbols that have at least one step
   matching a step in this feature's background or scenarios. Framework
   adapters get this set in `generate-preamble` and decide what to look up
   from each — Clojure adapters query helper-imports; the rspec adapter
   queries its own file-setup and describe-setup registries."
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
  (let [{:keys [step-namespaces extra-steps exclude-tags include-tags framework]} config
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
            used-nses (step-namespaces-used steps classified-bg filtered)
            rendered-bg (render-background config classified-bg)
            preamble (fw/generate-preamble config source used-nses)
            scenario-blocks (->> classified-scenarios
                                 (map (fn [scenario]
                                        (if (all-classified? scenario)
                                          (fw/wrap-scenario config (render-scenario config scenario) rendered-bg)
                                          (fw/wrap-pending config scenario rendered-bg))))
                                 (str/join "\n\n"))]
        (str preamble "\n\n"
             (fw/wrap-feature config feature scenario-blocks))))))
