(ns gherclj.features.steps.code-generation
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]))

(defgiven setup-feature "a feature named \"{name}\" from source \"{source}\""
  [name source]
  (h/set-feature! name source))

(defgiven add-scenario "a scenario \"{title}\" with steps:"
  [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (h/add-scenario! title steps)))

(defwhen generate-spec "generating the spec with framework {framework}"
  [framework]
  (h/generate-spec! (keyword framework)
                     ['gherclj.features.steps.step-definitions]))

(defthen output-should-contain "the output should contain \"{expected}\""
  [expected]
  (h/generated-output))
