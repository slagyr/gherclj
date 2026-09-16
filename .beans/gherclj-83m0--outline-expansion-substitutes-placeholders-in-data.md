---
# gherclj-83m0
title: Outline expansion substitutes placeholders in data tables and doc strings
status: completed
type: task
priority: normal
created_at: 2026-09-15T23:26:38Z
updated_at: 2026-09-16T00:10:07Z
---

## Problem

`expand-outline` (src/gherclj/parser.clj) substitutes `<placeholders>` only in each step's `:text`. A step's `:table` (headers and row cells) and `:doc-string` are copied verbatim, so an outline like

```gherkin
Scenario Outline: <provider> maps stop "<wire>"
  Then the last provider response matches:
    | key         | value      |
    | stop-reason | <expected> |

  Examples:
    | provider | wire     | expected  |
    | anthropic| end_turn | :end-turn |
```

compares against the literal string `<expected>`. Nor can a step definition fix it downstream: the expanded scenario carries only `{:scenario ... :steps [...]}` — the example row is not attached, so steps have nothing to substitute from.

Found while planning isaac-g71i (provider response schema, isaac repo), where per-API assertion tables are the natural shape and the workaround is an extra single-value step per assertion.

## Acceptance

- Placeholders in a step's data-table headers and row cells are substituted from the Examples row.
- Placeholders in a step's doc-string are substituted.
- Step `:text` substitution, tags, scenario naming, `:line`/`:header-line`/`:row-lines` provenance, and Rule fields are unchanged.
- `bb spec` and `bb features` green; `bb test-all` green before release.
- Dogfood scenarios in features/parsing/scenario_outline.feature.
