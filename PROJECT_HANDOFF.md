# Rot Tools Project Handoff

This file is a concise entry point. Maintained engineering details belong in
the linked documents rather than in a growing handoff transcript.

## Current baseline

| Item | Current value |
| --- | --- |
| Branch | `development` (stable releases: `main`) |
| Repository | [rot-tools/Rot-Client](https://github.com/rot-tools/Rot-Client) (public development) |
| Original author identity | OgRudolf (historical attribution) |
| Mod version | `2.0.0+mc26.2` |
| Mod ID | `rotclient` |
| Package | `fi.rotclient` |
| Playable JAR | `RotClient-2.0.0+mc26.2.jar` |
| Minecraft | `26.2` |
| Java | `25` (Gradle toolchain provisions JDK 25) |
| Runtime-verified baseline | Canonical Current Session mining accounting, restart/pause/resume, and Resume/Bazaar crash correction |
| Current automated checkpoint | 124 wired QoL modules; Daily Reward Claim is dashboard-wired (default off); Dark SkyBlock Pack is bundled; Item Tooltips is one parent for missing/info/price/style; Wardrobe, Slayer, dungeon F7 leftovers, Storage Overlay, and Inventory Buttons automated-tested; group-wide Minecraft validation pending |
| New privacy correction | Price Tooltip quote polling is inactive while the module is disabled |
| Prior gemstone checkpoint | `0ee5115` — gemstone HUD and active-time validation |

## Current implementation

- Live material selections: Coal, Iron, Gold, Lapis, Redstone, Emerald,
  Diamond, Quartz, combined Mithril + Titanium, Tungsten, and Umber.
- Ordinary and Pure Ore forms share their material ledger; Tungsten and Umber
  remain separate Dwarven Metal targets.
- Live gemstone selections: Ruby, Amber, Sapphire, Jade, Amethyst, Topaz,
  Jasper, Opal, Onyx, Aquamarine, Citrine, and Peridot.
- Live target ledgers remain authoritative and isolated by tracker family.
- The canonical Current Session target handoff now validates the active
  selection itself and rejects foreign material, gemstone, or unknown-target
  credits before durable mutation.
- `RotClientCurrentSession` is the one durable canonical generic ledger.
  Confidently correlated non-target mining observations enter it once; Mining
  HUD OTHERS and Session Analytics are read-only projections.
- `MiningSessionEngine` remains transient classification, correlation, dedupe,
  target parity, and diagnostic state. It is not a second live ledger.
- Current Session persists in `rotclient-current-session.json` and supports
  Pause, Resume, restart recovery, target/area segments, and Start New. Pause
  closes open activity segments and Resume opens new boundaries.
- Session History 2.0 freezes Current Session before reset, stores immutable
  lifecycle/item/valuation snapshots, remains bounded to 20 newest records,
  and reads legacy schema v1 records. Archive failure preserves Current Session;
  a later Current Session write failure restores the exact prior History
  document, and a bounded marker reconciles recognized process interruptions.
- `MOB` has bounded live Current Session ingest (player-caused melee and
  projectile, Combat/Slayer/Dungeon Sack, action-bar `+N`, rare-drop chat,
  and `+N` coins in the kill window). Displayed Magic Find is session context
  only. Remote-ability rewards remain a future evidence channel.
- Powder Chest Tracker is a read-only Current Session projection of `CHEST`
  loot and `CURRENCY` powder with an independent HUD. Reward totals,
  Pause/Resume/Reset, disable/re-enable, and HUD positioning have controlled
  runtime evidence.
- The Client UI includes a searchable QoL dashboard (`QolUtilityCatalog`, 124
  wired modules across ten groups). Wired means catalogued, persisted,
  runtime-bridged, and automated-tested; it is not a group-wide runtime claim.
  Automation-style modules are opt-in, disabled by default, and scoped to a
  Serveri
  play. Their runtime path intentionally matches Hypixel protocol contracts;
  no separate server-detection branch is planned.
- The mining mechanics registry covers all 25 official Mining Collection keys,
  while unresolved mechanics, aliases, and canonical item identities remain
  explicit. Reviewed identity boundaries, exact-name area/subarea recognition,
  context gates, Hard Stone quantity safety, and one shared mining break scan
  are separate verified capabilities.
- Bazaar valuation is sell-side (`sell_summary`, then
  `quick_status.sellPrice`). Invalid or excessive prices stay unavailable; a
  malformed sibling product must not discard a valid product.
- Opt-in diagnostics are local (`rotclient-diagnostic-*.log`) and must be
  reviewed before sharing.
- Canonical command root is `/rotclient`; `/miningtracker`, `/MiningTracker`,
  and `/miningui` remain temporary compatibility aliases for `2.0.0+mc26.2`.

## Source-of-truth documents

Read these before changing behavior:

1. [docs/PROJECT_STATE.md](docs/PROJECT_STATE.md) — current implementation,
   runtime evidence, limitations, and next phase.
2. [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — component boundaries and
   canonical/shadow data flow.
3. [docs/TRACKING.md](docs/TRACKING.md) — material, gemstone, Current Session,
   and History accounting rules.
4. [docs/TESTING.md](docs/TESTING.md) — commands, validation levels, runtime
   procedure, and diagnostic markers.
5. [docs/BRANDING.md](docs/BRANDING.md) — display name, palette, icon, and
   visual identity.
6. [PRIVACY.md](PRIVACY.md) — network, local state, History, and diagnostics.
7. [CONTRIBUTING.md](CONTRIBUTING.md) — contribution and repository rules.
8. [docs/QOL_UTILITIES.md](docs/QOL_UTILITIES.md) — current QoL module catalog,
   evidence labels, safety scope, and runtime order.

## Current phase

`development` is the public integration branch. Feature work merges there;
`main` receives only a fully stable, tested line. The mining tracker,
Current Session, Powder Chest Tracker, bounded MOB ingest, and the 124-module
QoL dashboard are implemented and automated-tested. Powder Chest and
representative MOB paths now have controlled runtime evidence; Session History
2.0, the latest MOB noise-filter correction, and the expanded QoL matrix still
need controlled Minecraft confirmation. Automated tests and a clean build prove
the checked code and package only.

## Next engineering phase

The current smaller-QoL checkpoint is implemented and automated-tested:
Wardrobe Swapper, the shared Slayer suite (including history, pricing, webhook,
RNG projection and drop filters), Storage Overlay, Inventory Buttons, and
Missing Enchantments still need controlled Minecraft confirmation. Run their
runtime matrix when the maintainer chooses. The
remaining Mining, Gemstone, and Powder Chest runtime work is intentionally
deferred, not discarded. After that larger tracker pass, run the Session History
2.0 lifecycle checklist. Do not treat automated coverage as Minecraft runtime
proof. Run the controlled six-family/miniboss/highlight/carry/Cocoon/Dagger/
Laser/history/webhook/RNG/drop-filter matrix before marking Slayer runtime
verified.
