---
# gherclj-cfeq
title: Stale generated specs keep running after their feature is deleted or renamed
status: in-progress
type: task
priority: normal
created_at: 2026-09-15T23:36:18Z
updated_at: 2026-09-16T00:09:44Z
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



## Verification failed

HEAD: 4930af23af1a8fda0812bf49d63c7639a4c87105
Working tree: clean

Acceptance criterion 3 is not met: a positional single-file selector run deletes other features' specs.

`sweep-orphaned-specs!` correctly keys the expected set off *all* parsed IRs, but `run!` still calls `emit-spec-for-ir!` on the *filtered* IR. `filter-ir-by-locations` sets `:scenarios []` for files not in the selector; `generate-spec` returns nil; emit then `.delete`s that spec. Sweep never gets a chance to keep it.

Reproduced at HEAD against two features in a temp dir (step namespaces `gherclj.pipeline-spec`):

- after `run!` with no `:locations`: `keep_spec.clj` and `other_spec.clj` both exist
- after `run!` with `:locations [{:source "keep.feature"}]`: `keep_spec.clj` exists, `other_spec.clj` is gone

The new specs cover delete-orphan and leave-the-other-framework-alone. They do not cover a selector run with two features. Rename is the same path as delete (old file gone) and is fine.

Secondary: `:edn-dir` is not swept. Constraint said leave IR files consistent with the same rule; only `:output-dir` is cleaned.

bb spec / bb features / bb test-all were green. Do not treat that as a pass of criterion 3.

Fix: skip emit (or skip delete) for IRs emptied only by the selector; sweep should remain the orphan cleaner.
