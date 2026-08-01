---
# gherclj-h9z
title: 'ir-edn config: skip IR persistence by default'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-25T20:09:56Z
updated_at: 2026-04-25T20:26:52Z
---

## Migrated from beads

- Original bead id: `gherclj-h9z`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-25T20:26:52Z
- Close reason: Closed

## Description

Why
---
The intermediate EDN IR was originally written to disk on every `run!` so
that AI agents could inspect it. In practice the IR isn't useful to AIs,
so writing it by default is just I/O noise that pollutes target/. We're
turning EDN persistence off by default and adding a single opt-in.

What
----
- New config key `:ir-edn` (boolean, default false). When true, `run!`
  also writes EDN IR files to `:edn-dir` alongside generated specs.
- New CLI flag `--ir-edn` that sets the same key.
- `run!` becomes an in-memory pipeline by default — parses features,
  generates specs, never touches `:edn-dir`.
- `parse!` and `generate!` keep their current behavior (explicit
  disk-EDN entry points; unchanged).

Affected files
--------------
- src/gherclj/pipeline.clj — refactor `run!` to in-memory; share an
  `emit-spec-for-ir!` helper between `generate!` and `run!`
- src/gherclj/config.cljc — validate `:ir-edn` boolean
- src/gherclj/main.cljc — add `--ir-edn` flag and update help text
- spec/gherclj/features/steps/pipeline.clj — add a step-def for the new
  `When the full pipeline runs with options:` table phrasing
- spec/gherclj/pipeline_spec.clj — flip the existing run!-writes-EDN
  assertion and add an opt-in test

Spec coverage (all currently @wip)
----------------------------------
- features/pipeline/pipeline.feature:110  Full pipeline does not persist IR by default
- features/pipeline/pipeline.feature:126  ir-edn opts into IR persistence
- features/pipeline/pipeline.feature:167  clojure.test generates _test files
- features/pipeline/pipeline.feature:256  WIP scenarios are parsed and generated when unfiltered
- features/pipeline/pipeline.feature

## Design

Step phrasing chosen to be table-based so it scales as more options are
added without proliferating step variants:

  When the full pipeline runs with options:
    | option    | value           |
    | framework | :clojure/speclj |
    | ir-edn    | true            |

The step-def parses values: `:`-prefixed → keyword, "true"/"false" →
boolean, otherwise string. Option names map directly to config keys
(e.g. `ir-edn` → `:ir-edn`).

The existing `When the full pipeline runs with framework :X` step stays
in place — only new scenarios use the table form. A future bead can
migrate the older scenarios if/when desired.

## Acceptance Criteria

- :ir-edn config key validates as boolean (default false)
- --ir-edn CLI flag sets the key; help text lists it
- run! writes generated specs but NOT EDN when :ir-edn is unset
- run! writes both generated specs AND EDN when :ir-edn is true
- parse! still writes EDN unconditionally (existing behavior)
- generate! still reads from disk EDN (existing behavior)
- All four @wip scenarios in features/pipeline/pipeline.feature pass
  with the @wip tag removed
- Unit specs in spec/gherclj/pipeline_spec.clj cover both default and
  opt-in cases
- bb test passes (unit specs + features)
