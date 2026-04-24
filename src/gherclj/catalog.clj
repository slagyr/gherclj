;; mutation-tested: 2026-04-24
(ns gherclj.catalog
  (:refer-clojure :exclude [run!])
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.pipeline :as pipeline]))

(def ^:private ordered-types [[:given "Given:"]
                              [:when "When:"]
                              [:then "Then:"]])

(defn usage-message []
  (str "\nUsage:  gherclj steps [option]...\n\n"
       "List registered step definitions grouped by type.\n\n"
       "  -s, --step-namespaces NS          Step namespace (repeatable, supports globs)\n"
       "      --given                       Show Given steps\n"
       "      --when                        Show When steps\n"
       "      --then                        Show Then steps\n"
       "      --color                       Force ANSI color output\n"
       "      --no-color                    Disable ANSI color output\n"
       "  -h, --help                        Show usage\n"))

(defn- step-text [{:keys [template regex]}]
  (or template (some-> regex str)))

(defn- source-location [{:keys [file line]}]
  (str (.getName (io/file file)) ":" line))

(defn- render-step [{:keys [doc] :as step}]
  (str (step-text step) "  (" (source-location step) ")"
       (when doc
         (str "\n  " doc))))

(defn- type-filter [{:keys [given then] when-flag :when}]
  (let [selected (cond-> #{}
                   given (conj :given)
                   when-flag (conj :when)
                   then (conj :then))]
    (when (seq selected) selected)))

(defn- matches-keyword? [{:keys [doc] :as step} keyword]
  (let [needle (str/lower-case keyword)
        haystacks [(some-> step step-text str/lower-case)
                   (some-> doc str/lower-case)]]
    (some #(and % (str/includes? % needle)) haystacks)))

(defn filter-steps [steps {:keys [keyword types]}]
  (->> steps
       (filter #(or (nil? types) (contains? types (:type %))))
       (filter #(or (nil? keyword) (matches-keyword? % keyword)))
       vec))

(defn render [steps]
  (->> ordered-types
       (map (fn [[step-type header]]
              (let [group-lines (->> steps
                                     (filter #(= step-type (:type %)))
                                     (map render-step))]
                (when (seq group-lines)
                  (str header "\n" (str/join "\n" group-lines))))))
       (remove nil?)
       (str/join "\n\n")))

(defn run! [config args]
  (let [step-namespaces (pipeline/load-step-namespaces! (:step-namespaces config))
        steps (core/collect-steps step-namespaces)
        filtered (filter-steps steps {:keyword (first args)
                                      :types (type-filter config)})]
    (println (render filtered))))
