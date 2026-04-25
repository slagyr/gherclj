(ns gherclj.main
  (:require [clojure.java.io :as io]
             [clojure.string :as str]
             [clojure.tools.cli :as cli]
             [gherclj.catalog :as catalog]
             [gherclj.config :as config]
             [gherclj.framework :as fw]
             [gherclj.unused :as unused]
             [gherclj.pipeline :as pipeline]))

(def version (str/trim (slurp (io/resource "gherclj/VERSION"))))

(def ^:private base-cli-options
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
   ["-t" "--tag TAG" "Generate only the scenarios with the specified tag(s). Repeatable. Use ~ prefix to exclude tags (eg ~slow)."
    :multi true
    :default :none
    :default-desc ""
    :update-fn (fn [acc v] (conj (if (= :none acc) [] acc) v))]
   [nil "--ir-edn" "Persist EDN IR files during full pipeline runs"]
   ["-F" "--framework FRAMEWORK" "Test framework: clojure/speclj, clojure/test, bash/testing, javascript/node-test, ruby/rspec, python/pytest, go/testing, java/junit5, typescript/node-test, rust/rustc-test, csharp/xunit (default: clojure/speclj)"
    :parse-fn keyword]
   ["-v" "--verbose" "Print progress to stdout"]
   ["-h" "--help" "Show usage"]])

;; Subcommand-only flags. Parsed alongside the base set so `gherclj steps
;; --given` etc. work, but excluded from the base `--help` output.
(def ^:private steps-cli-options
  [[nil "--given" "Show Given steps"]
   [nil "--when" "Show When steps"]
   [nil "--then" "Show Then steps"]
   [nil "--color" "Force ANSI color output"]
   [nil "--no-color" "Disable ANSI color output"]])

(def ^:private cli-options (vec (concat base-cli-options steps-cli-options)))

(defn- parse-tag-flags [tags]
  (when (seq tags)
    (let [{excludes true includes false} (group-by #(str/starts-with? % "~") tags)]
      (cond-> {}
              (seq includes) (assoc :include-tags (vec includes))
              (seq excludes) (assoc :exclude-tags (mapv #(subs % 1) excludes))))))

(defn- parse-location-arg [arg]
  (if-let [[_ source line] (re-matches #"^(.+\.feature):(\d+)$" arg)]
    {:source source
     :line   #?(:clj (Long/parseLong line)
                :bb  (Long/parseLong line))}
    (when (str/ends-with? arg ".feature")
      {:source arg})))

(defn- parse-positional-args [arguments]
  (if (#{"steps" "unused"} (first arguments))
    {:locations       []
     :framework-opts  []
     :subcommand      (keyword (first arguments))
     :subcommand-args (vec (rest arguments))}
    (reduce (fn [{:keys [locations framework-opts]} arg]
              (if-let [location (parse-location-arg arg)]
                {:locations      (conj locations location)
                 :framework-opts framework-opts}
                {:locations      locations
                 :framework-opts (conj framework-opts arg)}))
            {:locations [] :framework-opts []}
            arguments)))

(defn parse-args
  "Parse CLI arguments. Returns {:options map :help bool :errors seq}."
  [args]
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args cli-options)
        tags     (let [t (:tag options)] (when-not (= :none t) t))
        tag-opts (parse-tag-flags tags)
        step-ns  (let [s (:step-namespaces options)] (when-not (= :none s) s))
        {:keys [locations framework-opts subcommand subcommand-args]} (parse-positional-args arguments)
        opts     (-> (dissoc options :help :tag :step-namespaces)
                     (merge tag-opts)
                     (cond-> step-ns (assoc :step-namespaces step-ns)
                             (seq locations) (assoc :locations locations)
                             (seq framework-opts) (assoc :framework-opts framework-opts)
                             subcommand (assoc :subcommand subcommand)
                             (seq subcommand-args) (assoc :subcommand-args subcommand-args)))]
    (cond-> {:options opts
             :help    (:help options)
             :errors  errors
             :summary summary})))

(defn usage-message []
  (let [{:keys [summary]} (cli/parse-opts [] base-cli-options)]
    (str "\nGherclj " version " - pronounced /\u0261\u025c\u02d0rk\u0259l/, gur-kull: a Gherkin -> test code transducer.\n"
         "Copyright (c) 2026 Micah Martin under The MIT License.\n\n"
         "Usage:  gherclj [option]... [feature target]... [-- framework option...]\n"
         "        gherclj [option]... <subcommand> [subcommand option]...\n\n"
         "  feature targets  [file|file:line]... The union of all targeted scenarios get run. (default: all scenarios in --features-dir).\n"
         "                   file      all scenarios in the file\n"
         "                   file:line the scenario containing that line in the file\n\n"
         "  subcommands\n"
         "                   gherclj steps        list registered step definitions\n"
         "                   gherclj unused       list registered steps unused by features\n\n"
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
      (do (println (case (:subcommand options)
                     :steps (catalog/usage-message)
                     :unused (unused/usage-message)
                     (usage-message)))
          0)

      :else
      (let [file-config   (config/load-config)
            cli-overrides (into {} (filter (fn [[_ v]] (some? v))) options)
            merged        (merge file-config cli-overrides)]
        (case (:subcommand options)
          :steps (do
                   (catalog/run! merged (or (:subcommand-args options) []))
                   0)
          :unused (do
                    (unused/run! merged (or (:subcommand-args options) []))
                    0)
          (do
            (pipeline/run! merged)
            (let [result (fw/run-specs merged)]
              (if (failures? result) 1 0))))))))

(defn -main [& args]
  (let [exit-code (run args)]
    (when (pos? exit-code)
      #?(:clj (System/exit exit-code)
         :bb  (throw (ex-info "gherclj failed" {:babashka/exit exit-code}))))))
