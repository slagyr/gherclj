(ns gherclj.catalog-spec
  (:require [cheshire.core :as json]
            [clojure.string :as str]
             [gherclj.catalog :as catalog]
             [speclj.core :refer :all]))

(describe "Catalog"

  (context "usage"

    (it "describes the steps subcommand and its options"
      (let [text (catalog/usage-message)]
        (should (str/includes? text "gherclj steps"))
        (should (str/includes? text "--given"))
        (should (str/includes? text "--when"))
        (should (str/includes? text "--then"))
        (should (str/includes? text "--json"))
        (should (str/includes? text "--edn"))
        (should (str/includes? text "--color"))
        (should (str/includes? text "--no-color"))
        (should (str/includes? text "--step-namespaces")))))

  (context "render-output"

    (it "groups steps by type and prints source locations"
      (let [output (catalog/render [{:type :when
                                     :template "the user logs in"
                                     :file "/tmp/app_steps.clj"
                                     :line 8}
                                    {:type :given
                                     :template "a user {name:string}"
                                     :file "/tmp/app_steps.clj"
                                     :line 4}
                                    {:type :then
                                     :template "the response should be {status:int}"
                                     :file "/tmp/app_steps.clj"
                                     :line 12}]
                                   {:color? false})]
        (should (str/includes? output "Given:"))
        (should (str/includes? output "When:"))
        (should (str/includes? output "Then:"))
        (should (str/includes? output "a user {name:string}  (app_steps.clj:4)"))
        (should (str/includes? output "the user logs in  (app_steps.clj:8)"))
        (should (str/includes? output "the response should be {status:int}  (app_steps.clj:12)"))))

    (it "prints a docstring on the line after the entry"
      (let [output (catalog/render [{:type :given
                                     :template "a bare step with no doc"
                                     :file "/tmp/step_docstrings.clj"
                                     :line 5}
                                    {:type :given
                                     :template "a documented step"
                                     :doc "Sets :crew atom - does NOT write disk."
                                     :file "/tmp/step_docstrings.clj"
                                     :line 8}]
                                   {:color? false})]
        (should (str/includes? output "a documented step  (step_docstrings.clj:8)\n  Sets :crew atom - does NOT write disk."))
        (should (str/includes? output "a bare step with no doc  (step_docstrings.clj:5)\na documented step  (step_docstrings.clj:8)"))))

    (it "filters steps by keyword against phrase and docstring"
      (let [steps [{:type :given :template "a user {name:string}" :file "/tmp/app_steps.clj" :line 4}
                   {:type :when :template "the user logs in" :file "/tmp/app_steps.clj" :line 8}
                   {:type :given :template "a documented step"
                    :doc "Sets :crew atom - does NOT write disk."
                    :file "/tmp/step_docstrings.clj" :line 11}]]
        (should= ["a user {name:string}" "the user logs in"]
                 (mapv :template (catalog/filter-steps steps {:keyword "user"})))
        (should= ["a documented step"]
                 (mapv :template (catalog/filter-steps steps {:keyword "disk"})))))

    (it "filters steps by selected types additively"
      (let [steps [{:type :given :template "a user {name:string}" :file "/tmp/app_steps.clj" :line 4}
                   {:type :when :template "the user logs in" :file "/tmp/app_steps.clj" :line 8}
                   {:type :then :template "the response should be {status:int}" :file "/tmp/app_steps.clj" :line 12}]]
        (should= ["a user {name:string}"]
                 (mapv :template (catalog/filter-steps steps {:types #{:given}})))
        (should= ["a user {name:string}" "the user logs in"]
                 (mapv :template (catalog/filter-steps steps {:types #{:given :when}})))
        (should= ["a user {name:string}" "the user logs in" "the response should be {status:int}"]
                 (mapv :template (catalog/filter-steps steps {})))))

    (it "colorizes catalog output by default"
      (let [output (catalog/render [{:type :given
                                     :template "a documented step"
                                     :doc "Sets :crew atom - does NOT write disk."
                                     :file "/tmp/app_steps.clj"
                                     :line 4}])]
        (should (re-find #"\u001b\[[0-9;]*m" output))))

    (it "disables color codes when requested"
      (let [output (catalog/render [{:type :given
                                     :template "a documented step"
                                     :doc "Sets :crew atom - does NOT write disk."
                                     :file "/tmp/app_steps.clj"
                                     :line 4}]
                                    {:color? false})]
        (should-not (re-find #"\u001b\[[0-9;]*m" output))))

    (it "builds structured catalog data with sorted step entries"
      (let [data (catalog/build-data [{:type :when
                                       :template "the user logs in"
                                       :helper-ref 'app-steps/enter-the-realm
                                       :ns 'gherclj.sample.app-steps
                                       :file "src/gherclj/sample/app_steps.clj"
                                       :line 11
                                       :bindings []}
                                      {:type :given
                                       :template "a user {name:string}"
                                       :helper-ref 'app-steps/create-adventurer
                                       :ns 'gherclj.sample.app-steps
                                       :file "src/gherclj/sample/app_steps.clj"
                                       :line 10
                                       :bindings [{:name "name" :type "string"}]}]
                                     {})]
        (should= "steps" (:command data))
        (should= [:given :when] (mapv :type (:steps data)))
        (should= [{:name "name" :type "string"}] (:bindings (first (:steps data))))))

    (it "renders pretty json machine output"
      (let [output (catalog/render-json {:gherclj-version "1.0.0"
                                         :command "steps"
                                         :steps [{:type :given
                                                  :phrase "a user {name:string}"
                                                  :regex false
                                                  :helper-ref "app-steps/create-adventurer"
                                                  :ns "gherclj.sample.app-steps"
                                                  :file "src/gherclj/sample/app_steps.clj"
                                                  :line 10
                                                  :doc nil
                                                  :bindings [{:name "name" :type "string"}]}]})
            parsed (json/parse-string output keyword)]
        (should (str/includes? output "\n"))
        (should= "steps" (:command parsed))
        (should= "given" (get-in parsed [:steps 0 :type]))))

    (it "renders pretty edn machine output"
      (let [output (catalog/render-edn {:gherclj-version "1.0.0"
                                        :command "steps"
                                        :steps [{:type :given :phrase "a user {name:string}"}]})
            parsed (read-string output)]
        (should (str/includes? output "\n"))
        (should= "steps" (:command parsed))
        (should= :given (get-in parsed [:steps 0 :type]))))))
