# Changelog

All notable changes to Rot Client (made by Rot Tools, formerly MiningTracker) are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project uses semantic versioning where practical.

## [Unreleased]

### Settings profiles

* Visuals → **Profiles** saves named client setups (modules, QoL, HUD layout,
  mining/powder tracker preferences, Fullbright / Always Night). Switch live
  without restarting. Tracker history, sessions, storage cache, and workspace
  stay global. Stored in `rotclient-profiles.json`.

### GUI

* Custom Scoreboard is a new **GUI** left-panel group. It rebuilds the SkyBlock
  sidebar with a Rot-native appearance list, number layouts, alignment, events,
  mayor/party/Maxwell rows, title/footer markup (`&&` colors), and a rounded
  panel. Vanilla sidebar hide is on by default; the module stays off until
  enabled. Catalog lock is **131**.

### Dungeons (Athen / Nebulune port)

* Port screenshot Dungeons modules into Rot-native policies and runtimes.
  Existing cards keep saved configs: Auto Superboom (click-triggered extra
  walls, `/rot superboom`), Auto/Queue Terms (ms delay, click order, 800 ms
  resync), Breaker instamine, chest-close delays, Item Quality style tokens,
  Party Finder lore tints, terminal waypoints with class/lever nodes, and
  solver overlay extras (first-click delay, drop/L/R binds, hide header,
  sounds). New cards: Dungeon Carry Tracker (`/rot dcarry`), Hover Terms,
  Party Finder Join Stats (SkyCrypt cache, not starred.foo), Soulsand
  Triggerbot, Terminal Click Trails, Watcher Helper. Cheat options stay off
  until opt-in. Catalog lock is **130**.

### License notices

* Ship the full SIL OFL-1.1 text with the bundled Source Sans 3 font.
  Root `NOTICE` and `THIRD_PARTY.md` list bundled works. Dark-pack
  `ATTRIBUTION.txt` files name Rot-authored art versus Minecraft EULA
  vanilla paths. Gameplay behavior is unchanged.

### Git workflow

* The public repository uses `development` for integration and `main` for
  stable, tested releases. Feature and fix branches merge into `development`.
  The playtest JAR publishes from `development`. See `docs/BRANCHING.md`.

### Display name

* Dashboard chrome shows **Rot Client** in accent red on the by-line only.
  Mod Menu author / By-line / credits use **Rot Tools**. Appearance and HUD
  Elements Editor sidebar clicks switch pages instead of sending the user
  back to Overview.

### Playtest JAR

* Default-branch CI publishes `RotClient-2.0.1+mc26.2.jar` to the
  [playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest)
  pre-release so testers can download the latest playable build without
  compiling. The same file is the `RotClient-playable` Actions artifact.

### Dashboard and HUD Elements Editor

* Sidebar Visuals holds Appearance and HUD Elements Editor. Those pages are
  not Modules → HUD & Display cards. Clicking around an open Visuals landing
  returns to Overview. The world editor shows only enabled overlays; right-click
  hides, left-click moves, scroll scales. Ctrl+Z restores the last hide.

### Storage Overlay

* Page cards no longer draw extra empty slot rows into the next Ender Chest /
  Backpack. Each card has its own header strip, then its slot grid.
* `/storage` records which Ender Chests and Backpacks are unlocked, and each
  opened page stores full item data (SkyBlock NBT and head textures) in
  `rotclient-storage-cache.json`. The first Storage open walks unlocked pages
  that have never been cached so cards remember item order. Later opens only
  reload pages you clicked last, not every chest again. Dashboard Reload
  Storage Pages still walks every unlocked page. The overlay reloads that
  cache when Storage opens, including after a game restart, so cards are not
  empty Steve heads. Disk writes are delayed off the click so opening a card
  does not hitch.
* Each Ender Chest / Backpack header has a Rot-themed `$` icon. Hovering it
  shows that page's bazaar instant-sell total.
* The search caret uses the same UI font as the query and stays inside the
  field when the text is long. Search-match glow is a full 2px purple-red
  frame around each slot, with the traveling light clipped to that slot.
* Close with the header `×`, or click outside the overlay and player
  inventory, to return to normal play.
* Search matches use a clockwise purple-to-red edge light around the item
  instead of a flat green box.
* Hovering the `$` icon shows that page's bazaar instant-sell total (all
  bazaar products, not just Slayer IDs). The sum is also drawn next to `$`
  on hover. Cache writes the full item stack and flushes when Minecraft
  stops, so Steve-head placeholders do not replace known items.
