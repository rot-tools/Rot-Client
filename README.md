<p align="center">
  <img src="src/main/resources/assets/rotclient/icon.png" alt="Rot Client icon" width="128">
</p>

# Rot Client

[![Build](https://github.com/rot-tools/Rot-Client/actions/workflows/build.yml/badge.svg)](https://github.com/rot-tools/Rot-Client/actions/workflows/build.yml)

Rot Client is a modular, client-side Fabric mod for Minecraft `26.2`, built for
Hypixel SkyBlock. It brings mining tracking, session analytics, HUDs, item
information, visual helpers, dungeon tools, and a large quality-of-life catalog
into one understandable client.

Rot Client is an independent community project. It is **not** affiliated with,
endorsed by, or approved by Hypixel.

> **Status:** `2.0.1+mc26.2` is a public engineering checkpoint, **not a finished
> release**. Features are labelled by evidence so implemented code is not
> confused with completed in-game validation. Automation utilities (clickers,
> scanners, dungeon helpers, Free Camera, Terminal Simulator) are **opt-in and
> off by default**.

---

## Feature overview

### Mining tracker

- Searchable material and gemstone selector with isolated per-target state.
- Coal, Iron, Gold, Lapis, Redstone, Emerald, Diamond, and Quartz combine their
  ordinary and Pure Ore forms into one selected ledger.
- Mithril and Titanium are tracked together under one selection; Tungsten and
  Umber have their own trackers with reviewed block-form routing.
- Twelve gemstones (Ruby, Amber, Sapphire, Jade, Amethyst, Topaz, Jasper, Opal,
  Onyx, Aquamarine, Citrine, Peridot) each keep an isolated ledger with tier
  quantities and a Rough Equivalent.
- Signed Mining Sack reconciliation, inventory evidence, Compact handling,
  Pristine correlation, target-family isolation, and bounded deduplication.
- Mining Spread for eligible block removals, plus a separate conservative
  Gemstone Spread path.
- Movable, scalable, screen-clamped HUDs with active time, hourly rates,
  quantities, averages, and optional Bazaar value estimates.

Accepted totals are evidence-based estimates, not authoritative server
accounting. See [Tracking Model](docs/TRACKING.md) for source and rejection
rules.

### Current Session and History

- One durable live ledger with Pause, Resume, Start New, target/area segments,
  canonical item rows, source classification, and valuation state.
- Session History 2.0 keeps immutable local freezes, retains the 20 newest
  records, and reports failed recovery instead of hiding it.
- Mining HUD, Powder Chest HUD, and Session Analytics are read-only projections
  of that ledger.

### Powder Chest and reward tracking

- Independent Powder Chest dashboard and HUD for opened chests, chest rewards,
  Mithril / Gemstone Powder, and hourly rates.
- Bounded MOB accounting: player-attributed melee/projectile kills, Combat /
  Slayer / Dungeon Sack delivery, selected action-bar drops, and coins.

### Quality-of-life catalog

The dashboard groups **119** wired parent modules by task. "Wired" means a
catalog entry, persisted settings, a runtime bridge, and automated contracts.
The full module-by-module table is in [QoL Utilities](docs/QOL_UTILITIES.md).

| Group | Highlights |
| --- | --- |
| Utilities | Keybinds, Wardrobe Swapper, Chat Commands, Auto Sprint, Inventory Walk, macros, Market Guard |
| Render | Fullbright, render optimizer, player size, item rarity, viewmodel, camera, Free Camera, legacy textures, dark SkyBlock pack |
| HUD & Display | Player display, performance HUD, pet HUD, hide own name, item tooltips, skill levels |
| Interface | Click GUI, inventory & storage overlays, daily reward claim, inventory buttons, missing enchants, HUD layout editor, custom cursor |
| Combat | Auto Clicker, hide players, trajectories, etherwarp helper, Diana helper, mob highlight |
| Dungeons | HUD & map, ESP, secret hitboxes, terminals, Terminal Simulator, F7 helpers, puzzles, leap tints |
| Mining | World Scanner, commissions, Scatha, Glacite, HOTM, mining helpers |
| Slayer | Shared Slayer engine HUDs, carry manager, cocoon alert, dagger swap, laser/sound hiders, auto soulcry, vengeance tracker, big-drop filter |
| Fishing | Fishing helper, sea creatures, hotspots, trophy, visuals, tools |
| Foraging | Tree helpers, audio cues, foraging helpers |

Selected details:

- **Wardrobe Swapper** binds Wardrobe slots 1–9 to keys or mouse buttons, opens
  the real `/wd` Armor Sets flow invisibly, and only swaps while stationary.
- **Shared Slayer engine** drives quest lifecycle, the six boss families,
  owner/tier detection, minibosses, attunements, statistics, rare-drop
  observations, highlights, alerts, and carry progress. Its HUDs are read-only
  views. An optional carry summary can be sent to a user-provided Discord
  webhook (opt-in, HTTPS webhook endpoints only).
- **Storage Overlay** rebuilds supported Storage menus from pages the server has
  actually shown; **Inventory Buttons** adds an editable command-button layout;
  **Missing Enchantments** augments real tooltips with missing/upgradable
  enchantment info.
- **Market Guard** adds Bazaar search, sell protection, angry co-op auction
  protection, and a BIN overlay/highlight — local GUI only.

### Dungeons and Terminal Simulator

Dungeon modules stay disabled until you enable the parent. Cheat-tagged options
(auto terminals, auto Simon, auto I4, auto ultimate, auto debuff) stay off even
then unless you opt in.

- Dungeon HUD: score, secrets, class, blessings, F7 timers, quiz/Melody, and a
  scanned Magical Map (rooms, doors, checkmarks, player markers).
- ESP for starred mobs, bats, keys, mimic, withers, crystals, secret tags, and
  shipped room-core secret waypoints.
- I Hate Doors: client-side stained-glass rewrite of wither/blood/entrance doors.
- F7 helpers: terminals overlay, Terminal Simulator hub, Simon, Arrow Align, I4,
  gates, leap class tints and counter, and puzzle solvers (Quiz, Weirdos, Blaze,
  Ice Fill, Water Board, Boulder, TP Maze).
- Reward reels: local overlay for Croesus-style reward chests and Vanguard chat
  loot. Overlay only — it never buys the chest.

### Free Camera

A detached fly camera that never moves the real player.

---

## Requirements

- Minecraft `26.2`
- Java `25`
- Fabric Loader `0.19.3` or newer
- Fabric API `0.155.2+26.2` or newer
- Mod Menu (optional)

## Installation for development testing

1. Install Fabric Loader and Fabric API for Minecraft 26.2.
2. Build Rot Client with the included Gradle wrapper (`.\gradlew.bat build`).
3. Copy only `RotClient-2.0.1+mc26.2.jar` from `build/libs/` into the instance
   `mods/` folder. Do not install the sources JAR.
4. Keep exactly one Rot Client JAR installed. Remove any legacy MiningTracker
   JAR — the two mod IDs must not run together.

Rot Client includes bounded migration for supported legacy MiningTracker
configuration data. Back up the instance before upgrading, and never copy
runtime configuration or session files into the repository.

## Commands

The command root is `/rot`.

| Command family | Purpose |
| --- | --- |
| `/rot`, `/rot ui`, `/rot help` | Open the dashboard UI and help |
| `/rot toggle`, `reset`, `status`, `edit` | Control the selected mining tracker and HUD |
| `/rot target <material>` | Select a supported material target |
| `/rot session ...` | Pause / resume / reset / status / copy / save Current Session |
| `/rot history ...` | List / open / copy / delete / clear immutable history records |
| `/rot slayer status\|stats reset` | Inspect or reset the shared Slayer statistics |
| `/rot slayer carry ...` | Open and manage the per-player Slayer carry list |
| `/rot record start\|stop` | Start or stop optional local diagnostics |
| `/rot autoclicker ...` | Manage the Auto Clicker item whitelist |
| `/rot fortune auto\|<mining> [material]` | Control material Fortune input |
| `/rot debug tracking ...` | Engineering-only tracking trace controls |

Gemstones are selected from the searchable UI. The legacy aliases
`/miningtracker`, `/MiningTracker`, and `/miningui` show a deprecation notice.

## Privacy and networking

Rot Client has no telemetry, cloud sync, remote analytics, or account-token
requirement. Configuration, Current Session, History, UI state, and optional
diagnostics stay in the local Minecraft instance.

Material valuation can request public Hypixel Bazaar data. With Price Tooltips
enabled, Rot Client also requests public Hypixel item/Bazaar data and a public
lowest-BIN snapshot; those requests stay off while the module is disabled. The
optional Slayer carry webhook sends only the configured carry summary to the
user-provided Discord webhook and is disabled by default.

Read [Privacy](PRIVACY.md) before sharing diagnostics or launcher logs.

## Building and testing

Use Java 25 and the included wrapper:

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
.\gradlew.bat compileClientJava --console=plain
git diff --check
.\gradlew.bat clean build --console=plain
```

The current baseline passes **1,966** automated tests with zero failures,
errors, or skips across 285 suites. The playable artifact is written to
`build/libs/RotClient-2.0.1+mc26.2.jar`. A clean build proves packaging, not
in-game correctness.

## Project status

The active development branch is `qol-utilities`. Recommended order of work:

1. Playtest the dungeon HUD/map/ESP, Terminal Simulator, F7 helpers, puzzles,
   I Hate Doors, secret waypoints, leap, and Free Camera. Leave cheat toggles
   off unless you are explicitly testing them.
2. Runtime-test the smaller QoL checkpoint: Wardrobe Swapper, the shared Slayer
   suite, Storage Overlay, Inventory Buttons, and Missing Enchantments.
3. Return to Mining milestone M1, gemstone tracking, and Powder Chest Tracker.
4. Complete the canonical area engine and optional Location HUD.
5. Runtime-validate Session History 2.0 end to end.

Public engineering state is summarized in [Project State](docs/PROJECT_STATE.md).

## Links

- [Source](https://github.com/rot-tools/Rot-Client)
- [Issues](https://github.com/rot-tools/Rot-Client/issues)
- [Discord community](https://discord.gg/8UpMfvZugq)
- [Changelog](CHANGELOG.md)
- [Contributing](CONTRIBUTING.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Testing](docs/TESTING.md)

## Credits

Rot Client is created and maintained by **OgRudolf**.

## License

Rot Client is licensed under the [MIT License](LICENSE).
