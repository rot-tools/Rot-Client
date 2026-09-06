# Rot Client QoL Utilities

This document is the current public catalog for the QoL dashboard. It separates
implementation state from runtime evidence.

## Evidence model

- **Wired** means the module is registered in `QolUtilityCatalog`, persists its
  settings through `QolUtilityConfig` / `QolSkyblockExtras`, has a Minecraft
  runtime bridge or mixin, and is covered by automated contracts.
- **Runtime verified** requires a controlled Minecraft result for the exact
  module/options being claimed.
- **Pending runtime** means the implementation exists but the complete
  interactive matrix has not yet been accepted.

All 131 current catalog entries are wired and automated-tested. The group-wide
runtime matrix is still pending, so this table intentionally does not claim
that every option is release-ready.

## Catalog

| Group | Modules | Current evidence |
| --- | --- | --- |
| Combat | Auto Clicker; Hide Players; Trajectories; Etherwarp; Auto Dojo; Mob Highlight | Wired · automated tested · runtime matrix pending |
| Events | Diana Burrows; Diana Mobs; Diana Profit; Diana Share | Wired · automated tested · runtime matrix pending |
| Slayer | Slayer Display; Slayer Stats; Slayer Highlights; Miniboss Alert; Slayer Drops Data; Slayer Carry Tracker; Cocoon Alert; Dagger Swap; Enderman Laser Hider; Attunement Display; Auto Soulcry; Slayer Sounds; Vengeance Timer; Vengeance Damage Tracker; Big Slayer Drops; Disconnect Fix; family extras | Wired · automated tested · runtime matrix pending |
| Dungeons | Secret Hitboxes; Dungeon HUD/Map; Dungeon ESP; Dungeon Announce; Leap; Terminals; Term Sim; Requeue; Puzzles; F7 Boss; Dungeon Menus (Party Finder lore tints + SkyCrypt stats, chest prize reel); Dungeon Carry Tracker; Hover Terms; Party Finder Join Stats; Soulsand Triggerbot; Terminal Click Trails; Watcher Helper; Auto GFS; Auto Sell | Wired · automated tested · runtime matrix pending |
| Kuudra | Kuudra Tools | Wired · automated tested · runtime matrix pending |
| Mining | Mining Tracker; Powder Chest Tracker; Mining Session; Mining History; World Scanner; Commission Display; Scatha Alerts; Mining Events; Glacite Mineshaft; Mining Helpers; Heart of the Mountain | Wired · automated tested · runtime matrix pending |
| Fishing | Fishing Helper; Sea Creatures; Fishing Hotspots; Trophy Fishing; Fishing Visuals; Fishing Tools | Wired · automated tested · runtime matrix pending |
| Foraging | Foraging Trees; Foraging Audio; Foraging Helpers; Foraging Cheats | Wired · automated tested · runtime matrix pending |
| Garden | Farm Keys | Wired · automated tested · runtime matrix pending |
| GUI | Custom Scoreboard | Wired · automated tested · runtime matrix pending |
| HUD & Display | Player Display; Performance HUD; Pet HUD; Hide Own Name; Item Tooltips; Skill Levels; Custom Cursor. Appearance, HUD Elements Editor, and Profiles are Visuals-only, not cards on this page. | Wired · automated tested · runtime matrix pending |
| Render | Fullbright and Night; Render Optimizer; Player Size; Item Rarity Background; Viewmodel; Item Scale; Eye Height Fix; Instant Sneak; Ghosts; Camera; Free Camera; Legacy SkyBlock Textures; Dark SkyBlock Pack | Wired · automated tested · runtime matrix pending |
| Interface | Inventory Overlay; Storage Overlay; Daily Reward Claim; Inventory Buttons; Slot Binds; Item Count Fix; Active Pet Highlight; Anvil Helper; Calendar Date; Experiments Solver; Auto Experiments; Auto Harp; No Cursor Reset; Click GUI | Wired · automated tested · runtime matrix pending |
| Utilities | Hotkey Macros; Wardrobe Keybinds; Loadout Keybinds; Pet Keybinds; Wardrobe Swapper; Chat Commands (including overlay-only Vanguard prize reel); Auto Conversation; Auto Sprint; Inventory Walk; Waypoints; Animation Fix; Double Use Fix; Escrow Fix; Market Guard | Wired · automated tested · runtime matrix pending |

