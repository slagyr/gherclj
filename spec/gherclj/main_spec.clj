(ns gherclj.main-spec
  (:require [speclj.core :refer :all]
             [clojure.string :as str]
             [gherclj.catalog :as catalog]
             [gherclj.config :as config]
             [gherclj.framework :as fw]
             [gherclj.unused :as unused]
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

    (it "parses --ir-edn flag"
      (let [result (main/parse-args ["--ir-edn"])]
        (should= true (get-in result [:options :ir-edn]))))

    (it "parses --framework"
      (let [result (main/parse-args ["--framework" "clojure/test"])]
        (should= :clojure/test (get-in result [:options :framework]))))

    (it "parses short flags"
      (let [result (main/parse-args ["-F" "clojure/speclj" "-v"])]
        (should= :clojure/speclj (get-in result [:options :framework]))
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
        (should= ["-f" "documentation"] (get-in result [:options :framework-opts]))))

    (it "detects the steps subcommand as the first positional arg"
      (let [result (main/parse-args ["-s" "gherclj.sample.app-steps" "steps"])]
        (should= :steps (get-in result [:options :subcommand]))
        (should-be-nil (get-in result [:options :locations]))))

    (it "captures the remaining positional args for the steps subcommand"
      (let [result (main/parse-args ["steps" "user"])]
        (should= :steps (get-in result [:options :subcommand]))
        (should= ["user"] (get-in result [:options :subcommand-args]))))

    (it "parses type filters for the steps subcommand"
      (let [result (main/parse-args ["steps" "--given" "--when" "user"])]
        (should= :steps (get-in result [:options :subcommand]))
        (should= ["user"] (get-in result [:options :subcommand-args]))
        (should= true (get-in result [:options :given]))
        (should= true (get-in result [:options :when]))
        (should-be-nil (get-in result [:options :then]))))

    (it "parses --no-color for the steps subcommand"
      (let [result (main/parse-args ["steps" "--no-color"])]
        (should= :steps (get-in result [:options :subcommand]))
        (should= true (get-in result [:options :no-color]))))

    (it "detects the unused subcommand as the first positional arg"
      (let [result (main/parse-args ["-s" "gherclj.sample.app-steps" "unused"])]
        (should= :unused (get-in result [:options :subcommand]))
        (should-be-nil (get-in result [:options :locations]))))

    (it "captures the remaining positional args for the unused subcommand"
      (let [result (main/parse-args ["unused" "keyword"])]
        (should= :unused (get-in result [:options :subcommand]))
        (should= ["keyword"] (get-in result [:options :subcommand-args])))))

  (context "usage"

    (it "contains expected content"
      (let [text (main/usage-message)]
        (should (str/includes? text "Gherclj"))
        (should (str/includes? text "Gherkin -> test code transducer"))
        (should (str/includes? text "Copyright"))
        (should (str/includes? text "--features-dir"))
        (should (str/includes? text "--ir-edn"))
        (should (str/includes? text "--help"))))

    (it "includes the current version in the banner"
      (let [text (main/usage-message)]
        (should (str/includes? text (str "Gherclj " main/version)))))

    (it "describes how to pass feature targets"
      (let [text (main/usage-message)]
        (should (str/includes? text "Usage:  gherclj [option]... [feature target]... [-- framework option...]"))
        (should (str/includes? text "feature targets"))
        (should (str/includes? text "[file|file:line]"))
        (should (str/includes? text "file      all scenarios in the file"))
        (should (str/includes? text "file:line the scenario containing that line in the file"))))

    (it "mentions the steps subcommand"
      (let [text (main/usage-message)]
        (should (str/includes? text "gherclj steps"))))

    (it "lists the supported frameworks"
      (let [text (main/usage-message)]
        (should (str/includes? text "clojure/speclj"))
        (should (str/includes? text "clojure/test"))
        (should (str/includes? text "bash/testing"))
        (should (str/includes? text "ruby/rspec"))
        (should (str/includes? text "python/pytest"))
        (should (str/includes? text "javascript/node-test"))
        (should (str/includes? text "rust/rustc-test"))
        (should (str/includes? text "csharp/xunit")))))

  (context "run"

    (it "returns 0 and prints usage for --help"
      (let [output (with-out-str
                     (should= 0 (main/run ["--help"])))]
        (should (str/includes? output "Gherclj"))))

    (it "returns 0 for -h"
      (with-out-str
        (should= 0 (main/run ["-h"]))))

    (it "prints steps usage for steps --help"
      (let [output (with-out-str
                     (should= 0 (main/run ["steps" "--help"])))]
        (should (str/includes? output "gherclj steps"))
        (should (str/includes? output "--given"))))

    (it "prints unused usage for unused --help"
      (let [output (with-out-str
                     (should= 0 (main/run ["unused" "--help"])))]
        (should (str/includes? output "gherclj unused"))
        (should (str/includes? output "--features-dir"))
        (should (str/includes? output "--step-namespaces"))
        (should (str/includes? output "--tag"))))

    (it "returns 1 and prints message for unknown flags"
      (let [output (with-out-str
                     (should= 1 (main/run ["--turbo-mode"])))]
        (should (str/includes? output "Unknown option"))
        (should (str/includes? output "turbo-mode"))))

    (it "dispatches the steps subcommand without running the pipeline"
      (let [catalog-config (atom nil)]
        (with-redefs [config/load-config (fn [] {:step-namespaces ['gherclj.sample.app-steps]})
                      pipeline/run!      (fn [_] (throw (RuntimeException. "pipeline should not run")))
                      catalog/run!       (fn [config args]
                                           (reset! catalog-config [config args]))]
          (with-out-str
            (should= 0 (main/run ["-s" "gherclj.features.steps.step-docstrings" "steps"])))
          (should= [['gherclj.features.steps.step-docstrings] []]
                   ((juxt #(get-in % [0 :step-namespaces]) second) @catalog-config)))))

    (it "dispatches the unused subcommand without running the pipeline"
      (let [unused-config (atom nil)]
        (with-redefs [config/load-config (fn [] {:step-namespaces ['gherclj.sample.app-steps]})
                      pipeline/run!      (fn [_] (throw (RuntimeException. "pipeline should not run")))
                      unused/run!        (fn [config args]
                                           (reset! unused-config [config args]))]
          (with-out-str
            (should= 0 (main/run ["-s" "gherclj.sample.app-steps" "unused" "extra"])))
          (should= [['gherclj.sample.app-steps] ["extra"]]
                   ((juxt #(get-in % [0 :step-namespaces]) second) @unused-config)))))

    (context "pipeline execution"
      (around [it]
        (with-redefs [config/load-config (fn [] {})
                      pipeline/run!      (fn [_] nil)]
          (it)))

      (it "returns 0 when run-specs returns zero (number)"
        (with-redefs [fw/run-specs (fn [_] 0)]
          (should= 0 (main/run []))))

      (it "returns 1 when run-specs returns positive number"
        (with-redefs [fw/run-specs (fn [_] 3)]
          (should= 1 (main/run []))))

      (it "returns 0 when run-specs returns map with no failures"
        (with-redefs [fw/run-specs (fn [_] {:fail 0 :error 0})]
          (should= 0 (main/run []))))

      (it "returns 1 when run-specs returns map with :fail > 0"
        (with-redefs [fw/run-specs (fn [_] {:fail 2 :error 0})]
          (should= 1 (main/run []))))

      (it "returns 1 when run-specs returns map with :error > 0"
        (with-redefs [fw/run-specs (fn [_] {:fail 0 :error 1})]
          (should= 1 (main/run []))))

      (it "returns 0 when run-specs returns non-number non-map (else branch)"
        (with-redefs [fw/run-specs (fn [_] nil)]
          (should= 0 (main/run [])))))))
