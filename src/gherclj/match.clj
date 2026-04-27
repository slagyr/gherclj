(ns gherclj.match
  (:refer-clojure :exclude [run!])
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.pipeline :as pipeline]))

(def version (str/trim (slurp (io/resource "gherclj/VERSION"))))

(defn usage-message []
  (str "\nUsage:  gherclj match [option]... <phrase>\n\n"
       "Classify a step phrase against the registered step set.\n\n"
       "  -s, --step-namespaces NS          Step namespace (repeatable, supports globs)\n"
       "      --json                        Emit machine-readable JSON\n"
       "      --edn                         Emit machine-readable EDN\n"
       "      --no-color                    Disable ANSI color output\n"
       "  -h, --help                        Show usage\n"))

(defn parse-phrase
  "Strip a leading Gherkin keyword (Given / When / Then / And / But) from
   the phrase. Matching is type-blind, so the keyword is purely cosmetic
   in the input and is dropped."
  [phrase]
  (str/replace phrase #"^\s*(Given|When|Then|And|But)\s+" ""))

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
  (let [normalized (parse-phrase phrase)
        matches (->> (core/classify-all steps normalized)
                     (sort-by (juxt #(str (:ns %)) :line))
                     (mapv entry-with-values))]
    {:phrase normalized
     :match-status (case (count matches)
                     0 :no-match
                     1 :matched
                     :ambiguous)
     :matches matches}))

(defn build-data [{:keys [phrase match-status matches]}]
  {:gherclj-version version
   :command "match"
   :phrase phrase
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

(defn- type-label [step-type]
  (case step-type
    :given "Given"
    :when "When"
    :then "Then"))

(defn- render-bindings [bindings]
  (if (seq bindings)
    (concat ["Args:"]
            (map (fn [{:keys [name type value]}]
                   (str name " (" type ") = " (pr-str value)))
                 bindings))
    ["Args: none"]))

(defn- render-ambiguous-lines [matches]
  (let [width (+ 2 (apply max (map #(count (:phrase %)) matches)))]
    (map (fn [{:keys [type phrase file line]}]
           (str (type-label type) " " phrase
                (apply str (repeat (- width (count phrase)) " "))
                "(" file ":" line ")"))
         matches)))

(defn- render-single-match [{:keys [type phrase file line helper-ref doc bindings]}]
  (concat ["Matched step:"
           (str "Type:    " (type-label type))
           (str "Pattern: " phrase)
           (str "Source:  " file ":" line)
           (str "Helper:  " helper-ref)]
          (when doc
            [(str "Doc:     " doc)])
          (render-bindings bindings)))

(defn render [{:keys [phrase match-status matches]}]
  (let [header (str "Phrase: " phrase)]
    (case match-status
      :no-match (str header "\nNo matching step.")
      :matched (str/join "\n" (concat [header ""] (render-single-match (first matches))))
      :ambiguous (str/join "\n"
                           (concat [header
                                    (str "Ambiguous — " (count matches) " matching steps:")]
                                   (render-ambiguous-lines matches))))))

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
