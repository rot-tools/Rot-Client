# AirJump and AutoTool for Minecraft 26.3

Version `2.0.12+mc26.3` extends the [previous eight utility ports](AUTISM_PORT_2026-10-16.md)
with AirJump and completes the deferred AutoTool inventory/delay controls.
Lite retains 84 catalog parents, MIT licensing and no AUTISM implementation.
Plus has 145 parents and remains GPL-3.0-only. Search `autism` in Plus.

AirJump has JumpFreely, DoubleJump and GhostBlock modes. Repeated jumps retain
the normal ten-tick jump delay. DoubleJump permits one airborne jump per landing.
GhostBlock supports downward movement at the block-height plane beneath the
local player while Jump is held, with the upstream speed-dependent 300–900 ms
release grace. The 26.3 adapter modifies the final local collision result instead
of changing blocks or individual vanilla/Lithium collision sweepers. Other players,
world data, upward motion and horizontal collision results are preserved.
GUI, world/player changes, death, riding, fluids, flying and Free Camera clear or
suspend the behavior. Server correction is possible and requires runtime testing.

AutoTool's new **Consider Inventory** and **Vary Delays** controls default off,
preserving existing users' hotbar-only fixed-delay settings. Inventory mode scans
at most 36 slots when a mining target becomes eligible. It resolves menu slots
by player-inventory identity, uses one normal number-key SWAP with an empty
cursor, and permits at most one such swap per continuous mining session.
The tool remains in the hotbar; **Switch Back restores selected-slot position
only**. Manual slot changes/container actions, other active equip controllers,
menus, market automation and observed item/server corrections cancel ownership.
There is no blind retry or automatic inverse swap. Vanilla local click prediction
is not described as a server acknowledgement. Optional delay variation uses the
upstream distribution, bounded to 1000 ms and sampled once per action.

Neither feature writes mining quantities, changes History schemas, scans dungeon
rooms or imports upstream login/account, LAN/MQTT or multi-client services.
Source pin, reviewed file hashes and GPL attribution are in
[SOURCE.json](../licenses/plus/SOURCE.json). The official source commit is
[`cb6910b`](https://github.com/AutismDevelopment/Autism-Client/tree/cb6910b424f8b101b918c822ab66e865c58567cb).
The installed Lithium 0.26.1 movement mixin replaces the inner collision solver,
not `Entity.collide`; its bytecode was inspected. This is compatibility evidence,
not an applied-mixin or Minecraft runtime test.

## Validation

Policy tests cover jump consumption/rearming, mode and clock resets, grace
boundaries, negative-height collision clipping, inventory bounds/tie preferences,
durability/Silk Touch fallback, swap budgets/cancellation, delay bounds and config
migration. Descriptor tests check actual 26.3 methods/fields. Both built JARs
are checked for license and edition leakage. See PROJECT_STATE for full results.

## Manual playtest

1. Launch Lite alone. Recheck dashboard, inventory, mining and sessions. No
   AUTISM modules should appear. Then launch Plus alone and search `autism`.
2. Save/reset AirJump's mode and bind. Test each mode over normal blocks,
   slabs, edges and empty sections with Lithium enabled, then without Lithium.
   DoubleJump must stop after one airborne jump and rearm after landing.
3. Release Jump, disable the module, open a GUI, change worlds, die, ride,
   enter fluids, fly and use Free Camera. No support/jump state should remain.
   Check server corrections and other-player movement on the controlled server.
4. Test AutoTool with inventory mode off, then on. Cover Silk Touch, low durability,
   hotbar ties, empty/full inventory, varied delay, and one-swap-per-mining-session.
   Confirm Switch Back restores selection only and leaves the moved tool in place.
5. Manually scroll/change slots, open/click a menu, interrupt with wardrobe/pet/
   equipment/market actions, and provoke a rejected swap. It must stop without
   retrying, moving cursor contents or attempting an inverse swap.
6. Recheck the previous eight ports and dungeon/market workflows. Measure frame
   times before making performance claims. Gameplay validation remains pending.

The [inventory](reports/AUTISM_CLIENT_INVENTORY.md) now records 9 adapted module
surfaces, 13 existing Rot bases awaiting parity review and 54 other unported
modules. Macro/command systems and Lite public-release checks remain open.
