# Art license

**ScreenGarden's original artwork is licensed under
[CC BY-SA 4.0](https://creativecommons.org/licenses/by-sa/4.0/).**

That covers the launcher icon, the in-app illustration and the visual assets in
`app/src/main/res/drawable*/` and `app/src/main/res/mipmap*/`, plus any plant pack
shipped by the project itself under `assets/plants/`.

The **code** is licensed differently: `GPL-3.0-or-later`, see [LICENSE](LICENSE).

## Why a separate license for art

A code license is a poor fit for images. CC BY-SA 4.0 is an established,
human-readable content license that keeps artwork open the same way the GPL keeps
code open: you may reuse and remix it, including commercially, as long as you
credit the author and license your derivative under the same terms.

## Plant packs and third-party art

Each plant pack carries its own `license` field in its `plant.json`, so a
contributor may bring art under any license that permits redistribution with the
app. The bundled packs default to CC BY-SA 4.0. A pack that declares no license,
or an unreadable one, is rejected by the loader rather than silently assumed.

## Attribution

Attribution lives in the app (About section, planned with the release work) and
in the per-pack `author` field. If you redistribute the artwork standalone, keep
the author name and a link to this file.

## Note on the full text

Creative Commons recommends linking to the license rather than bundling a
localized copy, so this repository does not reproduce the CC BY-SA 4.0 legal
code. The canonical text is at
<https://creativecommons.org/licenses/by-sa/4.0/legalcode>.
