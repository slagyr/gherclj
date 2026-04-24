(ns gherclj.features.steps.lifecycle-hooks
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.lifecycle :as lifecycle]
            [gherclj.sample.app-steps]
            [gherclj.frameworks.clojure-test]
            [gherclj.frameworks.speclj]
            [gherclj.generator :as gen]
            [gherclj.pipeline :as pipeline]))

(def ^:private base-dir
  (str (System/getProperty "java.io.tmpdir") "/gherclj-lifecycle-test"))

(defonce ^:private events (atom []))

(defn- features-dir [] (str base-dir "/features"))
(defn- edn-dir [] (str base-dir "/target/gherclj/edn"))
(defn- output-dir [] (str base-dir "/target/gherclj/generated"))

(defn- delete-recursively! [path]
  (let [f (io/file path)]
    (when (.exists f)
      (doseq [child (reverse (file-seq f))]
        (.delete child)))))

(defn- clean-test-dir! []
  (delete-recursively! base-dir))

(defn record-event! [event]
  (swap! events conj event))

(defn clear-events! []
  (reset! events []))

(defn events-recorded []
  @events)

(defn- register-recording-hook! [register! event]
  (register! #(record-event! event)))

(defn- reset-lifecycle-state! []
  (clean-test-dir!)
  (clear-events!)
  (lifecycle/clear!)
  (g/reset!))

(defn- pipeline-config [framework]
  {:features-dir (features-dir)
   :edn-dir (edn-dir)
   :output-dir (output-dir)
   :step-namespaces ['gherclj.sample.app-steps]
   :test-framework framework})

(defn- ensure-feature-file! [source]
  (let [f (io/file (features-dir) source)]
    (io/make-parents f)
    f))

(defn- feature-text [{:keys [feature scenarios]}]
  (str "Feature: " feature "\n\n"
       (str/join
        "\n\n"
        (for [{:keys [scenario steps]} scenarios]
          (str "  Scenario: " scenario "\n"
               (str/join
                "\n"
                (for [{:keys [type text]} steps]
                  (str "    " (str/capitalize (name type)) " " text))))))))

(defn- write-feature-ir! []
  (let [{:keys [source] :as ir} (g/get :feature-ir)]
    (spit (ensure-feature-file! source) (feature-text ir))))

(defn- direct-clojure-test-run! []
  (let [dir (io/file (output-dir))
        test-files (->> (file-seq dir)
                        (filter #(str/ends-with? (.getName %) ".clj"))
                        (sort-by #(str (.toPath %))))
        test-nses (mapv (fn [f]
                          (let [forms (read-string (str "[" (slurp f) "]"))
                                ns-form (first (filter #(and (seq? %) (= 'ns (first %))) forms))]
                            (second ns-form)))
                        test-files)]
    (doseq [ns-sym test-nses]
      (when (find-ns ns-sym)
        (remove-ns ns-sym)))
    (doseq [f test-files]
      (load-file (.getPath f)))
    (let [nses (keep find-ns test-nses)]
      (binding [clojure.test/*test-out* (java.io.StringWriter.)]
        (apply clojure.test/run-tests nses)))))

(defgiven lifecycle-recording-enabled "lifecycle event recording is enabled"
  []
  (reset-lifecycle-state!))

(defgiven before-all-records "a before-all hook records {event:string}"
  [event]
  (register-recording-hook! g/before-all event))

(defgiven before-feature-records "a before-feature hook records {event:string}"
  [event]
  (register-recording-hook! g/before-feature event))

(defgiven before-scenario-records "a before-scenario hook records {event:string}"
  [event]
  (register-recording-hook! g/before-scenario event))

(defgiven after-scenario-records "an after-scenario hook records {event:string}"
  [event]
  (register-recording-hook! g/after-scenario event))

(defgiven after-feature-records "an after-feature hook records {event:string}"
  [event]
  (register-recording-hook! g/after-feature event))

(defgiven after-all-records "an after-all hook records {event:string}"
  [event]
  (register-recording-hook! g/after-all event))

(defgiven generated-feature-file "a generated feature file {source:string} with scenario {scenario:string}"
  [source scenario]
  (let [ir {:feature (str/capitalize (str/replace source #"\.feature$" ""))
            :source source
            :scenarios [{:scenario scenario
                         :steps [{:type :given :text "a user \"alice\""}
                                 {:type :when :text "the user logs in"}
                                 {:type :then :text "the response should be 200"}]}]}]
    (spit (ensure-feature-file! source) (feature-text ir))))

(defwhen generate-with-lifecycle-hooks "generating the spec with framework {framework} and lifecycle hooks enabled"
  [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (g/assoc! :generated-output (gen/generate-spec {:step-namespaces ['gherclj.sample.app-steps]
                                                    :test-framework fw}
                                                   (g/get :feature-ir)))))

(defwhen run-directly "the generated scenarios run directly with framework {framework}"
  [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (write-feature-ir!)
    (pipeline/run! (pipeline-config fw))
    (g/assoc! :run-result (case fw
                            :clojure.test (direct-clojure-test-run!)
                            :speclj (throw (ex-info "direct speclj run not implemented for lifecycle feature" {}))))))

(defwhen run-through-gherclj "the generated scenarios run through gherclj with framework {framework}"
  [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (write-feature-ir!)
    (pipeline/run! (pipeline-config fw))
    (g/assoc! :run-result
              (gen/run-specs {:output-dir (output-dir)
                              :test-framework fw}))))

(defthen recorded-events-should-be "the recorded lifecycle events should be:"
  [table]
  (g/should= (mapv first (:rows table)) (events-recorded)))

(defthen recorded-events-should-be-empty "the recorded events should be empty"
  []
  (g/should= [] (events-recorded)))

(defthen run-should-fail "the run should fail"
  []
  (let [result (g/get :run-result)]
    (g/should (or (number? result)
                  (map? result)))
    (g/should (cond
                (number? result) (pos? result)
                (map? result) (pos? (+ (:fail result 0) (:error result 0)))
                :else false))))
