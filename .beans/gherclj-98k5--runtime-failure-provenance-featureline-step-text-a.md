---
# gherclj-98k5
title: 'Runtime failure provenance: feature:line, step text, and table cells'
status: completed
type: feature
priority: high
tags:
    - unverified
created_at: 2026-08-01T16:54:52Z
updated_at: 2026-08-01T17:04:10Z
---

## Problem

When a generated scenario fails, the report is opaque:
- stack traces point at generated specs and helpers, not the `.feature` file
- multi-step scenarios do not say which Gherkin step failed
- data-table failures do not identify row/column/cell or the feature line of the row

## Goal

A failure should answer:
1. Feature path + line of the step
2. Step text (`Then the users should be:`)
3. When table-driven: row index, column header, expected cell value, feature line of that row

Ideal shape:

```
FAILED: Then the users should be:
  at features/auth.feature:12
  table cell [row 2, col "role"] = "guest"
  at features/auth.feature:15
Expected: "guest"
     got: "member"
```

## Design

### Phase 1 — IR provenance + generated annotations
- Parser attaches `:line` to scenarios and steps
- Tables gain `:header-line` and `:row-lines` (rows stay vectors — back-compat)
- Generators emit provenance comments for every step (all frameworks) including compact table dump with lines
- Feature `:source` + step `:line` available through classification → render

### Phase 2 — Clojure failure enrichment
- Wrap each step call with context (clojure.test `testing`, speclj rethrow/message)
- `g/should-table=` compares cell-by-cell with row/col/line in the message
- `g/each-row` binds table row context so plain `g/should*` names the row
- Dynamic bindings compose: step context + table cell context

### Phase 3 (later / optional)
- Polyglot comment parity already in Phase 1; cell APIs stay Clojure first
- `summarize-failures` / `run --explain` deferred

## Acceptance Criteria

- [ ] IR includes `:line` on scenarios and steps; tables include `:header-line` and `:row-lines`
- [ ] Existing `:headers`/`:rows` consumers still work unchanged
- [ ] Generated speclj (and other frameworks) include `feature:line` and step text comments per step
- [ ] Table step comments include each row's feature line
- [ ] Assertion failure inside a step wrapper reports step text + feature:line
- [ ] `g/should-table=` mismatch reports row index, column header, and row feature line when available
- [ ] Specs + features covering provenance and table-cell messages pass
- [ ] `bb features` / unit suite green

## Non-goals

- Auto-expand tables into N generated calls
- Changing row shape away from vectors of strings
- Removing gherclj runtime from Clojure targets (already required)

## Implementation notes

- Parser currently drops lines: sections collect bare strings, then `parse-scenario-lines` loses original numbers — fix at the section boundary
- Scenario outlines: step line from outline; example values already in expanded title
- Prefer shared comment formatter used by all framework adapters



## Implementation notes (2026-08-01)

### Shipped
- Parser: `:line` on scenarios/steps; tables get `:header-line` and `:row-lines` (rows stay vectors)
- Generator: language-aware provenance comments on every step; Clojure wraps calls in `g/with-step*`
- Step-def `:file`/`:line` stored as `:def-file`/`:def-line` so they never overwrite feature provenance
- Core: `with-step*`, `with-step`, `should-table=`, `each-row` + dynamic `*step-context*` / `*table-context*`
- IR equality helpers use subset match so additive provenance fields don't break older fixtures

### Exceptions (feature golden updates for new annotations)
Authorized edits to expected generated code (comments + with-step wrappers only):
- features/generation/ir_to_code.feature
- features/generation/typescript_generation.feature
- features/generation/xunit_generation.feature
- features/generation/rust_generation.feature
- features/generation/bash_generation.feature
- features/generation/rspec_generation.feature
- features/generation/javascript_generation.feature
- features/generation/pytest_generation.feature
