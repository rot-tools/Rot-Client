# Rot Client Architecture

Rot Client (`fi.rotclient`) is a client-only Fabric mod. The runtime is organized around an explicit tracker-family selection, family-specific detection and correlation, persistent ledgers, and separate material and gemstone HUD rendering.

## Component map

| Component | Responsibility |
| --- | --- |
| `TrackerSelection` | Canonical material-or-gemstone selection and persisted selection ID. |
| `TrackerConfig` | Global settings, selected family, guarded routing helpers, and maps of persistent family state. |
| `TrackedMaterial` | Material-specific block mappings, item names, conversion multipliers, Bazaar IDs, and Fortune category. |
| `TrackingTarget` | Groups one or more material ledgers into a selectable material target. |
| `MaterialTrackerState` | Persistent session/lifetime material counters, observations, prices, sales, and active time. |
| `GemstoneType` | Canonical gemstone IDs, display names, item names, and Bazaar IDs. |
| `GemstoneTier` | Rough through Perfect tiers and their Rough-equivalent multipliers. |
| `GemstoneLedger` | Checked, non-negative tier quantities and Rough-equivalent totals for one gemstone. |
| `GemstoneTrackerState` | One gemstone's persistent session/lifetime ledgers, block totals, and active-time state. |
| `GemstoneTrackerRegistry` | Normalizes and accesses the per-gemstone state map. |
| `GemstoneLiveAccounting` | Final selected-gemstone guard before validated blocks or gains mutate persistent state. |
| `GemstoneBlockClassifier` | Maps stained-glass block states to gemstone types. |
| `GemstoneDirectBreakTracker` | Matches a recently attacked gemstone block to the corresponding server-side removal. |
| `GemstoneGainDetector` | Coordinates direct breaks, `PRISTINE`, signed Sack changes, correlation, diagnostics, and validated live callbacks. |
| `GemstoneMiningCorrelationGate` | Keeps short-lived direct-break and `PRISTINE` signals isolated by gemstone for correlation diagnostics. |
| `GemstoneMiningBatchTracker` | Owns consumable Rough authorization and non-crediting Flawed confirmation batches. |
| `GemstoneSackCorrelationEvaluator` | Produces a short-window correlation classification without mutating state. |
| `SackChangeParser` | Parses signed item changes and their source Sack names without assigning them to a tracker. |
| `PristineMessageParser` | Parses exact gemstone and Flawed quantity from a valid `PRISTINE` message. |
| `MiningBreakDetector` | Correlates material attack/block evidence, packet changes, nearby snapshots, and supported ability windows. |
| `MiningGainDetector` | Observes material inventory, Sack, Compact, and sale messages while maintaining safe baselines. |
| `SackObservationGate` / `SaleObservationGate` | Reject duplicate or uncorrelated material observations. |
| `BazaarPriceService` / `FortuneDetector` | Resolve material market prices and optional live Fortune observations. |
| `RotClientClient` | Fabric entrypoint, event routing, command registration, state transitions, mutation callbacks, and persistence coordination. |
| `RotClientTheme` | Canonical blue-slate Rot Client palette (surfaces, teal accent, text, semantic colors, HUD and dashboard tokens). |
| `RotClientHud` | Family-specific HUD rendering, graph samples, movement, scaling, and clamping. |
| `PowderChestHud` | Independent movable HUD for Powder Chest Tracker Current Session projection. |
| `QolUtilityCatalog` / `QolUtilityConfig` | Minecraft-free QoL module catalog and persisted toggles/settings. |
| `QolUtilityDashboard` / `QolOverlayHud` | Client UI QoL pages and overlay HUD editor wiring. |
| `MiningUiScreen` | Module UI, searchable selection, settings, reset, edit, and enable/disable controls. |
| `HudLayoutMath` | Minecraft-independent layout heights and scale-aware clamping math. |
| `TrackerStore` | JSON load/save, normalization, and schema migration (`rotclient.json`). |
| `RotClientLegacyDataMigrator` | One-time byte-copy migration from legacy `miningtracker.json` / `miningtracker-session-history.json` when new files are absent. |
| `DiagnosticRecorder` | Opt-in structured engineering events; it does not decide accounting (`rotclient-diagnostic-*.log`). |
| `GemstoneDiagnosticObserver` | Diagnostic-only inventory and Sack observation; it never mutates tracker state. |
| `MiningResourceCatalog` | Canonical resource definitions and alias resolution for shadow classification. |
| `SkyBlockMiningResourceRegistry` | Mining identity, mechanics, context, Sack aliases, confidence, and provenance, including explicit unresolved fields. Its collection-key coverage is not complete mechanics coverage. |
| `SkyBlockPublicDataCache` | Bounded four-endpoint public-data cache with validation, temporary-file replacement (atomic move where supported), and offline fallback. |
| `SkyBlockMiningReconciliation` | Pure official-versus-Rot status and missing-fact analysis; it never adopts data. |
| `MiningSessionLedger` | Ephemeral, append-only shadow ledger with deduplication; not persisted. |
| `MiningSessionClassifier` | Classifies observations into source categories. |
| `MiningSessionTargetMirror` | Mirrors already-accepted live target events into `TARGET_MINED`. |
| `MiningSessionShadowObserver` | Classifies off-target mining into `OTHER_MINED` using direct-break correlation. |
| `MiningSessionDirectBreakTracker` | Time-windowed pending break contexts for shadow correlation (material window: 10,000 ms). |
| `MiningSessionEngine` | Transient classifier / correlation / dedupe / target-mirror state for Analytics TARGET and diagnostics. Does **not** own persisted Current Session quantities. |
| `RotClientCurrentSession` | **Canonical** persistent ledger for generic Current Session accounting. One accepted observation → one `creditItem` mutation. Pause closes target/area segments; Resume opens new boundaries. Analytics Other Mined and HUD OTHERS are read-only projections of this ledger. |
| `RotClientSessionFreeze` | Immutable Session History 2.0 boundary copied from Current Session. It closes target/area segments and freezes pause, item-row, and valuation metadata; it is not a live ledger. |
| `MiningSessionHistoryStore` | Bounded schema v2 archive with schema v1 read compatibility and same-directory replacement; the final move is atomic where the filesystem supports it. `Start New` is allowed to reset Current Session only after this store accepts the freeze. |
| `CurrentSessionStartNewCoordinator` | Coordinates archive-first Start New across the separate History and Current Session files, restores the exact prior History document if publishing the next Current Session fails, and recovers recognized process-interruption boundaries through a bounded marker. |
| `CurrentSessionStartNewJournal` | Temporary non-ledger recovery metadata: schema, one-way session key, archive fingerprint, and boundary timestamp. It never stores item rows or quantities. |
| `CurrentSessionCanonicalIds` | Maps Hypixel / Bazaar product ids onto Rot Client stable item ids (`TITANIUM`, not `TITANIUM_ORE`). |
| `MiningSessionParity` | Compares live target totals against shadow `TARGET_MINED` mirrors. |
| `MiningSessionSnapshot` | Immutable point-in-time shadow ledger and parity view. |

