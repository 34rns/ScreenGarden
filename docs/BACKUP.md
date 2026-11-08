# Your backup stays under your control

In **Settings → Data**, choose **Save a backup**. Android's file picker lets you choose a destination.
The JSON file contains settings and the seven user-data tables: daily totals, per-app usage, plants,
focus sessions, app limits, milestones and actual garden history. It includes app package names and
timestamps. It is **not encrypted**. Choose a private destination; a system file provider may itself
sync files, but ScreenGarden never uploads, schedules exports or requests internet access.

Choose **Restore a backup** to inspect its save date and record counts before changing anything.

- **Merge** adds records whose keys are absent. Existing records and settings win conflicts. Focus
  totals on conflicting days also stay unchanged; use Replace to restore the file's exact totals.
  Importing the same file again is safe.
- **Replace** removes the current seven user-data tables and restores the chosen file and settings.
  A second confirmation is required. Save your current backup first if you want to retain it.

Stop a running focus session before restoring. Imported running sessions become cancelled; they never
start a service or earn rewards. Merge marks imported past days as processed to avoid rewarding local
plants again; today's saved marker stays intact. Replace preserves the file's daily totals and growth
markers together with its plants. Existing earned milestones keep their timestamps. App permission
grants, notification-channel settings, delivery ledgers, caches and plant-pack artwork are not imported.

Validation finishes before database writes. The database replacement is one Room transaction. A small
`pending_restore` receipt commits the desired settings in that same transaction; DataStore updates all
settings in one edit, then the receipt is removed. A disk error or process interruption after the
database commit leaves that settings update pending, which ScreenGarden retries before its normal
screens, usage worker or nudge worker run. The UI reports an incomplete restore instead of claiming
success. If recovery fails, free device storage and use **Try again**.

The first published document format is schema version **1**, identified by `format: "ScreenGarden"`.
Top-level fields are `format`, `schemaVersion`, `createdAtUtcMillis`, `settings`, and `tables`.
Table column names mirror the Room user-data schema; booleans are JSON booleans and timestamps are
UTC epoch milliseconds. Stored day keys retain their original local-day instants when travelling.
The internal recovery receipt is not part of an exported document.

Wrong types, unknown/missing tables or columns, duplicate record keys, invalid settings, corrupt history
payloads, negative or out-of-range counters and trailing content are rejected. Files are limited to
**32 MiB** and **100,000 total records** to bound memory use. Larger exports report a failure, without
deleting stored history. Versions older/newer than v1 are rejected: no earlier backup format was
published, so no guessed migration is applied. A future format must define and test its upgrades.

Cancelling either file picker changes nothing. If Android kills the process while the save picker is
open, the unsaved export snapshot is lost and saving reports an error; make a new backup. A provider
write failure can leave an incomplete destination file, so retry and keep a known-good backup.

Contributor checks:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests '*Backup*Test'
./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.screengarden.app.data.local.BackupMigrationTest
```

These cover malformed/unsupported files, replacement, repeated merge, active-session refusal, cancelled
imported timers, receipt recovery, replacement confirmation and preservation through the v7 migration.
