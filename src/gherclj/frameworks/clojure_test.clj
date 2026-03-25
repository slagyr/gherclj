(ns gherclj.frameworks.clojure-test
  (:require [clojure.string :as str]
            [gherclj.generator :as gen]))

(defn- slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-|-$" "")))

(defmethod gen/generate-ns-form :clojure.test
  [_config source step-ns-syms]
  (let [ns-name (str (#'gen/source->ns-name source "-test"))
        step-reqs (->> step-ns-syms
                       sort
                       (map #(str "            [" % " :as " (gen/ns->alias %) "]")))]
    (str "(ns " ns-name "\n"
         "  (:require [clojure.test :refer :all]\n"
         "            [gherclj.core :as g]"
         (when (seq step-reqs)
           (str "\n" (str/join "\n" step-reqs)))
         "))")))

(defmethod gen/wrap-feature :clojure.test
  [_config _feature-name scenario-blocks]
  (str scenario-blocks "\n"))

(defmethod gen/wrap-scenario :clojure.test
  [_config scenario background]
  (let [title (:scenario scenario)
        test-name (slugify title)
        bg-calls (when background
                   (->> (:steps background)
                        (filter :classified?)
                        (map #'gen/generate-step-call-with-extras)))
        step-calls (->> (:steps scenario)
                        (map #'gen/generate-step-call-with-extras))
        all-calls (concat ["(g/reset!)"] bg-calls step-calls)
        body (->> all-calls
                  (map #(str "    " %))
                  (str/join "\n"))]
    (str "(deftest " test-name "\n"
         "  (testing \"" title "\"\n"
         body "))")))

(defmethod gen/wrap-pending :clojure.test
  [_config scenario _background]
  (let [title (:scenario scenario)
        test-name (slugify title)]
    (str "(deftest " test-name "\n"
         "  (testing \"" title "\"\n"
         "    ;; TODO: not yet implemented\n"
         "    ))")))

(defmethod gen/run-specs :clojure.test
  [config]
  (require 'clojure.test)
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        dir (clojure.java.io/file output-dir)
        test-nses (->> (.listFiles dir)
                       (filter #(str/ends-with? (.getName %) ".clj"))
                       (map #(-> (.getName %)
                                 (str/replace #"\.clj$" "")
                                 (str/replace #"_" "-")
                                 symbol)))]
    (doseq [ns-sym test-nses]
      (require ns-sym))
    (apply (resolve 'clojure.test/run-tests) test-nses)))