## Tracker-family separation

A `TrackerSelection` is exactly one of:

- **Material selection:** wraps a `TrackingTarget`, which routes to one ordinary/Pure Ore material ledger, the combined Mithril + Titanium ledgers, or the independent Tungsten or Umber ledger.
- **Gemstone selection:** wraps one `GemstoneType` and exposes no material target.

`TrackerConfig.selectedTarget()` predates the family split and retains a Gold fallback for compatibility with older callers and serialized assumptions. That fallback is not a runtime route. Family-aware code must first prove a material selection and then use guarded accessors. For a gemstone selection, `routedMaterials()` returns an empty list, `routesMaterial(...)` returns false, and the required material accessors fail explicitly.

Selection transitions persist and close the previous active-time window, clear transient detectors and correlation signals, select the new family, and leave tracking disabled until the user explicitly enables it. State is not copied across families.

## Material data flow

The material path is intentionally material-specific rather than one universal signal chain:

1. `MiningBreakDetector` observes the player's recent target and server-authoritative block changes. It can also use a nearby snapshot and supported mining-ability context when vanilla client destruction callbacks are insufficient.
2. A verified block event is routed through `RotClientClient` only when the selected `TrackingTarget` includes that material.
3. `MiningGainDetector` maintains inventory baselines and inspects relevant game messages and hover content. Inventory, Compact, Sack, and sale observations are correlated with recent material activity.
4. `SackObservationGate` and `SaleObservationGate` reject duplicate or uncorrelated observations before they can affect accounting.
5. `MaterialTrackerState` stores independent session and lifetime blocks, weighted base drops, observed item equivalents, Sack and compactor data, sale data, prices, and active time.
6. `BazaarPriceService` updates material price caches, while `FortuneDetector` optionally resolves live Mining and category Fortune. Fortune paths are never routed for gemstone selections.
7. `RotClientHud` renders the selected target. A combined Mithril + Titanium target displays independent material ledgers and combined presentation values.

