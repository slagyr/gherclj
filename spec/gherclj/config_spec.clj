(ns gherclj.config-spec
  (:require [speclj.core :refer :all]
            [gherclj.config :as config]
            [c3kit.apron.schema :as schema]))

(describe "Config"

  (context "pipeline-schema"

    (it "conforms a full config"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :edn-dir "target/gherclj/edn"
                       :output-dir "target/gherclj/generated"
                       :step-namespaces ['my.steps]
                       :framework :clojure/speclj
                       :verbose true})]
        (should= ["features"] (:features-dirs result))
        (should= :clojure/speclj (:framework result))
        (should= true (:verbose result))))

    (it "applies default features-dirs"
      (let [result (schema/conform config/pipeline-schema
                     {:framework :clojure/speclj})]
        (should= ["features"] (:features-dirs result))))

    (it "applies defaults for edn-dir"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :clojure/speclj})]
        (should= "target/gherclj/edn" (:edn-dir result))))

    (it "applies defaults for output-dir"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :clojure/speclj})]
        (should= "target/gherclj/generated" (:output-dir result))))

    (it "applies defaults for step-namespaces"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :clojure/speclj})]
        (should= [] (:step-namespaces result))))

    (it "applies defaults for exclude-tags"
      (let [result (schema/conform config/pipeline-schema
                       {:features-dirs ["features"]
                        :framework :clojure/speclj})]
        (should= [] (:exclude-tags result))))

    (it "applies default false for ir-edn"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :clojure/speclj})]
        (should= false (:ir-edn result))))

    (it "coerces verbose to boolean"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :clojure/speclj
                       :verbose nil})]
        (should= false (:verbose result))))

    (it "coerces ir-edn to boolean"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :clojure/speclj
                       :ir-edn true})]
        (should= true (:ir-edn result))))

    (it "rejects invalid test-framework"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :invalid})]
        (should (schema/error? (:framework result)))))

    (it "accepts :ruby/rspec as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                     {:features-dirs ["features"]
                       :framework :ruby/rspec})]
        (should= :ruby/rspec (:framework result))))

    (it "accepts :typescript/node-test as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                       {:features-dirs ["features"]
                        :framework :typescript/node-test})]
        (should= :typescript/node-test (:framework result))))

    (it "accepts :python/pytest as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                       {:features-dirs ["features"]
                        :framework :python/pytest})]
        (should= :python/pytest (:framework result))))

    (it "accepts :javascript/node-test as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                        {:features-dirs ["features"]
                         :framework :javascript/node-test})]
        (should= :javascript/node-test (:framework result))))

    (it "accepts :bash/testing as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                        {:features-dirs ["features"]
                         :framework :bash/testing})]
        (should= :bash/testing (:framework result))))

    (it "accepts :csharp/xunit as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                        {:features-dirs ["features"]
                         :framework :csharp/xunit})]
        (should= :csharp/xunit (:framework result))))

    (it "accepts :rust/rustc-test as a test-framework"
      (let [result (schema/conform config/pipeline-schema
                      {:features-dirs ["features"]
                        :framework :rust/rustc-test})]
        (should= :rust/rustc-test (:framework result)))))

  (context "load-config"

    (it "returns defaults when no config file exists"
      (let [result (config/load-config {:root-path "/nonexistent"})]
        (should= ["features"] (:features-dirs result))
        (should= "target/gherclj/edn" (:edn-dir result))
        (should= "target/gherclj/generated" (:output-dir result))
        (should= [] (:step-namespaces result))
        (should= [] (:exclude-tags result))
        (should= :clojure/speclj (:framework result))
        (should= false (:verbose result))))

    (it "loads config from project root"
      (let [tmp (str (System/getProperty "java.io.tmpdir") "/gherclj-config-test")]
        (clojure.java.io/make-parents (clojure.java.io/file tmp "dummy"))
        (spit (str tmp "/gherclj.edn") "{:features-dirs [\"my-features\"] :framework :clojure/test}")
        (try
          (let [result (config/load-config {:root-path tmp})]
            (should= ["my-features"] (:features-dirs result))
            (should= :clojure/test (:framework result)))
          (finally
            (.delete (clojure.java.io/file tmp "gherclj.edn"))
            (.delete (clojure.java.io/file tmp))))))

    (it "rejects invalid values via resolve-config"
      (let [result (config/resolve-config {:framework :banana})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) "banana"))))

    (it "rejects invalid types via resolve-config"
      (let [result (config/resolve-config {:features-dirs 42})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) "features-dirs"))))

    (it "rejects the legacy singular features-dir key"
      (let [result (config/resolve-config {:features-dir "features"})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) ":features-dir is no longer supported"))
        (should (clojure.string/includes? (config/error-message result) ":features-dirs"))))

    (it "rejects unknown keys"
      (let [result (config/resolve-config {:turbo-mode true})]
        (should (config/invalid? result))
        (should (clojure.string/includes? (config/error-message result) "turbo-mode"))))

    (it "returns resolved config for valid input"
      (let [result (config/resolve-config {:features-dirs ["my-features"]})]
        (should-not (config/invalid? result))
        (should= ["my-features"] (:features-dirs result))))))
