# Space Airlock (TypeScript)

Runs the shared space-airlock feature suite from `../features` against a
TypeScript implementation.

The production logic lives in `src/space_airlock.ts`. A thin TypeScript
test-helper facade in `src/space_airlock_steps.ts` exposes phrase-oriented
methods that generated tests can call directly. The Clojure step definitions in
`gherclj/airlock_steps.clj` are pure routing entries: each phrase maps to a
method call on the generated test `subject`. gherclj generates TypeScript
`node:test` files into `target/gherclj/generated/` and runs them with
`npx tsx --test`.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" subject.door-state)
```

and produces:

```ts
subject.doorState('inner', 'open')
```

inside the generated `test(...)` block. The describe-block setup is declared in
the same step namespace via `typescript/describe-setup!`.

## Running

Install the TypeScript runner first:

```bash
npm install
```

Run from this directory with Babashka:

```bash
bb features
```

`bb features` generates TypeScript tests into `target/gherclj/generated/` and
then runs them with `npx tsx --test`.
