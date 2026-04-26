(ns gherclj.ambiguity-spec
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [gherclj.ambiguity :as ambiguity]
            [speclj.core :refer :all]))

(describe "Ambiguity"

  (context "usage"

    (it "describes the ambiguity subcommand and its options"
      (let [text (ambiguity/usage-message)]
        (should (str/includes? text "gherclj ambiguity"))
        (should (str/includes? text "--features-dir"))
        (should (str/includes? text "--step-namespaces"))
        (should (str/includes? text "--tag"))
        (should (str/includes? text "--json"))
        (should (str/includes? text "--edn"))
        (should (str/includes? text "--color"))
        (should (str/includes? text "--no-color")))))

  (context "build-data"

    (it "builds a structured ambiguity report"
      (let [report (ambiguity/build-data {:scanned-scenarios 1
                                          :unscanned-scenarios 1
                                          :filters ["~slow"]
                                          :ambiguities [{:phrase "a user \"alice\""
                                                         :feature-file "ambiguous.feature"
                                                         :line 3
                                                         :matches [{:type :given
                                                                    :template "a user {name:string}"
                                                                    :helper-ref "ambiguous-steps/user-by-name"
                                                                    :ns 'gherclj.sample.ambiguous-steps
                                                                    :file "src/gherclj/sample/ambiguous_steps.clj"
                                                                    :line 10
                                                                    :doc nil
                                                                    :bindings [{:name "name" :type "string"}]}]}]})]
        (should= "ambiguity" (:command report))
        (should= 1 (:scenarios-scanned report))
        (should= {:include [] :exclude ["slow"]} (:tags-applied report))
        (should= 1 (:ambiguous-count report))
        (should= "a user \"alice\"" (get-in report [:ambiguities 0 :phrase])))))

  (context "renderers"

    (it "renders pretty json output"
      (let [output (ambiguity/render-json {:gherclj-version "1.0.0"
                                           :command "ambiguity"
                                           :scenarios-scanned 1
                                           :tags-applied {:include [] :exclude []}
                                           :ambiguous-count 1
                                           :ambiguities [{:phrase "a user \"alice\""
                                                          :feature-file "ambiguous.feature"
                                                          :line 3
                                                          :matches [{:type :given :phrase "a user {name:string}" :regex false :helper-ref "foo" :ns 'foo :file "f" :line 1 :doc nil :bindings []}]}]})
            parsed (json/parse-string output keyword)]
        (should (str/includes? output "\n"))
        (should= "ambiguity" (:command parsed))
        (should= "given" (get-in parsed [:ambiguities 0 :matches 0 :type]))))

    (it "renders pretty edn output"
      (let [output (ambiguity/render-edn {:gherclj-version "1.0.0"
                                          :command "ambiguity"
                                          :scenarios-scanned 1
                                          :tags-applied {:include [] :exclude []}
                                          :ambiguous-count 0
                                          :ambiguities []})
            parsed (read-string output)]
        (should (str/includes? output "\n"))
        (should= "ambiguity" (:command parsed))
        (should= [] (:ambiguities parsed))))))
