---
# gherclj-yti
title: 'Step catalog: filtering'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-24T03:03:31Z
updated_at: 2026-04-24T14:05:17Z
blocked_by:
    - gherclj-56o
---

## Migrated from beads

- Original bead id: `gherclj-yti`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-24T14:05:17Z
- Close reason: Closed

## Description

Add keyword and type filters to the step catalog.

Why: With many step namespaces a full catalog is noisy. Filtering lets agents narrow to exactly the steps they care about.

What:
- Optional keyword positional arg: 'gherclj steps user' shows only steps whose phrase or docstring contains 'user'
- --given flag: include only Given steps in output
- --when flag: include only When steps in output
- --then flag: include only Then steps in output
- Flags are additive: --given --when shows Given + When; no flags shows all types

Feature: features/step_catalog.feature (keyword filter + type filter scenarios)

## Notes

Verification failed: features/step_catalog.feature includes an unauthorized acceptance edit in commit 79f428b, which reworded the keyword-filter scenario from 'steps user' to 'steps name' and changed what the scenario was asserting. Commit b5c8536 later restored the contract, but verification stops at the first unauthorized feature-file edit in the bead's referenced feature history.
