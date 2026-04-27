(ns gherclj.match
  (:refer-clojure :exclude [run!])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.pipeline :as pipeline]))

(def version (str/trim (slurp (io/resource "gherclj/VERSION"))))

(def ^:private concrete-types [:given :when :then])

(defn usage-message []
  (str "\nUsage:  gherclj match [option]... <phrase>\n\n"
       "Classify a step phrase against the registered step set.\n\n"
       "  -s, --step-namespaces NS          Step namespace (repeatable, supports globs)\n"
       "      --json                        Emit machine-readable JSON\n"
       "      --edn                         Emit machine-readable EDN\n"
       "      --color                       Force ANSI color output\n"
       "      --no-color                    Disable ANSI color output\n"
       "  -h, --help                        Show usage\n"))

(defn parse-phrase [phrase]
  (let [[head & tail] (str/split phrase #"\s+")
        rest-phrase (str/join " " tail)]
    (case head
      "Given" {:phrase rest-phrase :requested-type :given}
      "When" {:phrase rest-phrase :requested-type :when}
      "Then" {:phrase rest-phrase :requested-type :then}
      "And" {:phrase rest-phrase :requested-type :any}
      "But" {:phrase rest-phrase :requested-type :any}
      {:phrase phrase :requested-type :any})))

(defn- step-phrase [{:keys [template regex]}]
  (or template (some-> regex .pattern)))

(defn- basename [path]
  (.getName (io/file path)))

(defn- entry-with-values [{:keys [type helper-ref ns file line doc bindings args] :as step}]
  {:type type
   :phrase (step-phrase step)
   :regex (not (contains? step :template))
   :ns ns
   :file (basename file)
   :line line
   :helper-ref (str helper-ref)
   :doc doc
   :bindings (mapv (fn [{:keys [name type]} value]
                     {:name name :type type :value value})
                   (or bindings [])
                   (or args []))})

(defn analyze [steps phrase]
  (let [{:keys [phrase requested-type]} (parse-phrase phrase)]
    (if (= :any requested-type)
      (let [grouped (mapv (fn [step-type]
                            [step-type (vec (core/classify-all steps step-type phrase))])
                          concrete-types)
            ambiguous? (some #(> (count (second %)) 1) grouped)
            matches (->> grouped
                         (mapcat second)
                         (sort-by (juxt :type #(str (:ns %)) :line))
                         (mapv entry-with-values))]
        {:phrase phrase
         :requested-type :any
         :match-status (cond
                         ambiguous? :ambiguous
                         (seq matches) :matched
                         :else :no-match)
         :matches matches})
      (let [matches (->> (core/classify-all steps requested-type phrase)
                         (sort-by (juxt #(str (:ns %)) :line))
                         (mapv entry-with-values))]
        {:phrase phrase
         :requested-type requested-type
         :match-status (case (count matches)
                         0 :no-match
                         1 :matched
                         :ambiguous)
         :matches matches}))))

(defn build-data [{:keys [phrase requested-type match-status matches]}]
  {:gherclj-version version
   :command "match"
   :phrase phrase
   :requested-type requested-type
   :match-status match-status
   :matches matches})

(defn- json-ready [value]
  (cond
    (map? value) (into (array-map)
                       (for [k (sort-by name (keys value))]
                         [k (json-ready (get value k))]))
    (vector? value) (mapv json-ready value)
    (keyword? value) (name value)
    (symbol? value) (str value)
    :else value))

(defn render-json [data]
  (json/generate-string (json-ready data) {:pretty true}))

(defn render-edn [data]
  (with-out-str
    (pprint/pprint data)))

(defn- type-label [requested-type]
  (case requested-type
    :given "Given"
    :when "When"
    :then "Then"
    :any "any type"))

(defn- render-bindings [bindings]
  (if (seq bindings)
    (concat ["Args:"]
            (map (fn [{:keys [name type value]}]
                   (str name " (" type ") = " (pr-str value)))
                 bindings))
    ["Args: none"]))

(defn- render-ambiguous-lines [matches]
  (let [width (+ 2 (apply max (map #(count (:phrase %)) matches)))]
    (map (fn [{:keys [phrase file line]}]
           (str phrase (apply str (repeat (- width (count phrase)) " ")) "(" file ":" line ")"))
         matches)))

(defn- render-single-match [prefix {:keys [phrase file line helper-ref doc bindings]}]
  (concat [prefix
           (str "Pattern: " phrase)
           (str "Source:  " file ":" line)
           (str "Helper:  " helper-ref)]
          (when doc
            [(str "Doc:     " doc)])
          (render-bindings bindings)))

(defn render [{:keys [phrase requested-type match-status matches]}]
  (let [header (str "Phrase: " phrase "  (" (type-label requested-type) ")")]
    (case match-status
      :no-match (str header "\nNo matching step.")
      :matched (if (= :any requested-type)
                 (str/join "\n"
                           (concat [header]
                                   (mapcat (fn [[step-type entries]]
                                             (when (seq entries)
                                               (let [entry (first entries)]
                                                 (concat [(str "Matched in " (type-label step-type) ":")]
                                                         (rest (render-single-match "Matched step:" entry))))))
                                           (group-by :type matches))))
                 (str/join "\n" (concat [header] (render-single-match "Matched step:" (first matches)))))
      :ambiguous (if (= :any requested-type)
                   (str/join "\n"
                             (concat [header "Ambiguous matches:"]
                                     (mapcat (fn [[step-type entries]]
                                               (when (> (count entries) 1)
                                                 (concat [(str (type-label step-type) ":")]
                                                         (render-ambiguous-lines entries))))
                                             (group-by :type matches))))
                   (str/join "\n"
                             (concat [header
                                      (str "Ambiguous — " (count matches) " matching " (type-label requested-type) " steps:")]
                                     (render-ambiguous-lines matches)))))))

(defn run! [config args]
  (let [step-namespaces (pipeline/load-step-namespaces! (:step-namespaces config))
        steps (core/collect-steps step-namespaces)
        phrase (str/join " " args)
        data (build-data (analyze steps phrase))]
    (println (cond
               (:json config) (render-json data)
               (:edn config) (render-edn data)
               :else (render data)))
    0))
