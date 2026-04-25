(ns gherclj.frameworks.go.testing
  "Go testing-package adapter. Generates *_test.go files driven by Go's
   stdlib `testing` package: `func TestX(t *testing.T)` per feature,
   `t.Run(\"scenario\", ...)` per scenario."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.core :as core]))

;; --- Step namespace setup registries ---

(defonce scenario-setup-registry (atom {}))

(defmacro scenario-setup!
  "Declare a line of Go code to inject at the top of every t.Run scenario
   closure. Use for fixtures like `subject := airlock.NewSpaceAirlock()`."
  [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

;; --- Translation helpers ---

(defn go-string
  "Render a Clojure string as a double-quoted Go string with proper escapes."
  [s]
  (str "\"" (str/escape (str s) {\\ "\\\\", \" "\\\"", \newline "\\n", \tab "\\t"}) "\""))

(defn go-literal
  "Coerce a Clojure value into a Go literal."
  [value]
  (cond
    (string? value)  (go-string value)
    (keyword? value) (go-string (name value))
    (true? value)    "true"
    (false? value)   "false"
    (nil? value)     "nil"
    (integer? value) (str value)
    (float? value)   (str value)
    :else            (go-string (str value))))

(defn- pascal
  "Convert kebab-case to PascalCase. Already-Pascal segments are idempotent."
  [s]
  (->> (str/split s #"-")
       (map #(if (seq %) (str (str/upper-case (subs % 0 1)) (subs % 1)) ""))
       (apply str)))

(defn go-method
  "Translate a helper-ref name into a Go identifier expression.
   Single-segment names are PascalCased: \"crew-member-inside\" → \"CrewMemberInside\".
   Dot-separated names treat the first segment as a receiver/variable
   (kept as-is) and subsequent segments as method names:
   \"subject.crew-member-inside\" → \"subject.CrewMemberInside\"."
  [name]
  (let [segments (str/split name #"\.")]
    (if (= 1 (count segments))
      (pascal (first segments))
      (str/join "." (cons (first segments) (map pascal (rest segments)))))))

(defn- go-import-line
  "Translate a helper-import value into a single Go import line. Strings of
   the form \"alias path\" produce aliased imports; otherwise the value is
   the bare package path."
  [import-val]
  (let [s (str import-val)
        parts (str/split s #"\s+" 2)]
    (if (= 2 (count parts))
      (str "    " (first parts) " " (go-string (second parts)))
      (str "    " (go-string s)))))

(defn generate-step-call
  "Generate a Go function/method call from a classified step. Go requires
   parentheses even on no-arg calls (otherwise the expression is a function
   value, not an invocation)."
  [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table      (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map go-literal all-args))]
    (str (go-method name) "(" args-str ")")))

;; --- Framework multimethods ---

(defmethod fw/render-step :go/testing [_config step]
  (generate-step-call step))

(defn- source->test-name
  "Convert source (e.g. \"airlock_exit.feature\") into a Go Test function name.
   \"airlock_exit.feature\" → \"TestAirlockExit\"."
  [source]
  (let [bare (-> source
                 (str/split #"/")
                 last
                 (str/replace #"\.feature$" ""))]
    (str "Test" (pascal (str/replace bare #"_" "-")))))

(defmethod fw/generate-preamble :go/testing
  [config source used-nses]
  (let [pkg     (or (:go-package config) "generated")
        imports (->> used-nses
                     (mapcat core/helper-imports-in-ns)
                     distinct)
        import-lines (concat ["    \"testing\""]
                             (map go-import-line imports))]
    (str "// generated from " source "\n"
         "package " pkg "\n\n"
         "import (\n"
         (str/join "\n" import-lines) "\n"
         ")\n")))

(defmethod fw/wrap-feature :go/testing
  [_config feature-name scenario-blocks]
  (let [test-name (str "Test" (pascal (str/replace feature-name #"\s+" "-")))]
    (str "\nfunc " test-name "(t *testing.T) {\n"
         scenario-blocks "\n"
         "}\n")))

(defn- collect-scenario-setup [used-nses]
  (->> used-nses
       (mapcat #(get @scenario-setup-registry % []))
       distinct))

(defmethod fw/wrap-scenario :go/testing
  [config scenario background]
  (let [used-nses  (or (:_used-nses config) #{})
        setup      (collect-scenario-setup used-nses)
        bg-calls   (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        body-lines (concat (map #(str "        " %) setup)
                           (map #(str "        " %) (concat bg-calls step-calls)))
        body       (str/join "\n" body-lines)]
    (str "    t.Run(" (go-string (:scenario scenario)) ", func(t *testing.T) {\n"
         body "\n"
         "    })")))

(defmethod fw/wrap-pending :go/testing
  [_config scenario _background]
  (str "    t.Run(" (go-string (:scenario scenario)) ", func(t *testing.T) {\n"
       "        t.Skip(\"not yet implemented\")\n"
       "    })"))

(defmethod fw/run-specs :go/testing
  [config]
  (let [output-dir (or (:output-dir config) "target/gherclj/generated")
        opts       (or (:framework-opts config) [])
        cmd        (concat ["go" "test"] opts [(str "./" output-dir "/...")])
        {:keys [exit out err]} (apply shell/sh cmd)]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))
    (when-not (zero? exit)
      (throw (ex-info "go test failed" {:exit exit :stderr err})))
    0))
