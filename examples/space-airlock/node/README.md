# Space Airlock (Node)

Runs the shared space-airlock feature suite from `../features` against a
plain JavaScript implementation.

The production logic lives in `src/space_airlock.js`. The same file also
exports a small `AirlockChecks` helper for phrase-level assertions. The
Clojure step definitions in `gherclj/airlock_steps.clj` are pure routing:
behavioral steps call methods on `airlock`, and `Then` steps call methods on
`checks`. gherclj generates native `node:test` files into
`target/gherclj/generated/` and runs them with `node --test`.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" airlock.door-state)
```

and produces:

```js
airlock.doorState('inner', 'open')
```

inside the generated `test(...)` block. Per-scenario setup is declared in the
same step namespace via `js/scenario-setup!`:

```clojure
(js/scenario-setup! "const airlock = new space_airlock.SpaceAirlock()")
(js/scenario-setup! "const checks = new space_airlock.AirlockChecks(airlock)")
```

## Running

This example uses only built-in Node support. With Node 20+ on your `PATH`:

```bash
bb features
```

`bb features` generates JavaScript tests into `target/gherclj/generated/` and
then runs them with `node --test`.
