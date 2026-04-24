(ns gherclj.features.steps.cli
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.frameworks.speclj :as speclj-fw]
            [gherclj.main :as main]
            [gherclj.config :as config]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defgiven cli-config-file "a config file:"
  "Stores EDN config in :cli-config state. NOT written to disk — used as file-config override when 'running gherclj with' executes."
  [doc-string]
  (g/assoc! :cli-config (edn/read-string doc-string)))

(defgiven generated-specs-dir "generated specs in {dir:string}"
  [dir]
  (g/assoc! :generated-specs-dir dir))

(defn- pipeline-base-dir []
  (some-> (g/get :pipeline-dir)
          io/file
          .getParent))

(defn- absolute-path? [path]
  (.isAbsolute (io/file path)))

(defn- absolutize-under [base-dir path]
  (if (or (nil? path) (absolute-path? path))
    path
    (str (io/file base-dir path))))

(defn- rewrite-sandbox-path-options [args]
  (if-let [base-dir (pipeline-base-dir)]
    (loop [remaining args
           rewritten []
           seen #{}]
      (if-let [token (first remaining)]
        (if (#{"-f" "--features-dir" "-e" "--edn-dir" "-o" "--output-dir"} token)
          (let [value (second remaining)
                path (case token
                       ("-f" "--features-dir") (or (g/get :pipeline-dir) (absolutize-under base-dir value))
                       ("-e" "--edn-dir") (absolutize-under base-dir value)
                       ("-o" "--output-dir") (absolutize-under base-dir value))]
            (recur (nnext remaining)
                   (conj rewritten token path)
                   (conj seen token)))
          (recur (next remaining) (conj rewritten token) seen))
        (let [missing-features? (not (or (contains? seen "-f") (contains? seen "--features-dir")))
              missing-edn? (not (or (contains? seen "-e") (contains? seen "--edn-dir")))
              missing-output? (not (or (contains? seen "-o") (contains? seen "--output-dir")))]
          (cond-> rewritten
            missing-features? (into ["-f" (or (g/get :pipeline-dir) (str (io/file base-dir "features")))])
            missing-edn? (into ["-e" (str (io/file base-dir "target/gherclj/edn"))])
            missing-output? (into ["-o" (str (io/file base-dir "target/gherclj/generated"))])))))
    args))

(defn- with-sandbox-defaults [args]
  (rewrite-sandbox-path-options args))

(defwhen run-gherclj "running gherclj with {args:string}"
  "Invokes main/run in a sandbox. Rewrites -f/-e/-o paths to be relative to the temp pipeline dir. Captures output to :cli-output when pipeline-base-dir exists or when the command is a subcommand or help call."
  [args]
  (let [arg-vec (str/split args #"\s+")
         {:keys [options help errors]} (main/parse-args arg-vec)]
    (cond
      (seq errors)
      (g/assoc! :cli-output (str/join "\n" errors))

      :else
      (let [file-config (or (g/get :cli-config) {})
            cli-overrides (into {} (filter (fn [[_ v]] (some? v))) options)
            merged (merge (config/resolve-config file-config) cli-overrides)
            run-args (with-sandbox-defaults arg-vec)]
        (g/assoc! :loaded-config merged)
        (when (or help (pipeline-base-dir) (= :steps (:subcommand options)))
          (let [previous-framework (g/get :_test-framework)
                output (with-out-str
                         (try
                           (main/run run-args)
                           (catch RuntimeException e
                             (println (.getMessage e)))
                            (finally
                              (when previous-framework
                                (g/set-test-framework! previous-framework)))))]
            (g/assoc! :cli-output output)))))))

(defwhen run-speclj-with-framework-options "speclj runs with framework options {opts:string}"
  [opts]
  (g/assoc! :speclj-run-args
            (speclj-fw/run-args {:output-dir (g/get :generated-specs-dir)
                                 :framework-opts (edn/read-string opts)})))

(defthen framework-should-receive "the framework should receive options {opts:string}"
  [opts]
  (g/should= (edn/read-string opts) (:framework-opts (g/get :loaded-config))))

(defthen speclj-should-receive-args "speclj should receive args {args:string}"
  [args]
  (g/should= (edn/read-string args) (g/get :speclj-run-args)))
