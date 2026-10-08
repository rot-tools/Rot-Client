# 26.3 dashboard, session stats and dungeon map

Candidate: **2.0.6+mc26.3**, for Lite and Plus. This is a bounded UI/map correction, not completion of the full dungeon module audit. Minecraft validation remains pending.

## Dashboard

Display, HUD, rendering and interface tools share **Display & Interface** in the sidebar. Original module IDs, configuration fields and catalog group IDs are preserved; saved links to old display categories resolve to the combined page. Catalog remains 83 Lite / 135 Plus parents. No Plus behavior is moved into Lite.

Each module/HUD settings drawer has a separate **Find settings** field. It searches labels, descriptions, IDs, aliases, options and section headings, preserving relevant section context. Matching sections open during search; clearing restores existing section collapse choices. Clear with × or Esc; a second Esc leaves search focus. Search scroll starts at the top, and active editor/slider captures are released. Global setting hits open the module with a matching local filter. Inline drawers have a bounded height and scroll internally.

## Read Current Session

1. Open **Mining > Current Session**, then **Open Current Session**.
2. **Active Time** excludes pauses. **Est. Item Value** is the priced item estimate, not coins received or realized profit. **Value / Active Hour** divides that estimate by active time. Price basis is shown in the help card; unpriced and currency rows do not silently become priced item value.
3. **Selected Tracker** describes the current mining selection. Change future target routing in **Mining > Tracker**. Prior target loot stays in the same persisted session; selection never rewrites past observations.
4. Choose **All / Target / Other mining / Mobs / Chests / Currency** to filter item lists. Summary metrics remain whole-session totals. Source lists stay separate, use full width and show every row through scrolling rather than truncating after eight.
5. **Pause / Resume** control collection in the same session. **Start New** requires confirmation and uses the existing archive-first lifecycle. **Copy** exports the current summary.
6. **Saved Sessions > Open Saved Sessions** displays archived snapshots. **OPEN** reads a record; **COPY** exports it. DELETE/CLEAR are existing destructive history controls, not list filters. The saved-session page retains its existing visible-row limit; pagination is still a follow-up.

This change does not mutate accounting, deduplication, Current Session quantities, History schemas or price calculations. Mining M1 remains paused. Session UI and source-filter behavior need a controlled restart/pause/target-switch check in both editions.

## Dungeon map, Plus only

Signed decoration coordinates are centred half-pixels: pixel = floor((packet coordinate + 128) / 2). Non-player decoration types are ignored. Dead teammates are removed from the observed roster, named foreign/dead markers are rejected and incomplete anonymous-marker packets do not get guessed identities. Calibration failure clears the stale board and preview. Map buffers must be complete, and entrance colour runs cannot wrap between rows.

Behavior references, independently integrated:

- [Odin map sources](https://github.com/odtheking/Odin/tree/833e0533ef9c47529b790612627a65618ebd5a58/src/main/kotlin/com/odtheking/odin/features/impl/dungeon/map), BSD-3-Clause.
- [Skyblocker dungeon sources](https://github.com/SkyblockerMod/Skyblocker/tree/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/dungeon), LGPL-3.0; behavioral comparison only.
- [Hypixel 0.27.2 Minister notes](https://hypixel.net/threads/hypixel-skyblock-0-27-2-the-minister-update-greenhouse-qol-and-more.6159904/): faster conditional boss transitions and dialogue, Melody three rows, Numbers ten, changed key pickup and secret drop timing. Existing terminal row limits remain present. No replacement boss delays were invented from the patch description.

No upstream implementation was copied in this slice. Older BSD/CC0 adaptation notices remain included. Shared pure dungeon policies still follow the existing JAR exclusion rules; live dungeon logic and the new observation policy are Plus-only.

## Remaining dungeon work

- Controlled Entrance/F1/F3/F7/M7 map checks: room sizes, doors, party ordering, dead/revived markers, names/secrets and map replacement.
- Current-server evidence for Maxor/Storm/crystal/relic timing. Existing hardcoded estimates are not verified against conditional Minister speed-ups.
- Full module-by-module puzzle/terminal/F7/ESP/secret/requeue lifecycle audit and runtime matrix.
- Release audit/provenance and broader Lite runtime checks remain required before public Modrinth/CurseForge publication.
