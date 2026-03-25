(ns gherclj.main-spec
  (:require [speclj.core :refer :all]
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
        (should (seq (:errors result))))))

  (context "usage"

    (it "contains expected content"
      (let [text (main/usage-message)]
        (should (clojure.string/includes? text "Usage: gherclj [options]"))
        (should (clojure.string/includes? text "--features-dir"))
        (should (clojure.string/includes? text "--help")))))

  (context "-main"

    (it "prints usage for --help"
      (let [output (with-out-str (main/-main "--help"))]
        (should (clojure.string/includes? output "Usage: gherclj [options]"))))

    (it "prints usage for -h"
      (let [output (with-out-str (main/-main "-h"))]
        (should (clojure.string/includes? output "Usage: gherclj [options]"))))))
