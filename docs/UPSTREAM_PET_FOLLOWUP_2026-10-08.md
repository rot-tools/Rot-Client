# Pet GUI observation follow-up — Minecraft 26.3

Scope: original shared Pet HUD/inventory-cache fixes for both Rot Client Lite
and Rot Client+. No gameplay actions, remote services or XP mechanics were
added. Older Minecraft versions are outside the current owner-authorized scope.

## Rechecked upstream pins

Fetched the primary repositories again on 2026-10-08:

| Repository / branch | Latest fetched commit | Focused comparison result |
| --- | --- | --- |
| [SkyHanni / beta](https://github.com/hannibal002/SkyHanni) | `a7528349a2d759581471ac530351d7848b23784b`, 2026-10-07 | Unchanged from the first audit. Current pet source distinguishes exact menu UUID observations, page-global selected-pet summaries, stale tab authority and name-only ambiguity. |
| [Skyblocker / main](https://github.com/SkyblockerMod/Skyblocker) | `f5cc8799c6d9c0c0a75bcba525bc1487245c31b0`, 2026-10-06 | Unchanged from the first audit. Widget parsing remains a useful local-data boundary. |
| [Devonian / 26.3](https://github.com/Synnerz/devonian) | `e5f4f515e6bc54ccaf7edf5213a99cf2f656de71`, 2026-10-08 | Remote branch changed since `0afe347...`. PetDisplay is unchanged; the focused delta is in service-key, InstaClear and terminal-solver sources. |

Licenses remain SkyHanni LGPL 2.1, Skyblocker LGPL 3.0 and Devonian GPL 3.0.
No upstream implementation was transplanted. This follow-up independently
implements observations over Rot's existing cache and policies; it is not a
claim that a new Hypixel API update broke Pet Display.

Primary behavior/data references:

- [SkyHanni menu observation and slot 4 selected-pet summary](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/api/pet/PetStorageApi.kt)
- [SkyHanni selected-pet text, progress and menu-title fixtures](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/api/pet/PetStoragePatterns.kt)
- [Devonian current PetDisplay](https://github.com/Synnerz/devonian/blob/e5f4f515e6bc54ccaf7edf5213a99cf2f656de71/src/main/kotlin/com/github/synnerz/devonian/features/misc/PetDisplay.kt)

## Concrete problem and resulting behavior

Previously, opening a populated Pets-menu page without an equipped item cleared
the cached pet. The equipped pet may be on another page or excluded by a search.
That absence provides no evidence of despawning it.

The GUI now accepts positive equipped-pet evidence from the known pet slots,
or the page-global `Selected pet:` summary in slot 4. An absent/unpopulated
summary preserves the cache; only explicit `Selected pet: None` clears it. A
summary's observed next-level percentage can update HUD progress while the
selected pet's item is on another page. `MAX LEVEL` is displayed without
inventing whether that pet's maximum is 100, 200 or something else.

Exact GUI `petInfo.uuid` confirms the individual selected after a click. Two
same-name pets with different UUIDs cannot prematurely confirm each other's
selection. The existing pending interval remains the fallback when no UUID is
available. A differing known UUID or rarity in the global summary invalidates
the old pet's cached icon/market metadata and uses a generic unpriced head
until the selected item's full GUI data is observed.

Pet UUID metadata already lives inside the serialized ItemStack cache; no
second pet registry or persistence schema was introduced. Strict UUID parsing
accepts dashed and compact UUIDs, and ignores malformed/non-string values.
Known `PET` item IDs or typed pet metadata are required alongside the level
title; an arbitrary named head cannot masquerade as the active pet. Equipped
lore uses a complete status instruction, supports harmless bullets/skin
symbols and Unicode padding, and rejects negated/instructional text.

Pets-menu titles now match the complete title and support multi-digit page
counts, prefix/suffix page labels and quoted search labels. Invalid page ranges,
overflow and unrelated titles such as `Petsitter` are ignored.

## Tests and remaining limits

Eight focused regressions were added: six GUI-summary/UUID/status cases, an
item-identity guard case and precise title/search/page parsing. They cover
filtered-page absence, explicit None, MAX without a guessed maximum,
same-name rarity changes, exact UUID distinction, rounded XP not becoming
total XP, malformed metadata and decorated equipped instructions.

The coordinating task runs the final project build and both edition checks.
This document alone does not establish a green build or a Minecraft playtest.

Remaining limits:

- Tab text has no pet UUID. Two pets sharing name and rarity remain ambiguous
  without a confirming menu observation; no UUID is guessed from their name.
- The implementation reads observed XP/progress. It does not reconstruct pet
  XP curves, skill/action gains, Exp Share or every pet's maximum level.
- The global menu summary can provide identity/progress without the actual pet
  texture. Generic unpriced rendering is intentional until exact item data is
  available; no old pet price is attributed to a different individual.
- The existing optimistic left-click selection still requires server/menu
  confirmation. Rejected clicks and unusual modifier actions need runtime
  verification rather than being treated as guaranteed selections.

Runtime-test another page/search while a pet remains equipped, two same-name
different-UUID pets, a failed selection, a despawn, summary rarity changes and
profile/restart persistence in both 26.3 editions. These are Ready for Runtime
Test, not runtime Complete.
