# gherclj

## Toolbox

This project uses [toolbox](https://raw.githubusercontent.com/slagyr/toolbox/main/SKILL.md)
to manage agent components. If `.toolbox/` doesn't exist, fetch the toolbox
SKILL.md from the URL above and follow its instructions. Once bootstrapped:

- **Skills:** Load from `.toolbox/skills/{name}/SKILL.md` when their descriptions match the task at hand.
- **Commands:** When the user invokes a command by name (e.g., "/plan"), read and follow `.toolbox/commands/{name}.md`.

### Skills

- [tdd](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/tdd/SKILL.md)
- [crap4clj](https://raw.githubusercontent.com/unclebob/crap4clj/master/SKILL.md)
- [clj-mutate](https://raw.githubusercontent.com/slagyr/clj-mutate/master/SKILL.md)
- [speclj-structure-check](https://raw.githubusercontent.com/unclebob/speclj-structure-check/master/.claude/skills/speclj-structure-check/SKILL.md)
- [gherclj](https://raw.githubusercontent.com/slagyr/agent-lib/main/skills/gherclj/SKILL.md)

### Commands

- [plan](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan.md)
- [todo](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/todo.md)
- [work](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/work.md)
- [plan-with-features](https://raw.githubusercontent.com/slagyr/agent-lib/main/commands/plan-with-features.md)

## Releasing a New Version

When the user asks to bump/tag/release a version:

1. **Run `bb test-all`** — all 6 combinations must pass
2. **Update CHANGES.md** — add a new section at the top with the version and bullet points summarizing changes since the last tag
3. **Update `src/gherclj/main.cljc`** — bump the `version` constant to the new X.Y.Z
4. **Update README.md** — replace git coordinates with the new tag and `"PENDING"` as the sha
5. **Commit** — `git commit` with message "Prepare vX.Y.Z release"
6. **Push** — `git push`
7. **Tag** — `git tag vX.Y.Z && git push origin vX.Y.Z`
8. **Get sha** — `git rev-parse vX.Y.Z | head -c 7`
9. **Update README.md** — replace `"PENDING"` with the actual short sha
10. **Commit and push** — "Update README with vX.Y.Z git coordinates"
