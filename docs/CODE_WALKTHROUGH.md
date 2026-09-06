# Rot Client code walkthrough

This note is for sitting next to someone who has opened the latest Rot Client
JAR (or the matching sources JAR) in VS Code / IntelliJ. Read it top to bottom
once, then jump to whichever class they have open.

Rot Client is **not** a pile of independent mods glued together. It is one
client-only Fabric mod. Almost every class lives in the same Java package:
`fi.rotclient`. Mixins that hook Minecraft live in `fi.rotclient.mixin`.

Current checkpoint: **`2.0.1+mc26.2`**, Minecraft **26.2**, Java **25**,
**131** QoL parent modules. This is a public engineering checkpoint, not a
finished 2.0 release. Rot Client is not affiliated with Hypixel.

Automation (clickers, scanners, dungeon helpers, Free Camera, Terminal
Simulator) is **opt-in and off by default**. Those paths exist for a private
test server. They are not claimed as official-network features.

---

## 1. One-minute pitch

Rot Client does four jobs in one JAR:

| Job | What the player sees | Where it lives in code |
| --- | --- | --- |
| **Dashboard** | Right Shift / `/rot` Click GUI | `RotClientHomeScreen`, `QolUtilityDashboard` |
| **QoL catalog** | 131 modules in 14 groups | `QolUtilityCatalog` + `*Policy` + `*Runtime` |
| **Mining tracker** | Material / gemstone HUD and ledgers | `TrackerConfig`, detectors, `RotClientHud` |
| **Current Session** | Pause / resume / start new, History | `RotClientCurrentSession`, History store |

The mining tracker is the older, carefully proven core. The QoL catalog is the
newer bulk of the product. They share one config file, one theme, and one
dashboard. They do **not** share one ledger.

---

## 2. How to open the JAR so the code is readable

A Gradle build produces two artifacts in `build/libs/`:

| File | Use it for |
| --- | --- |
| `RotClient-2.0.1+mc26.2.jar` | Playing. This is compiled `.class` files plus resources. |
| `RotClient-2.0.1+mc26.2-sources.jar` | **Reading.** Same Java that was compiled, with comments. |

**Prefer the sources JAR for reading.** VS Code and IntelliJ open it as a zip
of `.java` files. Do not drop the sources JAR into `mods/`. Minecraft cannot
load it as a mod.

If they only have the playable JAR:

1. Rename a copy to `.zip` and extract it, or open it as a zip in the IDE.
2. The Java extension / a decompiler will show reconstructed Java from
   `fi/rotclient/*.class`. Names and structure match the sources JAR. Comments
   and original formatting will be missing.

Inside either JAR, start here:

```text
fabric.mod.json                 ← Fabric identity, entrypoint, mixins
rotclient.client.mixins.json    ← list of Minecraft hooks
rotclient.compat.mixins.json    ← optional hooks that depend on other mods
assets/rotclient/               ← icon, textures, font pack
data/rotclient/                 ← bundled SkyBlock / Kuudra snapshots
fi/rotclient/*.class            ← almost all product code
fi/rotclient/mixin/*.class      ← hooks into vanilla / Fabric
```

`fabric.mod.json` is the table of contents. It says:

- mod id `rotclient`
- client-only (`"environment": "client"`)
- entrypoint `fi.rotclient.RotClientClient`
- mixins listed above
- it **breaks** a simultaneous `miningtracker` install (the old name)

---

## 3. The rule that explains most of the code

A feature is almost always four files, not one:

```text
QolUtilityCatalog          what the dashboard lists (id, name, settings)
        ↓
QolUtilityConfig           persisted on/off + slider values (rotclient.json)
        ↓
SomethingPolicy            pure Java: "given this state, should we act?"
        ↓
SomethingRuntime           Minecraft: ticks, chat, containers, rendering
        ↓ (only if vanilla must be patched)
SomethingMixin             injects into a Minecraft class
```

**Policy classes have no Minecraft imports.** They are unit-tested. Runtime
classes talk to `Minecraft.getInstance()`, screens, packets, and mixins.

If they open a file named `*Policy.java`, they are looking at decisions.
If they open `*Runtime.java`, they are looking at the game bridge.
If they open `mixin/*Mixin.java`, they are looking at a surgical hook.

That split is why the project can have ~2,000 automated tests without launching
Minecraft for every slider.

Example they can click through right now:

