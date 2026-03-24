(ns gherclj.frameworks.clojure-test
  (:require [clojure.string :as str]
            [gherclj.generator :as gen]))

(defn- slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-|-$" "")))

(defmethod gen/generate-ns-form :clojure.test
  [config source step-ns-syms harness-ns]
  (let [ns-name (str (#'gen/source->ns-name source "-test"))
        harness-req (str "            [" harness-ns " :as h]")
        step-reqs (->> step-ns-syms
                       sort
                       (map #(str "            [" % "]")))]
    (str "(ns " ns-name "\n"
         "  (:require [clojure.test :refer :all]\n"
         harness-req
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
                        (map #'gen/generate-step-call-with-table)))
        step-calls (->> (:steps scenario)
                        (map #'gen/generate-step-call-with-table))
        all-calls (concat ["(h/reset!)"] bg-calls step-calls)
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
