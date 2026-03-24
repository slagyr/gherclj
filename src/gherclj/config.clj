(ns gherclj.config
  (:require [c3kit.apron.schema :as schema]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def pipeline-schema
  {:features-dir    {:type   :string
                     :coerce (fn [v] (or v "features"))}
   :edn-dir         {:type   :string
                     :coerce (fn [v] (or v "target/gherclj/edn"))}
   :output-dir      {:type   :string
                     :coerce (fn [v] (or v "target/gherclj/generated"))}
   :step-namespaces {:type   :seq
                     :coerce (fn [v] (or v []))}
   :test-framework  {:type     :keyword
                     :coerce   (fn [v] (or v :speclj))
                     :validate #(contains? #{:speclj :clojure.test} %)}
   :verbose         {:type   :boolean
                     :coerce (fn [v] (boolean v))}})

(defn- read-config-file
  "Read a gherclj.edn file, returning nil if not found."
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn load-config
  "Load pipeline config. Resolution: schema defaults -> gherclj.edn.
   Options:
     :root-path - project root to search for gherclj.edn (default: \".\")"
  [& [{:keys [root-path] :or {root-path "."}}]]
  (let [root-file (str root-path "/gherclj.edn")
        file-config (or (read-config-file root-file)
                        (when-let [r (io/resource "gherclj.edn")]
                          (edn/read-string (slurp r)))
                        {})]
    (schema/conform pipeline-schema file-config)))
