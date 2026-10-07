# Shared HUD upstream audit — 2026-10-08

This records the SkyHanni, SkyCofl, Skyblocker and Devonian portion of the
upstream comparison. It is a bounded first implementation slice, not a claim
that Rot includes every upstream feature or that every upstream module has
been runtime-tested. Branch snapshots and stable releases are distinct.

## Verified source snapshots

| Project | Primary repository / inspected branch | Inspected commit and commit date | Repository license |
| --- | --- | --- | --- |
| SkyHanni | [hannibal002/SkyHanni](https://github.com/hannibal002/SkyHanni), default `beta` | `a7528349a2d759581471ac530351d7848b23784b`, 2026-10-07 | [LGPL 2.1](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/LICENSE) |
| SkyCofl Fabric | [Coflnet/SkyblockModFabric](https://github.com/Coflnet/SkyblockModFabric), default `main` | `f097655cd8e5244e5eab18f8e790669b459763c4`, 2026-10-04 | [AGPL 3.0](https://github.com/Coflnet/SkyblockModFabric/blob/f097655cd8e5244e5eab18f8e790669b459763c4/LICENSE) |
| Skyblocker | [SkyblockerMod/Skyblocker](https://github.com/SkyblockerMod/Skyblocker), default `main` | `f5cc8799c6d9c0c0a75bcba525bc1487245c31b0`, 2026-10-06 | [LGPL 3.0](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/LICENSE) |
| Devonian | [Synnerz/devonian](https://github.com/Synnerz/devonian), default `26.3` | `0afe347ab433fc00d77305eecdb66f7194b82027`, 2026-10-07 | [GPL 3.0](https://github.com/Synnerz/devonian/blob/0afe347ab433fc00d77305eecdb66f7194b82027/LICENSE) |

These four repositories are not MIT licensed. No implementation from them was
copied into Rot's MIT sources in this slice. References below document observed
server text formats and feature behavior; the fixes use independently written
Rot policies and Rot's existing rendering/configuration boundaries. A future
source transplant needs a separate license-compatible integration plan, rather
than an attribution comment alone.

## Release versus development status

| Project | Latest stable release reported by GitHub on 2026-10-08 | Release commit | Development compatibility |
| --- | --- | --- | --- |
| SkyHanni | [9.0.0](https://github.com/hannibal002/SkyHanni/releases/tag/9.0.0), published 2026-09-16 | `eedd95a9517e24e3e5faf6853777ca70310986f0` | Snapshot build targets 26.1 and 26.2; no automatic 26.3 compatibility inference. |
| SkyCofl Fabric | [1.9.3](https://github.com/Coflnet/SkyblockModFabric/releases/tag/1.9.3), published 2026-06-20 | `8ab5785911061d1c747f3dd098409d1360d09367` | `main` declares 2.0.0 / MC 26.3; [2.0.0-pre1](https://github.com/Coflnet/SkyblockModFabric/releases/tag/2.0.0-pre1) is a prerelease from 2026-09-18. |
| Skyblocker | [v6.10.4+26.2](https://github.com/SkyblockerMod/Skyblocker/releases/tag/v6.10.4%2B26.2), published 2026-09-15 | `e364eb8f7cfe0d7b73c552383cc7095ec2ba55fb` | Inspected `main` still targets MC 26.2. |
| Devonian | [v1.28.9-26.2](https://github.com/Synnerz/devonian/releases/tag/v1.28.9-26.2), published 2026-07-26 | `9c491fb4314120175cd8b05c311ff861a3f88b19` | Default branch is the newer 26.3 source port, not this stable release. |

SkyCofl's original [Forge frontend](https://github.com/Coflnet/SkyblockMod) is
separate: default `main`, AGPL 3.0, stable 1.8.4 from 2026-01-16. The Fabric
repository is the relevant source for this 26.3 comparison. Do not substitute
unrelated repositories merely because their name contains “SkyCofl”.

## What actually changed upstream

The release-to-snapshot comparisons were performed on the focused source paths.
They do not establish a Hypixel API break by themselves.

- SkyHanni's pet API/pattern implementation is unchanged between 9.0.0 and the
  inspected beta snapshot. The focused mining delta adds a Dwarven Mines
  visibility choice to its Fallen Star Cult timer; the Mineshaft Pity delta is
  an internal variable rename. Those are not evidence that commission or Pet
  XP server formats changed after Rot's October 1 release.
- Skyblocker's Pet widget and Dwarven commission sources are unchanged between
  v6.10.4+26.2 and the inspected main snapshot. Its current widget separation and
  icon caching are still useful comparison points. Other post-release work
  includes Forge/tab HUD and Foraging/UI changes outside this implementation
  slice.
- Devonian's Pet Display delta makes absent Pets-menu page numbers default to
  page 1, updates the 26.3 container-accessor calls, and includes import/null
  cleanup. Its existing observable pet formats include cosmetic level badges
  and a skin marker. A development branch is not proof of completed playtests.
- SkyCofl Fabric's post-1.9.3 source adds bounded background/debounce/request
  state, configurable backend-fed HUD panels, HUD layout/expiry handling and
  trade/sell protection work. These involve its service-backed architecture.
  They are not drop-in local replacements for Rot's public-market data path.

## Original fixes implemented in Rot

These changes are in shared `src/main` / `src/client` and therefore apply to
both Lite and Plus. They add no automatic game actions and no external service.

### Pet Display and inventory pet cache

- Pet identity excludes the known numeric cosmetic badge and trailing skin
  marker. A changed cosmetic level no longer resets the same Golden Dragon's
  cached held item/progress. Older decorated cache names compare correctly.
- Unknown/new pet names remain supported without a fixed registry. Tests cover
  Eagle and T-Rex. Existing SPECIAL/VERY_SPECIAL rarity handling is retained and
  explicitly tested for the Phoenix change; no XP curve is invented.
- A server-reported XP percentage wins over a fraction calculated from rounded
  abbreviations such as `1.4M`. Without an observed percentage or fraction, an
  XP total alone does not invent progress toward the next level.
- A new or unknown tab widget ends the Pet detail section. Its own XP rows
  cannot fill the Pet progress bar. A missing/unpopulated Pet widget preserves
  existing observations; explicit `None` / `No pet selected` clears the HUD
  after the existing GUI authority interval.
- A confirmed different pet invalidates the old GUI ItemStack, texture and
  price metadata. A generic unpriced head is used until the new pet's GUI data
  is observed. A recent menu selection still wins during the existing hold.
- Malformed levels and non-finite progress cannot overflow or crash parsing.

The cache currently identifies tab pets by canonical name, not UUID. Two pets
with the same name but different rarity/held item still require authoritative
menu observations. Reading no new server XP is not equivalent to an XP gain.

### Commission Display

- Split progress rows now include mineral/location tasks such as Umber and
  Rampart's Quarry Titanium, plus Star Sentry Puncher. Bullet-prefixed task
  titles are handled without leaking into the next widget.
- Completion lore uses a complete status row instead of substring matching:
  `NOT COMPLETED` or `Completed commissions: 45` cannot mark a task complete.
- Numerically overflowing percentages/fractions are rejected rather than
  showing a false completion. Existing valid percentages, fraction rows,
  duplicate merging and the short empty-refresh hold are preserved.

This is parser hardening for already known display forms, not a claim that
Hypixel recently changed every commission format. No REST endpoint is needed
to read the local tab widget.

## Primary behavior references

- [SkyHanni Pet storage formats](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/api/pet/PetStoragePatterns.kt)
  — cosmetic/skin text, no-selected-pet row, displayed XP fraction/percentage.
- [SkyHanni current-pet authority](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/api/pet/CurrentPetApi.kt)
  — menu/autopet observations can arrive before a stale tab refresh settles.
- [SkyHanni commission mob scope](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/features/mining/HighlightMiningCommissionMobs.kt)
  — incomplete commission and mining-area context matter.
- [Skyblocker Pet widget](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/tabhud/widget/PetWidget.java)
  and [commission labels](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/dwarven/CommissionLabels.java)
  — isolated widgets, cached icon lookup, manual navigation labels.
- [Devonian Pet Display](https://github.com/Synnerz/devonian/blob/0afe347ab433fc00d77305eecdb66f7194b82027/src/main/kotlin/com/github/synnerz/devonian/features/misc/PetDisplay.kt)
  — menu/slot/tab/chat observations and optional page labels.
- [SkyCofl Fabric architecture and HUD protocol](https://github.com/Coflnet/SkyblockModFabric/blob/f097655cd8e5244e5eab18f8e790669b459763c4/README.md)
  — backend-fed HUDs and CoflSkyCore dependency. No endpoint, credentials,
  inventory upload or websocket integration was added to Rot.

## Remaining comparisons / features

| Area | Current boundary / follow-up |
| --- | --- |
| Pet XP gains | Observed tab/GUI progress only. A full skill/action XP estimator, pet-level curves, Exp Share and UUID-aware source arbitration are separate work. |
| Commission navigation | Existing static Dwarven landmarks remain. Active task destination/emissary labels and verified modern Glacite anchors are not implemented by this slice. |
| SkyMall / HOTM | Existing Rot chat/tab/GUI observations remain; no evidence from these focused diffs requires replacing them. Runtime-test current buff, expiry and screen hint with a real observation. |
| SkyCofl economics | Remote authentication, service quotes, backend HUDs and trade/sell protection are not integrated. Any later integration needs explicit data-flow design and suitable licensing. |
| Devonian non-Pet modules | Dungeon, Foraging/Garden and other module comparisons remain outside this shared HUD slice. |
| SkyHanni / Skyblocker breadth | Hunting, Foraging, Garden, item repository and other complete feature sets are not all audited or imported. |

The owner's test server is expected to use the same game formats, but an assumed
1:1 match is not runtime proof. Validate the actual server rows received by the
client. Minecraft source ports, Hypixel server data and mod REST integrations
are different compatibility boundaries.

## Validation

Focused regressions were added to `PetHudPolicyTest` and
`CommissionDisplayPolicyTest`. Project-wide compilation/tests and edition JAR
verification are run by the coordinating task; do not infer a green build from
this audit document alone.

Runtime checklist: same pet with changing cosmetic badge; new pet switch and
generic icon until GUI observation; explicit no-pet versus a temporarily empty
widget; rounded XP fraction with explicit percentage; split mineral task;
completed commission then replacement; both editions after restart/profile
switch. Automated tests do not make these runtime Complete.
