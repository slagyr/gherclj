# Changes

## v1.0.0

- **BREAKING**: framework keywords are now namespaced — `:speclj` → `:clojure/speclj`, `:rspec` → `:ruby/rspec`, etc. Validation rejects the old un-namespaced forms
- **BREAKING**: `pipeline/run!` no longer persists `target/gherclj/edn/*.edn` by default. Pass `:ir-edn true` (or `--ir-edn` on the CLI) to opt back in. `parse!` and `generate!` are unchanged — they remain the explicit two-stage entry points and continue to read/write EDN
- **BREAKING**: step bodies are constrained to a single helper-ref. `defgiven`/`defwhen`/`defthen` accept `(template helper-ref [docstring])` only — no body, no arg vector. The macro builds the helper call mechanically from matched template args
- **Framework adapters**: `bash/testing`, `csharp/xunit`, `java/junit5`, `javascript/node-test`, `python/pytest`, `rust/rustc-test`, `typescript/node-test` (joining the existing `clojure/speclj`, `clojure/test`, `go/testing`, `ruby/rspec`)
- **Multi-language space-airlock examples**: shared feature suite under `examples/space-airlock/features/` exercises native implementations in 11 languages, each producing idiomatic test code via the language's native runner with no gherclj runtime in production code
- **`helper!` macro**: declares per-step-namespace module imports. The active framework adapter interprets the value (Clojure symbol → `require`; Ruby path string → `require`; Go `"alias path"` string → import; Java FQN string → `import`)
- **Per-framework scenario setup hooks**: `gotest/scenario-setup!`, `junit5/scenario-setup!`, `rspec/describe-setup!` inject a setup line at the top of every generated scenario closure
- **`--ir-edn` CLI flag** and matching `:ir-edn` config key

## v0.9.0

- **Step docstrings**: `defgiven`, `defwhen`, and `defthen` accept an optional docstring between the template and arg vector; stored in the step registry alongside source file and line
- **`gherclj steps` subcommand**: lists all registered step definitions grouped by Given/When/Then, with phrase, source location, and docstring; supports `--given`/`--when`/`--then` type filters (additive), keyword positional filter, and `--no-color`; colorized by default
- **`gherclj unused` subcommand**: detects registered step definitions never referenced in any feature file; respects `-t` tag filters and reports exactly how many scenarios were scanned
- Step definitions moved from `src/` to `spec/` (gherclj's own feature test infrastructure)
- Reference step implementations in `gherclj.sample.app-steps` and `gherclj.sample.dragon-steps`

## v0.8.0

- Bare `.feature` paths on the CLI run every scenario in the file, and may be mixed with `file:line` selectors in one invocation
- Usage banner shows the current gherclj version, read from `resources/gherclj/VERSION`
- Usage text now documents feature targets (`file`, `file:line`) and shows defaults for `--features-dir`, `--edn-dir`, `--output-dir`, `--test-framework`
- Speclj bumped to 3.13.0

## v0.7.1

- `bb spec` and `bb features` no longer print `clojure.test` reporter noise during normal runs
- Scenario location selection ignores scenario-like lines that appear inside doc-strings

## v0.7.0

- Scenario location selection via `file:line` selectors, including multiple selectors in one invocation
- Step namespace globs now resolve from classpath roots, so globbed step discovery can find namespaces under `spec/`
- Grouped generated-file assertions in pipeline acceptance specs using `"{path}" should exist and:`
- `wip` is now treated like any other tag; default `~wip` behavior moved into the project tasks and aliases

## v0.6.1

- `gherclj.lifecycle` extracted into its own namespace; framework adapters call it directly
- `compile-template` refactored from `loop/recur` to `reduce` over literal/capture pairs
- Feature step namespaces excluded from coverage report (they are tests, not code under test)
- Test coverage improvements across clojure-test, pipeline, speclj, and template namespaces

## v0.6.0

- Lifecycle hooks for generated runs, features, and scenarios: `before-all`, `before-feature`, `before-scenario`, `after-scenario`, `after-feature`, `after-all`
- Tag-filtered generation skips empty Speclj files and removes stale generated specs when no scenarios survive filtering
- Speclj passthrough options now append to the default generated-spec runner args instead of replacing them
- Background-only step namespaces are included in generated requires
- `bb features-slow` task for running `@slow` feature specs with Speclj documentation output and profiling

## v0.5.0

- `@wip` tag always excluded unless explicitly included via `-t wip`
- Persists when other exclude tags are used (e.g. `-t ~slow` still excludes wip)
- Tag filtering documented in README
- Release process documented in AGENTS.md

## v0.4.0

- **Breaking**: `-t` is now tag filter, `-T` is test framework
- Tag CLI flag: `-t smoke` to include, `-t ~slow` to exclude (repeatable)
- `@smoke` and `@slow` tags on feature scenarios
- `bb gherclj` task for direct CLI access with passthrough args
- CLI help header with pronunciation and copyright
- LICENSE file (MIT)

## v0.3.0

- Recursive feature discovery — parser finds `.feature` files in subdirectories
- Generated specs preserve directory structure with matching namespaces
- Organized feature files into screaming architecture (parsing/, generation/, steps/, pipeline/)

## v0.2.0

- Framework passthrough options via `--` CLI separator and `:framework-opts` config key
- `bb feature-docs` task for speclj documentation reporter
- Removed redundant `context` wrapper from speclj generated code
- AI Agent Skills section in README

## v0.1.0

Initial release.

- **Pipeline**: `.feature` files → EDN IR → generated test specs
- **Step macros**: `defgiven`, `defwhen`, `defthen` with template pattern matching
- **Template types**: `{name}` (word), `{name:string}` (greedy), `{name:int}`, `{name:float}`
- **Raw regex**: escape hatch for complex step patterns
- **Frameworks**: `:clojure/speclj` and `:clojure/test` output formats with `run-specs` multimethod
- **State management**: `g/assoc!`, `g/get`, `g/swap!`, `g/update!`, `g/reset!` and friends in `gherclj.core`
- **Assertions**: framework-agnostic `g/should=`, `g/should`, etc. dispatched to active framework
- **Config**: `gherclj.edn` file with schema validation and defaults via c3kit/apron
- **CLI**: `gherclj.main/-main` with `-f`, `-e`, `-o`, `-s`, `-t`, `-v`, `-h` flags
- **Step discovery**: glob patterns in `:step-namespaces` (e.g. `"myapp.features.steps.*"`)
- **Tags**: parsed into IR, include/exclude filtering, `@wip` convention
- **Scenario Outline**: expanded to concrete scenarios at parse time
- **Doc-strings**: parsed as step arguments, similar to tables
- **Background**: steps injected into every scenario
- **Error reporting**: malformed features and ambiguous step matches
- **Babashka**: runs natively in bb, `.cljc` reader conditionals for platform-specific code
- **Mutation testing**: integrated via clj-mutate
