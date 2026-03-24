(ns gherclj.pipeline
  (:refer-clojure :exclude [run!])
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.edn :as edn]
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
    (require ns-sym)))

(defn- source->edn-filename [source]
  (str/replace source #"\.feature$" ".edn"))

(defn- source->spec-filename [source]
  (-> source
      (str/replace #"\.(feature|edn)$" "")
      (str "_spec.clj")))

(defn- write-edn [path data]
  (spit path (with-out-str (pprint/pprint data))))

(defn parse!
  "Parse .feature files into .edn IR files.

   Config keys:
     :features-dir - directory containing .feature files
     :edn-dir      - directory to write .edn IR files (default: features/edn)"
  [config]
  (let [{:keys [features-dir edn-dir]
         :or {edn-dir "features/edn"}} config
        features (parser/parse-features-dir features-dir)]
    (io/make-parents (io/file edn-dir "dummy"))
    (doseq [ir features]
      (let [edn-name (source->edn-filename (:source ir))
            edn-path (str edn-dir "/" edn-name)]
        (println (str "Parsing " (:source ir) " -> " edn-path))
        (write-edn edn-path ir)
        (println (str "  " (count (:scenarios ir)) " scenarios parsed"))))))

(defn generate!
  "Generate spec files from .edn IR files.

   Config keys:
     :edn-dir         - directory containing .edn IR files (default: features/edn)
     :output-dir      - directory to write generated specs (default: features/generated)
     :step-namespaces - vector of namespace symbols containing step definitions
     :harness-ns      - namespace symbol for the test harness
     :test-framework  - :speclj or :clojure.test"
  [config]
  (let [{:keys [edn-dir output-dir step-namespaces test-framework]
         :or {edn-dir "features/edn"
              output-dir "features/generated"}} config]
    (ensure-framework-loaded! test-framework)
    (ensure-steps-loaded! step-namespaces)
    (let [edn-files (->> (.listFiles (io/file edn-dir))
                         (filter #(str/ends-with? (.getName %) ".edn"))
                         (sort-by #(.getName %)))]
      (io/make-parents (io/file output-dir "dummy"))
      (doseq [f edn-files]
        (let [ir (edn/read-string (slurp f))
              out-name (source->spec-filename (:source ir))
              out-path (str output-dir "/" out-name)
              spec-str (gen/generate-spec config ir)]
          (println (str "Generating " out-path " from " (.getName f)))
          (spit out-path spec-str)
          (println (str "  " (count (remove :wip (:scenarios ir))) " scenarios generated")))))))

(defn run!
  "Run the full pipeline: parse .feature -> .edn -> generated specs.

   Config keys:
     :features-dir    - directory containing .feature files
     :edn-dir         - directory to write .edn IR files (default: features/edn)
     :output-dir      - directory to write generated specs (default: features/generated)
     :step-namespaces - vector of namespace symbols containing step definitions
     :harness-ns      - namespace symbol for the test harness
     :test-framework  - :speclj or :clojure.test"
  [config]
  (parse! config)
  (generate! config))
