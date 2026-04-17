(ns gherclj.main
  (:require [clojure.java.io :as io]
            [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [gherclj.config :as config]
            [gherclj.pipeline :as pipeline]
            [gherclj.generator :as gen]))

(def version (str/trim (slurp (io/resource "gherclj/VERSION"))))

(def ^:private cli-options
  [["-f" "--features-dir DIR" "Features directory (default: features)"]
   ["-e" "--edn-dir DIR" "EDN IR output directory (default: target/gherclj/edn)"]
   ["-o" "--output-dir DIR" "Generated spec output directory (default: target/gherclj/generated)"]
   ["-s" "--step-namespaces NS" "Step namespace (repeatable, supports globs)"
    :multi true
    :default :none
    :default-desc ""
    :update-fn (fn [acc v]
                 (conj (if (= :none acc) [] acc)
                       (if (re-find #"[*?]" v) v (symbol v))))]
   ["-t" "--tag TAG" "Generate only the scenarios with the specified tag(s). To exclude, prefix the tag with ~ (eg ~slow). Use this option multiple times to filter multiple tags."
    :multi true
    :default :none
    :default-desc ""
    :update-fn (fn [acc v] (conj (if (= :none acc) [] acc) v))]
   ["-T" "--test-framework FRAMEWORK" "Test framework: speclj, clojure.test (default: speclj)"
    :parse-fn keyword]
   ["-v" "--verbose" "Print progress to stdout"]
   ["-h" "--help" "Show usage"]])

(defn- parse-tag-flags [tags]
  (when (seq tags)
    (let [{excludes true includes false} (group-by #(str/starts-with? % "~") tags)]
      (cond-> {}
        (seq includes) (assoc :include-tags (vec includes))
        (seq excludes) (assoc :exclude-tags (mapv #(subs % 1) excludes))))))

(defn- parse-location-arg [arg]
  (if-let [[_ source line] (re-matches #"^(.+\.feature):(\d+)$" arg)]
    {:source source
     :line #?(:clj (Long/parseLong line)
              :bb (Long/parseLong line))}
    (when (str/ends-with? arg ".feature")
      {:source arg})))

(defn- parse-positional-args [arguments]
  (reduce (fn [{:keys [locations framework-opts]} arg]
            (if-let [location (parse-location-arg arg)]
              {:locations (conj locations location)
               :framework-opts framework-opts}
              {:locations locations
               :framework-opts (conj framework-opts arg)}))
          {:locations [] :framework-opts []}
          arguments))

(defn parse-args
  "Parse CLI arguments. Returns {:options map :help bool :errors seq}."
  [args]
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args cli-options)
        tags (let [t (:tag options)] (when-not (= :none t) t))
        tag-opts (parse-tag-flags tags)
        step-ns (let [s (:step-namespaces options)] (when-not (= :none s) s))
        {:keys [locations framework-opts]} (parse-positional-args arguments)
        opts (-> (dissoc options :help :tag :step-namespaces)
                  (merge tag-opts)
                 (cond-> step-ns (assoc :step-namespaces step-ns)
                         (seq locations) (assoc :locations locations)
                         (seq framework-opts) (assoc :framework-opts framework-opts)))]
    (cond-> {:options opts
             :help (:help options)
             :errors errors
             :summary summary})))

(defn usage-message []
  (let [{:keys [summary]} (cli/parse-opts [] cli-options)]
    (str "\nGherclj " version " - pronounced /\u0261\u025c\u02d0rk\u0259l/, gur-kull: a Gherkin -> test code transducer.\n"
         "Copyright (c) 2026 Micah Martin under The MIT License.\n\n"
         "Usage:  gherclj [option]... [feature target]... [-- framework option...]\n\n"
         "  feature targets  [file|file:line]... The union of all targeted scenarios get run. (default: all scenarios in --features-dir).\n"
         "                   file      all scenarios in the file\n"
         "                   file:line the scenario containing that line in the file\n\n"
         summary "\n")))

(defn- failures? [run-specs-result]
  (cond
    (number? run-specs-result) (pos? run-specs-result)
    (map? run-specs-result) (pos? (+ (:fail run-specs-result 0)
                                     (:error run-specs-result 0)))
    :else false))

(defn run
  "Execute gherclj with the given args. Returns exit code (0 = success)."
  [args]
  (let [{:keys [options help errors]} (parse-args args)]
    (cond
      (seq errors)
      (do (doseq [e errors] (println e))
          1)

      help
      (do (println (usage-message))
          0)

      :else
      (let [file-config (config/load-config)
            cli-overrides (into {} (filter (fn [[_ v]] (some? v))) options)
            merged (merge file-config cli-overrides)]
        (pipeline/run! merged)
        (let [result (gen/run-specs merged)]
          (if (failures? result) 1 0))))))

(defn -main [& args]
  (let [exit-code (run args)]
    (when (pos? exit-code)
      #?(:clj (System/exit exit-code)
         :bb (throw (ex-info "gherclj failed" {:babashka/exit exit-code}))))))
