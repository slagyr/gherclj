(ns gherclj.frameworks.java.junit5
  "JUnit 5 (Jupiter) adapter. Generates one Java test class per feature with
   `@Test` methods per scenario. Generated tests live in package `airlock`,
   matching the directory layout the pipeline writes into
   (`<output-dir>/airlock/<ClassName>Test.java`)."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [gherclj.framework :as fw]
            [gherclj.core :as core]))

;; --- Step namespace setup registry ---

(defonce scenario-setup-registry (atom {}))

(defmacro scenario-setup!
  "Declare a Java statement to inject at the top of every `@Test` method body.
   Use for fixtures like `SpaceAirlock airlock = new SpaceAirlock();`."
  [line]
  `(swap! scenario-setup-registry update '~(ns-name *ns*) (fnil conj []) ~line))

;; --- Translation helpers ---

(defn java-string
  "Render a Clojure string as a double-quoted Java string with proper escapes."
  [s]
  (str "\"" (str/escape (str s) {\\ "\\\\", \" "\\\"", \newline "\\n", \tab "\\t"}) "\""))

(defn java-literal
  "Coerce a Clojure value into a Java literal expression."
  [value]
  (cond
    (string? value)  (java-string value)
    (keyword? value) (java-string (name value))
    (true? value)    "true"
    (false? value)   "false"
    (nil? value)     "null"
    (integer? value) (str value)
    (float? value)   (str value)
    :else            (java-string (str value))))

(defn- camel
  "Convert kebab-case to camelCase. The first segment stays lowercase;
   subsequent segments are capitalized. Already-camel segments are idempotent."
  [s]
  (let [segments (str/split s #"-")]
    (apply str
           (cons (first segments)
                 (map (fn [seg]
                        (if (seq seg)
                          (str (str/upper-case (subs seg 0 1)) (subs seg 1))
                          ""))
                      (rest segments))))))

(defn- pascal
  "Convert kebab-case to PascalCase."
  [s]
  (->> (str/split s #"-")
       (map #(if (seq %) (str (str/upper-case (subs % 0 1)) (subs % 1)) ""))
       (apply str)))

(defn java-method
  "Translate a helper-ref name into a Java identifier expression.
   Single-segment names are camelCased: \"crew-member-inside\" → \"crewMemberInside\".
   Dot-separated names treat the first segment as a receiver/variable
   (kept as-is) and subsequent segments as method names:
   \"airlock.crew-member-inside\" → \"airlock.crewMemberInside\"."
  [name]
  (let [segments (str/split name #"\.")]
    (if (= 1 (count segments))
      (camel (first segments))
      (str/join "." (cons (first segments) (map camel (rest segments)))))))

(defn- java-import-line
  "Translate a helper-import value into a Java `import` line. The value is
   used verbatim as the import target (e.g. \"com.example.foo.Bar\" →
   \"import com.example.foo.Bar;\")."
  [import-val]
  (str "import " (str import-val) ";"))

(defn generate-step-call
  "Generate a Java method/function call from a classified step. Java requires
   parentheses on every call, including no-arg ones."
  [{:keys [name args table doc-string]}]
  (let [all-args (cond-> (vec args)
                   table      (conj table)
                   doc-string (conj doc-string))
        args-str (str/join ", " (map java-literal all-args))]
    (str (java-method name) "(" args-str ");")))

(defn- source->class-name
  "Convert a source path to a JUnit class name.
   \"airlock_exit.feature\" → \"AirlockExitTest\"."
  [source]
  (let [bare (-> source
                 (str/split #"/")
                 last
                 (str/replace #"\.feature$" ""))
        parts (str/split bare #"[_-]")]
    (str (apply str
                (map #(if (seq %) (str (str/upper-case (subs % 0 1)) (subs % 1)) "")
                     parts))
         "Test")))

(defn- scenario->method-name
  "Convert a scenario title into a camelCased Java method identifier.
   Strips non-alphanumeric chars; leading digits get a `t` prefix to keep
   the identifier valid."
  [title]
  (let [tokens (->> (str/split title #"[^A-Za-z0-9]+")
                    (remove str/blank?))
        [head & rest-tokens] tokens
        head' (when head (str/lower-case head))
        rest' (map #(str (str/upper-case (subs % 0 1))
                         (str/lower-case (subs % 1)))
                   rest-tokens)
        ident (apply str (cons head' rest'))]
    (cond
      (str/blank? ident)              "scenario"
      (Character/isDigit (.charAt ident 0)) (str "t" ident)
      :else ident)))

;; --- Framework multimethods ---

(defmethod fw/render-step :java/junit5 [_config step]
  (generate-step-call step))

(defmethod fw/generate-preamble :java/junit5
  [_config source used-nses]
  (let [pkg     "airlock"
        imports (->> used-nses
                     (mapcat core/helper-imports-in-ns)
                     distinct
                     sort
                     (map java-import-line))
        class-name (source->class-name source)]
    (str "// generated from " source "\n"
         "package " pkg ";\n\n"
         "import org.junit.jupiter.api.DisplayName;\n"
         "import org.junit.jupiter.api.Test;\n"
         (when (seq imports)
           (str (str/join "\n" imports) "\n"))
         "\n"
         "public class " class-name " {\n")))

(defmethod fw/wrap-feature :java/junit5
  [_config _feature-name scenario-blocks]
  (str "\n" scenario-blocks "\n}\n"))

(defn- collect-scenario-setup [used-nses]
  (->> used-nses
       (mapcat #(get @scenario-setup-registry % []))
       distinct))

(defmethod fw/wrap-scenario :java/junit5
  [config scenario background]
  (let [used-nses  (or (:_used-nses config) #{})
        setup      (collect-scenario-setup used-nses)
        bg-calls   (:rendered-steps background)
        step-calls (:rendered-steps scenario)
        title      (:scenario scenario)
        method-name (scenario->method-name title)
        body-lines (concat (map #(str "        " %) setup)
                           (map #(str "        " %) (concat bg-calls step-calls)))
        body       (str/join "\n" body-lines)]
    (str "    @Test\n"
         "    @DisplayName(" (java-string title) ")\n"
         "    public void " method-name "() {\n"
         body "\n"
         "    }")))

(defmethod fw/wrap-pending :java/junit5
  [_config scenario _background]
  (let [title       (:scenario scenario)
        method-name (scenario->method-name title)]
    (str "    @Test\n"
         "    @DisplayName(" (java-string title) ")\n"
         "    public void " method-name "() {\n"
         "        org.junit.jupiter.api.Assumptions.assumeTrue(false, \"not yet implemented\");\n"
         "    }")))

(defmethod fw/run-specs :java/junit5
  [config]
  (let [opts (or (:framework-opts config) [])
        cmd  (concat ["mvn" "test"] opts)
        {:keys [exit out err]} (apply shell/sh cmd)]
    (when (seq out)
      (print out))
    (when (seq err)
      (binding [*out* *err*]
        (print err)))
    (when-not (zero? exit)
      (throw (ex-info "mvn test failed" {:exit exit :stderr err})))
    0))