Block mappings, base yields, item representations, and Fortune categories differ by material. Documentation and tests must not assume that every material uses identical evidence.

## Gemstone data flow

The live gemstone path separates mining context from exact quantity evidence:

```text
attacked gemstone block + matching server removal
    -> accepted direct break
    -> pending per-gemstone mining batch
    -> block count and active-time context

PRISTINE message
    -> exact gemstone type and Flawed quantity
    -> immediate Flawed credit
    -> pending confirmation amount

positive signed Gemstone Sack change
    -> exact Rough quantity
    -> consume matching direct-break batch once
    -> Rough credit

Flawed Sack change
    -> match pending PRISTINE amount
    -> confirmation only; no second Flawed credit

validated callback
    -> GemstoneLiveAccounting selection guard
    -> per-gemstone session and lifetime state
```

A block update establishes mining context and a block count. It is not exact item quantity evidence. Exact live quantities currently come from accepted positive Rough Sack changes and parsed `PRISTINE` Flawed rewards. Fine, Flawless, and Perfect Sack entries are not accepted as mined-item credit by the current batch policy.

## Correlation and deduplication

| Mechanism | Current window | Purpose |
| --- | ---: | --- |
| Attack target to matching server removal | 2,500 ms | Proves a direct break of the same position and gemstone type. |
| Short correlation gate | 8,000 ms | Produces per-gemstone correlation context and diagnostic classification. |
| Consumable direct-break batch | 60,000 ms | Authorizes one delayed positive Rough Sack credit. |
| Pending `PRISTINE` confirmation | 120,000 ms | Allows a delayed Flawed Sack confirmation without duplicate credit. |
| Gemstone Sack message fingerprint | 2,500 ms | Rejects immediate redelivery of the same visible message and hover content. |
| Shadow material direct-break to sack/inventory | 10,000 ms | Correlates off-target material breaks with delayed Mining Sack delivery in shadow `OTHER_MINED` path; newest matching context is selected first. |

The consumable batch decision, not the short diagnostic classification, is authoritative for live Sack credit. A Rough credit consumes the matching gemstone's accumulated direct-break batch. A Flawed confirmation consumes only the matched pending `PRISTINE` amount and never creates another credit.

Signed non-positive changes and unsupported Sack sources are rejected before batch evaluation, so they do not consume valid signals. Batch state is stored per gemstone; a wrong gemstone cannot consume another gemstone's batch. Unsupported tiers do not credit or consume a valid Rough batch.

The current Sack fingerprint is immediate-delivery deduplication, not a historical event database. `GemstoneDiagnosticObserver` has its own diagnostic-only observation state. Diagnostic observers and correlation records may describe what would be credited, but only the selected, enabled live path can invoke `GemstoneLiveAccounting`.

