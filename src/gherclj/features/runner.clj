(ns gherclj.features.runner
  (:require [gherclj.pipeline :as pipeline]
            [gherclj.features.steps.template-compilation]
            [gherclj.features.steps.step-definitions]
            [gherclj.features.steps.code-generation]
            [speclj.cli :as speclj]))

(defn -main [& _args]
  (pipeline/run!
    {:features-dir "features"
     :edn-dir "features/edn"
     :output-dir "features/generated"
     :step-namespaces ['gherclj.features.steps.template-compilation
                       'gherclj.features.steps.step-definitions
                       'gherclj.features.steps.code-generation]
     :harness-ns 'gherclj.features.harness
     :test-framework :speclj})
  (speclj/run "-c" "features/generated" "-s" "src"))
