(ns gherclj.frameworks.python.pytest
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.framework :as fw]))

(defonce scenario-setup-registry (atom {}))

(defmacro scenario-setup! [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defn python-string [s]
  (str "'"
       (-> (str s)
           (str/replace #"\\" "\\\\")
           (str/replace #"'" "\\'")
           (str/replace #"\n" "\\n"))
       "'"))

(defn python-literal [value]
  (cond
    (string? value) (python-string value)
    (keyword? value) (python-string (name value))
    (symbol? value) (python-string (name value))
    (true? value) "True"
    (false? value) "False"
    (nil? value) "None"
    (integer? value) (str value)
    (float? value) (str value)
    (vector? value) (str "[" (str/join ", " (map python-literal value)) "]")
    (map? value) (str "{" (str/join ", " (map (fn [[k v]] (str (python-literal (name k)) ": " (python-literal v))) value)) "}")
    :else (python-string value)))

(defn- python-method [name]
  (str/replace name #"-" "_"))

(defn generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map python-literal all-args))]
    (str (python-method name) "(" args-str ")")))

(defmethod fw/render-step :python/pytest [_config step]
  (generate-step-call step))

(defn- camelize [part]
  (->> (str/split part #"_")
       (remove str/blank?)
       (map str/capitalize)
       (apply str)))

(defn- import-line [import-val]
  (cond
    (and (string? import-val)
         (or (str/starts-with? import-val "from ")
             (str/starts-with? import-val "import ")))
    import-val

    (string? import-val)
    (let [module (str/trim import-val)
          class-name (camelize (last (str/split module #"\.")))]
      (str "from " module " import " class-name))

    :else
    (str "import " import-val)))

(defn- slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9]+" "_")
      (str/replace #"^_|_$" "")))

(defmethod fw/generate-preamble :python/pytest
  [_config source used-nses]
  (let [helper-imports (->> used-nses
                            (mapcat #(gherclj.core/helper-imports-in-ns %))
                            distinct
                            (map import-line))]
    (str/join "\n" (concat [(str "# generated from " source)
                              "import pytest"]
                             helper-imports))))

(defmethod fw/wrap-feature :python/pytest
  [_config _feature-name scenario-blocks]
  (str scenario-blocks "\n"))

(defmethod fw/wrap-scenario :python/pytest
  [_config scenario background]
  (let [test-name (str "test_" (slugify (:scenario scenario)))
        setup-lines (->> (:steps scenario)
                         (map :ns)
                         distinct
                         (mapcat #(get @scenario-setup-registry % [])))
        bg-calls (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body-lines (concat setup-lines bg-calls step-calls)]
    (str "def " test-name "():\n"
         (str/join "\n" (map #(str "    " %) body-lines)))))

(defmethod fw/wrap-pending :python/pytest
  [_config scenario _background]
  (let [test-name (str "test_" (slugify (:scenario scenario)))]
    (str "def " test-name "():\n"
         "    pytest.skip('not yet implemented')")))

(defn- run-pytest [python-exe opts output-dir]
  (let [cmd (concat [python-exe "-m" "pytest"] opts [output-dir])]
    (apply shell/sh cmd)))

(defn- python-executables []
  (let [venv-python (str (System/getProperty "user.dir") "/.venv/bin/python")]
    (cond-> []
      (.exists (java.io.File. venv-python)) (conj venv-python)
      true (into ["python" "python3"]))))

(defn- run-pytest-with-fallback [opts output-dir]
  (or (some (fn [python-exe]
              (try
                (run-pytest python-exe opts output-dir)
                (catch java.io.IOException _ nil)))
            (python-executables))
      (throw (java.io.IOException. "No Python interpreter found"))))

(defmethod fw/run-specs :python/pytest
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        opts (or (:framework-opts config) [])
        {:keys [exit out err]} (run-pytest-with-fallback opts output-dir)]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))
    (when-not (zero? exit)
      (throw (ex-info "pytest failed" {:exit exit :stderr err})))
    0))
