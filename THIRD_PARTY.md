# Third-party notices

Rot Client first-party code, catalog copy, and Rot-authored assets are MIT.
See [LICENSE](LICENSE). This file lists **bundled or generated** works that are
not Rot Tools original copyright, plus how SkyBlock facts are sourced.

Rot Client does **not** vendor other SkyBlock client source trees into this
repository or the playable JAR. Quality-of-life modules are Rot-owned
implementations (`fi.rotclient`). Feature overlap with community clients is
behavior-level only.

## Magical Map layout (CC0)

Dungeon HUD map scan, 128×128 paper layout, floor start corners, and default
room/door colors are adapted from NoammAddons Magical Map code, which is
CC0 1.0 Universal. The Rot implementation stays in `DungeonMapPolicy` and
the dungeon HUD overlay. Public catalog copy does not use that project name.

## Source Sans 3 (Adobe)

| Item | Value |
| --- | --- |
| Work | Source Sans 3 Regular (`ui.ttf`) |
| Copyright | Copyright 2010-2024 Adobe, Reserved Font Name 'Source' |
| License | SIL Open Font License 1.1 |
| Upstream | https://github.com/adobe-fonts/source-sans |
| In this repo | `src/client/resources/assets/rotclient/font/ui.ttf` |
| Full license text | `src/client/resources/assets/rotclient/license/OFL.txt` |
| Pack copy | `src/client/resources/resourcepacks/gameplay_font/OFL.txt` |

The font may be bundled with Rot Client. It must not be sold by itself. Do not
use the reserved name 'Source' for a modified font without Adobe's permission.

## Catacombs puzzle boards (BSD-3-Clause)

Ice Fill floors, Boulder clicks, and Water Board lever timings under
`src/main/resources/rotclient/dungeons/` (`iceFillFloors.json`,
`boulderSolutions.json`, `waterSolutions.json`) are adapted from the Odin
client puzzle solvers, licensed BSD-3-Clause. Public catalog copy does not
use that project name.

## Hypixel public item and Bazaar facts

Official item IDs and Bazaar product identity come from Hypixel's public
resources (`/v2/resources/skyblock/items`, `/v2/skyblock/bazaar`). Rot
normalizes those facts into Rot-owned JSON (`canonical-items.v1.json`,
`bazaar-product-index.v1.json`, `provenance.v1.json`). Handwritten Rot domain
rules stay in `domain-rules.v1.json`. See
[docs/reports/skyblock-data-license-provenance.md](docs/reports/skyblock-data-license-provenance.md).

Hypixel, SkyBlock, and related names are trademarks of their owners. Rot Client
is not affiliated with Hypixel.

## Legacy item model map

`src/main/resources/assets/rotclient/data/legacy-item-models.json` is a Rot
Client lookup table: SkyBlock item id → vanilla `minecraft:` model path. It is
used to restore pre-custom-model item looks while the Hypixel 26.2 server pack
stays loaded. It is not a third-party source tree. Generation notes live on
`LegacyTexturesPolicy`.

## Built-in dark world packs

`dark_overworld`, `dark_crimson`, and `dark_end` overlay Minecraft texture
paths. Rot Tools claims the darkened block/item/entity art shipped in those
folders as Rot-authored conversions for Minecraft 26.2.

Any file that is an unmodified Minecraft / Mojang atlas or GUI fragment (for
example vanilla `unicode_page_*.png` font sheets under `assets/minecraft/`)
remains under the Minecraft EULA. Rot Client does not relicense those pages.

See each pack's `ATTRIBUTION.txt`.

## Gameplay font pack

The optional gameplay font pack points Minecraft's default font at Rot's
bundled Source Sans 3 file. Adobe's OFL still applies; see the OFL files in
that pack.

## Minecraft and Fabric

Minecraft is Mojang / Microsoft. Fabric Loader and Fabric API are separate
mods the user installs; they are not nested inside the Rot Client JAR.
Mod Menu is compile-only and is not required at runtime.
