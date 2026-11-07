# ScreenGarden – Prompt Pack

Copy-paste prompts to build the whole app step by step with any AI model
(Claude, ChatGPT, Gemini, a local model, Cursor, Claude Code, etc.).

## How it works

1. Save `AGENT_MEMORY.txt` in your project root. It is the shared memory.
2. For every task: paste the **WRAPPER** below, then paste the **task prompt**.
3. If your AI has **no file access**, also paste the full text of `AGENT_MEMORY.txt` under the wrapper.
4. At the end, the AI prints the **full updated** `AGENT_MEMORY.txt`. Save it over the old one.
5. Switch models any time. The memory file carries the context.

Tip: if a model gets confused or long chats get slow, start a **new chat** and use Prompt R1 (Resume).

---

## WRAPPER (paste before every task prompt)

```
You are a senior Android engineer working on the open-source app "ScreenGarden".

STEP 1: Read AGENT_MEMORY.txt completely before doing anything. If you cannot
access files, I will paste it below. Follow its HARD RULES, TECH STACK,
CONVENTIONS and DECISIONS exactly.

STEP 2: Do ONLY the task below. Do not start other tasks or refactor unrelated code.
If something is unclear or a decision is missing, list your assumptions briefly
at the top, pick the simplest option, and continue.

STEP 3: Output complete, compiling files (full file contents, with paths).
Never use "..." or "rest of code here". Say clearly what you could not verify.

STEP 4 (MANDATORY): Finish by updating AGENT_MEMORY.txt:
 - tick completed items in the FEATURE CHECKLIST
 - update CURRENT STATUS and NEXT STEP
 - add new DECISIONS, DEPENDENCIES, PERMISSIONS, KNOWN ISSUES if any
 - append a SESSION LOG entry (Did / Decided / Verified / Not verified / Next)
 - update the folder layout in section 4 if it changed
Then print the ENTIRE updated AGENT_MEMORY.txt in one code block.
```

---

# PHASE 0 – Setup

## Prompt 01 – Project skeleton

```
TASK 0.1 + 0.2: Create the project skeleton.

- Kotlin + Jetpack Compose + Material 3, single activity, Compose Navigation.
- Gradle Kotlin DSL with a version catalog (libs.versions.toml).
- Create the package layout from section 4 of AGENT_MEMORY.txt (empty packages
  may hold a short placeholder file).
- Theme package with light, dark and AMOLED color schemes and a calm green palette.
- A bottom navigation shell with 4 placeholder tabs: Garden, Focus, Insights, Settings.
- Manifest MUST NOT include the INTERNET permission. Add android:allowBackup="false".
- Propose and record decisions for: DI (manual vs Hilt), minSdk, applicationId.
  Recommend the simplest option and explain in 2-3 lines.
- List the exact commands/steps for me to create and run this in Android Studio.

Do not implement any real features yet.
```

## Prompt 02 – Repo hygiene and CI

```
TASK 0.3: Repository setup.

- .gitignore for Android/Kotlin.
- GitHub Actions workflow: build debug APK, run lint and unit tests on pull requests.
- Add ktlint or detekt config (pick one, justify briefly).
- README stub: project pitch, privacy promise (no internet, no tracking), status badge
  placeholder, "work in progress" notice.
- CONTRIBUTING stub and a pull request template.
- Do NOT choose a license yourself. Create a LICENSE-PENDING.md that lists the
  options (GPL-3.0, Apache-2.0, MIT) with a 2-line pro/con for each, and add a
  question to the OPEN QUESTIONS section of AGENT_MEMORY.txt.
```

---

# PHASE 1 – Data

## Prompt 03 – Database

```
TASK 1.1: Room database.

- Implement entities, DAOs and the database class for the models in section 6
  (DailyStat, AppUsageDaily, AppLimit, Plant, FocusSession, Badge).
- Use epoch millis in UTC. Add a single time helper in core/ that defines
  "start of local day" and use it everywhere.
- Export Room schemas and set up a migration test skeleton.
- Provide repository interfaces (not full logic yet) in data/repo.
- Add in-memory Room tests for each DAO (insert, query, update, delete).
- Record dependencies and their licenses in section 7.
```

## Prompt 04 – Settings store