1. `QolUtilityCatalog` — search for `"qol.auto_clicker"`. That is the dashboard
   card: label, group Combat, child settings (CPS, whitelist, terminator-only).
2. `QolUtilityConfig` — fields `autoClickerEnabled`, `autoClickerCps`, …
3. `AutoClickerPolicy` — CPS clamp, jitter, whitelist, dungeon-breaker guard.
4. `AutoClickerRuntime` — each client tick, pulse attack/use key mappings.
5. `RotClientClient.onInitializeClient()` — registers
   `ClientTickEvents` → `AutoClickerRuntime.tick(client)`.

Same pattern for Wardrobe (`WardrobeKeybindPolicy` / `WardrobeAutoEquipRuntime`),
Slayer (`SlayerPolicy` / `SlayerRuntime` / `SlayerSessionEngine`), Storage
(`StorageOverlayPolicy` / `StorageOverlayRuntime`), Kuudra Tools
(`IotaKuudraPolicy` / `IotaKuudraRuntime` plus `IotaRuntime` for the extras).

---

## 4. Boot sequence (what happens when Minecraft loads the mod)

Open `RotClientClient`. It implements Fabric's `ClientModInitializer`. Fabric
calls `onInitializeClient()` once.

In order, roughly:

1. **Legacy migrate** — if old `miningtracker.json` exists and `rotclient.json`
   does not, copy bytes. Old files are never deleted.
2. **Load config** — `TrackerStore.load()` → `TrackerConfig` (includes
   `QolUtilityConfig` nested inside the same JSON).
3. **Load Current Session** — `RotClientCurrentSession.loadFromDisk()`.
4. **Force tracker off for this launch** — `CONFIG.enabled = false`. Tracking
   is an explicit per-launch action. QoL toggles stay as saved.
5. **Start Bazaar price polling** — public Hypixel Bazaar only. No player data
   is uploaded.
6. **Register HUD layers** — mining tracker HUD, Powder Chest HUD, QoL overlay
   HUD (player/pet/performance).
7. **Register ticks, chat, tooltips, screens, commands** — this is the giant
   `onInitializeClient` method. Each `ClientBoundaryGuard.run("NAME", …)`
   wrapper exists so one crashing helper cannot take down the whole client.
8. **Register `/rot`** — dashboard, QoL, HUD editor, tracker, session, slayer,
   autoclicker whitelist.

If they get lost in `RotClientClient` (it is large), they are looking at the
**wiring board**, not the brains. The brains are the Policy / ledger classes.

Related UI classes they will hit next:

| Class | Role |
| --- | --- |
| `RotClientHomeScreen` | Overview: Modules / Look & HUD / Mining / Events |
| `QolUtilityDashboard` | Searchable 131-module catalog + settings drawer |
| `MiningUiScreen` | Mining tracker selector, enable, reset, HUD edit |
| `RotClientTheme` | Blue-slate palette. Do not hardcode random colors. |
| `RotClientModMenuIntegration` | Optional Mod Menu config button |

---

## 5. How the 131 modules actually exist

There is **one catalog**, not 131 independent mods.

`QolUtilityCatalog` is a static list of `ModuleDef` records. Each module has:

- a stable id (`qol.auto_clicker`, `qol.iota`, `qol.world_scanner`, …)
- a human name and description
- a `Group` (Combat, Slayer, Dungeons, Kuudra, Mining, …)
- child `SettingDef` rows (toggles, numbers, keybinds, colors, actions)

The dashboard does not invent modules. It **renders this catalog**. Search in
the Click GUI is search over these labels and aliases.

Persisted values live in `QolUtilityConfig` (and a few older flags still on
`TrackerConfig`, such as Auto Sprint / Fullbright, synced by the dashboard).
`QolSkyblockExtras` holds the larger extra-option blob so `TrackerConfig` does
not grow forever.

`QolModuleEvidence` is the honesty layer. A module can be wired and
unit-tested and still show **Needs testing** in the UI until a controlled
Minecraft pass is recorded. Automated tests never promote a module to Ready.

Cheat-tagged options stay off even after the parent module is enabled, until
the player opts into that child setting.

### Mixin list

`rotclient.client.mixins.json` is the inventory of vanilla hooks. Names are
usually `WhatItTouchesWhatItDoesMixin` (`HudActionBarFilterMixin`,
`MouseHandlerInventoryWalkMixin`, `BlockStateSecretHitboxMixin`).

