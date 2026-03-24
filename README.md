<img align="left" width="200" src="https://raw.githubusercontent.com/slagyr/gherclj/master/gherclj.png" alt="gherclj" style="margin-right: 20px; margin-bottom: 10px;">

### gherclj

_pronounced: /ˈɡɜːrkəl/, gur-kull_

Translates Gherkin acceptance tests into code.

<br clear="left">

## Introduction

gherclj bridges the gap between human-readable feature specifications and executable Clojure tests. It parses standard [Gherkin](https://cucumber.io/docs/gherkin/reference/) `.feature` files and generates test files that call real Clojure functions — no string-concatenated code, no separate pattern/registry maps, no framework lock-in.

The pipeline:

```
.feature files → EDN intermediate representation → generated unit test files
```

Each stage produces a visible, inspectable artifact. If a step isn't matching, check the `.edn` IR. If the IR is right but the spec is wrong, it's a generator issue. The generated specs are readable, debuggable, and committable.

## Quick Start

### 1. Add gherclj to your project

```clojure
;; deps.edn
{:deps {gherclj/gherclj {:mvn/version "..."}}}
```

### 2. Write features

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

### 3. Define steps

Steps read like `defn` with a docstring. The template string doubles as documentation and a matching pattern. State is managed through `gherclj.core`, aliased as `g`:

```clojure
(ns myapp.features.steps.auth
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]
            [speclj.core :refer :all]))

(defgiven create-user "a user \"{name}\" with role \"{role}\""
  [name role]
  (g/assoc! :user {:name name :role role}))

(defwhen user-logs-in "the user logs in"
  []
  (let [user (g/get :user)
        status (if (= "admin" (:role user)) 200 401)]
    (g/assoc! :response {:status status})))

(defthen response-status "the response status should be {status:int}"
  [status]
  (should= status (g/get-in [:response :status])))
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
    (doseq [[_ h] headers]
      (should-contain h (g/get :output)))))
```

Steps that accept a Gherkin table receive it as an additional argument:

```clojure
(defgiven setup-users "the following users:"
  [table]
  (let [{:keys [headers rows]} table]
    (g/assoc! :users (mapv #(zipmap headers %) rows))))
```

### 4. Configure and run

Create a `gherclj.edn` at your project root:

```clojure
{:features-dir    "features"
 :step-namespaces [myapp.features.steps.auth]
 :test-framework  :speclj}
```

Run via CLI:

```bash
clj -M -m gherclj.main
```

Or with Babashka:

```bash
bb -m gherclj.main
```

This produces:
- `target/gherclj/edn/auth.edn` — the parsed IR (inspectable, debuggable)
- `target/gherclj/generated/auth_spec.clj` — executable spec with qualified function calls

### 5. Generated output

The generated specs are clean, readable function calls:

```clojure
(ns auth-spec
  (:require [speclj.core :refer :all]
            [gherclj.core :as g]
            [myapp.features.steps.auth :as auth]))

(describe "Authentication"

  (context "Admin can log in"
    (it "Admin can log in"
      (g/reset!)
      (auth/create-user "alice" "admin")
      (auth/user-logs-in)
      (auth/response-status 200)))

  (context "Guest gets 401"
    (it "Guest gets 401"
      (g/reset!)
      (auth/create-user "unknown" "guest")
      (auth/user-logs-in)
      (auth/response-status 401))))
```

Unrecognized steps generate pending scenarios with comments showing the step text, so you can see what needs to be implemented.

## Configuration

gherclj reads configuration from `gherclj.edn` (project root or classpath), with CLI flags as overrides.

| Key | Default | Description |
|-----|---------|-------------|
| `:features-dir` | `"features"` | Directory containing `.feature` files |
| `:edn-dir` | `"target/gherclj/edn"` | Directory for parsed EDN IR files |
| `:output-dir` | `"target/gherclj/generated"` | Directory for generated spec files |
| `:step-namespaces` | `[]` | Namespace symbols or glob pattern strings |
| `:test-framework` | `:speclj` | `:speclj` or `:clojure.test` |
| `:verbose` | `false` | Print progress to stdout |

Step namespaces support glob patterns for discovery:

```clojure
{:step-namespaces [myapp.steps.manual           ;; concrete symbol
                   "myapp.features.steps.*"      ;; glob pattern
                   "myapp.*-steps"]}             ;; glob in the middle
```

## State Management

gherclj provides a global state atom that is automatically reset before each scenario. Steps interact with state through `gherclj.core`:

```clojure
(g/assoc! :key val)            ;; set a key
(g/assoc-in! [:a :b] val)      ;; set nested
(g/get :key)                   ;; read a key (or full map with no args)
(g/get-in [:a :b])             ;; read nested
(g/update! :key f & args)      ;; update a key
(g/update-in! [:a :b] f)       ;; update nested
(g/dissoc! :key)               ;; remove a key
(g/swap! f & args)             ;; arbitrary transformation
(g/reset!)                     ;; clear all state
```

## Test Framework Support

gherclj ships with `:speclj` and `:clojure.test` output formats. Add your own by implementing the generator multimethods:

```clojure
(defmethod gherclj.generator/generate-ns-form :my-framework [config source step-ns-syms] ...)
(defmethod gherclj.generator/wrap-feature :my-framework [config feature-name scenario-blocks] ...)
(defmethod gherclj.generator/wrap-scenario :my-framework [config scenario background] ...)
(defmethod gherclj.generator/wrap-pending :my-framework [config scenario background] ...)
```

## Rationale

gherclj was extracted from the [braids](https://github.com/slagyr/braids) project, which had a working Gherkin pipeline with two pain points:

**Step definitions were verbose and fragmented.** Each step required entries in two separate data structures — a pattern map and a registry map. A single step took ~15 lines spread across two locations.

`defgiven`/`defwhen`/`defthen` collapse this to ~3 lines. The template *is* the pattern. The function *is* the implementation.

**Code generation used string concatenation.** Step implementations returned strings of Clojure code with manually escaped quotes. This was fragile and hard to read.

Since steps are now real functions, the generator emits aliased calls: `(auth/create-user "alice" "admin")`. The generated code is readable enough to debug directly.

## Development

### Prerequisites

- [Babashka](https://github.com/babashka/babashka)
- Clojure 1.12+

### Dev commands

```bash
bb parse      # Parse .feature files → EDN IR
bb generate   # Generate spec files from EDN
bb spec       # Run unit specs
bb features   # Run feature specs (parse + generate + execute)
bb test       # Run all tests
bb clean      # Remove generated files
```

### Project structure

```
src/gherclj/
  core.clj            - defgiven/defwhen/defthen macros, state management
  template.clj        - template string → regex compiler
  parser.clj          - Gherkin .feature file parser
  generator.clj       - codegen engine, framework multimethods
  pipeline.clj        - config-driven orchestration (parse!, generate!, run!)
  main.clj            - CLI entry point
  frameworks/
    speclj.clj        - speclj output format
    clojure_test.clj  - clojure.test output format

spec/gherclj/         - unit specs
features/             - .feature files (gherclj's own acceptance tests)
```
