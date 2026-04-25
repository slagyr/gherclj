(ns gherclj.frameworks.bash.testing
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.framework :as fw]))

(defonce scenario-setup-registry (atom {}))

(defmacro scenario-setup! [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defn shell-string [s]
  (str "'" (str/replace (str s) #"'" "'\"'\"'") "'"))

(defn shell-literal [value]
  (cond
    (string? value) (shell-string value)
    (keyword? value) (shell-string (name value))
    (symbol? value) (shell-string (name value))
    (true? value) "true"
    (false? value) "false"
    (nil? value) "''"
    (integer? value) (str value)
    (float? value) (str value)
    :else (shell-string value)))

(defn- snake-case [s]
  (-> s
      (str/replace #"\." "_")
      (str/replace #"-" "_")))

(defn generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        cmd (snake-case name)]
    (if (seq all-args)
      (str cmd " " (str/join " " (map shell-literal all-args)))
      cmd)))

(defmethod fw/render-step :bash/testing [_config step]
  (generate-step-call step))

(defn- source->spec-filename [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_test.sh")))

(defn- normalize-path [path]
  (str/replace path "\\" "/"))

(defn- relative-import [output-dir source import-val]
  (let [spec-path (-> (io/file output-dir (source->spec-filename source)) .getAbsoluteFile .toPath)
        spec-dir (.getParent spec-path)
        target-path (-> (io/file (str import-val)) .getAbsoluteFile .toPath)]
    (normalize-path (str (.relativize spec-dir target-path)))))

(defn- source-line [config source import-val]
  (str "source \"$SCRIPT_DIR/"
       (relative-import (:output-dir config "target/gherclj/generated") source import-val)
       "\""))

(defn- slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9]+" "_")
      (str/replace #"^_|_$" "")))

(defn- collect-scenario-setup [used-nses]
  (->> used-nses
       (mapcat #(get @scenario-setup-registry % []))
       distinct))

(defn- scenario-used-nses [scenario background]
  (into #{}
        (keep :ns)
        (concat (:steps background) (:steps scenario))))

(defmethod fw/generate-preamble :bash/testing
  [config source used-nses]
  (let [helper-imports (->> used-nses
                            (mapcat core/helper-imports-in-ns)
                            distinct
                            (map #(source-line config source %)))
        lines (concat ["#!/usr/bin/env bash"
                       (str "# generated from " source)
                       "set -uo pipefail"
                       "SCRIPT_DIR=\"$(cd \"$(dirname \"${BASH_SOURCE[0]}\")\" && pwd)\""]
                      helper-imports
                      [""
                       "failures=0"
                       ""
                       "run_test() {"
                       "  local name=\"$1\""
                       "  shift"
                       "  if \"$@\"; then"
                       "    printf 'ok - %s\\n' \"$name\""
                       "  else"
                       "    printf 'not ok - %s\\n' \"$name\" >&2"
                       "    failures=$((failures + 1))"
                       "  fi"
                       "}"])]
    (str/join "\n" lines)))

(defmethod fw/wrap-feature :bash/testing
  [_config _feature-name scenario-blocks]
  (str scenario-blocks "\nexit \"$failures\"\n"))

(defmethod fw/wrap-scenario :bash/testing
  [config scenario background]
  (let [used-nses (or (seq (:_used-nses config))
                      (scenario-used-nses scenario background)
                      #{})
        setup-lines (collect-scenario-setup used-nses)
        bg-calls (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body-lines (concat setup-lines bg-calls step-calls)
        fn-name (slugify (:scenario scenario))]
    (str fn-name "() {\n"
         (str/join "\n" (map #(str "  " %) body-lines)) "\n"
         "}\n\n"
         "run_test " (shell-string (:scenario scenario)) " " fn-name)))

(defmethod fw/wrap-pending :bash/testing
  [_config scenario _background]
  (str "printf 'skip - " (:scenario scenario) " # not yet implemented\\n'"))

(defmethod fw/run-specs :bash/testing
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        opts (or (:framework-opts config) [])
        test-files (->> (file-seq (io/file output-dir))
                        (filter #(.isFile %))
                        (filter #(str/ends-with? (.getName %) "_test.sh"))
                        (sort-by #(.getPath %))
                        (map str))]
    (doseq [test-file test-files]
      (let [{:keys [exit out err]} (apply shell/sh (concat ["bash" test-file] opts))]
        (when (seq out)
          (print out))
        (when (seq err)
          (binding [*out* *err*]
            (print err)))
        (when-not (zero? exit)
          (throw (ex-info "bash test failed" {:exit exit :stderr err :file test-file})))))
    0))
