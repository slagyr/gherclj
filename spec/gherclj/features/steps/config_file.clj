(ns gherclj.features.steps.config-file
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [gherclj.config :as config]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private test-root
  (str (System/getProperty "java.io.tmpdir") "/gherclj-config-test"))

(defn- cleanup! []
  (let [f (io/file test-root "gherclj.edn")]
    (when (.exists f) (.delete f)))
  (let [d (io/file test-root)]
    (when (.exists d) (.delete d))))

;; --- Given steps ---

(defgiven config-at-root "a gherclj.edn file at the project root:"
  [doc-string]
  (cleanup!)
  (io/make-parents (io/file test-root "dummy"))
  (spit (str test-root "/gherclj.edn") doc-string))

(defgiven config-on-classpath "a gherclj.edn file on the classpath:"
  "Stores to :classpath-config in state. NOT written to disk yet — written as root config when 'the config is loaded' runs if no root file exists."
  [doc-string]
  (g/assoc! :classpath-config doc-string))

(defgiven no-config-file "no gherclj.edn file exists"
  []
  (cleanup!))

;; --- When steps ---

(defwhen load-config "the config is loaded"
  "If :classpath-config is set and no root file exists, writes it to disk first. Then loads config from the test root dir."
  []
  (when (and (g/get :classpath-config)
             (not (.exists (io/file test-root "gherclj.edn"))))
    (io/make-parents (io/file test-root "dummy"))
    (spit (str test-root "/gherclj.edn") (g/get :classpath-config)))
  (let [result (config/load-config {:root-path test-root})]
    (g/assoc! :loaded-config result)))

;; --- Then steps ---

(defthen resolved-config-should-contain "the resolved config should contain:"
  [doc-string]
  (let [expected (edn/read-string doc-string)
        actual (g/get :loaded-config)]
    (doseq [[k v] expected]
      (g/should= v (clojure.core/get actual k)))))

(defthen resolved-config-should-be "the resolved config should be:"
  [doc-string]
  (let [expected (edn/read-string doc-string)
        actual (g/get :loaded-config)]
    (g/should= expected actual)))

(defthen config-should-be-invalid "the config should be invalid with message {text:string}"
  [text]
  (let [result (g/get :loaded-config)]
    (g/should (config/invalid? result))
    (g/should (str/includes? (config/error-message result) text))))
