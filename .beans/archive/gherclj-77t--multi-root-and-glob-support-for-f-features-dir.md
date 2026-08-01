---
# gherclj-77t
title: Multi-root and glob support for -f/--features-dir
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-05-09T20:27:59Z
updated_at: 2026-05-09T23:15:08Z
---

## Migrated from beads

- Original bead id: `gherclj-77t`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-05-09T23:15:08Z
- Close reason: All acceptance criteria met. @wip removed from features/features_dirs.feature with no other edits to the spec (verified via diff against creation commit 1e4c6a0). All 13 scenarios intact. bb features features/features_dirs.feature: 13/13 pass (45 assertions). bb features: 221/221 pass. bb test-all: all 6 combinations pass.

## Description

gherclj's -f/--features-dir is currently single-valued; repeating it silently
overwrites earlier values, and there is no way to compose feature suites from
a base directory plus per-module directories (e.g. -f features -f modules/*/features).

This change makes -f repeatable, adds glob support per value, and qualifies
:source by root path when more than one root contributes IRs so cross-root
collisions never overwrite each other downstream.

The user-facing contract is fully specified in features/features_dirs.feature
(currently @wip with 13 scenarios). Implementation is complete when @wip is
removed and the suite passes.

## Design

Source naming
- Single root: bare :source (back-compat).
- Multi root: :source prefixed with its root path. EDN/spec output paths key
  off the prefixed source. No collisions possible.

Selector resolution (for each source[:line] location)
1. Selector starts with a known root prefix -> strip prefix, check existence.
2. Bare selector -> for each root, check if <root>/<selector> exists.
- Zero candidates: 'Feature file not found: <selector>'
- One candidate:  use it.
- Multiple:       'Ambiguous selector: <selector>' listing each match, plus
                  'Qualify with the root path'.

Config schema
- New key :features-dirs (seq of strings, default ['features']).
- Legacy :features-dir is REMOVED. If present, fail with targeted message
  ':features-dir is no longer supported; use :features-dirs (a list)'.
- Glob patterns expand at config-resolve time, sorted lexicographically.
- Per-glob empty match -> fail with the pattern.
- Final empty list -> 'no feature roots resolved'.

CLI
- -f / --features-dir becomes :multi true with :id :features-dirs (long flag
  name unchanged for back-compat with existing scripts).
- Help text: 'Features directory (repeatable; supports glob; default: features)'.
- One or more -f on CLI replaces config :features-dirs entirely (no merging).

## Acceptance Criteria

- @wip removed from features/features_dirs.feature
- bb features features/features_dirs.feature   PASSES (all 13 scenarios)
- bb features                                  PASSES (no regressions)
- bb test-all                                  PASSES (all 6 combinations)

## Notes

Production code touched:
- src/gherclj/main.cljc            -- CLI option (:multi, :id, help text)
- src/gherclj/config.cljc          -- :features-dirs schema, glob expansion,
                                      legacy-key error, empty-list error
- src/gherclj/parser.clj           -- :source qualification in parse-feature-file;
                                      parse-features-dir multi-root path
- src/gherclj/pipeline.clj         -- iterate roots; selector resolution
                                      (selector->relative-source, selected-scenario-name,
                                       verify-feature-file!, selected-scenarios-by-source);
                                      full pipeline multi-root flow
- src/gherclj/ambiguity.clj        -- read :features-dirs, iterate roots
- src/gherclj/unused.clj           -- read :features-dirs, iterate roots

Test infrastructure touched:
- spec/gherclj/features/steps/cli.clj -- rewrite-sandbox-path-options must
                                         handle multiple -f tokens
- spec/gherclj/features/steps/pipeline.clj -- new step
                                              'Given features directories containing:'
                                              (table: root, file); extend
                                              'the feature {name:string} contains:'
                                              to accept paths under base-dir;
                                              pipeline-config reads :features-dirs
                                              from state when set; parse-option-value
                                              special-cases features-dirs (string -> vec)

Error message strings (contract -- must match scenarios literally):
- ':features-dir is no longer supported'
- 'no directories matched'
- 'no feature roots resolved'
- 'Feature file not found'
- 'Ambiguous selector'
- 'Qualify with the root path'

Targeted scenarios (features/features_dirs.feature):
  :14  Multi-root pipeline qualifies each :source
  :45  Glob -f expands to all matching directories
  :71  CLI -f with glob pattern expands
  :95  Glob patterns expand at config-resolve time
  :106 Glob with no matching directories errors
  :120 Bare selector resolves to its single matching root
  :147 Qualified selector picks qualified root
  :174 Bare selector matching multiple roots is ambiguous
  :201 Selector under no root errors
  :227 Config :features-dirs accepts a list
  :240 Legacy :features-dir rejected
  :251 CLI -f replaces config :features-dirs
  :263 Empty :features-dirs rejected
