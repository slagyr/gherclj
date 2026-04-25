# Space Airlock (Ruby)

Runs the shared space-airlock feature suite from `../features` against a
Ruby implementation.

The production logic lives in `lib/space_airlock.rb`. The Clojure step
definitions in `gherclj/airlock_steps.clj` are pure routing — each entry
maps a Gherkin phrase to a method call on the rspec `subject`. gherclj
generates Ruby RSpec files into `target/gherclj/generated/` and runs them.

A representative step def reads:

```clojure
(defgiven "the {door} door is {state}" subject.door-state)
```

and produces:

```ruby
subject.door_state('inner', 'open')
```

inside the generated `it` block. The describe-block setup
(`subject { SpaceAirlock.new }`) is declared in the same step namespace
via `rspec/describe-setup!`.

## Running

Install the Ruby dependencies first:

```bash
bundle install
```

Run from this directory with Babashka:

```bash
bb features
```

`bb features` generates Ruby specs into `target/gherclj/generated/` and
then runs them with `bundle exec rspec`.

## RSpec options

RSpec options are passed through `bb features` using the standard `--`
separator:

```clojure
;; bb.edn
:task (main/-main "-f" "../features"
                  "-s" "gherclj.airlock-steps"
                  "-F" "rspec"
                  "--" "--format" "documentation" "--color")
```

Anything after `--` is forwarded to `bundle exec rspec` verbatim. To
change formatters, color, etc., edit the task.

A traditional `.rspec` file at the project root would also be picked
up by RSpec automatically — that's an equivalent alternative if you'd
rather keep options out of `bb.edn`.
