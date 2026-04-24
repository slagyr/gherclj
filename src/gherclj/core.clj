;; mutation-tested: 2026-03-25
(ns gherclj.core
  (:refer-clojure :exclude [get get-in swap! dissoc update update-in reset! assoc! dissoc!])
  (:require [clojure.string :as str]
            [gherclj.lifecycle :as lifecycle]
            [gherclj.template :as template]))

;; --- State management ---
;; Shared state atom for step definitions to use. Internal gherclj
;; data lives under :_gherclj to avoid collisions with user state.

(defonce ^:private state (atom {}))

(defn before-all [f] (lifecycle/register! :before-all f))
(defn before-feature [f] (lifecycle/register! :before-feature f))
(defn before-scenario [f] (lifecycle/register! :before-scenario f))
(defn after-scenario [f] (lifecycle/register! :after-scenario f))
(defn after-feature [f] (lifecycle/register! :after-feature f))
(defn after-all [f] (lifecycle/register! :after-all f))

(defn reset!
  "Clear user state, preserving internal keys."
  []
  (clojure.core/swap! state (fn [s] {:_gherclj {} :_framework (:_framework s)})))

(defn get
  "Access state. No args returns user-visible state (excludes internal keys),
   with key returns value, with key+default returns value or default."
  ([] (clojure.core/dissoc @state :_gherclj :_framework))
  ([key] (clojure.core/get @state key))
  ([key default] (clojure.core/get @state key default)))

(defn get-in
  "Nested state access."
  ([keys] (clojure.core/get-in @state keys))
  ([keys default] (clojure.core/get-in @state keys default)))

(defn swap!
  "Apply function to entire state."
  [f & args]
  (apply clojure.core/swap! state f args))

(defn assoc!
  "Set key-value pairs in state."
  [key val & kvs]
  (apply clojure.core/swap! state clojure.core/assoc key val kvs))

(defn assoc-in!
  "Set a nested value in state."
  [keys val]
  (clojure.core/swap! state clojure.core/assoc-in keys val))

(defn dissoc!
  "Remove keys from state."
  [key & keys]
  (apply clojure.core/swap! state clojure.core/dissoc key keys))

(defn update!
  "Update a value in state by applying f."
  [key f & args]
  (apply clojure.core/swap! state clojure.core/update key f args))

(defn update-in!
  "Update a nested value in state by applying f."
  [keys f & args]
  (apply clojure.core/swap! state clojure.core/update-in keys f args))

;; --- Assertions ---
;; Delegate to the active test framework via multimethods.

(defn- active-framework []
  (clojure.core/get @state :_framework))

(defn set-framework! [fw]
  (clojure.core/swap! state clojure.core/assoc :_framework fw))

(defmulti should=           (fn [_ _] (active-framework)))
(defmulti should            (fn [_]   (active-framework)))
(defmulti should-not        (fn [_]   (active-framework)))
(defmulti should-be-nil     (fn [_]   (active-framework)))
(defmulti should-not-be-nil (fn [_]   (active-framework)))

(defmethod should= :default [expected actual]
  (when (not= expected actual)
    (throw (AssertionError. (str "Expected: " (pr-str expected) "\n     got: " (pr-str actual))))))
(defmethod should :default [value]
  (when-not value
    (throw (AssertionError. (str "Expected truthy but was: " (pr-str value))))))
(defmethod should-not :default [value]
  (when value
    (throw (AssertionError. (str "Expected falsy but was: " (pr-str value))))))
(defmethod should-be-nil :default [value]
  (when (some? value)
    (throw (AssertionError. (str "Expected nil but was: " (pr-str value))))))
(defmethod should-not-be-nil :default [value]
  (when (nil? value)
    (throw (AssertionError. "Expected not nil but was: nil"))))

;; --- Step registry ---
;; Each namespace that uses defgiven/defwhen/defthen accumulates steps here,
;; keyed by namespace symbol.

