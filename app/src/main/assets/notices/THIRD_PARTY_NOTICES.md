# Third-party notices

ScreenGarden uses unmodified open-source libraries. Their licenses do not change
the app's GPL-3.0-or-later code license. Original art is separately CC BY-SA 4.0.

| Component | License | Attribution/source |
| --- | --- | --- |
| AndroidX Core, Activity, Lifecycle, Navigation, Compose/Material 3, Room, DataStore, WorkManager and Glance | Apache-2.0 | The Android Open Source Project; [AndroidX source](https://android.googlesource.com/platform/frameworks/support/) |
| Kotlin standard library and coroutines | Apache-2.0 | JetBrains and contributors; [Kotlin](https://github.com/JetBrains/kotlin), [coroutines](https://github.com/Kotlin/kotlinx.coroutines) |
| Okio | Apache-2.0 | Square and contributors; [Okio](https://github.com/square/okio) |
| Glance's shaded Protocol Buffers | BSD-3-Clause | Copyright 2008 Google Inc.; [Protocol Buffers](https://github.com/protocolbuffers/protobuf) |

Complete Apache-2.0, Protocol Buffers BSD notice/disclaimer, and app GPL text are
included in `app/src/main/assets/notices/`, together with this attribution.
The BSD notice is retained verbatim. Pack metadata retains authors and art license.
Corresponding app source is available at every release tag.

JUnit, Robolectric, ktlint, Android test tools and build plugins do not ship in the
release APK. Their licenses are in `AGENT_MEMORY.txt`, Section 8. No proprietary
SDK or paid asset is bundled.
