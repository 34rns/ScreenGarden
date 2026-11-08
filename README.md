# ScreenGarden

A calm, offline garden that grows while you focus and keep your screen time in
check.

> **Status: work in progress.** Onboarding, local settings, usage collection and
> the Garden, Focus, Insights, App limits and Milestones screens are implemented. Settings now includes goals, themes, palettes, scenery, reminders and focus preferences. There is no installable signed release yet.

[![CI](https://github.com/34rns/ScreenGarden/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/34rns/ScreenGarden/actions/workflows/ci.yml)
![Status](https://img.shields.io/badge/status-work%20in%20progress-yellow)

## The idea

Optional [focus app blocking](docs/FOCUS_BLOCKING.md) is off by default. Its
restricted accessibility service never reads screen content or stores observed
events. Only the block and allow lists you edit are saved locally.

| Optional blocking | Permission explanation |
| --- | --- |
| <img src="docs/screenshots/blocking_light.png" width="220" alt="Optional focus blocking with separate permission and enable choices"> | <img src="docs/screenshots/blocking_permission.png" width="220" alt="Explanation before Android accessibility settings opens"> |

English and a Spanish sample are bundled locally. On Android 13+, choose the
language in Android's app settings; older versions follow the device language.
Spanish covers the current resources and is awaiting native-speaker review.
See [translation instructions](CONTRIBUTING.md#translations).

Battery help is available in Settings → About, with offline guides for seven device brands.
Menu paths may vary by version. See [battery help](docs/BATTERY_HELP.md).

Plant artwork is bundled and loaded entirely offline. Ferns are available from
the start; flowers unlock after five completed focus sessions, bonsai after a
three-day streak, and cacti during summer. Existing plants always stay. See
[the plant-pack contribution guide](docs/PLANT_PACKS.md) for the schema and validator.

Screen time apps usually shout: red numbers, streaks, guilt. ScreenGarden does
the opposite. You plant something, you sit with your work, and the garden
responds quietly — a session that goes well lets it grow, a distracted day lets
it rest. A daily health score explains progress, plants can always recover, and
nothing is uploaded or sold.

## Screenshots

Captured on an Android 15 emulator (1080 × 2400) with demo mode enabled.
Garden, Focus, Insights, App limits and Milestones images use fake debug-only demo data. Settings images use fake debug-only preferences. No image contains personal phone data.

| | Light | Dark | AMOLED |
| --- | --- | --- | --- |
| **Garden** | <img src="docs/screenshots/garden_light.png" width="200" alt="Garden tab, light theme"> | <img src="docs/screenshots/garden_dark.png" width="200" alt="Garden tab, dark theme"> | <img src="docs/screenshots/garden_amoled.png" width="200" alt="Garden tab, true-black AMOLED theme"> |
| **Focus** | <img src="docs/screenshots/focus_light.png" width="200" alt="Focus tab, light theme"> | <img src="docs/screenshots/focus_dark.png" width="200" alt="Focus tab, dark theme"> | <img src="docs/screenshots/focus_amoled.png" width="200" alt="Focus tab, AMOLED theme"> |
| **Insights** | <img src="docs/screenshots/insights_light.png" width="200" alt="Insights tab, light theme"> | <img src="docs/screenshots/insights_dark.png" width="200" alt="Insights tab, dark theme"> | <img src="docs/screenshots/insights_amoled.png" width="200" alt="Insights tab, AMOLED theme"> |
| **Settings** | <img src="docs/screenshots/settings_light.png" width="200" alt="Settings tab, light theme"> | <img src="docs/screenshots/settings_dark.png" width="200" alt="Settings tab, dark theme"> | <img src="docs/screenshots/settings_amoled.png" width="200" alt="Settings tab, AMOLED theme"> |
| **App limits** | <img src="docs/screenshots/limits_light.png" width="200" alt="App limits, light theme"> | <img src="docs/screenshots/limits_dark.png" width="200" alt="App limits, dark theme"> | <img src="docs/screenshots/limits_amoled.png" width="200" alt="App limits, AMOLED theme"> |
| **Milestones** | <img src="docs/screenshots/habits_light.png" width="200" alt="Counted-day streaks and earned milestones"> | <img src="docs/screenshots/habits_dark.png" width="200" alt="Milestones in dark mode"> | <img src="docs/screenshots/habits_amoled.png" width="200" alt="Milestones with true-black surfaces"> |

Accessibility and layout:

| Large text (1.3×) | Forced right-to-left layout |
| --- | --- |
| <img src="docs/screenshots/garden_large_font.png" width="240" alt="Garden tab at 1.3x font scale"> | <img src="docs/screenshots/garden_rtl.png" width="240" alt="Garden tab with a forced Compose right-to-left layout"> |

All current public screenshots are stored in `docs/screenshots/`.

| Spanish Garden | Spanish Focus | Large translated text |
| --- | --- | --- |
| <img src="docs/screenshots/language_es_garden.png" width="220" alt="Spanish Garden with localized labels"> | <img src="docs/screenshots/language_es_focus.png" width="220" alt="Spanish Focus with wrapping choices"> | <img src="docs/screenshots/language_es_large_text.png" width="220" alt="Spanish goal editor at 1.3 times font scale"> |

| Privacy introduction | Optional Usage access | Initial goals |
| --- | --- | --- |
| <img src="docs/screenshots/intro_privacy.png" width="220" alt="Introduction explaining local storage and voluntary exports"> | <img src="docs/screenshots/intro_usage.png" width="220" alt="Optional Usage access with an explanation of focus without it"> | <img src="docs/screenshots/intro_goals.png" width="220" alt="Initial screen-time, pickup and bedtime goals"> |

| Focusing | Completed session | Large text | Right-to-left layout |
| --- | --- | --- | --- |
| <img src="docs/screenshots/focus_running.png" width="200" alt="Focus countdown and growing plant"> | <img src="docs/screenshots/focus_result.png" width="200" alt="Completed focus session"> | <img src="docs/screenshots/focus_large_font.png" width="200" alt="Focus setup at 1.3x font scale"> | <img src="docs/screenshots/focus_rtl.png" width="200" alt="Focus setup with a right-to-left layout"> |

| Edit a limit | No apps available | Large text | Right-to-left layout |
| --- | --- | --- | --- |
| <img src="docs/screenshots/limits_editor.png" width="200" alt="Daily app limit editor"> | <img src="docs/screenshots/limits_empty.png" width="200" alt="App limits empty state and refresh action"> | <img src="docs/screenshots/limits_large_font.png" width="200" alt="App limits at 1.3x font scale"> | <img src="docs/screenshots/limits_rtl.png" width="200" alt="App limits with mirrored controls and correct text order"> |

Optional reminders, using fake debug-only examples:

| Light | Dark |
| --- | --- |
| <img src="docs/screenshots/nudges_light.png" width="240" alt="Gentle daily and app-limit reminders in the notification shade"> | <img src="docs/screenshots/nudges_dark.png" width="240" alt="Gentle reminders with a dark notification shade"> |

Sleep garden during the saved wind-down window, using fake debug-only data:

| Light | Dark | AMOLED |
| --- | --- | --- |
| <img src="docs/screenshots/bedtime_light.png" width="220" alt="Sleep garden with a moonlit plot"> | <img src="docs/screenshots/bedtime_dark.png" width="220" alt="Sleep garden in dark mode"> | <img src="docs/screenshots/bedtime_amoled.png" width="220" alt="Sleep garden with true-black surfaces"> |

| Large text | Mirrored layout | Empty garden | Usage access missing |
| --- | --- | --- | --- |
| <img src="docs/screenshots/bedtime_large_font.png" width="200" alt="Sleep garden at 1.3 times font scale"> | <img src="docs/screenshots/bedtime_rtl.png" width="200" alt="Sleep garden with RTL controls and readable English copy"> | <img src="docs/screenshots/bedtime_empty.png" width="200" alt="Sleep garden waiting for data"> | <img src="docs/screenshots/bedtime_permission.png" width="200" alt="Sleep garden explaining optional Usage access"> |

Compassion days and milestones, with fake data:

| Compassion controls | Large text | Mirrored layout | No milestones yet |
| --- | --- | --- | --- |
| <img src="docs/screenshots/habits_compassion.png" width="200" alt="Mark an off day and configure the monthly allowance"> | <img src="docs/screenshots/habits_large_font.png" width="200" alt="Milestones at 1.3 times font scale"> | <img src="docs/screenshots/habits_rtl.png" width="200" alt="Mirrored milestone layout with readable text"> | <img src="docs/screenshots/habits_empty.png" width="200" alt="No counted days or earned badges yet"> |

Insights, with fake debug-only data and explicit coverage:

| Recent hours | Month view | Large text | Mirrored layout |
| --- | --- | --- | --- |
| <img src="docs/screenshots/insights_hours.png" width="200" alt="Labelled local-hour heatmap and recent-day coverage"> | <img src="docs/screenshots/insights_month.png" width="200" alt="Thirty-day screen time with an off-day gap"> | <img src="docs/screenshots/insights_large_font.png" width="200" alt="Insights at 1.3 times font scale"> | <img src="docs/screenshots/insights_rtl.png" width="200" alt="Insights with mirrored chart chronology and controls"> |

| Today | No data yet | Usage access missing |
| --- | --- | --- |
| <img src="docs/screenshots/insights_day.png" width="200" alt="Today's measured screen time"> | <img src="docs/screenshots/insights_empty.png" width="200" alt="An honest empty history with no invented totals"> | <img src="docs/screenshots/insights_permission.png" width="200" alt="Saved Insights remain visible after Usage access is switched off"> |

Local reflections and saved gardens, shown with fake debug-only data:

| Weekly reflection | Garden history | Month snapshots |
| --- | --- | --- |
| <img src="docs/screenshots/reflection_light.png" width="220" alt="A calm local weekly reflection with measured coverage"> | <img src="docs/screenshots/history_light.png" width="220" alt="Actual saved plant stages in the garden timeline"> | <img src="docs/screenshots/history_months.png" width="220" alt="Latest stored snapshot for each month"> |

Scenery follows the local clock and season. These emulator previews use fake debug-only data; weather is decorative:

| Winter | Evening | Night |
|---|---|---|
| <img src="docs/screenshots/environment_winter.png" width="220" alt="A winter plot with decorative snow"> | <img src="docs/screenshots/environment_sunset.png" width="220" alt="Warm evening light over the garden"> | <img src="docs/screenshots/environment_night.png" width="220" alt="A moonlit garden with decorative fireflies"> |

Settings includes stored themes, curated accents and optional visual controls:

| Appearance | AMOLED appearance | Data and about |
|---|---|---|
| <img src="docs/screenshots/settings_light_appearance.png" width="220" alt="Stored theme, curated palette and scenery choices"> | <img src="docs/screenshots/settings_amoled_appearance.png" width="220" alt="True-black AMOLED settings with labelled choices"> | <img src="docs/screenshots/settings_data_about.png" width="220" alt="Local data, introduction restart and license information"> |

Garden pictures can be previewed and saved with an optional date and streak caption:

| Picture preview | Moonlit picture | Without a caption |
|---|---|---|
| <img src="docs/screenshots/snapshot_light.png" width="220" alt="Garden picture preview with an optional date and streak caption"> | <img src="docs/screenshots/snapshot_night.png" width="220" alt="A moonlit garden picture using the shared plant art"> | <img src="docs/screenshots/snapshot_without_caption.png" width="220" alt="A garden picture without its caption"> |

Backups use Android's file picker and show a preview before restoring:

| Backup controls | Review records | Confirm replacement |
|---|---|---|
| <img src="docs/screenshots/backup_light.png" width="220" alt="User-controlled local backup save and restore controls"> | <img src="docs/screenshots/backup_preview.png" width="220" alt="Fake backup date and counts with merge and replace choices"> | <img src="docs/screenshots/backup_replace.png" width="220" alt="Second confirmation before replacing current data"> |

## Privacy promise

- **No internet permission.** The app cannot make a network request, even by
  accident: the manifest declares no network access at all.
- **No tracking.** No analytics, no crash reporting, no ads, no A/B testing, no
  account, no identifier.
- **No cloud backup.** `android:allowBackup` is `false`, so your history does not
  leave the device through backup or device transfer.
- **Local by default.** Measurements stay in app-private storage. A backup or garden
  picture leaves that storage only when you choose to save it through Android's file picker.

These are enforced by the build and by the rules in `AGENT_MEMORY.txt`, not just
by intention. A pull request that adds network access or telemetry is rejected.

## What works today

- Single-activity Jetpack Compose app (Material 3) with a four-tab navigation
  shell: Garden, Focus, Insights, Settings.
- Calm green palette with light, dark and true-black (AMOLED) schemes.
- A local Room database (seven tables for daily stats, app usage, limits, plants,
  focus sessions, badges and garden snapshots), covered by unit tests.
- A reflection for the last seven days, made from local templates: time versus
  your current goal, best score, saved focus minutes and one gentle idea.
  Missing measurements and compassion days never inflate time saved.
- A garden timeline with weekly/monthly views of actual saved plant snapshots.
  A snapshot is saved once when completed-day growth is applied. Earlier layouts
  were not recorded and are never reconstructed or guessed.
- Preview and save a garden picture as PNG through Android's file picker, with
  an optional date and streak caption. The image includes no app list or usage
  history and needs no storage-wide permission.
- Save or restore a private JSON backup through Android's file picker. Review record
  counts, merge missing records or confirm a complete replacement. Imported timers
  stay stopped and past garden rewards never repeat. See [backup details](docs/BACKUP.md)
  for privacy, recovery, conflicts and document limits.
- First-run privacy and permission explanations, goal setup and optional
  notifications permission.
- Preferences DataStore for goals and light/dark/AMOLED theme selection.
- Offline usage collection with screen-off intervals excluded on supported
  Android versions, plus a battery-friendly daily sync.
- A Canvas garden with tappable plant details, today's score, streak and a Focus
  tab action. Completed days grow plants once; refreshing never duplicates rewards.
- Gentle growth, bloom, wilt and recovery transitions. The garden's idle motion
  stops when it is hidden, system animations are disabled or battery saver is on.
- Day, evening and night scenery follows the local clock. Seasonal ground, snow,
  rain, sun rays and fireflies are decorative and generated offline. Choose your
  hemisphere or turn on low power visuals in Settings; plants grow as usual.
- Focus duration presets and custom times, tags, a growing seed/countdown,
  gentle results, saved session totals and optional keep-screen-on behavior.
  A visible foreground timer keeps running when you leave the app.
- Brief phone checks have a 30-second grace. Measured distractions end focus
  gently, preserving every plant's stage and health. The first two completed
  sessions each day grow and heal the garden, with a new seed for an empty plot.
  Detection needs Usage access and reliable screen events (Android 9+).
- Search launchable apps and choose daily limits from Settings. Pausing a limit
  keeps its chosen time. Enabled breaches shape the score without blocking apps.
- Optional gentle reminders at 50%, 80% and 100% of the daily goal and enabled
  app limits. Quiet hours use your bedtime window. Separate Android notification
  channels let you control daily and app reminders; milestones do not repeat that day.
- Your saved bedtime window opens a quiet sleep garden with a moonlit plot.
  Measured screen time inside that window shapes the health score after a 15-minute
  grace, using the same gradual curve as the other habits. Saved plants remain
  accessible and recoverable. Old bedtime history is unknown until remeasured.
- Usage without reliable screen events (including Android 8.0–8.1) is shown as
  incomplete rather than given a fabricated score.
- Counted-day streaks and permanent badges celebrate progress gently. Mark today
  or a past date as a compassion day to preserve a streak and exclude it from
  averages. The monthly allowance defaults to three and is configurable from zero
  to ten. Rest never erases applied growth or an earned badge.
- Insights shows today, the last seven days or the last thirty days: screen-time
  bars against your goal, a health-score line, top apps and pickups. Readable daily
  values accompany the charts. A labelled hourly heatmap uses recent measured
  events only; missing and compassion days stay out of averages and totals.

Insights preserves collected daily totals locally. Android usually retains about a
week of detailed usage events, so a fresh install cannot reconstruct the previous
month. Hourly detail is read from that recent event history and may be unavailable;
unknown usage is never shown as zero. The month view covers the last thirty local days.

Reminder checks use Android's background scheduler at a 15-minute minimum interval,
with a battery-not-low constraint. Checks can be delayed by Doze, battery restrictions
or manufacturer settings, so reminders are approximate. A late check shows the highest
new milestone rather than a burst of older reminders. Disabled reminders stop before
reading usage; missing permission or incomplete screen-state evidence produces no nudge.
There are no exact alarms, foreground reminder services or network requests.

## Garden widget and focus tile

Add ScreenGarden from your launcher's widget picker to see today's score,
counted-day streak and a plant in a small or medium widget. Tap it to open the
garden. Missing Usage access or incomplete measurements are labelled clearly.
Saved garden and goal changes refresh installed widgets after a short delay.
Android also schedules a refresh about every thirty minutes; Doze and launcher
restrictions can delay it. The widget has no animation or usage polling.

Add **Focus** from the Quick Settings edit panel. Tap to start a 25-minute Study
session, or tap again to stop the current session. Android may ask you to unlock
first. It uses the same saved timer and notification as the Focus tab.

<details>
<summary>Widget and tile previews</summary>

All images below show fake debug-only data on the Android emulator.

<img src="docs/screenshots/widgets.png" alt="Small and medium garden widgets with a score, streak and flower" width="260" />
<img src="docs/screenshots/widgets_large_text.png" alt="Garden widgets at enlarged system text size" width="260" />
<img src="docs/screenshots/focus_tile_idle.png" alt="Quick Settings Focus tile ready to start a session" width="260" />
<img src="docs/screenshots/focus_tile_running.png" alt="Active Quick Settings Focus tile with a stop action" width="260" />

</details>

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
  compatible with every runtime dependency we ship (Apache-2.0 and
  Glance's BSD-3-Clause shaded protobuf).
- **Original artwork and plant packs:**
  [CC BY-SA 4.0](ART_LICENSE.md). A contributor may license their own plant pack
  differently in its `plant.json`.

The GPL text in [LICENSE](LICENSE) is version 3; the "or later" part comes from
the SPDX headers in the source and from this notice.

Live wallpaper reuses your saved plants and scene choices, with still frames and
battery-aware refresh. Android owns preview and applying. See [wallpaper help](docs/WALLPAPER.md).

| Wallpaper preview | Wallpaper settings |
| --- | --- |
| <img src="docs/screenshots/wallpaper_preview.png" width="240" alt="Android preview of a fake garden wallpaper"> | <img src="docs/screenshots/settings_wallpaper_light.png" width="240" alt="Appearance controls and optional wallpaper preview"> |