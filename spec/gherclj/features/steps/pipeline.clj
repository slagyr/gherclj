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

(defn- clean-base-dir! []
  (let [root (io/file base-dir)]
    (when (.exists root)
      (doseq [f (reverse (file-seq root))]
        (.delete f)))))

(defn- pipeline-config [& {:keys [verbose framework include-tags exclude-tags locations]}]
  (cond-> {:features-dir (g/get :pipeline-dir)
           :edn-dir (edn-dir)
           :output-dir (output-dir)
           :step-namespaces (or (g/get :step-namespaces) [])}
    verbose (assoc :verbose true)
    framework (assoc :framework framework)
    include-tags (assoc :include-tags include-tags)
    exclude-tags (assoc :exclude-tags exclude-tags)
    locations (assoc :locations locations)))

(defn- strip-quotes [s]
  (if (and (str/starts-with? s "\"") (str/ends-with? s "\""))
    (subs s 1 (dec (count s)))
    s))

;; --- Given steps ---

(defgiven setup-features-dir "a features directory containing:"
  "Creates real files under the pipeline temp dir and deletes the entire base dir first. Sets :pipeline-dir."
  [table]
  (let [dir (str base-dir "/features")]
    (clean-base-dir!)
    (io/make-parents (io/file dir "dummy"))
    (doseq [row (:rows table)]
      (let [filename (first row)
            f (io/file dir filename)]
        (io/make-parents f)
        (spit f "")))
    (g/assoc! :pipeline-dir dir)))

(defgiven write-feature-content "the feature {name:string} contains:"
  "Writes content to :pipeline-dir/{name}. Requires 'a features directory containing' to have run first."
  [name doc-string]
  (spit (io/file (g/get :pipeline-dir) name) doc-string))

(defgiven parse-stage-has-run "the parse stage has run"
  "Runs the parse stage silently (stdout suppressed). Use as a Given prerequisite, not as the When step under test."
  []
  (let [output (with-out-str (pipeline/parse! (pipeline-config)))]
    (g/assoc! :pipeline-output output)))

(defgiven step-namespace-pattern "step namespaces include pattern {pattern:string}"
  "Stores glob pattern to :step-namespaces for use by subsequent pipeline runs."
  [pattern]
  (g/assoc! :step-namespaces [pattern]))

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

(defwhen run-full-pipeline-with-tags "the full pipeline runs with framework {fw} and tags:"
  [fw table]
  (let [framework (keyword (str/replace fw #"^:" ""))
        tags (mapv first (:rows table))
        includes (vec (remove #(str/starts-with? % "~") tags))
        excludes (mapv #(subs % 1) (filter #(str/starts-with? % "~") tags))
        output (with-out-str
                  (pipeline/run! (pipeline-config :framework framework
                                                 :include-tags includes
                                                 :exclude-tags excludes)))]
    (g/assoc! :pipeline-output output)))

(defwhen run-full-pipeline-with-locations "the full pipeline runs with framework {fw} and locations:"
  [fw table]
  (let [framework (keyword (str/replace fw #"^:" ""))
        locations (mapv (fn [[selector]]
                          (if-let [[_ source line] (re-matches #"^(.+\.feature):(\d+)$" selector)]
                            {:source source :line (Long/parseLong line)}
                            {:source selector}))
                        (:rows table))
        output (with-out-str
                 (pipeline/run! (pipeline-config :framework framework
                                                :locations locations)))]
    (g/assoc! :pipeline-output output)))

;; --- Then steps ---

(defthen file-should-exist "{path} should exist"
  [path]
  (g/should (.exists (io/file base-dir (strip-quotes path)))))

(defthen file-should-not-exist "{path} should not exist"
  [path]
  (g/should-not (.exists (io/file base-dir (strip-quotes path)))))

(defthen file-should-exist-and "{path} should exist and:"
  [path table]
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

(defthen file-should-contain-ir #"^(\S+) should contain IR:$"
  [path doc-string]
  (let [p (strip-quotes path)
        actual (edn/read-string (slurp (io/file base-dir p)))
        expected (edn/read-string doc-string)]
    (g/should= expected actual)))

(defthen file-should-contain-ir-with-n-scenarios "{path} should contain IR with {n:int} scenarios"
  [path n]
  (let [ir (edn/read-string (slurp (io/file base-dir (strip-quotes path))))]
    (g/should= n (count (:scenarios ir)))))

(defthen pipeline-output-should-be-empty "the output should be empty"
  []
  (g/should= "" (g/get :pipeline-output)))
