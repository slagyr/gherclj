(ns gherclj.main-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
            [gherclj.config :as config]
            [gherclj.generator :as gen]
            [gherclj.pipeline :as pipeline]
            [gherclj.main :as main]))

(describe "Main"

  (context "parse-args"

    (it "parses --help flag"
      (let [result (main/parse-args ["--help"])]
        (should (:help result))))

    (it "parses -h flag"
      (let [result (main/parse-args ["-h"])]
        (should (:help result))))

    (it "parses --verbose flag"
      (let [result (main/parse-args ["--verbose"])]
        (should= true (get-in result [:options :verbose]))))

    (it "parses --test-framework"
      (let [result (main/parse-args ["--test-framework" "clojure.test"])]
        (should= :clojure.test (get-in result [:options :test-framework]))))

    (it "parses short flags"
      (let [result (main/parse-args ["-T" "speclj" "-v"])]
        (should= :speclj (get-in result [:options :test-framework]))
        (should= true (get-in result [:options :verbose]))))

    (it "parses -t as tag include"
      (let [result (main/parse-args ["-t" "smoke"])]
        (should= ["smoke"] (get-in result [:options :include-tags]))))

    (it "parses -t ~prefix as tag exclude"
      (let [result (main/parse-args ["-t" "~slow"])]
        (should= ["slow"] (get-in result [:options :exclude-tags]))))

    (it "parses multiple -t flags"
      (let [result (main/parse-args ["-t" "smoke" "-t" "~slow" "-t" "~wip"])]
        (should= ["smoke"] (get-in result [:options :include-tags]))
        (should= ["slow" "wip"] (get-in result [:options :exclude-tags]))))

    (it "parses a single file:line selector"
      (let [result (main/parse-args ["features/adventure/dragon_cave.feature:42"])]
        (should= [{:source "features/adventure/dragon_cave.feature"
                   :line 42}]
                 (get-in result [:options :locations]))))

    (it "parses multiple file:line selectors"
      (let [result (main/parse-args ["features/adventure/dragon_cave.feature:42"
                                     "features/adventure/moon_castle.feature:73"])]
        (should= [{:source "features/adventure/dragon_cave.feature" :line 42}
                  {:source "features/adventure/moon_castle.feature" :line 73}]
                 (get-in result [:options :locations]))))

    (it "parses a bare .feature path as a location with no :line"
      (let [result (main/parse-args ["features/adventure/dragon_cave.feature"])]
        (should= [{:source "features/adventure/dragon_cave.feature"}]
                 (get-in result [:options :locations]))))

    (it "parses mixed bare and file:line selectors"
      (let [result (main/parse-args ["features/adventure/dragon_cave.feature"
                                     "features/adventure/moon_castle.feature:73"])]
        (should= [{:source "features/adventure/dragon_cave.feature"}
                  {:source "features/adventure/moon_castle.feature" :line 73}]
                 (get-in result [:options :locations]))))

    (it "leaves non-.feature positional args as framework-opts"
      (let [result (main/parse-args ["--" "notes.txt" "plain-arg"])]
        (should-be-nil (get-in result [:options :locations]))
        (should= ["notes.txt" "plain-arg"]
                 (get-in result [:options :framework-opts]))))

    (it "accumulates step-namespaces"
      (let [result (main/parse-args ["-s" "myapp.steps.auth" "-s" "myapp.steps.cart"])]
        (should= ['myapp.steps.auth 'myapp.steps.cart]
                 (get-in result [:options :step-namespaces]))))

    (it "reports unknown flags"
      (let [result (main/parse-args ["--turbo-mode"])]
        (should (seq (:errors result)))))

    (it "leaves nil for unspecified options"
      (let [result (main/parse-args ["-v"])]
        (should-be-nil (get-in result [:options :step-namespaces]))
        (should-be-nil (get-in result [:options :features-dir]))))

    (it "captures positional args as :framework-opts"
      (let [result (main/parse-args ["--" "-f" "documentation"])]
        (should= ["-f" "documentation"] (get-in result [:options :framework-opts])))))

  (context "usage"

    (it "contains expected content"
      (let [text (main/usage-message)]
        (should (str/includes? text "Gherclj"))
        (should (str/includes? text "Gherkin -> test code transducer"))
        (should (str/includes? text "Copyright"))
        (should (str/includes? text "--features-dir"))
        (should (str/includes? text "--help")))))

  (context "run"

    (it "returns 0 and prints usage for --help"
      (let [output (with-out-str
                     (should= 0 (main/run ["--help"])))]
        (should (str/includes? output "Gherclj"))))

    (it "returns 0 for -h"
      (with-out-str
        (should= 0 (main/run ["-h"]))))

    (it "returns 1 and prints message for unknown flags"
      (let [output (with-out-str
                     (should= 1 (main/run ["--turbo-mode"])))]
        (should (str/includes? output "Unknown option"))
        (should (str/includes? output "turbo-mode"))))

    (context "pipeline execution"
      (around [it]
        (with-redefs [config/load-config (fn [] {})
                      pipeline/run!      (fn [_] nil)]
          (it)))

      (it "returns 0 when run-specs returns zero (number)"
        (with-redefs [gen/run-specs (fn [_] 0)]
          (should= 0 (main/run []))))

      (it "returns 1 when run-specs returns positive number"
        (with-redefs [gen/run-specs (fn [_] 3)]
          (should= 1 (main/run []))))

      (it "returns 0 when run-specs returns map with no failures"
        (with-redefs [gen/run-specs (fn [_] {:fail 0 :error 0})]
          (should= 0 (main/run []))))

      (it "returns 1 when run-specs returns map with :fail > 0"
        (with-redefs [gen/run-specs (fn [_] {:fail 2 :error 0})]
          (should= 1 (main/run []))))

      (it "returns 1 when run-specs returns map with :error > 0"
        (with-redefs [gen/run-specs (fn [_] {:fail 0 :error 1})]
          (should= 1 (main/run []))))

      (it "returns 0 when run-specs returns non-number non-map (else branch)"
        (with-redefs [gen/run-specs (fn [_] nil)]
          (should= 0 (main/run [])))))))
