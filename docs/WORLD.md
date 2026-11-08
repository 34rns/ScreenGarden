# Your world, Focus and Desk mode

Your plants now share a growing field. Older plants remain in your world and a
good completed day can add another seed. A difficult day never deletes a plant.

## Explore

Open **World**, drag the field to pan, and pinch to zoom. The camera buttons offer
the same controls without gestures. **Return to the first planting** resets the
view. **All your plants** lists the whole collection, including plants outside
the current view; choose one to center it and open its details.
If replacing a backup leaves the camera away from your new plants, use Reset
or select a plant from the list to return to the field.

Only a limited number of visible plants are drawn at once. This keeps a large
collection manageable without deleting older plants. Placement follows saved
planting order; no database upgrade is needed. Importing an older collection or
deleting data can change that order. Snapshots and wallpaper use the same scene
and start at the first planting; snapshots do not record your current camera.

## Focus in your world

Start a session from **Focus**. Your world opens fullscreen, with a small timer
and a plant growing during the session. This animation is a preview; the existing
session rules award saved growth when the timer ends.

Tap **Exit view**, use Android Back or its back-swipe gesture to return to the
Focus controls. The session keeps running. **Enter your world** reopens the view;
**Stop focus** explicitly ends the session. Fullscreen Focus keeps the screen on
while visible. The existing optional screen-on setting applies outside fullscreen.
Usage access, the grace period and gentle soft-fail behavior are unchanged.

## Use Desk mode

In **Settings → Appearance → Desk mode**, enable the option, then tap **Start
Desk mode**. When Android reports charging, the button offers to start the view.
Charging never starts it automatically. The mode also works manually while unplugged.
Use **Show a clock** to choose whether the clock appears.

Desk uses a mostly black scene, a local window brightness of 6%, and at most
12 frames per second. The scene and controls shift by a few pixels every
30 seconds. Reduced animations, battery saver and low-power visuals stop decorative
motion; the slow protection shift continues. Back or **Exit view** closes it.
Brightness and screen-on overrides are restored on pause and exit.

Pixel shifting and dimming reduce static exposure but **cannot guarantee prevention
of OLED burn-in**. Avoid leaving any ambient view on indefinitely. Display hardware
and manufacturer brightness behavior vary; real battery consumption, heat and
long-term panel wear have not been measured. No extra permission is requested.

## Artwork and privacy

The meadow is drawn locally with code and the original bundled plant packs.
Android system typography adds no downloaded font. Code remains GPL-3.0-or-later;
original art remains CC BY-SA 4.0. The same renderer serves World, Focus, Desk,
widgets, wallpaper and exported pictures. It reads no usage data itself and makes
no network requests. All public screenshots use fake debug-only emulator data.
