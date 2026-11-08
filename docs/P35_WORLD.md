# P35 world overhaul

Owner request: refactor the existing local garden into a shared world renderer, a
pan/zoom World tab, fullscreen Focus and opt-in low-power Desk mode, then release
v1.1.0. Existing habits, permissions, backup, focus rewards, packs, widgets,
wallpaper and exports must continue working. No paid service or new permission.

## Audit

- `GardenScene.kt` owns backdrop, weather, layout, plant controls and animations.
- `PlantRenderer.kt` and `PackPlantRenderer.kt` already share species drawing.
- `GardenSnapshotImage.kt` repeats grid placement and stage/health visual mapping;
  Android bitmap/text code is mixed with the scene composition.
- `GardenWidget.kt` repeats stage/health mapping and owns a separate plant bitmap.
- `FocusScreen.kt` draws a temporary flower independently of the owner's plants.
- `GardenWallpaperService.kt` creates a GardenUiState and calls the export renderer.
- `GardenMotion.kt` mixes the platform motion/visibility adapter and scene clock.
- History/details reuse individual plant art; their rendering must stay shared.

## Design decisions

Use pure Kotlin world/camera/policy models and Compose's portable DrawScope for
drawing. Android window, lifecycle, bitmap, font measurement and wallpaper surface
code remain adapters. All clocks are explicit drawing inputs; no data queries or
random generation in the renderer. Reuse validated pack art and vector fallback.

First preserve existing pixels through extraction, then change the appearance.
The new look is a layered meadow with quiet earth/green/dusk colors and rounded
system typography; original vector graphics retain CC BY-SA 4.0. No downloaded
font, new runtime library or license dependency is required.

Positions derive deterministically from persisted plant identity and planting
order. Normal new planting appends without moving prior plants. No Room schema
change is intended; existing schema v7 and immutable versions 1–7 remain. If
persistence becomes necessary, add v8 plus migration/tests; never replace v2.
The world may hold more than five plants; rendering uses viewport culling and a
mode cap, rather than deleting history for performance.

Focus preview growth is ephemeral and uses the existing persisted session. Only
the unchanged terminal-session transaction awards actual points. Exiting the
fullscreen view leaves the timer running; stopping is an explicit separate action.

Desk is opt-in, mostly black even with a light theme, with local window brightness,
slow pixel shifts and an optional shifting clock. Manual start always works;
charging may offer entry but never starts it automatically. No DreamService is
needed for this task. Reduced motion/low power use still frames with infrequent
protection shifts. Pixel shifting cannot guarantee absence of OLED burn-in.

## Small-step implementation plan

> Agent execution: implement inline with executing-plans, using this file as the
> spec/ledger. Keep each milestone independently verifiable and commit it.

**Goal:** one offline world across six render modes, released as v1.1.0.
**Architecture:** pure world mapping/camera/policies → shared Compose renderer →
UI, bitmap, widget and wallpaper adapters. Existing repositories own all rewards.
**Tech stack:** existing Kotlin/Compose/Room/DataStore/Glance, PowerShell and Gradle.

### 1. Extract without visual changes

- [x] Capture current fake-only Garden themes, widget and snapshot on the emulator.
- [x] Add `domain/world/WorldState.kt`, `WorldCamera.kt`, `RenderMode.kt` and tests
  for finite camera bounds, zoom anchors, deterministic placement and mode limits.
- [x] Add `ui/world/WorldRenderer.kt`; move stage/health and backdrop/particle
  drawing there. Existing Garden, individual art, widget, snapshot and wallpaper
  must call it, with platform types confined to adapters.
- [x] Compare before/after emulator pixels; run all gates, update memory, commit/CI.

### 2. Persistent field and World interaction

- [ ] Add `ui/world/WorldScene.kt`, bounded pan/pinch zoom, reset/zoom buttons and
  screen-reader/keyboard alternatives. Preserve plant detail, score, empty/error,
  permission, bedtime and export flows; rename the tab to World.
- [ ] Extend good-day planting beyond the old cap while preserving all old plants;
  test camera recovery, mapping after a new plant and a large visible field.
- [ ] Run gates and emulator interactions/captures, checkpoint, commit/CI.

### 3. New shared appearance

- [ ] Update Color/Type/Shape/environment tokens and original scene/vector art.
  Preserve curated accents, true-black AMOLED and large-text/RTL legibility.
- [ ] Refresh affected captures and site originals; inspect each, gates, commit/CI.

### 4. Fullscreen Focus and Desk

- [ ] Add `ui/world/WorldAmbientScreen.kt` and the single-Activity window adapter.
  Focus shows the owner's field, ephemeral growing plant, small timer, accessible
  exit/stop, screen-on flag and system back. Keep soft-fail/service rules unchanged.
- [ ] Add validated local Desk enabled/clock settings and backup compatibility;
  dim and restore window brightness/system bars/flags on exit/pause, stop frame
  work when hidden, cap Desk at 12 fps, preserve shifting with reduced motion.
- [ ] Test charging offer opt-in, unplug behavior, lifecycle/window restoration,
  focus exit without cancellation, mode limits and settings round-trip.
- [ ] Gates, emulator checks/captures, checkpoint, commit/CI.

### 5. Verify and ship

- [ ] Fresh World/Focus/Desk/Settings light/dark/AMOLED/1.3×/RTL emulator captures,
  visual inspection and fixes. Native integration, migration and release smoke.
- [ ] Measure emulator frame work/pause behavior; explicitly leave real-device
  battery consumption, thermal/OLED lifetime and OEM behavior unverified.
- [ ] Update README, user guides, gallery, site, changelog and memory. Set version
  name 1.1.0 and increase versionCode. Keep private signing key untouched.
- [ ] Fresh review, four gates, signed production lint/build/checks, main CI,
  signed main preflight, new immutable v1.1.0 tag, GitHub release/public download,
  matching certificate and live website checks per `docs/RELEASING.md`.

## Review focus

Nonfinite/huge gestures must not lose the camera; old/imported plants must remain
accessible; hidden/paused screens must stop rendering and release window overrides;
no completion reward may occur from a visual preview; restoring older backups must
default Desk off and preserve all existing preferences and data.

## Verification ledger

Extraction complete: all four gates PASS, 369 JVM tests; five legacy before/after
app frames identical below the Android status bar. Scene and snapshot inspected.
Local evidence: .tools/p35-{before,after}/ and p35-extract-{gates,captures}.log.
World/Focus/Desk integration and v1.1.0 are next.
