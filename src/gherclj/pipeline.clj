(ns gherclj.pipeline
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.parser :as parser]
            [gherclj.generator :as gen]))

(defn- ensure-framework-loaded!
  "Load the framework namespace for the given test-framework keyword."
  [test-framework]
  (let [fw-ns (case test-framework
                :speclj 'gherclj.frameworks.speclj
                :clojure.test 'gherclj.frameworks.clojure-test
                (symbol (str "gherclj.frameworks." (name test-framework))))]
    (require fw-ns)))

(defn- ensure-steps-loaded!
  "Require all step namespaces."
  [step-namespaces]
  (doseq [ns-sym step-namespaces]
    (when-not (find-ns ns-sym)
      (require ns-sym))))

(defn- source->spec-filename
  "Convert a source filename to a spec output filename."
  [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_spec.clj")))

(defn generate!
  "Run the full pipeline: parse .feature files and generate spec files.

   Config keys:
     :features-dir    - directory containing .feature files
     :output-dir      - directory to write generated specs (default: features/generated)
     :step-namespaces - vector of namespace symbols containing step definitions
     :harness-ns      - namespace symbol for the test harness
     :test-framework  - :speclj or :clojure.test"
  [config]
  (let [{:keys [features-dir output-dir step-namespaces test-framework]
         :or {output-dir "features/generated"}} config]
    (ensure-framework-loaded! test-framework)
    (ensure-steps-loaded! step-namespaces)
    (let [features (parser/parse-features-dir features-dir)]
      (io/make-parents (io/file output-dir "dummy"))
      (doseq [ir features]
        (let [out-name (source->spec-filename (:source ir))
              out-path (str output-dir "/" out-name)
              spec-str (gen/generate-spec config ir)]
          (println (str "Generating " out-path " from " (:source ir)))
          (spit out-path spec-str)
          (println (str "  " (count (remove :wip (:scenarios ir))) " scenarios generated")))))))
