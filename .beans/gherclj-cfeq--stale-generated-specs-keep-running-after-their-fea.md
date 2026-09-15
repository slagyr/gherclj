---
# gherclj-cfeq
title: Stale generated specs keep running after their feature is deleted or renamed
status: completed
type: task
priority: normal
tags:
    - unverified
created_at: 2026-09-15T23:36:18Z
updated_at: 2026-09-15T23:39:34Z
---

## Problem

`main/run` calls `pipeline/run!` (regenerate) then `fw/run-specs`, which executes what is in `target/gherclj/generated`. Nothing removes a generated spec whose source `.feature` no longer exists: `emit-spec-for-ir!` deletes a file only while regenerating that same IR when it has no scenarios (pipeline.clj:141). So deleting or renaming a feature leaves its generated spec behind and the suite keeps running it forever.

Observed 2026-09-15 while working gherclj-83m0: a temporary `features/parsing/zz_tmp_fail.feature` was deleted, but `target/gherclj/generated/parsing/zz_tmp_fail_spec.clj` survived; `bb features` kept reporting `244 examples, 1 failures` and exit 1 with the feature gone. Deleting the orphan file by hand restored `243 examples, 0 failures`, exit 0.

Consequences: ghost scenarios inflate counts, a deleted feature can fail CI forever, and a renamed feature runs twice (old name + new). `bb clean` fixes it only if you know to run it.

## Proposal

During `pipeline/run!`, after emitting this run's specs, delete generated spec files in `:output-dir` that no parsed feature accounts for. Safe because `run!` always parses every feature in `:features-dirs` — positional selectors only filter scenarios *within* each IR, so the orphan set is well defined even for a single-file run.

Constraints:
- Sweep only files gherclj generates, matched by the framework's own filename mapping (`source->spec-filename`); leave the other framework's files alone (`_spec.clj` and `_test.clj` coexist in the same tree).
- Leave `:edn-dir` IR files consistent with the same rule.

## Acceptance

- A generated spec whose `.feature` was deleted is removed on the next run and does not execute.
- Renaming a feature leaves only the new name's spec.
- A positional single-file selector run does not delete other features' specs.
- `bb test-all` green.
