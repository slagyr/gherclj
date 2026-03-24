(ns gherclj.pipeline-spec
  (:require [speclj.core :refer :all]
            [gherclj.pipeline :as pipeline]
            [gherclj.core :refer [defgiven defwhen defthen]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
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

  (context "parse!"

    (it "parses feature files into EDN IR files"
      (let [features-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-features")
            edn-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-edn")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Authentication\n"
                   "\n"
                   "  Scenario: User can log in\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (try
          (with-out-str (pipeline/parse! {:features-dir features-dir :edn-dir edn-dir}))

          (let [edn-file (io/file edn-dir "auth.edn")
                ir (edn/read-string (slurp edn-file))]
            (should (.exists edn-file))
            (should= "Authentication" (:feature ir))
            (should= 1 (count (:scenarios ir)))
            (should= "User can log in" (:scenario (first (:scenarios ir))))
            (should= 3 (count (:steps (first (:scenarios ir))))))
          (finally
            (doseq [f (reverse (file-seq (io/file features-dir)))] (.delete f))
            (doseq [f (reverse (file-seq (io/file edn-dir)))] (.delete f)))))))

  (context "run!"

    (it "runs the full pipeline: feature -> edn -> spec"
      (let [features-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-features")
            edn-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-edn")
            output-dir (str (System/getProperty "java.io.tmpdir") "/gherclj-test-output")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Authentication\n"
                   "\n"
                   "  Scenario: User can log in\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (try
          (with-out-str
            (pipeline/run!
              {:features-dir features-dir
               :edn-dir edn-dir
               :output-dir output-dir
               :step-namespaces ['gherclj.pipeline-spec]
               :harness-ns 'myapp.harness
               :test-framework :speclj}))

          ;; EDN IR should exist
          (should (.exists (io/file edn-dir "auth.edn")))

          ;; Generated spec should exist and be correct
          (let [content (slurp (io/file output-dir "auth_spec.clj"))]
            (should (str/includes? content "(describe \"Authentication\""))
            (should (str/includes? content "(gherclj.pipeline-spec/setup-user \"alice\" \"admin\")"))
            (should (str/includes? content "(gherclj.pipeline-spec/user-logs-in)"))
            (should (str/includes? content "(gherclj.pipeline-spec/response-status 200)")))
          (finally
            (doseq [f (reverse (file-seq (io/file features-dir)))] (.delete f))
            (doseq [f (reverse (file-seq (io/file edn-dir)))] (.delete f))
            (doseq [f (reverse (file-seq (io/file output-dir)))] (.delete f))))))))
