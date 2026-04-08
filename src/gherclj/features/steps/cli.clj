(ns gherclj.features.steps.cli
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.frameworks.speclj :as speclj-fw]
            [gherclj.main :as main]
            [gherclj.config :as config]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defgiven cli-config-file "a config file:"
  [doc-string]
  (g/assoc! :cli-config (edn/read-string doc-string)))

(defgiven generated-specs-dir "generated specs in {dir:string}"
  [dir]
  (g/assoc! :generated-specs-dir dir))

(defwhen run-gherclj "running gherclj with {args:string}"
  [args]
  (let [arg-vec (str/split args #"\s+")
        {:keys [options help errors]} (main/parse-args arg-vec)]
    (cond
      (seq errors)
      (g/assoc! :cli-output (str/join "\n" errors))

      help
      (g/assoc! :cli-output (main/usage-message))

       :else
       (let [file-config (or (g/get :cli-config) {})
             cli-overrides (into {} (filter (fn [[_ v]] (some? v))) options)
             merged (merge (config/resolve-config file-config) cli-overrides)]
         (g/assoc! :loaded-config merged)))))

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
