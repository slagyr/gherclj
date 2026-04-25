(ns gherclj.frameworks.rspec
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.framework :as fw]))

(defonce file-setup-registry (atom {}))
(defonce describe-setup-registry (atom {}))

(defmacro file-setup! [line]
  `(swap! file-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defmacro describe-setup! [line]
  `(swap! describe-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

(defn ruby-string [s]
  (str "'" (str/replace (str s) #"['\\]" #(str "\\" %)) "'"))

(defn ruby-literal [value]
  (cond
    (string? value) (ruby-string value)
    (keyword? value) (ruby-string (name value))
    (symbol? value) (ruby-string (name value))
    (true? value) "true"
    (false? value) "false"
    (nil? value) "nil"
    (integer? value) (str value)
    (float? value) (str value)
    (vector? value) (str "[" (str/join ", " (map ruby-literal value)) "]")
    (map? value) (str "{" (str/join ", " (map (fn [[k v]] (str (ruby-literal (name k)) " => " (ruby-literal v))) value)) "}")
    :else (ruby-string value)))

(defn- ruby-method [name]
  (str/replace name "-" "_"))

(defn generate-step-call [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map ruby-literal all-args))]
    (if (seq all-args)
      (str (ruby-method name) "(" args-str ")")
      (ruby-method name))))

(defmethod fw/render-step :rspec [_config step]
  (generate-step-call step))

(defmethod fw/generate-preamble :rspec
  [_config source used-nses]
  (let [feature-name (-> source
                         (str/split #"/")
                         last
                         (str/replace #"\.feature$" "")
                         (str/replace #"_" " ")
                         (str/capitalize))
        file-setup   (->> used-nses
                          (mapcat #(get @file-setup-registry % []))
                          distinct)
        desc-setup   (->> used-nses
                          (mapcat #(get @describe-setup-registry % []))
                          distinct)]
    (str "# generated from " source "\n"
         "require 'rspec'\n"
         (str/join (map #(str % "\n") file-setup))
         "\n"
         "RSpec.describe " (ruby-string feature-name) " do\n"
         (when (seq desc-setup)
           (str (str/join "\n" (map #(str "  " %) desc-setup)) "\n")))))

(defmethod fw/wrap-feature :rspec
  [_config _feature-name scenario-blocks]
  (str "\n" scenario-blocks "\nend\n"))

(defmethod fw/wrap-scenario :rspec
  [_config scenario background]
  (let [bg-calls   (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body       (->> (concat bg-calls step-calls)
                        (map #(str "    " %))
                        (str/join "\n"))]
    (str "  it " (ruby-string (:scenario scenario)) " do\n"
         body "\n"
         "  end")))

(defmethod fw/wrap-pending :rspec
  [_config scenario _background]
  (str "  it " (ruby-string (:scenario scenario)) " do\n"
       "    skip 'not yet implemented'\n"
       "  end"))

(defmethod fw/run-specs :rspec
  [config]
  (let [{:keys [exit out err]} (shell/sh "bundle" "exec" "rspec" "--tty" (or (:output-dir config) "target/gherclj/generated"))]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))
    (when-not (zero? exit)
      (throw (ex-info "rspec failed" {:exit exit :stderr err})))
    0))