## Persistence

`TrackerStore` persists the normalized `TrackerConfig` JSON as `rotclient.json` in the Fabric configuration directory. On first launch after upgrading from MiningTracker, `RotClientLegacyDataMigrator` byte-copies `miningtracker.json` to `rotclient.json` when the new file does not exist; the legacy file is not deleted or modified.

The current schema persists:

- The selected tracker target.
- Material and gemstone session ledgers.
- Material and gemstone lifetime totals.
- HUD position and scale.
- HUD, dashboard, Fullbright, tax, and Fortune settings.
- Cached material prices and their update timestamps.

Activity timestamps are closed and cleared when the client stops, and launch preparation clears open activity windows. Closed-client time is never added to active time. Tracker enablement is explicit per launch: the loaded `enabled` value is set to false during client initialization. Transient detector batches, duplicate fingerprints, live graph samples, diagnostic recording state, and shadow ledger data do not persist.

## Mining-session observation and Current Session ingestion

`MiningSessionEngine` owns transient classification, direct-break correlation, deduplication, target parity, and diagnostic snapshots. It does not own persisted Current Session quantities. `RotClientCurrentSession` is the durable generic item ledger; accepted non-target mining observations reach it through one terminal mutation path. Live target ledgers remain authoritative and independent.

```text
Current Session active
  -> MiningSessionEngine collection/correlation state

live accepted target event
  -> authoritative target ledger
  -> MiningSessionTargetMirror -> transient TARGET_MINED parity

confirmed off-target break + correlated sack/inventory
  -> MiningSessionShadowObserver classification/dedupe
  -> SackItemGainPipeline -> RotClientCurrentSession.creditItem (once)

diagnostic parity tick
  -> MiningSessionParity compares live vs shadow TARGET_MINED

HUD / Analytics
  -> read-only Current Session OTHERS / OTHER_MINED projection

confirmed Start New
  -> durable bounded Start New marker (no ledger rows)
  -> RotClientSessionFreeze from canonical Current Session
  -> Session History temporary-file replacement (atomic move where supported)
  -> fresh Current Session only after archive success
  -> exact prior-History restoration if fresh Current Session persistence fails
  -> marker removal after completion; startup reconciliation if interrupted

shadow status / diagnostic stop
  -> MiningSessionSnapshot
```

Session boundary invariants:

- Live target accounting remains authoritative; generic Current Session accounting does not replace material or gemstone target ledgers.
- Transient engine state and diagnostic snapshots are not persisted. Accepted `OTHER_MINED` item quantities are persisted separately by `RotClientCurrentSession` and projected into HUD OTHERS and Analytics.
- Target-family gains are excluded before Current Session OTHERS ingestion. Selection transitions clear pending correlation so a delayed prior-target Sack batch cannot be reclassified as OTHERS.
- Session History v2 preserves canonical rows and their original `MOB`, `CHEST`, or `CURRENCY` source without reclassification. MOB has bounded live Current Session ingest; Powder Chest Tracker projects CHEST and CURRENCY rows. Representative Powder Chest and MOB paths have controlled runtime evidence, while broader identity/reward formats and the latest MOB noise-filter correction remain open.
- Per-resource Bazaar instant-sell gross valuation is refreshed independently; unresolved resources remain explicitly unresolved.
- History freezes store the active/paused lifecycle split, closed target and parent-area segments, item rows, and the valuation basis timestamp. Staleness is decided at freeze time without mutating the live ledger.
- HUD and Analytics never write History data and never receive ownership of Current Session quantities.
- Start New is archive-first and still not a single two-file storage
  transaction. Reported Current Session write failures restore the exact prior
  History document. A bounded marker lets startup distinguish and recover
  recognized process interruptions before archive, between archive and Current
  Session publication, and after publication. Invalid marker data fails closed;
  this is not a power-loss atomicity claim.

## HUD architecture

