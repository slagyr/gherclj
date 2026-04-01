# Changes

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