## Fishing suite

Six Fishing parents cover bite detect, sea creatures, hotspots, trophy, visuals, and tools. Bite detect is a nearby `!!!` hologram, not vanilla splash. Automation stays on the Serveri; party ping and auto-attack default off.

Correctness gates: hook timers only match SkyBlock `N` / `N.N` values up to 20s, hook nametags hide only next to your bobber, sea-creature tracking requires a health hologram marker, and auto-attack only fires on tracked live ids.

Leftovers wired onto the same parents: volcano geyser box, sulphur sponge box, Thunder spark skulls, hotspot despawn title, Magmafish fillet tooltip (rarity defaults 1/2/5/10), Banshee/Reindrake mute, bait-change title. Not included: lava-to-water chunk rebuild, bobber-in-lava mixin, profit/session trackers, Odger GUI, wormhole pathfind, Legion/Bobbin overlays.

World Scanner worm-lava spots stay on Mining (`qol.world_scanner.worm`).

## Dungeons (Athen / Nebulune port)

Existing Dungeons parents keep saved configs. Superboom is click-triggered with extra walls via `/rot superboom`. Auto Terms uses millisecond delays and First/Random/Closest/Furthest order. Queue Terms drops stale clicks after 800 ms. Breaker instamine, chest-close delays, and Item Quality `#cur/#max/#floor` stay on the existing cards.

New cards: Dungeon Carry Tracker (`/rot dcarry`), Hover Terms, Party Finder Join Stats (SkyCrypt `sky.shiiyu.moe`, not starred.foo), Soulsand Triggerbot, Terminal Click Trails, Watcher Helper. Cheat-tagged options stay off until opt-in. Terminal Simulator stays local `/rot termsim`.

## Daily Reward Claim

`qol.reward_claim` (Interface / Inventory, default **off**) opens Hypixel daily-reward choices inside Rot Client when chat contains a `rewards.hypixel.net/claim-reward/` link. The module fetches the claim page, shows three Rot-themed cards, and POSTs the selected option. Hide Chat Link, Keep In Client, and Wait For Ad default on once the parent is enabled: the vanilla website-confirm screen is blocked while a claim loads, and if Hypixel marks the page as not skippable Claim waits the posted ad duration instead of opening the ad. Automated-tested; Minecraft playtest pending.

## Kuudra Tools and Market Guard

`qol.iota` — **Kuudra Tools** (Kuudra group, parent default **off**). Provides Kuudra 3D waypoints and hitboxes, Fresh Tools and build HUDs, phase titles, party join/limbo alerts, `!` party commands (including `!t1`–`!t5`), an arrow tracker HUD, terminator/fishing-cast mutes, a fishing-hook fix, and optional toggle-left/right latch clicks. Bundled arena data uses Rot's own `rot.kuudra.*.v2` overlay schema (`pile_locations.json`, `pearl_waypoints.json`, `etherwarp_config.json`) drives the supply-crate, pile, pearl, build, stun-pod, ichor, hitbox, double-pearl, and etherwarp overlays. Child waypoint toggles default on but stay gated by the parent. Supply-progress timers read the title and subtitle. SkyBlock gating uses the shared scoreboard detector. `!chests` / `!runs` increment on `KUUDRA DOWN!` / `DEFEAT`; opening a Free/Paid Chest (slot 31 → `PAID/FREE CHEST REWARDS`) decrements the chest counter; slot 50/51 rerolls subtract live Kismet Feather / Wheel of Fate quotes (lore cost as fallback). `!profit` values a run against Rot's own Bazaar/BIN quotes (sell-order then lowest BIN, Mage key mats, 20% essence pet bonus, salvage to essence, lore-coin fallback). No websocket flipper is included. Run duration is persisted so the hourly rate survives a restart, and the session resets after 21 idle minutes.

`qol.stall_market` — **Market Guard** (Utilities / Market, parent default off). Local GUI only: Bazaar search (`/rot bazaarsearch` and a hovered-item bind), sell protection, angry co-op protection, a BIN overlay, and auction highlights versus the latest lowest BIN. Hovered-item search reads the hovered slot through an accessor mixin. No websocket flipper is included.

