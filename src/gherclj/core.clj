;; mutation-tested: 2026-03-25
(ns gherclj.core
  (:refer-clojure :exclude [get get-in swap! dissoc update update-in reset! assoc! dissoc!])
  (:require [clojure.string :as str]
            [gherclj.lifecycle :as lifecycle]
            [gherclj.template :as template]))

;; --- State management ---
;; Step definitions read and write through dynamic bindings so each scenario
;; can get its own atom without changing the public API.

(def ^:dynamic *state* (atom {}))
(def ^:dynamic *framework* nil)

(defn before-all [f] (lifecycle/register! :before-all f))
(defn before-feature [f] (lifecycle/register! :before-feature f))
(defn before-scenario [f] (lifecycle/register! :before-scenario f))
(defn after-scenario [f] (lifecycle/register! :after-scenario f))
(defn after-feature [f] (lifecycle/register! :after-feature f))
(defn after-all [f] (lifecycle/register! :after-all f))

(defn reset!
  "Clear user state for the current scenario binding."
  []
  (clojure.core/reset! *state* {}))

(defn get
  "Access state. No args returns the whole scenario state,
   with key returns value, with key+default returns value or default."
  ([] @*state*)
  ([key] (clojure.core/get @*state* key))
  ([key default] (clojure.core/get @*state* key default)))

(defn get-in
  "Nested state access."
  ([keys] (clojure.core/get-in @*state* keys))
  ([keys default] (clojure.core/get-in @*state* keys default)))

(defn swap!
  "Apply function to entire state."
  [f & args]
  (apply clojure.core/swap! *state* f args))

(defn assoc!
  "Set key-value pairs in state."
  [key val & kvs]
  (apply clojure.core/swap! *state* clojure.core/assoc key val kvs))

(defn assoc-in!
  "Set a nested value in state."
  [keys val]
  (clojure.core/swap! *state* clojure.core/assoc-in keys val))

(defn dissoc!
  "Remove keys from state."
  [key & keys]
  (apply clojure.core/swap! *state* clojure.core/dissoc key keys))

(defn update!
  "Update a value in state by applying f."
  [key f & args]
  (apply clojure.core/swap! *state* clojure.core/update key f args))

(defn update-in!
  "Update a nested value in state by applying f."
  [keys f & args]
  (apply clojure.core/swap! *state* clojure.core/update-in keys f args))

;; --- Assertions ---
;; Delegate to the active test framework via multimethods.

(defn- active-framework []
  *framework*)

