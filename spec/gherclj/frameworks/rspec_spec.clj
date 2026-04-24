(ns gherclj.frameworks.rspec-spec
  (:require [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.frameworks.rspec]
            [gherclj.generator :as gen]
            [speclj.core :refer :all]))

(describe "RSpec framework"

  (context "generate-preamble"

    (it "generates a configurable Ruby require prelude"
      (let [result (fw/generate-preamble {:framework :rspec
                                         :rspec-requires ["lib/space_airlock"]
                                         :rspec-subject "SpaceAirlock.new"}
                                         "features/auth.feature"
                                         ['gherclj.frameworks.rspec-spec])]
        (should (str/includes? result "generated from features/auth.feature"))
        (should (str/includes? result "require File.expand_path('lib/space_airlock', Dir.pwd)"))
        (should (str/includes? result "RSpec.describe 'Auth' do"))
        (should (str/includes? result "subject { SpaceAirlock.new }")))))

  (context "wrap-scenario"

    (it "wraps pre-rendered Ruby lines"
      (let [scenario {:scenario "User can log in"
                      :rendered-steps ["subject.seed_user('alice')"
                                       "subject.log_in"]}
             result (fw/wrap-scenario {:framework :rspec} scenario nil)]
        (should (str/includes? result "it 'User can log in' do"))
        (should (str/includes? result "subject.seed_user('alice')"))
        (should (str/includes? result "subject.log_in")))))

    (it "includes background steps before scenario steps"
      (let [background {:rendered-steps ["subject.clean_db"]}
             scenario {:scenario "User can log in"
                       :rendered-steps ["subject.log_in"]}
             result (fw/wrap-scenario {:framework :rspec} scenario background)]
        (should (str/includes? result "subject.clean_db"))
        (should (str/includes? result "subject.log_in")))))

  (context "run-specs"

    (it "executes bundle exec rspec with tty against the output directory"
      (let [captured (atom nil)]
        (with-redefs [clojure.java.shell/sh (fn [& args]
                                             (reset! captured args)
                                             {:exit 0 :out "" :err ""})]
           (fw/run-specs {:framework :rspec
                           :output-dir "tmp/generated"}))
        (should= ["bundle" "exec" "rspec" "--tty" "tmp/generated"] @captured)))

    (it "prints rspec stdout and stderr"
      (let [stdout (with-out-str
                     (with-redefs [clojure.java.shell/sh (fn [& _]
                                                          {:exit 0 :out "..\nFinished\n" :err "warnings\n"})]
                       (binding [*err* *out*]
                         (fw/run-specs {:framework :rspec
                                        :output-dir "tmp/generated"}))))]
        (should (str/includes? stdout "..\nFinished\n"))
        (should (str/includes? stdout "warnings\n")))))