## Hotkey Macros

`qol.command_keybinds` keeps the eight SkyBlock menu binds (`/pets`, `/storage`, `/armor`, `/equipment`, `/loadout`, `/stats`, `/warp dungeon_hub`, `/potionbag`) and runs backward-compatible macro logic on the same parent (catalog remains **131**). Custom macros are one line each: `KEY[+LIMIT] | message[,,message] | SEND/TYPE/EDIT/CYCLE/RANDOM/REPEAT | ASSERT/SUBMIT/VETO/AVOID | HOLD/VANILLA/RELEASE`. Placeholders (`%pos%`, `%x+3%`, `%clipboard%`, `%#regex%`, …), a 4-per-20-tick default rate limit, and a 256-character SEND cap are supported. SkyBlock presets stay SkyBlock-gated; custom macros fire in any world while no GUI is focused. The visual sequence editor supports list/add/edit/delete, key capture, ordered steps, delays, rate-limit selection, and safe save/cancel. EDIT opens chat and selects `%edit%`. Automated-tested; Minecraft playtest pending.

## Protocol and test scope

Modules that click, send commands, change movement input, or expand development
interaction/scanner behavior are opt-in and disabled by default. They are built
for a Serveri
play. Runtime code intentionally uses one Hypixel-protocol-compatible path; it
does not branch on a separate Serveri identity or server allowlist.

The runtime logic matches Hypixel-style GUI titles, scoreboard text, item
`ExtraAttributes` ids, chat messages, and commands so the Serveri exercises
the same protocol contracts. This does not make local automation acceptable on
the official network.

## Fullbright and Night

`qol.fullbright` stays one Render / Lighting card (catalog still **131**). Child
rows are Fullbright, Always Night, and Force both on (square latch, accent red
when armed). The two modes are exclusive unless Force both is on. Releasing
force while both are on keeps Fullbright. Flags persist in `rotclient.json`
and are **not** SkyBlock-gated: Hypixel lobby, SkyBlock, hub, and instance
switches keep the same saved state.

Always Night is client-only sky and lightmap override (no `/time` packets).
Turning it on in an already-loaded world plays a vanilla-style dusk once, then
parks the moon. A new world while it is already on snaps to that parked night
and does not replay dusk. Fullbright is an instant lightmap. End and Nether
skyboxes are skipped. Automated-tested; Minecraft playtest pending.

## Wardrobe Swapper

Wardrobe Swapper is opt-in and disabled by default. It exposes one explicit
keyboard or mouse bind for each Wardrobe slot from 1 through 9. A matching press
uses the actual `/wd` command, consumes only the expected `Armor Sets` container,
and maps the selection to Hypixel container slots 36–44. Lime-dye equipped and
gray-dye empty markers remain part of the click policy.

`Require Stationary` is enabled by default. The hidden flow does not start while
a movement input is held or horizontal motion remains, and movement before the
click cancels the swap and closes the hidden container. Custom, Hotbar, and
Simple bindings all resolve through the same policy. These rules are
automated-tested; controlled Minecraft validation is still pending.

## Slayer foundation

The first Slayer slice has one shared in-memory engine. Entity/chat observers
mutate it, while Slayer Display, Slayer Stats, Slayer Carry Display, and the
Carry Manager only read or intentionally edit that same state. There is no
parallel Slayer statistics ledger hidden behind a HUD.

The classifier covers Revenant Horror, Tarantula Broodfather, Sven Packmaster,
Voidgloom Seraph, Inferno Demonlord, and Riftstalker Bloodfiend; their reviewed
miniboss names; Inferno Quazii/Typhoeus demons; owner/tier tags; and Inferno
attunement labels. Quest completion is an authoritative fallback for a missed
owned-boss death event, and entity ids prevent double death accounting.

