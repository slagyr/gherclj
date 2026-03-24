<img src="https://raw.githubusercontent.com/slagyr/gherclj/master/gherclj.png" alt="gherclj" 
width="200" align="left">

<h1 style="border: 0">gherclj</h1>
*pronounced: /ˈɡɜːrkəl/, gur-kull*

A library to translate Gherkin Acceptance Tests into code.

<br clear="both">

## Introduction

gherclj bridges the gap between human-readable feature specifications and executable Clojure tests. It parses standard Gherkin `.feature` files and generates test files that call real Clojure functions — no string-concatenated code, no separate pattern/registry maps, no framework lock-in.

The pipeline:

```
.feature files → EDN intermediate representation → generated unit test files
```

Each stage produces a visible, inspectable artifact. If a step isn't matching, check the `.edn` IR. If the IR is right but the spec is wrong, it's a generator issue. The generated specs are readable, debuggable, and committable.

## Usage

### 1. Define steps

Steps read like `defn` with a docstring. The template string doubles as documentation and a matching pattern.

```clojure
(ns myapp.features.steps.auth
  (:require [gherclj.core :refer [defgiven defwhen defthen]]
            [myapp.features.harness :as h]))

(defgiven create-user "a user \"{name}\" with role \"{role}\""
  [name role]
  (h/create-user! name role))

(defwhen user-logs-in "the user logs in"
  []
  (h/login!))

(defthen response-status "the response status should be {status:int}"
  [status]
  (should= status (h/response-status)))
```

Template syntax:
- `"{name}"` — quoted string capture
- `{name:int}` — integer capture (coerced via `parse-long`)
- `{name:float}` — float capture (coerced via `parse-double`)
- `{name}` — word capture (`\S+`)

For edge cases, pass a raw regex instead of a template string:

```clojure
(defthen check-headers #"^the output should contain headers (.+)$"
  [headers-str]
  (let [headers (re-seq #"\"([^\"]+)\"" headers-str)]
    (doseq [h headers]
      (should (str/includes? (h/output) (second h))))))
```

Steps that accept a Gherkin table receive it as an additional argument:

```clojure
(defgiven set-projects "a scenario \"{title}\" with steps:"
  [title table]
  (let [{:keys [headers rows]} table]
    (h/setup-scenario! title headers rows)))
```

### 2. Write features

Standard [Gherkin syntax](https://cucumber.io/docs/gherkin/reference/). Nothing special.

```gherkin
Feature: Authentication

  Scenario: Admin can log in
    Given a user "alice" with role "admin"
    When the user logs in
    Then the response status should be 200

  Scenario: Guest gets 401
    Given a user "unknown" with role "guest"
    When the user logs in
    Then the response status should be 401
```

### 3. Write a harness

The harness is your project-specific test state. The library doesn't impose a structure — just provide a `reset!` function that the generated specs call before each scenario.

```clojure
(ns myapp.features.harness
  (:refer-clojure :exclude [reset!]))

(def ^:private state (atom nil))

(defn reset! []
  (clojure.core/reset! state {:user nil :response nil}))

(defn create-user! [name role]
  (swap! state assoc :user {:name name :role role}))

(defn login! []
  (let [user (:user @state)
        status (if (= "admin" (:role user)) 200 401)]
    (swap! state assoc :response {:status status})))

(defn response-status []
  (get-in @state [:response :status]))
```

### 4. Run the pipeline

```clojure
(require '[gherclj.pipeline :as pipeline])

(pipeline/run!
  {:features-dir    "features"
   :edn-dir         "features/edn"
   :output-dir      "features/generated"
   :step-namespaces ['myapp.features.steps.auth]
   :harness-ns      'myapp.features.harness
   :test-framework  :speclj})
```

This produces:
- `features/edn/auth.edn` — the parsed IR (inspectable, debuggable)
- `features/generated/auth_spec.clj` — executable spec with qualified function calls

You can also run the stages independently:

```clojure
(pipeline/parse! {:features-dir "features" :edn-dir "features/edn"})
(pipeline/generate! {:edn-dir "features/edn" :output-dir "features/generated" ...})
```

### 5. Generated output

The generated specs are clean, readable function calls:

```clojure
(ns auth-spec
  (:require [speclj.core :refer :all]
            [myapp.features.harness :as h]
            [myapp.features.steps.auth]))

(describe "Authentication"

  (context "Admin can log in"
    (it "Admin can log in"
      (h/reset!)
      (myapp.features.steps.auth/create-user "alice" "admin")
      (myapp.features.steps.auth/user-logs-in)
      (myapp.features.steps.auth/response-status 200)))

  (context "Guest gets 401"
    (it "Guest gets 401"
      (h/reset!)
      (myapp.features.steps.auth/create-user "unknown" "guest")
      (myapp.features.steps.auth/user-logs-in)
      (myapp.features.steps.auth/response-status 401))))
```

Unrecognized steps generate pending scenarios with comments showing the step text, so you can see what needs to be implemented.

### Test framework support

gherclj ships with `:speclj` and `:clojure.test` output formats. Add your own by implementing the generator multimethods:

```clojure
(defmethod gherclj.generator/generate-ns-form :my-framework [config source step-ns-syms harness-ns] ...)
(defmethod gherclj.generator/wrap-feature :my-framework [config feature-name scenario-blocks] ...)
(defmethod gherclj.generator/wrap-scenario :my-framework [config scenario background] ...)
(defmethod gherclj.generator/wrap-pending :my-framework [config scenario background] ...)
```

## Rationale

gherclj was extracted from the [braids](https://github.com/slagyr/braids) project, which had a working Gherkin pipeline with two pain points:

**Step definitions were verbose and fragmented.** Each step required entries in two separate data structures — a pattern map (regex → classifier function → IR map) and a registry map (pattern keyword → text function + code function). A single step took ~15 lines spread across two locations.

`defgiven`/`defwhen`/`defthen` collapse this to ~3 lines. The template *is* the pattern. The function *is* the implementation. No IR map, no separate text function, no registry.

**Code generation used string concatenation.** Step implementations returned strings of Clojure code with manually escaped quotes: `(str "(should= \"" expected "\" ...)")`. This was fragile and hard to read.

Since steps are now real functions, the generator just emits qualified calls: `(myapp.steps.auth/create-user "alice" "admin")`. No string escaping. The generated code is readable enough to debug directly.

**Steps weren't discoverable.** The original generator hardcoded `require` statements for every step namespace. Adding a new domain meant editing the generator.

gherclj discovers steps through a config map. Each `defgiven`/`defwhen`/`defthen` self-registers into a namespace-level registry. The pipeline collects steps from the configured namespaces at generation time.

## Development

### Prerequisites

- Clojure 1.12+
- [speclj](https://github.com/slagyr/speclj) (test dependency, loaded via `:spec` alias)

### Run unit specs

```bash
clj -M:spec
```

### Run feature specs

gherclj eats its own dogfood — its own acceptance tests are `.feature` files run through the pipeline. This regenerates and runs them in one step:

```bash
clj -M:features
```

### Project structure

```
src/gherclj/
  core.clj            - defgiven/defwhen/defthen macros, step registration
  template.clj        - template string → regex compiler
  parser.clj          - Gherkin .feature file parser
  generator.clj       - codegen engine, framework multimethods
  pipeline.clj        - config-driven orchestration (parse!, generate!, run!)
  frameworks/
    speclj.clj        - speclj output format
    clojure_test.clj  - clojure.test output format

spec/gherclj/         - unit specs
features/             - .feature files (gherclj's own acceptance tests)
features/edn/         - parsed IR
features/generated/   - generated spec files
```
