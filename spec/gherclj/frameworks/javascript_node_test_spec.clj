(ns gherclj.frameworks.javascript.node-test-spec
  (:require [clojure.java.io :as io]
             [clojure.string :as str]
            [gherclj.core :refer [helper!]]
            [gherclj.framework :as fw]
            [gherclj.frameworks.javascript.node-test :as js]
            [speclj.core :refer :all]))

(helper! "spec/gherclj/frameworks/javascript_node_test_spec.clj")

(js/scenario-setup! "const subject = new javascript_app_steps.SampleAppSteps()")

(describe "JavaScript node:test framework"

  (context "generate-preamble"

    (it "generates JavaScript preamble from step namespace registrations"
      (let [result (fw/generate-preamble {:framework :javascript/node-test
                                          :output-dir "target/gherclj/generated"}
                                         "features/auth.feature"
                                         ['gherclj.frameworks.javascript.node-test-spec])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "import test from 'node:test'"))
        (should (str/includes? result "import * as javascript_node_test_spec from "))))

    (it "omits helper imports when step namespace has none"
      (let [result (fw/generate-preamble {:framework :javascript/node-test
                                          :output-dir "target/gherclj/generated"}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "import test from 'node:test'"))
        (should-not (str/includes? result "import * as")))))

  (context "render-step"

    (it "renders a step as a camelCase method call"
      (let [step {:name "subject.create-adventurer" :args ["alice"] :ns 'myapp.steps}]
        (should= "subject.createAdventurer('alice')" (fw/render-step {:framework :javascript/node-test} step))))

    (it "renders a no-arg step with parentheses"
      (let [step {:name "subject.enter-the-realm" :args [] :ns 'myapp.steps}]
        (should= "subject.enterTheRealm()" (fw/render-step {:framework :javascript/node-test} step))))

    (it "renders integer args as JavaScript literals"
      (let [step {:name "subject.verify-outcome" :args [200] :ns 'myapp.steps}]
        (should= "subject.verifyOutcome(200)" (fw/render-step {:framework :javascript/node-test} step)))))

  (context "wrap-scenario"

    (it "wraps setup and step calls in a test block"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["logIn()"]
                      :steps [{:ns 'gherclj.frameworks.javascript.node-test-spec}]}
            result (fw/wrap-scenario {:framework :javascript/node-test} scenario nil)]
        (should (str/includes? result "test('User can log in', () => {"))
        (should (str/includes? result "const subject = new javascript_app_steps.SampleAppSteps()"))
        (should (str/includes? result "logIn()"))))

    (it "includes background steps before scenario steps"
      (let [background {:rendered-steps ["cleanDb()"]}
            scenario {:scenario "User can log in"
                      :rendered-steps ["logIn()"]
                      :steps [{:ns 'gherclj.frameworks.javascript.node-test-spec}]}
            result (fw/wrap-scenario {:framework :javascript/node-test} scenario background)]
        (should (str/includes? result "cleanDb()"))
        (should (str/includes? result "logIn()")))))

  (context "run-specs"

    (it "executes node --test against generated JavaScript files"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-javascript-framework-spec")
            output-dir (io/file tmp-dir "generated")
            a-test (io/file output-dir "a_test.js")
            b-test (io/file output-dir "b_test.js")
            captured (atom nil)]
        (io/make-parents a-test)
        (spit a-test "test('a', () => {})")
        (spit b-test "test('b', () => {})")
        (try
          (with-redefs [clojure.java.shell/sh (fn [& args]
                                                (reset! captured args)
                                                {:exit 0 :out "" :err ""})]
            (fw/run-specs {:framework :javascript/node-test :output-dir (str output-dir)}))
          (should= ["node" "--test" (str a-test) (str b-test)] @captured)
          (finally
            (.delete a-test)
            (.delete b-test)
            (.delete output-dir)
            (.delete (io/file tmp-dir))))))

    (it "prints node test stdout and stderr"
      (let [stdout (with-out-str
                     (with-redefs [clojure.java.shell/sh (fn [& _]
                                                           {:exit 0 :out "ok\n" :err "warnings\n"})]
                       (binding [*err* *out*]
                         (fw/run-specs {:framework :javascript/node-test :output-dir "tmp/generated"}))))]
        (should (str/includes? stdout "ok\n"))
        (should (str/includes? stdout "warnings\n"))))))
