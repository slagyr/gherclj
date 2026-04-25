(ns gherclj.features.steps.shared-state
  (:require [gherclj.core :as g :refer [defwhen defthen helper!]]
            [clojure.edn :as edn]))

(helper! gherclj.features.steps.shared-state)

(defn- resolve-val [v]
  (if (symbol? v) (deref (resolve v)) v))

;; --- Helper fns (When) ---

(defn state-is-reset! []
  (g/reset!))

(defn assoc-kv! [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        pairs (partition 2 parsed)]
    (doseq [[k v] pairs]
      (g/assoc! k v))))

(defn assoc-in-kv! [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        ks (first parsed)
        v (second parsed)]
    (g/assoc-in! ks v)))

(defn dissoc-k! [args]
  (let [parsed (edn/read-string (str "[" args "]"))]
    (apply g/dissoc! parsed)))

(defn swap-fn! [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        f (resolve-val (first parsed))
        remaining (mapv resolve-val (rest parsed))]
    (apply g/swap! f remaining)))

(defn update-kf! [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        k (first parsed)
        f (resolve-val (second parsed))
        remaining (mapv resolve-val (rest (rest parsed)))]
    (apply g/update! k f remaining)))

(defn update-in-kf! [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        ks (first parsed)
        f (resolve-val (second parsed))
        remaining (mapv resolve-val (rest (rest parsed)))]
    (apply g/update-in! ks f remaining)))

(defn store-internal-data! []
  ;; The :_gherclj key is set by g/reset! — just verify it exists
  nil)

;; --- Helper fns (Then) ---

(defn state-should-be [doc-string]
  (g/should= (edn/read-string doc-string) (g/get)))

(defn get-should-return [doc-string]
  (g/should= (edn/read-string doc-string) (g/get)))

(defn get-key-should-return [key value]
  (let [k (edn/read-string key)
        expected (edn/read-string value)]
    (g/should= expected (g/get k))))

(defn get-key-default-should-return [key default value]
  (let [k (edn/read-string key)
        d (edn/read-string default)
        expected (edn/read-string value)]
    (g/should= expected (g/get k d))))

(defn get-in-should-return [keys value]
  (let [ks (edn/read-string keys)
        expected (edn/read-string value)]
    (g/should= expected (g/get-in ks))))

(defn get-in-should-return-doc [keys doc-string]
  (let [ks (edn/read-string keys)
        expected (edn/read-string doc-string)]
    (g/should= expected (g/get-in ks))))

(defn get-key-should-not-be-nil [key]
  (let [k (edn/read-string key)]
    (g/should-not-be-nil (g/get k))))

(defn get-key-should-be-nil [key]
  (let [k (edn/read-string key)]
    (g/should-be-nil (g/get k))))

;; --- Step defs ---

(defwhen "the state is reset" shared-state/state-is-reset!)

(defwhen "assoc! {args:string}" shared-state/assoc-kv!)

(defwhen #"^assoc-in! (.+)$" shared-state/assoc-in-kv!)

(defwhen "dissoc! {args:string}" shared-state/dissoc-k!)

(defwhen "swap! {args:string}" shared-state/swap-fn!)

(defwhen "update! {args:string}" shared-state/update-kf!)

(defwhen #"^update-in! (.+)$" shared-state/update-in-kf!)

(defwhen "gherclj stores internal data" shared-state/store-internal-data!)

(defthen "the state should be:" shared-state/state-should-be)

(defthen "get should return:" shared-state/get-should-return)

(defthen "get {key} should return {value}" shared-state/get-key-should-return)

(defthen "get {key} {default} should return {value}" shared-state/get-key-default-should-return)

(defthen "get-in {keys:string} should return {value}" shared-state/get-in-should-return)

(defthen "get-in {keys:string} should return:" shared-state/get-in-should-return-doc)

(defthen "get {key} should not be nil" shared-state/get-key-should-not-be-nil)

(defthen "get {key} should be nil" shared-state/get-key-should-be-nil)