(defonce ^:private registry (atom {}))

(defn register-step!
  "Register a step definition. Called by the defgiven/defwhen/defthen macros."
  [ns-sym step-type step-name template-or-regex compiled {:keys [doc file line]}]
  (let [entry (merge {:name (name step-name)
                      :type step-type
                      :ns ns-sym
                      :doc doc
                      :file file
                      :line line}
                     (if (instance? java.util.regex.Pattern template-or-regex)
                       {:regex template-or-regex}
                       {:template template-or-regex
                         :regex (:regex compiled)
                         :bindings (:bindings compiled)}))]
    (clojure.core/swap! registry clojure.core/update ns-sym (fnil conj []) entry)
    nil))

(defn steps-in-ns
  "Return all step definitions registered in the given namespace."
  [ns-sym]
  (clojure.core/get @registry ns-sym []))

(defn collect-steps
  "Collect all step definitions from the given namespace symbols, in order.
   Returns a flat vector of step entries."
  [ns-syms]
  (into [] (mapcat #(steps-in-ns %)) ns-syms))

(defn classify-step
  "Match step text against collected steps. Returns the matching step
   entry with :args populated, or nil if no match.
   Throws if multiple steps match (ambiguous)."
  [steps text]
  (let [matches (keep (fn [{:keys [regex bindings] :as step}]
                        (when-let [match (re-matches regex text)]
                          (let [groups (if (string? match) [] (vec (rest match)))
                                args (if bindings
                                       (mapv (fn [group {:keys [coerce]}] (coerce group))
                                             groups bindings)
                                       groups)]
                            (assoc step :args args))))
                      steps)]
    (when (> (count matches) 1)
      (let [names (mapv :name matches)]
        (throw (RuntimeException.
                 (str "ambiguous step match: \"" text "\" matches: "
                      (str/join ", " names))))))
    (first matches)))

;; --- Macros ---

(defmacro defstep* [step-type step-name template-or-regex docstring args source & body]
  (let [ns-sym (ns-name *ns*)
        fn-doc (or docstring (when (string? template-or-regex) template-or-regex))]
    `(do
       (defn ~step-name ~@(cond-> [] fn-doc (conj fn-doc) true (conj args) true (into body)))
       (register-step! '~ns-sym ~step-type '~step-name
                        ~template-or-regex
                        ~(when (string? template-or-regex)
                           `(template/compile-template ~template-or-regex))
                        ~(assoc source :doc docstring)))))

(defmacro defgiven
  "Define a Given step. Reads like defn with a docstring.

   (defgiven add-project \"a project \\\"{slug}\\\" with timeout {timeout:int}\"
     [slug timeout]
     (h/add-project slug {:timeout timeout}))"
  [step-name template-or-regex & definition]
  (let [[docstring args body] (if (vector? (first definition))
                                [nil (first definition) (rest definition)]
                                [(first definition) (second definition) (nnext definition)])]
    `(defstep* :given ~step-name ~template-or-regex ~docstring ~args
       {:file ~*file* :line ~(-> &form meta :line)}
       ~@body)))

(defmacro defwhen
  "Define a When step."
  [step-name template-or-regex & definition]
  (let [[docstring args body] (if (vector? (first definition))
                                [nil (first definition) (rest definition)]
                                [(first definition) (second definition) (nnext definition)])]
    `(defstep* :when ~step-name ~template-or-regex ~docstring ~args
       {:file ~*file* :line ~(-> &form meta :line)}
       ~@body)))

(defmacro defthen
  "Define a Then step."
  [step-name template-or-regex & definition]
  (let [[docstring args body] (if (vector? (first definition))
                                [nil (first definition) (rest definition)]
                                [(first definition) (second definition) (nnext definition)])]
    `(defstep* :then ~step-name ~template-or-regex ~docstring ~args
       {:file ~*file* :line ~(-> &form meta :line)}
       ~@body)))
