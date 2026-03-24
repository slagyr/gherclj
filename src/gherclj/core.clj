(ns gherclj.core
  (:require [gherclj.template :as template]))

;; --- Step registry ---
;; Each namespace that uses defgiven/defwhen/defthen accumulates steps here,
;; keyed by namespace symbol.

(defonce ^:private registry (atom {}))

(defn register-step!
  "Register a step definition. Called by the defgiven/defwhen/defthen macros."
  [ns-sym step-type step-name template-or-regex compiled]
  (let [entry (merge {:name (name step-name)
                      :type step-type
                      :ns ns-sym
                      :fn-var (ns-resolve ns-sym step-name)}
                     (if (instance? java.util.regex.Pattern template-or-regex)
                       {:regex template-or-regex}
                       {:template template-or-regex
                        :regex (:regex compiled)
                        :bindings (:bindings compiled)}))]
    (swap! registry update ns-sym (fnil conj []) entry)
    nil))

(defn steps-in-ns
  "Return all step definitions registered in the given namespace."
  [ns-sym]
  (get @registry ns-sym []))

(defn collect-steps
  "Collect all step definitions from the given namespace symbols, in order.
   Returns a flat vector of step entries."
  [ns-syms]
  (into [] (mapcat #(steps-in-ns %)) ns-syms))

(defn classify-step
  "Match step text against collected steps. Returns the first matching step
   entry with :args populated, or nil if no match."
  [steps text]
  (some (fn [{:keys [regex bindings] :as step}]
          (when-let [match (re-matches regex text)]
            (let [groups (if (string? match) [] (vec (rest match)))
                  args (if bindings
                         (mapv (fn [group {:keys [coerce]}] (coerce group))
                               groups bindings)
                         groups)]
              (assoc step :args args))))
        steps))

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
