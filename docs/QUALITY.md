# Quality and verification

Last reviewed: 8 November 2026. Screenshots use only debug demo data on the
dedicated Android 15 emulator. No physical-device screenshots or data were used.

## Release checks

The required checks are `ktlintCheck --no-build-cache`,
`:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`.
The current suite has 384 JVM tests, including real Room operations, Compose UI,
settings, usage boundaries, recoverable growth, backup validation and localization.

The full Android 15 integration suite passed **20 tests, no failures or skips**.
It covers all published migration paths through Room v7, tagged focus completion,
soft failure, notification stop, optional blocking, reminders, tile control,
plant-pack assets and the saved bedtime route. The first run failed one test
because a fresh installation did not retain Usage access; its fixture now grants
and restores that access itself, and the complete rerun passed.

`scripts/Check-Repository.ps1` checks the actual merged manifest when supplied,
disabled automatic backup, production Activity count, Kotlin license headers,
private-file hygiene, and SHA-256 checksums of the seven immutable Room schemas.
A deliberate invalid-manifest probe proved that the check rejects INTERNET.
CI runs this check alongside the four Gradle gates.

Android lint has **zero errors and six accepted warnings**: guarded notification
API, pre-33 locale metadata, the score ratio wording, disabled-backup metadata,
version-qualified launcher resources, and deliberate synchronous reminder-ledger
commit. There is no lint baseline or new suppression hiding these warnings.

## Review and fixes

1. Fixed the emulator test's dependence on a manually retained Usage access grant.
2. Added enforceable offline/backup and immutable-schema checks to CI.
3. Updated CI actions to verified, immutable release commits using Node 24.
4. Replaced the deprecated Kotlin target configuration with `compilerOptions`,
   preserving Java/Kotlin 17 and the existing dependency versions.

Focused source review found no hard-coded Compose text, undocumented TODO/FIXME,
network client, proprietary runtime library, or Android import in domain/core.
Drawing uses one visible scene clock with bounded particles. Reduced animations,
battery saver, low-power visuals and hidden destinations stop scene motion.
Wallpaper rendering is bounded and static; workers respect Android's scheduling
limits. Export/import uses bounded streams off the main thread.

Charts include readable values and semantic descriptions. Controls use Material
touch targets, headings, wrapping and start/end layout. Existing English, Spanish,
RTL, permission, empty and retry-state regression tests remain part of the suite.
Additional small-phone, tablet and landscape emulator captures use 1.3x text.

## Practical limits

P35 adds bounded camera/zoom and large-world mapping, mode/rendering limits,
ephemeral focus growth, RTL targets, fullscreen exit without cancellation,
Desk settings/charging offer, window restoration and backup-v3 compatibility
tests. Extraction preserved five old app frames pixel-for-pixel below the Android
status bar. Review found and fixed explicit sway loss and wallpaper dock overlap
with a forty-plant regression case. No new permission, dependency or Room version.

P35 emulator frame sampling over ten seconds recorded 227 World frames,
132 Focus frames and 108 Desk frames. All three recorded zero frames during
three-second reduced-motion and hidden-window samples. Desk's 30-second protection
shift is checked separately. A failing-first clock test fixes English time reading order in RTL. Fullscreen tests verify cutout coverage, screen-on,
6% Desk window brightness, Back exit and restoration after Home.

The software-rendered emulator reported high frame delays (median approximately
30 ms World, 32 ms Focus and 36 ms Desk). These measurements verify bounded work
and pausing; they do not establish smoothness on a low-end phone or battery savings.
Use low-power visuals for a still scene. The screenshot matrix covers World,
fullscreen Focus, Desk and Desk settings in light, dark, AMOLED, 1.3× text and RTL.

Compilation and emulator testing do not establish real-device reliability.
Physical-device/OEM Doze behavior, long-term battery consumption, TalkBack listening,
low-end performance, actual process-death/reboot recovery and native-speaker Spanish
review remain unverified. API 26 usage-event behavior was measured in the earlier
P05b pass; the full current UI/release has not been reverified on API 26.

Usage history and screen events depend on Android/OEM evidence. Missing evidence
stays unknown. Reminders and widgets are approximate; focus completion during
device sleep may be delayed. Optional blocking is a voluntary aid. Backup files
are unencrypted and can contain private usage; saving/sharing is user initiated.

Signing, R8 release install/launch and the public download/site are verified
separately during P33; this report does not claim them before they occur.
