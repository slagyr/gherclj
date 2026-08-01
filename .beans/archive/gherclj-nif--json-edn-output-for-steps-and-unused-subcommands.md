---
# gherclj-nif
title: --json/--edn output for steps and unused subcommands
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-26T15:41:22Z
updated_at: 2026-04-26T16:23:50Z
---

## Migrated from beads

- Original bead id: `gherclj-nif`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-26T16:23:50Z
- Close reason: Closed

## Description

Why
---
Agents consuming `gherclj steps` and `gherclj unused` currently have to
parse colorized text output, which is brittle. ENHANCEMENTS.md flags
machine-readable output as the highest-leverage agent-facing addition.
Both formats matter: JSON for non-Clojure consumers (most agents,
shell pipelines), EDN for Clojure-native callers and faithful
representation of keywords/symbols.

What
----
- New `--json` and `--edn` boolean flags on `gherclj steps` and
  `gherclj unused`. Mutually exclusive — passing both is an error.
- Both formats are pretty-printed (multi-line) for human inspection.
- Single shared schema, two renderers; kebab-case field names in both
  formats (no auto-camelCase) so JSON and EDN are 1:1.
- Existing filters (`--given`/`--when`/`--then`, positional keyword
  filter, `-t` for unused) compose with `--json`/`--edn` and apply
  before serialization.
- Color flags become no-ops in machine-output mode.

Schema
------
For `gherclj steps`:
  {:gherclj-version "1.0.0"
   :command         "steps"
   :steps           [{:type :given            ; "given" in JSON
                      :phrase "a user {name:string}"  ; always populated; regex source for regex-based steps
                      :regex false            ; true if :phrase is a regex source
                      :helper-ref "app-steps/create-adventurer"
                      :ns "gherclj.sample.app-steps"
                      :file "src/gherclj/sample/app_steps.clj"
                      :line 10
                      :doc nil-or-string
                      :bindings [{:name "name" :type "string"}]}]}

For `gherclj unused`:
  {:gherclj-version   "1.0.0"
   :command           "unused"
   :scenarios-scanned 42
   :tags-applied      {:include [] :exclude ["slow"]}
   :unused-steps      [<same step entries as above>]}

Steps sorted deterministically by [ns line]. Map keys alphabetized in
JSON output.

Affected files
--------------
- src/gherclj/main.cljc — add `--json` and `--edn` to steps-cli-options
  (kept out of base `--help`, exposed via the steps subcommand help)
- src/gherclj/catalog.clj — refactor to build-data + render-text |
  render-json | render-edn pipeline; subcommand --help text mentions
  the new flags
- src/gherclj/unused.clj — same refactor for the unused subcommand
- spec/gherclj/features/steps/cli.clj (or wherever CLI step-defs live)
  — new step-defs:
    - "the output should be valid EDN"
    - "the output should be valid JSON"
    - "the output should span multiple lines"
    - "the EDN output should include a step with: <table>"
    - "the JSON output should include a step with: <table>"
    - "every step entry in the JSON output has type {string}"
    - "the EDN report should include: <table>"
    - "the JSON report should include: <table>"
    - "the :unused-steps list should contain a step with phrase {string}"
    - "the exit code should be non-zero"
    - "the error output should mention {string}"

Spec coverage (all currently @wip)
----------------------------------
- features/step_catalog.feature  gherclj steps --edn emits a structured catalog
- features/step_catalog.feature  gherclj steps --json emits a structured catalog
- features/step_catalog.feature  regex-based steps are flagged with :regex true
- features/step_catalog.feature  --given filter composes with --json
- features/step_catalog.feature  --json and --edn together is an error
- features/unused_steps.feature  gherclj unused --edn emits a structured report
- features/unused_steps.feature  gherclj unused --json emits a structured report

## Design

Refactor each subcommand into:

  config → build-data → render
                         ├─ render-text  (current behavior)
                         ├─ render-json
                         └─ render-edn

build-data returns the schema map directly (single source of truth for
sort order, field names, value coercion). Each renderer consumes it.

JSON serialization via clojure.data.json (verify it's a transitive dep;
add explicitly if not). Pretty-print with indent.

EDN via clojure.pprint/pprint after stripping any metadata.

Field-name policy: kebab-case in both formats. JSON's :type value
becomes "given"/"when"/"then" strings (keywords have no JSON
representation). Symbol fields like :ns become strings ("gherclj.foo")
in JSON; remain symbols in EDN.

Mutual-exclusion check happens in main/parse-args before subcommand
dispatch; error printed to stderr in plain text (not the requested
format) so consumers can detect failure even if their parser chokes
on the message.

## Acceptance Criteria

- gherclj steps --json emits valid pretty-printed JSON conforming to the schema
- gherclj steps --edn emits valid pretty-printed EDN conforming to the schema
- gherclj unused --json and --edn emit valid pretty-printed output for the unused report schema
- :phrase always populated; :regex true iff the underlying step was registered with a regex
- :doc carries the docstring (or null/nil when absent)
- :bindings populated for templated steps (with name + type), empty for regex-based
- Steps sorted by [ns line] in both formats
- Existing filters (--given/--when/--then/-t/keyword) compose with --json/--edn
- --json and --edn together exits non-zero with stderr message about mutual exclusivity
- Color flags suppressed (no-op) in machine-output mode
- subcommand --help text lists --json and --edn
- All seven @wip scenarios pass with @wip removed
- bb test passes (unit + features)