`RotClientMixinPlugin` + `ModCompatibilityPolicy` decide whether a mixin that
targets **another mod** should apply. Dynamic FPS and similar optional mods
must not become hard dependencies.

If a feature needs to change Minecraft behavior that Fabric events do not
expose (cursor, camera, block outline, key input while a container is open),
there will be a mixin. If Fabric already has an event (client tick, chat,
tooltip, HUD registry), `RotClientClient` registers that event instead.

---

## 6. Mining tracker and Current Session (the other half)

Do not confuse these three stores:

```text
Live target ledgers          ← selected ore / gemstone counts (authoritative)
RotClientCurrentSession      ← generic item ledger (OTHERS, MOB, chests, …)
MiningSessionEngine          ← transient shadow / parity / diagnostics
                                (NOT a second quantity store)
```

### Live target path (selected material or gemstone)

1. Player picks a target in `MiningUiScreen` / `/rot target …`.
2. `TrackerSelection` is **exactly one family**: material **or** gemstone.
3. `MiningBreakDetector` / `GemstoneDirectBreakTracker` correlate an attacked
   block with the server removing it.
4. `MiningGainDetector` / gemstone Sack + `PRISTINE` parsers supply **quantity**.
   A break is context. Inventory / Sack / PRISTINE is the number.
5. `MaterialTrackerState` or `GemstoneTrackerState` stores session + lifetime.
6. `RotClientHud` renders the selected family. It does not own the numbers.

Gemstone credit is stricter than materials: a Rough Sack change must consume a
pending direct-break batch; a `PRISTINE` Flawed credit must not be credited
again when the Flawed Sack confirmation arrives. See `docs/ARCHITECTURE.md`
if they want the timing windows.

### Current Session path (everything else)

`RotClientCurrentSession` is the **one** durable generic ledger. HUD "OTHERS"
and Session Analytics only **read** it. They never keep a parallel total.

- Pause closes open target/area segments. Resume opens new ones.
- Start New **archives first** into Session History 2.0, then opens a fresh
  session. History is immutable. It is not a live ledger.
- Files on disk: `rotclient.json`, `rotclient-current-session.json`,
  `rotclient-session-history.json` in the Minecraft config folder.

Mining tracker / gemstone matrix work is currently **paused** as a product
phase. The code is still in the JAR. QoL and UI are the active track.

---

## 7. Data the JAR ships vs data it fetches

Bundled JSON under `data/rotclient/` (and related resources) is Rot Client's
canonical snapshot: items, mining mechanics, Kuudra overlay points, and so on.

At runtime, public Hypixel endpoints may refresh Bazaar (and, if Price
Tooltips is on, item / lowest-BIN quotes). That flow never uploads inventory,
chat, or session files.

Authority order, if they open `SkyBlockMiningResourceRegistry` or
`docs/skyblock-data.md`:

1. Official Hypixel data when Hypixel actually publishes the fact
2. Community references for gaps only
3. Live traces to prove what the client currently sees
4. Rot Client canonical dataset — what gameplay code reasons against

The mining catalog is **not** an allow-list. Unknown stable item ids with
credible gameplay evidence can still be observed.

---

## 8. How work actually happens (workflow)

This is the loop to describe out loud.

### Build and prove (without Minecraft)

```powershell
.\gradlew.bat test --rerun-tasks --console=plain
.\gradlew.bat compileClientJava --console=plain
git diff --check
.\gradlew.bat clean build --console=plain
```

- `test` — Policy / ledger / catalog contracts (~2,000 tests today).
- `compileClientJava` — Minecraft-facing sources actually compile.
- `clean build` — packages the playable JAR **and** the sources JAR.

A green build proves **packaging**. It does not prove the HUD looked right in
game.

### Install for play

Only `RotClient-2.0.1+mc26.2.jar` goes into the instance `mods/` folder. Keep
exactly one Rot Client JAR. Remove any leftover MiningTracker JAR. If the game
has the JAR open, wait until it is closed; do not kill Minecraft to copy.

### In game

Right Shift opens the dashboard. `/rot qol` is the module catalog. `/rot edit`
is the HUD layout editor. `/rot help` lists commands.

### Honesty labels used in this project

