(ns gherclj.core
  (:refer-clojure :exclude [get get-in swap! dissoc update update-in reset! assoc! dissoc!])
  (:require [clojure.string :as str]
            [gherclj.template :as template]))

;; --- State management ---
;; Shared state atom for step definitions to use. Internal gherclj
;; data lives under :_gherclj to avoid collisions with user state.

(defonce ^:private state (atom {}))

(defn reset!
  "Clear all state, preserving only the internal :_gherclj key."
  []
  (clojure.core/swap! state (fn [_] {:_gherclj {}})))

(defn get
  "Access state. No args returns user-visible state (excludes internal keys),
   with key returns value, with key+default returns value or default."
  ([] (clojure.core/dissoc @state :_gherclj))
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
;; Framework-agnostic matchers for use in step definitions.

(defn should= [expected actual]
  (when (not= expected actual)
    (throw (AssertionError. (str "Expected: " (pr-str expected) "\n     got: " (pr-str actual))))))

(defn should [value]
  (when-not value
    (throw (AssertionError. (str "Expected truthy but was: " (pr-str value))))))

(defn should-not [value]
  (when value
    (throw (AssertionError. (str "Expected falsy but was: " (pr-str value))))))

(defn should-be-nil [value]
  (when (some? value)
    (throw (AssertionError. (str "Expected nil but was: " (pr-str value))))))

(defn should-not-be-nil [value]
  (when (nil? value)
    (throw (AssertionError. "Expected not nil but was: nil"))))

;; --- Step registry ---
;; Each namespace that uses defgiven/defwhen/defthen accumulates steps here,
;; keyed by namespace symbol.

(defonce ^:private registry (atom {}))

(defn register-step!
  "Register a step definition. Called by the defgiven/defwhen/defthen macros."
  [ns-sym step-type step-name template-or-regex compiled]
  (let [entry (merge {:name (name step-name)
                      :type step-type
                      :ns ns-sym}
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
    (if (> (count matches) 1)
      ;; Prefer the most specific match (most literal characters in template)
      (let [literal-len (fn [step]
                          (count (str/replace (or (:template step) "") #"\{[^}]+\}" "")))
            sorted (sort-by literal-len > matches)
            best (first sorted)
            runner-up (second sorted)]
        (if (> (literal-len best) (literal-len runner-up))
          best
          (let [names (mapv :name matches)]
            (throw (RuntimeException.
                     (str "Ambiguous step match — \"" text "\" matches: "
                          (str/join ", " names)))))))
      (first matches))))

;; --- Macros ---

(defmacro defstep* [step-type step-name template-or-regex args & body]
  (let [ns-sym (ns-name *ns*)]
    `(do
       (defn ~step-name ~(if (string? template-or-regex) template-or-regex "") ~args ~@body)
       (register-step! '~ns-sym ~step-type '~step-name
                       ~template-or-regex
                       ~(when (string? template-or-regex)
                          `(template/compile-template ~template-or-regex))))))

(defmacro defgiven
  "Define a Given step. Reads like defn with a docstring.

   (defgiven add-project \"a project \\\"{slug}\\\" with timeout {timeout:int}\"
     [slug timeout]
     (h/add-project slug {:timeout timeout}))"
  [step-name template-or-regex args & body]
  `(defstep* :given ~step-name ~template-or-regex ~args ~@body))

(defmacro defwhen
  "Define a When step."
  [step-name template-or-regex args & body]
  `(defstep* :when ~step-name ~template-or-regex ~args ~@body))

(defmacro defthen
  "Define a Then step."
  [step-name template-or-regex args & body]
  `(defstep* :then ~step-name ~template-or-regex ~args ~@body))
