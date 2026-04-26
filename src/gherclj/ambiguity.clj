(ns gherclj.ambiguity
  (:refer-clojure :exclude [run!])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.pipeline :as pipeline]))

(def version (str/trim (slurp (io/resource "gherclj/VERSION"))))

(def ^:private step-keywords ["Given" "When" "Then" "And" "But"])

(defn usage-message []
  (str "\nUsage:  gherclj ambiguity [option]...\n\n"
       "List ambiguous step phrases found across scanned feature scenarios.\n\n"
       "  -f, --features-dir DIR            Features directory (default: features)\n"
       "  -s, --step-namespaces NS          Step namespace (repeatable, supports globs)\n"
       "  -t, --tag TAG                     Limit scanned scenarios using the normal tag filter semantics\n"
       "      --json                        Emit machine-readable JSON\n"
       "      --edn                         Emit machine-readable EDN\n"
       "      --color                       Force ANSI color output\n"
       "      --no-color                    Disable ANSI color output\n"
       "  -h, --help                        Show usage\n"))

(defn- tag-line? [trimmed]
  (str/starts-with? trimmed "@"))

(defn- parse-tags [trimmed]
  (->> (str/split trimmed #"\s+")
       (filter #(str/starts-with? % "@"))
       (mapv #(subs % 1))))

(defn- step-keyword? [trimmed]
  (some #(str/starts-with? trimmed (str % " ")) step-keywords))

(defn- strip-keyword [trimmed]
  (subs trimmed (inc (str/index-of trimmed " "))))

(defn- scenario-included? [{:keys [tags]} {:keys [include-tags exclude-tags]}]
  (let [scenario-tags (set (or tags []))]
    (and (or (empty? exclude-tags)
             (not-any? scenario-tags exclude-tags))
         (or (empty? include-tags)
             (some scenario-tags include-tags)))))

(defn- scan-feature-file [root-dir file {:keys [include-tags exclude-tags] :as filter-config}]
  (let [lines (str/split-lines (slurp file))
        rel-path (str (.relativize (.toPath (io/file root-dir)) (.toPath file)))]
    (loop [indexed (map-indexed vector lines)
           state {:feature-tags []
                  :pending-tags []
                  :section :start
                  :in-doc-string false
                  :scanned-scenarios 0
                  :total-scenarios 0
                  :current-included? false
                  :background-occurrences []
                  :occurrences []}]
      (if-let [[idx line] (first indexed)]
        (let [trimmed (str/trim line)
              line-number (inc idx)]
          (cond
            (= trimmed "\"\"\"")
            (recur (rest indexed) (update state :in-doc-string not))

            (:in-doc-string state)
            (recur (rest indexed) state)

            (tag-line? trimmed)
            (recur (rest indexed) (update state :pending-tags into (parse-tags trimmed)))

            (str/starts-with? trimmed "Feature:")
            (recur (rest indexed) (-> state
                                      (assoc :feature-tags (:pending-tags state))
                                      (assoc :pending-tags [])
                                      (assoc :section :description)))

            (str/starts-with? trimmed "Background:")
            (recur (rest indexed) (-> state
                                      (assoc :section :background)
                                      (assoc :pending-tags [])))

            (or (str/starts-with? trimmed "Scenario:")
                (str/starts-with? trimmed "Scenario Outline:"))
            (let [tags (into (:feature-tags state) (:pending-tags state))
                  included? (scenario-included? {:tags tags} filter-config)]
              (recur (rest indexed)
                     (-> state
                         (update :total-scenarios inc)
                         (update :scanned-scenarios #(+ % (if included? 1 0)))
                         (assoc :current-included? included?)
                         (assoc :pending-tags [])
                         (assoc :section :scenario))))

            (str/starts-with? trimmed "Examples:")
            (recur (rest indexed) (assoc state :section :examples))

            (and (= :background (:section state)) (step-keyword? trimmed))
            (recur (rest indexed)
                   (update state :background-occurrences conj {:phrase (strip-keyword trimmed)
                                                              :feature-file rel-path
                                                              :line line-number}))

            (and (= :scenario (:section state)) (:current-included? state) (step-keyword? trimmed))
            (recur (rest indexed)
                   (update state :occurrences conj {:phrase (strip-keyword trimmed)
                                                   :feature-file rel-path
                                                   :line line-number}))

            :else
            (recur (rest indexed) state)))
        (let [occurrences (if (pos? (:scanned-scenarios state))
                            (into (:background-occurrences state) (:occurrences state))
                            (:occurrences state))]
          {:scanned-scenarios (:scanned-scenarios state)
           :unscanned-scenarios (- (:total-scenarios state) (:scanned-scenarios state))
           :occurrences occurrences})))))

(defn- step-phrase [{:keys [template regex]}]
  (or template (some-> regex .pattern)))

(defn- sort-steps [steps]
  (sort-by (juxt #(str (:ns %)) :line) steps))

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

(defn analyze [steps features-dir config]
  (let [filter-config {:include-tags (vec (or (:include-tags config) []))
                       :exclude-tags (vec (or (:exclude-tags config) []))}
        files (->> (file-seq (io/file features-dir))
                   (filter #(str/ends-with? (.getName %) ".feature"))
                   (sort-by #(str (.toPath %))))
        scans (map #(scan-feature-file features-dir % filter-config) files)
        ambiguities (->> (mapcat :occurrences scans)
                         (keep (fn [{:keys [phrase] :as occurrence}]
                                 (let [matches (vec (core/classify-all steps phrase))]
                                   (when (> (count matches) 1)
                                     (assoc occurrence :matches (vec (sort-steps matches)))))))
                         (sort-by (juxt :feature-file :line))
                         vec)]
    {:scanned-scenarios (reduce + (map :scanned-scenarios scans))
     :unscanned-scenarios (reduce + (map :unscanned-scenarios scans))
     :ambiguities ambiguities
     :filters (into [] (concat (map #(str "~" %) (:exclude-tags filter-config))
                               (:include-tags filter-config)))}))

(defn build-data [{:keys [scanned-scenarios ambiguities filters]}]
  {:gherclj-version version
   :command "ambiguity"
   :scenarios-scanned scanned-scenarios
   :tags-applied {:include (vec (remove #(str/starts-with? % "~") filters))
                  :exclude (mapv #(subs % 1) (filter #(str/starts-with? % "~") filters))}
   :ambiguous-count (count ambiguities)
   :ambiguities (mapv (fn [{:keys [matches] :as ambiguity}]
                        (assoc ambiguity :matches (mapv step-entry matches)))
                      ambiguities)})

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
  (json/generate-string
   (json-ready
    (update data :ambiguities
            #(mapv (fn [entry]
                     (update entry :matches (fn [matches] (mapv json-step-entry matches))))
                   %)))
   {:pretty true}))

(defn render-edn [data]
  (with-out-str
    (pprint/pprint data)))

(defn- pluralize [n singular plural]
  (str n " " (if (= 1 n) singular plural)))

(defn render [{:keys [scanned-scenarios unscanned-scenarios ambiguities filters]}]
  (let [total-scenarios (+ scanned-scenarios unscanned-scenarios)
        summary (if (seq filters)
                  (str "Scanned " scanned-scenarios " of " total-scenarios " "
                       (if (= total-scenarios 1) "scenario" "scenarios") ". "
                       (pluralize unscanned-scenarios "scenario" "scenarios")
                       " filtered out by tags: " (str/join ", " filters) ".")
                  (str "Scanned " (pluralize scanned-scenarios "scenario" "scenarios") ". No tag filtering applied."))]
    (if (empty? ambiguities)
      (str summary "\nNo ambiguous step phrases found.")
      (str/join "\n"
                (concat [summary "Ambiguous step phrases:"]
                        (mapcat (fn [{:keys [phrase feature-file line matches]}]
                                  (concat [(str phrase "  (" feature-file ":" line ")")
                                           "Matches:"]
                                          (map #(str (step-phrase %) "    (" (.getName (io/file (:file %))) ":" (:line %) ")") matches)
                                          [""]))
                                ambiguities))))))

(defn run! [config _args]
  (let [step-namespaces (pipeline/load-step-namespaces! (:step-namespaces config))
        steps (core/collect-steps step-namespaces)
        analysis (analyze steps (:features-dir config) config)
        data (build-data analysis)]
    (println (cond
               (:json config) (render-json data)
               (:edn config) (render-edn data)
               :else (render analysis)))
    (if (seq (:ambiguities analysis)) 1 0)))
