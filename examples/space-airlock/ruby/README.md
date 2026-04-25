# Space Airlock (Ruby)

This example runs the shared space-airlock feature suite from `../features`
against a Ruby implementation.

The production logic lives in `lib/space_airlock.rb`.
gherclj classifies the shared features using the Clojure step definitions in
`gherclj/airlock.clj`. Those step definitions emit Ruby lines, and gherclj wraps
them in generated Ruby RSpec files.
Those generated specs require `lib/space_airlock.rb` and call `SpaceAirlock`
directly.

Install the Ruby dependencies first:

```bash
bundle install
```

Run the shared suite from this directory with Babashka:

```bash
bb features
```

This generates Ruby specs into `target/gherclj/generated` and then runs them
with RSpec.
