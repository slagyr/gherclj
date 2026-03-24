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

(def feature-content
  (str "Feature: Authentication\n"
       "\n"
       "  Scenario: User can log in\n"
       "    Given a user \"alice\" with role \"admin\"\n"
       "    When the user logs in\n"
       "    Then the response status should be 200\n"))

(defn- tmp [name] (str (System/getProperty "java.io.tmpdir") "/gherclj-test-" name))

(defn- cleanup [& dirs]
  (doseq [dir dirs]
    (doseq [f (reverse (file-seq (io/file dir)))] (.delete f))))

(describe "Pipeline"

  (context "parse!"

    (it "parses feature files into EDN IR files"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (pipeline/parse! {:features-dir features-dir :edn-dir edn-dir})

          (let [edn-file (io/file edn-dir "auth.edn")
                ir (edn/read-string (slurp edn-file))]
            (should (.exists edn-file))
            (should= "Authentication" (:feature ir))
            (should= 1 (count (:scenarios ir)))
            (should= "User can log in" (:scenario (first (:scenarios ir))))
            (should= 3 (count (:steps (first (:scenarios ir))))))
          (finally (cleanup features-dir edn-dir)))))

    (it "is silent by default"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (let [output (with-out-str (pipeline/parse! {:features-dir features-dir :edn-dir edn-dir}))]
            (should= "" output))
          (finally (cleanup features-dir edn-dir)))))

    (it "prints progress when :verbose is true"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (let [output (with-out-str (pipeline/parse! {:features-dir features-dir :edn-dir edn-dir :verbose true}))]
            (should (str/includes? output "Parsing"))
            (should (str/includes? output "scenarios parsed")))
          (finally (cleanup features-dir edn-dir))))))

  (context "run!"

    (it "runs the full pipeline: feature -> edn -> spec"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            output-dir (tmp "output")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :harness-ns 'myapp.harness
             :test-framework :speclj})

          (should (.exists (io/file edn-dir "auth.edn")))

          (let [content (slurp (io/file output-dir "auth_spec.clj"))]
            (should (str/includes? content "(describe \"Authentication\""))
            (should (str/includes? content "(gherclj.pipeline-spec/setup-user \"alice\" \"admin\")"))
            (should (str/includes? content "(gherclj.pipeline-spec/user-logs-in)"))
            (should (str/includes? content "(gherclj.pipeline-spec/response-status 200)")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "is silent by default"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            output-dir (tmp "output")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (let [output (with-out-str
                         (pipeline/run!
                           {:features-dir features-dir
                            :edn-dir edn-dir
                            :output-dir output-dir
                            :step-namespaces ['gherclj.pipeline-spec]
                            :harness-ns 'myapp.harness
                            :test-framework :speclj}))]
            (should= "" output))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "prints progress when :verbose is true"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            output-dir (tmp "output")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (let [output (with-out-str
                         (pipeline/run!
                           {:features-dir features-dir
                            :edn-dir edn-dir
                            :output-dir output-dir
                            :step-namespaces ['gherclj.pipeline-spec]
                            :harness-ns 'myapp.harness
                            :test-framework :speclj
                            :verbose true}))]
            (should (str/includes? output "Parsing"))
            (should (str/includes? output "Generating")))
          (finally (cleanup features-dir edn-dir output-dir)))))))
