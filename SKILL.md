---
name: gherclj
description: Use this skill when implementing gherclj feature steps, working on tasks that reference .feature files, or writing defgiven/defwhen/defthen definitions and their helpers. Ensures step definitions follow gherclj conventions — especially that defthen helpers assert results.
---

# gherclj Step Implementation

## When This Skill Applies

Use this skill whenever you are implementing step definitions for `.feature` files in a gherclj project. This includes any task that references a feature file or involves writing `defgiven`, `defwhen`, or `defthen` steps and their helper functions.

## Getting Started

If gherclj is not yet wired into the project, do this once:

1. **Add the dependency.** See the project README for the current `:git/tag` and `:git/sha`:

   ```clojure
   {:deps {io.github.slagyr/gherclj {:git/tag "vX.Y.Z" :git/sha "..."}}}
   ```

2. **Wire a babashka task** (or the equivalent runner alias for `deps.edn`/`project.clj`):

   ```clojure
   ;; bb.edn
   {:deps  {io.github.slagyr/gherclj {:git/tag "vX.Y.Z" :git/sha "..."}}
    :tasks
    {gherclj {:doc      "Run gherclj feature specs"
              :requires ([gherclj.main :as main])
              :task     (apply main/-main "-s" "myapp.features.steps.*"
                                          "-F" "clojure/speclj"
                                          *command-line-args*)}}}
   ```

   Forward `*command-line-args*` so callers can pass tag filters, `file:line` selectors, and other flags through.

3. **Lay out features and steps.** Feature files live under `features/` at the project root (override with `-f` or `:features-dirs` in `gherclj.edn`). Step routing and helpers live under your spec tree:

   ```
   myapp/
     features/
       authentication.feature
     spec/myapp/features/
       steps/auth.clj    — phrase → helper routing
       helpers/auth.clj  — actual test logic
     src/...
   ```

4. **Run the suite** with `bb gherclj`. The runner invokes `gherclj.main`, which parses every `.feature` file under `:features-dirs`, generates spec files under `target/gherclj/generated/`, and executes them via the configured test framework.

Run `bb gherclj --help` to see all CLI flags and subcommands (`steps`, `match`, `unused`, `ambiguity`). The README has the full reference: configuration keys, supported frameworks, tag filtering, and `file:line` scenario selectors.

## Feature Contract Integrity

Approved `.feature` files are behavioral contracts.

- Do NOT semantically weaken, reinterpret, or rewrite approved scenarios without user approval.
- Clarifying wording changes are fine only when they preserve the approved behavior exactly.
- If implementation and approved feature text diverge, stop and raise the mismatch instead of changing the scenario to fit partial implementation.

## Step Definitions Must Exercise Real Behavior

Helper functions should test real product behavior through real code paths where feasible.

- Prefer calling production entry points, public APIs, application services, or other real seams the product already exposes.
- If a scenario needs a seam, prefer a real public seam or an explicitly approved test hook.
- Do not move product logic into helper functions just because the implementation is missing.

## Forbidden Acceptance-Test Shortcuts

- Do NOT inject unimplemented product behavior directly in helper functions just to make scenarios pass.
- Do NOT add acceptance-test-only shims that simulate promised behavior the product does not actually implement.
- Passing feature scenarios must reflect actual implemented behavior, not test-only shortcuts.
- A task is not complete if the feature passes only because the helpers fake missing behavior.

## Architecture: Steps and Helpers

Step definitions are pure routing — a phrase mapped to a helper function reference. All logic lives in a separate helpers namespace.

```
spec/<app>/features/steps/<feature>.clj   — phrase → helper routing
spec/<app>/features/helpers/<feature>.clj — actual test logic
```

### Steps file — pure routing

Use `helper!` to declare the helper namespace, then route each phrase to its helper function:

```clojure
(ns myapp.features.steps.auth
  (:require [gherclj.core :refer [defgiven defwhen defthen helper!]]
            [myapp.features.helpers.auth]))

(helper! myapp.features.helpers.auth)

(defgiven "a user {name:string} with role {role:string}" auth/create-user!)
(defwhen  "the user logs in"                              auth/user-logs-in!)
(defthen  "the response status should be {status:int}"   auth/response-status)
```

### Helpers file — logic and assertions

```clojure
(ns myapp.features.helpers.auth
  (:require [gherclj.core :as g]
            [myapp.auth :as app]))

(defn create-user! [name role]
  (g/assoc! :user {:name name :role role}))

(defn user-logs-in! []
  (let [{:keys [role]} (g/get :user)]
    (g/assoc! :response (app/authenticate role))))

(defn response-status [expected]
  (g/should= expected (g/get-in [:response :status])))
```

## Step Types and Their Responsibilities

### defgiven — Set up preconditions

Given helpers mutate state to establish preconditions. No assertions needed.

### defwhen — Perform actions

When helpers perform the action under test. No assertions needed.

### defthen — Assert results

Then helpers MUST assert. A `defthen` helper that returns a value without asserting is a bug — it produces 0 assertions and silently passes.

