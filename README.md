# ScreenGarden

A calm, offline garden that grows while you focus and keep your screen time in
check.

> **Status: work in progress.** Onboarding, local settings, usage collection and
> the Garden screen are implemented. Focus, Insights and Settings controls are
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
Garden images use fake debug-only demo plants and scores. The other tabs still
show their earlier placeholder screens. No image contains personal phone data.

| | Light | Dark | AMOLED |
| --- | --- | --- | --- |
| **Garden** | <img src="docs/screenshots/garden_light.png" width="200" alt="Garden tab, light theme"> | <img src="docs/screenshots/garden_dark.png" width="200" alt="Garden tab, dark theme"> | <img src="docs/screenshots/garden_amoled.png" width="200" alt="Garden tab, true-black AMOLED theme"> |
| **Focus** | <img src="docs/screenshots/focus_light.png" width="200" alt="Focus tab, light theme"> | <img src="docs/screenshots/focus_dark.png" width="200" alt="Focus tab, dark theme"> | <img src="docs/screenshots/focus_amoled.png" width="200" alt="Focus tab, AMOLED theme"> |
| **Insights** | <img src="docs/screenshots/insights_light.png" width="200" alt="Insights tab, light theme"> | <img src="docs/screenshots/insights_dark.png" width="200" alt="Insights tab, dark theme"> | <img src="docs/screenshots/insights_amoled.png" width="200" alt="Insights tab, AMOLED theme"> |
| **Settings** | <img src="docs/screenshots/settings_light.png" width="200" alt="Settings tab, light theme"> | <img src="docs/screenshots/settings_dark.png" width="200" alt="Settings tab, dark theme"> | <img src="docs/screenshots/settings_amoled.png" width="200" alt="Settings tab, AMOLED theme"> |

Accessibility and layout:

| Large text (1.3×) | Forced right-to-left layout |
| --- | --- |
| <img src="docs/screenshots/garden_large_font.png" width="240" alt="Garden tab at 1.3x font scale"> | <img src="docs/screenshots/garden_rtl.png" width="240" alt="Garden tab with a forced Compose right-to-left layout"> |

All current public screenshots are stored in `docs/screenshots/`.

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
- Usage without reliable screen events (including Android 8.0–8.1) is shown as
  incomplete rather than given a fabricated score. Focus and Insights remain
  placeholders until their roadmap steps are finished.

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
