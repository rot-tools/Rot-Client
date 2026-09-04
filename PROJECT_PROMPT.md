# Rot Tools maintainer brief

Rot Tools (formerly MiningTracker / Rot Client) is a client-side Fabric
mod for Minecraft `26.2` using Java `25`. The current mod version is
`2.0.1+mc26.2`, and active work is on `development` in the public
repository [rot-tools/Rot-Client](https://github.com/rot-tools/Rot-Client).
Stable releases merge into `main`. See `docs/BRANCHING.md`.
The original author identity is OgRudolf (historical attribution).

## Identity

| Item | Value |
| --- | --- |
| Product | Rot Tools |
| Repository | [rot-tools/Rot-Client](https://github.com/rot-tools/Rot-Client) |
| Mod ID | `rotclient` |
| Package | `fi.rotclient` |
| Canonical command | `/rotclient` (and `/rotclient ui`) |
| Legacy aliases (2.0.0) | `/miningtracker`, `/MiningTracker`, `/miningui` |
| Main config | `rotclient.json` |
| Current Session | `rotclient-current-session.json` |
| Session History | `rotclient-session-history.json` |
| Appearance/workspace | `rotclient-appearance.json`, `rotclient-workspace.json` |
| Diagnostics | `rotclient-diagnostic-*.log` |
| Playable JAR | `RotClient-2.0.0+mc26.2.jar` |
| Visual theme | `RotClientTheme` — blue-slate surfaces, teal interaction accent, semantic status colors (see `docs/BRANDING.md`) |

Legacy `miningtracker.json` and `miningtracker-session-history.json` are migrated
by byte-copy on first launch when the new files do not exist. Legacy files are
not deleted.

## Working rules

- Verify the repository root, branch, HEAD, remote relationship, and
  working-tree scope before changing anything.
- Treat existing user changes as owned by the user. Stage only explicitly
  approved files.
- Never push to the historical `ogrudolf-old` remote.
- Do not force-push, rewrite history, deploy a JAR, or modify runtime files
  without explicit approval.
- Discuss the project with the owner in Finnish, but keep code, UI strings,
  comments, commit messages, and public documentation in English.
- Keep public files free of personal machine paths and usernames, launcher
  instance names, logs, screenshots, secrets, and machine-specific settings.
- Existing automation-style development modules
  are opt-in, disabled by default, and limited to Serveri. Preserve that usage
  scope and one SkyBlock-protocol runtime path; do not add a separate
  server-detection branch unless the owner changes scope.
- Use Java 25 and the Gradle wrapper for verification. Gradle declares a Java
  25 toolchain.
- A clean build proves packaging; runtime claims require controlled in-game
  evidence.
- Rot Tools is local-first: no telemetry, cloud sync, or remote state upload.

## Core invariants

- Current Session is the one canonical durable generic live ledger.
- Session History is an immutable frozen archive, never a second live ledger.
- HUD and Analytics are read-only projections.
- Material and gemstone target routes remain separate and may mutate only the
  selected family's authoritative state.
- One accepted generic observation may produce at most one Current Session
  item mutation. Target-family gains must never leak into OTHERS.
- Unknown stable item IDs are retained where appropriate rather than silently
  discarded.
- Pause stops Current Session collection and closes open target/area segments;
  Resume opens new segment boundaries. Target auto-pause must not stop generic
  Current Session observation.
- Restart preserves Current Session.
- Start New must archive successfully before creating the next session. Archive
  failure preserves the original session identity, lifecycle, and quantities.
  If publishing the next Current Session fails, exact prior-History restoration
  is attempted and any rollback failure must be surfaced. A bounded non-ledger
  marker recovers recognized process-interruption boundaries; do not claim
  power-loss atomicity across the two files.
- Pricing never modifies quantities. Unavailable pricing stays explicit, and
  historical frozen valuation never changes after later Bazaar refreshes.
- Hard Stone quantity comes from a quantity-bearing signal, never one item per
  block. Stone is not Hard Stone without reviewed context.
- Bazaar is sell-side: `sell_summary`, then `quick_status.sellPrice`; there is no
  buy-side fallback.
- Diagnostic-only evidence never authorizes an otherwise invalid credit.

## Current feature state

- Material selections: Coal, Iron, Gold, Lapis, Redstone, Emerald, Diamond,
  Quartz, Mithril + Titanium, Tungsten, and Umber. Ordinary and Pure Ore forms
  share one ledger for their material; Tungsten and Umber remain separate.
- Gemstone selections: Ruby, Amber, Sapphire, Jade, Amethyst, Topaz, Jasper,
  Opal, Onyx, Aquamarine, Citrine, and Peridot.
- The gemstone HUD, active-time calculation, routing safety, and Gold positive
  control have controlled runtime evidence from checkpoint `0ee5115`.
- Shadow target parity and Hard Stone OTHER_MINED correlation have controlled
  runtime evidence from checkpoint `3b372d4`.
- Canonical Current Session mining accounting, restart/pause/resume, and the
  Resume/Bazaar crash correction have controlled runtime evidence.
- Session History 2.0 freeze, archive-first Start New with explicit compensating
  rollback outcomes and bounded process-crash recovery, durations, target/area
  segments, canonical rows, frozen valuation, and v1 compatibility are
  automated-test validated. Post-audit Minecraft runtime validation is pending.
- `MOB`, `CHEST`, and `CURRENCY` are archive-schema-ready; this is not complete
  durable live ingress.
- Powder Chest CHEST/CURRENCY projection and representative bounded MOB paths
  have controlled runtime evidence. The latest MOB noise-filter correction and
  broader identity matrix still require runtime retest.
- `QolUtilityCatalog` currently contains 124 wired modules across thirteen groups.
  They are automated-tested; do not claim group-wide runtime verification.
- Price Tooltip remote quote requests are allowed only while Price Tooltips is
  enabled. Public networking and third-party lowest-BIN use must stay disclosed.
- The mechanics registry covers all 25 official Mining Collection keys. This is
  registry coverage, not complete mechanics or canonical-identity coverage;
  unresolved gaps stay explicit. Reviewed identity boundaries, exact-name area
  matching, context gates, and the shared scan are separate capabilities.

## Next planned work

Preserve the public checkpoint. Wardrobe Swapper, the shared Slayer suite,
Storage Overlay, Inventory Buttons, and Missing Enchantments are implemented
and automated-tested; their coverage is not a runtime claim. Run the accumulated
validation matrix on the Serveri when requested. Mining, Gemstone,
and Powder Chest tracker
work is deliberately deferred until the maintainer returns to the larger
tracker pass. Then close mining M1 and perform the Session History 2.0 lifecycle
pass.

## Read before implementation

1. `docs/PROJECT_STATE.md`
2. `docs/ARCHITECTURE.md`
3. `docs/TRACKING.md`
4. `docs/TESTING.md`
5. `docs/BRANDING.md`
6. `PRIVACY.md`
7. `CONTRIBUTING.md`
8. `docs/QOL_UTILITIES.md`
