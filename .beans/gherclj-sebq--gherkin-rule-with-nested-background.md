---
# gherclj-sebq
title: Gherkin Rule with nested Background
status: in-progress
type: feature
priority: high
created_at: 2026-08-01T17:28:41Z
updated_at: 2026-08-01T17:28:41Z
---

## Goal

Support Gherkin `Rule:` blocks so a Feature can group scenarios under rules,
each with its own nested `Background:` that runs for scenarios in that rule.

## Cucumber semantics (target)

- `Feature` may contain zero or more `Rule`s (and/or top-level scenarios).
- A `Rule` may have its own `Background`.
- Feature-level `Background` still applies; for a scenario under a Rule the
  effective background steps are feature background then rule background.
- Rule name is documentation/grouping; tags on Rule apply to its scenarios.
- Rule may have a description (like Feature).

## IR shape (proposed)

Option A — flatten at parse time (simplest for generators):
```clojure
{:feature "..."
 :background {:steps [...]}  ; feature-level only
 :scenarios [{:scenario "..."
              :rule "Rule name"      ; optional
              :rule-line N
              :background-steps [...] ; merged feature+rule backgrounds already applied?
              :steps [...]}]}
```

Option B — nested rules in IR:
```clojure
{:feature "..."
 :background {...}
 :rules [{:rule "..."
          :line N
          :background {...}
          :scenarios [...]}]
 :scenarios [...]  ; top-level only
}
```

Prefer **B in IR for fidelity**, with generator **merging backgrounds** when rendering
so adapters keep a single `:background` argument:
effective-bg = concat(feature-bg steps, rule-bg steps).

## Acceptance

- Parse Rule with name, optional description, nested Background, scenarios/outlines
- Feature Background + Rule Background both apply in order to rule scenarios
- Top-level scenarios (no Rule) unchanged
- Tags on Rule merge into scenarios (like feature tags)
- Line numbers on Rule and rule Background steps
- Generated specs include rule background steps before scenario steps
- Dogfood feature scenarios in features/parsing/ (and generation if needed)
- bb features + unit specs green

## Non-goals (unless free)

- Rule-level hooks API
- Changing polyglot adapter signatures beyond receiving merged background