* Storage Overlay: a plain wheel always pages the overlay. Shift+wheel pans
  the item tooltip. Other inventories: wheel pans the tooltip vertically,
  Shift+wheel pans it sideways. Tooltip pan resets when the hovered item
  changes so the box stays next to the cursor. Ctrl+left-click pins Missing
  Enchants on that item.
* The overlay scrollbar thumb can be dragged, and clicking the track pages
  the list. Chrome, cards, search field, and slots now use the same
  `RotClientTheme` dashboard palette (black / red / violet) instead of the
  old teal panel.

### Inventory Overlay

* Equipment bars and the pet slot remember the last items seen in Stats /
  Pets, using the same local stack cache as Storage Overlay
  (`rotclient-inventory-chrome-cache.json`). The cache reloads on the title
  screen and again after the world exists, and an empty overlay does not
  overwrite a known loadout. Opening inventory after a restart no longer
  shows empty `+` slots until those menus are opened again.

### Custom Tooltip pan (26.2)

* Hover-box scrolling no longer wraps a queued-frame setter. Minecraft 26.2
  draws tooltips in `GuiGraphicsExtractor.tooltip` and pins them with
  `positionTooltip`; Rot Client now pans that already-clamped point. Wheel
  deltas come from `MouseHandler.onScroll`. Infinite Scroll wraps at the pan
  limit. This replaces the `@ModifyVariable` mixin that aborted client init.

### Roadmap

* Mining tracker, gemstone matrix, and related M1 work is paused. Next work is
  QoL/runtime (tooltips, HUD, dungeon/Slayer playtest). Mining resumes only
  when you reopen that track.

### Startup crash

* Custom Tooltip infinite scroll no longer injects `Screen.mouseScrolled`. That
  method is not declared on `Screen` in Minecraft 26.2, so mixin apply aborted
  during bootstrap (`InvalidInjectionException`). Scroll now uses Fabric's
  per-screen mouse-scroll gate, which also covers Storage Overlay paging.
* The follow-up launch crashed on `GuiGraphicsExtractorCustomTooltipMixin`:
  `@ModifyVariable` targeted every `setTooltipForNextFrame` overload, including
  ones without a `ClientTooltipPositioner`. The mixin now binds the Font/List/
  positioner/x/y/boolean overload only.

* Long HUD and enum lists clamp to the screen, mouse-wheel scroll, and show a
  search field from six options up. Clicking a module card background opens
  Settings. Dashboard page and open drawer survive close/restart; Plus still
  opens a clean Overview tab.
* Missing Enchants, Info, Price, and Custom Tooltip style are one Item Tooltips
  module. Player Display parses icon-after-number and `§` action bars. Inventory
  Walk is WASD-only. Item rarity paints the slot behind the item.
* Nested sidebar accordion scissors now pop the clip stack so the main
  pane is not left clipped to the sidebar (QoL module cards were blank).

### Dashboard motion and type

* Sidebar and settings-drawer sections ease open and closed instead of
  snapping, with a rotating chevron. Dashboard glyphs use a slightly larger
  Source Sans face plus a dark halo so letters stay clear on near-black panels.

### Dashboard theme and module cards

* Dashboard uses black, red, and violet instead of gold/teal. Module cards
  have a clear Module switch, a HUD switch (or HUD menu when a module has
  several overlays), and a Settings button. Wheel scrolling eases instead of
  jumping a full step per tick.

### Inventory overlay crash

* Storage overlay and inventory-button drawing are contained so a corrupt or
  replaced JAR during play cannot take down the whole client. The 2026-08-23
  crash was `ZipException: invalid LOC header` while loading
  `StorageOverlayRuntime$CachedPage` after a JAR overwrite while Minecraft was
  still running — not Shift-drag logic. Nested overlay types now load with the
  parent class. Close the game before installing a new JAR.

### 26.2 mixin launch crash

* Inventory chrome no longer injects inherited `extractBackground` on
  `AbstractContainerScreen` (26.2 declares it on `Screen` / `ContainerScreen`).
  Legacy SkyBlock textures inject `PatchedDataComponentMap.get` (a class);
  `ItemStack.get` is not declared and `DataComponentHolder` is an interface.
  Diana particle hook uses `handleParticleEvent` (not Yarn `handleParticles`).
  Sound mute waits until `SoundInstance.resolve` so menu music cannot NPE on
  `getPitch()`.
  Free Camera no longer shadows `KeyboardInput.moveVector` (the field lives on
  `ClientInput`); that APPLY failure aborted Hypixel and Serveri joins.
  Outbound privacy no longer calls `getCurrentServer()` during status ping.

