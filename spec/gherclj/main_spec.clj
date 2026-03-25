(ns gherclj.main-spec
  (:require [speclj.core :refer :all]
            [clojure.string :as str]
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
      (let [result (main/parse-args ["-t" "speclj" "-v"])]
        (should= :speclj (get-in result [:options :test-framework]))
        (should= true (get-in result [:options :verbose]))))

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
        (should-be-nil (get-in result [:options :features-dir])))))

  (context "usage"

    (it "contains expected content"
      (let [text (main/usage-message)]
        (should (str/includes? text "Usage: gherclj [options]"))
        (should (str/includes? text "--features-dir"))
        (should (str/includes? text "--help")))))

  (context "run"

    (it "returns 0 and prints usage for --help"
      (let [output (with-out-str
                     (should= 0 (main/run ["--help"])))]
        (should (str/includes? output "Usage: gherclj [options]"))))

    (it "returns 0 for -h"
      (with-out-str
        (should= 0 (main/run ["-h"]))))

    (it "returns 1 and prints message for unknown flags"
      (let [output (with-out-str
                     (should= 1 (main/run ["--turbo-mode"])))]
        (should (str/includes? output "Unknown option"))
        (should (str/includes? output "turbo-mode"))))))