```
TASK 1.2: DataStore settings.

- Wrap Preferences DataStore in a SettingsRepository exposing Flow for:
  dailyGoalMs, pickupGoal, bedtimeStart, bedtimeEnd, theme (light/dark/amoled/system),
  accentColor, streakThreshold, gentleMode, notificationsOn, onboardingDone.
- Sensible defaults: daily goal 3h, pickup goal 80, streak threshold 60, gentleMode true.
- Unit tests for defaults and updates.
- Wire the chosen theme setting into the app theme.
```

## Prompt 05 – Usage stats repository

```
TASK 1.3: Usage statistics.

- Implement UsageRepository using UsageStatsManager and UsageEvents:
  total screen time for a day, per-app usage for a day, pickup count and unlock count.
- Handle: permission not granted (return a clear state, never crash), day
  boundaries using the core time helper, apps with no launcher icon, and the
  difference between foreground time and "last time used".
- Provide a function to check whether Usage access is granted and an Intent to open
  the correct settings screen.
- Put the Android-specific code behind an interface so the rest can be unit tested
  with a fake.
- Explain any Android version behavior you are not 100% sure about instead of guessing.
```

## Prompt 06 – Daily sync worker

```
TASK 1.4: Daily sync.

- WorkManager periodic worker that reads usage for yesterday and today and writes
  DailyStat and AppUsageDaily rows (idempotent: safe to run many times).
- Also sync when the app opens.
- Backfill up to the last 7 days on first run (usage history availability is limited
  by Android, document that in code comments).
- Tests for the idempotency logic using fakes.
```

---

# PHASE 2 – Onboarding

## Prompt 07 – Onboarding and permissions

```
TASK 2.1 + 2.2: Onboarding flow.

- 3-4 calm intro screens explaining: the garden idea, privacy (no internet, local only),
  and why Usage access is needed.
- Permission step: button that opens Usage access settings, then detects the grant when
  the user returns (lifecycle aware) and continues. Allow "skip for now" with a clear
  explanation that the garden will stay empty without it.
- Notification permission step (Android 13+), optional.
- Goal setup: daily screen-time goal slider, pickup goal, bedtime window.
- Save to SettingsRepository and set onboardingDone.
- Show onboarding only on first launch. Add UI tests for the happy path.
- All text in strings.xml and accessible.
```

---

# PHASE 3 – Garden core

## Prompt 08 – Health score formula

```
TASK 3.1: Daily Health Score.

- Design a simple, explainable score from 0-100 using: screen time vs goal,
  per-app limit breaches, pickups vs goal, bedtime-window usage, focus minutes.
- Put it in domain/garden as pure Kotlin (no Android imports).
- Make weights configurable constants. Score must degrade smoothly, never cliff-edge.
- Write the formula in plain language into section 5.1 of AGENT_MEMORY.txt.
- Extensive unit tests with edge cases (zero usage, huge usage, missing data, no limits set).
```

## Prompt 09 – Plant lifecycle logic

```
TASK 3.2: Plant lifecycle.

- Pure Kotlin domain logic for stages SEED, SPROUT, BUD, BLOOM, plus WILTING and RECOVERING.
- Inputs: daily score, focus completions, streak bonus. Output: updated growth points,
  stage, health.
- Wilting is gradual over several days and ALWAYS recoverable. Add tests that prove
  a plant can never be permanently lost.
- A "GardenEngine" that, given today's stats and the current plants, returns the new
  garden state. Include rules for planting a new plant (e.g. one new seed after a
  good day) and a cap on number of plants.
- Document the rules in section 5.2 of AGENT_MEMORY.txt.
```

## Prompt 10 – Garden screen

```
TASK 3.3: Garden UI.

- Main Garden screen: a calm scene (sky, ground), plants placed on a small plot,
  today's score as a gentle ring or badge, streak count, and a primary button to start focus.
- Render plants with Compose Canvas using simple vector shapes for now (flower, bonsai,
  cactus), so the app has no art dependency yet. Make the drawing code pluggable so image
  assets from plant packs can replace it later.
- Tap a plant to see a bottom sheet: name, stage, health, planted date.
- State handled in a GardenViewModel exposing a single UiState.
- Empty states: no permission, no data yet.
- Previews and one Compose UI test.
```

## Prompt 11 – Animations

