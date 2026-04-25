(ns gherclj.frameworks.typescript.node-test-spec
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.frameworks.typescript.node-test :as ts]
            [speclj.core :refer :all]))

(ts/describe-setup! "let subject: typescript_app_steps.SampleAppSteps")
(ts/describe-setup! "beforeEach(() => {\n  subject = new typescript_app_steps.SampleAppSteps()\n})")

(describe "TypeScript node:test framework"

  (context "generate-preamble"

    (it "generates TypeScript preamble from step namespace registrations"
      (let [result (fw/generate-preamble {:framework :typescript/node-test}
                                         "features/auth.feature"
                                         ['gherclj.frameworks.typescript.node-test-spec])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "import { beforeEach, describe, test } from 'node:test'"))
        (should (str/includes? result "describe('Auth', () => {"))
        (should (str/includes? result "let subject: typescript_app_steps.SampleAppSteps"))))

    (it "omits describe setup when step namespace has none"
      (let [result (fw/generate-preamble {:framework :typescript/node-test}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "import { beforeEach, describe, test } from 'node:test'"))
        (should (str/includes? result "describe('Auth', () => {"))
        (should-not (str/includes? result "let subject")))))

  (context "render-step"

    (it "renders a step as a camelCase method call"
      (let [step {:name "subject.create-adventurer" :args ["alice"] :ns 'myapp.steps}]
        (should= "subject.createAdventurer('alice')" (fw/render-step {:framework :typescript/node-test} step))))

    (it "renders a no-arg step with parentheses"
      (let [step {:name "subject.enter-the-realm" :args [] :ns 'myapp.steps}]
        (should= "subject.enterTheRealm()" (fw/render-step {:framework :typescript/node-test} step))))

    (it "renders integer args as TypeScript literals"
      (let [step {:name "subject.verify-outcome" :args [200] :ns 'myapp.steps}]
        (should= "subject.verifyOutcome(200)" (fw/render-step {:framework :typescript/node-test} step)))))

  (context "wrap-scenario"

    (it "wraps pre-rendered lines in a test block"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["seedUser('alice')"
                                       "logIn()"]}
            result (fw/wrap-scenario {:framework :typescript/node-test} scenario nil)]
        (should (str/includes? result "test('User can log in', () => {"))
        (should (str/includes? result "seedUser('alice')"))
        (should (str/includes? result "logIn()"))))

    (it "includes background steps before scenario steps"
      (let [background {:rendered-steps ["cleanDb()"]}
            scenario {:scenario "User can log in"
                      :rendered-steps ["logIn()"]}
            result (fw/wrap-scenario {:framework :typescript/node-test} scenario background)]
        (should (str/includes? result "cleanDb()"))
        (should (str/includes? result "logIn()")))))

  (context "run-specs"

    (it "executes tsx against generated TypeScript files"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-typescript-framework-spec")
            output-dir (io/file tmp-dir "generated")
            a-test (io/file output-dir "a_test.ts")
            b-test (io/file output-dir "b_test.ts")
            captured (atom nil)]
        (io/make-parents a-test)
        (spit a-test "test('a', () => {})")
        (spit b-test "test('b', () => {})")
        (try
          (with-redefs [clojure.java.shell/sh (fn [& args]
                                                (reset! captured args)
                                                {:exit 0 :out "" :err ""})]
            (fw/run-specs {:framework :typescript/node-test :output-dir (str output-dir)}))
          (should= ["npx" "tsx" "--test" (str a-test) (str b-test)] @captured)
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
                         (fw/run-specs {:framework :typescript/node-test :output-dir "tmp/generated"}))))]
        (should (str/includes? stdout "ok\n"))
        (should (str/includes? stdout "warnings\n"))))))
