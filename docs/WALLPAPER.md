# Your garden on the Home screen

Open **Settings → Preview live wallpaper**. Android shows its preview and owns the
apply/cancel controls. ScreenGarden never changes your wallpaper automatically.
Saved theme, accent, hemisphere and low power choices customize the same local art.

The scene stays still and refreshes at most once a minute while visible. Low power
visuals refresh at most every five minutes. Hidden wallpaper, a sleeping screen,
battery at or below 15%, and Android battery saver stop recurring work. A cold
low-battery preview may render one still image before pausing. Changes to your garden
appear on the next visible refresh. No usage events, rewards or network are involved.

Plant packs and fallback shapes use the same renderer as the Garden and picture
export. Portrait images are bounded to 720 pixels wide and 2048 high before Android
scales them to the surface. An empty garden shows the scenery without invented plants.
Background allowances, exact refresh timing and launcher presentation vary by device.

The service binding uses Android's `BIND_WALLPAPER` protection; it is not a user
permission request. Preview screenshots use the emulator and a debug-only fake garden;
the release build has no demo provider. No schema, preference or dependency was added.

Reference: [Android WallpaperService](https://developer.android.com/reference/android/service/wallpaper/WallpaperService).
