(ns gherclj.features.runner
  (:require [gherclj.pipeline :as pipeline]
            [gherclj.features.steps.template-compilation]
            [gherclj.features.steps.step-definitions]
            [gherclj.features.steps.code-generation]
            [gherclj.features.steps.step-patterns]
            [speclj.cli :as speclj]))

(defn -main [& _args]
  (pipeline/run!
    {:features-dir "features"
     :edn-dir "target/gherclj/edn"
     :output-dir "target/gherclj/generated"
     :step-namespaces ['gherclj.features.steps.template-compilation
                       'gherclj.features.steps.step-definitions
                       'gherclj.features.steps.code-generation
                       'gherclj.features.steps.step-patterns]
     :harness-ns 'gherclj.features.harness
     :test-framework :speclj
     :verbose true})
  (speclj/run "-c" "target/gherclj/generated" "-s" "src"))
