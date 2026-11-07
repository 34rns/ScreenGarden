# ScreenGarden

A calm, offline garden that grows while you focus and keep your screen time in
check.

> **Status: work in progress.** Onboarding, local settings, usage collection and
> the Garden, Focus and App limits screens are implemented. Insights and Settings controls are
> still being built. There is no installable signed release yet.

[![CI](https://github.com/34rns/ScreenGarden/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/34rns/ScreenGarden/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)

## The idea

Screen time apps usually shout: red numbers, streaks, guilt. ScreenGarden does
the opposite. You plant something, you sit with your work, and the garden
responds quietly — a session that goes well lets it grow, a distracted day lets
it rest. A daily health score explains progress, plants can always recover, and
nothing is uploaded or sold.

## Screenshots

Captured on an Android 15 emulator (1080 × 2400) with demo mode enabled.
Garden, Focus and App limits images use fake debug-only demo data. Settings shows
the App limits entry; Insights remains a placeholder. No image contains personal phone data.

| | Light | Dark | AMOLED |
| --- | --- | --- | --- |
| **Garden** | <img src="docs/screenshots/garden_light.png" width="200" alt="Garden tab, light theme"> | <img src="docs/screenshots/garden_dark.png" width="200" alt="Garden tab, dark theme"> | <img src="docs/screenshots/garden_amoled.png" width="200" alt="Garden tab, true-black AMOLED theme"> |
| **Focus** | <img src="docs/screenshots/focus_light.png" width="200" alt="Focus tab, light theme"> | <img src="docs/screenshots/focus_dark.png" width="200" alt="Focus tab, dark theme"> | <img src="docs/screenshots/focus_amoled.png" width="200" alt="Focus tab, AMOLED theme"> |
| **Insights** | <img src="docs/screenshots/insights_light.png" width="200" alt="Insights tab, light theme"> | <img src="docs/screenshots/insights_dark.png" width="200" alt="Insights tab, dark theme"> | <img src="docs/screenshots/insights_amoled.png" width="200" alt="Insights tab, AMOLED theme"> |
| **Settings** | <img src="docs/screenshots/settings_light.png" width="200" alt="Settings tab, light theme"> | <img src="docs/screenshots/settings_dark.png" width="200" alt="Settings tab, dark theme"> | <img src="docs/screenshots/settings_amoled.png" width="200" alt="Settings tab, AMOLED theme"> |
| **App limits** | <img src="docs/screenshots/limits_light.png" width="200" alt="App limits, light theme"> | <img src="docs/screenshots/limits_dark.png" width="200" alt="App limits, dark theme"> | <img src="docs/screenshots/limits_amoled.png" width="200" alt="App limits, AMOLED theme"> |

Accessibility and layout:

| Large text (1.3×) | Forced right-to-left layout |
| --- | --- |
| <img src="docs/screenshots/garden_large_font.png" width="240" alt="Garden tab at 1.3x font scale"> | <img src="docs/screenshots/garden_rtl.png" width="240" alt="Garden tab with a forced Compose right-to-left layout"> |

All current public screenshots are stored in `docs/screenshots/`.

| Focusing | Completed session | Large text | Right-to-left layout |
| --- | --- | --- | --- |
| <img src="docs/screenshots/focus_running.png" width="200" alt="Focus countdown and growing plant"> | <img src="docs/screenshots/focus_result.png" width="200" alt="Completed focus session"> | <img src="docs/screenshots/focus_large_font.png" width="200" alt="Focus setup at 1.3x font scale"> | <img src="docs/screenshots/focus_rtl.png" width="200" alt="Focus setup with a right-to-left layout"> |

| Edit a limit | No apps available | Large text | Right-to-left layout |
| --- | --- | --- | --- |
| <img src="docs/screenshots/limits_editor.png" width="200" alt="Daily app limit editor"> | <img src="docs/screenshots/limits_empty.png" width="200" alt="App limits empty state and refresh action"> | <img src="docs/screenshots/limits_large_font.png" width="200" alt="App limits at 1.3x font scale"> | <img src="docs/screenshots/limits_rtl.png" width="200" alt="App limits with mirrored controls and correct text order"> |

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
- First-run privacy and permission explanations, goal setup and optional
  notifications permission.
- Preferences DataStore for goals and light/dark/AMOLED theme selection.
- Offline usage collection with screen-off intervals excluded on supported
  Android versions, plus a battery-friendly daily sync.
- A Canvas garden with tappable plant details, today's score, streak and a Focus
  tab action. Completed days grow plants once; refreshing never duplicates rewards.
- Gentle growth, bloom, wilt and recovery transitions. The garden's idle motion
  stops when it is hidden or system animations are disabled.
- Focus duration presets and custom times, tags, a growing seed/countdown,
  gentle results, saved session totals and optional keep-screen-on behavior.
  A visible foreground timer keeps running when you leave the app.
- Brief phone checks have a 30-second grace. Measured distractions end focus
  gently, preserving every plant's stage and health. The first two completed
  sessions each day grow and heal the garden, with a new seed for an empty plot.
  Detection needs Usage access and reliable screen events (Android 9+).
- Search launchable apps and choose daily limits from Settings. Pausing a limit
  keeps its chosen time. Enabled breaches shape the score without blocking apps.
- Usage without reliable screen events (including Android 8.0–8.1) is shown as
  incomplete rather than given a fabricated score. Insights and the remaining
  Settings controls are still being built.

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

# Migration tests (needs a device or emulator; uninstalls the app afterwards)
./gradlew :app:connectedDebugAndroidTest
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Project layout

`app/src/main/java/com/screengarden/app/` is split into `ui/` (Compose screens,
theme, navigation), `core/` (pure helpers such as the local-day maths),
`data/` and `di/`. The full layout, the coding conventions and every
architectural decision live in [`AGENT_MEMORY.txt`](AGENT_MEMORY.txt).

## Distribution

Free to build, free to install:

- **GitHub Releases** — planned signed APKs attached to a release tag.
- **GitHub Pages** — a project site with screenshots and installation guidance.

There is no Google Play release and no plan to add one: it charges a one-off
registration fee, and nothing here needs it.

## Cost

ScreenGarden is built with free tooling only: Android Studio and its emulator,
Gradle, the Android SDK, GitHub's free tier for hosting and CI, and free
distribution through GitHub Releases. No paid dependency, service,
font, asset pack or developer account is used, and a dependency that requires
payment will not be accepted.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md). Formatting is enforced with ktlint;
Android lint must stay at zero errors.

## License

- **Code:** [GPL-3.0-or-later](LICENSE). Strong copyleft, so a fork stays open:
  nobody can turn ScreenGarden into a closed, tracker-laden build. This is
  compatible with every runtime dependency we ship (all
  Apache-2.0).
- **Original artwork and plant packs:**
  [CC BY-SA 4.0](ART_LICENSE.md). A contributor may license their own plant pack
  differently in its `plant.json`.

The GPL text in [LICENSE](LICENSE) is version 3; the "or later" part comes from
the SPDX headers in the source and from this notice.
