# Rot Client Project State

This document is the maintainer-facing snapshot of the current engineering state. Validation labels are used deliberately: **runtime-tested** means observed in Minecraft, **unit-tested** means covered by automated tests, **compile-tested** means the relevant client source compiled, **experimental** means an engineering aid or incomplete surface, and **planned** means it is not implemented.

## Current status

### Current checkpoint

| Item | Current value |
| --- | --- |
| Stable branch | `main` (tested releases only) |
| Development branch | `development` |
| Repository | [rot-tools/Rot-Client](https://github.com/rot-tools/Rot-Client) (public development) |
| Runtime feature checkpoint | Canonical Current Session mining accounting + Resume/Bazaar crash correction (runtime-validated) |
| Current QoL / session checkpoint | 131 wired QoL modules. Custom Leap overlay now labels **DEAD** vs **OFFLINE** from Spirit Leap skull lore. Maxor crystal spawn HUD is **34 ticks** after beam / YOU TRICKED ME and follows Spawn Timer. **Fullbright and Night** remains Ready for Runtime Test (one implementation; lobby persist + dusk-once + hub snap). New **GUI** group with Custom Scoreboard as the first card. Dashboard chrome is **Rot Client** in accent red on the by-line. All new slices remain Ready for Runtime Test. |
| Minecraft | 26.2 |
| Mod | 2.0.1+mc26.2 |
| Display name | Rot Client (by-line, accent red). Author/owner: Rot Tools |
| Package | `fi.rotclient` |
| Playable JAR | `RotClient-2.0.1+mc26.2.jar` |
| Java | 25 |
| Gradle wrapper | 9.5.1 |
| Gradle toolchain | Java 25 (`toolchain { languageVersion = 25 }`) |
| Automated baseline | Current working tree: **2,158 tests**, 318 suites, 0 failures, 0 errors, 0 skipped; client compilation and build passed. Playable JAR SHA-256 `A4EF98E2C34E0EFE26D3A88C4F98FBE9AB7B8E599DF6596634C84B317BA5D9D4`. |
| Current phase | Mining tracker / M1 gemstone matrix is **paused**. Dashboard is 14 groups and **131** catalog parents. Fullbright and Night and Custom Scoreboard are Ready for Runtime Test. Appearance and HUD Elements Editor are Visuals-only; unhandled clicks dismiss the landing. |
| Online data foundation | Generated item/Bazaar snapshots plus a mechanics registry covering all 25 official collection keys while retaining explicit unresolved fields; see `docs/skyblock-data.md` |
| Next planned feature phase | QoL/runtime first: tooltip pan, HUD editor, dungeon/Slayer playtest. Mining tracker and gemstone matrix stay paused until reopened. |

The current client source is compile-tested with Java 25. Gradle's Java 25 toolchain provisions the compiler and test runtime even when `JAVA_HOME` points elsewhere. `MiningSessionEngine` remains transient classification, correlation, dedupe, parity, and diagnostic state; accepted `OTHER_MINED` quantities persist in the separate `RotClientCurrentSession` ledger and feed read-only HUD and Analytics projections. Live material and gemstone target ledgers remain authoritative.

Persistence uses `rotclient.json`, `rotclient-current-session.json`,
`rotclient-session-history.json`, `rotclient-storage-cache.json`, and
`rotclient-inventory-chrome-cache.json`. Legacy `miningtracker.json` and
`miningtracker-session-history.json` are migrated by byte-copy on first launch
when the new files do not exist; legacy files are not deleted.

## Runtime-tested features

Controlled runtime validation of the gemstone checkpoint (`0ee5115`) confirmed:

- Searchable tracker selection and gemstone target selection.
- Ruby direct-break correlation and accepted block accounting.
- Exact Rough Ruby credit from accepted positive Gemstone Sack changes.
- Exact Flawed Ruby quantities parsed from `PRISTINE` messages.
- Ruby state remained isolated from Gold and uncorrelated Topaz observations.
- Ruby HUD metrics in running and paused states.
- Active-time accumulation and the 60-second auto-pause window.
- HUD movement, scaling, and screen clamping.
- Material/gemstone tracker-family isolation.
- Fortune-command rejection while a gemstone target is selected.
- Gold material tracking as a positive control after gemstone tracking,
  including Sack accounting, manual Fortune, and reset isolation.
- Immediate duplicate rejection in the observed gemstone credit path.

### Runtime-tested shadow ledger (checkpoint `3b372d4`)

Controlled runtime validation with Gold selected and diagnostic recording active confirmed:

- Target parity MATCH: 371 live blocks / 371 shadow blocks.
- Target raw-equivalent parity MATCH: 27,206 live / 27,206 shadow.
- OTHER_MINED Hard Stone credited in shadow: 2,235 Hard Stone, 1 Enchanted Hard Stone.
- Delayed Hard Stone Mining Sack correlation succeeded at approximately 6.15 seconds (within the 10-second material window).
- Final shadow ledger: 33 `TARGET_MINED` entries, 3 `OTHER_MINED` entries.
- Target parity aggregate: 0 mismatches.
- Live Gold session totals unchanged by shadow observation.

### Runtime-tested persistent Current Session (2026-08-08)

Controlled Hypixel validation confirmed real Mining Sack ingress for Mithril
into `OTHER_MINED` and persistent Current Session, real `Last 9s` parsing,
restart persistence, pause/resume behavior, HUD/Analytics read-only projection,
and target-family exclusion. Hypixel controls notification timing, so a live
`Last 24s` message is not a requirement. Break count was not used as a Hard
Stone quantity source.

A later controlled run also confirmed real Titanium Sack ingestion and that
collection remained off while paused. The valuation-price crash exposed after
Resume was contained, regression-tested, and passed controlled runtime
revalidation.

## Test-verified features

Focused automated coverage verifies:

- The behavior-level clean-room audit of the reference SkyBlock clients, kept in
  the local engineering notes (behavior, settings, and mixin targets only; no
  reconstructed source).
- Atomic tracker-config replacement, pre-migration backup, and future-schema
  downgrade protection.
- Typed normalized domain events, profile-name extraction/scoped state, and the
  reflection-isolated optional Hypixel Mod API location adapter with scoreboard
  fallback.
- Duplicate SkyBlock mod warnings and the exact overlapping-client
  item-animation / Player Animals mixin gates.
- Terminal clicks are classified before queue mutation; queued clicks are
  discarded when the terminal closes or another container opens.
- Shared bounded tab-list snapshots and reduced Diana nametag scan cadence.
- Diana evidence is accepted and rendered only while the burrow scoreboard
  widget and Ancestral Spade context agree; stale burrow particles expire after
  750 ms.
- Retired owned Slayer entities cannot re-register between quest generations.
- Craft Helper now honors its child toggle, adds recipe totals to item
  tooltips, and includes quantities from observed Storage pages.
- Dungeon chest-profit projections disclose that values come from the bundled
  offline price snapshot.
- Bounded/cached chat rules reject risky regex constructs and bad replacement
  groups without breaking chat.
- The 131-parent catalog lock, evidence-state guards, status-badge layout, and
  configuration contracts for all newly exposed child settings.
- Hotkey sequence parser/editor round trips, bundled item search and recursive
  recipe aggregation with cycle termination, museum-set gaps, deterministic
  animation frames, and Player Animals scope selection.
- Registration of the full-shadow, player-animal, and render-only item-animation
  mixins plus the Source Sans built-in pack and OFL attribution.
- Player Animals uses one `LivingEntityRenderState`-compatible quadruped model
  type for every species shape, avoiding invalid species-state bridge casts
  during transformed class verification. Startup retest is pending.
- The custom cursor is emitted once from the final Screen render wrapper, after
  container subclasses and tooltips. Pointer smoothing and duplicate storage
  submissions were removed; storage transitions use two bounded restores so
  movement is preserved without pinning the pointer. The default/legacy-default
  visual now resolves to a conventional white arrow with dark outline.
- HUD Elements Editor is a single switchboard: vanilla/Hypixel layers hide through Fabric
  HUD registry wraps (on/off), Rot overlays keep move/scale/on-off. The landing page lists
  every layer; the world editor is one chrome card with right-click hide and Ctrl+Z undo.
  Vanilla hearts/hotbar are not dragged.
  Minecraft playtest pending.
- Signed Sack parsing, including preservation of negative deltas.
- Rejection of negative Sack changes without consuming a pending batch.
- Rejection of unsupported Sack sources without consuming a pending batch.
- Preservation of another gemstone's pending batch when the observed gemstone is wrong.
- Tracker-family routing safety: gemstone selection exposes no material routes.
- Reset isolation between material and gemstone families.
- Selection-transition isolation between material and gemstone state.
- Active-time arithmetic, pause-window capping, backward-clock handling, and normalization.
- Overflow behavior without partial active-time mutation.
- Gemstone and material HUD layout math, including scale-aware screen clamping.
- Shadow `TARGET_MINED` mirroring from accepted live target events.
- Shadow `OTHER_MINED` classification for non-target materials (including Hard Stone).
- Material direct-break correlation window (10,000 ms) and newest-context selection.
- End-to-end Hard Stone sack correlation regression at 7,320 ms delay.
- Shadow parity comparison between live and shadow `TARGET_MINED` quantities.
- Shadow ledger deduplication and correlation consumption.
- Legacy config/history byte-copy migration without deleting source files.
- All 25 official Mining Collection families in the separate mechanics registry.
- Physical block/gameplay/Hypixel/Bazaar identity boundaries.
- Block, Ore, Dwarven Metal, and Gemstone Fortune parsing/configuration.
- Hard Stone area-gate acceptance and rejection cases.
- Exact 2,475-position scan geometry and six-detector read reduction model.
- Separate equal Sack occurrences versus same-component redelivery.
- Exact area-name matching that rejects narrative fragments.
- Exact Deep Caverns parent and six-floor recognition.
- Context gating for every reviewed resource marked `requiresAreaContext`.
- Reviewed Dwarven Metal mechanics for Mithril, Titanium, Tungsten, Umber, and Glacite.
- Four-endpoint public-data cache validation, offline fallback, and temporary-file replacement with atomic move where supported.
- Official-versus-Rot reconciliation statuses, conflict reporting, and missing-fact reporting.
- Shared scan bounds that reject negative or excessive detector plans.
- Session History 2.0 immutable freeze of canonical Current Session rather than
  transient Analytics ownership.
- Archive-before-reset Start New ordering, preservation of the original session
  after archive failure, and explicit exact-History-restoration outcomes if the
  next Current Session cannot be published.
- Bounded Start New process-crash recovery metadata and startup reconciliation
  without storing a second ledger or item quantities.
- Exact active/paused duration split and closed target/parent-area segments.
- Immutable canonical item rows and valuation snapshots, including stale-price
  handling without live-row mutation.
- Schema v1 History compatibility alongside schema v2 records.
- Distinct `MOB`, `CHEST`, and `CURRENCY` archive rows without claiming complete
  Hypixel runtime proof for those live paths.
- Bounded MOB live ingest: player-caused melee/projectile, Combat/Slayer/Dungeon
  Sack, action-bar `+N`, rare-drop chat, and `+N` coins in the kill window.
  Displayed Magic Find is session context only.
- Powder Chest Tracker presentation of Current Session `CHEST` / `CURRENCY` rows
  with an independent HUD.
- QoL catalog wiring for 131 modules across GUI, Utilities, Render, HUD & Display,
  Interface, Combat, Dungeons, Mining, Slayer, and Fishing. Catalog, settings,
  runtime bridges/mixins, and focused automated contracts are present; the
  group-wide Minecraft matrix remains pending.
- Explicit numeric ranges for the current settings sliders, including Auto
  Experiments Serum Count, click delay, and delay variety.
- Price Tooltip remote quote polling stays inactive while the module is
  disabled.
- One shared Slayer foundation engine now owns live quest, entity, statistic,
  drop, and carry state for the six boss families. Slayer HUDs and the carry
  manager project that engine. Cocoon Alert/Timer, delayed NBT-aware Inferno
  Dagger Swap, other-player Voidgloom Laser Hider, Attunement Display, Auto
  Soulcry, Slayer Sounds, Vengeance Timer/Damage, and Big Slayer Drops extend
  the same boundary; controlled Minecraft validation remains pending.

## Important invariants

These constraints are part of the current safety model:

- `TrackerSelection` determines the tracker family.
- `TrackerConfig.selectedTarget()` has a compatibility fallback to Gold for gemstone selections.
- No gemstone runtime path may call material-only accessors without first proving the selection is material.
- Runtime material access must use guarded routes such as `hasMaterialSelection()`, `requireMaterialTarget()`, `requireSelectedMaterial()`, `routedMaterials()`, or `routesMaterial(...)` as appropriate.
- A target gemstone gain must not be duplicated as another source.
- A `PRISTINE` Flawed credit must not be credited again from Sack confirmation.
- Negative Sack changes must never credit or consume a valid mining batch.
- A wrong gemstone or unsupported Sack source must not consume another target's pending signal.
- Diagnostics must remain observational and must never gate or duplicate domain accounting.
- Physical Stone is Hard Stone evidence only in a reviewed Hard Stone area.
- Context-sensitive physical evidence is rejected unless its mechanics row permits the current parent area.
- Break evidence opens correlation; observed item deltas remain quantity authority.
- The mechanics registry enriches decisions but is never an observation allow-list.
- One accepted generic item observation produces at most one `RotClientCurrentSession.creditItem` mutation. Transient engine ledgers never replace persisted Current Session rows.
- HUD OTHERS and Analytics OTHER_MINED are read-only projections of Current Session; they do not own independent durable quantities.
- Pause closes open Current Session target/area segments, and Resume opens new boundaries without rewriting the closed segments.
- Session History v2 freezes canonical Current Session only. Start New archives
  before reset and preserves the current session if the archive fails. A
  subsequent Current Session write failure restores the exact prior History
  document and surfaces restoration failure.
- History v2 retains active and paused duration, closed target/area segments, canonical item rows, and per-row valuation status. Schema v1 remains readable.
- `MOB`, `CHEST`, and `CURRENCY` rows survive History v2 without source reclassification.
  MOB has bounded live Current Session ingest; Powder Chest Tracker projects CHEST
  and CURRENCY rows. Controlled Hypixel runtime validation is still pending.
- All 25 official Mining Collection keys are represented in the mechanics registry, but unresolved mechanics, aliases, and canonical identities remain explicit gaps.
- Compilation and tests do not replace runtime validation.
- Rot Tools mod ID `rotclient` is incompatible with a simultaneous MiningTracker JAR install.

## Current known limitations

- There is no mining-session coin-profit ledger for gemstones.
- Persistent HUD OTHERS covers confidently correlated non-target mining items; the new detector hardening still requires runtime validation.
- `MOB` live ingest is bounded (player-caused melee/projectile, reviewed Sack
  sources, action-bar `+N`, rare-drop chat, and kill-window coins).
  Representative melee, bow, non-flesh, and coin paths have runtime evidence;
  the latest Magic Find suffix/noise-filter correction still needs retest.
  Remote-ability rewards are not ingested.
- Powder Chest Tracker projects durable Current Session `CHEST` and `CURRENCY`
  rows into a dedicated HUD. Reward totals, lifecycle controls,
  disable/re-enable identity continuity, and independent HUD positioning have
  runtime evidence; broader reward formats remain open.
- QoL modules are wired and automated-tested, not group-wide runtime verified.
  Automation-style modules are opt-in and disabled by default for a
  Serveri. Runtime logic deliberately
  follows the same Hypixel protocol path without separate server detection.
- Gemstone Bazaar and NPC value calculation are not implemented.
- Graph history can remain visible after the tracker pauses.
- Sack deduplication focuses on immediate duplicate delivery; it is not a general historical event store.
- Some older material accounting arithmetic still uses legacy mutation patterns rather than uniformly checked ledger operations.
- Coal, Iron, Gold, Lapis, Redstone, Emerald, Diamond, and Quartz each combine ordinary and Pure Ore blocks in one selectable ledger. Tungsten and Umber are independent selectable targets. This expanded matrix is automated-tested and awaits controlled Hypixel runtime validation.
- Current Session independently verifies its active tracker selection at the
  final TARGET handoff. Foreign materials, wrong gemstones, and unknown target
  identifiers cannot create durable TARGET rows even if an upstream caller is
  misrouted. Matching inventory and Sack raw totals reconcile once across every
  selectable material, including Redstone.
- Legacy command aliases (`/miningtracker`, `/MiningTracker`, `/miningui`) remain for `2.0.0+mc26.2` only.
- Start New still updates separate History and Current Session files. Recognized
  process-interruption boundaries now use a bounded startup recovery marker;
  unreadable marker data fails closed and power-loss atomicity is not claimed.
- Dungeon awareness (2026-09-05): Entrance HUD floor, Hypixel -200 room-core
  grid, Magical Map wither/blood/entrance world boxes (Depth Check still hides
  through walls), hashed-room secret cache this run, Extra Stats requeue from
  chat or GUI without matching `the catacombs -`. Starred ESP remains nearby.
  No party secret websocket.
- Dungeon map HUD (2026-09-06): 128 Magical Map paper (floor start corners,
  Hypixel room/door palette, unopened gray, split names, yaw heads). Scan every
  tick; hashing stays throttled. Map Mode is Explored or Reveal Hidden.
  DUNG-001 / DUNG-009 stay Ready for Runtime Test. Automated tests are not
  runtime Complete.
- Dungeon leftover helpers (2026-09-06) on existing 131 parents: named ground-drop
  highlight, 7s clicked-secret boxes, Wither/Blood key-drop HUD, Pre-4 device
  complete, hide other Goldor progress titles, mute those titles at SS/Pre-4,
  hide teammates at Simon Says and for 3s after leap, teammate Melody HUD from
  party `%`, held relic pad, wrong-relic block (cheat, off). DUNG-010 stays
  Ready for Runtime Test, not runtime Complete.

For a class-by-class briefing of the shipped JAR (what to open first, Policy vs Runtime vs Mixin, dashboard vs mining vs Current Session), see [`docs/CODE_WALKTHROUGH.md`](CODE_WALKTHROUGH.md).

## Local development workflow

1. Use Java 25 for the Gradle process. Gradle's `toolchain { languageVersion = 25 }` block provisions JDK 25 for compile and test tasks.
2. Run `./gradlew test --rerun-tasks`.
3. Run `./gradlew compileClientJava`.
4. Run `git diff --check`.
5. Run `./gradlew clean build`.
6. Select `RotClient-2.0.1+mc26.2.jar` from `build/libs/`; never select the sources JAR.
7. Back up the installed JAR and install the playable JAR into a Serveri instance's `.minecraft/mods/` directory. Remove any MiningTracker JAR first.
8. Verify the deployed artifact and runtime-test the feature before claiming completion.

## Next planned feature phase

Use this order for the next checkpoint:

1. Playtest the Serveri dungeon checklist (close and relaunch Prism): 128
   Magical Map paper (layout, names, no lag); Entrance HUD Floor Entrance; Door
   Highlight world boxes with Depth Check off vs on; hashed-room secrets after
   leaving a room; Mimic/Prince/Bat and blood/Watcher alerts; F7 P3 other
   players’ terminal chat plus Melody 3 / Numbers 10; Extra Stats requeue
   without a dungeon-start false fire. Do not mark DUNG-001 / DUNG-009 runtime
   Complete.
2. Continue smaller maintainer-selected QoL/settings slices. Wardrobe Swapper
   and the expanded Slayer foundation are automated-tested, including
   Cocoon/Dagger/Laser behavior, Attunement, Auto Soulcry, Vengeance, sound
   filtering, configurable carry prices/webhooks, persistent history, RNG
   projection, and per-drop Big Drops filters. Storage Overlay and editable
   Inventory Buttons are implemented and automated-tested.
3. Runtime-test the accumulated QoL batch later in one controlled pass, covering
   dashboard persistence, UI/window behavior, Wardrobe Swapper, Slayer
   Cocoon/Dagger/Laser behavior, inventory
   overlay, tooltips/viewmodel/render helpers, Experiments, Harp, GFS, Sell, and
   Ghosts.
4. Return to mining M1, Gemstone tracking, and Powder Chest Tracker after the QoL
   batch. Their current checkpoint is preserved; the remaining material/area
   controls, tool swaps, and Gemstone Spread case are deliberately deferred.
5. Validate Session History 2.0 in a controlled session:

   - Current Session item rows and valuation remain unchanged when History is
     opened or copied.
   - Pause duration excludes paused time from active duration and survives
     restart.
   - Target and parent-area transitions produce closed archive segments.
   - Confirmed Start New writes exactly one v2 archive before opening the next
     display-numbered session.
   - Failure paths preserve or explicitly report Current Session and exact
     prior-History restoration outcomes.
   - Legacy schema v1 history remains readable after a v2 archive is added.

Add or extend a source only after it has a precise identity, credible provenance, dedupe strategy, and shadow-first runtime evidence. QoL automation stays on the Serveri until its own runtime confirmation pass is recorded.

## Compact phase history

- Material tracker baseline with persistent session and lifetime accounting.
- Searchable target selector.
- Gemstone domain model and per-gemstone ledgers.
- Signed Sack pipeline and source-aware rejection.
- Gemstone HUD and active-time accounting.
- Runtime tracker-family routing safety and positive controls.
- Accidental repository artifact cleanup.
- Maintained project-state, architecture, and testing guides.
- Mining session ledger domain model and shadow observer.
- Mining session engine integration with target mirroring and parity.
- Hard Stone OTHER_MINED shadow tracking and delayed-sack correlation fix (10 s window).
- Runtime validation: Gold parity match and Hard Stone OTHER_MINED credit.
- Session Analytics v1 and Session History v1 (`1.10.0+mc26.2`).
- Session History 2.0 canonical lifecycle freeze, archive-first Start New with
  compensating rollback and bounded process-crash recovery, and schema v1
  compatibility (`2.0.0+mc26.2`, runtime validation pending).
- Rot Tools rebrand: mod ID, package, commands, persistence filenames, artifact naming (`2.0.0+mc26.2`).
- Rot Tools visual identity: branded icon, `RotClientTheme` palette, dashboard and HUD chrome (`2.0.0+mc26.2`).
- Powder Chest Tracker Current Session projection and independent HUD.
- Bounded MOB loot Current Session ingest (generic + Diana in catalog scope).
- QoL dashboard: 131 wired modules across fourteen task-oriented groups, with
  automation-style development features disabled by default and scoped to the
  local Serveri. No separate server-detection
  branch is planned.
