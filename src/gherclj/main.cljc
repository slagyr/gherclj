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
  (let [{:keys [options errors summary arguments]} (cli/parse-opts args cli-options)]
    (cond-> {:options (dissoc options :help)
             :help (:help options)
             :errors errors
             :summary summary}
      (seq arguments) (assoc-in [:options :framework-opts] (vec arguments)))))

(defn usage-message []
  (let [{:keys [summary]} (cli/parse-opts [] cli-options)]
    (str "Usage: gherclj [options]\n\n" summary)))

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
