# Enhancements

Potential enhancements for making `gherclj` more effective for agentic development.

The unifying theme: gherclj should be sharper at answering **what matched?**, **what didn't?**, **why?**, and **what will this change break?** Most items below ladder up to one of those four.

## Tier 1 — Highest Leverage

Each item was rated in the top 5 by both reviewers and matches daily friction during agent-driven work.

1. **`gherclj doctor --json`.**
   One command for "what is broken and why" — missing steps, ambiguous matches, unused steps, broken helper imports, framework/runtime issues. Replaces the current "run the suite, read 14 seconds of wall-of-text" pre-flight loop.

2. **`gherclj explain-step "<text>"`.**
   Given a step phrase, show the matched definition, extracted args, helper route, and rendered target call. Single biggest win for understanding existing code without grepping step files.

3. **Nearest-match suggestions for missing steps.**
   When a phrase doesn't match, surface the closest existing one. Directly attacks the step-duplication trap that comes up repeatedly when agents author features against an unfamiliar catalog.

## Tier 2 — Strong Candidates

The next set both agents rated meaningfully — different ordering between them, but each item earned a top-10 slot somewhere.

6. **`gherclj who-uses-step <phrase>`.**
   Reverse lookup: which scenarios depend on this step. Critical before mass renames or deletions, especially for parameterized/regex steps that aren't grep-discoverable.

7. **`gherclj trace <feature:line>`.**
   Full path from feature text → IR → matched steps → generated code. Earns its keep when a generated spec fails to compile and the resolution chain is opaque.

8. **`gherclj scaffold-missing`.**
   Generate routing stubs (and optionally helper stubs) for unmatched steps. Speeds the feature → routing pass on `@wip` features.

9. **Helper-discipline lint.**
   Warn when a step routes to a helper module not declared via `helper!`. Catches an entire class of subtle import bugs at parse time rather than at first invocation.

10. **`gherclj summarize-failures --json` and `list-pending --json`.**
    Fast triage after a run — what failed, what's still pending. Pairs with #1 to make CI-driven and agent-driven loops snappy.

## Machine-Readable Output

- Add `--json-summary` output to parse and generate workflows.
- Add `gherclj snapshot` to emit a single structured project-state file.

## Diagnostics

- Add `gherclj doctor`.
- Report missing step definitions.
- Report ambiguous step matches.
- Report unused steps and orphan helpers.
- Report broken helper imports.
- Report framework/runtime/toolchain issues.
- Report scenarios with zero assertions — Thens that route to helpers which don't actually assert.
- Report weak Then coverage — scenarios whose Thens only inspect raw output rather than domain-level state.

## Debugging And Traceability

- Add `gherclj explain-step "<step text>"`.
- Add `gherclj trace <feature-or-scenario>`.
- Add optional generated-code annotations showing feature file, scenario, and source step provenance.
- Add a dry-run debug bundle that writes parsed IR, classified IR, generation summaries, and generated code into a single target directory.
- Add `gherclj why-not <feature-or-scenario>` to explain why something didn't run — filtered by tag, `@wip`, pending due to unmatched step, ambiguous match, generation failure.
- Add `gherclj why-pending <feature:line>` for the focused case: explain why a generated scenario came out pending (no match, ambiguous, excluded by tag) — the targeted variant of `why-not`.
- Add `gherclj run --explain <file:line>` to run one scenario and print the matched steps, extracted args, and the location of any pending or failure.
- Add a direct debug mode that surfaces gherkin → matched-steps → runner output in one go without exposing intermediate artifacts unless explicitly requested.

## Discovery And Navigation

- Add reverse lookup for step usage, such as `gherclj who-uses-step`.
- Add feature coverage reporting for fully implemented, pending, ambiguous, and filtered scenarios.
- Add nearest-match suggestions for missing steps.
- Add namespace metadata for domain, framework, target language, and ownership.
- Add `gherclj tags` to inventory all tags across features with usage counts.
- Add parameter-value coverage for parameterized steps — show the distinct captured values that have actually been exercised across scenarios.
- Show example invocations alongside step phrases in `gherclj steps`, drawn from observed parameter values, so agents writing features see how a step is typically used.
- Add `gherclj steps --grep <keyword>` as a structured substring filter over phrase + docstring (the same content as today's positional keyword filter, but discoverable via `--help` and composable with other flags). Preserves the type grouping that `bb steps | grep` loses.
- Add `gherclj steps --orphan-helpers` to list `defn`s in a step namespace with no matching `defgiven`/`defwhen`/`defthen` registration — the symmetric bug to "unused steps", a function that should have been registered but wasn't.
- Add `gherclj namespace-graph <feature>` to show which step namespaces a feature touches — useful for understanding feature/namespace coupling and for splitting features.
- Support metadata-based filtering on the catalog once steps declare it: `gherclj steps --domain bridge --transport acp`.

## Authoring Support

- Add canonical step style checks.
- Warn on duplicate phrasing.
- Warn on overly broad regexes.
- Warn when `Then` routes are not assertion-oriented.
- Encourage or require docstrings for non-obvious steps.
- Add a formatter for `.feature` files.
- Warn on duplicate scenario titles within a feature — they collide on generated test method names in frameworks that derive method names from titles (JUnit, Go).
- Warn when a step routes to a helper module that isn't declared via `helper!` in the step namespace.
- Add custom parameter types (e.g., `{user-id:uuid}`, `{date:iso}`) with project-defined types registered in step namespaces, so validation moves up to gherclj and the catalog can show parameter shapes.
- Allow steps to declare metadata (domain, transport, framework, helper module, ownership) so the catalog and `who-uses-step` can filter and group by it. Pairs with namespace-level metadata for coarser cuts.
- Warn on hidden setup — step definitions that pull substantial implicit state when the feature text doesn't declare the seam, so the seam shows up in the Gherkin rather than disappearing into helpers.

## Generation And Refactoring

- Keep generated output deterministic and stable.
- Add clean per-scenario generation for tighter edit/verify loops.
- Add rename/refactor support for step routes and helper references.
- Add a change impact report showing which scenarios, languages, and frameworks are affected by a step or helper change.
- Add `gherclj diff <commit>` to compare the catalog (phrase + helper-ref per step) between two git refs, so a migration can prove every phrase that worked before still works.

## Maintenance

- Detect dead features and dead helpers, not just unused routes.
- Report ambiguity risk or regex complexity hotspots.
- Add auto-fix mode for simple cleanup tasks.

## Polyglot Support

- Add target capability manifests so each framework adapter declares its conventions explicitly.
- Add a new-target scaffolder to generate framework adapter skeletons, sample step namespaces, and generation fixtures.

## Workflow Commands

- Add agent-friendly commands like `gherclj next-task`.
- Add `gherclj summarize-failures --json`.
- Add `gherclj list-pending --json`.
- Add `gherclj watch` for live re-parse/re-classify feedback while an agent is iterating on `.feature` files or step namespaces — distinct from `scaffold-missing` since it's continuous rather than one-shot.
- Add `gherclj new-feature <path>` to scaffold a starter `.feature` file with `@wip` and the project's tag/structure conventions.
- Add `gherclj new-helper <ns>` to scaffold paired routing and helper namespace stubs with the right `helper!` declaration and requires — distinct from `scaffold-missing` (which fills in stubs for unmatched steps in an existing project) by being for greenfield additions.
