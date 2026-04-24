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
language. Clojure step definitions generate Python, Ruby, Go, or whatever
implementation language is in use.

The intermediate representation (IR) files are what provide value to the AIs.

## Implementations

- [Clojure](./clojure/)

Each implementation directory contains:
- The production code in that language
- Clojure step definitions (using `gherclj`) that invoke the production code
- A `bb.edn` and `gherclj.edn` for running the suite
- A `README.md` explaining setup and invocation
