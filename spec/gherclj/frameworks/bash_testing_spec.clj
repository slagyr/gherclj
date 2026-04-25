(ns gherclj.frameworks.bash.testing-spec
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.core :refer [helper!]]
            [gherclj.framework :as fw]
            [gherclj.frameworks.bash.testing :as bash]
            [speclj.core :refer :all]))

(helper! "spec/gherclj/frameworks/bash_testing_spec.clj")

(bash/scenario-setup! "subject_new")

(describe "Bash testing framework"

  (context "generate-preamble"

    (it "generates Bash preamble from step namespace registrations"
      (let [result (fw/generate-preamble {:framework :bash/testing
                                          :output-dir "target/gherclj/generated"}
                                         "features/auth.feature"
                                         ['gherclj.frameworks.bash.testing-spec])]
        (should (str/includes? result "#!/usr/bin/env bash"))
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "source \"$SCRIPT_DIR/"))))

    (it "omits helper imports when step namespace has none"
      (let [result (fw/generate-preamble {:framework :bash/testing
                                          :output-dir "target/gherclj/generated"}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "#!/usr/bin/env bash"))
        (should-not (str/includes? result "source \"$SCRIPT_DIR/")))))

  (context "render-step"

    (it "renders a step as a snake_case function call"
      (let [step {:name "subject.create-adventurer" :args ["alice"] :ns 'myapp.steps}]
        (should= "subject_create_adventurer 'alice'" (fw/render-step {:framework :bash/testing} step))))

    (it "renders a no-arg step with no parentheses"
      (let [step {:name "subject.enter-the-realm" :args [] :ns 'myapp.steps}]
        (should= "subject_enter_the_realm" (fw/render-step {:framework :bash/testing} step))))

    (it "renders integer args as shell literals"
      (let [step {:name "subject.verify-outcome" :args [200] :ns 'myapp.steps}]
        (should= "subject_verify_outcome 200" (fw/render-step {:framework :bash/testing} step)))))

  (context "wrap-scenario"

    (it "wraps setup and step calls in a scenario function"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["log_in"]
                      :steps [{:ns 'gherclj.frameworks.bash.testing-spec}]}
            result (fw/wrap-scenario {:framework :bash/testing} scenario nil)]
        (should (str/includes? result "user_can_log_in() {"))
        (should (str/includes? result "  subject_new"))
        (should (str/includes? result "  log_in"))
        (should (str/includes? result "run_test 'User can log in' user_can_log_in"))))

    (it "includes background steps before scenario steps"
      (let [background {:rendered-steps ["clean_db"]}
            scenario {:scenario "User can log in"
                      :rendered-steps ["log_in"]
                      :steps [{:ns 'gherclj.frameworks.bash.testing-spec}]}
            result (fw/wrap-scenario {:framework :bash/testing} scenario background)]
        (should (str/includes? result "  clean_db"))
        (should (str/includes? result "  log_in")))))

  (context "run-specs"

    (it "executes bash against generated shell test files"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-bash-framework-spec")
            output-dir (io/file tmp-dir "generated")
            a-test (io/file output-dir "a_test.sh")
            b-test (io/file output-dir "b_test.sh")
            captured (atom nil)]
        (io/make-parents a-test)
        (spit a-test "#!/usr/bin/env bash")
        (spit b-test "#!/usr/bin/env bash")
        (try
          (with-redefs [clojure.java.shell/sh (fn [& args]
                                                (swap! captured conj args)
                                                {:exit 0 :out "" :err ""})]
            (reset! captured [])
            (fw/run-specs {:framework :bash/testing :output-dir (str output-dir)}))
          (should= [["bash" (str a-test)]
                    ["bash" (str b-test)]]
                   @captured)
          (finally
            (.delete a-test)
            (.delete b-test)
            (.delete output-dir)
            (.delete (io/file tmp-dir))))))

    (it "prints bash stdout and stderr"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-bash-framework-spec-output")
            output-dir (io/file tmp-dir "generated")
            test-file (io/file output-dir "a_test.sh")
            stdout (do
                     (io/make-parents test-file)
                     (spit test-file "#!/usr/bin/env bash")
                     (try
                       (with-out-str
                         (with-redefs [clojure.java.shell/sh (fn [& _]
                                                               {:exit 0 :out "ok\n" :err "warnings\n"})]
                           (binding [*err* *out*]
                             (fw/run-specs {:framework :bash/testing :output-dir (str output-dir)}))))
                       (finally
                         (.delete test-file)
                         (.delete output-dir)
                         (.delete (io/file tmp-dir)))))]
        (should (str/includes? stdout "ok\n"))
        (should (str/includes? stdout "warnings\n"))))))
