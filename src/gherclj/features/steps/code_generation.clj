(ns gherclj.features.steps.code-generation
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [gherclj.features.harness :as h]
            [gherclj.features.steps.sample-app]
            [clojure.string :as str]))

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

(defgiven add-background "a background with steps:"
  [table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (h/set-background! steps)))

(defgiven add-wip-scenario "a wip scenario \"{title}\" with steps:"
  [title table]
  (let [{:keys [headers rows]} table
        steps (mapv (fn [row]
                      (let [m (zipmap headers row)]
                        {:type (keyword (get m "type"))
                         :text (get m "text")}))
                    rows)]
    (h/add-wip-scenario! title steps)))

(defwhen generate-spec "generating the spec with framework {framework}"
  [framework]
  (let [fw (keyword (str/replace framework #"^:" ""))]
    (h/generate-spec! fw ['gherclj.features.steps.sample-app])))

(defthen output-should-contain "the output should contain \"{expected}\""
  [expected]
  (h/generated-output))

(defthen output-should-not-contain "the output should not contain \"{text}\""
  [text]
  (h/generated-output))

(defthen generated-code-should-be "the generated code should be:"
  [doc-string]
  (h/generated-output))
