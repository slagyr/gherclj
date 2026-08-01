---
# gherclj-dvl
title: 'gherclj match: classify a step phrase against the registry'
status: completed
type: feature
priority: normal
tags:
    - "migrated-from-beads"
created_at: 2026-04-27T13:54:57Z
updated_at: 2026-04-27T14:11:08Z
blocked_by:
    - gherclj-sqp
---

## Migrated from beads

- Original bead id: `gherclj-dvl`
- Created by: Micah
- Owner: micahmartin@gmail.com
- Closed at: 2026-04-27T14:11:08Z
- Close reason: Closed

## Description

Why
---
Agents and humans authoring features need to understand how a phrase
classifies before writing it: does it match an existing step, which
one, what args get extracted, where does it route? Today the only way
to find out is to grep step namespaces and compile templates by eye,
or run the full pipeline and read the generation error.

`gherclj match "<phrase>"` answers the question directly — given a
phrase, show the matched step (type, pattern, source, helper-ref,
docstring) and the extracted args paired with binding names and types.
Read-only inspection; exits zero in all outcomes (matched / no-match /
ambiguous) and the structured output's :match-status carries the
verdict.

What
----
- New `gherclj match "<phrase>"` subcommand. Phrase as positional arg
  (everything after `match` joined with single spaces).
- Leading Given / When / Then narrows matching to that type.
- Leading And / But, or no leading keyword, matches across all three
  types — same-phrase pairs across types are not ambiguous.
- :match-status is :matched / :no-match / :ambiguous. Ambiguous means
  ≥2 matches within a single type bucket. (Cross-type same-phrase pairs
  are :matched, with one entry per type.)
- --json / --edn pretty-printed (kebab-case both, mutually exclusive),
  matching the renderer contract from gherclj-nif.
- --color / --no-color, --help.
- Exit zero in all three outcomes; status carried in output.

Schema
------
  {:gherclj-version "1.0.0"
   :command         "match"
   :phrase          "a user \"alice\""
   :requested-type  :given             ; :given / :when / :then / :any
   :match-status    :matched           ; :matched / :no-match / :ambiguous
   :matches
   [{:type :given
     :phrase "a user {name:string}"
     :regex false
     :ns "gherclj.sample.app-steps"
     :file "app_steps.clj" :line 14
     :helper-ref "app-steps/create-adventurer"
     :doc nil
     :bindings [{:name "name" :type "string" :value "alice"}]}]}

Step entry shape mirrors `gherclj steps --edn` plus :value on each
binding (`match` is the only place values exist).

Affected files
--------------
- src/gherclj/main.cljc — recognize "match" subcommand; route the
  positional phrase (join subcommand-args with single spaces).
- src/gherclj/match.clj — new namespace; parse leading keyword, route
  to typed or untyped classification, build-data + render-text /
  render-json / render-edn (mirrors catalog.clj/unused.clj/ambiguity.clj).
- src/gherclj/sample/same_phrase_steps.clj — new fixture: a Given and
  a When registered with the same phrase ("the user logs in").
- spec/gherclj/features/steps/ — new step-defs as needed:
  - "the :matches list should contain an entry with phrase {string}"

Spec coverage (all currently @wip)
----------------------------------
features/match.feature (10 scenarios)

## Design

Phrase parsing: split on first whitespace to peek the leading token. If
it's Given/When/Then, requested-type = that type, phrase = rest. If
it's And/But or anything else, requested-type = :any, phrase = full
input.

Classification: typed → core/classify-all filtered by type. Untyped →
core/classify-all run three times (one per type) and bucketed.
classify-all already exists (extracted in commit 85c063d for the
ambiguity report). Once gherclj-sqp lands, classify-all may take a
type arg directly; this command will adapt.

Status:
- typed:  0 matches → :no-match;  1 → :matched;  ≥2 → :ambiguous
- untyped: roll up across types. If any type bucket has ≥2 → :ambiguous.
  Otherwise (all ≤1) → :matched if any have 1, :no-match if all zero.

Text rendering branches on :match-status and :requested-type. Untyped
matched output groups by type with per-type sub-reports.

bindings :value field added by zipping classified-step :args with
:bindings. (:args from classify-all is positional.)

## Acceptance Criteria

- gherclj match "<Given/When/Then phrase>" filters classification to
  that type
- gherclj match "<And/But/no-keyword phrase>" classifies across all
  three types; cross-type same-phrase pairs are :matched, not :ambiguous
- :match-status :matched / :no-match / :ambiguous as defined above
- Exit code zero in all three outcomes
- Single-match text output shows pattern, source (file:line), helper-ref,
  doc, and args paired with binding name + type + value
- Untyped multi-type-match output groups results under per-type headers
- Within-type :ambiguous output lists every candidate
- --json and --edn emit valid pretty-printed structured reports
  conforming to the schema in the description; :bindings entries
  include :value
- --json and --edn together exits non-zero with stderr message about
  mutual exclusivity (color flags suppressed in machine-output mode)
- gherclj.sample.same-phrase-steps fixture created and used by the
  untyped scenarios
- All 10 @wip scenarios in features/match.feature pass with the @wip
  tag removed
- bb test passes (unit + features)
