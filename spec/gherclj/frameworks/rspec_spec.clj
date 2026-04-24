(ns gherclj.frameworks.rspec-spec
  (:require [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.frameworks.rspec]
            [gherclj.generator :as gen]
            [speclj.core :refer :all]))

(describe "RSpec framework"

  (context "generate-preamble"

    (it "generates a Ruby require prelude"
      (let [result (fw/generate-preamble {:framework :rspec}
                                         "features/auth.feature"
                                         ['myapp.steps.auth])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "require File.expand_path('spec/support/gherclj_world', Dir.pwd)"))
        (should (str/includes? result "RSpec.describe 'Auth' do")))))

  (context "wrap-scenario"

    (it "generates Ruby world method calls"
      (let [scenario {:scenario "User can log in"
                      :steps [{:type :given :text "a user exists" :classified? true
                               :ns 'myapp.steps :name "create-user" :args ["alice"]}
                              {:type :when :text "they log in" :classified? true
                               :ns 'myapp.steps :name "log-in" :args []}]}
            result (fw/wrap-scenario {:framework :rspec} scenario nil)]
        (should (str/includes? result "it 'User can log in' do"))
        (should (str/includes? result "world.create_user('alice')"))
        (should (str/includes? result "world.log_in")))))

    (it "includes background steps before scenario steps"
      (let [background {:steps [{:type :given :text "db is clean" :classified? true
                                 :ns 'myapp.steps :name "clean-db" :args []}]}
            scenario {:scenario "User can log in"
                      :steps [{:type :when :text "they log in" :classified? true
                               :ns 'myapp.steps :name "log-in" :args []}]}
            result (fw/wrap-scenario {:framework :rspec} scenario background)]
        (should (str/includes? result "world.clean_db"))
        (should (str/includes? result "world.log_in")))))

  (context "run-specs"

    (it "executes bundle exec rspec against the output directory"
      (let [captured (atom nil)]
        (with-redefs [clojure.java.shell/sh (fn [& args]
                                             (reset! captured args)
                                             {:exit 0 :out "" :err ""})]
          (fw/run-specs {:framework :rspec
                          :output-dir "tmp/generated"}))
        (should= ["bundle" "exec" "rspec" "tmp/generated"] @captured))))
