(ns gherclj.features.steps.lifecycle-hooks
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.framework :as fw]
            [gherclj.lifecycle :as lifecycle]
            [gherclj.sample.app-steps]
            [gherclj.frameworks.clojure.test]
            [gherclj.frameworks.clojure.speclj]
            [gherclj.generator :as gen]
            [gherclj.pipeline :as pipeline]))

(helper! gherclj.features.steps.lifecycle-hooks)

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
   :step-namespaces ['gherclj.sample.app-steps 'gherclj.sample.dragon-steps]
   :framework framework})

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

(defn lifecycle-recording-enabled! []
  (reset-lifecycle-state!))

(defn before-all-records! [event]
  (register-recording-hook! g/before-all event))

(defn before-feature-records! [event]
  (register-recording-hook! g/before-feature event))

(defn before-scenario-records! [event]
  (register-recording-hook! g/before-scenario event))

(defn after-scenario-records! [event]
  (register-recording-hook! g/after-scenario event))

(defn after-feature-records! [event]
  (register-recording-hook! g/after-feature event))

(defn after-all-records! [event]
  (register-recording-hook! g/after-all event))

(defn generate-with-lifecycle-hooks! [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (g/assoc! :generated-output (gen/generate-spec {:step-namespaces ['gherclj.sample.app-steps]
                                                    :framework fw}
                                                   (g/get :feature-ir)))))

(defn run-directly! [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (write-feature-ir!)
    (pipeline/run! (pipeline-config fw))
    (g/assoc! :run-result (case fw
                            :clojure/test (direct-clojure-test-run!)
                            :clojure/speclj (throw (ex-info "direct speclj run not implemented for lifecycle feature" {}))))))

(defn run-through-gherclj! [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (write-feature-ir!)
    (pipeline/run! (pipeline-config fw))
    (g/assoc! :run-result
              (fw/run-specs {:output-dir (output-dir)
                              :framework fw}))))

(defn recorded-events-should-be [table]
  (g/should= (mapv first (:rows table)) (events-recorded)))

(defn run-should-fail []
  (let [result (g/get :run-result)]
    (g/should (or (number? result)
                  (map? result)))
    (g/should (cond
                (number? result) (pos? result)
                (map? result) (pos? (+ (:fail result 0) (:error result 0)))
                :else false))))

(defgiven "lifecycle event recording is enabled" lifecycle-hooks/lifecycle-recording-enabled!
  "Must be the first step in any lifecycle scenario. Resets state, clears recorded events, and clears all registered lifecycle hooks.")

(defgiven "a before-all hook records {event:string}" lifecycle-hooks/before-all-records!)

(defgiven "a before-feature hook records {event:string}" lifecycle-hooks/before-feature-records!)

(defgiven "a before-scenario hook records {event:string}" lifecycle-hooks/before-scenario-records!)

(defgiven "an after-scenario hook records {event:string}" lifecycle-hooks/after-scenario-records!)

(defgiven "an after-feature hook records {event:string}" lifecycle-hooks/after-feature-records!)

(defgiven "an after-all hook records {event:string}" lifecycle-hooks/after-all-records!)

(defwhen "generating the spec with framework {framework} and lifecycle hooks enabled" lifecycle-hooks/generate-with-lifecycle-hooks!)

(defwhen "the generated scenarios run directly with framework {framework}" lifecycle-hooks/run-directly!
  "Loads generated .clj files into the current JVM. Removes pre-existing namespaces before loading to avoid stale state.")

(defwhen "the generated scenarios run through gherclj with framework {framework}" lifecycle-hooks/run-through-gherclj!)

(defthen "the recorded lifecycle events should be:" lifecycle-hooks/recorded-events-should-be)

(defthen "the run should fail" lifecycle-hooks/run-should-fail)
