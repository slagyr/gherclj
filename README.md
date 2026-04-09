<img align="left" width="200" src="https://raw.githubusercontent.com/slagyr/gherclj/master/gherclj.png" alt="gherclj" style="margin-right: 20px; margin-bottom: 10px;">

### gherclj

_pronounced: /ˈɡɜːrkəl/, gur-kull_

Translates Gherkin acceptance tests into code.

<br clear="left">

## Introduction

gherclj bridges the gap between human-readable feature specifications and executable tests. It parses standard [Gherkin](https://cucumber.io/docs/gherkin/reference/) `.feature` files and generates test files that call real code.

The pipeline:

```
.feature files → EDN intermediate representation → generated unit test files
```

Each stage produces a visible, inspectable artifact. If a step isn't matching, check the `.edn` IR. If the IR is right but the spec is wrong, it's a generator issue. The generated specs are readable, debuggable, and committable.

## Quick Start

### 1. Add gherclj to your project

```clojure
;; deps.edn or bb.edn
{:deps {io.github.slagyr/gherclj {:git/tag "v0.6.1" :git/sha "PENDING"}}}
```

### 2. Write features

Create a `features/` directory at your project root and add `.feature` files using standard [Gherkin syntax](https://cucumber.io/docs/gherkin/reference/):

```
myapp/
  features/
    authentication.feature
    checkout.feature
  src/
    ...
```

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

The `features/` directory is the default; configure a different location with `:features-dir` in `gherclj.edn`.

### 3. Define steps

Steps read like `defn` with a docstring. The template string doubles as documentation and a matching pattern. State is managed through `gherclj.core`, aliased as `g`:

```clojure
(ns myapp.features.steps.auth
  (:require [gherclj.core :as g :refer [defgiven defwhen defthen]]))

(defgiven create-user "a user {name:string} with role {role:string}"
  [name role]
  (g/assoc! :user {:name name :role role}))

(defwhen user-logs-in "the user logs in"
  []
  (let [user (g/get :user)
        status (if (= "admin" (:role user)) 200 401)]
    (g/assoc! :response {:status status})))

(defthen response-status "the response status should be {status:int}"
  [status]
  (g/should= status (g/get-in [:response :status])))
```

Template syntax:
- `{name:string}` — greedy string capture (bounded by surrounding literal text)
- `{name:int}` — integer capture (coerced via `parse-long`)
- `{name:float}` — float capture (coerced via `parse-double`)
- `{name}` — word capture (`\S+`)

For edge cases, pass a raw regex instead of a template string:

```clojure
(defthen check-headers #"^the output should contain headers (.+)$"
  [headers-str]
  (let [headers (re-seq #"\"([^\"]+)\"" headers-str)]
    (doseq [[_ h] headers]
      (g/should (clojure.string/includes? (g/get :output) h)))))
```

Steps that accept a Gherkin table receive it as an additional argument:

```clojure
(defgiven setup-users "the following users:"
  [table]
  (let [{:keys [headers rows]} table]
    (g/assoc! :users (mapv #(zipmap headers %) rows))))
```

### 4. Configure and run

There are several ways to configure and run the pipeline.

**Option A: Task/alias with CLI flags (recommended)**

```clojure
;; bb.edn
{:deps {io.github.slagyr/gherclj {:git/tag "v0.6.0" :git/sha "6831fb5"}}
 :tasks
 {features {:doc "Run feature specs"
            :requires ([gherclj.main :as main])
            :task (main/-main "-s" "myapp.features.steps.auth"
                              "-s" "myapp.features.steps.cart"
                              "-t" "speclj")}}}

;; deps.edn
{:deps {io.github.slagyr/gherclj {:git/tag "v0.6.0" :git/sha "6831fb5"}}
 :aliases
 {:features {:main-opts ["-m" "gherclj.main"
                         "-s" "myapp.features.steps.auth"
                         "-s" "myapp.features.steps.cart"
                         "-t" "speclj"]}}}
```

```bash
bb features
# or
clj -M:features
```

**Option B: Config file with CLI runner**

Create a `gherclj.edn` at your project root (or on the classpath):

```clojure
{:step-namespaces [myapp.features.steps.auth
                   myapp.features.steps.cart]
 :test-framework  :speclj}
```

```bash
clj -M -m gherclj.main --verbose
# or
bb -m gherclj.main --verbose
```

**Option C: Custom main**

```clojure
(ns myapp.features.runner
  (:require [gherclj.pipeline :as pipeline]))

(defn -main [& _]
  (pipeline/run!
    {:features-dir    "features"
     :step-namespaces ['myapp.features.steps.auth
                       'myapp.features.steps.cart]
     :test-framework  :speclj
     :verbose         true}))
```

All options produce:
- `target/gherclj/edn/auth.edn` — the parsed IR (inspectable, debuggable)
- `target/gherclj/generated/auth_spec.clj` — executable spec with qualified function calls

### 5. Generated output

The generated specs are clean, readable function calls:

```clojure
(ns authentication-spec
  (:require [speclj.core :refer :all]
            [gherclj.core :as g]
            [myapp.features.steps.auth :as auth]))

(describe "Authentication"

  (before-all (lifecycle/run-before-feature-hooks!))
  (before (g/reset!) (lifecycle/run-before-scenario-hooks!))
  (after (lifecycle/run-after-scenario-hooks!))
  (after-all (lifecycle/run-after-feature-hooks!))

  (it "Admin can log in"
    (auth/create-user "alice" "admin")
    (auth/user-logs-in)
    (auth/response-status 200))

  (it "Guest gets 401"
    (auth/create-user "unknown" "guest")
    (auth/user-logs-in)
    (auth/response-status 401)))
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
| `:framework-opts` | `[]` | Options passed to the test runner |

Step namespaces support glob patterns for discovery:

```clojure
{:step-namespaces [myapp.steps.manual           ;; concrete symbol
                   "myapp.features.steps.*"      ;; glob pattern
                   "myapp.*-steps"]}             ;; glob in the middle
```

### Tag filtering

Scenarios tagged `@wip` are always excluded from generation unless explicitly included. Use `-t` to filter by additional tags:

```bash
gherclj -t smoke          # only @smoke scenarios
gherclj -t '~slow'        # exclude @slow (wip still excluded)
gherclj -t smoke -t '~slow'  # combine include and exclude
```

### Framework passthrough options

Pass framework-specific options to the test runner via `--` on the CLI or `:framework-opts` in config. When provided, these are appended to the default runner arguments.

```bash
# Pass options to speclj after --
clj -M -m gherclj.main -- -f documentation -c -P

# Or in gherclj.edn
{:framework-opts ["-f" "documentation" "-c" "-P"]}
```

CLI `--` arguments override `:framework-opts` from the config file.

### Useful speclj options

The documentation reporter (`-f documentation`) with profiling (`-P`) and color (`-c`) gives a readable, timed overview of all scenarios:

```bash
gherclj -- -f documentation -c -P
```

```
          Authentication
[0.00003s]  - Admin can log in
[0.00002s]  - Guest gets 401

          Checkout
[0.00005s]  - Empty cart shows error
[0.00003s]  - Valid cart creates order
```

Add a bb task for easy access:

```clojure
;; bb.edn
feature-docs {:requires ([gherclj.main :as main])
              :task     (main/-main "--" "-f" "documentation" "-c" "-P")}

features-slow {:requires ([gherclj.main :as main])
               :task     (main/-main "-t" "slow" "--" "-f" "documentation" "-P")}
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

## Lifecycle Hooks

Step namespaces can register lifecycle hooks through `gherclj.core`:

```clojure
(ns myapp.features.steps.hooks
  (:require [gherclj.core :as g]))

(g/before-all #(println "starting feature run"))
(g/before-feature #(println "starting feature"))
(g/before-scenario #(println "starting scenario"))

(g/after-scenario cleanup!)
(g/after-feature #(println "finished feature"))
(g/after-all #(println "finished feature run"))
```

Hook timing:
- `g/before-all` - once before the generated test run starts
- `g/before-feature` - once per generated feature file
- `g/before-scenario` - before each generated scenario, after `g/reset!`
- `g/after-scenario` - after each generated scenario, even when it fails
- `g/after-feature` - once after each generated feature file finishes
- `g/after-all` - once after the generated test run finishes

Example cleanup hook:

```clojure
(defn cleanup! []
  (app/stop!)
  (when-let [s @mock-ollama-server]
    (httpkit/server-stop! s)
    (reset! mock-ollama-server nil)))

(g/after-scenario cleanup!)
```

`g/before-all` and `g/after-all` run when specs are executed through gherclj's runner. If you invoke generated Speclj or `clojure.test` files directly, feature and scenario hooks still run, but the `all` hooks do not.

## Assertions

gherclj provides framework-agnostic assertion functions that delegate to the active test framework:

```clojure
(g/should= expected actual)
(g/should value)
(g/should-not value)
(g/should-be-nil value)
(g/should-not-be-nil value)
```

These work under both `:speclj` and `:clojure.test`. If your project uses a single framework, you can use its native assertions directly (e.g., speclj's `should=`). Use `g/should=` when your steps need to be framework-agnostic.

## Test Framework Support

gherclj ships with `:speclj` and `:clojure.test` output formats. Add your own by implementing the generator multimethods:

```clojure
(defmethod gherclj.generator/generate-ns-form :my-framework [config source step-ns-syms] ...)
(defmethod gherclj.generator/wrap-feature :my-framework [config feature-name scenario-blocks] ...)
(defmethod gherclj.generator/wrap-scenario :my-framework [config scenario background] ...)
(defmethod gherclj.generator/wrap-pending :my-framework [config scenario background] ...)
(defmethod gherclj.generator/run-specs :my-framework [config] ...)
```

## AI Agent Skills

The following skills are available for AI coding agents working with gherclj:

- [`gherkin`](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/gherkin/SKILL.md) — How to specify features. Gherkin writing guide: scenario design, step conventions, anti-patterns 
- [`gherclj`](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/gherclj/SKILL.md) — How to implement features. Step implementation conventions: assertions, state management, definition of done

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
