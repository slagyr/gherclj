(ns gherclj.features.steps.pipeline
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.config :as config]
            [gherclj.pipeline :as pipeline]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(helper! gherclj.features.steps.pipeline)

(def ^:private base-dir
  (str (System/getProperty "java.io.tmpdir") "/gherclj-pipeline-test"))

(defn- edn-dir [] (str base-dir "/target/gherclj/edn"))
(defn- output-dir [] (str base-dir "/target/gherclj/generated"))

(defn- clean-base-dir! []
  (let [root (io/file base-dir)]
    (when (.exists root)
      (doseq [f (reverse (file-seq root))]
        (.delete f)))))

(defn- pipeline-config [& {:keys [verbose framework include-tags exclude-tags locations]}]
  (cond-> {:features-dirs (or (g/get :features-dirs) [(g/get :pipeline-dir)])
            :root-path base-dir
            :edn-dir (edn-dir)
            :output-dir (output-dir)
            :step-namespaces (or (g/get :step-namespaces) [])}
    verbose (assoc :verbose true)
    framework (assoc :framework framework)
    include-tags (assoc :include-tags include-tags)
    exclude-tags (assoc :exclude-tags exclude-tags)
    locations (assoc :locations locations)))

(defn- parse-option-value [option value]
  (cond
    (= :features-dirs option) (if (str/starts-with? value "[")
                                (edn/read-string value)
                                [value])
    (= "true" value) true
    (= "false" value) false
    (str/starts-with? value ":") (keyword (subs value 1))
    :else value))

(defn- with-pipeline-dir [f]
  (let [previous-dir (System/getProperty "user.dir")]
    (try
      (System/setProperty "user.dir" base-dir)
      (f)
      (finally
        (System/setProperty "user.dir" previous-dir)))))

(defn- resolve-pipeline-config [pipeline-config]
  (let [pipeline-keys (set (keys config/pipeline-schema))
        resolved (with-pipeline-dir #(config/resolve-config (select-keys pipeline-config pipeline-keys)
                                                           {:root-path (:root-path pipeline-config base-dir)}))]
    (if (config/invalid? resolved)
      resolved
      (merge resolved (apply dissoc pipeline-config pipeline-keys)))))

(defn- run-pipeline! [pipeline-config]
  (let [resolved (resolve-pipeline-config pipeline-config)]
    (if (config/invalid? resolved)
      (g/assoc! :pipeline-output ""
                :cli-error-output (config/error-message resolved)
                :run-result 1)
      (try
        (let [output (with-pipeline-dir #(with-out-str (pipeline/run! resolved)))]
          (g/assoc! :pipeline-output output
                    :cli-error-output ""
                    :run-result 0))
        (catch RuntimeException e
          (g/assoc! :pipeline-output ""
                    :cli-error-output (.getMessage e)
                    :run-result 1))))))

(defn- strip-quotes [s]
  (if (and (str/starts-with? s "\"") (str/ends-with? s "\""))
    (subs s 1 (dec (count s)))
    s))

;; --- Helper fns ---

(defn setup-features-dir! [table]
  (let [dir (str base-dir "/features")]
    (clean-base-dir!)
    (io/make-parents (io/file dir "dummy"))
    (doseq [row (:rows table)]
      (let [filename (first row)
            f (io/file dir filename)]
        (io/make-parents f)
        (spit f "")))
    (g/assoc! :pipeline-dir dir
              :features-dirs nil)))

(defn setup-features-dirs! [table]
  (clean-base-dir!)
  (doseq [[root file] (:rows table)]
    (let [f (io/file base-dir root file)]
      (io/make-parents f)
      (spit f "")))
  (g/assoc! :pipeline-dir base-dir
            :features-dirs (->> (:rows table) (map first) distinct vec)))

(defn write-feature-content! [name doc-string]
  (let [target (if (g/get :features-dirs)
                 (io/file base-dir name)
                 (io/file (g/get :pipeline-dir) name))]
    (io/make-parents target)
    (spit target doc-string)))

(defn parse-stage-has-run! []
  (let [output (with-out-str (pipeline/parse! (pipeline-config)))]
    (g/assoc! :pipeline-output output)))

(defn step-namespace-pattern! [pattern]
  (g/assoc! :step-namespaces [pattern]))

(defn run-parse-stage! []
  (let [output (with-out-str (pipeline/parse! (pipeline-config)))]
    (g/assoc! :pipeline-output output)))

(defn run-parse-stage-verbose! []
  (let [output (with-out-str (pipeline/parse! (pipeline-config :verbose true)))]
    (g/assoc! :pipeline-output output)))

(defn run-generate-stage! [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))
        output (with-out-str (pipeline/generate! (pipeline-config :framework framework)))]
    (g/assoc! :pipeline-output output)))

(defn run-generate-stage-verbose! [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))
        output (with-out-str (pipeline/generate! (pipeline-config :framework framework :verbose true)))]
    (g/assoc! :pipeline-output output)))

