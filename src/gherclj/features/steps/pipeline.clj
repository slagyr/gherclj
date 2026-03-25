(ns gherclj.features.steps.pipeline
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.pipeline :as pipeline]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def ^:private base-dir
  (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test"))

(defn- edn-dir [] (str base-dir "/target/gherclj/edn"))
(defn- output-dir [] (str base-dir "/target/gherclj/generated"))

(defn- pipeline-config [& {:keys [verbose framework]}]
  (cond-> {:features-dir (g/get :pipeline-dir)
           :edn-dir (edn-dir)
           :output-dir (output-dir)}
    verbose (assoc :verbose true)
    framework (assoc :test-framework framework
                     :step-namespaces [])))

(defn- strip-quotes [s]
  (if (and (str/starts-with? s "\"") (str/ends-with? s "\""))
    (subs s 1 (dec (count s)))
    s))

;; --- Given steps ---

(defgiven setup-features-dir "a features directory containing:"
  [table]
  (let [dir (str base-dir "/features")]
    (io/make-parents (io/file dir "dummy"))
    (doseq [row (:rows table)]
      (let [filename (first row)]
        (spit (io/file dir filename) "")))
    (g/assoc! :pipeline-dir dir)))

(defgiven write-feature-content "the feature {name:string} contains:"
  [name doc-string]
  (spit (io/file (g/get :pipeline-dir) name) doc-string))

(defgiven parse-stage-has-run "the parse stage has run"
  []
  (let [output (with-out-str (pipeline/parse! (pipeline-config)))]
    (g/assoc! :pipeline-output output)))

;; --- When steps ---

(defwhen run-parse-stage "the parse stage runs"
  []
  (let [output (with-out-str (pipeline/parse! (pipeline-config)))]
    (g/assoc! :pipeline-output output)))

(defwhen run-parse-stage-verbose "the parse stage runs with :verbose"
  []
  (let [output (with-out-str (pipeline/parse! (pipeline-config :verbose true)))]
    (g/assoc! :pipeline-output output)))

(defwhen run-generate-stage "the generate stage runs with framework {fw}"
  [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))
        output (with-out-str (pipeline/generate! (pipeline-config :framework framework)))]
    (g/assoc! :pipeline-output output)))

(defwhen run-generate-stage-verbose "the generate stage runs with framework {fw} and :verbose"
  [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))
        output (with-out-str (pipeline/generate! (pipeline-config :framework framework :verbose true)))]
    (g/assoc! :pipeline-output output)))

(defwhen run-full-pipeline "the full pipeline runs with framework {fw}"
  [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))
        output (with-out-str (pipeline/run! (pipeline-config :framework framework)))]
    (g/assoc! :pipeline-output output)))

;; --- Then steps ---

(defthen file-should-exist "{path} should exist"
  [path]
  (g/should (.exists (io/file base-dir (strip-quotes path)))))

(defthen file-should-contain-ir #"^(\S+) should contain IR:$"
  [path doc-string]
  (let [p (strip-quotes path)
        actual (edn/read-string (slurp (io/file base-dir p)))
        expected (edn/read-string doc-string)]
    (g/should= expected actual)))

(defthen file-should-contain #"^(\S+) should contain \"(.+)\"$"
  [path text]
  (let [content (slurp (io/file base-dir (strip-quotes path)))]
    (g/should (str/includes? content text))))

(defthen file-should-not-contain #"^(\S+) should not contain \"(.+)\"$"
  [path text]
  (let [content (slurp (io/file base-dir (strip-quotes path)))]
    (g/should-not (str/includes? content text))))

(defthen file-should-contain-ir-with-n-scenarios "{path} should contain IR with {n:int} scenarios"
  [path n]
  (let [ir (edn/read-string (slurp (io/file base-dir (strip-quotes path))))]
    (g/should= n (count (:scenarios ir)))))

(defthen pipeline-output-should-be-empty "the output should be empty"
  []
  (g/should= "" (g/get :pipeline-output)))
