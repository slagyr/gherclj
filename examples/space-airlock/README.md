# Space Airlock Example

This example demonstrates `gherclj` testing production code written in different
languages against a shared Gherkin contract.

The shared contract lives in:

- `features/`

Those `.feature` files describe a playful but deterministic domain: a station
airlock with authorization checks, pressure rules, safety interlocks, and
emergency override behavior.

## The key point

**`gherclj` is the runner for every implementation.** The step definitions are
always written in Clojure, but just enough to map to code written in the desired language. 

Each implementation keeps its native idioms. Production code never imports gherclj; generated tests run on the language's native test runner (JUnit, go test, RSpec, pytest, etc.) with no gherclj runtime. gherclj's footprint on each project is just bb.edn and a gherclj/ directory of step routing — delete those and you're left with a normal Java/Go/Ruby/etc. project.

## Specifying the framework

Each implementation tells `gherclj` which test framework to generate for via
the `-F` (or `--framework`) flag. Framework names are namespaced
`<language>/<runner>` keywords. Looking at `examples/space-airlock/java/bb.edn`:

```clojure
(main/-main "-f" "../features"
            "-s" "gherclj.airlock-steps"
            "-F" "java/junit5")
```

Available frameworks:

| Framework              | Generated test runner       |
|------------------------|-----------------------------|
| `clojure/speclj`       | speclj                      |
| `clojure/test`         | `clojure.test`              |
| `ruby/rspec`           | RSpec                       |
| `python/pytest`        | pytest                      |
| `go/testing`           | Go's `testing` package      |
| `java/junit5`          | JUnit 5 (Jupiter) + Maven   |
| `javascript/node-test` | Node's built-in `node:test` |
| `typescript/node-test` | Node `node:test` (via tsx)  |
| `rust/rustc-test`      | `cargo test`                |
| `csharp/xunit`         | xUnit + `dotnet test`       |
| `bash/testing`         | shell-based assertions      |

Alternatively, drop a `gherclj.edn` at the project root with
`{:framework :java/junit5 ...}` and omit the `-F` flag. CLI flags override
file config.

## Implementations

- [Bash](./bash/)
- [Clojure](./clojure/)
- [Node](./node/)
- [Ruby](./ruby/)
- [TypeScript](./typescript/)
- [Go](./go/)
- [Java](./java/)
- [Python](./python/)
- [Rust](./rust/)
- [C#](./csharp/)

Each implementation directory contains:
- The production code in that language
- Clojure step definitions (using `gherclj`) that invoke the production code
- A `bb.edn` or equivalent runner configuration for the suite
- A `README.md` explaining setup and invocation
