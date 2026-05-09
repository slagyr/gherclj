;; mutation-tested: 2026-03-25
(ns gherclj.config
  (:require [c3kit.apron.schema :as schema]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- normalize-path [path]
  (-> path
      (str/replace "\\" "/")
      (str/replace #"^\./" "")))

(defn- absolute-path? [path]
  (.isAbsolute (io/file path)))

(defn- glob-pattern? [path]
  (boolean (re-find #"[*?\[{]" path)))

(defn- literal-prefix [pattern]
  (->> (str/split (normalize-path pattern) #"/")
       (take-while #(not (glob-pattern? %)))
       (str/join "/")))

(defn- search-root [root-path pattern]
  (let [prefix (literal-prefix pattern)]
    (cond
      (and (absolute-path? pattern) (seq prefix)) (str (io/file "/" prefix))
      (absolute-path? pattern) "/"
      (seq prefix) (str (io/file root-path prefix))
      :else root-path)))

(defn- candidate-path [root-path pattern file]
  (if (absolute-path? pattern)
    (normalize-path (str file))
    (normalize-path (str (.relativize (.toPath (io/file root-path)) (.toPath file))))))

(defn- expand-glob [root-path pattern]
  (let [matcher (.getPathMatcher (java.nio.file.FileSystems/getDefault)
                                 (str "glob:" (normalize-path pattern)))
        root (io/file (search-root root-path pattern))]
    (when (.exists root)
      (->> (file-seq root)
           (filter #(.isDirectory %))
           (map #(candidate-path root-path pattern %))
           (filter #(.matches matcher (.toPath (io/file %))))
           sort
           vec))))

(defn- resolve-features-dirs [features-dirs root-path]
  (let [expanded (reduce (fn [resolved dir]
                           (if (glob-pattern? dir)
                             (let [matches (expand-glob root-path dir)]
                               (if (seq matches)
                                 (into resolved matches)
                                 (reduced {:_invalid true
                                           :_message (str "no directories matched: " dir)})))
                             (conj resolved (normalize-path dir))))
                         []
                         features-dirs)]
    (cond
      (map? expanded) expanded
      (empty? expanded) {:_invalid true
                         :_message "no feature roots resolved"}
      :else expanded)))

(def pipeline-schema
  {:features-dirs   {:type     :seq
                     :coerce   (fn [v] (or v ["features"]))
                     :validate sequential?}
   :edn-dir         {:type   :string
                     :coerce (fn [v] (or v "target/gherclj/edn"))}
   :output-dir      {:type   :string
                     :coerce (fn [v] (or v "target/gherclj/generated"))}
   :step-namespaces {:type   :seq
                     :coerce (fn [v] (or v []))}
   :ir-edn          {:type   :boolean
                     :coerce (fn [v] (boolean v))}
   :framework       {:type     :keyword
                     :coerce   (fn [v] (or v :clojure/speclj))
                     :validate #(contains? #{:clojure/speclj :clojure/test :bash/testing :javascript/node-test :ruby/rspec :python/pytest :go/testing :typescript/node-test :rust/rustc-test :csharp/xunit :java/junit5} %)}
   :verbose         {:type   :boolean
                     :coerce (fn [v] (boolean v))}
   :framework-opts  {:type   :seq
                     :coerce (fn [v] (or v []))}
   :include-tags    {:type   :seq
                     :coerce (fn [v] (or v []))}
   :exclude-tags    {:type   :seq
                     :coerce (fn [v] (or v []))}})

(defn- read-config-file
  "Read a gherclj.edn file, returning nil if not found."
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn resolve-config
  "Validate and apply defaults to a config map. Returns the resolved config
   or a map with :_invalid and :_message keys on error."
  ([config]
   (resolve-config config {:root-path "."}))
  ([config {:keys [root-path] :or {root-path "."}}]
   (let [unknown-keys (remove (set (keys pipeline-schema)) (keys config))]
     (cond
       (contains? config :features-dir)
       {:_invalid true
        :_message ":features-dir is no longer supported; use :features-dirs (a list)"}

       (seq unknown-keys)
       {:_invalid true
        :_message (str "Unknown config keys: " (str/join ", " (map name unknown-keys)))}

       :else
       (let [present-schema (select-keys pipeline-schema (keys config))
             validated (when (seq present-schema) (schema/validate present-schema config))
             val-errors (filter (fn [[_ v]] (schema/error? v)) validated)]
         (if (seq val-errors)
           {:_invalid true
            :_message (str "Invalid config: "
                           (str/join ", " (map (fn [[k v]]
                                                 (str (name k) " — " (pr-str (clojure.core/get config k))
                                                      " " (schema/error-message v)))
                                               val-errors)))}
           (let [resolved (schema/conform pipeline-schema config)
                 features-dirs (resolve-features-dirs (:features-dirs resolved) root-path)]
             (if (:_invalid features-dirs)
               features-dirs
               (assoc resolved :features-dirs features-dirs)))))))))

(defn invalid? [result]
  (:_invalid result))

(defn error-message [result]
  (:_message result))

(defn raw-config
  "Load gherclj.edn without applying defaults or validation."
  [& [{:keys [root-path] :or {root-path "."}}]]
  (let [root-file (str root-path "/gherclj.edn")
        file-config (or (read-config-file root-file)
                        (when-let [r (io/resource "gherclj.edn")]
                          (edn/read-string (slurp r)))
                        {})]
    file-config))

(defn load-config
  "Load pipeline config. Resolution: schema defaults -> gherclj.edn.
   Options:
     :root-path - project root to search for gherclj.edn (default: \".\")"
  [& [{:keys [root-path] :or {root-path "."}}]]
  (resolve-config (raw-config {:root-path root-path}) {:root-path root-path}))
