(ns gherclj.config-spec
  (:require [speclj.core :refer :all]
            [gherclj.config :as config]
            [c3kit.apron.schema :as schema]))

(describe "Config"

  (context "pipeline-schema"

    (it "conforms a full config"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dir "features"
                      :edn-dir "target/gherclj/edn"
                      :output-dir "target/gherclj/generated"
                      :step-namespaces ['my.steps]
                      :test-framework :speclj
                      :verbose true})]
        (should= "features" (:features-dir result))
        (should= :speclj (:test-framework result))
        (should= true (:verbose result))))

    (it "applies defaults for edn-dir"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dir "features"
                      :test-framework :speclj})]
        (should= "target/gherclj/edn" (:edn-dir result))))

    (it "applies defaults for output-dir"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dir "features"
                      :test-framework :speclj})]
        (should= "target/gherclj/generated" (:output-dir result))))

    (it "applies defaults for step-namespaces"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dir "features"
                      :test-framework :speclj})]
        (should= [] (:step-namespaces result))))

    (it "coerces verbose to boolean"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dir "features"
                      :test-framework :speclj
                      :verbose nil})]
        (should= false (:verbose result))))

    (it "rejects invalid test-framework"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dir "features"
                      :test-framework :invalid})]
        (should (schema/error? (:test-framework result)))))))
