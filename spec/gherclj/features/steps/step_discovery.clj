(ns gherclj.features.steps.step-discovery
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.discovery :as discovery]
            [clojure.edn :as edn]))

(helper! gherclj.features.steps.step-discovery)

(defn a-config! [doc-string]
  (g/assoc! :test-config (edn/read-string doc-string)))

(defn namespaces-on-classpath! [table]
  (let [nses (mapv #(symbol (first %)) (:rows table))]
    (g/assoc! :available-nses nses)))

(defn resolve-step-namespaces! []
  (let [config (g/get :test-config)
        entries (or (:step-namespaces config) [])
        available (or (g/get :available-nses) [])]
    (g/assoc! :resolved-nses (discovery/resolve-step-namespaces entries available))))

(defn resolved-step-namespaces-should-be [doc-string]
  (g/should= (edn/read-string doc-string) (g/get :resolved-nses)))

(defgiven "a config:" step-discovery/a-config!)

(defgiven "namespaces on the classpath:" step-discovery/namespaces-on-classpath!
  "Provides a mock classpath to the resolver — does NOT reflect the real JVM classpath.")

(defwhen "step namespaces are resolved" step-discovery/resolve-step-namespaces!)

(defthen "the resolved step namespaces should be:" step-discovery/resolved-step-namespaces-should-be)