`RotClientHud` chooses a material or gemstone renderer from `TrackerSelection`.

- The material HUD combines target-specific blocks, resource observations, value estimates, Fortune, active time, and optional graph data.
- The gemstone HUD shows the selected gemstone, tracker state, blocks, tier quantities, total items, Rough Equivalent, average per block, active time, and graph data.
- `PowderChestHud` is a separate movable, scalable card for Powder Chest Tracker rows.
- `QolOverlayHud` hosts Player Display, Performance HUD, and Pet HUD editor placement.
- `HudLayoutMath` keeps card heights and screen-origin clamping testable without Minecraft runtime classes.
- Shift-drag moves the HUD in edit mode; Shift-scroll scales it from 0.5 to 2.5.
- Movement and scaling clamp the scaled logical HUD bounds to the current screen.
- Gemstone metrics that are always shown are labelled `FIXED` in settings.
- Unimplemented gemstone value/profit controls are labelled `LATER`, and the UI states `VALUE / PROFIT NOT AVAILABLE`.

## Command architecture

The current client registers `/rot` as the canonical command root. Legacy aliases `/rotclient`, `/miningtracker`, `/MiningTracker`, and `/miningui` remain temporarily for `2.0.0+mc26.2` and show a deprecation notice.

| Command | Behavior |
| --- | --- |
| `/rot` | Open the Rot Client dashboard. |
| `/rot ui` | Open the same dashboard. |
| `/rot qol` | Open the QoL module catalog inside the dashboard. |
| `/rot edit` | Enable tracking and open the HUD editor. |
| `/rot layout reset [ui\|hud]` | Reset both layouts, or only the selected UI/HUD layout. |
| `/rot toggle` | Toggle the selected tracker on or off. |
| `/rot reset` | Reset only the selected target's session state. |
| `/rot status` | Show family-specific session status. |
| `/rot shadow status` | Show ephemeral shadow ledger totals, `TARGET_MINED`/`OTHER_MINED` quantities, parity status, and mismatch count. Requires an active or retained diagnostic session. |
| `/rot autoclicker add\|remove left\|right` / `list` | Manage the Serveri Auto Clicker item whitelist from the held item. |
| `/rot session start\|stop\|reset\|status\|copy\|save` | Resume, pause, clear, inspect, copy, or manually archive the canonical Current Session through shared lifecycle/projection controls. Manual archive requires PAUSED state. |
| `/rot history ...` | List, open, copy, delete, or confirmation-clear immutable local History records without replacing Current Session. |
| `/rot slayer status\|stats reset` | Inspect or reset the shared live Slayer session engine. |
| `/rot slayer carry manager\|list\|add\|remove\|complete` | Manage the one live per-player Slayer carry list used by the manager and HUD. |
| `/rot target coal\|iron\|lapis\|redstone\|emerald\|quartz` | Select the matching ordinary/Pure Ore ledger. |
| `/rot target gold` | Select Gold. |
| `/rot target diamond` | Select Diamond. |
| `/rot target mithril` | Select Mithril + Titanium. |
| `/rot target titanium` | Select Mithril + Titanium. |
| `/rot target tungsten` | Select the independent Tungsten target. |
| `/rot target umber` | Select the independent Umber target. |
| `/rot record start` | Start structured diagnostic recording and write a family-specific snapshot. |
| `/rot record stop` | Stop diagnostic recording. |
| `/rot debug tracking [on\|off\|clear\|status]` | Engineering-only tracking-trace controls; the bare `tracking` form also reports status. |
| `/rot fortune auto` | Enable optional live Fortune detection for a material selection. |
| `/rot fortune <mining> [material]` | Set manual Mining and category Fortune for a material selection. |

Gemstone targets are selected through the searchable UI; there are no gemstone target subcommands. Both Fortune command forms are material-only and return explicit rejection feedback for gemstone selections without mutating Fortune state.

## QoL architecture

