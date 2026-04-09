# Changes

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
- **Frameworks**: `:speclj` and `:clojure.test` output formats with `run-specs` multimethod
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
