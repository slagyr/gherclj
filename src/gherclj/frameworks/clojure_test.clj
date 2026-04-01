(ns gherclj.frameworks.clojure-test
  (:require [clojure.string :as str]
            [clojure.test :as ct]
            [gherclj.core :as g]
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

(defn- read-ns-name
  "Read the namespace name from a Clojure source file."
  [f]
  (let [forms (read-string (str "[" (slurp f) "]"))
        ns-form (first (filter #(and (seq? %) (= 'ns (first %))) forms))]
    (when ns-form (second ns-form))))

(defmethod gen/run-specs :clojure.test
  [config]
  (g/set-test-framework! :clojure.test)
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        dir (clojure.java.io/file output-dir)
        test-files (->> (file-seq dir)
                        (filter #(str/ends-with? (.getName %) ".clj"))
                        (sort-by #(str (.toPath %))))]
    (doseq [f test-files]
      (load-file (.getPath f)))
    (let [test-nses (keep read-ns-name test-files)
          loaded (keep find-ns test-nses)]
      (apply (resolve 'clojure.test/run-tests) loaded))))

(defn- ct-assert [pass? msg]
  (ct/do-report (if pass?
                  {:type :pass :message msg}
                  {:type :fail :message msg})))

(defmethod g/should= :clojure.test [expected actual]
  (ct-assert (= expected actual)
             (when (not= expected actual)
               (str "Expected: " (pr-str expected) "\n  Actual: " (pr-str actual)))))

(defmethod g/should :clojure.test [value]
  (ct-assert value (str "Expected truthy but was: " (pr-str value))))

(defmethod g/should-not :clojure.test [value]
  (ct-assert (not value) (str "Expected falsy but was: " (pr-str value))))

(defmethod g/should-be-nil :clojure.test [value]
  (ct-assert (nil? value) (str "Expected nil but was: " (pr-str value))))

(defmethod g/should-not-be-nil :clojure.test [value]
  (ct-assert (some? value) "Expected not nil but was: nil"))
