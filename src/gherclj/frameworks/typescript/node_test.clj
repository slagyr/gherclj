(ns gherclj.frameworks.typescript.node-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.core]
            [gherclj.framework :as fw]))

(defonce file-setup-registry (atom {}))
(defonce describe-setup-registry (atom {}))

(defmacro file-setup! [line]
  `(swap! file-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defmacro describe-setup! [line]
  `(swap! describe-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defn- ts-string [s]
  (str "'" (str/replace (str s) #"['\\]" #(str "\\" %)) "'"))

(defn- ts-literal [value]
  (cond
    (string? value) (ts-string value)
    (keyword? value) (ts-string (name value))
    (symbol? value) (ts-string (name value))
    (true? value) "true"
    (false? value) "false"
    (nil? value) "null"
    (integer? value) (str value)
    (float? value) (str value)
    (vector? value) (str "[" (str/join ", " (map ts-literal value)) "]")
    (map? value) (str "{" (str/join ", " (map (fn [[k v]] (str (pr-str (name k)) ": " (ts-literal v))) value)) "}")
    :else (ts-string value)))

(defn- camelize [s]
  (let [[head & tail] (str/split s #"-")]
    (apply str head (map str/capitalize tail))))

(defn- ts-method [name]
  (let [parts (str/split name #"\.")
        prefix (butlast parts)
        method (camelize (last parts))]
    (str/join "." (concat prefix [method]))))

(defn- generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map ts-literal all-args))]
    (str (ts-method name) "(" args-str ")")))

(defmethod fw/render-step :typescript/node-test [_config step]
  (generate-step-call step))

(defn- source->spec-filename [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_test.ts")))

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
      (str/replace #"[^A-Za-z0-9_]" "_")))

(defn- import-line [config source import-val]
  (str "import * as " (import-alias import-val) " from "
       (ts-string (relative-import (:output-dir config "target/gherclj/generated") source import-val))))

(defn- indent-lines [spaces text]
  (let [prefix (apply str (repeat spaces " "))]
    (->> (str/split-lines text)
         (map #(str prefix %))
         (str/join "\n"))))

(defmethod fw/generate-preamble :typescript/node-test
  [config source used-nses]
  (let [feature-name (-> source
                         (str/split #"/")
                         last
                         (str/replace #"\.feature$" "")
                         (str/replace #"_" " ")
                         str/capitalize)
        helper-imports (->> used-nses
                            (mapcat #(gherclj.core/helper-imports-in-ns %))
                            distinct
                            (map #(import-line config source %)))
        file-setup (->> used-nses
                        (mapcat #(get @file-setup-registry % []))
                        distinct)
        describe-setup (->> used-nses
                            (mapcat #(get @describe-setup-registry % []))
                            distinct)
        top-lines (concat [(str "// generated from " source)
                           "import { beforeEach, describe, test } from 'node:test'"]
                          helper-imports
                          file-setup)]
    (str (str/join "\n" top-lines)
         "\n\n"
         "describe(" (ts-string feature-name) ", () => {\n"
         (when (seq describe-setup)
           (str/join "\n" (map #(indent-lines 2 %) describe-setup))))))

(defmethod fw/wrap-feature :typescript/node-test
  [_config _feature-name scenario-blocks]
  (str "\n" scenario-blocks "\n})\n"))

(defmethod fw/wrap-scenario :typescript/node-test
  [_config scenario background]
  (let [bg-calls (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body (->> (concat bg-calls step-calls)
                  (map #(str "    " %))
                  (str/join "\n"))]
    (str "  test(" (ts-string (:scenario scenario)) ", () => {\n"
         body "\n"
         "  })")))

(defmethod fw/wrap-pending :typescript/node-test
  [_config scenario _background]
  (str "  test(" (ts-string (:scenario scenario)) ", { skip: 'not yet implemented' }, () => {})"))

(defmethod fw/run-specs :typescript/node-test
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        test-files (->> (file-seq (io/file output-dir))
                        (filter #(.isFile %))
                        (filter #(str/ends-with? (.getName %) "_test.ts"))
                        (sort-by #(.getPath %))
                        (map str))
        opts (or (:framework-opts config) [])
        cmd (concat ["npx" "tsx" "--test"] opts test-files)
        {:keys [exit out err]} (apply shell/sh cmd)]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))
    (when-not (zero? exit)
      (throw (ex-info "tsx failed" {:exit exit :stderr err})))
    0))
