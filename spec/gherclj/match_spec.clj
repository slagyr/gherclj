(ns gherclj.match-spec
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [gherclj.match :as match]
            [speclj.core :refer :all]))

(def sample-steps
  [{:type :given
    :template "a user {name:string}"
    :regex #"^a user (.+)$"
    :helper-ref 'app-steps/create-adventurer
    :ns 'gherclj.sample.app-steps
    :file "src/gherclj/sample/app_steps.clj"
    :line 10
    :doc nil
    :bindings [{:name "name" :type "string" :coerce identity}]}
   {:type :when
    :template "the user logs in"
    :regex #"^the user logs in$"
    :helper-ref 'app-steps/perform-login
    :ns 'gherclj.sample.app-steps
    :file "src/gherclj/sample/app_steps.clj"
    :line 14
    :doc nil
    :bindings []}])

(describe "Match"

  (context "usage"

    (it "describes the match subcommand and its options"
      (let [text (match/usage-message)]
        (should (str/includes? text "gherclj match"))
        (should (str/includes? text "--step-namespaces"))
        (should (str/includes? text "--json"))
        (should (str/includes? text "--edn"))
        (should (str/includes? text "--no-color")))))

  (context "parse-phrase"

    (it "strips a leading Given keyword"
      (should= "a user \"alice\""
               (match/parse-phrase "Given a user \"alice\"")))

    (it "strips a leading And keyword"
      (should= "the user logs in"
               (match/parse-phrase "And the user logs in")))

    (it "leaves a bare phrase unchanged"
      (should= "the user logs in"
               (match/parse-phrase "the user logs in"))))

  (context "analyze"

    (it "matches a single registered step regardless of feature-side keyword"
      (let [report (match/analyze sample-steps "Given the user logs in")]
        (should= :matched (:match-status report))
        (should= "the user logs in" (:phrase report))
        (should= 1 (count (:matches report)))
        (should= :when (:type (first (:matches report))))))

    (it "reports no-match when no registered step matches"
      (let [report (match/analyze sample-steps "When the dragon arrives")]
        (should= :no-match (:match-status report))))

    (it "reports ambiguous when more than one stepdef matches the phrase"
      (let [steps [{:name "login-a" :type :given :template "the user logs in" :regex #"^the user logs in$" :ns 'test :file "x" :line 1 :doc nil :bindings []}
                   {:name "login-b" :type :when :template "the user logs in" :regex #"^the user logs in$" :ns 'test :file "x" :line 2 :doc nil :bindings []}]
            report (match/analyze steps "the user logs in")]
        (should= :ambiguous (:match-status report))
        (should= 2 (count (:matches report))))))

  (context "build-data"

    (it "builds a matched report with bound values"
      (let [report (match/build-data {:phrase "a user \"alice\""
                                      :match-status :matched
                                      :matches [{:type :given
                                                 :phrase "a user {name:string}"
                                                 :regex false
                                                 :helper-ref "app-steps/create-adventurer"
                                                 :ns 'gherclj.sample.app-steps
                                                 :file "app_steps.clj"
                                                 :line 10
                                                 :doc nil
                                                 :bindings [{:name "name" :type "string" :value "alice"}]}]})]
        (should= "match" (:command report))
        (should= :matched (:match-status report))
        (should= "alice" (get-in report [:matches 0 :bindings 0 :value])))))

  (context "renderers"

    (it "renders pretty json output"
      (let [output (match/render-json {:gherclj-version "1.0.0"
                                       :command "match"
                                       :phrase "a user \"alice\""
                                       :match-status :matched
                                       :matches [{:type :given :phrase "a user {name:string}" :regex false :helper-ref "foo" :ns 'foo :file "app_steps.clj" :line 10 :doc nil :bindings [{:name "name" :type "string" :value "alice"}]}]})
            parsed (json/parse-string output keyword)]
        (should (str/includes? output "\n"))
        (should= "match" (:command parsed))
        (should= "matched" (:match-status parsed))
        (should= "alice" (get-in parsed [:matches 0 :bindings 0 :value]))))

    (it "renders pretty edn output"
      (let [output (match/render-edn {:gherclj-version "1.0.0"
                                      :command "match"
                                      :phrase "a user \"alice\""
                                      :match-status :matched
                                      :matches []})
            parsed (read-string output)]
        (should (str/includes? output "\n"))
        (should= :matched (:match-status parsed))))))