(defn run-full-pipeline! [fw]
  (let [framework (keyword (str/replace fw #"^:" ""))]
    (run-pipeline! (pipeline-config :framework framework))))

(defn run-full-pipeline-with-tags! [fw table]
  (let [framework (keyword (str/replace fw #"^:" ""))
        tags (mapv first (:rows table))
        includes (vec (remove #(str/starts-with? % "~") tags))
        excludes (mapv #(subs % 1) (filter #(str/starts-with? % "~") tags))]
    (run-pipeline! (pipeline-config :framework framework
                                    :include-tags includes
                                    :exclude-tags excludes))))

(defn run-full-pipeline-with-locations! [fw table]
  (let [framework (keyword (str/replace fw #"^:" ""))
        locations (mapv (fn [[selector]]
                          (if-let [[_ source line] (re-matches #"^(.+\.feature):(\d+)$" selector)]
                            {:source source :line (Long/parseLong line)}
                            {:source selector}))
                        (:rows table))]
    (run-pipeline! (pipeline-config :framework framework
                                    :locations locations))))

(defn run-full-pipeline-with-options! [table]
  (let [{:keys [headers rows]} table
        options (reduce (fn [acc row]
                          (let [m (zipmap headers row)
                                option (keyword (get m "option"))
                                value (parse-option-value option (get m "value"))]
                             (assoc acc option value)))
                         {}
                         rows)]
    (run-pipeline! (merge (pipeline-config) options))))

(defn file-should-exist [path]
  (g/should (.exists (io/file base-dir (strip-quotes path)))))

(defn file-should-not-exist [path]
  (g/should-not (.exists (io/file base-dir (strip-quotes path)))))

(defn file-should-exist-and [path table]
  (let [file (io/file base-dir (strip-quotes path))]
    (g/should (.exists file))
    (let [content (slurp file)
          {:keys [headers rows]} table]
      (doseq [row rows
              :let [m (zipmap headers row)
                    check (get m "check")
                    value (get m "value")]]
        (g/should (contains? #{"contains" "not-contains"} check))
        (case check
          "contains" (g/should (str/includes? content value))
          "not-contains" (g/should-not (str/includes? content value))
          nil)))))

(defn file-should-contain-ir [path doc-string]
  (let [p (strip-quotes path)
        actual (edn/read-string (slurp (io/file base-dir p)))
        expected (edn/read-string doc-string)]
    (g/should= expected actual)))

(defn file-should-contain-ir-with-n-scenarios [path n]
  (let [ir (edn/read-string (slurp (io/file base-dir (strip-quotes path))))]
    (g/should= n (count (:scenarios ir)))))

(defn pipeline-output-should-be-empty []
  (g/should= "" (g/get :pipeline-output)))

;; --- Step defs ---

(defgiven "a features directory containing:" pipeline/setup-features-dir!
  "Creates real files under the pipeline temp dir and deletes the entire base dir first. Sets :pipeline-dir.")

(defgiven "features directories containing:" pipeline/setup-features-dirs!
  "Creates real files under multiple relative roots inside the pipeline temp dir. Sets :features-dirs for subsequent pipeline runs.")

(defgiven "the feature {name:string} contains:" pipeline/write-feature-content!
  "Writes content to :pipeline-dir/{name}. Requires 'a features directory containing' to have run first.")

(defgiven "the parse stage has run" pipeline/parse-stage-has-run!
  "Runs the parse stage silently (stdout suppressed). Use as a Given prerequisite, not as the When step under test.")

(defgiven "step namespaces include pattern {pattern:string}" pipeline/step-namespace-pattern!
  "Stores glob pattern to :step-namespaces for use by subsequent pipeline runs.")

(defwhen "the parse stage runs" pipeline/run-parse-stage!)

(defwhen "the parse stage runs with :verbose" pipeline/run-parse-stage-verbose!)

(defwhen "the generate stage runs with framework {fw}" pipeline/run-generate-stage!)

(defwhen "the generate stage runs with framework {fw} and :verbose" pipeline/run-generate-stage-verbose!)

(defwhen "the full pipeline runs with framework {fw}" pipeline/run-full-pipeline!)

(defwhen "the full pipeline runs with framework {fw} and tags:" pipeline/run-full-pipeline-with-tags!)

(defwhen "the full pipeline runs with framework {fw} and locations:" pipeline/run-full-pipeline-with-locations!)

(defwhen "the full pipeline runs with options:" pipeline/run-full-pipeline-with-options!
  "Parses option values from a table: :keywords stay keywords, true/false become booleans, everything else stays string.")

(defthen "{path} should exist" pipeline/file-should-exist)

(defthen "{path} should not exist" pipeline/file-should-not-exist)

(defthen "{path} should exist and:" pipeline/file-should-exist-and)

(defthen #"^(\S+) should contain IR:$" pipeline/file-should-contain-ir)

(defthen "{path} should contain IR with {n:int} scenarios" pipeline/file-should-contain-ir-with-n-scenarios)

(defthen "the output should be empty" pipeline/pipeline-output-should-be-empty)
