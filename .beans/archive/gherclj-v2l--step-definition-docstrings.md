---
# gherclj-v2l
title: Step definition docstrings
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-24T03:00:59Z
updated_at: 2026-04-24T14:43:48Z
---

## Migrated from beads

- Original bead id: `gherclj-v2l`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-24T14:43:48Z
- Close reason: Closed

## Description

Add optional docstring support to defgiven, defwhen, and defthen macros.

Why: Agents rediscover step contracts every session by reading full source files. A docstring stored in the registry makes contracts visible in the step catalog without reading source.

What:
- Extend defgiven/defwhen/defthen to accept an optional docstring string between the template and arg vector: (defgiven name "phrase" "docstring" [args] body)
- Store :doc in the registry entry when present (nil when absent)
- Capture source :file and :line at macro expansion time; store in the registry entry
- Backward compatible — steps without docstrings continue to work unchanged

New fixture namespace: gherclj.features.steps.step-docstrings
  - fixture-no-doc (Given, no docstring, defined first)
  - fixture-with-doc (Given, with docstring "Sets :crew atom — does NOT write disk.", defined second)
  - fixture-async (When, docstring "Polls for up to 2s.")
  - fixture-check (Then, docstring "Matches within 2s timeout.")

New step definitions in step_docstrings.clj steps file:
  - 'the registered step {name} from docstring suite'
  - 'the step should have docstring {doc}'
  - 'the step should have no docstring'

Feature: features/steps/step_docstrings.feature
