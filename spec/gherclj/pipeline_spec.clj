(ns gherclj.pipeline-spec
  (:require [speclj.core :refer :all]
            [gherclj.pipeline :as pipeline]
            [gherclj.core :refer [defgiven defwhen defthen]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;; Sample steps for pipeline test

(defgiven setup-user "a user \"{name}\" with role \"{role}\""
  [name role]
  :setup)

(defwhen user-logs-in "the user logs in"
  []
  :login)

(defthen response-status "the response status should be {status:int}"
  [status]
  :check)

(describe "Pipeline"

  (context "generate!"

    (it "generates spec files from feature files"
      (let [features-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-features")
            output-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-output")
            feature-file (io/file features-dir "auth.feature")]
        ;; Setup
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Authentication\n"
                   "\n"
                   "  Scenario: User can log in\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (try
          ;; Generate
          (pipeline/generate!
            {:features-dir features-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :harness-ns 'myapp.harness
             :test-framework :speclj})

          ;; Verify output
          (let [output-file (io/file output-dir "auth_spec.clj")
                content (slurp output-file)]
            (should (.exists output-file))
            (should (str/includes? content "(describe \"Authentication\""))
            (should (str/includes? content "(context \"User can log in\""))
            (should (str/includes? content "(gherclj.pipeline-spec/setup-user \"alice\" \"admin\")"))
            (should (str/includes? content "(gherclj.pipeline-spec/user-logs-in)"))
            (should (str/includes? content "(gherclj.pipeline-spec/response-status 200)")))
          ;; Cleanup
          (finally
            (doseq [f (reverse (file-seq (io/file features-dir)))] (.delete f))
            (doseq [f (reverse (file-seq (io/file output-dir)))] (.delete f))))))))