```
TASK 3.4: Growth and wilt animations.

- Smooth animated transitions between stages (scale, sway, bloom opening).
- Gentle idle motion (sway, floating particles, butterflies appear on good streaks).
- Wilting visual: color desaturates, plant droops. Recovery animation: color returns.
- Respect the system "reduce animations" setting and keep CPU/battery use low
  (stop animating when the screen is not visible).
- Keep drawing logic separate from state so it stays testable.
```

---

# PHASE 4 – Focus

## Prompt 12 – Focus logic and service

```
TASK 4.1: Focus sessions.

- Domain logic for FocusSession (planned time, actual time, tag, status:
  RUNNING, COMPLETED, FAILED_SOFT, CANCELLED).
- A foreground service that runs the timer, shows an ongoing notification with remaining
  time and a stop action, and survives the app being closed.
- Correct foreground service type declaration and permissions for current Android versions.
  If unsure which type applies, say so and explain the options.
- Persist sessions to Room. Handle process death and device reboot sensibly.
- Unit tests for the session state machine.
```

## Prompt 13 – Focus UI

```
TASK 4.2: Focus UI.

- Focus tab: duration picker (15/25/45/60 + custom), tag chips (Study, Work, Reading,
  Other), "Plant a seed" start button.
- Running screen: large calm countdown with the seed growing as time passes.
- Result screen: completed celebration or a gentle soft-fail message, plus reward summary.
- Stats row: sessions today, total focus minutes this week.
- Keep screen-on optional via a setting.
```

## Prompt 14 – Soft fail and rewards

```
TASK 4.3: Soft-fail and recovery.

- Detect "phone used during session" using screen-on events and usage events
  (gentle mode, no blocking). Add a grace period (default 30 seconds).
- Soft fail = small growth setback, never kills a plant.
- Completed sessions award growth points and can revive wilting plants.
- Hook rewards into GardenEngine and add tests.
```

---

# PHASE 5 – Habits

## Prompt 15 – Per-app limits

```
TASK 5.1: Per-app limits.

- UI to list installed launchable apps (with search), set a daily limit, enable or disable it.
- Optional category grouping (social, video, games, other) using ApplicationInfo
  categories where available, with manual override.
- Store in AppLimit table. Feed breaches into the health score.
- Handle the Android 11+ package visibility rules correctly and explain which
  manifest declaration is used and why (prefer the least invasive approach).
```

## Prompt 16 – Gentle notifications

```
TASK 5.2: Nudges.

- Notification channels with clear names and user control.
- Nudges at 50%, 80% and 100% of daily goal and of each app limit, once per threshold per day.
- Friendly, non-shaming copy (put all text in strings.xml with a few variations).
- Quiet hours respect bedtime window. Respect notificationsOn and permission state.
- Use WorkManager or a lightweight periodic check. Explain the battery tradeoff and
  the accuracy you can realistically achieve.
```

## Prompt 17 – Bedtime mode

```
TASK 5.3: Bedtime mode.

- Wind-down window from settings. Usage inside the window reduces growth and tints
  the garden to a calm night look.
- A "sleep garden" screen shown when opening the app in the window.
- Optional evening reminder to start winding down.
```

## Prompt 18 – Streaks and badges

```
TASK 5.4: Streaks, compassion days, badges.

- Streak logic per section 5.4 with a configurable threshold.
- Compassion day: user marks today or a past day as off, streak preserved, day excluded
  from averages. Limit use sensibly (e.g. a few per month) but make it configurable.
- Badges: 7-day streak, 30-day streak, 10 focus sessions in a week, first bloom, first
  recovery, etc. Pure Kotlin rules + tests + simple badge screen.
```

---

# PHASE 6 – Insights

## Prompt 19 – Charts

```
TASK 6.1: Insights.

- Insights tab: day/week/month toggle, bar chart of screen time vs goal, line of score,
  top apps list, pickups, peak usage hours heatmap.
- Use Vico or custom Canvas charts (justify, record license).
- Handle empty and partial data gracefully. Add accessibility descriptions for charts.
```

## Prompt 20 – Weekly reflection and timeline

```
TASK 6.2 + 6.3: Reflection and history.

- Weekly reflection card with calm copy: time saved vs goal, best day, focus minutes,
  one small suggestion. Generated locally from templates (no AI, no network).
- Garden history timeline: scrollable weekly/monthly snapshots rendered from stored data.
```

