(ns gherclj.features.steps.pipeline-config
  (:require [gherclj.core :as g :refer [defgiven defwhen helper!]]
            [gherclj.config :as config]))

(helper! gherclj.features.steps.pipeline-config)

(defn empty-config! []
  (g/assoc! :test-config {}))

(defn resolve-config! []
  (let [result (config/resolve-config (g/get :test-config))]
    (g/assoc! :loaded-config result)))

(defgiven "an empty config" pipeline-config/empty-config!)

(defwhen "the config is resolved" pipeline-config/resolve-config!)
