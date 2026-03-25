(ns gherclj.config
  (:require [c3kit.apron.schema :as schema]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

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

(defn resolve-config
  "Validate and apply defaults to a config map. Returns the resolved config
   or a map with :_invalid and :_message keys on error."
  [config]
  (let [unknown-keys (remove (set (keys pipeline-schema)) (keys config))]
    (if (seq unknown-keys)
      {:_invalid true
       :_message (str "Unknown config keys: " (str/join ", " (map name unknown-keys)))}
      (let [result (schema/conform pipeline-schema config)
            errors (filter (fn [[_ v]] (schema/error? v)) result)]
        (if (seq errors)
          {:_invalid true
           :_message (str "Invalid config: "
                          (str/join ", " (map (fn [[k v]] (str (name k) " " (schema/error-message v))) errors)))}
          result)))))

(defn invalid? [result]
  (:_invalid result))

(defn error-message [result]
  (:_message result))

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
    (resolve-config file-config)))
