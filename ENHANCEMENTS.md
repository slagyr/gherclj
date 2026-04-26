# Enhancements

Potential enhancements for making `gherclj` more effective for agentic development.

## Top Five

1. Add machine-readable output for discovery commands.
   `gherclj steps --json`, `gherclj unused --json`, and similar flags would reduce parsing ambiguity for agents.

2. Add `gherclj doctor --json`.
   A single command should report missing steps, ambiguous matches, unused steps, broken generated files, and framework/runtime issues.

3. Add `gherclj explain-step`.
   Given a step string, show the matched definition, extracted args, helper route, and rendered target call.

4. Add `gherclj scaffold-missing`.
   Generate routing stubs for unmatched steps, and optionally helper stubs as well.

5. Add `gherclj trace`.
   Show the full path from feature text to parsed IR to matched steps to generated code.

## Machine-Readable Output

- Add `--json` output to `gherclj steps`.
- Add `--json` output to `gherclj unused`.
- Add `--json-summary` output to parse and generate workflows.
- Add `gherclj snapshot` to emit a single structured project-state file.

## Diagnostics

- Add `gherclj doctor`.
- Report missing step definitions.
- Report ambiguous step matches.
- Report unused steps and orphan helpers.
- Report broken helper imports.
- Report framework/runtime/toolchain issues.

## Debugging And Traceability

- Add `gherclj explain-step "<step text>"`.
- Add `gherclj trace <feature-or-scenario>`.
- Add optional generated-code annotations showing feature file, scenario, and source step provenance.
- Add a dry-run debug bundle that writes parsed IR, classified IR, generation summaries, and generated code into a single target directory.

## Discovery And Navigation

- Add reverse lookup for step usage, such as `gherclj who-uses-step`.
- Add feature coverage reporting for fully implemented, pending, ambiguous, and filtered scenarios.
- Add nearest-match suggestions for missing steps.
- Add namespace metadata for domain, framework, target language, and ownership.
- Add `gherclj tags` to inventory all tags across features with usage counts.
- Add parameter-value coverage for parameterized steps — show the distinct captured values that have actually been exercised across scenarios.
- Show example invocations alongside step phrases in `gherclj steps`, drawn from observed parameter values, so agents writing features see how a step is typically used.

## Authoring Support

- Add canonical step style checks.
- Warn on duplicate phrasing.
- Warn on overly broad regexes.
- Warn when `Then` routes are not assertion-oriented.
- Encourage or require docstrings for non-obvious steps.
- Add a formatter for `.feature` files.
- Warn on duplicate scenario titles within a feature — they collide on generated test method names in frameworks that derive method names from titles (JUnit, Go).
- Surface ambiguous step regex collisions statically as part of `gherclj lint`/`doctor`, instead of only at run time when classification fails.
- Warn when a step routes to a helper module that isn't declared via `helper!` in the step namespace.
- Add custom parameter types (e.g., `{user-id:uuid}`, `{date:iso}`) with project-defined types registered in step namespaces, so validation moves up to gherclj and the catalog can show parameter shapes.

## Generation And Refactoring

- Keep generated output deterministic and stable.
- Add clean per-scenario generation for tighter edit/verify loops.
- Add rename/refactor support for step routes and helper references.
- Add a change impact report showing which scenarios, languages, and frameworks are affected by a step or helper change.

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
