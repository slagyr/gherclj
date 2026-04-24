(ns gherclj.features.steps.step-patterns
  (:require [clojure.string :as str]
            [gherclj.core :as g :refer [defgiven]]
            [gherclj.sample.dragon-steps]))

(defgiven lookup-registered-step "the registered step {name}"
  "Looks up by name from gherclj.sample.dragon-steps namespace only. Silently no-ops if not found."
  [name]
  (let [normalized-name (str/replace name #"^\"|\"$" "")
        steps (g/steps-in-ns 'gherclj.sample.dragon-steps)
        step (first (filter #(= normalized-name (:name %)) steps))]
    (when step
      (g/update! :steps (fnil conj []) step))))
