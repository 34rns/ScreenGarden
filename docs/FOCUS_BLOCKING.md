# Optional focus app blocking

Blocking is off by default. In **Settings → Focus app blocking**, choose the apps you want to
block or always allow. Read the explanation before opening Android accessibility settings.
Enable ScreenGarden's service there, return, and turn on the separate blocking switch.
Enabling the Android service alone does not turn blocking on.

During a fresh running focus timer, opening a blocked app returns to Home. The allow list wins
over the block list. ScreenGarden, Home, Settings, system UI, permission dialogs and known
calling handlers stay available. Non-launchable packages are never redirected. You can return
to ScreenGarden to stop focus, turn the switch off, or remove Android's service permission.

This is a voluntary focus aid. It cannot prevent uninstalling, force-stop, permission removal
or every Android/OEM event delay. A stale, rebooted or ended timer cannot redirect apps. Newly
installed apps appear in the editor on reopening; restart the accessibility service if Android
has not refreshed the launchable-app inventory. No physical-device reliability claim is made.

## Options considered

| Option | Access and practical limit | Decision |
| --- | --- | --- |
| Usage-event polling | Reuses Usage access, but needs repeated reads and background activity launches are restricted. | Keep it for gentle soft-fail detection; it cannot reliably implement immediate blocking. |
| Draw-over-apps overlay | Adds a broad drawing permission, continuous polling and an overlay; the underlying app remains open. | Omit it. |
| Restricted accessibility events | Adds an explicit service grant, but can use Android's Home action when an app opens. | Use package-only window-state events with screen-content retrieval disabled. |

The implemented service never reads accessibility nodes, event text, screen views or passwords.
It compares the event's package name transiently and discards it. It never logs or persists
observed packages or events. Only the lists you edit and your enable choice are stored locally.
There is no network permission, upload, paid service or dependency.

Backup v2 includes your selected lists; importing v1 starts with empty lists. Every restore
leaves blocking off. Android grants are not backed up. Room remains at schema v7.

## Distribution and policy

Android may show broad warnings when enabling an accessibility service. This service is an
optional focus tool and does not claim to be an accessibility aid. Google Play places additional
disclosure, declaration and use restrictions on non-accessibility tools using its API; acceptance
is not guaranteed. ScreenGarden distributes through free GitHub Releases and Pages only.

Platform references: [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService)
and [service configuration](https://developer.android.com/guide/topics/ui/accessibility/service).