QoL modules are defined in `QolUtilityCatalog` (Minecraft-free labels, groups, and setting types) and persisted through `QolUtilityConfig` inside `rotclient.json`. Client runtimes and mixins live under `src/client/java`. Policy classes that can be unit-tested stay in `src/main/java`.

The dashboard currently exposes 72 modules across Utilities, Render, HUD &
Display, Interface, Combat, Dungeons, Mining, Slayer, and Fishing.
`QolUtilityCatalog` owns Minecraft-free metadata; `QolUtilityConfig` and
`QolSkyblockExtras` own persisted values; pure `*Policy` classes stay in the
main source set; `*Runtime` bridges and mixins stay in the client source set.
Every numeric control requires an explicit `QolNumberSettings.spec`.

Wardrobe Swapper follows the same separation. `WardrobeKeybindPolicy` owns pure
binding, slot, menu-marker, timeout, and stationary-gate decisions. The client
`WardrobeAutoEquipRuntime` sends the real `/wd` command, accepts only the
expected Armor Sets container, maps Wardrobe 1–9 to container slots 36–44, and
cancels before the hidden click if stationary requirements stop being true.
The catalog retains the existing `qol.cheater_wardrobe` id for configuration
compatibility while presenting the feature as Wardrobe Swapper.

Slayer follows the same ownership rule. `SlayerPolicy` classifies Hypixel-style
quest chat, boss/miniboss/demon name tags, owners, tiers, drops, and Inferno
attunements without Minecraft dependencies. `SlayerSessionEngine` is the single
live Slayer state owner. `SlayerRuntime` observes Minecraft events; Slayer HUDs
and `SlayerCarryManagerScreen` are projections/editor surfaces over that engine,
not independent ledgers. Advanced Slayer modules must extend this boundary
instead of introducing another session counter.

`SlayerMechanicsPolicy` owns the pure Cocoon timer, Inferno attunement/dagger
family rules, delayed swap state, and Voidgloom laser-anchor decision. The
client runtime observes the exact chat/entity/attack protocols, reads
`td_attune_mode`, selects a compatible hotbar dagger, and exposes the movable
Cocoon timer as another read-only HUD projection. Entity suppression delegates
to the same runtime and never creates a second Slayer state owner.

`SlayerCarryPolicy` owns carry-price rules, reviewed webhook validation, carry
history serialization, and the RNG Meter probability projection.
`SlayerRngCatalog` is the reviewed item-id catalog used by the per-drop filter
editor. `SlayerCarryWebhookRuntime` is the only network boundary and only sends
an explicitly enabled completed-carry summary to the configured Discord
webhook. The manager, filters, pricing, RNG display, and completed history all
remain views or configuration around the shared engine.

The inventory suite preserves the same split. `StorageOverlayPolicy` owns
layout and selector decisions, while `StorageOverlayRuntime` replaces the
vanilla Storage GUI with a compact projection of pages previously observed from
genuine server containers; it does not invent, persist, or remotely fetch item
contents. Navigation uses the real overview selector slot or `/enderchest` /
`/backpack`. `InventoryButtonsPolicy`
validates command-button layouts, while `InventoryButtonsRuntime` and
`InventoryButtonsEditorScreen` render and edit those local commands. Missing
Enchantments is a tooltip projection over the held item's real enchantment
data, with upgrade and conflict decisions kept in `MissingEnchantsPolicy`.

Automation-style modules are opt-in and disabled by default. They target a
Serveri
play. They intentionally share one Hypixel-protocol-compatible implementation
path instead of a separate server-detection branch. See `docs/QOL_UTILITIES.md`
for the current catalog and evidence rules.

## Mining session ledger: current and remaining boundaries

### Implemented canonical and transient paths

- `TARGET_MINED` remains a transient mirror of already-accepted authoritative
  target-ledger events for parity and diagnostics. It is not persisted as a
  second target ledger.
