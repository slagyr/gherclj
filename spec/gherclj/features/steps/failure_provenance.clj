(ns gherclj.features.steps.failure-provenance
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen helper!]]
            [gherclj.generator :as gen]
            [gherclj.frameworks.clojure.speclj]
            [clojure.string :as str]))

(helper! gherclj.features.steps.failure-provenance)

(defn- strip-quotes [s]
  (str/replace (str s) #"^\"|\"$" ""))

(defn source-parsed-ir! [source]
  (g/assoc! :ir-source (strip-quotes source))
  (when-let [ir (g/get :parsed-ir)]
    (g/assoc! :parsed-ir (assoc ir :source (strip-quotes source)))))

(defn generate-speclj-from-parsed! [ns-name]
  (let [ns-sym (symbol (strip-quotes ns-name))
        source (or (g/get :ir-source) "unknown.feature")
        ir (assoc (g/get :parsed-ir) :source source)]
    (require ns-sym)
    (g/assoc! :generated-output
              (gen/generate-spec {:step-namespaces [ns-sym]
                                  :framework :clojure/speclj}
                                 ir))))

(defn- table-from-gherkin [table]
  {:headers (:headers table)
   :rows (:rows table)})

(defn labeled-step-with-table! [label line source table]
  (let [label (strip-quotes label)
        source (strip-quotes source)
        [kw & text-parts] (str/split label #" " 2)
        text (or (first text-parts) "")
        type (keyword (str/lower-case kw))]
    (g/assoc! :provenance-step
              {:type type
               :text text
               :line line
               :table (table-from-gherkin table)}
              :ir-source source)))

(defn labeled-step! [label line source]
  (let [label (strip-quotes label)
        source (strip-quotes source)
        [kw & text-parts] (str/split label #" " 2)
        text (or (first text-parts) "")
        type (keyword (str/lower-case kw))]
    (g/assoc! :provenance-step
              {:type type
               :text text
               :line line}
              :ir-source source)))

(defn table-header-and-rows-at! [header-line row-a row-b]
  (g/update! :provenance-step
             (fn [step]
               (assoc-in step [:table]
                         (merge (:table step)
                                {:header-line header-line
                                 :row-lines [row-a row-b]})))))

(defn format-provenance-comments! [framework]
  (let [fw (keyword (str/replace (str framework) #"^:" ""))
        step (g/get :provenance-step)
        source (or (g/get :ir-source) "unknown.feature")
        config {:framework fw :_source source}
        lines (gen/provenance-comment-lines config step)]
    (g/assoc! :provenance-comments (str/join "\n" lines))))

(defn provenance-comments-should-contain [text]
  (let [text (strip-quotes text)
        comments (or (g/get :provenance-comments) "")]
    (g/should-include text comments)))

(defn step-fails-with! [label source line doc-string]
  (let [err (try
              (g/with-step* (strip-quotes label) (strip-quotes source) line
                (fn [] (throw (AssertionError. doc-string))))
              nil
              (catch Throwable t t))]
    (g/assoc! :failure-message (some-> err .getMessage))))

(defn should-include-fails! [expected actual]
  (let [err (try
              (binding [g/*framework* nil]
                (g/should-include (strip-quotes expected) (strip-quotes actual)))
              nil
              (catch Throwable t t))]
    (g/assoc! :failure-message (some-> err .getMessage))))

(defn expected-table-with-row-lines! [line-a line-b table]
  (g/assoc! :expected-table (merge (table-from-gherkin table)
                                   {:row-lines [line-a line-b]})))

(defn actual-table! [table]
  (g/assoc! :actual-table (table-from-gherkin table)))

(defn table-with-row-lines! [line-a line-b table]
  (g/assoc! :working-table (merge (table-from-gherkin table)
                                  {:row-lines [line-a line-b]})))

(defn compare-tables! []
  (let [err (try
              (binding [g/*framework* nil]
                (g/should-table= (g/get :expected-table) (g/get :actual-table)))
              nil
              (catch Throwable t t))]
    (g/assoc! :failure-message (some-> err .getMessage))))

(defn compare-tables-inside-step! [label source line]
  (let [err (try
              (g/with-step* (strip-quotes label) (strip-quotes source) line
                (fn []
                  (binding [g/*framework* nil]
                    (g/should-table= (g/get :expected-table) (g/get :actual-table)))))
              nil
              (catch Throwable t t))]
    (g/assoc! :failure-message (some-> err .getMessage))))

(defn each-row-asserts-role! [expected-role]
  (let [expected-role (strip-quotes expected-role)
        err (try
              (binding [g/*framework* nil]
                (g/each-row (g/get :working-table)
                  (fn [row]
                    (g/should= expected-role (row "role")))))
              nil
              (catch Throwable t t))]
    (g/assoc! :failure-message (some-> err .getMessage))))

(defn failure-message-should-include [text]
  (let [text (strip-quotes text)
        msg (or (g/get :failure-message) "")]
    (g/should-not-be-nil (g/get :failure-message))
    (g/should-include text msg)))

(defn generated-wrappers-reference-lines! [a b c]
  (let [out (or (g/get :generated-output) "")]
    (doseq [line [a b c]]
      (g/should (re-find (re-pattern (str "\"auth\\.feature\" " line "\\b")) out)))))

;; --- Routing ---

(defgiven "the parsed IR is sourced as {source:string}" failure-provenance/source-parsed-ir!
  "Sets :source on :parsed-ir so generated provenance comments include the path.")

(defwhen "generating a speclj spec from the parsed IR using step namespace {ns-name:string}"
  failure-provenance/generate-speclj-from-parsed!
  "Loads ns-name, generates :clojure/speclj output into :generated-output.")

(defgiven "a table step labeled {label:string} at line {line:int} of {source:string}:"
  failure-provenance/labeled-step-with-table!)

(defgiven "a plain step labeled {label:string} at line {line:int} of {source:string}"
  failure-provenance/labeled-step!)

(defgiven "the table header is at line {header-line:int} and data rows at lines {row-a:int} and {row-b:int}"
  failure-provenance/table-header-and-rows-at!)

(defwhen "formatting provenance comments for framework {framework}"
  failure-provenance/format-provenance-comments!)

(defthen "the provenance comments should contain {text:string}"
  failure-provenance/provenance-comments-should-contain)

(defwhen "a step {label:string} at {source:string} line {line:int} fails with:"
  failure-provenance/step-fails-with!)

(defwhen "should-include fails looking for {expected:string} in {actual:string}"
  failure-provenance/should-include-fails!)

(defgiven "an expected table with row lines {line-a:int} and {line-b:int}:"
  failure-provenance/expected-table-with-row-lines!)

(defgiven "an actual table:" failure-provenance/actual-table!)

(defgiven "a table with row lines {line-a:int} and {line-b:int}:"
  failure-provenance/table-with-row-lines!)

(defwhen "comparing the tables with should-table=" failure-provenance/compare-tables!)

(defwhen "comparing the tables with should-table= inside step {label:string} at {source:string} line {line:int}"
  failure-provenance/compare-tables-inside-step!)

(defwhen "each-row asserts role {expected-role:string} for every row"
  failure-provenance/each-row-asserts-role!)

(defthen "the failure message should include {text:string}"
  failure-provenance/failure-message-should-include)

(defthen "the generated with-step wrappers should reference lines {a:int}, {b:int}, and {c:int}"
  failure-provenance/generated-wrappers-reference-lines!
  "Asserts each with-step* call embeds \"auth.feature\" followed by the given line numbers.")
