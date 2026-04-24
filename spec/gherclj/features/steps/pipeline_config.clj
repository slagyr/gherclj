(ns gherclj.features.steps.pipeline-config
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.config :as config]
            [clojure.edn :as edn]))

(defgiven empty-config "an empty config"
  []
  (g/assoc! :test-config {}))

(defwhen resolve-config "the config is resolved"
  []
  (let [result (config/resolve-config (g/get :test-config))]
    (g/assoc! :loaded-config result)))
