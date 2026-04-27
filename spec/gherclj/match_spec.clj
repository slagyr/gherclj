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
   {:type :given
    :template "the user logs in"
    :regex #"^the user logs in$"
    :helper-ref 'same-phrase-steps/login-state
    :ns 'gherclj.sample.same-phrase-steps
    :file "src/gherclj/sample/same_phrase_steps.clj"
    :line 10
    :doc nil
    :bindings []}
   {:type :when
    :template "the user logs in"
    :regex #"^the user logs in$"
    :helper-ref 'same-phrase-steps/perform-login
    :ns 'gherclj.sample.same-phrase-steps
    :file "src/gherclj/sample/same_phrase_steps.clj"
    :line 11
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
        (should (str/includes? text "--color"))
        (should (str/includes? text "--no-color")))))

  (context "parse-phrase"

    (it "parses a leading Given as typed request"
      (should= {:phrase "a user \"alice\"" :requested-type :given}
               (match/parse-phrase "Given a user \"alice\"")))

    (it "treats And like any-type matching"
      (should= {:phrase "the user logs in" :requested-type :any}
               (match/parse-phrase "And the user logs in")))

    (it "treats a bare phrase as any-type matching"
      (should= {:phrase "the user logs in" :requested-type :any}
               (match/parse-phrase "the user logs in"))))

  (context "build-data"

    (it "builds a matched typed report with bound values"
      (let [report (match/build-data {:phrase "a user \"alice\""
                                      :requested-type :given
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
        (should= :given (:requested-type report))
        (should= "alice" (get-in report [:matches 0 :bindings 0 :value]))))

    (it "builds a matched any-type report with one entry per type"
      (let [report (match/analyze sample-steps "the user logs in")]
        (should= :matched (:match-status report))
        (should= :any (:requested-type report))
        (should= [:given :when] (mapv :type (:matches report))))))

  (context "renderers"

    (it "renders pretty json output"
      (let [output (match/render-json {:gherclj-version "1.0.0"
                                       :command "match"
                                       :phrase "a user \"alice\""
                                       :requested-type :given
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
                                      :requested-type :given
                                      :match-status :matched
                                      :matches []})
            parsed (read-string output)]
        (should (str/includes? output "\n"))
        (should= :given (:requested-type parsed))
        (should= :matched (:match-status parsed))))))
