---
# gherclj-41w
title: 'Unused step detection: gherclj unused subcommand'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-24T15:55:16Z
updated_at: 2026-04-24T16:18:42Z
---

## Migrated from beads

- Original bead id: `gherclj-41w`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-24T16:18:42Z
- Close reason: Closed

## Description

Add a 'unused' subcommand to gherclj that identifies registered step definitions never referenced in any feature file.

Why: Step namespaces accumulate dead code over time as features evolve. gherclj already has everything it needs — the step registry and the feature parser — making this a natural fit.

What:
- Detect 'unused' as first positional arg in main.cljc and dispatch to unused-check mode
- Parse all feature files in the configured features directory to collect every step text
- Apply tag filters when -t flags are present (same semantics as pipeline mode)
- Load and require configured step namespaces (-s flag)
- For each registered step, check if it matched any parsed step text
- Report unmatched steps grouped by Given/When/Then with phrase + (file.clj:N)
- Output format:
    No filter:    'Scanned N scenario(s). No tag filtering applied.'
    With filter:  'Scanned N scenario(s). M scenario(s) unscanned due to tag filters: <filters>.'
    No unused:    'No unused steps found.'
    With unused:  'N unused step(s) found.'
- New step definition: 'the output should contain lines:' — iterates table rows, checks each as substring of cli-output
- unused does NOT write EDN or generated specs — read-only analysis

Feature: features/unused_steps.feature
