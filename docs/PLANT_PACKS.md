# Plant packs

ScreenGarden loads its artwork offline from `app/src/main/assets/plants/<id>/`.
Each pack has one `plant.json` and four transparent PNGs. The bundled fern,
flower, bonsai and cactus use original ScreenGarden artwork under CC BY-SA 4.0.
No art account, SDK or network connection is needed.

## Add a pack

1. Choose a unique lowercase identifier, starting with a letter, using at most
   40 letters, digits, underscores or hyphens. Create a folder with that name.
2. Draw four transparent PNGs for seed, sprout, bud and bloom. Put the soil line
   near the bottom so stages stay anchored. Keep the plant readable against both
   light and night gardens. Each image must be 1–512 pixels in each dimension
   and no larger than 1 MiB. Bundled images are 256 × 256.
3. Add metadata based on this complete example. The `id` must match the folder.
4. Supply accurate creator and license information. Supported artwork licenses
   are `CC0-1.0`, `CC-BY-4.0` and `CC-BY-SA-4.0`. Include any required attribution
   in `ART_LICENSE.md`. Restricted, noncommercial, no-derivatives and missing
   licenses are rejected. Do not copy art without permission.
5. Run the validator below, then the full repository checks. To add a bundled
   species, update the expected identifiers in the validation test and add its
   localized label in `GardenPlantText.kt` and the string resources.
6. Review the garden and snapshot preview on the emulator using fake debug data,
   including dark mode, reduced motion, large text and RTL. Submit the images,
   metadata, license notice, tests and updated memory in the same change.

```json
{
  "schemaVersion": 1,
  "id": "fern",
  "name": "Fern",
  "description": "A quiet fern with paired leaves.",
  "unlock": { "type": "default" },
  "stageImages": {
    "SEED": "seed.png",
    "SPROUT": "sprout.png",
    "BUD": "bud.png",
    "BLOOM": "bloom.png"
  },
  "license": "CC-BY-SA-4.0",
  "author": "ScreenGarden contributors"
}
```

## Unlock rules

| Rule | Complete `unlock` object | When eligible for a new seed |
| --- | --- | --- |
| Default | `{"type":"default"}` | Always |
| Streak | `{"type":"streak","days":7}` | At least 7 counted days in that day's streak |
| Focus | `{"type":"focus","completions":5}` | At least 5 persisted completed sessions by that day |
| Season | `{"type":"season","season":"SUMMER"}` | That completed local day's season, using the saved hemisphere |

The bundled bonsai unlocks after 3 counted days so it can appear before the five-plant cap.
Streak thresholds accept 1–365; focus thresholds accept 1–10,000. Seasons are
`SPRING`, `SUMMER`, `AUTUMN`, `WINTER`. The loader rejects unknown rules, extra
fields, wrong JSON types and out-of-range values.

On a good completed day, the existing garden engine still plants at most one
seed, up to five plants. Eligible packs are ordered with defaults first, then
by identifier, and rotated using the current plant count. Focus completion in
an empty garden still creates a fern. Unlocks never remove plants, alter health,
re-lock an existing plant or replace old history. Seasonal plants keep their
art after the season ends. Missing packs fall back to the original vector fern.

## Validation and limits

From the repository root in PowerShell:

```powershell
./gradlew.bat :app:testDebugUnitTest --tests '*PlantPackLoaderTest'
```

The test decodes every bundled stage and checks metadata, license, malformed
JSON, unsafe paths, unavailable images and unlock boundaries. The regular
test suite also runs it in CI. No emulator is needed for this validator.

Schema v1 has exactly the fields above; metadata is limited to 16 KiB, names to
80 characters, descriptions to 500 and authors to 120. Text must be nonempty
and contain no control characters. Stage filenames are simple lowercase PNG
basenames, up to 64 characters before the extension. Paths, URLs and archives
are unsupported. At most 16 packs load. Images decode once off the UI thread;
there is no decoding or disk read per animation frame. Invalid packs are skipped
with a fixed diagnostic reason, while other packs and vector fallback remain usable.

The four original bundled packs adapt their leaf colors to the selected theme
and accent, with seeds using the theme's soil ink so they stay visible at night.
Other pack identifiers keep the artist's supplied palette. Wilt desaturation,
sway, growth scaling and bloom crossfades use the shared motion policy.

P25 supports built-in packs. Optional user imports through the system file picker
are deferred; there is no storage permission or import surface to mislead users.
