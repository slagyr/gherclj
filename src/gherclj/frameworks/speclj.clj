(ns gherclj.frameworks.speclj
  (:require [clojure.string :as str]
            [gherclj.core :as g]
            [gherclj.generator :as gen]
            [speclj.cli :as speclj]
            [speclj.core :as sc]))

(defmethod gen/generate-ns-form :speclj
  [_config source step-ns-syms]
  (let [ns-name (str (#'gen/source->ns-name source "-spec"))
        step-reqs (->> step-ns-syms
                       sort
                       (map #(str "            [" % " :as " (gen/ns->alias %) "]")))]
    (str "(ns " ns-name "\n"
         "  (:require [speclj.core :refer :all]\n"
         "            [gherclj.core :as g]"
         (when (seq step-reqs)
           (str "\n" (str/join "\n" step-reqs)))
         "))")))

(defmethod gen/wrap-feature :speclj
  [_config feature-name scenario-blocks]
  (str "(describe \"" feature-name "\"\n\n"
       scenario-blocks ")\n"))

(defmethod gen/wrap-scenario :speclj
  [_config scenario background]
  (let [title (:scenario scenario)
        bg-calls (when background
                   (->> (:steps background)
                        (filter :classified?)
                        (map #'gen/generate-step-call-with-extras)))
        step-calls (->> (:steps scenario)
                        (map #'gen/generate-step-call-with-extras))
        all-calls (concat ["(g/reset!)"] bg-calls step-calls)
        body (->> all-calls
                  (map #(str "      " %))
                  (str/join "\n"))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         body "))")))

(defmethod gen/wrap-pending :speclj
  [_config scenario background]
  (let [title (:scenario scenario)
        step-comments (->> (concat (when background
                                     (map (fn [s] (str ";; " (name (:type s)) " " (:text s)))
                                          (:steps background)))
                                   (map (fn [s] (str ";; " (name (:type s)) " " (:text s)))
                                        (:steps scenario)))
                           (map #(str "      " %))
                           (str/join "\n"))]
    (str "  (context \"" title "\"\n"
         "    (it \"" title "\"\n"
         step-comments "\n"
         "      (pending \"not yet implemented\")))")))

(defmethod gen/run-specs :speclj
  [config]
  (g/set-test-framework! :speclj)
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        fw-opts (or (:framework-opts config) [])
        args (if (seq fw-opts)
               fw-opts
               ["-c" output-dir "-s" "src"])]
    (apply speclj/run args)))

(defmethod g/should= :speclj [expected actual] (sc/should= expected actual))
(defmethod g/should :speclj [value] (sc/should value))
(defmethod g/should-not :speclj [value] (sc/should-not value))
(defmethod g/should-be-nil :speclj [value] (sc/should-be-nil value))
(defmethod g/should-not-be-nil :speclj [value] (sc/should-not-be-nil value))
