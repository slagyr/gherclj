(ns gherclj.features.steps.step-patterns
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven helper!]]
            [gherclj.sample.dragon-steps]))

(helper! gherclj.features.steps.step-patterns)

(defn lookup-registered-step! [name]
  (let [normalized-name (str/replace name #"^\"|\"$" "")
        steps (g/steps-in-ns 'gherclj.sample.dragon-steps)
        step (first (filter #(= normalized-name (:name %)) steps))]
    (when step
      (g/update! :steps (fnil conj []) step))))

(defgiven "the registered step {name}" step-patterns/lookup-registered-step!
  "Looks up by name from gherclj.sample.dragon-steps namespace only. Silently no-ops if not found.")
