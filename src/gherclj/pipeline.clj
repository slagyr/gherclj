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
  "Load the framework namespace for the given framework keyword.
   The keyword is expected to be language-namespaced (e.g. :clojure/speclj,
   :ruby/rspec); the corresponding namespace is gherclj.frameworks.<lang>.<name>."
  [framework]
  (let [lang (namespace framework)
        nm   (name framework)
        fw-ns (if lang
                (symbol (str "gherclj.frameworks." lang "." nm))
                (symbol (str "gherclj.frameworks." nm)))]
    (require fw-ns)))

(defn- scan-namespaces
  "Scan directories for .clj files and derive namespace symbols."
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

(defn- classpath-roots
  "Return classpath directory entries used for namespace glob resolution."
  []
  (let [classpath (or (System/getProperty "java.class.path") "")
        path-separator (re-pattern (java.util.regex.Pattern/quote (System/getProperty "path.separator")))]
    (or (seq (->> (str/split classpath path-separator)
                  (remove str/blank?)
                  (map io/file)
                  (filter #(.isDirectory %))
                  (map str)
                  distinct
                  vec))
        ["src"])))

(defn load-step-namespaces!
  "Resolve glob patterns and require all step namespaces."
  [step-namespaces]
  (let [has-globs? (some string? step-namespaces)
        resolved (if has-globs?
                   (let [available (scan-namespaces (classpath-roots))]
                      (discovery/resolve-step-namespaces step-namespaces available))
                   (vec step-namespaces))]
    (doseq [ns-sym resolved]
      (require ns-sym))
    resolved))

(defn- source->edn-filename [source]
  (str/replace source #"\.feature$" ".edn"))

(defn- source->spec-filename [source framework]
  (let [suffix (case framework
                 :clojure/test "_test.clj"
                 :ruby/rspec "_spec.rb"
                 "_spec.clj")]
    (-> source
        (str/replace #"\.(feature|edn)$" "")
        (str suffix))))

(defn- write-edn [path data]
  (spit path (with-out-str (pprint/pprint data))))

(defn- log [verbose & args]
  (when verbose (apply println args)))

(defn- normalize-path [path]
  (-> path
      (str/replace "\\" "/")
      (str/replace #"^\./" "")))

(defn- scenario-title [trimmed]
  (cond
    (str/starts-with? trimmed "Scenario:") (str/trim (subs trimmed 9))
    (str/starts-with? trimmed "Scenario Outline:") (str/trim (subs trimmed 17))
    :else nil))

(defn- scenario-ranges [feature-file]
  (let [lines (str/split-lines (slurp feature-file))
        count-lines (count lines)
        {:keys [starts]} (reduce (fn [{:keys [in-doc-string starts]} [idx line]]
                                  (let [line-number (inc idx)
                                        trimmed (str/trim line)]
                                    (cond
                                      (= "\"\"\"" trimmed)
                                      {:in-doc-string (not in-doc-string)
                                       :starts starts}

                                      in-doc-string
                                      {:in-doc-string in-doc-string
                                       :starts starts}

                                      :else
                                      {:in-doc-string in-doc-string
                                       :starts (if-let [title (scenario-title trimmed)]
                                                 (conj starts {:scenario title
                                                               :start-line line-number})
                                                 starts)})))
                                {:in-doc-string false :starts []}
                                (map-indexed vector lines))]
    (mapv (fn [idx {:keys [scenario start-line]}]
            (let [next-start (:start-line (nth starts (inc idx) nil))]
              {:scenario scenario
               :start-line start-line
               :end-line (or (some-> next-start dec) count-lines)}))
          (range (count starts))
          starts)))

(defn- selector->relative-source [features-dir source]
  (let [normalized-source (normalize-path source)
        normalized-features-dir (normalize-path features-dir)
        prefix (str normalized-features-dir "/")]
    (if (str/starts-with? normalized-source prefix)
      (subs normalized-source (count prefix))
      normalized-source)))

(defn- selected-scenario-name [features-dir {:keys [source line]}]
  (let [relative-source (selector->relative-source features-dir source)
        feature-file (io/file features-dir relative-source)
        scenario (when (.exists feature-file)
                   (->> (scenario-ranges feature-file)
                        (some (fn [{:keys [scenario start-line end-line]}]
                                (when (<= start-line line end-line)
                                  scenario)))))]
    (when-not scenario
      (throw (ex-info (str "No scenario found for location " source ":" line)
                      {:source source :line line})))
    {:source relative-source
     :scenario scenario}))

(defn- verify-feature-file! [features-dir source]
  (let [relative (selector->relative-source features-dir source)
        file (io/file features-dir relative)]
    (when-not (.exists file)
      (throw (ex-info (str "Feature file not found: " source)
                      {:source source})))
    relative))

(defn- selected-scenarios-by-source [features-dir locations]
  (reduce (fn [acc {:keys [source line] :as location}]
            (if line
              (let [{:keys [source scenario]} (selected-scenario-name features-dir location)]
                (if (= :all (get acc source))
                  acc
                  (update acc source (fnil conj #{}) scenario)))
              (let [relative (verify-feature-file! features-dir source)]
                (assoc acc relative :all))))
          {}
          locations))

(defn- filter-ir-by-locations [ir selected]
  (let [entry (get selected (:source ir))]
    (cond
      (= :all entry) ir
      (set? entry)
      (update ir :scenarios
              (fn [scenarios]
                (->> scenarios
                     (filter #(contains? entry (:scenario %)))
                     vec)))
      :else (assoc ir :scenarios []))))

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
     :framework  - :clojure/speclj or :clojure/test"
  [config]
  (let [{:keys [edn-dir output-dir step-namespaces framework verbose locations features-dir]
          :or {edn-dir "target/gherclj/edn"
               output-dir "target/gherclj/generated"
               features-dir "features"}} config]
    (ensure-framework-loaded! framework)
    (let [resolved-steps (load-step-namespaces! step-namespaces)
          config (assoc config :step-namespaces resolved-steps)
          selected-scenarios (when (seq locations)
                               (selected-scenarios-by-source features-dir locations))
          edn-files (->> (file-seq (io/file edn-dir))
                         (filter #(str/ends-with? (.getName %) ".edn"))
                         (sort-by #(str (.toPath %))))]
      (doseq [f edn-files]
        (let [parsed-ir (edn/read-string (slurp f))
              ir (if selected-scenarios
                   (filter-ir-by-locations parsed-ir selected-scenarios)
                   parsed-ir)
              out-name (source->spec-filename (:source ir) framework)
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
     :framework  - :clojure/speclj or :clojure/test"
  [config]
  (parse! config)
  (generate! config))
