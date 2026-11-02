## What changed

A short description of the change and why it is needed.

Closes #

## Type of change

- [ ] Bug fix (`fix:`)
- [ ] New feature (`feat:`)
- [ ] Documentation (`docs:`)
- [ ] Tests or QA (`test:`)
- [ ] Chore, build or refactor (`chore:` / `refactor:`)

## How this was verified

Paste the commands you ran and their result. A change is done when it builds,
is formatted and passes lint, not when it looks right.

```bash
./gradlew ktlintCheck
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
```

Result:

## Screenshots

For any visual change, before and after.

## Checklist

- [ ] The change is one logical thing, not several
- [ ] No new dependency without prior discussion and an entry in AGENT_MEMORY
- [ ] User-visible text goes through string resources, not hardcoded strings
- [ ] No `INTERNET` permission, analytics, network library or telemetry was added
- [ ] `android:allowBackup` is still `false`
- [ ] AGENT_MEMORY.txt updated (checklist, decisions, session log) in the same commit
- [ ] Commit messages follow Conventional Commits
- [ ] Any new permission is documented in AGENT_MEMORY SECTION 9 with a reason
