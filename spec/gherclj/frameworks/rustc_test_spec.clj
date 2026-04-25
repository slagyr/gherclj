(ns gherclj.frameworks.rust.rustc-test-spec
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.frameworks.rust.rustc-test :as rust]
            [speclj.core :refer :all]))

(rust/scenario-setup! "let mut subject = sample_app::SampleAppSteps::new();")

(describe "Rust rustc-test framework"

  (context "generate-preamble"

    (it "generates Rust preamble from step namespace registrations"
      (let [result (fw/generate-preamble {:framework :rust/rustc-test}
                                         "features/auth.feature"
                                         ['gherclj.sample.rust-app-steps])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "mod sample_app;"))
        (should (str/includes? result "#[path = \"../../../../lib/sample_app.rs\"]"))))

    (it "omits module imports when step namespace has none"
      (let [result (fw/generate-preamble {:framework :rust/rustc-test}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should-not (str/includes? result "mod ")))))

  (context "render-step"

    (it "renders a step as a snake_case method call"
      (let [step {:name "subject.create-adventurer" :args ["alice"] :ns 'myapp.steps}]
        (should= "subject.create_adventurer(\"alice\");" (fw/render-step {:framework :rust/rustc-test} step))))

    (it "renders a no-arg step with parentheses"
      (let [step {:name "subject.enter-the-realm" :args [] :ns 'myapp.steps}]
        (should= "subject.enter_the_realm();" (fw/render-step {:framework :rust/rustc-test} step))))

    (it "renders integer args as Rust literals"
      (let [step {:name "subject.verify-outcome" :args [200] :ns 'myapp.steps}]
        (should= "subject.verify_outcome(200);" (fw/render-step {:framework :rust/rustc-test} step)))))

  (context "wrap-scenario"

    (it "wraps pre-rendered lines in a Rust #[test] function"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["seed_user(\"alice\")"
                                       "log_in()"]}
            result (fw/wrap-scenario {:framework :rust/rustc-test} scenario nil)]
        (should (str/includes? result "#[test]"))
        (should (str/includes? result "fn user_can_log_in() {"))
        (should (str/includes? result "seed_user(\"alice\")"))
        (should (str/includes? result "log_in()"))))

    (it "includes scenario setup and background steps before scenario steps"
      (let [background {:rendered-steps ["clean_db();"]}
            scenario {:scenario "User can log in"
                      :_used-nses ['gherclj.frameworks.rust.rustc-test-spec]
                      :rendered-steps ["log_in();"]}
            result (fw/wrap-scenario {:framework :rust/rustc-test :_used-nses ['gherclj.frameworks.rust.rustc-test-spec]} scenario background)]
        (should (str/includes? result "let mut subject = sample_app::SampleAppSteps::new();"))
        (should (str/includes? result "clean_db();"))
        (should (str/includes? result "log_in();")))))

  (context "run-specs"

    (it "compiles and runs each generated Rust test file with rustc --test"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-rust-framework-spec")
            output-dir (io/file tmp-dir "generated")
            target-dir (io/file tmp-dir "target")
            a-test (io/file output-dir "a_test.rs")
            b-test (io/file output-dir "b_test.rs")
            captured (atom [])]
        (io/make-parents a-test)
        (io/make-parents (io/file target-dir "dummy"))
        (spit a-test "#[test] fn a() {}")
        (spit b-test "#[test] fn b() {}")
        (try
          (with-redefs [clojure.java.shell/sh (fn [& args]
                                                (swap! captured conj args)
                                                {:exit 0 :out "" :err ""})]
            (fw/run-specs {:framework :rust/rustc-test
                           :output-dir (str output-dir)
                           :rust-target-dir (str target-dir)}))
          (should= ["rustc" "--edition=2021" "--test" (str a-test) "-o" (str (io/file target-dir "a_test"))]
                   (first @captured))
          (should= [(str (io/file target-dir "a_test"))]
                   (second @captured))
          (finally
            (.delete a-test)
            (.delete b-test)
            (.delete (io/file target-dir "a_test"))
            (.delete (io/file target-dir "b_test"))
            (.delete output-dir)
            (.delete target-dir)
            (.delete (io/file tmp-dir))))))

    (it "prints rustc test stdout and stderr"
      (let [tmp-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-rust-framework-stdout")
            output-dir (io/file tmp-dir "generated")
            target-dir (io/file tmp-dir "target")
            a-test (io/file output-dir "a_test.rs")
            stdout (with-out-str
                     (io/make-parents a-test)
                     (spit a-test "#[test] fn a() {}")
                     (try
                       (with-redefs [clojure.java.shell/sh (fn [& args]
                                                             (if (= "rustc" (first args))
                                                               {:exit 0 :out "" :err ""}
                                                               {:exit 0 :out "ok\n" :err "warnings\n"}))]
                         (binding [*err* *out*]
                           (fw/run-specs {:framework :rust/rustc-test
                                          :output-dir (str output-dir)
                                          :rust-target-dir (str target-dir)})))
                       (finally
                         (.delete a-test)
                         (.delete (io/file target-dir "a_test"))
                         (.delete output-dir)
                         (.delete target-dir)
                         (.delete (io/file tmp-dir)))))]
        (should (str/includes? stdout "ok\n"))
        (should (str/includes? stdout "warnings\n"))))))
