---
# gherclj-vz7
title: 'gherclj ambiguity: structured pre-flight report for ambiguous step matches'
status: completed
type: feature
priority: low
tags:
    - "migrated-from-beads"
created_at: 2026-04-26T17:39:40Z
updated_at: 2026-04-26T18:51:16Z
blocked_by:
    - gherclj-sqp
---

## Migrated from beads

- Original bead id: `gherclj-vz7`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-26T18:51:16Z
- Close reason: Closed

## Description

Why
---
classify-step (src/gherclj/core.clj:176-195) already throws at generation
time when a step phrase matches more than one registered step. The
runtime error works but has three friction points:
- it bails on the first ambiguity and hides the rest,
- the message is a plain string with no structure for tooling,
- it requires running through generation to surface anything.

`gherclj ambiguity` exposes the same detection as a standalone report —
finds every ambiguous phrase across the corpus in a single pass,
renders text by default, --json/--edn for tooling, --tag for scoped
reporting. This is reporting/UX over existing detection, not new
detection capability — hence P3.

What
----
- New `gherclj ambiguity` subcommand. Same input shape as `gherclj unused`:
  --features-dir, --step-namespaces, --tag.
- Walks scenarios (post-tag-filter), classifies each step phrase against
  the registered step set, records every phrase that matches more than
  one step.
- Per-occurrence output: a phrase appearing in multiple scenarios is
  reported once per occurrence with its feature-file:line.
- --json / --edn pretty-printed (kebab-case both, mutually exclusive),
  matching the renderer contract from gherclj-nif.
- Exit code non-zero on findings.
- --color/--no-color, --help.

Schema
------
  {:gherclj-version   "1.0.0"
   :command           "ambiguity"
   :scenarios-scanned 42
   :tags-applied      {:include [] :exclude ["slow"]}
   :ambiguous-count   2
   :ambiguities
   [{:phrase       "a user \"alice\""
     :feature-file "ambiguous.feature"
     :line         3
     :matches      [<step entries — same shape as gherclj steps --edn>]}]}

Affected files
--------------
- src/gherclj/main.cljc — recognize "ambiguity" subcommand; route to new ns
- src/gherclj/ambiguity.clj — new namespace; build-data +
  render-text/render-json/render-edn (mirrors catalog.clj/unused.clj
  structure after gherclj-nif refactor); usage-message for --help
- src/gherclj/core.clj — extract a non-throwing classify-all helper
  returning the full match vector. classify-step (which throws on >1)
  built on top of it; ambiguity uses the raw helper directly
- src/gherclj/sample/ambiguous_steps.clj — fixture namespace with
  intentional template/template, template/regex, and duplicate-regex
  collisions
- spec/gherclj/features/steps/ — new step-defs:
  - "the :ambiguities list should contain an entry with phrase {string}"

Spec coverage (all currently @wip)
----------------------------------
features/ambiguous_steps.feature (10 scenarios)

## Design

Extract classification core that returns all matches without throwing.
Existing classify-step keeps its throw-on-multiple semantics, built
on top of the new helper, so generation behavior is unchanged.
gherclj ambiguity uses the raw helper directly to collect every
match per phrase.

Sort ambiguities by [feature-file line] for stable output.

Help text lives in src/gherclj/ambiguity.clj/usage-message — same
pattern as catalog/unused.

This explicitly does NOT introduce a `gherclj lint` umbrella. If
helper-discipline lint or broad-regex warnings land later, each is
its own leaf subcommand or a separate aggregator can be considered
once we have ~3 checks worth bundling.

## Acceptance Criteria

- gherclj ambiguity walks --features-dir, classifies each step phrase,
  reports every phrase with >1 match
- Per-occurrence reporting (a repeated phrase reports once per location)
- --tag filter respected; "scenarios-scanned" reflects post-filter count
- --json and --edn emit valid pretty-printed structured reports with
  the schema documented in the description
- --json --edn together exits non-zero with stderr message about
  mutual exclusivity
- Exit code non-zero when ambiguities found; zero on clean catalog
- Existing classify-step semantics unchanged (still throws at generation
  time on the first ambiguity)
- All 10 @wip scenarios in features/ambiguous_steps.feature pass with
  the @wip tag removed
- bb test passes (unit + features)