If your project uses a single framework, you can use its native assertions directly (e.g., speclj's `should=`). Use `g/should=` when helpers need to be framework-agnostic — for example, when the project runs tests under both speclj and clojure.test.

```clojure
;; WRONG — no assertion, silently passes
(defn response-status [expected]
  (g/get-in [:response :status]))

;; RIGHT — asserts the expected value
(defn response-status [expected]
  (g/should= expected (g/get-in [:response :status])))
```

## Common Assertion Patterns

```clojure
;; Exact match
(g/should= expected (g/get :key))

;; Truthiness
(g/should (g/get :key))
(g/should-not (g/get :key))

;; Nil check
(g/should-be-nil (g/get :key))
(g/should-not-be-nil (g/get :key))

;; Collection
(g/should= expected-vec (g/get :items))
```

## State Management

Helpers use `gherclj.core` for state, aliased as `g`:

- `g/assoc!`, `g/assoc-in!` — set state
- `g/get`, `g/get-in` — read state
- `g/update!`, `g/update-in!` — modify state
- `g/swap!` — arbitrary state transformation
- `g/dissoc!` — remove state
- `g/reset!` — called automatically before each scenario

## Parallel-safe Helpers

gherclj can isolate `gherclj.core/*state*` per scenario, but parallel execution
is only safe when helper code also avoids shared external resources.

- Use a fresh temp directory per scenario instead of a singleton temp path
- Use ephemeral ports instead of fixed ports
- Use per-thread or per-scenario DB connections instead of singletons
- Avoid mutating JVM-global process state such as system properties; inject config instead
- Treat files, sockets, and other shared handles as scenario-local resources unless they are explicitly synchronized

## Running Features and Scenarios

You can run a whole feature file by passing its path, or run a specific scenario by passing a `file:line` selector as a positional argument. A selector matches the scenario whose declaration contains the given line.

```bash
gherclj features/adventure/dragon_cave.feature

# Run one scenario from a feature
gherclj features/adventure/dragon_cave.feature:42

# Mix full-feature and single-scenario targets
gherclj features/adventure/dragon_cave.feature \
        features/adventure/moon_castle.feature:73
```

Feature paths and location selectors combine with normal options like `-f`, `-e`, `-o`, and tag filters.

## Reuse Before Inventing

Before drafting a new `defgiven`/`defwhen`/`defthen`, run `gherclj steps`
to see every registered step's phrase, docstring, and source location
grouped by Given/When/Then. This is the authoritative list — reuse before
inventing.

A new step-def is a real cost — one more thing to maintain, learn,
and grep for. Each scope that grows its own step-defs is a place
where future scenarios won't benefit from shared tooling. Start by
reusing, even if the existing step needs a small extension.

Common patterns that often already exist in mature projects:

- HTTP assertions → `the last outbound HTTP request matches:` and
  `an outbound HTTP request to "<url>" matches:` with k-v tables
- File/data inspection → `the EDN file "<relpath>" contains:` with
  path/value rows
- File existence → `a file "<name>" exists with content "<X>"` plus
  the negative variant for "does not exist"
- Tool invocation → `the tool "<name>" is called with:` + `the tool
  result lines match:` / `contains ...`
- Session state → `session "<name>" has transcript matching:`

If you're adding a new Given step phrase, grep all existing step files
for the same or similar pattern first to avoid ambiguous match errors —
gherclj auto-discovers all step namespaces on the classpath via
`helper!` registrations.

## Docstrings on Step Defs

gherclj v0.9.0+ accepts an optional docstring between the phrase and
the arg vector:

```clojure
(defgiven setup-crew "the following users exist:"
  "Sets :users atom (test only — does NOT write disk)."
  [table]
  ...)
```

Add a docstring whenever the phrase alone doesn't convey the contract:

- side-effect surface (disk vs atom)
- sync/async behavior (polling, timeouts)
- state-slot being read/written
- gotchas (overwrites, bypasses production loader)

Skip docstrings on trivial matchers (`the exit code is N`) — the phrase is
the contract. Docstrings surface in `gherclj steps` output, so they're the
first thing the next agent reads.

## Discovering and Auditing Steps

```bash
# List all registered steps organized by Given/When/Then, with phrase, source location, and docstring
gherclj steps

# Check whether a phrase matches a registered step (reports match/no-match/ambiguous, with arg bindings)
gherclj match "the user logs in"

# Detect phrases in feature files that match multiple registered steps
gherclj ambiguity

# Find steps unused by any feature file (respects tag filters)
gherclj unused
```

Use `gherclj steps` to understand what steps already exist before writing new ones. Use `gherclj match` to verify a phrase routes to the intended step. Use `gherclj ambiguity` as a pre-flight check before running. Use `gherclj unused` during cleanup to find dead step definitions.

All four subcommands support `--json` and `--edn` for structured output.

## Verification

After implementing steps, always run the feature specs and verify:

1. **Assertion count > 0** — If you see `0 assertions`, your `defthen` helpers are not asserting
2. **No unexpected pending** — Pending scenarios mean step text isn't matching registered steps
3. **Real behavior is exercised** — Scenarios pass because the product implements the behavior, not because helpers simulated it
4. **Run:** the project's feature-suite task (e.g. `bb gherclj`, `clj -M:features`, `lein test`)

```
# Good — assertions present
40 examples, 0 failures, 68 assertions, 5 pending

# Bad — no assertions means defthen helpers aren't asserting
40 examples, 0 failures, 0 assertions, 5 pending
```

## Definition of Done

A feature implementation task is NOT complete until:

1. **Step definition file exists** — `spec/<app>/features/steps/<feature>.clj`
2. **Helper file exists** — `spec/<app>/features/helpers/<feature>.clj`
3. **All scenarios run** — Remove the `@wip` tag from the feature file
4. **No pending scenarios** — Every scenario's step text matches a registered step
5. **Assertions > 0** — Every `defthen` helper asserts
6. **No fake behavior in helpers** — Helpers are not simulating missing product behavior
7. **All pass** — The feature-suite command shows 0 failures

If the project tracks work in a task/issue tracker (beads, Linear, Jira, GitHub issues, etc.), do NOT mark the task complete if the feature file still has `@wip`, if scenarios are pending, or if the feature only passes because the helpers fake missing behavior.
