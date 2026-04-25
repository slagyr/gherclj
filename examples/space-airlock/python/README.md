# Space Airlock (Python)

Runs the shared space-airlock feature suite from `../features` against a
Python implementation.

The production logic lives in `space_airlock.py`. The Clojure step
definitions in `gherclj/airlock_steps.clj` are pure routing — each entry
maps a Gherkin phrase to a method call on `airlock` or `checks`. gherclj generates Python
pytest files into `target/gherclj/generated/` and runs them.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" airlock.door-state)
```

and produces:

```python
airlock.door_state('inner', 'open')
```

inside the generated `test_...` function. The per-scenario setup
creates both the production object and a dedicated assertion helper:

```python
airlock = SpaceAirlock()
checks = AirlockChecks(airlock)
```

## Running

Create a local virtualenv and install pytest first:

```bash
python3 -m venv .venv
./.venv/bin/python -m pip install -r requirements.txt
```

Run from this directory with Babashka:

```bash
bb features
```

`bb features` generates Python tests into `target/gherclj/generated/` and
then runs them with `python -m pytest`.
