(ns gherclj.generator-spec
  (:require [speclj.core :refer :all]
   [gherclj.generator :as gen]
   [gherclj.core :as core]
   [gherclj.core :refer [defgiven defwhen defthen helper!]]
   [clojure.string :as str]
   [gherclj.frameworks.clojure.speclj]
   [gherclj.sample.app-steps]))

(helper! gherclj.generator-spec)

;; Sample helpers and step routing

(defn setup-project [_slug _timeout] :setup)
(defn run-action [] :action)
(defn check-result [_expected] :check)
(defn setup-table [_table] :table-setup)

(defgiven "a project {slug:string} with timeout {timeout:int}" generator-spec/setup-project)
(defwhen  "running the action"                                 generator-spec/run-action)
(defthen  "the result should be {expected:string}"             generator-spec/check-result)
(defgiven "a table of projects:"                               generator-spec/setup-table)

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

  (context "call-step-renderer"

    (it "produces a form invoking the helper-ref with matched args"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            classified (core/classify-step steps "a project \"alpha\" with timeout 300")]
        (should= '(generator-spec/setup-project "alpha" 300)
                 (gen/call-step-renderer classified))))

    (it "appends table when present"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            classified (assoc (core/classify-step steps "a table of projects:")
                              :table {:headers ["name"] :rows [["alpha"]]})]
        (should= '(generator-spec/setup-table {:headers ["name"] :rows [["alpha"]]})
                 (gen/call-step-renderer classified))))

    (it "appends doc-string when present"
      (let [steps (core/collect-steps ['gherclj.generator-spec])
            classified (assoc (core/classify-step steps "the result should be \"ok\"")
                              :doc-string "some content")]
        (should= '(generator-spec/check-result "ok" "some content")
                 (gen/call-step-renderer classified)))))

  (context "generate-spec"

    (it "generates a complete spec string for a feature IR"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :framework :clojure/speclj}
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
        (should (str/includes? result "(g/reset!)"))))

    (it "returns nil when tag filtering removes every scenario"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :exclude-tags ["slow"]
                    :framework :clojure/speclj}
            ir {:feature "Sample feature"
                :source "sample.feature"
                :scenarios [{:scenario "Slow only"
                             :tags ["slow"]
                             :steps [{:type :given :text "a project alpha with timeout 300"}]}]}]
        (should-be-nil (gen/generate-spec config ir))))

    (it "includes wip-tagged scenarios when no tag filters are configured"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :framework :clojure/speclj}
            ir {:feature "Sample feature"
                :source "sample.feature"
                :scenarios [{:scenario "Ready"
                             :steps [{:type :given :text "a project alpha with timeout 300"}]}
                            {:scenario "Not ready"
                             :tags ["wip"]
                             :steps [{:type :given :text "a project alpha with timeout 300"}]}]}
            result (gen/generate-spec config ir)]
        (should (str/includes? result "(it \"Ready\""))
        (should (str/includes? result "(it \"Not ready\""))))

    (it "treats wip like any other tag when included"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :include-tags ["wip"]
                    :framework :clojure/speclj}
            ir {:feature "Sample feature"
                :source "sample.feature"
                :scenarios [{:scenario "Ready"
                             :steps [{:type :given :text "a project alpha with timeout 300"}]}
                            {:scenario "Not ready"
                             :tags ["wip"]
                             :steps [{:type :given :text "a project alpha with timeout 300"}]}]}
            result (gen/generate-spec config ir)]
        (should-not (str/includes? result "(it \"Ready\""))
        (should (str/includes? result "(it \"Not ready\""))))

    (it "includes helper imports referenced only by background steps"
      (let [config {:step-namespaces ['gherclj.generator-spec 'gherclj.sample.app-steps]
                    :framework :clojure/speclj}
            ir {:feature "Sample feature"
                :source "sample.feature"
                :background {:steps [{:type :given :text "a user \"alice\""}]}
                :scenarios [{:scenario "Does the thing"
                             :steps [{:type :when :text "running the action"}
                                     {:type :then :text "the result should be ok"}]}]}
             result (gen/generate-spec config ir)]
        (should (str/includes? result "[gherclj.sample.app-steps :as app-steps]"))
        (should (str/includes? result "(app-steps/create-adventurer \"alice\")")))))

  (context "generate-spec with tables and doc-strings"

    (it "generates spec with table step"
      (let [config {:step-namespaces ['gherclj.generator-spec]
                    :framework :clojure/speclj}
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
                    :framework :clojure/speclj}
            ir {:feature "Docs"
                :source "docs.feature"
                :scenarios [{:scenario "With doc"
                             :steps [{:type :then :text "the result should be ok"
                                      :doc-string "some content"}]}]}
            result (gen/generate-spec config ir)]
        (should (str/includes? result "check-result"))
        (should (str/includes? result "some content"))))))
