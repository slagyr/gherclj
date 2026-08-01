---
# gherclj-sqp
title: 'classify-step: type-aware classification (foundational)'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-26T18:52:31Z
updated_at: 2026-04-27T13:54:35Z
---

## Migrated from beads

- Original bead id: `gherclj-sqp`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-27T13:54:35Z
- Close reason: Closed

## Description

Why
---
classify-step (src/gherclj/core.clj:176-195) ignores the Gherkin keyword
on parsed step nodes — it matches by regex only, across all registered
steps regardless of type. As a result two stepdefs registered with the
same phrase but different types (e.g. Given "the user logs in" and When
"the user logs in") falsely throw ambiguous-match at generation time,
even though the Given gets the Given stepdef and the When gets the When
stepdef. Nobody's hit it because authors use distinct phrasings, but
the foundation is lossy and blocks correct semantics for downstream
diagnostics:

- gherclj match (the new command) needs typed classification to give
  accurate answers for typed input ("Given a user X").
- gherclj ambiguity (gherclj-vz7) currently would report cross-type
  same-phrase pairs as ambiguous when they aren't.

What
----
- classify-step signature changes from (steps text) to (steps type text).
  Only steps where (:type %) = type are considered. Throws on >1 within
  that type. Returns nil if zero match.
- Generator threads the parsed step's keyword through to classify-step.
  And/But resolve to the prior step's type within the same scenario
  (the parser may already do this; if not, generator handles it).
- step-namespaces-used (generator.clj) updates similarly.

Affected files
--------------
- src/gherclj/core.clj — classify-step new signature
- src/gherclj/generator.clj — classify-scenario passes (:keyword node)
  → type; resolve And/But to prior type; step-namespaces-used updated
- spec/gherclj/core_spec.clj — update existing classify-step unit tests
  for new signature; add a same-phrase-cross-type test
- spec/gherclj/features/steps/ (wherever step-pattern step-defs live) —
  three new step-defs:
    - "matching {phrase} as {type} finds {step-name}"
    - "matching {phrase} as {type} is ambiguous"
    - "matching {phrase} as {type} finds nothing"

Spec coverage (all currently @wip)
----------------------------------
features/steps/step_classification.feature (3 scenarios)

## Design

Smallest change that does it: filter the steps collection by :type
inside classify-step before running the regex matches. No new data
structure, no separate per-type registry. Existing single-throw-on-
ambiguous logic is preserved (now within a single type bucket).

For And/But: the parser may already replace these with the prior
step's keyword on the node. If not, generator's classify-scenario
walks scenario steps in order, tracking the last concrete keyword
(Given/When/Then) and using it as the type for any And/But it sees.

The ambiguity report bead (gherclj-vz7) gets corrected semantics
for free once this lands — it'll filter by type just like runtime.

## Acceptance Criteria

- classify-step takes (steps type text); only matches steps of the
  given type
- Same phrase registered as Given and When in the same namespace works
  in a feature that uses both (no ambiguity at generation time)
- Same phrase registered twice within one type still throws
  ambiguous-match
- A phrase with no matching step of the requested type returns nil
- Generator passes (:keyword node) through; And/But resolve to the
  prior step's keyword within the scenario
- All 3 @wip scenarios in features/steps/step_classification.feature
  pass with @wip removed
- Existing bb test passes (unit + features); no regression in
  generator behavior or sample-namespace classification
