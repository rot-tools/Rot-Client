<p align="center">
  <img src="src/main/resources/assets/rotclient/icon.png" alt="Rot Client" width="128" height="128">
</p>

<h1 align="center">Rot Client</h1>

<p align="center">
  made by <b>Rot Tools</b><br>
  <b>A client-side Fabric companion for Hypixel SkyBlock</b><br>
  One dashboard for quality-of-life modules, Visuals, mining, and sessions.<br>
  Built for Minecraft <code>26.2</code>.
</p>

<p align="center">
  <a href="https://github.com/rot-tools/Rot-Client/actions/workflows/build.yml"><img src="https://github.com/rot-tools/Rot-Client/actions/workflows/build.yml/badge.svg" alt="Build"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-a855f7.svg" alt="MIT License"></a>
  <img src="https://img.shields.io/badge/Minecraft-26.2-2ea44f?logo=minecraft&logoColor=white" alt="Minecraft 26.2">
  <img src="https://img.shields.io/badge/Java-25-f89820?logo=openjdk&logoColor=white" alt="Java 25">
  <img src="https://img.shields.io/badge/Fabric-0.19.3-dbd0b4" alt="Fabric 0.19.3">
  <a href="https://discord.gg/8UpMfvZugq"><img src="https://img.shields.io/badge/Discord-community-5865F2?logo=discord&logoColor=white" alt="Discord"></a>
</p>

<p align="center">
  <a href="#install">Install</a> ·
  <a href="#playtest-jar">Playtest JAR</a> ·
  <a href="#in-game">Using the client</a> ·
  <a href="#features">Features</a> ·
  <a href="#commands">Commands</a> ·
  <a href="#privacy">Privacy</a> ·
  <a href="https://discord.gg/8UpMfvZugq">Discord</a>
</p>

Rot Client is an independent community project. It is **not** affiliated with,
endorsed by, or approved by Hypixel.

> **`2.0.1+mc26.2`** is a public checkpoint, not a finished 2.0 release.
> Automation (clickers, scanners, dungeon helpers, Free Camera, Terminal
> Simulator) is **opt-in and off by default**.

## Install

