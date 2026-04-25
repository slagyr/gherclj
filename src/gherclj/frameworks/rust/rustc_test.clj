(ns gherclj.frameworks.rust.rustc-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.core :as core]
            [gherclj.framework :as fw]))

(defonce scenario-setup-registry (atom {}))

(defmacro scenario-setup! [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defn rust-string [s]
  (str "\""
       (-> (str s)
           (str/replace #"\\" "\\\\")
           (str/replace #"\"" "\\\"")
           (str/replace #"\n" "\\n")
           (str/replace #"\t" "\\t"))
       "\""))

(defn rust-literal [value]
  (cond
    (string? value) (rust-string value)
    (keyword? value) (rust-string (name value))
    (symbol? value) (rust-string (name value))
    (true? value) "true"
    (false? value) "false"
    (nil? value) "None"
    (integer? value) (str value)
    (float? value) (str value)
    (vector? value) (str "vec![" (str/join ", " (map rust-literal value)) "]")
    :else (rust-string value)))

(defn- rust-method [name]
  (-> name
      (str/replace #"-" "_")))

(defn generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map rust-literal all-args))]
    (str (rust-method name) "(" args-str ");")))

(defmethod fw/render-step :rust/rustc-test [_config step]
  (generate-step-call step))

(defn- source->spec-filename [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_test.rs")))

(defn- normalize-path [path]
  (str/replace path "\\" "/"))

(defn- relative-import [output-dir source import-val]
  (let [spec-path (-> (io/file output-dir (source->spec-filename source)) .getAbsoluteFile .toPath)
        spec-dir (.getParent spec-path)
        target-path (-> (io/file (str import-val)) .getAbsoluteFile .toPath)
        rel (normalize-path (str (.relativize spec-dir target-path)))]
    rel))

(defn- module-name [import-val]
  (-> (str import-val)
      io/file
      .getName
      (str/replace #"\.rs$" "")
      (str/replace #"[^A-Za-z0-9_]" "_")))

(defn- import-lines [config source import-val]
  [(str "#[path = " (rust-string (relative-import (:output-dir config "target/gherclj/generated") source import-val)) "]")
   (str "mod " (module-name import-val) ";")])

(defn- slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9]+" "_")
      (str/replace #"^_|_$" "")))

(defmethod fw/generate-preamble :rust/rustc-test
  [config source used-nses]
  (let [helper-imports (->> used-nses
                            (mapcat core/helper-imports-in-ns)
                            distinct
                            (mapcat #(import-lines config source %)))]
    (str/join "\n" (concat [(str "// generated from " source)]
                              helper-imports))))

(defmethod fw/wrap-feature :rust/rustc-test
  [_config _feature-name scenario-blocks]
  (str scenario-blocks "\n"))

(defn- collect-scenario-setup [used-nses]
  (->> used-nses
       (mapcat #(get @scenario-setup-registry % []))
       distinct))

(defmethod fw/wrap-scenario :rust/rustc-test
  [config scenario background]
  (let [used-nses (or (:_used-nses config) #{})
        setup-lines (collect-scenario-setup used-nses)
        bg-calls (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body-lines (concat setup-lines bg-calls step-calls)]
    (str "#[test]\n"
         "fn " (slugify (:scenario scenario)) "() {\n"
         (str/join "\n" (map #(str "    " %) body-lines)) "\n"
         "}")))

(defmethod fw/wrap-pending :rust/rustc-test
  [_config scenario _background]
  (str "#[test]\n"
       "#[ignore = \"not yet implemented\"]\n"
       "fn " (slugify (:scenario scenario)) "() {}"))

(defn- binary-path [target-dir test-file]
  (let [name (-> test-file io/file .getName (str/replace #"\.rs$" ""))]
    (str (io/file target-dir name))))

(defmethod fw/run-specs :rust/rustc-test
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        target-dir (or (:rust-target-dir config) "target/gherclj")
        opts (or (:framework-opts config) [])
        test-files (->> (file-seq (io/file output-dir))
                        (filter #(.isFile %))
                        (filter #(str/ends-with? (.getName %) "_test.rs"))
                        (sort-by #(.getPath %))
                        (map str))]
    (io/make-parents (io/file target-dir "dummy"))
    (doseq [test-file test-files]
      (let [binary (binary-path target-dir test-file)
            compile-cmd ["rustc" "--edition=2021" "--test" test-file "-o" binary]
            {:keys [exit out err]} (apply shell/sh compile-cmd)]
        (when (seq out)
          (print out))
        (when (seq err)
          (binding [*out* *err*]
            (print err)))
        (when-not (zero? exit)
          (throw (ex-info "rustc failed" {:exit exit :stderr err :file test-file})))
        (let [{run-exit :exit run-out :out run-err :err} (apply shell/sh (concat [binary] opts))]
          (when (seq run-out)
            (print run-out))
          (when (seq run-err)
            (binding [*out* *err*]
              (print run-err)))
          (when-not (zero? run-exit)
            (throw (ex-info "rust test binary failed" {:exit run-exit :stderr run-err :file test-file}))))))
    0))