| Label | Meaning |
| --- | --- |
| **Wired** | Catalog entry + saved settings + runtime/mixin + automated tests |
| **Unit-tested** | Java tests passed. Not the same as "works on Hypixel." |
| **Ready for runtime test** | Implemented; waiting for a controlled Minecraft pass |
| **Runtime-tested** | Observed in Minecraft for the exact claim being made |
| **Paused** | Intentionally not the current product track |

When someone reads the dashboard and sees **Needs testing**, that is
intentional. Most of the 131 modules are wired and automated-tested. The
group-wide Minecraft matrix is still pending.

### Adding or changing a QoL module (the usual PR shape)

1. Add or edit the `ModuleDef` in `QolUtilityCatalog`.
2. Add fields on `QolUtilityConfig` / `QolSkyblockExtras`.
3. Put decisions in a `*Policy` class in the main source set.
4. Put Minecraft I/O in a `*Runtime` class in the client source set.
5. Add a mixin **only** if Fabric events are not enough.
6. Wire a tick/chat/render callback from `RotClientClient` if needed.
7. Add focused tests (catalog lock, numeric ranges, policy cases).
8. Build, install the playable JAR, then runtime-test before calling it done.

Numeric sliders must have an explicit `QolNumberSettings.spec`. Do not rely on
a generic fallback range.

---

## 9. Class index (open these while talking)

### Identity and boot

| File in the JAR | Why open it |
| --- | --- |
| `fabric.mod.json` | Mod id, version, entrypoint, mixin files |
| `RotClientClient` | Boot, events, `/rot`, HUD registration |
| `TrackerStore` | Load/save `rotclient.json` |
| `RotClientLegacyDataMigrator` | Old MiningTracker filename migration |

### QoL system

| File | Why |
| --- | --- |
| `QolUtilityCatalog` | The 131 modules and their settings |
| `QolUtilityConfig` | Persisted toggles |
| `QolSkyblockExtras` | Extra option blob |
| `QolModuleEvidence` | Ready vs needs-testing vs upcoming |
| `QolUtilityDashboard` | The in-game module UI |
| `QolOverlayHud` | Player / pet / performance overlays |

### Mining / session

| File | Why |
| --- | --- |
| `TrackerConfig` / `TrackerSelection` | Family split: material vs gemstone |
| `MiningBreakDetector` / `MiningGainDetector` | Material evidence |
| `GemstoneGainDetector` / `GemstoneLiveAccounting` | Gemstone evidence |
| `RotClientCurrentSession` | Canonical generic ledger |
| `MiningSessionEngine` | Transient shadow / parity only |
| `MiningSessionHistoryStore` | Immutable History 2.0 |
| `RotClientHud` / `PowderChestHud` | On-screen cards |

### Slayer / Kuudra (same ownership idea)

| File | Why |
| --- | --- |
| `SlayerSessionEngine` | One live Slayer state owner |
| `SlayerPolicy` / `SlayerMechanicsPolicy` / `SlayerCarryPolicy` | Pure rules |
| `SlayerRuntime` | Minecraft observers |
| `IotaKuudraPolicy` / `IotaKuudraRuntime` | Kuudra Tools (`qol.iota`) |

### Hooks

| File | Why |
| --- | --- |
| `rotclient.client.mixins.json` | Full mixin inventory |
| `RotClientMixinPlugin` | Optional other-mod mixins |
| `fi.rotclient.mixin.*` | Individual vanilla patches |

If they want the long form of mining correlation windows, persistence, and
SkyBlock data authority, that is `docs/ARCHITECTURE.md`. Module-by-module
catalog status is `docs/QOL_UTILITIES.md`. Current engineering snapshot is
`docs/PROJECT_STATE.md`.

---

## 10. What not to assume while reading

- **Decompiled playable JAR ≈ sources JAR for names**, not for comments.
- **`RotClientClient` is huge on purpose.** It is a composition root. Feature
  logic should be in Policy / Runtime / ledger classes.
- **HUD classes do not own session totals.** If a number is wrong, start at
  the ledger (`RotClientCurrentSession` or the selected tracker state), not
  the renderer.
- **A passing test suite is not a Hypixel proof.** Ask "wired, unit-tested, or
  runtime-tested?"
- **Do not install two Rot Client JARs**, and do not install MiningTracker
  next to Rot Client.
- **Do not copy `rotclient.json` or session files into git.** Those are
  per-instance player data.

That is the whole map. After this, pick one module they care about, search its
`qol.*` id in `QolUtilityCatalog`, and walk Catalog → Config → Policy →
Runtime → Mixin in that order.