---

# PHASE 7 – Polish

## Prompt 21 – Day/night, seasons, weather

```
TASK 7.1: Environment.

- Sky, lighting and ground change with the real local time (day, sunset, night).
- Seasons derived from the device date (and hemisphere setting, default northern).
- Simple weather visuals (rain, snow, sun rays, fireflies) using Canvas particles. Weather
  is visual only, chosen from time/season/score, NOT from any network data.
- Performance budget: stay smooth on a low-end device, stop when invisible, add a
  "low power visuals" toggle.
```

## Prompt 22 – Themes and customization

```
TASK 7.2: Theming.

- Light, dark, AMOLED, system. Accent color picker with a few curated palettes.
- Optional dynamic color on Android 12+.
- Settings screen sections: Goals, Notifications, Appearance, Focus, Data, About.
```

## Prompt 23 – Widget and tile

```
TASK 7.3: Widget and Quick Settings tile.

- Jetpack Glance widget showing today's garden status (score, streak, a simple plant
  drawing) in small and medium sizes. Update when data changes and on a schedule.
- Quick Settings tile to start/stop a focus session.
- Explain any limits on widget update frequency.
```

## Prompt 24 – Snapshot export

```
TASK 7.4: Share a snapshot.

- Render the garden to a bitmap (with optional date/streak caption) and save it via the
  system share sheet or Storage Access Framework.
- No storage-wide permissions. Use MediaStore or SAF only.
```

---

# PHASE 8 – Platform

## Prompt 25 – Plant pack system

```
TASK 8.1: Plant packs.

- Define a versioned plant.json schema (id, name, description, unlock rule, stage image
  names, license, author, schemaVersion) and a loader that validates it and fails safely.
- Support built-in packs in assets and (optionally) user-imported packs via SAF.
- Unlock rules: streak length, focus count, season, or default.
- Write docs/PLANT_PACKS.md with a step-by-step guide and an example pack, plus a
  validation script or unit test that contributors can run.
- Document the schema in section 5.5 of AGENT_MEMORY.txt.
```

## Prompt 26 – Export and import

```
TASK 8.2: Backup.

- Export all data (settings, daily stats, plants, sessions, badges, limits) to one JSON
  file via SAF, with a schema version.
- Import with validation, preview of what will change, and a safe merge/replace choice.
- Tests including corrupted and older-version files.
```

## Prompt 27 – Battery help

```
TASK 8.3: Reliability help.

- A Help screen explaining how to exclude the app from aggressive battery optimization,
  with brand-specific steps (Samsung, Xiaomi, Oppo, Vivo, Huawei, OnePlus, stock Android).
- Detect manufacturer and show the relevant section first.
- Mark steps as "may vary by version" rather than claiming certainty.
```

## Prompt 28 – Localization

```
TASK 8.4: Localization.

- Audit for hard-coded strings and move them to strings.xml.
- Make layouts RTL-safe. Use plurals and formatted time properly.
- Add a CONTRIBUTING section on adding translations. Add one sample translation.
```

---

# PHASE 9 – Optional / later

## Prompt 29 – Focus app blocking (opt-in)

```
TASK 9.1: Optional blocking.

- OFF by default. Clear explanation screen before asking for any special permission.
- Compare options (Accessibility service vs overlay vs usage-event polling), recommend
  the least invasive one, and implement it with a user-editable block list and allow list.
- Never collect or store what the accessibility service sees. State this in code and README.
- Note the Google Play policy risk for accessibility use; the app is distributed through GitHub only.
```

## Prompt 30 – Live wallpaper

```
TASK 9.2: Live wallpaper.

- WallpaperService that renders the current garden with the same drawing code.
- Battery-friendly: low frame rate, pause when the screen is off or the battery is low.
- Settings for scene options and a preview.
```

---

# PHASE 10 – Release

## Prompt 31 – Testing pass

```
TASK 10.1: Quality pass.

- Review the whole repo against AGENT_MEMORY.txt rules: no INTERNET permission, no
  proprietary libraries, no hard-coded strings, no TODOs missing from KNOWN ISSUES.
- Run/describe lint, unit and UI tests. List failing or missing tests and add them.
- Check performance risks (recomposition, background work) and accessibility.
- Produce a prioritized fix list and fix the top items.
```

