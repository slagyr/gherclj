---
# gherclj-ky7
title: 'Step catalog: gherclj steps subcommand'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-24T03:01:30Z
updated_at: 2026-04-24T03:03:41Z
blocked_by:
    - gherclj-v2l
---

## Migrated from beads

- Original bead id: `gherclj-ky7`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-24T03:03:41Z
- Other dependency edges (not mapped to beans `blocked_by`):
  - `supersedes`: `gherclj-56o`

## Description

Add a 'steps' subcommand to gherclj that prints a formatted catalog of all registered step definitions.

Why: Agents and developers reinvent existing steps because there is no fast way to see what already exists. The catalog makes step discovery a one-command operation, reducing redundant step definitions.

What:
- New 'steps' subcommand detected when first positional arg is literally 'steps'
- Loads and requires configured step namespaces (-s flag, same semantics as pipeline mode)
- Prints steps grouped by Given/When/Then, each entry: phrase + (file.clj:line) on one line, optional docstring indented on next line
- --given/--when/--then flags select step types (additive; no flags = all types)
- Optional keyword positional arg filters by phrase or docstring content
- --color forces ANSI output; --no-color disables it (auto-detect TTY otherwise)
- 'gherclj steps --help' shows catalog-specific options
- 'gherclj --help' mentions the steps subcommand and 'gherclj steps --help'

Infrastructure changes:
- Fix output-should-not-contain step to check :cli-output (currently only checks :generated-output)
- run-gherclj step must capture 'steps' subcommand output into :cli-output
- New step definitions: 'the catalog output should include:' (doc-string, consecutive lines check), 'the output should have no color codes', 'the output should have color codes'
- sample-app.clj may need docstrings added to make catalog docstring scenarios meaningful

Feature: features/step_catalog.feature (and one scenario added to features/cli.feature)
