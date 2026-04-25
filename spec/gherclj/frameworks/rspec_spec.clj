(ns gherclj.frameworks.ruby.rspec-spec
  (:require [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.frameworks.ruby.rspec :as rspec]
            [speclj.core :refer :all]))

(rspec/file-setup! "require File.expand_path('lib/space_airlock', Dir.pwd)")
(rspec/describe-setup! "subject { SpaceAirlock.new }")

(describe "RSpec framework"

  (context "generate-preamble"

    (it "generates Ruby preamble from step namespace registrations"
      (let [result (fw/generate-preamble {:framework :ruby/rspec}
                                         "features/auth.feature"
                                         ['gherclj.frameworks.ruby.rspec-spec])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "require File.expand_path('lib/space_airlock', Dir.pwd)"))
        (should (str/includes? result "RSpec.describe 'Auth' do"))
        (should (str/includes? result "subject { SpaceAirlock.new }"))))

    (it "omits setup block when step namespace has none"
      (let [result (fw/generate-preamble {:framework :ruby/rspec}
                                         "features/auth.feature"
                                         [])]
        (should (str/includes? result "require 'rspec'"))
        (should (str/includes? result "RSpec.describe 'Auth' do"))
        (should-not (str/includes? result "subject")))))

  (context "render-step"

    (it "renders a step as a bare snake_case method call"
      (let [step {:name "create-adventurer" :args ["alice"] :ns 'myapp.steps}]
        (should= "create_adventurer('alice')" (fw/render-step {:framework :ruby/rspec} step))))

    (it "renders a no-arg step without parentheses"
      (let [step {:name "enter-the-realm" :args [] :ns 'myapp.steps}]
        (should= "enter_the_realm" (fw/render-step {:framework :ruby/rspec} step))))

    (it "renders integer args as Ruby literals"
      (let [step {:name "verify-outcome" :args [200] :ns 'myapp.steps}]
        (should= "verify_outcome(200)" (fw/render-step {:framework :ruby/rspec} step)))))

  (context "wrap-scenario"

    (it "wraps pre-rendered Ruby lines in an it block"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["seed_user('alice')"
                                       "log_in"]}
            result (fw/wrap-scenario {:framework :ruby/rspec} scenario nil)]
        (should (str/includes? result "it 'User can log in' do"))
        (should (str/includes? result "seed_user('alice')"))
        (should (str/includes? result "log_in"))))

    (it "includes background steps before scenario steps"
      (let [background {:rendered-steps ["clean_db"]}
            scenario   {:scenario "User can log in"
                        :rendered-steps ["log_in"]}
            result (fw/wrap-scenario {:framework :ruby/rspec} scenario background)]
        (should (str/includes? result "clean_db"))
        (should (str/includes? result "log_in")))))

  (context "run-specs"

    (it "executes bundle exec rspec with tty against the output directory"
      (let [captured (atom nil)]
        (with-redefs [clojure.java.shell/sh (fn [& args]
                                              (reset! captured args)
                                              {:exit 0 :out "" :err ""})]
          (fw/run-specs {:framework :ruby/rspec :output-dir "tmp/generated"}))
        (should= ["bundle" "exec" "rspec" "--tty" "tmp/generated"] @captured)))

    (it "prints rspec stdout and stderr"
      (let [stdout (with-out-str
                     (with-redefs [clojure.java.shell/sh (fn [& _]
                                                           {:exit 0 :out "..\nFinished\n" :err "warnings\n"})]
                       (binding [*err* *out*]
                         (fw/run-specs {:framework :ruby/rspec :output-dir "tmp/generated"}))))]
        (should (str/includes? stdout "..\nFinished\n"))
        (should (str/includes? stdout "warnings\n"))))))
