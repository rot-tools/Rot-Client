# Existing module refresh — 2026-10-08

Rot Client 26.3 `2.0.2` updates existing features using the current source and
release comparisons below. This is a focused compatibility update. It does
not embed ten complete clients or establish feature-for-feature parity with
them. Shared observation/UI repairs ship in Lite and Plus; automatic dungeon
and dagger actions remain physically Plus-owned.

## Sources and implemented scope

| Requested project | Current source reviewed | Actual license | Result in this update |
| --- | --- | --- | --- |
| Athen | `skies-starred/Athen`, master `21ada452` | BSD-3-Clause | Local Slayer announcement compatibility; existing T5 double-count guard retained. |
| Nebulune | `skies-starred/Nebulune`, master `31918330`, archived | BSD-3-Clause | Single-use dagger completion, stale-context cancellation, Soulcry guards. |
| Odin | `odtheking/Odin`, main `833e0533` | BSD-3-Clause | Exact color prefixes, numeric state/order, Starts With intrinsic glint comparisons. |
| OdinClient | `skies-starred/OdinClient`, master `77dba985` | BSD-3-Clause | Queue/click contract comparison; current AutoTerms/QueueTerms are stubs, not imported implementations. |
| NoamAddons / NoammAddons | `Noamm9/NoammAddons`, 26.1.2 `6d0c95a5` | CC0-1.0 | Container/glint/click comparisons; no upstream session/authentication code imported. |
| BladeAddons | `BladeMasterGabe/blade-addons`, new `fda86776` | CC0-1.0 | Melody progress comparison; stale upstream four-row notification replaced with observed-row percentages. |
| SkyHanni | `hannibal002/SkyHanni`, beta `a7528349` | LGPL-2.1 | Observable Pet formats/source priority; original Rot parser repairs. |
| SkyCofl | `Coflnet/SkyblockModFabric`, main `f097655c` | AGPL-3.0 | Backend/debounce/HUD comparison; no silent backend integration or copied AGPL implementation. |
| Skyblocker | `SkyblockerMod/Skyblocker`, main `f5cc8799` | LGPL-3.0 | Widget boundaries/icon lifecycle comparison; original Rot fixes. |
| Devonian | `Synnerz/devonian`, 26.3 `0afe347a` | GPL-3.0 | Decorated pet identity/26.3 observations; original Rot fixes. |

Full hashes, commit/release dates, pinned source paths, adaptations and limits
are in [Slayer audit](UPSTREAM_SLAYER_AUDIT.md),
[dungeon audit](UPSTREAM_DUNGEON_AUDIT.md) and
[shared HUD audit](UPSTREAM_SHARED_AUDIT.md). Latest development branches are
identified separately from their latest published stable releases.

Derived permitted snippets retain inline attribution and full upstream notices
under `docs/third-party/` and inside both JARs at `META-INF/licenses/`.
Attribution alone is not a substitute for the actual license conditions.
No copyleft implementation or third-party assets were copied in this update.

## Behavior repaired

- Pet Display distinguishes missing widget data from explicit no-pet state,
  keeps cosmetic identity stable and discards an old pet's icon/valuation on a
  confirmed switch. Server XP percentage takes precedence over rounded totals.
- Commission Display accepts split mineral/Puncher tasks, recognizes exact
  completion rows and rejects numeric overflow.
- Plus terminals distinguish related colors and use unfinished numeric panes
  in order. Queues belong to one menu instance and actual container slots;
  Clone no longer turns a required Rubix right-click into a middle-click.
- Current simulator defaults are Melody three rows and Numbers ten entries.
  Observed legacy four/fourteen layouts still work. Progress messages use the
  observed row count; teammate HUD shows the received percentage.
- Plus dagger actions complete after one use and cancel when their target,
  held-item mode or UI context is no longer valid. Shared local Slayer ownership
  requires exact server announcement and unambiguous fresh entity evidence.
- API failure polling backs off and retains the last valid quote. Retry-After
  accepts HTTP dates and cannot overflow seconds into an immediate retry.

Canonical mining quantities, Current Session and History storage are preserved.
The separate [Hypixel audit](HYPIXEL_COMPATIBILITY_2026-10-08.md) distinguishes
confirmed patches and live API probes from unverified timings.

## Manual validation and remaining work

Test Lite and Plus separately with backed-up profiles. Recheck existing 26.3
color-picker/inventory/storage input, pet switch/unequip/XP, split commissions,
island changes and reconnect. In Plus test Numbers ordering, Blue versus Light
Blue, Green versus Lime, Melody three-row progress, legacy layouts, Rubix right
click/Clone, same-title reopened menus and screen-close cancellation. For Slayer
test both owner holograms and new server spawn lines, ambiguous nearby bodies,
delayed NBT, target death and opening a screen while a dagger action is pending.

Open engineering work includes locked Rubix goal colors, per-slot server
acknowledgement of intrinsic-glint items, faster Watcher/F7/M7 phase timings,
Tarantula phase-two/cocoon ownership and Rift body identity. Same-name pets
still need menu evidence to distinguish UUID/rarity. SkyCofl backend panels,
new weather/Fiesta/Greenhouse mechanics and wider upstream modules are separate
unimplemented slices. Existing exact Pickobulus/accounting gaps remain open.

Builds and regression tests do not establish Minecraft runtime completion or
Modrinth/CurseForge approval. This update adds no public-release approval claim.

## Automated checkpoint

Minecraft 26.3: Gradle `build` passes, including 2,456 shared and 227 Plus tests (zero failures/errors/skips), both client compilers, `verifyLegitJar` and `verifyDungeonJarBoundary`. Both playable archives contain all six retained source-license files. Minecraft runtime validation remains pending.

26.2 port: `build` passes with 2,456 shared and 227 Plus tests (zero failures/errors/skips), preserving 26.2 mouse IDs and the EnderMan class binding.
