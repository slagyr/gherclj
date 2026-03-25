(ns gherclj.features.steps.step-discovery
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.discovery :as discovery]
            [clojure.edn :as edn]))

(defgiven a-config "a config:"
  [doc-string]
  (g/assoc! :test-config (edn/read-string doc-string)))

(defgiven namespaces-on-classpath "namespaces on the classpath:"
  [table]
  (let [nses (mapv #(symbol (first %)) (:rows table))]
    (g/assoc! :available-nses nses)))

(defwhen resolve-step-namespaces "step namespaces are resolved"
  []
  (let [config (g/get :test-config)
        entries (or (:step-namespaces config) [])
        available (or (g/get :available-nses) [])]
    (g/assoc! :resolved-nses (discovery/resolve-step-namespaces entries available))))

(defthen resolved-step-namespaces-should-be "the resolved step namespaces should be:"
  [doc-string]
  (g/should= (edn/read-string doc-string) (g/get :resolved-nses)))