(defn set-framework! [fw]
  (if (thread-bound? #'*framework*)
    (set! *framework* fw)
    (alter-var-root #'*framework* (constantly fw))))

(defmulti should=            (fn [_ _] (active-framework)))
(defmulti assert-truthy      (fn [_form _value] (active-framework)))
(defmulti assert-falsy       (fn [_form _value] (active-framework)))
(defmulti should-be-nil      (fn [_]   (active-framework)))
(defmulti should-not-be-nil  (fn [_]   (active-framework)))
(defmulti should-include     (fn [_ _] (active-framework)))
(defmulti should-not-include (fn [_ _] (active-framework)))

(defmacro should
  "Assert that form is truthy. Captures the expression in the failure message."
  [form]
  `(let [v# ~form]
     (assert-truthy '~form v#)))

(defmacro should-not
  "Assert that form is falsy. Captures the expression in the failure message."
  [form]
  `(let [v# ~form]
     (assert-falsy '~form v#)))

(defmethod should= :default [expected actual]
  (when (not= expected actual)
    (throw (AssertionError. (str "Expected: " (pr-str expected) "\n     got: " (pr-str actual))))))

(defmethod assert-truthy :default [form value]
  (when-not value
    (throw (AssertionError. (str "Expected truthy: " (pr-str form)
                                 "\n     got: " (pr-str value))))))

(defmethod assert-falsy :default [form value]
  (when value
    (throw (AssertionError. (str "Expected falsy: " (pr-str form)
                                 "\n     got: " (pr-str value))))))

(defmethod should-be-nil :default [value]
  (when (some? value)
    (throw (AssertionError. (str "Expected nil but was: " (pr-str value))))))

(defmethod should-not-be-nil :default [value]
  (when (nil? value)
    (throw (AssertionError. "Expected not nil but was: nil"))))

(defn- contains-expected?
  "True when actual includes expected: substring for strings, membership for colls/sets,
   key presence for maps."
  [expected actual]
  (cond
    (string? actual) (str/includes? actual (str expected))
    (map? actual) (contains? actual expected)
    (or (coll? actual) (seq? actual)) (boolean (some #(= expected %) (seq actual)))
    :else
    (throw (IllegalArgumentException.
             (str "should-include does not support actual of type "
                  (if (nil? actual) "nil" (.getName (class actual))))))))

(defmethod should-include :default [expected actual]
  (when-not (contains-expected? expected actual)
    (throw (AssertionError.
             (str "Expected to include: " (pr-str expected)
                  "\n      actual value: " (pr-str actual))))))

(defmethod should-not-include :default [expected actual]
  (when (contains-expected? expected actual)
    (throw (AssertionError.
             (str "Expected not to include: " (pr-str expected)
                  "\n         actual value: " (pr-str actual))))))

;; --- Failure provenance ---
;; Generated specs wrap each step in with-step*; helpers can use each-row /
;; should-table= so failures name the Gherkin step and table cell.

(def ^:dynamic *step-context* nil)
(def ^:dynamic *table-context* nil)

(defn- format-step-context
  [{:keys [label source line]}]
  (when label
    (str label
         (when (or source line)
           (str "\n  at " source (when line (str ":" line)))))))

(defn- format-table-context
  [{:keys [row-index col-header col-index row-line]}]
  (when row-index
    (str "table cell [row " row-index
         (cond
           col-header (str ", col " (pr-str col-header))
           col-index  (str ", col " col-index)
           :else "")
         "]"
         (when row-line (str "\n  at line " row-line)))))

(defn- context-prefix []
  (->> [(format-step-context *step-context*)
        (format-table-context *table-context*)]
       (remove nil?)
       (str/join "\n")))

(defn- enrich-message [msg]
  (let [prefix (context-prefix)]
    (if (str/blank? prefix)
      msg
      (str prefix "\n" msg))))

(defn- enrich-throwable
  "Return a new throwable of the same class with context prefixed on the message.
   Cause chain is preserved via initCause when supported."
  [^Throwable t]
  (let [prefix (context-prefix)]
    (if (str/blank? prefix)
      t
      (let [msg (str prefix "\n" (.getMessage t))
            enriched (try
                       (.newInstance
                         (.getConstructor (class t) (into-array Class [String]))
                         (into-array Object [msg]))
                       (catch Exception _
                         (AssertionError. msg)))]
        (try (.initCause ^Throwable enriched t) (catch Exception _))
        enriched))))

(defn with-step*
  "Run f under step provenance. Assertion failures (and other throwables)
   are rethrown with the step label and feature location prefixed."
  [label source line f]
  (binding [*step-context* {:label label :source source :line line}]
    (try
      (f)
      (catch Throwable t
        (throw (enrich-throwable t))))))

(defmacro with-step
  "Macro form of with-step* for hand-written specs."
  [label source line & body]
  `(with-step* ~label ~source ~line (fn [] ~@body)))

(defn- row->map [headers row]
  (zipmap headers row))

(defn each-row
  "Invoke f once per data row. f receives a map of header->cell-value.
   Binds *table-context* so nested assertions name the row and feature line."
  [{:keys [headers rows row-lines]} f]
  (doseq [[idx row] (map-indexed vector rows)]
    (let [row-index (inc idx)
          row-line (when row-lines (nth row-lines idx nil))
          row-map (row->map headers row)]
      (binding [*table-context* {:row-index row-index :row-line row-line}]
        (try
          (f row-map)
          (catch Throwable t
            (throw (enrich-throwable t))))))))

(defn should-table=
  "Assert two Gherkin-style tables are equal cell-by-cell.
   On mismatch, throws with row index, column header, and optional row line.
   Actual may be a full table map or a sequence of row vectors."
  [expected actual]
  (let [exp-headers (:headers expected)
        exp-rows (:rows expected)
        act-headers (if (map? actual) (:headers actual) exp-headers)
        act-rows (if (map? actual) (:rows actual) (vec actual))
        row-lines (:row-lines expected)]
    (when (not= exp-headers act-headers)
      (throw (AssertionError.
               (enrich-message
                 (str "Table headers differ\nExpected: " (pr-str exp-headers)
                      "\n     got: " (pr-str act-headers))))))
    (when (not= (count exp-rows) (count act-rows))
      (throw (AssertionError.
               (enrich-message
                 (str "Table row count differs\nExpected: " (count exp-rows)
                      "\n     got: " (count act-rows))))))
    (doseq [r-idx (range (count exp-rows))]
      (let [exp-row (nth exp-rows r-idx)
            act-row (nth act-rows r-idx)
            row-line (when row-lines (nth row-lines r-idx nil))]
        (doseq [c-idx (range (count exp-headers))]
          (let [header (nth exp-headers c-idx)
                exp-cell (nth exp-row c-idx nil)
                act-cell (nth act-row c-idx nil)]
            (when (not= exp-cell act-cell)
              (let [msg (str "table cell [row " (inc r-idx)
                             ", col " (pr-str header) "]"
                             (when row-line (str "\n  at line " row-line))
                             "\nExpected: " (pr-str exp-cell)
                             "\n     got: " (pr-str act-cell))
                    prefix (context-prefix)
                    full (if (str/blank? prefix) msg (str prefix "\n" msg))]
                (throw (AssertionError. full))))))))))

;; --- Step registry ---
;; Each namespace that uses defgiven/defwhen/defthen accumulates steps here,
;; keyed by namespace symbol.

(defonce ^:private registry (atom {}))

;; --- Helper imports ---
;; Step namespaces declare which helper modules they depend on via (helper! ...).
;; Generator collects these for namespaces in scope and emits language-appropriate
;; imports/requires in the generated spec preamble.

(defonce ^:private helper-imports (atom {}))

(defn register-helper-import!
  "Register a helper module dependency for a step namespace.
   `module` is opaque to gherclj.core — its shape is interpreted by the
   active framework adapter (e.g. a symbol for Clojure, a path string for Ruby)."
  [ns-sym module]
  (clojure.core/swap! helper-imports clojure.core/update ns-sym (fnil conj []) module)
  nil)

(defn helper-imports-in-ns
  "Return helper imports declared by the given step namespace."
  [ns-sym]
  (clojure.core/get @helper-imports ns-sym []))

(defmacro helper!
  "Declare that this step namespace uses helpers from the given module.
   The active framework adapter decides how to translate this into the
   generated spec's import statement."
  [module]
  `(register-helper-import! '~(ns-name *ns*) (quote ~module)))

(defn- helper-ref-name
  "Extract the bare name part from a helper-ref. Symbols have their Clojure
   namespace stripped via clojure.core/name; strings pass through verbatim
   so dot-style identifiers like \"subject.door-state\" or \"Helpers.do_swap\"
   keep their receiver. Other values are stringified."
  [helper-ref]
  (cond
    (symbol? helper-ref) (name helper-ref)
    (string? helper-ref) helper-ref
    :else                (str helper-ref)))

(defn register-step!
  "Register a step definition. Called by the defgiven/defwhen/defthen macros.
   `renderer` is a function that, given the matched step args (and optional
   table/doc-string appended), returns the form (or string) to inline into
   the generated spec."
  [ns-sym step-type helper-ref template-or-regex compiled {:keys [doc file line]} renderer]
  (let [entry (merge {:name (helper-ref-name helper-ref)
                      :helper-ref helper-ref
                      :type step-type
                      :ns ns-sym
                      :doc doc
                      :file file
                      :line line
                      :renderer renderer}
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

(defn classify-all
  "Match step text against collected steps. Returns every matching step
   entry with :args populated. Never throws. Matching is type-blind:
   the Gherkin keyword (Given/When/Then) is narrative, not part of
   step-definition identity, in line with Cucumber semantics."
  [steps text]
  (keep (fn [{:keys [regex bindings] :as step}]
          (when-let [match (re-matches regex text)]
            (let [groups (if (string? match) [] (vec (rest match)))
                  args (if bindings
                         (mapv (fn [group {:keys [coerce]}] (coerce group))
                               groups bindings)
                         groups)]
              (assoc step :args args))))
        steps))

(defn classify-step
  "Match step text against collected steps. Returns the matching step
   entry with :args populated, or nil if no match.
   Throws if multiple steps match (ambiguous)."
  [steps text]
  (let [matches (vec (classify-all steps text))]
    (when (> (count matches) 1)
      (let [names (mapv :name matches)]
        (throw (RuntimeException.
                 (str "ambiguous step match: \"" text "\" matches: "
                      (str/join ", " names))))))
    (first matches)))

;; --- Macros ---

(defmacro defstep*
  "Register a step. Body of the generated spec is a single helper invocation
   built from the step's matched args plus optional table/doc-string."
  [step-type template helper-ref docstring]
  `(register-step!
     '~(ns-name *ns*) ~step-type '~helper-ref
     ~template
     ~(when (string? template) `(template/compile-template ~template))
     {:file ~*file* :line ~(-> &form meta :line) :doc ~docstring}
     (fn [& args#]
       (clojure.core/cons '~helper-ref args#))))

(defmacro defgiven
  "Define a Given step. Step bodies are constrained to a single helper reference;
   the generated spec inlines `(helper-ref param1 param2 ...)` from the matched args.

   (defgiven \"a user {name:string}\" myapp.helpers/create-user!)
   (defgiven \"a user {name:string}\" myapp.helpers/create-user! \"docstring\")"
  ([template helper-ref] `(defgiven ~template ~helper-ref nil))
  ([template helper-ref docstring]
   `(defstep* :given ~template ~helper-ref ~docstring)))

(defmacro defwhen
  "Define a When step. See `defgiven` for the constrained signature."
  ([template helper-ref] `(defwhen ~template ~helper-ref nil))
  ([template helper-ref docstring]
   `(defstep* :when ~template ~helper-ref ~docstring)))

(defmacro defthen
  "Define a Then step. See `defgiven` for the constrained signature."
  ([template helper-ref] `(defthen ~template ~helper-ref nil))
  ([template helper-ref docstring]
   `(defstep* :then ~template ~helper-ref ~docstring)))
