(ns gherclj.features.steps.cli
  (:require [cheshire.core :as json]
            [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
             [gherclj.frameworks.clojure.speclj :as speclj-fw]
             [gherclj.main :as main]
             [gherclj.config :as config]
             [clojure.edn :as edn]
             [clojure.java.io :as io]
            [clojure.string :as str]))

(helper! gherclj.features.steps.cli)

(defn cli-config-file! [doc-string]
  (g/assoc! :cli-config (edn/read-string doc-string)))

(defn generated-specs-dir! [dir]
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

(defn run-gherclj! [args]
  (let [arg-vec (str/split args #"\s+")
        {:keys [options help errors]} (main/parse-args arg-vec)
        file-config (or (g/get :cli-config) {})
        cli-overrides (into {} (filter (fn [[_ v]] (some? v))) options)
        merged (merge (config/resolve-config file-config) cli-overrides)
        run-args (with-sandbox-defaults arg-vec)]
    (when-not (seq errors)
      (g/assoc! :loaded-config merged))
    (when (or help (pipeline-base-dir) (= :steps (:subcommand options)) (= :unused (:subcommand options)) (= :ambiguity (:subcommand options)) (seq errors))
      (let [previous-framework (g/get :_framework)
            stdout (java.io.StringWriter.)
            stderr (java.io.StringWriter.)
            exit-code (binding [*out* stdout *err* stderr]
                        (try
                          (main/run run-args)
                          (catch RuntimeException e
                            (println (.getMessage e))
                            1)
                          (finally
                            (when previous-framework
                              (g/set-framework! previous-framework)))))]
        (g/assoc! :cli-output (str stdout)
                  :cli-error-output (str stderr)
                  :cli-exit-code exit-code)))))

(defn run-speclj-with-framework-options! [opts]
  (g/assoc! :speclj-run-args
            (speclj-fw/run-args {:output-dir (g/get :generated-specs-dir)
                                 :framework-opts (edn/read-string opts)})))

(defn framework-should-receive [opts]
  (g/should= (edn/read-string opts) (:framework-opts (g/get :loaded-config))))

(defn speclj-should-receive-args [args]
  (g/should= (edn/read-string args) (g/get :speclj-run-args)))

(defn output-should-be-valid-edn []
  (g/assoc! :cli-edn-output (edn/read-string (g/get :cli-output ""))))

(defn output-should-be-valid-json []
  (g/assoc! :cli-json-output (json/parse-string (g/get :cli-output "") keyword)))

(defn output-should-span-multiple-lines []
  (g/should (>= (count (str/split-lines (g/get :cli-output ""))) 2)))

(defn- table->lookup [table]
  (into {} (map (fn [[field value]] [field value]) (:rows table))))

(defn- parse-expected-value [value]
  (cond
    (= "true" value) true
    (= "false" value) false
    (str/starts-with? value ":") (keyword (subs value 1))
    (re-matches #"^-?\d+$" value) (Long/parseLong value)
    :else value))

(defn- machine-output-step-matches? [step expected]
  (every? (fn [[field value]]
            (= (parse-expected-value value) (get step (keyword field))))
          expected))

(defn edn-output-should-include-step-with [table]
  (let [steps (:steps (or (g/get :cli-edn-output) (edn/read-string (g/get :cli-output ""))))]
    (g/should (some #(machine-output-step-matches? % (table->lookup table)) steps))))

(defn json-output-should-include-step-with [table]
  (let [steps (:steps (or (g/get :cli-json-output) (json/parse-string (g/get :cli-output "") keyword)))]
    (g/should (some #(machine-output-step-matches? % (table->lookup table)) steps))))

(defn every-step-entry-in-json-output-has-type [step-type]
  (let [steps (:steps (or (g/get :cli-json-output) (json/parse-string (g/get :cli-output "") keyword)))]
    (doseq [step steps]
      (g/should= step-type (:type step)))))

(defn edn-report-should-include [table]
  (let [report (or (g/get :cli-edn-output) (edn/read-string (g/get :cli-output "")))]
    (doseq [[field value] (:rows table)]
      (g/should= (parse-expected-value value) (get report (keyword field))))))

(defn json-report-should-include [table]
  (let [report (or (g/get :cli-json-output) (json/parse-string (g/get :cli-output "") keyword))]
    (doseq [[field value] (:rows table)]
      (g/should= (parse-expected-value value) (get report (keyword field))))))

(defn unused-steps-list-should-contain-step-with-phrase [phrase]
  (let [report (or (g/get :cli-edn-output)
                   (g/get :cli-json-output)
                   (edn/read-string (g/get :cli-output "")))
        steps (:unused-steps report)]
    (g/should (some #(= phrase (:phrase %)) steps))))

(defn- unescape-quoted-text [text]
  (str/replace text #"\\\"" "\""))

(defn ambiguities-list-should-contain-entry-with-phrase [phrase]
  (let [report (or (g/get :cli-edn-output)
                   (g/get :cli-json-output)
                   (edn/read-string (g/get :cli-output "")))
        entries (:ambiguities report)
        expected (unescape-quoted-text phrase)]
    (g/should (some #(= expected (:phrase %)) entries))))

(defn exit-code-should-be-zero []
  (g/should= 0 (or (g/get :cli-exit-code) 0)))

(defn exit-code-should-be-non-zero []
  (g/should (pos? (or (g/get :cli-exit-code) 0))))

(defn error-output-should-mention [text]
  (g/should (str/includes? (g/get :cli-error-output "") text)))

(defgiven "a config file:" cli/cli-config-file!
  "Stores EDN config in :cli-config state. NOT written to disk — used as file-config override when 'running gherclj with' executes.")

(defgiven "generated specs in {dir:string}" cli/generated-specs-dir!)

(defwhen "running gherclj with {args:string}" cli/run-gherclj!
  "Invokes main/run in a sandbox. Rewrites -f/-e/-o paths to be relative to the temp pipeline dir. Captures output to :cli-output when pipeline-base-dir exists or when the command is a subcommand or help call.")

(defwhen "speclj runs with framework options {opts:string}" cli/run-speclj-with-framework-options!)

(defthen "the framework should receive options {opts:string}" cli/framework-should-receive)

(defthen "speclj should receive args {args:string}" cli/speclj-should-receive-args)

(defthen "the output should be valid EDN" cli/output-should-be-valid-edn)

(defthen "the output should be valid JSON" cli/output-should-be-valid-json)

(defthen "the output should span multiple lines" cli/output-should-span-multiple-lines)

(defthen "the EDN output should include a step with:" cli/edn-output-should-include-step-with)

(defthen "the JSON output should include a step with:" cli/json-output-should-include-step-with)

(defthen "every step entry in the JSON output has type {step-type:string}" cli/every-step-entry-in-json-output-has-type)

(defthen "the EDN report should include:" cli/edn-report-should-include)

(defthen "the JSON report should include:" cli/json-report-should-include)

(defthen "the :unused-steps list should contain a step with phrase {phrase:string}" cli/unused-steps-list-should-contain-step-with-phrase)

(defthen "the :ambiguities list should contain an entry with phrase {phrase:string}" cli/ambiguities-list-should-contain-entry-with-phrase)

(defthen "the exit code should be zero" cli/exit-code-should-be-zero)

(defthen "the exit code should be non-zero" cli/exit-code-should-be-non-zero)

(defthen "the error output should mention {text:string}" cli/error-output-should-mention)
