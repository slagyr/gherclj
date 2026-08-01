---
# gherclj-56o
title: 'Step catalog: core listing'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-24T03:03:27Z
updated_at: 2026-04-24T03:20:17Z
blocked_by:
    - gherclj-v2l
---

## Migrated from beads

- Original bead id: `gherclj-56o`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-24T03:20:17Z
- Close reason: Closed

## Description

Implement the 'gherclj steps' subcommand with formatted output.

Why: Agents need a fast way to see all registered steps without reading source files. This is the foundation of the catalog feature.

What:
- Detect 'steps' as first positional arg in main.cljc and dispatch to catalog mode instead of pipeline
- Load and require configured step namespaces (-s flag, same semantics as pipeline)
- Print steps grouped by type with headers: Given:, When:, Then:
- Each entry on one line: phrase + source location — 'phrase text  (filename.clj:N)'
- When docstring is present, print it indented on the next line
- 'gherclj steps --help' shows catalog-specific options (-s, --given, --when, --then, --color, --no-color)
- 'gherclj --help' updated to mention 'gherclj steps' subcommand and 'gherclj steps --help'
- Fix output-should-not-contain step to check :cli-output (currently only checks :generated-output)
- run-gherclj step must capture 'steps' output into :cli-output
- New step: 'the catalog output should include:' (doc-string, checks consecutive lines appear adjacent)

Feature: features/step_catalog.feature (format scenarios + help scenarios)
