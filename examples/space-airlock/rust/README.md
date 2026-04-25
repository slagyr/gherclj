# Space Airlock (Rust)

Runs the shared space-airlock feature suite from `../features` against a
Rust implementation.

The production logic lives in `src/space_airlock.rs`. A thin Rust test-helper
facade in `src/space_airlock_steps.rs` exposes phrase-oriented methods that the
generated tests call directly. The Clojure step definitions in
`gherclj/airlock_steps.clj` are pure routing entries: each phrase maps to a
method call on the generated test `subject`.

gherclj generates throwaway Rust `*_test.rs` files into
`target/gherclj/generated/` and runs them by compiling each file with
`rustc --test`, outputting temporary binaries into `target/gherclj/`.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" subject.door-state)
```

and produces:

```rust
subject.door_state("inner", "open");
```

inside the generated `#[test]` function. Per-scenario setup is declared in the
same step namespace via `rust/scenario-setup!`.

## Running

You'll need a Rust toolchain with `rustc` on your `PATH`.

```bash
bb features
```

`bb features` parses the shared `.feature` files, generates Rust tests into
`target/gherclj/generated/`, compiles them with `rustc --test`, and runs the
resulting binaries.

## Layout

```text
examples/space-airlock/rust/
  Cargo.toml                       # normal Rust package metadata
  bb.edn                           # gherclj runner task
  src/lib.rs                       # crate module declarations
  src/space_airlock.rs             # production code
  src/space_airlock_steps.rs       # Rust test-helper facade
  gherclj/airlock_steps.clj        # Clojure step routing
  target/gherclj/generated/        # generated *_test.rs (throwaway)
```

The generated Rust files stay under `target/gherclj/`; production code under
`src/` remains clean.
