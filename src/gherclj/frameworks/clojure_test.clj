(ns gherclj.frameworks.clojure-test
  (:require [clojure.string :as str]
            [clojure.test :as ct]
            [gherclj.core :as g]
            [gherclj.framework :as fw]
            [gherclj.generator :as gen]
            [gherclj.lifecycle :as lifecycle]))

(defn- slugify [title]
  (-> title
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-|-$" "")))

(defmethod fw/generate-preamble :clojure.test
  [_config source used-nses]
  (let [ns-name (str (gen/source->ns-name source "-test"))
        helper-imports (->> used-nses
                            (mapcat g/helper-imports-in-ns)
                            distinct)
        helper-reqs (->> helper-imports
                         sort
                         (map #(str "            [" % " :as " (gen/ns->alias %) "]")))]
    (str "(ns " ns-name "\n"
         "  (:require [clojure.test :refer :all]\n"
         "            [gherclj.core :as g]\n"
         "            [gherclj.lifecycle :as lifecycle]"
         (when (seq helper-reqs)
           (str "\n" (str/join "\n" helper-reqs)))
         "))")))

(defmethod fw/wrap-feature :clojure.test
  [_config _feature-name scenario-blocks]
  (str "(defn ^:private feature-fixture [f]\n"
       "  (lifecycle/run-before-feature-hooks!)\n"
       "  (try\n"
       "    (f)\n"
       "    (finally\n"
       "      (lifecycle/run-after-feature-hooks!))))\n\n"
       "(defn ^:private scenario-fixture [f]\n"
       "  (g/reset!)\n"
       "  (lifecycle/run-before-scenario-hooks!)\n"
       "  (try\n"
       "    (f)\n"
       "    (finally\n"
       "      (lifecycle/run-after-scenario-hooks!))))\n\n"
       "(use-fixtures :once feature-fixture)\n"
       "(use-fixtures :each scenario-fixture)\n\n"
       scenario-blocks "\n"))

(defmethod fw/wrap-scenario :clojure.test
  [_config scenario background]
  (let [title      (:scenario scenario)
        test-name  (slugify title)
        bg-calls   (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body       (->> (concat bg-calls step-calls)
                        (map #(str "    " %))
                        (str/join "\n"))]
    (str "(deftest " test-name "\n"
         "  (testing \"" title "\"\n"
         body "))")))

(defmethod fw/wrap-pending :clojure.test
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

(defmethod fw/run-specs :clojure.test
  [config]
  (g/set-framework! :clojure.test)
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        dir (clojure.java.io/file output-dir)
        test-files (->> (file-seq dir)
                        (filter #(str/ends-with? (.getName %) ".clj"))
                        (sort-by #(str (.toPath %))))
        test-nses (keep read-ns-name test-files)]
    (lifecycle/run-before-all-hooks!)
    (try
      (doseq [ns-sym test-nses]
        (when (find-ns ns-sym)
          (remove-ns ns-sym)))
      (doseq [f test-files]
        (load-file (.getPath f)))
      (let [loaded (keep find-ns test-nses)]
        (binding [ct/*test-out* (java.io.StringWriter.)]
          (apply (resolve 'clojure.test/run-tests) loaded)))
      (finally
        (lifecycle/run-after-all-hooks!)))))

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
