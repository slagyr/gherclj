(ns gherclj.generator-spec
  (:require [speclj.core :refer :all]
            [gherclj.generator :as gen]
            [gherclj.core :as core]
            [gherclj.core :refer [defgiven defwhen defthen]]
            [clojure.string :as str]
            [gherclj.frameworks.speclj]))

;; Sample steps for generation tests

(defgiven setup-project "a project {slug:string} with timeout {timeout:int}"
  [slug timeout]
  :setup)

(defwhen run-action "running the action"
  []
  :action)

(defthen check-result "the result should be {expected:string}"
  [expected]
  :check)

(defgiven setup-table "a table of projects:"
  [table]
  :table-setup)

(describe "Generator"

  (context "classify-scenario"

    (it "classifies all steps in a scenario"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            scenario {:scenario "Test scenario"
                      :steps [{:type :given :text "a project \"alpha\" with timeout 300"}
                              {:type :when :text "running the action"}
                              {:type :then :text "the result should be \"ok\""}]}
            result (gen/classify-scenario steps scenario)]
        (should= 3 (count (:steps result)))
        (should (every? :classified? (:steps result)))
        (should= ["alpha" 300] (:args (first (:steps result))))
        (should= [] (:args (second (:steps result))))
        (should= ["ok"] (:args (nth (:steps result) 2))))))

  (context "generate-step-call"

    (it "generates an aliased function call with args"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            classified (core/classify-step steps "a project \"alpha\" with timeout 300")
            code (gen/generate-step-call classified)]
        (should= "(generator-spec/setup-project \"alpha\" 300)" code)))

    (it "generates an aliased no-arg function call"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            classified (core/classify-step steps "running the action")
            code (gen/generate-step-call classified)]
        (should= "(generator-spec/run-action)" code))))

  (context "generate-spec"

    (it "generates a complete spec string for a feature IR"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :test-framework :speclj}
            ir {:feature "Sample feature"
                :source "sample.feature"
                :scenarios [{:scenario "Does the thing"
                             :steps [{:type :given :text "a project alpha with timeout 300"}
                                     {:type :when :text "running the action"}
                                     {:type :then :text "the result should be ok"}]}]}
            result (gen/generate-spec config ir)]
        (should (str/includes? result "(describe \"Sample feature\""))
        (should (str/includes? result "(it \"Does the thing\""))
        (should (str/includes? result "(generator-spec/setup-project \"alpha\" 300)"))
        (should (str/includes? result "(generator-spec/run-action)"))
        (should (str/includes? result "(generator-spec/check-result \"ok\")"))
        (should (str/includes? result "[gherclj.generator-spec :as generator-spec]"))
        (should (str/includes? result "[gherclj.core :as g]"))
        (should (str/includes? result "(g/reset!)")))))

  (context "generate-step-call with string args"

    (it "properly escapes string args containing quotes"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            classified (core/classify-step steps "a project alpha with timeout 300")
            code (gen/generate-step-call classified)]
        (should= "(generator-spec/setup-project \"alpha\" 300)" code))))

  (context "generate-spec with tables and doc-strings"

    (it "generates spec with table step"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :test-framework :speclj}
            ir {:feature "Tables"
                :source "tables.feature"
                :scenarios [{:scenario "With table"
                             :steps [{:type :given :text "a table of projects:"
                                      :table {:headers ["name"] :rows [["alpha"]]}}]}]}
            result (gen/generate-spec config ir)]
        (should (str/includes? result "setup-table"))
        (should (str/includes? result ":headers"))))

    (it "generates spec with doc-string step"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :test-framework :speclj}
            ir {:feature "Docs"
                :source "docs.feature"
                :scenarios [{:scenario "With doc"
                             :steps [{:type :then :text "the result should be ok"
                                      :doc-string "some content"}]}]}
            result (gen/generate-spec config ir)]
        (should (str/includes? result "check-result"))
        (should (str/includes? result "some content"))))))