## Prompt 32 – Documentation and release assets

```
TASK 10.2: Docs.

- Write a polished README: pitch, features, privacy promise, screenshots placeholders,
  install options, build instructions, architecture overview, roadmap, contributing, FAQ
  ("Why no internet permission?", "Why does it need Usage access?").
- CONTRIBUTING.md, CODE_OF_CONDUCT.md, SECURITY.md, issue templates, "good first issue"
  suggestions.
- Insert the license chosen by the human (check AGENT_MEMORY.txt DECISIONS; if still
  unchosen, ask).
```

## Prompt 33 – GitHub release automation

```
TASK 10.3: Release.

- Verify the build is reproducible-friendly: no proprietary blobs, pinned versions.
- GitHub Actions workflow that builds a signed release APK on tag, using secrets the
  human sets up, and attaches it to a GitHub Release.
- Publish the verified signed APK and clear release notes on GitHub Releases.
- Publish a professional GitHub Pages site with emulator-only fake debug screenshots,
  privacy information, installation instructions and a link to the release.
- Do not prepare or submit an F-Droid release. GitHub free hosting is the only target.
```

---

# UTILITY PROMPTS (use any time)

## Prompt R1 – Resume (new chat or new model)

```
Read the AGENT_MEMORY.txt below (or in the project). Summarize in 8 lines or fewer:
current status, what is done, what is next, known issues, open questions.
Then ask me which task to do, suggesting NEXT STEP. Do not write code yet.

[paste AGENT_MEMORY.txt here if you have no file access]
```

## Prompt R2 – Fix a bug

```
Read AGENT_MEMORY.txt first.
BUG: <describe what happens, what you expected, steps to reproduce>
LOGS/ERROR: <paste stack trace>
RELEVANT FILES: <paste or attach>

Find the root cause before changing code. Explain it in 3 lines, then give the minimal fix.
Add a test that would have caught it. Update KNOWN ISSUES and SESSION LOG in
AGENT_MEMORY.txt and print the whole file.
```

## Prompt R3 – Review my code

```
Read AGENT_MEMORY.txt first, then review the files below against its HARD RULES,
CONVENTIONS and DECISIONS. Report: rule violations, bugs, performance and battery risks,
accessibility gaps, missing tests. Rank by severity. Do not rewrite everything; propose
targeted fixes. Update KNOWN ISSUES in AGENT_MEMORY.txt and print the whole file.

<paste code>
```

## Prompt R4 – Add a feature not in the plan

```
Read AGENT_MEMORY.txt first. I want to add: <feature>.
1) Check it against HARD RULES (privacy, permissions, dependencies). Say if it conflicts.
2) Propose the smallest design and where it fits in the architecture.
3) Add it to the FEATURE CHECKLIST as a new numbered item, then implement it.
Update AGENT_MEMORY.txt and print the whole file.
```

## Prompt R5 – End of session handoff

```
We are stopping for now. Update AGENT_MEMORY.txt so another AI can continue seamlessly:
make CURRENT STATUS accurate, list exactly what is half-done (files + what remains),
write the single best NEXT STEP, record anything not verified, and append a SESSION LOG
entry. Print the entire file.
```

## Prompt R6 – Memory sync check (use if code and memory drift)

```
Compare AGENT_MEMORY.txt with the code I paste below. List every mismatch (folder layout,
dependencies, permissions, data models, checklist status). Propose corrections, then output
the corrected full AGENT_MEMORY.txt.

<paste file tree + key files>
```

---

## Suggested order

0: 01 → 02 · 1: 03 → 06 · 2: 07 · 3: 08 → 11 · 4: 12 → 14 · 5: 15 → 18 · 6: 19 → 20 · 7: 21 → 24 · 8: 25 → 28 · 9: 29 → 30 (optional) · 10: 31 → 33

**Minimum viable version:** prompts 01, 03–11. That gives a working garden that reads screen time and grows plants. Add focus (12–14) next, then everything else.

## Good habits

- One prompt per chat session when possible. Smaller chats stay accurate.
- After every task, build and run the app before moving on. Paste any compiler errors back with Prompt R2.
- Commit to Git after every finished prompt.
- Always save the updated `AGENT_MEMORY.txt` before closing the chat.
