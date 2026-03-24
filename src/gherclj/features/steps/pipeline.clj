(ns gherclj.features.steps.pipeline
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]
            [gherclj.pipeline :as pipeline]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

;; --- Given steps ---

(defgiven setup-features-dir "a features directory containing:"
  [table]
  (let [dir (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test/features")]
    (io/make-parents (io/file dir "dummy"))
    (doseq [row (:rows table)]
      (let [filename (first row)]
        (spit (io/file dir filename) "")))
    (h/set-pipeline-dir! dir)))

(defgiven write-feature-content "the feature \"{name}\" contains:"
  [name doc-string]
  (let [dir (h/pipeline-dir)]
    (spit (io/file dir name) doc-string)))

(defgiven parse-stage-has-run "the parse stage has run"
  []
  (h/run-parse-stage!))

;; --- When steps ---

(defwhen run-parse-stage "the parse stage runs"
  []
  (h/run-parse-stage!))

(defwhen run-parse-stage-verbose "the parse stage runs with :verbose"
  []
  (h/run-parse-stage-verbose!))

(defwhen run-generate-stage "the generate stage runs with framework {fw}"
  [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))]
    (h/run-generate-stage! framework)))

(defwhen run-generate-stage-verbose "the generate stage runs with framework {fw} and :verbose"
  [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))]
    (h/run-generate-stage-verbose! framework)))

(defwhen run-full-pipeline "the full pipeline runs with framework {fw}"
  [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))]
    (h/run-full-pipeline! framework)))

;; --- Then steps ---

(defthen file-should-exist "\"{path}\" should exist"
  [path]
  (let [base (h/pipeline-base-dir)]
    (.exists (io/file base path))))

(defthen file-should-contain-ir "\"{path}\" should contain IR:"
  [path doc-string]
  (let [base (h/pipeline-base-dir)
        actual (edn/read-string (slurp (io/file base path)))
        expected (edn/read-string doc-string)]
    [actual expected]))

(defthen file-should-contain "\"{path}\" should contain \"{text}\""
  [path text]
  (let [base (h/pipeline-base-dir)
        content (slurp (io/file base path))]
    (str/includes? content text)))

(defthen file-should-not-contain "\"{path}\" should not contain \"{text}\""
  [path text]
  (let [base (h/pipeline-base-dir)
        content (slurp (io/file base path))]
    (not (str/includes? content text))))

(defthen file-should-contain-ir-with-n-scenarios "\"{path}\" should contain IR with {n:int} scenarios"
  [path n]
  (let [base (h/pipeline-base-dir)
        ir (edn/read-string (slurp (io/file base path)))]
    (count (:scenarios ir))))

(defthen pipeline-output-should-be-empty "the output should be empty"
  []
  (h/pipeline-output))
