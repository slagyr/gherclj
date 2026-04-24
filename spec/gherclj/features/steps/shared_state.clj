(ns gherclj.features.steps.shared-state
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [clojure.edn :as edn]
            [clojure.string :as str]))

;; --- When steps ---

(defwhen state-is-reset "the state is reset"
  []
  (g/reset!))

(defwhen assoc-kv "assoc! {args:string}"
  [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        pairs (partition 2 parsed)]
    (doseq [[k v] pairs]
      (g/assoc! k v))))

(defwhen assoc-in-kv #"^assoc-in! (.+)$"
  [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        ks (first parsed)
        v (second parsed)]
    (g/assoc-in! ks v)))

(defwhen dissoc-k "dissoc! {args:string}"
  [args]
  (let [parsed (edn/read-string (str "[" args "]"))]
    (apply g/dissoc! parsed)))

(defn- resolve-val [v]
  (if (symbol? v) (deref (resolve v)) v))

(defwhen swap-fn "swap! {args:string}"
  [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        f (resolve-val (first parsed))
        remaining (mapv resolve-val (rest parsed))]
    (apply g/swap! f remaining)))

(defwhen update-kf "update! {args:string}"
  [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        k (first parsed)
        f (resolve-val (second parsed))
        remaining (mapv resolve-val (rest (rest parsed)))]
    (apply g/update! k f remaining)))

(defwhen update-in-kf #"^update-in! (.+)$"
  [args]
  (let [parsed (edn/read-string (str "[" args "]"))
        ks (first parsed)
        f (resolve-val (second parsed))
        remaining (mapv resolve-val (rest (rest parsed)))]
    (apply g/update-in! ks f remaining)))

(defwhen store-internal-data "gherclj stores internal data"
  []
  ;; The :_gherclj key is set by g/reset! — just verify it exists
  nil)

;; --- Then steps ---

(defthen state-should-be "the state should be:"
  [doc-string]
  (g/should= (edn/read-string doc-string) (g/get)))

(defthen get-should-return "get should return:"
  [doc-string]
  (g/should= (edn/read-string doc-string) (g/get)))

(defthen get-key-should-return "get {key} should return {value}"
  [key value]
  (let [k (edn/read-string key)
        expected (edn/read-string value)]
    (g/should= expected (g/get k))))

(defthen get-key-default-should-return "get {key} {default} should return {value}"
  [key default value]
  (let [k (edn/read-string key)
        d (edn/read-string default)
        expected (edn/read-string value)]
    (g/should= expected (g/get k d))))

(defthen get-in-should-return "get-in {keys:string} should return {value}"
  [keys value]
  (let [ks (edn/read-string keys)
        expected (edn/read-string value)]
    (g/should= expected (g/get-in ks))))

(defthen get-in-should-return-doc "get-in {keys:string} should return:"
  [keys doc-string]
  (let [ks (edn/read-string keys)
        expected (edn/read-string doc-string)]
    (g/should= expected (g/get-in ks))))

(defthen get-key-should-not-be-nil "get {key} should not be nil"
  [key]
  (let [k (edn/read-string key)]
    (g/should-not-be-nil (g/get k))))

(defthen get-key-should-be-nil "get {key} should be nil"
  [key]
  (let [k (edn/read-string key)]
    (g/should-be-nil (g/get k))))
