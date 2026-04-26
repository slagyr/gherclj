;; mutation-tested: 2026-04-24
(ns gherclj.unused
  (:refer-clojure :exclude [run!])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.parser :as parser]
            [gherclj.pipeline :as pipeline]))

(def version (str/trim (slurp (io/resource "gherclj/VERSION"))))

(def ^:private ordered-types [[:given "Given:"]
                              [:when "When:"]
                              [:then "Then:"]])

(defn usage-message []
  (str "\nUsage:  gherclj unused [option]...\n\n"
       "List registered steps that are never referenced by scanned feature scenarios.\n\n"
        "  -f, --features-dir DIR            Features directory (default: features)\n"
        "  -s, --step-namespaces NS          Step namespace (repeatable, supports globs)\n"
        "  -t, --tag TAG                     Limit scanned scenarios using the normal tag filter semantics\n"
        "      --json                        Emit machine-readable JSON\n"
        "      --edn                         Emit machine-readable EDN\n"
        "  -h, --help                        Show usage\n"))

(defn- pluralize [n singular plural]
  (str n " " (if (= 1 n) singular plural)))

(defn- source-location [{:keys [file line]}]
  (str (.getName (io/file file)) ":" line))

(defn- scenario-included? [{:keys [tags]} {:keys [include-tags exclude-tags]}]
  (let [scenario-tags (set (or tags []))]
    (and (or (empty? exclude-tags)
             (not-any? scenario-tags exclude-tags))
         (or (empty? include-tags)
             (some scenario-tags include-tags)))))

(defn- used-step-texts [{:keys [background scenarios]} filter-config]
  (let [background-texts (mapv :text (:steps background))
        included (filter #(scenario-included? % filter-config) scenarios)
        scanned-texts (mapcat :steps included)
        unscanned-count (- (count scenarios) (count included))]
    {:texts (into background-texts (map :text scanned-texts))
     :scanned-scenarios (count included)
     :unscanned-scenarios unscanned-count}))

(defn- step-used? [step texts]
  (some #(re-matches (:regex step) %) texts))

(defn analyze [steps irs {:keys [include-tags exclude-tags] :as _config}]
  (let [filter-config {:include-tags (vec (or include-tags []))
                       :exclude-tags (vec (or exclude-tags []))}
        usage (map #(used-step-texts % filter-config) irs)
        texts (mapcat :texts usage)
        unused-steps (->> steps
                          (remove #(step-used? % texts))
                          vec)
        total-step-count (count steps)]
    {:scanned-scenarios (reduce + (map :scanned-scenarios usage))
     :unscanned-scenarios (reduce + (map :unscanned-scenarios usage))
     :used-step-count (- total-step-count (count unused-steps))
     :total-step-count total-step-count
     :unused-steps unused-steps
       :filters (into [] (concat (map #(str "~" %) (or exclude-tags []))
                                 (or include-tags [])))}))

(defn- sort-steps [steps]
  (sort-by (juxt #(str (:ns %)) :line) steps))

(defn- step-phrase [{:keys [template regex]}]
  (or template (some-> regex .pattern)))

(defn- step-entry [{:keys [type helper-ref ns file line doc bindings] :as step}]
  {:type type
   :phrase (step-phrase step)
   :regex (not (contains? step :template))
   :helper-ref (str helper-ref)
   :ns ns
   :file file
   :line line
   :doc doc
   :bindings (mapv #(select-keys % [:name :type]) (or bindings []))})

(defn build-data [{:keys [scanned-scenarios unused-steps filters]}]
  {:gherclj-version version
   :command "unused"
   :scenarios-scanned scanned-scenarios
   :tags-applied {:include (vec (remove #(str/starts-with? % "~") filters))
                  :exclude (mapv #(subs % 1) (filter #(str/starts-with? % "~") filters))}
   :unused-steps (->> unused-steps sort-steps (mapv step-entry))})

(defn- json-step-entry [step]
  (-> step
      (assoc :type (name (:type step))
             :ns (str (:ns step)))))

(defn- json-ready [value]
  (cond
    (map? value) (into (array-map)
                       (for [k (sort-by name (keys value))]
                         [k (json-ready (get value k))]))
    (vector? value) (mapv json-ready value)
    :else value))

(defn render-json [data]
  (json/generate-string (json-ready (update data :unused-steps #(mapv json-step-entry %))) {:pretty true}))

(defn render-edn [data]
  (with-out-str
    (pprint/pprint data)))

(defn render [{:keys [scanned-scenarios unscanned-scenarios used-step-count total-step-count unused-steps filters]}]
  (let [total-scenarios (+ scanned-scenarios unscanned-scenarios)
        summary (if (seq filters)
                  (str "Scanned " scanned-scenarios " of " total-scenarios " "
                       (if (= total-scenarios 1) "scenario" "scenarios") ". "
                       (pluralize unscanned-scenarios "scenario" "scenarios")
                       " filtered out by tags: " (str/join ", " filters) ".")
                  (str "Scanned " (pluralize scanned-scenarios "scenario" "scenarios") ". No tag filtering applied."))
        usage-summary (if (empty? unused-steps)
                        (str used-step-count " of " total-step-count " registered steps are in use.")
                        (str used-step-count " of " total-step-count " registered steps are in use ("
                             (count unused-steps) " unused)."))
        grouped-lines (->> ordered-types
                           (mapcat (fn [[step-type header]]
                                     (let [lines (->> unused-steps
                                                      (filter #(= step-type (:type %)))
                                                      (map #(str (or (:template %) (some-> (:regex %) str))
                                                                 "  (" (source-location %) ")")))]
                                       (when (seq lines)
                                         (into [header] lines)))))
                           vec)
        distinct-types (count (into #{} (map :type unused-steps)))
        unused-body (cond
                      (empty? unused-steps) ["No unused steps found."]
                      (= 1 distinct-types) (into ["Unused steps:"]
                                                 (map #(str (or (:template %) (some-> (:regex %) str))
                                                            "  (" (source-location %) ")")
                                                      unused-steps))
                      :else (into ["Unused steps:"] grouped-lines))]
    (str/join "\n" (concat [summary usage-summary] unused-body))))

(defn run! [config _args]
  (let [step-namespaces (pipeline/load-step-namespaces! (:step-namespaces config))
        steps (core/collect-steps step-namespaces)
        irs (parser/parse-features-dir (:features-dir config))
        analysis (analyze steps irs config)
        data (build-data analysis)]
    (println (cond
               (:json config) (render-json data)
               (:edn config) (render-edn data)
               :else (render analysis)))))