- Confidently correlated `OTHER_MINED` observations can pass through the single
  terminal mutation path into canonical `RotClientCurrentSession` exactly once.
  HUD OTHERS and Analytics read those persisted rows; they do not own copies.
- Hard Stone direct-break/Sack correlation has controlled shadow evidence.
  Persistent Current Session Mining Sack ingress has controlled runtime evidence
  for non-target Mithril and Titanium. This evidence does not approve every
  catalogued material or source.
- Deduplication, correlation consumption, target-family exclusion, transient
  target parity, and `/rot shadow status` remain engine responsibilities.
- Material direct-break correlation uses a 10,000 ms window and
  newest-context-first selection.

### Remaining live-ingress work

Source types that have bounded or projected live paths and still have open
runtime coverage:

- `MOB` — bounded live ingest (player-caused melee/projectile, reviewed Sack
  sources, action-bar `+N`, rare-drop chat, kill-window coins). Representative
  paths have runtime evidence; the latest noise-filter correction and broader
  identity matrix need retest. Remote-ability rewards remain unimplemented.
- `CHEST` / `CURRENCY` — Powder Chest Tracker projects Current Session rows;
  its core lifecycle and observed reward totals are runtime verified, while
  broader chat/Sack formats and unmatched-event expiry remain open.

Additional remaining work:

- Extend price mapping and value presentation only for future source rows whose
  live ingress has been independently approved. Existing Current Session and
  History valuation must remain quantity-independent and explicit when
  unavailable.
- Chest chat/Sack deduplication and unmatched-event expiry.
- Durable Current Session ingestion for each independently validated non-mining
  source.
- Keep a separate live mutation gate for each proposed source. There is no
  global ban on the already-approved persistent mining OTHERS path.

Every proposed event should carry a stable item identifier, quantity, source
type, price-resolution status, unit price, estimated total, and timestamp. Item
display name alone must never authorize credit.

The design must reject manual transfers and Bazaar purchases, deduplicate chest
reward chat against Sack delivery, expire unmatched chest events, and exclude
chest gemstones from target `AVG / BLOCK`. Currency may be reported separately
but must not be treated as an item or added to item-derived coin totals.

New source categories must remain diagnostic/shadow-first until runtime
validation demonstrates source classification, deduplication, expiry, pricing
behavior, and family isolation. Existing accepted mining OTHERS can remain
persistent; unrelated target accounting routes must remain unchanged.

## SkyBlock Data Authority and Provenance

This section is a **binding architectural invariant** for Rot Client. It is not milestone-scoped advice.

Detailed operational notes live in [`docs/skyblock-data.md`](skyblock-data.md). License and provenance audit artifacts live under `docs/reports/`.

### Authority stack

| Priority | Layer | Role |
| ---: | --- | --- |
| 1 | Official Hypixel data | Primary authority whenever the required fact is actually provided by Hypixel. |
| 2 | Third-party community references | Research, enrichment, coverage, aliases, patterns, and reference only. |
| 3 | Rot Client live traces | Runtime verification and gap detection against current client/server communication. |
| 4 | Rot Client canonical dataset | The normalized representation domain and runtime code reason against. |

#### 1. Official Hypixel data

Primary authority when Hypixel exposes the fact. Examples: official SkyBlock item IDs and metadata (`/v2/resources/skyblock/items`), Bazaar product data (`/v2/skyblock/bazaar`), and other official published game data.

Do **not** override an explicit current official fact with a community repository unless there is concrete evidence that the official field is stale or ambiguous. Such conflicts must be surfaced and kept auditable; they must never be silently replaced.

#### 2. Third-party community references

Useful for filling fields Hypixel does not expose and for understanding real client behavior. They do **not** own Rot Client's canonical truth. Source, license, and provenance information must be preserved. No third-party source tree may be copied wholesale into this repository or the shipped JAR.

#### 3. Rot Client live traces

Used to prove what the actual client/server communication currently exposes and to detect gaps: conflicts, missing aliases, parser failures, new items, new area strings, and quantity/source behavior. A display string alone must **not** automatically promote an identity to verified canonical metadata.

