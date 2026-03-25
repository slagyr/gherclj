(ns gherclj.main
  (:require [clojure.tools.cli :as cli]
            [clojure.string :as str]
            [gherclj.config :as config]
            [gherclj.pipeline :as pipeline]
            [gherclj.generator :as gen]))

(def ^:private cli-options
  [["-f" "--features-dir DIR" "Features directory"]
   ["-e" "--edn-dir DIR" "EDN IR output directory"]
   ["-o" "--output-dir DIR" "Generated spec output directory"]
   ["-s" "--step-namespaces NS" "Step namespace (repeatable, supports globs)"
    :multi true
    :default nil
    :update-fn (fn [acc v] (conj (or acc []) (symbol v)))]
   ["-t" "--test-framework FRAMEWORK" "Test framework (speclj, clojure.test)"
    :parse-fn keyword]
   ["-v" "--verbose" "Print progress to stdout"]
   ["-h" "--help" "Show usage"]])

(defn parse-args
  "Parse CLI arguments. Returns {:options map :help bool :errors seq}."
  [args]
  (let [{:keys [options errors summary]} (cli/parse-opts args cli-options)]
    {:options (dissoc options :help)
     :help (:help options)
     :errors errors
     :summary summary}))

(defn usage-message []
  (let [{:keys [summary]} (cli/parse-opts [] cli-options)]
    (str "Usage: gherclj [options]\n\n" summary)))

(defn run
  "Execute gherclj with the given args. Returns :ok, :help, or :error.
   Prints output to stdout."
  [args]
  (let [{:keys [options help errors]} (parse-args args)]
    (cond
      (seq errors)
      (do (doseq [e errors] (println e))
          :error)

      help
      (do (println (usage-message))
          :help)

      :else
      (let [file-config (config/load-config)
            cli-overrides (into {} (filter (fn [[_ v]] (some? v))) options)
            merged (merge file-config cli-overrides)]
        (pipeline/run! merged)
        (gen/run-specs merged)
        :ok))))

(defn -main [& args]
  (when (= :error (run args))
    (System/exit 1)))
