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
always written in Clojure — but the production code they exercise can be in any
language. Clojure step definitions call into Python, Ruby, Go, or whatever the
implementation language is, typically via subprocess with JSON over stdin/stdout.

This is the unique advantage over language-specific BDD frameworks:

- One workflow, one tool, regardless of implementation language
- The same `gherclj steps` and `gherclj unused` commands work across all implementations
- AI agents learn one pattern and apply it everywhere
- The Gherkin contract is truly language-neutral — not just in theory

## Implementations

- [Clojure](./clojure/)

Each implementation directory contains:
- The production code in that language
- Clojure step definitions (using `gherclj`) that invoke the production code
- A `bb.edn` and `gherclj.edn` for running the suite
- A `README.md` explaining setup and invocation
