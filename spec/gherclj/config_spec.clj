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
        (should (schema/error? (:test-framework result))))))

  (context "load-config"

    (it "returns defaults when no config file exists"
      (let [result (config/load-config {:root-path "/nonexistent"})]
        (should= "features" (:features-dir result))
        (should= "target/gherclj/edn" (:edn-dir result))
        (should= "target/gherclj/generated" (:output-dir result))
        (should= [] (:step-namespaces result))
        (should= :speclj (:test-framework result))
        (should= false (:verbose result))))

    (it "loads config from project root"
      (let [tmp (str (System/getProperty "java.io.tmpdir") "/gherclj-config-test")]
        (clojure.java.io/make-parents (clojure.java.io/file tmp "dummy"))
        (spit (str tmp "/gherclj.edn") "{:features-dir \"my-features\" :test-framework :clojure.test}")
        (try
          (let [result (config/load-config {:root-path tmp})]
            (should= "my-features" (:features-dir result))
            (should= :clojure.test (:test-framework result)))
          (finally
            (.delete (clojure.java.io/file tmp "gherclj.edn"))
            (.delete (clojure.java.io/file tmp))))))

    (it "rejects invalid values via resolve-config"
      (let [result (config/resolve-config {:test-framework :banana})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) "banana"))))

    (it "rejects invalid types via resolve-config"
      (let [result (config/resolve-config {:features-dir 42})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) "features-dir"))))

    (it "rejects unknown keys"
      (let [result (config/resolve-config {:turbo-mode true})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) "turbo-mode"))))

    (it "returns resolved config for valid input"
      (let [result (config/resolve-config {:features-dir "my-features"})]
        (should-not (config/invalid? result))
        (should= "my-features" (:features-dir result))))))