**Requires** Minecraft `26.2`, Java `25`, [Fabric Loader](https://fabricmc.net/use/) `0.19.3` or newer, and [Fabric API](https://modrinth.com/mod/fabric-api) `0.155.2+26.2`. [Mod Menu](https://modrinth.com/mod/modmenu) is optional.

### Playtest JAR

Testers do not need to build. Each green push to `development` replaces
the [latest playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest)
pre-release:

1. Open [Latest playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest).
2. Download `RotClient-2.0.1+mc26.2.jar` only. Skip `-sources.jar`.
3. Put that file in the instance `mods/` folder. Keep exactly one Rot Tools JAR.
   Remove any legacy MiningTracker JAR.

The same JAR is also attached as artifact `RotClient-playable` on the matching
green [Build](https://github.com/rot-tools/Rot-Client/actions/workflows/build.yml)
run if you need a specific commit.

### Build from source

1. Install Fabric Loader and Fabric API for Minecraft 26.2.
2. Build with the included wrapper: `.\gradlew.bat build`
3. Copy only `RotClient-2.0.1+mc26.2.jar` from `build/libs/` into `mods/`.

Rot Tools includes bounded migration for supported legacy MiningTracker
configuration data. Back up the instance before upgrading. Never copy runtime
configuration or session files into this repository.

## In game

| Action | What it does |
| --- | --- |
| **Right Shift** | Opens the dashboard (Click GUI key, rebindable) |
| **Sidebar** | Overview, Visuals (Appearance and HUD Elements Editor), then Modules |
| **Overview** | Session / tracker / powder chips, then Go-to cards: Modules, Visuals, Mining, Events |
| `/rot` or `/rot ui` | Same dashboard |
| `/rot qol` | QoL module catalog |
| `/rot edit` | HUD Elements Editor (world overlay placement) |
| `/rot help` | Command list |

Search the dashboard address bar to jump to a module. Most utilities stay off until you enable them.

## Features

**124** quality-of-life modules ship in one catalog, grouped by task. Wired means a catalog entry, saved settings, a runtime bridge, and automated contracts. The module-by-module table lives in [QoL Utilities](docs/QOL_UTILITIES.md).

| Area | What you get |
| --- | --- |
| **Interface** | Click GUI, inventory and storage overlays, inventory buttons, SkyBlock menus |
| **Utilities** | Hotkey macros, wardrobe swapper, chat commands, auto sprint, inventory walk, market guard |
| **HUD & display** | Player, pet, performance overlays, custom cursor, tooltip extras. Appearance and HUD Elements Editor live under Visuals |
| **Render** | Fullbright, viewmodel, player size, camera, Free Camera, legacy textures |
| **Combat** | Auto clicker, trajectories, etherwarp helper, mob highlight |
| **Events** | Diana burrows, rare mob ESP, drop HUD, and share helpers |
| **Dungeons** | HUD and map, ESP, secret hitboxes, terminals, puzzles, F7 helpers, reward reels |
| **Kuudra** | Waypoints, Fresh Tools, party commands, fight HUDs |
| **Slayer** | Shared boss engine, HUDs, carry manager, alerts, and drop helpers |
| **Mining QoL** | World scanner, commissions, Scatha, Glacite, HOTM helpers |
| **Fishing, foraging & garden** | Bite helpers, sea creatures, trophy, tree HUD, farm keys |

Dungeon cheat-tagged options (auto terminals, auto Simon, auto I4, and similar) stay off even after you enable the parent module, until you opt in separately.

### Mining tracker and sessions

A searchable material and gemstone selector with isolated per-target state, movable HUDs, and optional Bazaar estimates. Coal through Quartz combine ordinary and Pure Ore forms. Mithril and Titanium share a ledger; Tungsten and Umber are independent. Twelve gemstones keep their own tier quantities.

Current Session is the one live ledger (pause, resume, start new). Session History 2.0 keeps immutable local freezes. Powder Chest and bounded MOB accounting sit beside that ledger, not inside a second quantity store.

Accepted totals are evidence-based estimates, not server accounting. See [Tracking Model](docs/TRACKING.md).

## Commands

The command root is `/rot`. Legacy aliases `/rotclient`, `/miningtracker`, `/MiningTracker`, and `/miningui` print a deprecation notice.

| Command | Purpose |
| --- | --- |
| `/rot`, `/rot ui`, `/rot qol`, `/rot help` | Dashboard, QoL catalog, help |
| `/rot edit` | HUD Elements Editor |
| `/rot toggle`, `reset`, `status` | Selected mining tracker |
| `/rot target <material>` | Select a supported material |
| `/rot session ...` | Pause, resume, reset, copy, or save Current Session |
| `/rot history ...` | List, open, copy, or delete history records |
| `/rot slayer ...` | Slayer status, stats, and carry list |
| `/rot autoclicker ...` | Auto Clicker item whitelist |
| `/rot fortune auto\|<mining> [material]` | Material Fortune input |

Gemstones are selected from the searchable UI.

## Privacy

No telemetry, cloud sync, remote analytics, or account-token requirement. Configuration, sessions, history, and optional diagnostics stay in the local Minecraft instance.

Material valuation can request public Hypixel Bazaar data. Price Tooltips, when enabled, also request public item and lowest-BIN snapshots. An optional Slayer carry webhook sends only the configured summary to a URL you provide, and is off by default.

Read [Privacy](PRIVACY.md) before sharing diagnostics or launcher logs.

## Development

Java 25 and the included Gradle wrapper:

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
.\gradlew.bat compileClientJava --console=plain
git diff --check
.\gradlew.bat clean build --console=plain
```

A green build proves packaging. It does not prove in-game correctness. See [Contributing](CONTRIBUTING.md), [Branching](docs/BRANCHING.md), and [Testing](docs/TESTING.md).

## Docs

- [Changelog](CHANGELOG.md)
- [Branching](docs/BRANCHING.md) — `main`, `development`, and feature branches
- [Code walkthrough](docs/CODE_WALKTHROUGH.md) — how the JAR is organized, Policy/Runtime/Mixin, and the build-to-play loop
- [Project state](docs/PROJECT_STATE.md)
- [Architecture](docs/ARCHITECTURE.md)
- [QoL utilities](docs/QOL_UTILITIES.md)
- [Branding](docs/BRANDING.md)
- [Issues](https://github.com/rot-tools/Rot-Client/issues)

## Credits

Rot Client is created and owned by **Rot Tools**.

## License

[MIT](LICENSE)
