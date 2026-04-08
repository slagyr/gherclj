;; mutation-tested: 2026-03-25
(ns gherclj.pipeline
  (:refer-clojure :exclude [run!])
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [gherclj.parser :as parser]
            [gherclj.generator :as gen]
            [gherclj.discovery :as discovery]))

(defn- ensure-framework-loaded!
  "Load the framework namespace for the given test-framework keyword."
  [test-framework]
  (let [fw-ns (case test-framework
                :speclj 'gherclj.frameworks.speclj
                :clojure.test 'gherclj.frameworks.clojure-test
                (symbol (str "gherclj.frameworks." (name test-framework))))]
    (require fw-ns)))

(defn- scan-namespaces
  "Scan source directories for .clj files and derive namespace symbols."
  [dirs]
  (->> dirs
       (mapcat (fn [dir]
                 (let [root (io/file dir)]
                   (when (.exists root)
                     (->> (file-seq root)
                          (filter #(str/ends-with? (.getName %) ".clj"))
                          (map (fn [f]
                                 (let [rel (.relativize (.toPath root) (.toPath f))]
                                   (-> (str rel)
                                       (str/replace #"\.clj$" "")
                                       (str/replace "/" ".")
                                       (str/replace "_" "-")
                                       symbol)))))))))
       vec))

(defn- ensure-steps-loaded!
  "Resolve glob patterns and require all step namespaces."
  [step-namespaces]
  (let [has-globs? (some string? step-namespaces)
        resolved (if has-globs?
                   (let [available (scan-namespaces ["src"])]
                     (discovery/resolve-step-namespaces step-namespaces available))
                   (vec step-namespaces))]
    (doseq [ns-sym resolved]
      (require ns-sym))
    resolved))

(defn- source->edn-filename [source]
  (str/replace source #"\.feature$" ".edn"))

(defn- source->spec-filename [source test-framework]
  (let [suffix (case test-framework
                 :clojure.test "_test.clj"
                 "_spec.clj")]
    (-> source
        (str/replace #"\.(feature|edn)$" "")
        (str suffix))))

(defn- write-edn [path data]
  (spit path (with-out-str (pprint/pprint data))))

(defn- log [verbose & args]
  (when verbose (apply println args)))

(defn parse!
  "Parse .feature files into .edn IR files.

   Config keys:
     :features-dir - directory containing .feature files
     :edn-dir      - directory to write .edn IR files (default: target/gherclj/edn)
     :verbose      - when truthy, print progress to stdout"
  [config]
  (let [{:keys [features-dir edn-dir verbose]
         :or {edn-dir "target/gherclj/edn"}} config
        features (parser/parse-features-dir features-dir)]
    (doseq [ir features]
      (let [edn-name (source->edn-filename (:source ir))
            edn-path (str edn-dir "/" edn-name)]
        (io/make-parents (io/file edn-path))
        (log verbose (str "Parsing " (:source ir) " -> " edn-path))
        (write-edn edn-path ir)
        (log verbose (str "  " (count (:scenarios ir)) " scenarios parsed"))))))

(defn generate!
  "Generate spec files from .edn IR files.

   Config keys:
     :edn-dir         - directory containing .edn IR files (default: target/gherclj/edn)
     :output-dir      - directory to write generated specs (default: target/gherclj/generated)
     :step-namespaces - vector of namespace symbols containing step definitions
     :test-framework  - :speclj or :clojure.test"
  [config]
  (let [{:keys [edn-dir output-dir step-namespaces test-framework verbose]
         :or {edn-dir "target/gherclj/edn"
              output-dir "target/gherclj/generated"}} config]
    (ensure-framework-loaded! test-framework)
    (let [resolved-steps (ensure-steps-loaded! step-namespaces)
          config (assoc config :step-namespaces resolved-steps)
          edn-files (->> (file-seq (io/file edn-dir))
                         (filter #(str/ends-with? (.getName %) ".edn"))
                         (sort-by #(str (.toPath %))))]
      (doseq [f edn-files]
        (let [ir (edn/read-string (slurp f))
              out-name (source->spec-filename (:source ir) test-framework)
              out-path (str output-dir "/" out-name)
              spec-str (gen/generate-spec config ir)
              out-file (io/file out-path)]
          (io/make-parents out-file)
          (if spec-str
            (do
              (log verbose (str "Generating " out-path " from " (.getName f)))
              (spit out-path spec-str)
              (log verbose (str "  " (count (:scenarios ir)) " scenarios generated")))
            (.delete out-file)))))))

(defn run!
  "Run the full pipeline: parse .feature -> .edn -> generated specs.

   Config keys:
     :features-dir    - directory containing .feature files
     :edn-dir         - directory to write .edn IR files (default: target/gherclj/edn)
     :output-dir      - directory to write generated specs (default: target/gherclj/generated)
     :step-namespaces - vector of namespace symbols containing step definitions
     :test-framework  - :speclj or :clojure.test"
  [config]
  (parse! config)
  (generate! config))
