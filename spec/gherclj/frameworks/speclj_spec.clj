(ns gherclj.frameworks.speclj-spec
  (:require [gherclj.frameworks.speclj :as speclj-fw]
            [gherclj.core :as g]
            [gherclj.generator :as gen]
            [gherclj.lifecycle :as lifecycle]
            [clojure.string :as str]
            [speclj.core :refer :all]))

(describe "Speclj framework"

  (context "run-args"
    (it "appends framework options to the default generated spec args"
      (should= ["-c" "tmp/generated" "-s" "src" "-f" "documentation" "-c" "-P"]
               (speclj-fw/run-args {:output-dir     "tmp/generated"
                                    :framework-opts ["-f" "documentation" "-c" "-P"]})))

    (it "uses default output-dir and empty opts when none provided"
      (should= ["-c" "target/gherclj/generated" "-s" "src"]
               (speclj-fw/run-args {}))))

  (context "generate-ns-form"

    (it "generates a namespace form with required imports and step requires"
      (let [result (gen/generate-ns-form {:test-framework :speclj}
                                         "features/auth.feature"
                                         ['myapp.steps.auth 'myapp.steps.cart])]
        (should (str/includes? result "speclj.core :refer :all"))
        (should (str/includes? result "gherclj.core :as g"))
        (should (str/includes? result "myapp.steps.auth"))
        (should (str/includes? result "myapp.steps.cart"))))

    (it "generates a namespace form with no step requires"
      (let [result (gen/generate-ns-form {:test-framework :speclj}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "speclj.core :refer :all"))
        (should-not (str/includes? result "myapp")))))

  (context "wrap-feature"

    (it "wraps scenario blocks in a describe with lifecycle hooks"
      (let [result (gen/wrap-feature {:test-framework :speclj}
                                     "Authentication"
                                     "  (it \"test\")")]
        (should (str/includes? result "(describe \"Authentication\""))
        (should (str/includes? result "before-all"))
        (should (str/includes? result "before"))
        (should (str/includes? result "after"))
        (should (str/includes? result "after-all"))
        (should (str/includes? result "(it \"test\")")))))

  (context "wrap-scenario"

    (it "wraps steps in an it block with the scenario title"
      (let [scenario {:scenario "User can log in"
                      :steps [{:type :given :text "a user exists" :classified? true
                               :ns 'myapp.steps :name "setup-user" :args []}
                              {:type :when :text "they log in" :classified? true
                               :ns 'myapp.steps :name "log-in" :args []}]}
            result (gen/wrap-scenario {:test-framework :speclj} scenario nil)]
        (should (str/includes? result "(it \"User can log in\""))
        (should (str/includes? result "steps/setup-user"))
        (should (str/includes? result "steps/log-in"))))

    (it "includes background steps before scenario steps"
      (let [background {:steps [{:type :given :text "db is clean" :classified? true
                                 :ns 'myapp.steps :name "clean-db" :args []}]}
            scenario {:scenario "User can log in"
                      :steps [{:type :when :text "they log in" :classified? true
                               :ns 'myapp.steps :name "log-in" :args []}]}
            result (gen/wrap-scenario {:test-framework :speclj} scenario background)]
        (should (str/includes? result "steps/clean-db"))
        (should (str/includes? result "steps/log-in")))))

  (context "wrap-pending"

    (it "generates a pending it block with step comments"
      (let [scenario {:scenario "Unimplemented feature"
                      :steps [{:type :given :text "something exists"}
                              {:type :when :text "action happens"}]}
            result (gen/wrap-pending {:test-framework :speclj} scenario nil)]
        (should (str/includes? result "(it \"Unimplemented feature\""))
        (should (str/includes? result "pending"))
        (should (str/includes? result ";; given something exists"))
        (should (str/includes? result ";; when action happens"))))

    (it "includes background step comments when background is provided"
      (let [background {:steps [{:type :given :text "db is clean"}]}
            scenario {:scenario "Unimplemented feature"
                      :steps [{:type :when :text "action happens"}]}
            result (gen/wrap-pending {:test-framework :speclj} scenario background)]
        (should (str/includes? result ";; given db is clean"))
        (should (str/includes? result ";; when action happens")))))

  (context "assertion methods"

    (around [it]
      (g/set-test-framework! :speclj)
      (it)
      (g/set-test-framework! nil))

    (it "should= passes when values are equal"
      (g/should= 42 42))

    (it "should passes for truthy value"
      (g/should true))

    (it "should-not passes for falsy value"
      (g/should-not false))

    (it "should-be-nil passes for nil"
      (g/should-be-nil nil))

    (it "should-not-be-nil passes for non-nil"
      (g/should-not-be-nil :something)))

  (context "run-specs"

    (it "appends framework options to the default generated spec args"
      (let [captured (atom nil)]
        (with-redefs [speclj.cli/run                     (fn [& args]
                                                           (reset! captured args)
                                                           0)
                      gherclj.lifecycle/run-before-all-hooks! (fn [])
                      gherclj.lifecycle/run-after-all-hooks!  (fn [])]
          (gen/run-specs {:test-framework :speclj
                          :output-dir     "tmp/generated"
                          :framework-opts ["-f" "documentation" "-c" "-P"]}))

        (should= ["-c" "tmp/generated" "-s" "src" "-f" "documentation" "-c" "-P"]
                 @captured)))

    (it "still runs after-all hooks when speclj/run throws"
      (let [after-called (atom false)]
        (with-redefs [speclj.cli/run                     (fn [& _] (throw (RuntimeException. "runner error")))
                      gherclj.lifecycle/run-before-all-hooks! (fn [])
                      gherclj.lifecycle/run-after-all-hooks!  (fn [] (reset! after-called true))]
          (should-throw RuntimeException
                        (gen/run-specs {:test-framework :speclj
                                        :output-dir     "tmp/generated"})))
        (should @after-called)))))
