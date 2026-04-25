(ns gherclj.pipeline-spec
  (:require [speclj.core :refer :all]
            [gherclj.pipeline :as pipeline]
            [gherclj.generator :as gen]
            [gherclj.framework :as fw]
            [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(helper! gherclj.pipeline-spec)

;; Minimal custom framework for fallback-path coverage
(defmethod fw/generate-preamble :custom-pipeline-fw [_ source _]
  (str "(ns custom-fw-test)"))
(defmethod fw/wrap-feature :custom-pipeline-fw [_ _ scenario-blocks]
  scenario-blocks)
(defmethod fw/wrap-scenario :custom-pipeline-fw [_ scenario _]
  (str "(it \"" (:scenario scenario) "\")"))
(defmethod fw/wrap-pending :custom-pipeline-fw [_ scenario _]
  (str "(pending \"" (:scenario scenario) "\")"))

;; Sample helpers for pipeline test
(defn summon-hero [name role] :setup)
(defn enter-the-realm [] :login)
(defn check-the-gate [status] :check)

(defgiven "a user {name:string} with role {role:string}"      pipeline-spec/summon-hero)
(defwhen  "the user logs in"                                   pipeline-spec/enter-the-realm)
(defthen  "the response status should be {status:int}"        pipeline-spec/check-the-gate)

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
          (finally (cleanup features-dir edn-dir)))))

    (it "parses an empty features directory without error"
      (let [features-dir (tmp "features-empty")
            edn-dir (tmp "edn-empty")]
        (.mkdirs (io/file features-dir))
        (try
          (pipeline/parse! {:features-dir features-dir :edn-dir edn-dir})
          (should-not (.exists (io/file edn-dir)))
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
             :framework :clojure/speclj})

          (should (.exists (io/file edn-dir "auth.edn")))

          (let [content (slurp (io/file output-dir "auth_spec.clj"))]
            (should (str/includes? content "(describe \"Authentication\""))
            (should (str/includes? content "(pipeline-spec/summon-hero \"alice\" \"admin\")"))
            (should (str/includes? content "(pipeline-spec/enter-the-realm)"))
            (should (str/includes? content "(pipeline-spec/check-the-gate 200)")))
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
                            :framework :clojure/speclj}))]
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
                            :framework :clojure/speclj
                            :verbose true}))]
            (should (str/includes? output "Parsing"))
            (should (str/includes? output "Generating")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "resolves glob patterns in step-namespaces"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            output-dir (tmp "output")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Auth\n"
                   "\n"
                   "  Scenario: Login\n"
                   "    Given a user \"alice\"\n"
                   "    When the user logs in\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ["gherclj.sample.*"]
             :framework :clojure/speclj})

          (should (.exists (io/file output-dir "auth_spec.clj")))
          (let [content (slurp (io/file output-dir "auth_spec.clj"))]
            (should (str/includes? content "app-steps/create-adventurer")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "generates clojure.test files with _test suffix"
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
             :framework :clojure/test})

          (should (.exists (io/file output-dir "auth_test.clj")))
          (let [content (slurp (io/file output-dir "auth_test.clj"))]
            (should (str/includes? content "deftest"))
            (should (str/includes? content "clojure.test")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "generates spec directly from EDN files via generate!"
      (let [edn-dir (tmp "edn-direct")
            output-dir (tmp "output-direct")
            edn-file (io/file edn-dir "auth.edn")
            ir {:source "auth.feature"
                :feature "Authentication"
                :scenarios [{:scenario "User can log in"
                             :steps [{:type :given :text "a user \"alice\" with role \"admin\"" :classified? true
                                      :fn-sym 'gherclj.pipeline-spec/summon-hero :args ["alice" "admin"]}
                                     {:type :when :text "the user logs in" :classified? true
                                      :fn-sym 'gherclj.pipeline-spec/enter-the-realm :args []}
                                     {:type :then :text "the response status should be 200" :classified? true
                                      :fn-sym 'gherclj.pipeline-spec/check-the-gate :args [200]}]}]}]
        (io/make-parents edn-file)
        (spit edn-file (pr-str ir))
        (try
          (pipeline/generate!
            {:edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :framework :clojure/speclj})
          (let [out-file (io/file output-dir "auth_spec.clj")]
            (should (.exists out-file))
            (should (str/includes? (slurp out-file) "Authentication")))
          (finally (cleanup edn-dir output-dir)))))

    (it "generate! prints progress when :verbose is true"
      (let [edn-dir (tmp "edn-verbose")
            output-dir (tmp "output-verbose")
            edn-file (io/file edn-dir "auth.edn")
            ir {:source "auth.feature"
                :feature "Authentication"
                :scenarios [{:scenario "User can log in"
                             :steps [{:type :given :text "a user \"alice\" with role \"admin\"" :classified? true
                                      :fn-sym 'gherclj.pipeline-spec/summon-hero :args ["alice" "admin"]}
                                     {:type :when :text "the user logs in" :classified? true
                                      :fn-sym 'gherclj.pipeline-spec/enter-the-realm :args []}
                                     {:type :then :text "the response status should be 200" :classified? true
                                      :fn-sym 'gherclj.pipeline-spec/check-the-gate :args [200]}]}]}]
        (io/make-parents edn-file)
        (spit edn-file (pr-str ir))
        (try
          (let [output (with-out-str
                         (pipeline/generate!
                           {:edn-dir edn-dir
                            :output-dir output-dir
                            :step-namespaces ['gherclj.pipeline-spec]
                            :framework :clojure/speclj
                            :verbose true}))]
            (should (str/includes? output "Generating")))
          (finally (cleanup edn-dir output-dir)))))

    (it "uses fallback framework namespace symbol for non-standard :framework values"
      (let [features-dir (tmp "fw-features")
            edn-dir (tmp "fw-edn")
            output-dir (tmp "fw-output")
            feature-file (io/file features-dir "auth.feature")]
        (io/make-parents feature-file)
        (spit feature-file feature-content)
        (try
          (with-redefs [clojure.core/require (fn [_] nil)]
            (pipeline/run!
              {:features-dir features-dir
               :edn-dir edn-dir
               :output-dir output-dir
               :step-namespaces ['gherclj.pipeline-spec]
               :framework :custom-pipeline-fw}))
          (should (.exists (io/file edn-dir "auth.edn")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "removes stale generated spec files when tags exclude every scenario"
      (let [features-dir (tmp "features")
            edn-dir (tmp "edn")
            output-dir (tmp "output")
            feature-file (io/file features-dir "auth.feature")
            spec-file (io/file output-dir "auth_spec.clj")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Authentication\n"
                   "\n"
                   "  @slow\n"
                   "  Scenario: User can log in\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (io/make-parents spec-file)
        (spit spec-file "stale")
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :exclude-tags ["slow"]
             :framework :clojure/speclj})

          (should-not (.exists spec-file))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "generates only the scenario selected by file:line"
      (let [features-dir (tmp "loc-features")
            edn-dir (tmp "loc-edn")
            output-dir (tmp "loc-output")
            feature-file (io/file features-dir "adventure.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Adventure\n"
                   "\n"
                   "  Scenario: Wake the dragon\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"
                   "\n"
                   "  Scenario: Negotiate for treasure\n"
                   "    Given a user \"bob\" with role \"guest\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :locations [{:source "adventure.feature" :line 4}]
             :framework :clojure/speclj})

          (let [content (slurp (io/file output-dir "adventure_spec.clj"))]
            (should (str/includes? content "Wake the dragon"))
            (should-not (str/includes? content "Negotiate for treasure")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "selects a scenario when the location line is inside its body"
      (let [features-dir (tmp "loc-body-features")
            edn-dir (tmp "loc-body-edn")
            output-dir (tmp "loc-body-output")
            feature-file (io/file features-dir "adventure.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Adventure\n"
                   "\n"
                   "  Scenario: Wake the dragon\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "\n"
                   "  Scenario: Crown the raccoon king\n"
                   "    Given a user \"bob\" with role \"guest\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :locations [{:source "adventure.feature" :line 8}]
             :framework :clojure/speclj})

          (let [content (slurp (io/file output-dir "adventure_spec.clj"))]
            (should-not (str/includes? content "Wake the dragon"))
            (should (str/includes? content "Crown the raccoon king")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "rejects unknown file:line selectors"
      (let [features-dir (tmp "loc-missing-features")
            edn-dir (tmp "loc-missing-edn")
            output-dir (tmp "loc-missing-output")
            feature-file (io/file features-dir "adventure.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Adventure\n"
                   "\n"
                   "  Scenario: Wake the dragon\n"
                   "    Given a user \"alice\" with role \"admin\"\n"))
        (try
          (let [message (try
                          (pipeline/run!
                            {:features-dir features-dir
                             :edn-dir edn-dir
                             :output-dir output-dir
                             :step-namespaces ['gherclj.pipeline-spec]
                             :locations [{:source "adventure.feature" :line 99}]
                             :framework :clojure/speclj})
                          nil
                          (catch RuntimeException e
                            (.getMessage e)))]
            (should (str/includes? message "No scenario found for location"))
            (should (str/includes? message "adventure.feature:99")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "ignores scenario-like lines inside doc-strings when selecting locations"
      (let [features-dir (tmp "loc-doc-features")
            edn-dir (tmp "loc-doc-edn")
            output-dir (tmp "loc-doc-output")
            feature-file (io/file features-dir "adventure.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Adventure\n"
                   "\n"
                   "  Scenario: Embedded feature text\n"
                   "    Given a feature file containing:\n"
                   "      \"\"\"\n"
                   "      Feature: Embedded\n"
                   "\n"
                   "        Scenario: Not the real target\n"
                   "          Given something\n"
                   "      \"\"\"\n"
                   "\n"
                   "  Scenario: Real target\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "    When the user logs in\n"
                   "    Then the response status should be 200\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :locations [{:source "adventure.feature" :line 12}]
             :framework :clojure/speclj})

          (let [content (slurp (io/file output-dir "adventure_spec.clj"))]
            (should (str/includes? content "Real target"))
            (should-not (str/includes? content "Embedded feature text")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "generates every scenario when the location has no line number"
      (let [features-dir (tmp "loc-bare-features")
            edn-dir (tmp "loc-bare-edn")
            output-dir (tmp "loc-bare-output")
            feature-file (io/file features-dir "adventure.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Adventure\n"
                   "\n"
                   "  Scenario: Wake the dragon\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "\n"
                   "  Scenario: Negotiate for treasure\n"
                   "    Given a user \"bob\" with role \"guest\"\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :locations [{:source "adventure.feature"}]
             :framework :clojure/speclj})

          (let [content (slurp (io/file output-dir "adventure_spec.clj"))]
            (should (str/includes? content "Wake the dragon"))
            (should (str/includes? content "Negotiate for treasure")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "bare feature location wins when mixed with file:line for the same file"
      (let [features-dir (tmp "loc-union-features")
            edn-dir (tmp "loc-union-edn")
            output-dir (tmp "loc-union-output")
            feature-file (io/file features-dir "adventure.feature")]
        (io/make-parents feature-file)
        (spit feature-file
              (str "Feature: Adventure\n"
                   "\n"
                   "  Scenario: Wake the dragon\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "\n"
                   "  Scenario: Negotiate for treasure\n"
                   "    Given a user \"bob\" with role \"guest\"\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :locations [{:source "adventure.feature" :line 3}
                         {:source "adventure.feature"}]
             :framework :clojure/speclj})

          (let [content (slurp (io/file output-dir "adventure_spec.clj"))]
            (should (str/includes? content "Wake the dragon"))
            (should (str/includes? content "Negotiate for treasure")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "mixes a bare feature and a file:line across two files"
      (let [features-dir (tmp "loc-mixed-features")
            edn-dir (tmp "loc-mixed-edn")
            output-dir (tmp "loc-mixed-output")
            bare-file (io/file features-dir "bare.feature")
            line-file (io/file features-dir "line.feature")]
        (io/make-parents bare-file)
        (spit bare-file
              (str "Feature: Bare\n"
                   "\n"
                   "  Scenario: First bare\n"
                   "    Given a user \"alice\" with role \"admin\"\n"
                   "\n"
                   "  Scenario: Second bare\n"
                   "    Given a user \"bob\" with role \"guest\"\n"))
        (spit line-file
              (str "Feature: Line\n"
                   "\n"
                   "  Scenario: Chosen line\n"
                   "    Given a user \"carol\" with role \"admin\"\n"
                   "\n"
                   "  Scenario: Skipped line\n"
                   "    Given a user \"dave\" with role \"guest\"\n"))
        (try
          (pipeline/run!
            {:features-dir features-dir
             :edn-dir edn-dir
             :output-dir output-dir
             :step-namespaces ['gherclj.pipeline-spec]
             :locations [{:source "bare.feature"}
                         {:source "line.feature" :line 3}]
             :framework :clojure/speclj})

          (let [bare-content (slurp (io/file output-dir "bare_spec.clj"))
                line-content (slurp (io/file output-dir "line_spec.clj"))]
            (should (str/includes? bare-content "First bare"))
            (should (str/includes? bare-content "Second bare"))
            (should (str/includes? line-content "Chosen line"))
            (should-not (str/includes? line-content "Skipped line")))
          (finally (cleanup features-dir edn-dir output-dir)))))

    (it "rejects an unknown bare feature location"
      (let [features-dir (tmp "loc-bare-missing-features")
            edn-dir (tmp "loc-bare-missing-edn")
            output-dir (tmp "loc-bare-missing-output")]
        (io/make-parents (io/file features-dir ".placeholder"))
        (try
          (let [message (try
                          (pipeline/run!
                            {:features-dir features-dir
                             :edn-dir edn-dir
                             :output-dir output-dir
                             :step-namespaces ['gherclj.pipeline-spec]
                             :locations [{:source "ghost.feature"}]
                             :framework :clojure/speclj})
                          nil
                          (catch RuntimeException e
                            (.getMessage e)))]
            (should (str/includes? message "Feature file not found"))
            (should (str/includes? message "ghost.feature")))
          (finally (cleanup features-dir edn-dir output-dir)))))))
