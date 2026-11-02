# ScreenGarden

A calm, offline garden that grows while you focus and keep your screen time in
check.

> **Status: work in progress.** The app is a skeleton. The four tabs exist and
> navigate, and the local database layer is in place; none of the garden, focus
> or insights features are built yet.

<!-- Badge placeholder: replace 34rns/ScreenGarden once the repository is public. -->
[![CI](https://github.com/34rns/ScreenGarden/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/34rns/ScreenGarden/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)

## The idea

Screen time apps usually shout: red numbers, streaks, guilt. ScreenGarden does
the opposite. You plant something, you sit with your work, and the garden
responds quietly — a session that goes well lets it grow, a distracted day lets
it rest. Nothing is scored, nothing is shared, nothing is sold.

## Privacy promise

- **No internet permission.** The app cannot make a network request, even by
  accident: the manifest declares no network access at all.
- **No tracking.** No analytics, no crash reporting, no ads, no A/B testing, no
  account, no identifier.
- **No cloud backup.** `android:allowBackup` is `false`, so your history does not
  leave the device through backup or device transfer.
- **Local only.** Everything you record stays in app-private storage.

These are enforced by the build and by the rules in `AGENT_MEMORY.txt`, not just
by intention. A pull request that adds network access or telemetry is rejected.

## What works today

- Single-activity Jetpack Compose app (Material 3) with a four-tab navigation
  shell: Garden, Focus, Insights, Settings.
- Calm green palette with light, dark and true-black (AMOLED) schemes.
- A local Room database (six tables for daily stats, app usage, limits, plants,
  focus sessions and badges), covered by unit tests.
- Everything else is a placeholder.

## Requirements

- Android Studio (latest stable) with JDK 17
- Android SDK 35
- Device or emulator running Android 8.0 (API 26) or newer

## Build and run

```bash
# Debug build + install on a connected device/emulator
./gradlew :app:assembleDebug
./gradlew :app:installDebug

# The same checks CI runs
./gradlew ktlintCheck
./gradlew :app:lintDebug
./gradlew :app:testDebugUnitTest
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Project layout

`app/src/main/java/com/screengarden/app/` is split into `ui/` (Compose screens,
theme, navigation), `core/` (pure helpers such as the local-day maths),
`data/` and `di/`. The full layout, the coding conventions and every
architectural decision live in [`AGENT_MEMORY.txt`](AGENT_MEMORY.txt).

## Distribution

Free to build, free to install:

- **GitHub Releases** — signed APKs attached to a release tag.
- **F-Droid** — the main catalog listing; no Google Play account, so no
  developer fee.
- **IzzyOnDroid** — optional extra catalog for the same builds.

There is no Google Play release and no plan to add one: it charges a one-off
registration fee, and nothing here needs it.

## Cost

ScreenGarden is built with free tooling only: Android Studio and its emulator,
Gradle, the Android SDK, GitHub's free tier for hosting and CI, and free
distribution through GitHub Releases and F-Droid. No paid dependency, service,
font, asset pack or developer account is used, and a dependency that requires
payment will not be accepted.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Formatting is enforced with ktlint;
Android lint must stay at zero errors.

## License

- **Code:** [GPL-3.0-or-later](LICENSE). Strong copyleft, so a fork stays open:
  nobody can turn ScreenGarden into a closed, tracker-laden build. This is
  compatible with F-Droid and with every runtime dependency we ship (all
  Apache-2.0).
- **Original artwork and plant packs:**
  [CC BY-SA 4.0](ART_LICENSE.md). A contributor may license their own plant pack
  differently in its `plant.json`.

The GPL text in [LICENSE](LICENSE) is version 3; the "or later" part comes from
the SPDX headers in the source and from this notice.
