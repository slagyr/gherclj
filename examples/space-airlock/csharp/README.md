# Space Airlock (C#)

Runs the shared space-airlock feature suite from `../features` against a
C# implementation.

The production logic lives in `src/SpaceAirlock/`. A small helper facade in
`src/SpaceAirlock.Steps/` exposes phrase-oriented methods that generated xUnit
tests call directly. The Clojure step definitions in `gherclj/airlock_steps.clj`
are pure routing entries: each phrase maps to a method call on the generated
test `subject`.

gherclj generates throwaway C# `*_test.cs` files and a temporary xUnit project
under `target/gherclj/generated/`, then runs them with `dotnet test`.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" subject.door-state)
```

and produces:

```csharp
subject.DoorState("inner", "open");
```

inside a generated `[Fact]` method. Per-scenario setup is declared in the same
step namespace via `xunit/scenario-setup!`.

## Running

You'll need the .NET SDK on your `PATH`.

```bash
bb features
```

`bb features` parses the shared `.feature` files, generates throwaway xUnit
tests under `target/gherclj/generated/`, writes a temp test project there, and
runs it with `dotnet test`.

## Layout

```text
examples/space-airlock/csharp/
  bb.edn                                      # gherclj runner task
  gherclj/airlock_steps.clj                   # Clojure step routing
  src/SpaceAirlock/SpaceAirlock.csproj        # production project
  src/SpaceAirlock/SpaceAirlock.cs            # production domain logic
  src/SpaceAirlock.Steps/SpaceAirlock.Steps.csproj
  src/SpaceAirlock.Steps/SpaceAirlockSteps.cs # helper facade for generated tests
  target/gherclj/generated/                   # generated *_test.cs + temp csproj
```

The generated C# files stay under `target/gherclj/`; the source tree stays clean.
