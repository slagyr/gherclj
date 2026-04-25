(ns gherclj.frameworks.javascript.node-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.core]
            [gherclj.framework :as fw]))

(defonce scenario-setup-registry (atom {}))

(defmacro scenario-setup! [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defn- js-string [s]
  (str "'" (str/replace (str s) #"['\\]" #(str "\\" %)) "'"))

(defn- js-literal [value]
  (cond
    (string? value) (js-string value)
    (keyword? value) (js-string (name value))
    (symbol? value) (js-string (name value))
    (true? value) "true"
    (false? value) "false"
    (nil? value) "null"
    (integer? value) (str value)
    (float? value) (str value)
    (vector? value) (str "[" (str/join ", " (map js-literal value)) "]")
    (map? value) (str "{" (str/join ", " (map (fn [[k v]] (str (pr-str (name k)) ": " (js-literal v))) value)) "}")
    :else (js-string value)))

(defn- camelize [s]
  (let [[head & tail] (str/split s #"-")]
    (apply str head (map str/capitalize tail))))

(defn- js-method [name]
  (let [parts (str/split name #"\.")
        prefix (butlast parts)
        method (camelize (last parts))]
    (str/join "." (concat prefix [method]))))

(defn- generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map js-literal all-args))]
    (str (js-method name) "(" args-str ")")))

(defmethod fw/render-step :javascript/node-test [_config step]
  (generate-step-call step))

(defn- source->spec-filename [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_test.js")))

(defn- normalize-path [path]
  (str/replace path "\\" "/"))

(defn- relative-import [output-dir source import-val]
  (let [spec-path (-> (io/file output-dir (source->spec-filename source)) .getAbsoluteFile .toPath)
        spec-dir (.getParent spec-path)
        target-path (-> (io/file (str import-val)) .getAbsoluteFile .toPath)
        rel (normalize-path (str (.relativize spec-dir target-path)))]
    (if (or (str/starts-with? rel "./") (str/starts-with? rel "../"))
      rel
      (str "./" rel))))

(defn- import-alias [import-val]
  (-> (str import-val)
      io/file
      .getName
      (str/replace #"\.[^.]+$" "")
      (str/replace #"[^A-Za-z0-9_]" "_")))

(defn- import-line [config source import-val]
  (str "import * as " (import-alias import-val) " from "
       (js-string (relative-import (:output-dir config "target/gherclj/generated") source import-val))))

(defmethod fw/generate-preamble :javascript/node-test
  [config source used-nses]
  (let [helper-imports (->> used-nses
                            (mapcat #(gherclj.core/helper-imports-in-ns %))
                            distinct
                            (map #(import-line config source %)))
        top-lines (concat [(str "// generated from " source)
                           "import test from 'node:test'"]
                          helper-imports)]
    (str/join "\n" top-lines)))

(defmethod fw/wrap-feature :javascript/node-test
  [_config _feature-name scenario-blocks]
  (str scenario-blocks "\n"))

(defmethod fw/wrap-scenario :javascript/node-test
  [_config scenario background]
  (let [setup-lines (->> (:steps scenario)
                         (map :ns)
                         distinct
                         (mapcat #(get @scenario-setup-registry % [])))
        bg-calls (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body-lines (concat setup-lines bg-calls step-calls)]
    (str "test(" (js-string (:scenario scenario)) ", () => {\n"
         (str/join "\n" (map #(str "  " %) body-lines)) "\n"
         "})")))

(defmethod fw/wrap-pending :javascript/node-test
  [_config scenario _background]
  (str "test(" (js-string (:scenario scenario)) ", { skip: 'not yet implemented' }, () => {})"))

(defmethod fw/run-specs :javascript/node-test
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        test-files (->> (file-seq (io/file output-dir))
                        (filter #(.isFile %))
                        (filter #(str/ends-with? (.getName %) "_test.js"))
                        (sort-by #(.getPath %))
                        (map str))
        opts (or (:framework-opts config) [])
        cmd (concat ["node" "--test"] opts test-files)
        {:keys [exit out err]} (apply shell/sh cmd)]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))
    (when-not (zero? exit)
      (throw (ex-info "node --test failed" {:exit exit :stderr err})))
    0))
