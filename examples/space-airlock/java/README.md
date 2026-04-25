# Space Airlock (Java)

Runs the shared space-airlock feature suite from `../features` against a
Java implementation.

The airlock state machine — pure production code, no test-framework
dependency — lives in `src/main/java/airlock/SpaceAirlock.java`. A thin
facade under `src/test/java/airlock/SpaceAirlockHelper.java` delegates
Givens and Whens to the airlock and turns its accessors into JUnit
assertions for the Thens. Both classes — and the generated tests — share
package `airlock`.

The Clojure step definitions in `gherclj/airlock_steps.clj` are pure
routing — each entry maps a Gherkin phrase to a method call on a
per-scenario `SpaceAirlockHelper`. gherclj generates Java `*Test.java`
files into `target/gherclj/generated/airlock/` and Maven's Surefire runs
them through JUnit 5.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" airlock.doorState)
```

and produces:

```java
airlock.doorState("inner", "open");
```

inside an `@Test` method. Helper-ref names pass through as-is, so write
them in the Java identifier style you want. Kebab-case still works —
`door-state` would also produce `doorState` — but the step files in
this example use camelCase to match Java conventions.

Per-scenario setup is declared in the same step namespace via
`junit5/scenario-setup!`:

```clojure
(junit5/scenario-setup! "SpaceAirlockHelper airlock = new SpaceAirlockHelper();")
```

Each `@Test` method body starts with that line, giving every scenario a
fresh helper.

## Running

You'll need Java 17+ and Maven 3.9+ on your `PATH`.

```bash
bb features
```

`bb features` parses the shared `.feature` files, generates `*Test.java`
files into `target/gherclj/generated/airlock/`, then runs `mvn test`.

## Maven layout

JUnit 5 expects test sources at `src/test/java`. Maven only takes one
test source root by default, so the `pom.xml` uses
`build-helper-maven-plugin` to register `target/gherclj/generated` as an
additional test source root during `generate-test-sources`. Surefire
picks the generated classes up automatically.

```xml
<execution>
  <id>add-gherclj-tests</id>
  <phase>generate-test-sources</phase>
  <goals><goal>add-test-source</goal></goals>
  <configuration>
    <sources>
      <source>target/gherclj/generated</source>
    </sources>
  </configuration>
</execution>
```

## Layout

```
examples/space-airlock/java/
  pom.xml                                       # JUnit 5 + build-helper plugin
  bb.edn                                        # gherclj runner task
  src/main/java/airlock/
    SpaceAirlock.java                           # production state machine
  src/test/java/airlock/
    SpaceAirlockHelper.java                     # JUnit-flavored facade
  gherclj/airlock_steps.clj                     # step routing
  target/gherclj/generated/airlock/             # generated *Test.java (gitignore-able)
```

The Java code in `src/main/java/` doesn't import gherclj, JUnit, or
anything else test-related. gherclj's footprint is `bb.edn` plus the
`gherclj/` directory.
