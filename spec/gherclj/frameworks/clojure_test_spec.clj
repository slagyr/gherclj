(ns gherclj.frameworks.clojure-test-spec
  (:require [speclj.core :refer :all]
            [gherclj.framework :as fw]
            [gherclj.frameworks.clojure-test]
            [gherclj.core :as g]
            [gherclj.generator :as gen]
            [gherclj.lifecycle :as lifecycle]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(describe "Clojure.test framework"

  (context "generate-preamble"

    (it "generates a namespace form with clojure.test require and step requires"
      (let [result (fw/generate-preamble {:framework :clojure.test}
                                         "features/auth.feature"
                                         ['myapp.steps.auth 'myapp.steps.cart])]
        (should (str/includes? result "clojure.test :refer :all"))
        (should (str/includes? result "gherclj.core :as g"))
        (should (str/includes? result "myapp.steps.auth"))
        (should (str/includes? result "myapp.steps.cart"))))

    (it "generates a namespace form with no step requires"
      (let [result (fw/generate-preamble {:framework :clojure.test}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "clojure.test :refer :all"))
        (should-not (str/includes? result "myapp"))))

    (it "uses -test suffix instead of -spec"
      (let [result (fw/generate-preamble {:framework :clojure.test}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "auth-test")))))

  (context "wrap-feature"

    (it "generates fixture functions with lifecycle hooks"
      (let [result (fw/wrap-feature {:framework :clojure.test}
                                     "Authentication"
                                     "(deftest login-test)")]
        (should (str/includes? result "feature-fixture"))
        (should (str/includes? result "scenario-fixture"))
        (should (str/includes? result "use-fixtures"))
        (should (str/includes? result "run-before-feature-hooks!"))
        (should (str/includes? result "run-after-feature-hooks!"))
        (should (str/includes? result "run-before-scenario-hooks!"))
        (should (str/includes? result "run-after-scenario-hooks!"))
        (should (str/includes? result "(deftest login-test)")))))

  (context "wrap-scenario"

    (it "wraps steps in a deftest with slugified name"
      (let [scenario {:scenario "User Can Log In"
                      :steps [{:type :given :text "a user exists" :classified? true
                               :ns 'myapp.steps :name "summon-hero" :args []}
                              {:type :when :text "they log in" :classified? true
                               :ns 'myapp.steps :name "log-in" :args []}]}
            result (fw/wrap-scenario {:framework :clojure.test} scenario nil)]
        (should (str/includes? result "(deftest user-can-log-in"))
        (should (str/includes? result "(testing \"User Can Log In\""))
        (should (str/includes? result "steps/summon-hero"))
        (should (str/includes? result "steps/log-in"))))

    (it "includes background steps before scenario steps"
      (let [background {:steps [{:type :given :text "db is clean" :classified? true
                                 :ns 'myapp.steps :name "clean-db" :args []}]}
            scenario {:scenario "User can log in"
                      :steps [{:type :when :text "they log in" :classified? true
                               :ns 'myapp.steps :name "log-in" :args []}]}
            result (fw/wrap-scenario {:framework :clojure.test} scenario background)]
        (should (str/includes? result "steps/clean-db"))
        (should (str/includes? result "steps/log-in"))))

    (it "slugifies scenario names with special characters"
      (let [scenario {:scenario "User: Login & Logout!"
                      :steps [{:type :given :text "setup" :classified? true
                               :ns 'myapp.steps :name "setup" :args []}]}
            result (fw/wrap-scenario {:framework :clojure.test} scenario nil)]
        (should (str/includes? result "(deftest user-login-logout")))))

  (context "wrap-pending"

    (it "generates a pending deftest with TODO comment"
      (let [scenario {:scenario "Unimplemented feature"
                      :steps [{:type :given :text "something exists"}
                              {:type :when :text "action happens"}]}
            result (fw/wrap-pending {:framework :clojure.test} scenario nil)]
        (should (str/includes? result "(deftest unimplemented-feature"))
        (should (str/includes? result "TODO: not yet implemented"))))

    (it "uses slugified name for pending scenario"
      (let [scenario {:scenario "User Can Sign Up"
                      :steps []}
            result (fw/wrap-pending {:framework :clojure.test} scenario nil)]
        (should (str/includes? result "(deftest user-can-sign-up")))))

  (context "assertion methods"

    (around [it]
      (g/set-framework! :clojure.test)
      (it)
      (g/set-framework! nil))

    (it "should= passes when values are equal"
      ;; ct/do-report :pass does not throw
      (g/should= 42 42))

    (it "should passes for truthy value"
      (g/should true))

    (it "should-not passes for falsy value"
      (g/should-not false))

    (it "should-be-nil passes for nil"
      (g/should-be-nil nil))

    (it "should-not-be-nil passes for non-nil"
      (g/should-not-be-nil :something)))

  (context "assertion method failure paths"

    (around [it]
      (g/set-framework! :clojure.test)
      (it)
      (g/set-framework! nil))

    (it "should= reports :fail with message when values differ"
      (let [reported (atom nil)]
        (with-redefs [clojure.test/do-report #(reset! reported %)]
          (g/should= 1 2))
        (should= :fail (:type @reported))
        (should (str/includes? (:message @reported) "Expected"))))

    (it "should reports :fail for falsy value"
      (let [reported (atom nil)]
        (with-redefs [clojure.test/do-report #(reset! reported %)]
          (g/should nil))
        (should= :fail (:type @reported))))

    (it "should-not reports :fail for truthy value"
      (let [reported (atom nil)]
        (with-redefs [clojure.test/do-report #(reset! reported %)]
          (g/should-not :something))
        (should= :fail (:type @reported))))

    (it "should-be-nil reports :fail for non-nil value"
      (let [reported (atom nil)]
        (with-redefs [clojure.test/do-report #(reset! reported %)]
          (g/should-be-nil :not-nil))
        (should= :fail (:type @reported))))

    (it "should-not-be-nil reports :fail for nil"
      (let [reported (atom nil)]
        (with-redefs [clojure.test/do-report #(reset! reported %)]
          (g/should-not-be-nil nil))
        (should= :fail (:type @reported)))))

  (context "run-specs"

    (it "runs specs from the output directory"
      (let [output-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-ct-run-test")
            captured (atom [])]
        (io/make-parents (io/file output-dir "placeholder"))
        (try
          (with-redefs [gherclj.lifecycle/run-before-all-hooks! (fn [])
                        gherclj.lifecycle/run-after-all-hooks!  (fn [])]
            (fw/run-specs {:framework :clojure.test
                            :output-dir output-dir}))
          ;; No .clj files exist, so run-tests called with empty list — result is a map
          (finally
            (doseq [f (reverse (file-seq (io/file output-dir)))]
              (.delete f))))))

    (it "loads and runs clojure.test files covering read-ns-name and doseq paths"
      (let [output-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-ct-load-test")
            test-file (io/file output-dir "sample_test.clj")]
        (io/make-parents test-file)
        (spit test-file
              (str "(ns gherclj-ct-sample-ns\n"
                   "  (:require [clojure.test :refer :all]))\n"
                   "(deftest sample-pass (is (= 1 1)))"))
        ;; Pre-create namespace so (when (find-ns ns-sym) (remove-ns ...)) path is covered
        (create-ns 'gherclj-ct-sample-ns)
        (try
          (with-redefs [gherclj.lifecycle/run-before-all-hooks! (fn [])
                        gherclj.lifecycle/run-after-all-hooks!  (fn [])]
            (let [result (fw/run-specs {:framework :clojure.test
                                         :output-dir output-dir})]
              (should (pos? (:test result)))))
          (finally
            (doseq [f (reverse (file-seq (io/file output-dir)))] (.delete f))
            (when (find-ns 'gherclj-ct-sample-ns)
              (remove-ns 'gherclj-ct-sample-ns)))))))

    (it "suppresses clojure.test reporter output"
      (let [output-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-ct-quiet-test")
            test-file (io/file output-dir "sample_test.clj")]
        (io/make-parents test-file)
        (spit test-file
              (str "(ns gherclj-ct-sample-ns\n"
                   "  (:require [clojure.test :refer :all]))\n"
                   "(deftest sample-pass (is (= 1 1)))"))
        (try
          (with-redefs [gherclj.lifecycle/run-before-all-hooks! (fn [])
                        gherclj.lifecycle/run-after-all-hooks!  (fn [])]
            (let [output (with-out-str
                           (fw/run-specs {:framework :clojure.test
                                           :output-dir output-dir}))]
              (should= "" output)))
          (finally
            (doseq [f (reverse (file-seq (io/file output-dir)))] (.delete f))
            (when (find-ns 'gherclj-ct-sample-ns)
              (remove-ns 'gherclj-ct-sample-ns)))))))