#### 4. Rot Client canonical dataset

The only dataset domain/runtime code should reason against directly. It may contain official fields, verified enrichment, aliases, relations, target-family metadata, pricing mappings, and explicit overrides, each with provenance. Runtime gameplay code uses local normalized snapshots and indexes; it must not query upstream repositories directly.

### Binding rules

- Hypixel wins when an explicit current official fact exists.
- Community sources fill gaps and improve coverage; they do not silently override primary authority.
- Live traces verify behavior and may expose upstream conflicts.
- Conflicts remain visible and auditable.
- Generated upstream metadata and handwritten Rot Client domain rules are separate artifacts.
- The catalog is **never** an allow-list for gameplay observation.
- Unknown stable IDs with credible gameplay provenance remain valid observations.
- Dynamic market data is separate from static item metadata.
- Bazaar valuation remains Bazaar instant sell (gross), using the existing sell-side policy (`sell_summary` with `quick_status.sellPrice` fallback). No buy-side substitution.
- Public metadata refresh must not upload user, session, inventory, or chat data.
- Offline mode must retain tracking functionality when refresh or network access fails.
- Every non-trivial enrichment or override must carry provenance.

### Mining mechanics and physical evidence

The generated item snapshot and handwritten mining-mechanics layer have
different ownership. `canonical-items.v1.json` records normalized upstream
item/product facts. `mining-resources.v2.json`, loaded through
`SkyBlockMiningResourceRegistry`, records reviewed gameplay semantics and keeps
physical block tokens, Rot resource IDs, official Hypixel item/drop IDs, and
Bazaar valuation IDs separate.

The registry represents all 25 official Mining Collection keys, but that is
key-set coverage only. Several mechanics, aliases, and canonical item mappings
remain explicitly unresolved; the registry must not be described as complete
live tracker or complete identity coverage.

Physical block state is candidate evidence, never item identity. Context-gated
resources must pass `MiningBlockEvidencePolicy` before a detector may create a
direct-break signal. Every mechanics row marked `requiresAreaContext` is
rejected outside its reviewed parent areas unless that resource is the user's
explicit selected target. Active routes use this for Hard Stone, Mithril,
Titanium, Tungsten, and Umber; Glacite metadata remains a future detector
boundary. Ordinary Stone is rejected as Hard Stone outside Crystal Hollows,
Glacite Tunnels, and Glacite Mineshaft.
Quantity still comes from inventory/Sack evidence; break count never fabricates
a Hard Stone quantity.

The fallback detector retains its 15 x 11 x 15 coverage (2,475 positions).
`MiningBreakScanFrame` captures that state once per tick and shares it across
active material detectors. Diagnostic recording emits a low-frequency
`BREAK_SCAN_PROFILE` aggregate without coordinates or player data.

Mining-area parsing accepts exact normalized canonical location rows. The
current parent/subarea model includes Dwarven Mines, Crystal Hollows, the
Glacite locations, and Deep Caverns with its six official floors. An immutable
`SkyBlockLocation` updates parent and optional child together; an unknown child
is represented as parent-only, and an explicit world reset clears sticky state.
Narrative text and partial names remain unknown.

The development-time public-data flow is separate from runtime gameplay:

```text
public Hypixel items / collections / skills / Bazaar
  -> bounded sequential fetch or validated offline cache
  -> temporary-file cache replacement (atomic move where supported)
  -> pure reconciliation and generated review report
  -> explicit maintainer --adopt only
```

No inventories, chat, session histories, diagnostics, player data, or API
credentials enter that flow.

Sack occurrence identity is assigned at `SackComponentIngress`. Re-delivery of
the same component retains one short-lived identity; separate equal components
receive separate identities. This supplements the existing correlation and
ledger dedupe so duplicate callbacks remain at-most-once while legitimate
equal-quantity gains may consume distinct direct-break contexts.
