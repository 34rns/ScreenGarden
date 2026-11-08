# When background focus is interrupted

Open **Settings → About → Help focus keep going**. ScreenGarden puts the phone manufacturer's guide
first. All seven guides are available locally: Samsung, Xiaomi, Oppo, Vivo, Huawei, OnePlus and
stock Android/other. The app does not save a device identifier.

The guide offers common battery/background/autostart menu names, with a visible warning that they
vary by Android version, vendor release, region and model. They are suggestions, not verified paths
for every device. The app-settings button opens Android's settings for ScreenGarden only. It does
not request a battery exemption permission or change battery settings automatically.

Allowing background activity can increase battery use. Restrict the change to ScreenGarden and
reverse it in the same device menu if needed. An exemption cannot make scheduled checks exact,
override a force stop, or guarantee foreground-service recovery. Reminders remain approximate.

Official Android references checked during implementation:

- [Doze and App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)
- [App settings intent](https://developer.android.com/reference/android/provider/Settings#ACTION_APPLICATION_DETAILS_SETTINGS)

Verified coverage is recorded in AGENT_MEMORY.txt. Manufacturer-specific physical devices have not
been tested; only free Android emulators are used for published screenshots.
