# Contributing to ScreenGarden

Thanks for wanting to help. ScreenGarden is a small, deliberately quiet app, and
the rules below exist to keep it that way.

## Before you start

1. Read [`AGENT_MEMORY.txt`](AGENT_MEMORY.txt). It is the source of truth for hard
   rules, conventions and decisions. If your change contradicts a decision, open
   an issue first instead of working around it.
2. For anything larger than a typo, open an issue describing what you want to
   change and why. Unannounced large pull requests usually get asked to split.

## Non-negotiable rules

These come from the project's privacy promise and are enforced in review:

- **No `INTERNET` permission** and no network library. The app must work fully
  offline, always.
- **No analytics, ads, telemetry, crash reporting or tracking SDKs.**
- **`android:allowBackup` stays `false`.**
- **No new permission** without a written justification in AGENT_MEMORY SECTION 9
  and approval in an issue first.
- **One Activity.** All UI is Compose; XML is only for resources.
- **Nothing paid.** No paid dependency, service, API, font, asset, device farm or
  developer account. Open-source tooling and GitHub's free tier only. If
  a change would need something paid, open an issue proposing a free alternative
  instead of adding it.

## Setup

- Android Studio with JDK 17, Android SDK 35.
- Device or emulator on Android 8.0 (API 26) or newer. The free Android Studio
  emulator is fine — you do not need a physical phone.

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
```

On Windows use `gradlew.bat`.

## Code style

Formatting and basic style are enforced by **ktlint** (plugin
`org.jlleitschuh.gradle.ktlint`, Android Studio code style, 120-column limit),
configured in `.editorconfig`.

```bash
./gradlew ktlintCheck    # what CI runs
./gradlew ktlintFormat   # fix the mechanical violations
```

Beyond ktlint, follow the conventions in AGENT_MEMORY SECTION 5: every composable
takes `modifier: Modifier = Modifier` first, user-visible text comes from
`strings.xml`, and KDoc explains *why* rather than *what*.

## Branch and commits

- Branch from `main`: `feat/focus-timer`, `fix/nav-state`, `docs/readme`.
- Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/):
  `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `refactor:`.
- Keep one logical change per commit and per pull request.

## Pull requests

- Fill in the pull request template, including the commands you ran.
- CI must be green: ktlint, debug build, Android lint, unit tests.
- Update `AGENT_MEMORY.txt` in the same pull request — tick the checklist item,
  record new decisions, and add a session log entry. This is not optional; a
  reviewer will ask for it.
- Screenshots for anything visual, from the emulator with fake debug-only demo data.

## Tests

Please add unit tests for logic that can be tested without Android (use cases,
growth rules, time maths) under `app/src/test/`. New behaviour without tests is
acceptable only when it is purely visual and you say so in the pull request.

Run all four gates before submitting, using `gradlew.bat` in PowerShell:

```powershell
.\gradlew.bat ktlintCheck --no-build-cache
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

Run `scripts/Check-Repository.ps1` for offline/backup, license-header and schema
checks. For a Room upgrade, append a new schema, Migration and preservation test;
never edit a published schema or use destructive fallback. Extend the hash file
with the new version without replacing old hashes. Run the dedicated emulator's
integration tests and record results honestly; a compiled app is not a tested app.

## Good first contributions

- Review the Spanish sample as a fluent speaker, including plurals and dates.
- Add a complete translation using the instructions below.
- Verify a battery-help menu path on a recent device and note the exact version.
- Improve chart accessibility with screen-reader feedback and a focused test.
- Contribute original plant art with complete license metadata and validator results.

Keep proposals focused. Privacy, free tools and offline operation are required.
Read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) and use [SECURITY.md](SECURITY.md)
for sensitive reports. Never post an unredacted private backup or credentials.

## Translations

English in `app/src/main/res/values/strings*.xml` is the fallback. Spanish in
`values-es/` is a sample language covering the current app. It needs native-speaker
review; translation coverage is not proof of linguistic quality.

To add a language, create `app/src/main/res/values-<language>/` and translate all
string and plural resources. Keep names and numbered arguments such as `%1$d`
and `%2$s` unchanged. Keep `%%` as a literal percent and `\n` as a line break.
Use the plural categories required by the language, including `other`. Escape XML
characters and Android string apostrophes correctly. Brand names and licenses
should retain their official spelling.

Add the language tag to `res/xml/locale_config.xml`. Android 13+ then offers the
language in **Settings → Apps → ScreenGarden → Language**; older Android versions
follow the device language. There is no translation service or account.

Run all four quality checks and the localization test. Review the app on the
emulator using fake debug-only demos with large text and, for RTL languages,
mirrored layout. Check buttons, field labels, dates, plurals, charts and empty/error
states. Prefer start/end alignment and wrapping; avoid fixed text widths. Numeric
editors use their documented input format; saved UTC timestamps and stable enum
keys are never translated. Submit screenshots and note any wording not reviewed
by a fluent speaker.

## Licensing

ScreenGarden is **GPL-3.0-or-later** (see [`LICENSE`](LICENSE)). By opening a pull
request you agree that your code contribution is released under that license.

Artwork and plant packs are separate: the project's own art is **CC BY-SA 4.0**
(see [`ART_LICENSE.md`](ART_LICENSE.md)), and a plant pack you contribute may
declare its own license in its `plant.json`. Only contribute art you actually own
or that is already under a license permitting redistribution.

Because the code is copyleft, a contribution cannot be relicensed into a closed
product — that is the point of the choice, not an oversight. Every source file
starts with an SPDX line (`// SPDX-License-Identifier: GPL-3.0-or-later`); keep it
in new files.
