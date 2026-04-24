(ns gherclj.framework)

(defmulti generate-preamble
  "Generate the preamble string for a generated test file (namespace declaration, imports)."
  (fn [config & _] (:framework config)))

(defmulti wrap-feature
  "Wrap scenario blocks in a feature-level container."
  (fn [config & _] (:framework config)))

(defmulti wrap-scenario
  "Wrap step calls in a scenario-level form."
  (fn [config & _] (:framework config)))

(defmulti wrap-pending
  "Generate a pending/skipped scenario form."
  (fn [config & _] (:framework config)))

(defmulti render-step
  "Render a classified step as a framework-specific code string."
  (fn [config _step] (:framework config)))

(defmulti run-specs
  "Execute generated spec files."
  (fn [config] (:framework config)))
