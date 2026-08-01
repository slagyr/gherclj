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
                      ;; Keep feature-file :line/:text/:table from the IR node.
                      ;; Step-definition location is preserved as :def-file/:def-line
                      ;; so it never masquerades as feature provenance.
                      (merge (dissoc classified :line :file)
                             node
                             {:classified? true
                              :def-file (:file classified)
                              :def-line (:line classified)})
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

(defn- step-keyword-label [type]
  (case type
    :given "Given"
    :when  "When"
    :then  "Then"
    :and   "And"
    :but   "But"
    (str/capitalize (name type))))

(defn step-label
  "Human-readable Gherkin step label, e.g. \"Then the status is 200\"."
  [{:keys [type text]}]
  (str (step-keyword-label type) " " text))

(defn step-location
  "feature-path or feature-path:line for a step."
  [config {:keys [line]}]
  (let [source (or (:_source config) "unknown")]
    (if line (str source ":" line) source)))

(defn comment-prefix
  "Line-comment introducer for the active framework's target language."
  [framework]
  (case framework
    (:clojure/speclj :clojure/test) ";;"
    (:ruby/rspec :bash/testing :python/pytest) "#"
    ;; javascript, typescript, java, csharp, rust, go, default
    "//"))

(defn provenance-comment-lines
  "Comment lines annotating a step (and optional table rows) with feature location."
  [config step]
  (let [source (or (:_source config) "unknown")
        cmt (comment-prefix (:framework config))
        head (str cmt " " (step-label step) "  (" (step-location config step) ")")
        table (:table step)]
    (if-not table
      [head]
      (let [headers (:headers table)
            header-line (:header-line table)
            row-lines (:row-lines table)
            header-comment
            (when headers
              (str cmt "   | " (str/join " | " headers) " |"
                   (when header-line (str "  (" source ":" header-line ")"))))
            row-comments
            (map-indexed
              (fn [i row]
                (let [rl (get row-lines i)]
                  (str cmt "   | " (str/join " | " row) " |"
                       (when rl (str "  (" source ":" rl ")")))))
              (:rows table))]
        (into [head]
              (concat (when header-comment [header-comment])
                      row-comments))))))

(defn- clojure-framework? [framework]
  (contains? #{:clojure/speclj :clojure/test} framework))

(defn- wrap-with-step [config step call-str]
  (let [label (step-label step)
        source (or (:_source config) "unknown")
        line (:line step)]
    (str "(g/with-step* " (pr-str label) " " (pr-str source) " " (pr-str line)
         " (fn [] " call-str "))")))

(defn render-step-lines
  "Render a step as a sequence of code lines: provenance comments, then the call.
   Clojure frameworks wrap the call in g/with-step* for runtime failure context."
  [config step]
  (let [call (fw/render-step config step)
        call (if (clojure-framework? (:framework config))
               (wrap-with-step config step call)
               call)]
    (concat (provenance-comment-lines config step) [call])))

(defn- render-background [config background]
  (when background
    (assoc background :rendered-steps (->> (:steps background)
                                           (filter :classified?)
                                           (mapcat #(render-step-lines config %))
                                           vec))))

(defn- render-scenario [config scenario]
  (assoc scenario :rendered-steps (->> (:steps scenario)
                                       (mapcat #(render-step-lines config %))
                                       vec)))

(defn- merge-backgrounds
  "Feature Background steps, then Rule Background steps (Cucumber order)."
  [feature-bg rule-bg]
  (let [fsteps (or (:steps feature-bg) [])
        rsteps (or (:steps rule-bg) [])]
    (when (or (seq fsteps) (seq rsteps))
      {:steps (into (vec fsteps) rsteps)})))

(defn- effective-background
  "Background applied to a scenario: feature-level then that scenario's rule background."
  [feature-bg scenario]
  (merge-backgrounds feature-bg (:rule-background scenario)))

(defn- step-namespaces-used
  "Return the set of step namespace symbols that have at least one step
   matching a step in this feature's background, rule backgrounds, or scenarios."
  [feature-bg scenarios]
  (->> (concat (when feature-bg (:steps feature-bg))
               (mapcat (fn [sc]
                         (concat (get-in sc [:rule-background :steps])
                                 (:steps sc)))
                       scenarios))
       (keep :ns)
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
        classified-scenarios
        (mapv (fn [sc]
                (let [sc (classify-scenario steps sc)]
                  (if-let [rb (:rule-background sc)]
                    (assoc sc :rule-background (classify-scenario steps rb))
                    sc)))
              filtered)]
    (when (seq classified-scenarios)
      (let [classified-bg (when background (classify-scenario steps background))
            used-nses     (step-namespaces-used classified-bg classified-scenarios)
            ;; Make used-nses and source available to all framework adapter
            ;; calls. Per-scenario setup registries (e.g. go.testing) look up
            ;; their declarations via :_used-nses; Java derives class names
            ;; from :_source since wrap-feature doesn't otherwise see it.
            config        (assoc config :_used-nses used-nses :_source source)
            preamble      (fw/generate-preamble config source used-nses)
            scenario-blocks
            (->> classified-scenarios
                 (map (fn [scenario]
                        (let [eff-bg (render-background
                                       config
                                       (effective-background classified-bg scenario))]
                          (if (and (all-classified? scenario)
                                   (or (nil? (:rule-background scenario))
                                       (all-classified? (:rule-background scenario))))
                            (fw/wrap-scenario config (render-scenario config scenario) eff-bg)
                            (fw/wrap-pending config scenario eff-bg)))))
                 (str/join "\n\n"))]
        (str preamble "\n\n"
             (fw/wrap-feature config feature scenario-blocks))))))
