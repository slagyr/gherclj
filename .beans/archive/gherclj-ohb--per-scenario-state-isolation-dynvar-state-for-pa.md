---
# gherclj-ohb
title: 'Per-scenario state isolation: dynvar *state* for parallel-execution readiness'
status: completed
type: feature
priority: low
tags:
    - "migrated-from-beads"
created_at: 2026-05-09T21:31:08Z
updated_at: 2026-05-10T02:47:53Z
---

## Migrated from beads

- Original bead id: `gherclj-ohb`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-05-10T02:47:53Z
- Close reason: All acceptance criteria met after fix commit e4297d2. spec/gherclj/features/steps/cli.clj framework save/restore now reads g/*framework* directly and restores unconditionally. bb features-ct exits 0 standalone (223 tests, 519 assertions, 0 failures, 0 errors). bb test-all: all 6 combinations pass. SKILL.md 'Parallel-safe Helpers' section added at line 149 covering temp dirs, ports, DB connections, system properties, and shared handles.

## Description

gherclj.core/state is currently a single global atom shared across every
scenario. Sequential execution has been fine, but as soon as scenarios run
concurrently (e.g. when speclj ships a parallel mode, or when downstream
runners parallelize), each thread will read/write the same atom and step
state will corrupt across scenarios.

Convert *state* to a thread-local dynamic var so each scenario gets its own
atom via per-scenario binding. Sequential behavior is preserved; parallel
becomes safe to enable.

This is forward-looking work blocked on speclj parallel mode (does not
exist yet). The core change is benign in sequential execution, so it can
land ahead of speclj's feature without risk.

Before activation: draft Gherkin scenarios for the isolation contract per
the plan-with-features workflow.

## Design

1. Convert state to dynvar
   (def ^:dynamic *state* (atom {}))      ; private, root binding for REPL
   All g/get, g/assoc!, g/swap!, etc. deref/swap *state* (not the old
   private state). Public API signatures unchanged.

2. Hoist :_framework out of state
   Currently :_framework lives inside the state atom; reset! preserves it.
   With per-scenario state, every scenario would need to re-set :_framework.
   Move it to its own dynvar:
     (def ^:dynamic *framework* nil)
   set-framework! and active-framework target *framework*. Assertion
   multimethod dispatch reads *framework*. reset! becomes simpler — just
   empties the atom, no preserved keys.

3. Generator: per-scenario binding via 'around'
   Speclj output emits:
     (around [it]
       (binding [g/*state* (atom {})]
         (lifecycle/run-before-scenario-hooks!)
         (it)
         (lifecycle/run-after-scenario-hooks!)))
   clojure.test output uses the analogous (use-fixtures :each ...).
   Process-isolated frameworks (bash, python, go, ...) need no change —
   OS process boundary already isolates state.

4. Feature-level state inheritance (the wrinkle)
   before-feature hooks today write to global state and every scenario
   inherits those writes. Per-scenario binding would break this — each
   scenario gets a fresh atom with no feature-level seed.
   Fix: nested binding. Feature-level around establishes a feature atom;
   scenario-level around initializes its atom as a snapshot copy:
     (around [it]
       (binding [g/*state* (atom {})]
         (lifecycle/run-before-feature-hooks!)
         (it)
         (lifecycle/run-after-feature-hooks!)))
     (around [it]
       (binding [g/*state* (atom @g/*state*)]   ; snapshot copy
         (lifecycle/run-before-scenario-hooks!)
         (it)
         (lifecycle/run-after-scenario-hooks!)))
   Each scenario starts with feature seed; per-scenario writes don't leak
   back to the feature atom or sibling scenarios.

5. Other atoms are safe
   - registry, helper-imports, lifecycle/hooks: write-once at namespace
     load, read-only at scenario runtime. Safe under any concurrency.
   - The dynvar root binding remains an atom for REPL/non-spec callers.

6. Async binding propagation
   future and pmap propagate dynamic bindings via binding-conveyor-fn —
   helpers using these patterns "just work" inside a scenario binding.
   core.async go blocks do NOT propagate; helpers using core.async would
   need explicit bound-fn wrapping. Document as a footgun, not a blocker.

## Acceptance Criteria

Sequential-execution invariants preserved:
- bb test-all passes (all 6 combinations: bb/clj × spec/features/features-ct)
- No regression in existing scenarios

Per-scenario state isolation (new scenarios needed before activation):
- Two scenarios in the same feature can each g/assoc! the same key with
  different values without seeing each other's writes
- Scenario writes do not leak back to feature-level state
- before-feature hooks' writes are visible to every scenario in the feature

Generator output:
- Speclj output uses 'around' with binding [*state* ...]
- clojure.test output uses use-fixtures :each with the same binding
- Snapshot inheritance from feature-level binding to scenario-level binding

Documentation:
- SKILL.md gains a 'Parallel-safe helpers' section listing the external
  resources that must be made per-scenario for parallel execution to be
  safe

## Notes

Verification failed.

Two gaps relative to acceptance criteria:

1. CRITICAL — bb test-all does NOT pass.

   bb features-ct (and clj -M:features-ct) produce 145 errors out of 223 tests.
   The earlier 'All passed.' from test-all is misleading: the wrapper at
   bb.edn checks (:babashka/exit (ex-data e)) but the clojure-test framework
   adapter swallows test output via (binding [ct/*test-out* (java.io.StringWriter.)] ...)
   AND returns the result map; gherclj.main converts that to exit code 1. In
   test-all, the catch fires but the exit-code path may behave differently
   per task. Standalone 'bb features-ct' exits 1 every time after a clean.

   Root cause: spec/gherclj/features/steps/cli.clj:76 reads
   (g/get :_framework) to save the framework before main/run, then restores
   at line 88 with (g/set-framework! previous-framework). After the dynvar
   refactor, :_framework is NO LONGER in the state atom — it lives in
   g/*framework*. So previous-framework is always nil, the restore at line
   88 never fires, and the framework leaks across scenarios.

   Symptom: a scenario like 'CLI step namespace glob patterns are resolved
   during the pipeline' invokes main/run with -F clojure/speclj, which sets
   *framework* to :clojure/speclj via alter-var-root. After that scenario
   returns to a clojure.test-running context, *framework* is still
   :clojure/speclj, so g/should= dispatches to the speclj impl, which calls
   sc/should= → speclj.components/inc-assertions! → (swap! *assertions* ...).
   But *assertions* is unbound under clojure.test, raising
   'sci.impl.vars.SciUnbound cannot be cast to clojure.lang.IAtom'.

   Fix: replace (g/get :_framework) with g/*framework* (or expose
   g/active-framework as public) at cli.clj:76, and (g/set-framework!
   previous-framework) at cli.clj:89 should fire even when previous was nil
   (so the restore is unconditional). Confirm with: bb features-ct exits 0
   and 'Ran N tests ... 0 failures, 0 errors.'

2. SKILL.md 'Parallel-safe helpers' section missing.

   The acceptance criteria call for SKILL.md to gain a 'Parallel-safe
   helpers' section listing external resources that must be made
   per-scenario for parallel execution to be safe. SKILL.md exists only on
   master (commits a8b41e8/4f4c49a/aa7f774); this branch (gherclj-ohb) has
   not merged master. Either merge master and add the section, or do it as
   a follow-up bead — but the criterion is currently unmet.

The implementation otherwise looks good:
- core.clj dynvar work matches the design exactly
- speclj generator emits 'around' with feature/scenario binding
- clojure.test generator emits use-fixtures with snapshot inheritance
- Two new scenarios in lifecycle_hooks.feature cover isolation +
  inheritance contract
- bb features (speclj) passes with 223 examples, 0 failures, 349 assertions
