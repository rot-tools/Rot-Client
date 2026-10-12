# Dashboard, settings and purchase checkpoint — 2026-10-12

`2.0.10+mc26.3`, targeting `port/26.3`. Dashboard, Viewmodel and price-tooltip
fixes ship in both editions. Market purchases and their journal/UI/HUD remain
Plus-only. Older Minecraft branches and installations are preserved.

## Shared dashboard and settings

The settings drawer leaked one scissor-stack entry into later render passes.
Clips now balance, HUD modals and search suggestions get separate render
strata, and drawing/clicking/dragging use the same body geometry. Long inline
drawers scroll their own body, with a working scrollbar and pinned group
Expand all / Collapse all controls. Per-module search includes section aliases,
IDs and normalized words; closed groups cannot hide matching controls.

Header search owns character/key input while focused. Clicking a page control
transfers focus back to it. Ctrl+L, Ctrl+A, paste, Unicode backspace and result
navigation work through the same bounded query. Search results/page layout are
cached, and the search index refreshes after Plus installs its catalog provider.
HUD scale updates live but saves on release/close, rather than each drag event.

Viewmodel's missing 26.3 equip-progress and swing-translation hooks are restored
at the actual `FirstPersonHandsAndItemsRenderer` methods. Offset, rotation,
scale, empty-hand scope, swing and local effect settings retain one config.
Catalog persistence tests exercise toggles, numeric bounds, colors and enum
options in both editions, including the separate tracker-owned HUD flags.
Lighting/text/keybind/action controls retain their separate contracts.
Persistence coverage does **not** establish that every feature works in-game.
The retired Miniboss Alert distance slider is removed from the visible catalog;
its stored value remains readable for older profiles. Alerts use own-spawn chat.

## Plus purchases

The setup has four pages: **Targets, Budget, Safety, Purchases**. Watches and
pins must be explicitly selected. Filters retain exact item/rarity/reforge
identity; Auction House transactions accept BIN only. Budget is the smallest
of remaining total spending, per-purchase cap, protected Purse and its allowed
percentage. Zero budgets cannot buy. Save does not start; Start saves/arms;
Stop/Esc/world/profile/connection loss cancel pending activity.

The four-second countdown revalidates fresh API/menu/identity/price evidence.
A purchase completes only after a durable server receipt and exact newly owned
item, or a saved Bazaar buy-order receipt, full owned fill and exact claim
delta. Contradictory/rounded menu evidence fails closed. Confirmation produces
an eight-second **Buy done** card; the Safety HUD switch hides both progress
and receipts. Bought items remain for **manual sale**. No automatic relisting,
sell-offer stage or invented profit is produced. Fresh snapshots continue to
feed further selected opportunities within spending/holding limits.

The schema-v2 journal reads v1 without discarding records. Unresolved legacy
purchase/listing outcomes require manual review. Reviewing releases a holding
slot but preserves spent budget and transaction deduplication. No new purchase
starts automatically after restart. Writes flush before transaction actions.

Auction indexing and proposals are bounded across ticks, with cached exact
lookups and comparable groups during the countdown. This removes repeated full
market work from each frame; actual FPS/frame-time improvement remains unmeasured.

Java 25 `clean build`, **2,533 shared + 364 Plus tests** (zero failures,
errors or skips), `verifyLegitJar`, `verifyDungeonJarBoundary` and diff checks
pass for this checkpoint. Both playable 26.3 artifacts compile.

## Current upstream and API review

The official [patch-notes index](https://hypixel.net/forums/skyblock-patch-notes.158/)
still lists 0.27.2 as the latest main update. Previous dungeon changes already
cover its relevant Melody/Numbers corrections; conditional boss timings remain
a runtime gate. Five public v2 resources (items, collections, skills, Bazaar,
auction page 0) returned HTTP 200/success with expected keys during this audit.
[PublicAPI](https://github.com/HypixelDev/PublicAPI) remains at
`f928da471c2727caf3ea1c393631046b9570266f`;
[ModAPI](https://github.com/HypixelDev/ModAPI) at
`5245ef1f161883ae9152d719de29885bb09e3f57`. These checks establish reachable
contracts for those paths, not every private/profile endpoint or server menu.
The retired official wiki no longer provides live AH/Bazaar layouts; unrecognized
layouts cannot authorize a transaction.

Fetched and compared source changes:

| Project / revision | Relevance and disposition |
| --- | --- |
| [SkyHanni](https://github.com/hannibal002/SkyHanni/tree/9ce4cab6ac525dfd75307516d639a2fdd167333a) | Special Phoenix quote identity / Bait Sack icon identity reviewed; separate quote identity adapter avoids changing raw tracker/item identity. |
| [Athen](https://github.com/skies-starred/Athen/tree/77b2f82826201ae24e9a19bf0612ae71b77b7dcd) | Broader guessed Slayer ownership; Rot keeps exact/local evidence and ambiguity rejection. |
| [OdinClient](https://github.com/skies-starred/OdinClient/tree/4df18c564c097ae630ef0eb2aa1f42837e62fdcf) | Modern block-outline extraction hook; Rot already hooks block shape/clip context. |
| [Skyblocker](https://github.com/SkyblockerMod/Skyblocker/tree/a3f25d798b5ea8d8a91a690232bbd4274b4a7a3a) | Repeated shortcut dedupe, custom model compatibility, Bazaar maximum; reviewed against existing input paths. |
| [BladeAddons](https://github.com/BladeMasterGabe/blade-addons/tree/0c77f0f2b685f47d6292ac4bfc57fca91ef154f3) | Melody /3 and highlight flag correction; Rot already derives observed Melody rows. |
| [SkyCofl](https://github.com/Coflnet/SkyblockModFabric/tree/e9327699dda33d0b6e7e5a9e8409a6c75e09ed46) | Enum moved outside mixin package; no equivalent Rot class-load defect found. |

Odin, NoammAddons, Devonian and archived Nebulune heads did not change from the
preceding audit. Comparison is not a claim of complete upstream parity. Quote
repairs use an original adapter and verified identifier conventions; LGPL/AGPL
implementations are not copied into the MIT source. Existing packaged notices remain.

## Required Minecraft checks

1. Test Lite and Plus separately: header typing/paste, switching search focus,
   opening/closing modules, all groups, narrow GUI scales, long drawer last rows,
   enum/color/text/keybind controls, drag/release and restart persistence.
2. With Viewmodel on, change each axis, equip animation and swing; test both
   hands/empty hand and module-off neutral behavior.
3. Compare frame times in the same world/settings with the dashboard closed,
   open and scrolling. Automated work limits are not measured FPS evidence.
4. On the authorized test server, use a small Plus budget for an exact BIN buy
   and a full Bazaar buy/claim. Check the receipt/HUD, no sale, continued fresh
   scans, spending limits, cancellation, unknown menu, restart and manual review.
5. Wider all-module/all-floor/two-player gameplay, Party Finder's obsolete
   SkyCrypt backend/schema and the public Lite release/provenance audit remain open.
