# Space Airlock (Bash)

Runs the shared space-airlock feature suite from `../features` against a
Bash implementation.

The stateful domain logic lives in `lib/space_airlock.sh`. Phrase-level
assertions live in `lib/airlock_checks.sh`. The Clojure step definitions in
`gherclj/airlock_steps.clj` are pure routing: behavioral steps call
`airlock_*` functions, and `Then` steps call `checks_*` functions. gherclj
generates Bash `*_test.sh` files into `target/gherclj/generated/` and runs
them with `bash`.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" "airlock.door-state")
```

and produces:

```bash
airlock_door_state 'inner' 'open'
```

inside a generated scenario function. Per-scenario setup is declared in the
same step namespace via `bash/scenario-setup!`:

```clojure
(bash/scenario-setup! "airlock_reset")
```

## Running

This example only needs Bash on your `PATH`.

```bash
bb features
```

`bb features` generates Bash tests into `target/gherclj/generated/` and then
runs them with `bash`.
