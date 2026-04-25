# Space Airlock (Go)

Runs the shared space-airlock feature suite from `../features` against a
Go implementation.

The production logic lives in `airlock/airlock.go`. The Clojure step
definitions in `gherclj/airlock_steps.clj` are pure routing — each entry
maps a Gherkin phrase to a method call on a per-scenario `airlock`.
gherclj generates Go `*_test.go` files into `target/gherclj/generated/`
and runs them with `go test`.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" airlock.door-state)
```

and produces:

```go
airlock.DoorState("inner", "open")
```

inside a `t.Run` block. Helper-ref names get kebab→PascalCase translated
on the way out (`door-state` → `DoorState`).

Per-scenario setup is declared in the same step namespace via
`gotest/scenario-setup!`:

```clojure
(gotest/scenario-setup! "airlock := airlock.NewSpaceAirlock(t)")
```

Each `t.Run` closure starts with that line, giving every scenario a
fresh `*SpaceAirlock` bound to the subtest's `*testing.T`. The local
`airlock` variable shadows the package import within the closure —
Go evaluates the RHS before the LHS binds, so this is well-defined.
After that line, `airlock` refers to the variable; we don't need the
package again inside the scenario.

## Running

You'll need Go 1.18+ on your `PATH`.

```bash
bb features
```

`bb features` parses the shared `.feature` files, generates `_test.go`
files into `target/gherclj/generated/`, then runs `go test` against
that directory.

## Go test options

Options are passed through `bb features` using the standard `--`
separator:

```clojure
;; bb.edn
:task (main/-main "-f" "../features"
                  "-s" "gherclj.airlock-steps"
                  "-F" "go/testing"
                  "--" "-v")
```

Anything after `--` is forwarded to `go test` verbatim. `-v` prints
each subtest's pass/fail line. Add `-run TestAirlockExit` to filter,
`-count=1` to disable result caching, etc.

## Layout

```
examples/space-airlock/go/
  bb.edn                          # gherclj runner task
  go.mod                          # module example.com/space_airlock
  airlock/airlock.go              # production code
  gherclj/airlock_steps.clj       # step routing
  target/gherclj/generated/       # generated *_test.go (gitignore-able)
```

The Go production code in `airlock/` doesn't import gherclj at all.
gherclj's footprint is `bb.edn` plus the `gherclj/` directory.
