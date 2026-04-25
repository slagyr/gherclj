(ns gherclj.features.steps.config-file
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.config :as config]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(helper! gherclj.features.steps.config-file)

(def ^:private test-root
  (str (System/getProperty "java.io.tmpdir") "/gherclj-config-test"))

(defn- cleanup! []
  (let [f (io/file test-root "gherclj.edn")]
    (when (.exists f) (.delete f)))
  (let [d (io/file test-root)]
    (when (.exists d) (.delete d))))

;; --- Helper fns ---

(defn config-at-root! [doc-string]
  (cleanup!)
  (io/make-parents (io/file test-root "dummy"))
  (spit (str test-root "/gherclj.edn") doc-string))

(defn config-on-classpath! [doc-string]
  (g/assoc! :classpath-config doc-string))

(defn no-config-file! []
  (cleanup!))

(defn load-config! []
  (when (and (g/get :classpath-config)
             (not (.exists (io/file test-root "gherclj.edn"))))
    (io/make-parents (io/file test-root "dummy"))
    (spit (str test-root "/gherclj.edn") (g/get :classpath-config)))
  (let [result (config/load-config {:root-path test-root})]
    (g/assoc! :loaded-config result)))

(defn resolved-config-should-contain [doc-string]
  (let [expected (edn/read-string doc-string)
        actual (g/get :loaded-config)]
    (doseq [[k v] expected]
      (g/should= v (clojure.core/get actual k)))))

(defn resolved-config-should-be [doc-string]
  (let [expected (edn/read-string doc-string)
        actual (g/get :loaded-config)]
    (g/should= expected actual)))

(defn config-should-be-invalid [text]
  (let [result (g/get :loaded-config)]
    (g/should (config/invalid? result))
    (g/should (str/includes? (config/error-message result) text))))

;; --- Step defs ---

(defgiven "a gherclj.edn file at the project root:" config-file/config-at-root!)

(defgiven "a gherclj.edn file on the classpath:" config-file/config-on-classpath!
  "Stores to :classpath-config in state. NOT written to disk yet — written as root config when 'the config is loaded' runs if no root file exists.")

(defgiven "no gherclj.edn file exists" config-file/no-config-file!)

(defwhen "the config is loaded" config-file/load-config!
  "If :classpath-config is set and no root file exists, writes it to disk first. Then loads config from the test root dir.")

(defthen "the resolved config should contain:" config-file/resolved-config-should-contain)

(defthen "the resolved config should be:" config-file/resolved-config-should-be)

(defthen "the config should be invalid with message {text:string}" config-file/config-should-be-invalid)
