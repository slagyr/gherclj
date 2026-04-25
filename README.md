<img align="left" width="200" src="https://raw.githubusercontent.com/slagyr/gherclj/master/gherclj.png" alt="gherclj" style="margin-right: 20px; margin-bottom: 10px;">

### gherclj

_pronounced: /ˈɡɜːrkəl/, gur-kull_

Translates Gherkin acceptance tests into code.

<br clear="left">

## Introduction

gherclj bridges the gap between human-readable feature specifications and executable tests. It parses standard [Gherkin](https://cucumber.io/docs/gherkin/reference/) `.feature` files and generates test files that call real code.

The pipeline:

```
.feature files → generated unit test files
```

The generated specs are native to the target language's test runner — speclj, JUnit 5, RSpec, Go's `testing`, pytest, etc. They're readable, debuggable, committable, and have no gherclj runtime dependency. Production code never imports gherclj either; gherclj's footprint is the step-routing namespace plus a `bb.edn` task.

Internally the pipeline goes `feature → IR → spec`. The IR isn't persisted by default; pass `--ir-edn` (or `:ir-edn true` in config) to also write it to `target/gherclj/edn/` for inspection.

## Quick Start

### 1. Add gherclj to your project

```clojure
;; deps.edn or bb.edn
{:deps {io.github.slagyr/gherclj {:git/tag "v1.0.0" :git/sha "PENDING"}}}
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

A step definition is a **single-line routing entry** that maps a Gherkin phrase to a helper function. The macro signature is intentionally constrained:

```clojure
(defgiven phrase helper-ref [docstring])
(defwhen  phrase helper-ref [docstring])
(defthen  phrase helper-ref [docstring])
```

There is no body and no arg vector. Helpers are plain Clojure functions; the macro builds the call mechanically from the matched template args.

Recommended pattern: helpers in their own namespace, step defs in a routing-only namespace. Each step namespace declares its helper module via `helper!`:

```clojure
;; myapp/features/helpers/auth.clj — real test logic lives here
(ns myapp.features.helpers.auth
  (:require [gherclj.core :as g]
            [myapp.auth :as app]))

(defn create-user! [name role]
  (g/assoc! :user {:name name :role role}))

(defn user-logs-in! []
  (let [{:keys [role]} (g/get :user)]
    (g/assoc! :response {:status (if (= "admin" role) 200 401)})))

(defn response-status [expected]
  (g/should= expected (g/get-in [:response :status])))
```

```clojure
;; myapp/features/steps/auth.clj — pure routing
(ns myapp.features.steps.auth
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [myapp.features.helpers.auth]))

(helper! myapp.features.helpers.auth)

(defgiven "a user {name:string} with role {role:string}" auth/create-user!)
(defwhen  "the user logs in"                              auth/user-logs-in!)
(defthen  "the response status should be {status:int}"   auth/response-status)
```

Why the split: the generated spec inlines `(auth/create-user! "alice" "admin")` directly. It depends only on the helper namespace — never on the step namespace. Helpers are normal code, can be unit tested, and look like idiomatic functions in your project's tongue.

Step defs accept an optional docstring as the third arg. The docstring surfaces in `gherclj steps` output:

```clojure
(defgiven "the following crew exist:" auth/setup-crew!
  "Sets :crew atom (test only — does NOT write disk).")

(defwhen "the log has entries matching:" auth/check-logs!
  "Polls for up to 2s. Timeout is not configurable.")
