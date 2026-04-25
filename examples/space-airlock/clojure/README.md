# Space Airlock (Clojure)

This example uses the shared features in `../features` and implements the
airlock in Clojure with step definitions in `space-airlock.features.steps.airlock`.

Run the shared space-airlock feature suite from this directory with Clojure:

```bash
clojure -M -m gherclj.main -f ../features -s space-airlock.features.steps.airlock -F speclj
```

Or run it with Babashka:

```bash
bb features
```

Both commands execute the shared feature suite against the Clojure
implementation.