Current foundation behavior includes movable HUDs, session kill statistics,
rare-drop chat observations, configurable highlights, nearby miniboss alerts,
and live per-player carry progress with an open manager. The second slice adds
an exact `YOU COCOONED YOUR SLAYER BOSS` trigger with a six-second movable HUD,
configurable local title/sound, Inferno Dagger Swap using `td_attune_mode` and
the reviewed fire/maw dagger families, and Guardian-laser hiding anchored only
to other-player Voidgloom bosses. Carry bosses are included only when the
explicit option is enabled. The next slice adds a reviewed Attunement Display;
tick- and attack-based Auto Soulcry with the
three Voidgloom katanas and 200/100 mana rule; Voidgloom sound suppression;
Vengeance Timer and minimum-500,000 damage reporting; and temporary scaling of
known drops near an owned boss death. These paths are automated-tested but not
yet runtime accepted. The shared runtime additionally provides RNG Meter chance
projection from selected drop/stored XP/Magic Find, configurable Voidgloom and
Inferno carry-price matching, persistent completed-carry history, an opt-in
Discord webhook, and a per-family/per-drop Big Drops filter editor. These
features extend the same engine and do not create a parallel Slayer ledger.

## Storage, inventory buttons, and enchantment tooltips

Storage Overlay replaces supported Storage menus with a compact
overview. It caches only pages observed from genuine server-provided Storage,
Ender Chest, or Backpack containers, skipping the control row, and navigates
through the recorded overview selector or the real `/enderchest` / `/backpack`
commands. Opening Storage walks unlocked pages that have never been cached so cards
are not blank after a first look; later opens only reload Ender Chests and
Backpacks you actually clicked. A dashboard Reload Storage Pages action still
walks every unlocked page. Selected pages show live stacks; other cards keep
the last cached preview without treating loading placeholders as empty. Observed
pages persist in `rotclient-storage-cache.json`, including skull textures when
the item codec drops PROFILE on reload.

Inventory Overlay paints necklace, cloak, belt, and gloves plus the chosen pet
on survival inventory. Those last-seen stacks persist in
`rotclient-inventory-chrome-cache.json` with the same item encoding as Storage
Overlay, so the bars are not empty after logging back into SkyBlock.

Inventory Buttons renders user-configured command buttons around supported
containers. The editor changes command, icon, size, anchors, and offsets and
offers Simple and All Warps presets. A click sends the configured command
through the normal client connection; no inventory contents are fabricated.

Missing Enchantments reads the item's actual enchantment components and appends
missing, upgradable, or conflicting entries to the normal tooltip while its
configured key is held. It never changes the item or sends a command.

## Runtime validation order

1. Dashboard search, per-module switches, settings drawers, reset behavior, and
   persistence across restart.
2. UI window move/resize/snap, inventory equipment overlay, and HUD positioning.
3. Passive render/UI modules: Item Rarity, Custom/Info/Price Tooltips, Viewmodel,
   Item Scale, No Render controls, Camera, and Ghosts.
4. Serveri workflows: Experiments, Wardrobe, Harp, GFS, Sell, command
   keybinds, and input helpers.
5. Cross-module regression pass: simultaneous passive HUD/render modules,
   persistence across restart, and no hidden activation while a switch is off.
6. Slayer foundation and mechanics: all six families, own/other owner tags, tier detection,
   death deduplication, miniboss alerts, highlights, carry manager progress,
   party announcement opt-in, world-change cleanup, Cocoon countdown/title/sound,
   all four Inferno attunements, hotbar dagger selection/NBT switching, and
   own/other/carry Voidgloom laser filtering, tick/attack Soulcry with mana and
   hitbox gates, attunement count, Voidgloom sounds, Vengeance timing/damage,
   persistent carry completion/history, configurable carry prices, safe opt-in
   webhook delivery, RNG Meter chance projection, per-drop filters, and
   family-level Big Drops scaling.
7. Storage/Inventory matrix: observed-page caching, active selector navigation,
   scrolling, button editor persistence, anchors, presets, command execution,
   and disabled-state isolation.

Every number setting must have an explicit `QolNumberSettings.spec`. Unknown
number ids have no generic 0-100 fallback and therefore cannot silently render
with a misleading slider range.

Automated catalog contracts additionally require globally unique module and
setting ids, a working read/write contract for every visible setting, working
toggle/reset persistence for every internally owned module, and implemented
behavior for every visible action row.