```

Template syntax:
- `{name:string}` — greedy string capture (bounded by surrounding literal text)
- `{name:int}` — integer capture (coerced via `parse-long`)
- `{name:float}` — float capture (coerced via `parse-double`)
- `{name}` — word capture (`\S+`)

For edge cases, pass a raw regex instead of a template string. Capture groups become positional helper args:

```clojure
(defthen #"^the output should contain headers (.+)$" auth/check-headers)
```

Steps that match a Gherkin table or doc-string receive it as a final argument; the helper just declares the extra param:

```clojure
;; helpers
(defn setup-users! [table]
  (let [{:keys [headers rows]} table]
    (g/assoc! :users (mapv #(zipmap headers %) rows))))

;; routing
(defgiven "the following users:" auth/setup-users!)
```

### 4. Configure and run

There are several ways to configure and run the pipeline.

**Option A: Task/alias with CLI flags (recommended)**

```clojure
;; bb.edn
{:deps {io.github.slagyr/gherclj {:git/tag "v1.0.0" :git/sha "PENDING"}}
 :tasks
 {features {:doc "Run feature specs"
            :requires ([gherclj.main :as main])
            :task (main/-main "-s" "myapp.features.steps.auth"
                              "-s" "myapp.features.steps.cart"
                              "-t" "speclj")}}}

;; deps.edn
{:deps {io.github.slagyr/gherclj {:git/tag "v1.0.0" :git/sha "PENDING"}}
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
 :framework       :clojure/speclj}
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
     :framework       :clojure/speclj
     :verbose         true}))
```

All options produce:
- `target/gherclj/generated/auth_spec.clj` — executable spec with qualified function calls

Add `--ir-edn` (or `:ir-edn true` in config) to also persist the parsed IR to `target/gherclj/edn/auth.edn` for inspection.

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

## Supported frameworks

Step definitions are written in Clojure, but the production code they exercise — and the test code gherclj generates — can be in any supported language.

| Framework keyword       | Generated test runner       |
|-------------------------|-----------------------------|
| `:clojure/speclj`       | speclj                      |
| `:clojure/test`         | `clojure.test`              |
| `:ruby/rspec`           | RSpec                       |
| `:python/pytest`        | pytest                      |
| `:go/testing`           | Go's `testing` package      |
| `:java/junit5`          | JUnit 5 (Jupiter) + Maven   |
| `:javascript/node-test` | Node's built-in `node:test` |
| `:typescript/node-test` | Node `node:test` (via tsx)  |
| `:rust/rustc-test`      | `cargo test`                |
| `:csharp/xunit`         | xUnit + `dotnet test`       |
| `:bash/testing`         | shell-based assertions      |

Working examples for every supported framework live under [`examples/space-airlock/`](examples/space-airlock/) — one shared feature suite, eleven native implementations.

## Configuration

gherclj reads configuration from `gherclj.edn` (project root or classpath), with CLI flags as overrides.

| Key | Default | Description |
|-----|---------|-------------|
| `:features-dir` | `"features"` | Directory containing `.feature` files |
| `:output-dir` | `"target/gherclj/generated"` | Directory for generated spec files |
| `:edn-dir` | `"target/gherclj/edn"` | Directory for parsed EDN IR files (only used when `:ir-edn` is true, or when invoking `parse!`/`generate!` directly) |
| `:ir-edn` | `false` | When true, `pipeline/run!` also persists the parsed IR to `:edn-dir` |
| `:step-namespaces` | `[]` | Namespace symbols or glob pattern strings |
| `:framework` | `:clojure/speclj` | Target test framework — see [Supported frameworks](#supported-frameworks) |
| `:verbose` | `false` | Print progress to stdout |
| `:framework-opts` | `[]` | Options passed to the test runner |
| `:include-tags` | `[]` | Include scenarios that match any listed tag |
| `:exclude-tags` | `[]` | Exclude scenarios that match any listed tag |

Step namespaces support glob patterns for discovery:

```clojure
{:step-namespaces [myapp.steps.manual           ;; concrete symbol
                   "myapp.features.steps.*"      ;; glob pattern
                   "myapp.*-steps"]}             ;; glob in the middle
```

### Tag filtering

Tags are filtered uniformly. Use `-t` to include tags and `-t '~tag'` to exclude tags:

```bash
gherclj -t smoke          # only @smoke scenarios
gherclj -t '~slow'        # exclude @slow
gherclj -t wip            # only @wip scenarios
gherclj -t smoke -t '~slow'  # combine include and exclude
```

### Scenario location selection

You can run only specific scenarios by passing `file:line` selectors as positional arguments. A selector matches the scenario whose declaration contains the given line.

```bash
gherclj features/adventure/dragon_cave.feature:42

# Multiple selectors run all selected scenarios in one invocation
gherclj features/adventure/dragon_cave.feature:42 \
        features/adventure/moon_castle.feature:73
```

Location selectors combine with normal options like `-f`, `-e`, `-o`, and tag filters.

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
              :task     (main/-main "-t" "~wip" "--" "-f" "documentation" "-c" "-P")}

features-slow {:requires ([gherclj.main :as main])
               :task     (main/-main "-t" "slow" "-t" "~wip" "--" "-f" "documentation" "-P")}
```

## Step Catalog

`gherclj steps` lists all registered step definitions grouped by type. Each entry shows the phrase and source location on one line, with an optional docstring on the next. Output is colorized by default.

```bash
gherclj -s myapp.features.steps.* steps
```

```
Given:
  a user {name:string}  (auth_steps.clj:4)
  the following crew exist:  (crew_steps.clj:18)
    Sets :crew atom (test only — does NOT write disk).

When:
  the user logs in  (auth_steps.clj:12)
    Polls for up to 2s. Timeout is not configurable.

Then:
  the response status should be {status:int}  (auth_steps.clj:20)
```

**Type filters** (`--given`, `--when`, `--then`) are additive — each flag includes that type; no flags means all types:

```bash
gherclj -s myapp.features.steps.* steps --given --when   # Given + When only
```

**Keyword filter** — pass a word as a positional argument to narrow results by phrase or docstring:

```bash
gherclj -s myapp.features.steps.* steps crew   # only steps mentioning "crew"
```

**Color** — colorized by default; suppress with `--no-color` for scripted or piped use:

```bash
gherclj -s myapp.features.steps.* steps --no-color
```

**Help:**

```bash
gherclj steps --help
```

## Unused Step Detection

`gherclj unused` compares registered step definitions against all step texts in your feature files and reports any that are never referenced. Useful for keeping step namespaces clean as features evolve.

```bash
gherclj -f features -s myapp.features.steps.* unused
```

```
Scanned 42 scenarios. No tag filtering applied.
38 of 40 registered steps are in use (2 unused).
Unused steps:

Given:
  setup legacy auth  (auth_steps.clj:87)

When:
  the legacy system responds  (auth_steps.clj:93)
```

**Tag filtering** — use the same `-t` flags as the pipeline. Steps that only appear in excluded scenarios are reported as unused, and the output is explicit about what was scanned:

```bash
gherclj -f features -s myapp.features.steps.* unused -t ~slow
```

```
Scanned 35 of 42 scenarios. 7 scenarios filtered out by tags: ~slow.
38 of 40 registered steps are in use (2 unused).
```

**Note:** Steps used as test data (looked up by name via the registry rather than matched by step text) will appear as unused. This is a known limitation.

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

These work under both `:clojure/speclj` and `:clojure/test`. If your project uses a single framework, you can use its native assertions directly (e.g., speclj's `should=`). Use `g/should=` when your steps need to be framework-agnostic.

## Test Framework Support

gherclj ships with `:clojure/speclj` and `:clojure/test` output formats. Add your own by implementing the generator multimethods:

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
bb parse      # Parse .feature files → EDN IR (writes to disk; explicit two-stage flow)
bb generate   # Generate spec files from EDN (reads disk; pair with `bb parse`)
bb spec       # Run unit specs
bb features   # Run feature specs (in-memory parse + generate + execute, excludes @wip by default)
bb test       # Run all tests
bb clean      # Remove generated files
```