### Dashboard and HUD type

* Rot Client screens and HUD overlays draw Source Sans 3 instead of the
  vanilla pixel face. Sidebar, cards, and settings rows have more space and
  sentence-case labels.

### Playable 120-module QoL checkpoint (local alpha)

* Catalog is locked at **120** parent modules across **ten** groups (Foraging
  added). Ring macros sit on `qol.command_keybinds`. Iota `!profit`
  uses Rot's own Bazaar/BIN valuation (sell-order, Mage key mats, 20% essence pet,
  salvage armor) with per-item lore fallback; kismet/wheel rerolls use live
  quotes. No websocket flipper is included. Automated tests **1,861**; Minecraft
  runtime matrix still pending. Local-clone automation only.

### Dungeon cheat-list catalog extensions

* Door Highlight titles, Spirit Bear ESP, Breaker skip-secrets, Breaker charges
  HUD, Close Chest, and Camera Clip/custom distance now sit on existing Dungeon
  ESP, F7, Menus, and Camera parents. Queue Terms is intentionally not
  implemented (kept out as an unsafe cheat). Auto Superboom is a cheat
  toggle default off. Local-clone only.

Rot Client is the continuation of MiningTracker under a new product
identity. Version `2.0.0+mc26.2` is developed publicly at
[rot-tools/Rot-Client](https://github.com/rot-tools/Rot-Client).

## [2.0.1+mc26.2] - 2026-08-21

### Slayer hologram and ability wiring

* Slayer hologram scans now cover the real Hypixel nametag box, Dagger Swap
  uses attunement tags on the attacked entity with a timed use
  pulse, and Vengeance Timer starts from the `ASHEN ♨7` nametag.

### Storage Overlay vanilla chest texture

* Hypixel Storage is a `ContainerScreen`. That class draws the vanilla chest
  texture in its own `extractBackground` override, so cancelling `Screen`'s
  background never hid it. The overlay now cancels that chest blit and draws
  itself at the start of `extractContents`, before vanilla slots are skipped.

### Storage Overlay mixin crash

* Removed the invalid `extractBackground` inject from
  `AbstractContainerScreen`. That method is not declared there; chest GUIs
  override it on `ContainerScreen`.

### Auto Clicker combat pulse

* Restored Auto Clicker pulses that release attack/use after each
  click. Keeping the mapping held while LMB was down let vanilla `missTime`
  swallow extra hits, so combat felt slower than the configured CPS. Block
  mining still uses the separate hold-attack path.

### Slayer QoL and runtime polish checkpoint

* Added movable Slayer Progress, RNG Meter, and live Bazaar Item Profit HUDs.
  Unpriced drops are shown separately and never counted as coins.
* Added target-line rendering that reads the shared Slayer engine, plus
  threshold/repeat-safe boss-spawn progress warnings.
* Hardened Auto Clicker block breaking and CPS feedback, chat presentation,
  Etherwarp visuals, HUD editing, and wardrobe/inventory interaction behavior.
* Verified with the full automated suite and a Java 25 playable build.

### Complete Slayer and inventory tooling checkpoint — 2026-08-21

* Expanded the dashboard to 72 wired modules with Storage Overlay and editable
  Inventory Buttons, including server-observed storage-page caching, real
  selector navigation, scroll/layout controls, command-button anchors, and
  Simple/All Warps presets.
* Completed the requested shared Slayer suite with configurable carry pricing,
  persistent completed-carry history, a validated opt-in Discord webhook,
  RNG Meter probability projection, and a per-family/per-drop filter editor.
* Kept all Slayer, storage, and command-button modules opt-in and disabled by
  default. Webhook URLs and carry records remain local and are excluded from
  diagnostics and source control.
* Preserved one shared Slayer engine: HUDs, Carry Manager, history, filters, and
  alerts remain projections or controlled mutations of the same runtime state.
* Added policy/catalog/config tests for storage, inventory buttons, carry
  parsing, webhook validation, RNG formulas, all reviewed Slayer drop ids, and
  settings persistence. The checkpoint passes 1,605 automated tests; controlled
  Minecraft validation remains pending.

### Public QoL checkpoint — 2026-08-20

* Expanded the task-oriented QoL dashboard from 27 to 55 wired modules across
  Utilities, Render, HUD & Display, Interface, Combat, Dungeons, Mining, Slayer,
  and Fishing.
* Added the policy/runtime implementations and mixin wiring for the current
  development batch, including Experiments, Wardrobe, Harp, GFS, Sell, Ghosts,
  tooltips, viewmodel, item/render helpers, UI fixes, and explicit numeric
  setting ranges.
* Kept command/click/movement automation opt-in, disabled by default, and scoped
  to the Serveri. Runtime code follows
  the same Hypixel protocol path without a separate server-detection branch.
* Added catalog-wide contracts for unique module/setting ids, implemented
  dashboard actions, toggle/reset persistence, and every visible setting type.
* Reworked Cheater Wardrobe into Wardrobe Swapper with nine explicit keyboard or
  mouse binds for slots 1–9, stationary-only activation by default, movement
  cancellation before the hidden click, and the real `/wd` Armor Sets slot
  mapping (container slots 36–44).
* Fixed hidden Wardrobe auto-equip to honor Custom, Hotbar, and Simple binding
  styles instead of silently falling back to the number row.
* Removed inert `Show Settings` / `Overflow Mana` action rows and removed the
  unsafe generic 0-100 number-control fallback; every slider now requires a
  reviewed explicit range.
* Refreshed the README and public engineering documentation so wired,
  automated-tested, runtime-verified, and planned behavior are visibly
  separated.
* Prevented Price Tooltips from polling Hypixel item/Bazaar and the public
  lowest-BIN endpoints while the module is disabled.
* Added precise ignore rules for local editor state and accidental root-level
  Fabric JAR extraction artifacts.
* Added a fail-closed canonical target boundary so Current Session rejects
  cross-selection material and gemstone credits even if an upstream runtime
  caller is misrouted. Added the full 23-selection acceptance/rejection matrix
  and raw inventory/Sack reconciliation coverage for every material target.
* Verified the checkpoint with 1,568 automated tests, zero failures, zero
  errors, and zero skipped tests before the final clean-build pass.

### Slayer foundation — 2026-08-21

* Expanded the dashboard to 61 wired modules with Slayer Display, Slayer
  Stats, Slayer Highlights, Miniboss Alert, Slayer Drops Data, and Slayer Carry
  Tracker.
* Added one shared Slayer session engine for quest lifecycle, the six boss
  families, owner/tier classification, minibosses, Inferno demons and
  attunements, exactly-once death accounting, rare-drop observations, and
  per-player carry progress.
* Added movable read-only Slayer, Slayer Stats, and Slayer Carry HUDs plus a
  carry manager operating on the same live carry list.
* Added optional first-observation miniboss chat/title alerts, configurable
  boss/miniboss/demon highlights, matching carry spawn messages, and party
  progress messages.
* Kept all new Slayer modules disabled by default. Automated tests and client
  compilation pass; controlled Minecraft validation and later Slayer
  automation slices remain pending.

### Slayer mechanics slice — 2026-08-21

* Expanded the dashboard to 64 wired modules with Cocoon Alert, Dagger Swap,
  and Enderman Laser Hider; all remain disabled by default.
* Added exact Cocoon chat recognition, a configurable local title/sound, and a
  movable six-second countdown HUD.
* Added delayed Inferno dagger selection and `td_attune_mode` switching for the
  reviewed ASHEN/AURIC fire family and SPIRIT/CRYSTAL maw family, including the
  current upgraded dagger ids.
* Added other-player Voidgloom Guardian-laser suppression with explicit own-boss
  and carry filtering over the shared Slayer engine.
* Added pure policy tests, catalog/config persistence contracts, client wiring,
  and Java 25 client compilation. Controlled Minecraft validation remains
  pending.

### Expanded Slayer mechanics and tooltip parity — 2026-08-21

* Expanded the dashboard to 70 wired modules with Attunement Display, Auto
  Soulcry, Slayer Sounds, Vengeance Timer, Vengeance Damage Tracker, and Big
  Slayer Drops; all remain disabled by default.
* Added the reviewed Soulcry behavior: the three Voidgloom
  katanas, 200 mana or 100 with Ultimate Wise, tick/attack detection, optional
  hitbox and mana checks, other-boss attack support, and a bounded random delay.
* Added owned-Inferno attunement/count projection, the six-second Vengeance
  timer, minimum-500,000 Vengeance damage reporting, Voidgloom sound filtering,
  and family-level known-drop scaling around an owned boss death.
* Reworked Missing Enchantments to use real NBT enchant
  levels, upgrade and conflict handling, and insertion into the actual item
  tooltip instead of a second floating panel.
* Verified the working tree with 1,594 automated tests, zero failures, zero
  errors, and zero skipped tests. Controlled Minecraft validation remains
  pending.

### Added

* Added a searchable QoL dashboard with 72 wired modules across nine
  task-oriented groups. Wired means catalogued, persisted, runtime-bridged, and
  automated-tested; the complete Minecraft validation matrix remains pending.
* Added Serveri Auto Clicker, Inventory Walk, Trajectories, Secret
  Hitboxes, and World Scanner. These are not intended for live Hypixel
  production play. Auto Clicker whitelist items are managed with
  `/rotclient autoclicker add|remove|list`.
* Added Powder Chest Tracker as a read-only Current Session projection of
  `CHEST` loot and `CURRENCY` powder, with an independent movable HUD.
* Added bounded MOB loot Current Session ingest (player-caused melee and
  projectile, Combat/Slayer/Dungeon Sack, action-bar `+N`, rare-drop chat,
  and kill-window coins). Displayed Magic Find is recorded as session
  context only.
* Added outbound handshake scrubbing on multiplayer so servers do not receive
  the `rotclient` plugin-channel id and the client brand is sent as `vanilla`.
* Added a bounded local Start New transaction marker that recovers recognized
  process interruptions before archive, between archive and Current Session
  publication, and after publication. It stores no ledger rows or quantities.
* Added archive-first Current Session Start New coordinator with full History
  document rollback when publishing the next Current Session fails (including
  retention-cap restoration).
* Added Target and Pristine Current Session handoff pipelines so accepted target
  and non-target Pristine gains credit the canonical ledger exactly once while
  ACTIVE.
* Added GitHub Actions CI (`build.yml`) for Java 25 `test` + `clean build` and
  playable JAR artifact upload on the public `rot-tools/Rot-Client` repository.
* Added mod ID `rotclient`, package `fi.rotclient`, and entrypoint classes
  `RotClientClient` and `RotClientHud`.
* Added persistence filenames `rotclient.json`, `rotclient-session-history.json`,
  and `rotclient-diagnostic-*.log`.
* Added one-time byte-copy migration from legacy `miningtracker.json` and
  `miningtracker-session-history.json` when the new files do not already exist.
* Added playable artifact `RotClient-2.0.0+mc26.2.jar`.
* Added Rot Client's high-contrast slate/teal visual identity via
  `RotClientTheme`: layered blue-slate surfaces, a restrained teal interaction
  accent, near-white primary text, cool-gray secondary text, and semantic
  success/warning/error colors.
* Added a confirmed Discord community link to the Rot Client home screen.
* Added branded mod icon at `assets/rotclient/icon.png`.
* Applied the shared theme across the dashboard (`MiningUiScreen`), HUD
  (`RotClientHud`), toggles, charts, and module panels.
* Added a durable canonical Current Session ledger with restart persistence,
  Pause/Resume/Start New lifecycle controls, source-aware item rows, target and
  area segments that close on Pause and reopen on Resume, and read-only
  HUD/Analytics projections.
* Added persistent OTHERS accounting for confidently correlated non-target
  mining observations without replacing authoritative target ledgers.
* Added Session History 2.0 immutable Current Session freezes with active and
  paused durations, closed target/area segments, canonical item rows, frozen
  valuation metadata, bounded same-directory replacement with atomic move where
  supported, and schema v1 read compatibility.
* Added a mechanics registry keyed to all 25 official Mining Collection
  families. Missing mechanics, canonical identities, and aliases remain
  explicitly unresolved rather than guessed; reviewed identity boundaries,
  exact-name area matching, context gates, and the shared 2,475-position mining
  break scan are separate verified capabilities.
* Added local workspace and appearance persistence for the redesigned Client
  UI without storing live accounting in either UI model.

### Changed

* Simplified Client UI navigation: Appearance now lives only in the Settings
  sidebar, HUD Layout has its own direct editor entry, and QoL pages use the
  full content area without a duplicate appearance card.
* Camera now uses Minecraft's configured Toggle Perspective key to cycle only
  between first person and rear third person; front-facing third person is
  skipped.
* Hardened Current Session recovery: future/corrupt schema loads fail closed to
  PAUSED, Resume is blocked until safe, write failures use ~30s autosave
  backoff, and corrupt backups are not repeatedly overwritten.
* Expanded Session History 2.0 codec limits for long sessions (~100k segments /
  ~50k item rows) with a shared 64 MiB file-size guard on read and write.
* Unified Analytics/status/copy lifecycle with canonical Current Session state,
  including empty RUNNING/PAUSED sessions (no fabricated item counts).
* Aligned Pause/Resume target and area segment boundaries with active duration;
  unclean shutdown closes activity at the last reliable heartbeat.
* Updated current project-facing repository links to `rot-tools/Rot-Client`
  while preserving historical OgRudolf attribution where factual.
* Restored accidentally emptied `PRIVACY.md` from the committed policy text.
* Narrowed client crash-containment boundaries (`ClientBoundaryGuard`) so
  external/runtime failures skip safely without swallowing `Error`.
* Renamed the product from MiningTracker to Rot Client across mod metadata,
  documentation, and user-facing strings.
* Refreshed dashboard and HUD chrome to the canonical Rot Client palette;
  semantic green/yellow remain status-only, not brand colors.
* Changed the primary command namespace from `/miningtracker` to `/rotclient`.
* Changed the Fabric mod ID from `miningtracker` to `rotclient`.
* Changed the Java package from `fi.miningtracker` to `fi.rotclient`.
* Updated public documentation for public-development status, local-only
  operation, QoL Serveri scope, and the absence of telemetry or cloud sync.
* Made Current Session the single durable generic ledger. The transient mining
  engine retains classification, correlation, deduplication, parity, and
  diagnostic responsibilities only.
* Changed confirmed Start New to archive successfully before opening the next
  Current Session; a failed archive leaves the original session unchanged.
* Changed Session History presentation and copy operations to remain read-only
  over immutable stored records.

### Fixed

* Kept live Analytics and archived History entry counts aligned when the same
  canonical item has separate area-attributed rows.
* Cleared stale world area state before offline Current Session resume so a
  reconnect cannot create a phantom segment for the previous world.
* Hardened Bazaar price normalization against null, non-positive, non-finite,
  excessive, and high-scale values while preserving valid peer products.
* Fixed the Resume-time high-scale Bazaar valuation crash and added narrow
  client-boundary containment that does not erase Current Session quantities.
* Hardened target-switch and pause/resume boundaries so delayed observations
  cannot silently become a second or wrong-target ledger credit.

### Validation status

* The complete 23-selection target matrix is automated-test validated: 11
  material targets and all 12 gemstones cover UI and command availability,
  exact target routing, wrong-target rejection, OTHERS target-family
  protection, reset isolation, and persistence round-trips. Ordinary and Pure
  Ore block forms share one material ledger; Tungsten and Umber are separate.
* Selected targets are no longer gated by the detected scoreboard area, while
  ambiguous background OTHERS block evidence remains area-gated. Coal, Iron,
  Gold, Lapis, Redstone, Emerald, Diamond, and Quartz now recognize both their
  ordinary ore and Pure Ore block forms.
* Canonical Current Session mining accounting, restart/pause/resume behavior,
  and the Resume/Bazaar crash correction have controlled runtime evidence.
* Session History 2.0 lifecycle, freeze, archive-first ordering, compensating
  rollback outcomes, segments, item rows, valuation snapshots, and schema v1
  compatibility are automated-test validated and still await the post-audit
  controlled Minecraft runtime test.

### Known limitations

* Start New updates separate History and Current Session files rather than one
  storage transaction. Recognized process-crash boundaries are journaled and
  recovered; an unreadable marker fails closed for manual review. This does not
  claim filesystem or power-loss atomicity across both files.

### Deprecated

* `/miningtracker`, `/MiningTracker`, and `/miningui` remain as temporary legacy
  aliases for `2.0.0+mc26.2` and show a deprecation notice directing users to
  `/rotclient`.

### Removed

* Removed support for installing MiningTracker and Rot Client JARs side by side.
  The mod IDs differ; keep only one Rot Client JAR in `.minecraft/mods/`.

### Migration

* On first launch, existing `miningtracker.json` is copied to `rotclient.json`
  when `rotclient.json` does not exist. The legacy file is not deleted or
  modified.
* On first launch, existing `miningtracker-session-history.json` is copied to
  `rotclient-session-history.json` when the new history file does not exist.
  The legacy file is not deleted or modified.
* Remove any installed MiningTracker JAR before installing
  `RotClient-2.0.0+mc26.2.jar`.

## [1.10.0+mc26.2] - 2026-08-05

### Added

* Added searchable selection for all supported gemstone trackers: Ruby, Amber,
  Sapphire, Jade, Amethyst, Topaz, Jasper, Opal, Onyx, Aquamarine, Citrine,
  and Peridot.
* Added isolated gemstone session and lifetime state with tier quantities,
  total items, Rough Equivalent, block averages, and active time.
* Added direct-break batch correlation for positive matching Rough Gemstone
  Sack changes.
* Added exact `PRISTINE` Flawed Gemstone credits with Sack confirmation
  deduplication.
* Added gemstone session HUD views for running and paused states.
* Added engineering guides for project state, architecture, testing, and
  controlled runtime validation.
* Added diagnostic mining-session engine (`MiningSessionEngine`) with ephemeral
  shadow ledger, target mirroring, and parity comparison.
* Added `/miningtracker shadow status` for ephemeral shadow ledger totals and
  target parity while diagnostics are active.
* Added shadow `OTHER_MINED` classification for catalogued off-target
  resources, including Hard Stone (not a selectable live target).
* Added Powder Chest `CHEST_LOOT` and `CURRENCY` shadow credits with atomic
  mixed item/currency batches and replay dedupe.
* Added immutable derived Bazaar instant-sell (gross) session valuation for
  supported shadow item entries; currency remains excluded from resolved value.
* Added experimental Session Analytics dashboard module with Start, Stop,
  Reset, and Copy Summary controls.
* Added `/miningtracker session start|stop|reset|status|copy|save` user-facing
  aliases that share one analytics controller with the dashboard.
* Added privacy-safe Session Analytics summary formatting for status and
  clipboard copy.
* Added experimental Session History for explicitly saved STOPPED analytics
  snapshots in a dedicated local `miningtracker-session-history.json` file,
  bounded to 20 newest records and isolated from `TrackerStore`.
* Added Session History dashboard module with Save Session, Open, Copy,
  Delete, and confirmed Clear History controls.
* Added `/miningtracker history list|open|copy|delete|clear` commands.
* Added release-candidate hardening for atomic writes, fail-closed history
  decoding, clear-token consumption, diagnostic/analytics isolation, help
  text, and Excluded currency display.
* Added 10-second material direct-break correlation window with
  newest-context-first selection for delayed Mining Sack delivery.
* Added automated regression coverage for shadow ledger correlation,
  deduplication, valuation, Session Analytics projection behavior, and Session
  History persistence safety.

### Changed

* Changed gemstone Sack handling to use signed changes and reject non-positive,
  wrong-family, stale, duplicate, or otherwise uncorrelated observations.
* Added a 60-second auto-pause window to gemstone active-time and hourly-rate
  calculations.
* Expanded structured diagnostic markers for direct breaks, Sack correlation,
  `PRISTINE`, signal consumption, and live ledger mutations.
* Refreshed public documentation, privacy guidance, handoff notes, and project
  metadata to distinguish current, runtime-tested, planned, and unsupported
  behavior.
* Updated mod metadata and Bazaar User-Agent for version `1.10.0+mc26.2`.

### Fixed

* Prevented material Sack observations from crediting a selected gemstone and
  prevented one gemstone from crediting another gemstone's ledger.
* Prevented material-only Fortune commands and detection from mutating state
  while a gemstone is selected.
* Added tested HUD height and screen-clamping behavior for dynamic gemstone
  metrics.
* Removed two accidental tracked repository artifacts without rewriting
  history.
* Hardened Session History clear confirmation so mismatched tokens cannot be
  reused.
* Hardened diagnostic recording so it cannot silently wipe an active Session
  Analytics session.

### Planned

* Perform controlled Hypixel runtime validation for the expanded ordinary/Pure Ore, Tungsten, and Umber target matrix.
* Expand runtime validation of Session Analytics and Session History across
  materials and gemstones.
* Integrate shadow categories into live HUD or persistence only after runtime
  evidence approves each source path.
* Add gemstone value and profit only after pricing and source attribution have
  been validated safely.

## [1.9.1+mc26.2] - 2026-07-30

### Added

* Added Titanium as an independent tracked material.
* Added the combined `MITHRIL + TITANIUM` tracker target.
* Added Titanium raw and Enchanted Bazaar product support:

  * `TITANIUM_ORE`
  * `ENCHANTED_TITANIUM`
* Added Titanium recognition for Polished Diorite.
* Added Titanium base drop accounting with a base yield of 2.
* Added separate Titanium session quantities, active time, Bazaar estimates,
  and persistent ledger data.
* Added `/miningtracker target titanium`.
* Added Titanium coverage to material, tracking-target, Bazaar, and
  configuration-migration tests.
* Added migration support for legacy saved target IDs.

### Changed

* Replaced the incorrect active `MITHRIL + TUNGSTEN` target with
  `MITHRIL + TITANIUM`.
* Updated the dashboard material selector to show:

  * `GOLD`
  * `DIAMOND`
  * `MITHRIL + TITANIUM`
  * `TUNGSTEN (WIP)`
* Updated the combined tracker status to display Mithril and Titanium
  separately.
* Updated the HUD labels from Mithril + Tungsten to Mithril + Titanium.
* Increased the MiningTracker dashboard width to fit the updated material
  selector.
* Updated `/miningtracker target mithril` to select the combined
  Mithril + Titanium target.
* Updated the configuration data version to version 7.
* Updated documentation and mod metadata for version `1.9.1+mc26.2`.

### Fixed

* Fixed Titanium being represented incorrectly as Tungsten in the active
  combined mining tracker.
* Fixed Mithril and Titanium not having separate persistent session ledgers.
* Fixed the selected-target migration for old Mithril, Titanium, Tungsten, and
  `MITHRIL_TUNGSTEN` target values.
* Fixed a migration regression that temporarily removed the older legacy Gold
  configuration migration.
* Fixed the combined active-time calculation so that it uses Mithril and
  Titanium instead of Mithril and Tungsten.
* Fixed duplicate material-option spacing configuration in the dashboard.
* Fixed compilation errors caused by accidental manual paste artifacts during
  development.

### Migration

Saved configurations from older versions are migrated automatically.

The following old selected-target values are mapped to the current combined
Mithril + Titanium target:

```text
MITHRIL_TUNGSTEN
MITHRIL
TITANIUM
TUNGSTEN
```

Migration does not rename, remove, merge, or copy existing Tungsten ledger
values into Titanium.

```text
Existing Mithril ledger  → preserved
Existing Tungsten ledger → preserved
Missing Titanium ledger  → created empty
```

Gold and Diamond ledgers remain unchanged.

### Tungsten status

Tungsten data and implementation scaffolding remain in the project for future
development.

Tungsten currently:

* retains its independent material profile
* retains its item and Bazaar product definitions
* retains its block-recognition data
* retains its persistent ledger data
* appears in the tracker selector as `TUNGSTEN (WIP)`
* cannot be selected as an active tracker target
* does not replace or share the Titanium ledger

Clicking the Tungsten WIP option does not change the active tracking target.

### Validation

* All 56 automated tests passed.
* The Gradle build completed successfully.
* Mithril and Titanium tracking were tested in Minecraft.
* Mithril and Titanium updated their own separate counters.
* Tungsten remained disabled as intended.
* Existing Gold, Diamond, Mithril, and Tungsten behavior and data were
  preserved.

## [1.9.0+mc26.2]

### Added

* Added Mithril material tracking support.
* Added the initial Tungsten material profile and persistent ledger.
* Added Mithril and Tungsten Bazaar product support.
* Added Dwarven Metal Fortune handling for Dwarven mining materials.
* Added material-specific block appearances and base-yield data.
* Added separate per-material ledgers for Gold, Diamond, Mithril, and
  Tungsten.

### Changed

* Expanded MiningTracker from Gold and Diamond tracking to additional mining
  materials.
* Added a combined Mithril-related tracker option.

### Known issue

* The active combined tracker incorrectly paired Mithril with Tungsten.
* Titanium was not yet represented as its own material.
* This was corrected in version `1.9.1+mc26.2`.

## [1.8.0+mc26.2]

### Added

* Added separate Gold and Diamond tracker targets.
* Added independent material session ledgers.
* Added persistent per-material counters and active-session data.
* Added material-target selection to the MiningTracker dashboard.

### Changed

* Migrated the old single-material configuration into the Gold ledger.
* Created a separate Diamond ledger during configuration migration.
* Made Gold the safe fallback when a saved target is absent or unknown.

## Version compatibility

| Version | Minecraft version |
| --------------------- | ----------------- |
| `2.0.0+mc26.2` (Rot Client) | Minecraft 26.2 |
| `1.10.0+mc26.2` (MiningTracker) | Minecraft 26.2 |
| `1.9.1+mc26.2` (MiningTracker) | Minecraft 26.2 |
| `1.9.0+mc26.2` (MiningTracker) | Minecraft 26.2 |
| `1.8.0+mc26.2` (MiningTracker) | Minecraft 26.2 |

Rot Client and MiningTracker builds made for a different Minecraft version should not be
installed in the same instance unless they are explicitly documented as
compatible.
