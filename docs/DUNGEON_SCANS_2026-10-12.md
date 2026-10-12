# Dungeon scan and puzzle checkpoint — 2026-10-12

`2.0.9+mc26.3`, targeting `port/26.3`. These dungeon repairs remain
**Rot Client+ only**. Lite receives no puzzle implementation. Previous modern
room/secret, mining, session, UI and market checkpoints are retained.

## Source adaptation

The actual implementations were read at these revisions:

* NoammAddons `4164abc38500e041a805e03ca0f01b46e37bd8a5`:
  `floor7/devices/SimonSays.kt`, `dungeon/solvers/puzzles/TeleportMazeSolver.kt`.
  CC0. Simon separates playback/input; TP Maze groups pads, records visits
  and accumulates server yaw hints. The Rot adapter additionally rejects
  relative/malformed packets and isolates evidence by verified room/run.
* Odin `b0058000511ef755dfc6379330a1d23404e08df2`:
  `boss/SimonSays.kt`, `dungeon/puzzlesolvers/TPMazeSolver.kt`, map scanning.
  BSD-3-Clause. Simon observes actual grid/button power and start resets;
  map terrain and moving player markers have separate update paths. Existing
  Odin pad geometry is reused. Full license notices remain in both JARs.

See [the preceding upstream/API audit](DUNGEON_ROOMS_2026-10-12.md) for
Athen, Nebulune, OdinClient, Devonian, current Hypixel changes and unresolved
Party Finder backend/schema issues. This slice does not replace an API
backend or claim complete feature parity with those projects.

## Behavior corrections

Simon now keeps separate playback/input/complete states. A dark interval
between flashes no longer publishes an incomplete sequence or starts a new
round. Repeated separated flashes are retained. Input requires the actual
stone-button grid; ambiguous simultaneous observations suppress guidance.
Every raw server block update is observed, including updates between ticks.
Server snapshots retain section packet updates before the world applies them.

Input advances only on a matching server-observed powered edge. Minecraft's
local button-power prediction and local clicks do not advance the sequence.
Start reset requires a server-observed rising edge. Automatic start/next
clicks recheck the enabled F7/M7, boss, loaded-device and distance context;
auto-start is restricted to idle. Existing automation remains opt-in.
World/context loss clears the sequence and server snapshots.

TP Maze now requires an identified, rotated room and all thirty loaded portal
frames. The solver consumes pre-application absolute player-position packets
with the maze coordinate/yaw signature. Walking and arbitrary distance jumps
cannot create links. Relative position/rotation, malformed/foreign/duplicate
packets are rejected. A known teleport item use suppresses observations for
20 ticks. Candidate colors express accumulated directional hints, not a
guaranteed route; only recorded directed teleports constitute links.

Room hashes, rotations and puzzle block IDs read true chunk states, bypassing
the client-side I Hate Doors glass substitutions. Door replacement also
rechecks the true server block/settings before returning glass. Entrance
iron-bar ESP remains present without changing those bars into glass.

## Bounded work

| Work | Before this slice | Current implementation |
| --- | --- | --- |
| Diorite boxes | 2,197 block reads per render frame | Cached boxes; at most 384 sampled positions per tick; stationary volume completes in six ticks |
| I Hate Doors | 3,757 positions per tick plus a separate 1,690-position ESP scan every five ticks | One shared traversal, at most 384 positions per tick; stationary volume completes in ten ticks |
| Hidden F7 pillar scan | Up to 7,448 candidate positions per tick | At most 384 positions per tick; all four fixed pillar volumes complete in twenty ticks |
| Room identification | Up to 36 attempts per reveal tick | At most two attempts per tick, player room first; unknown cores consume budget and retry after 40 ticks |
| Pending rotations | All pending rooms/tiles every ten ticks | At most one additional attempt every ten ticks, failures included; failed positions defer for twenty ticks |
| TP Maze fallback | 11,767 positions every puzzle scan when rotation unavailable | Thirty expected frames in a verified room; no broad fallback scan |
| Magical Map terrain | Recalibration/terrain decoding every tick | Selected terrain identity/floor/content cache plus four bounded candidate probes; rejected hand maps do not evict it; markers refresh every tick |

The traversal finishes an overlapping region before accepting a walking
player's new region, preventing starvation. Warps to a nonoverlapping volume
discard the previous traversal. Disabling a feature or leaving its context
clears its cache. Chunk availability is checked before sampling.

These are operation limits and test results, **not measured FPS gains**.
Incremental traversal intentionally trades immediate full-volume discovery
for bounded work. Initial pillar discovery may take about one second at 20
TPS. Actual frame time, lag, loaded-room timing and gameplay require testing.
Other puzzle solvers still have scans and remain follow-up audit targets.

## Automated validation

Java 25 `build` passes: **2,520 shared + 345 Plus tests**, no failures,
errors or skipped tests. Both clients compile; `verifyLegitJar` and
`verifyDungeonJarBoundary` reject the four new Plus classes in Lite and
require them in Plus. New regression coverage checks Simon phase/power
edges, TP evidence/links, mutable map pixels/floor/world identity, moving
markers, scan coverage/fairness/warp reset and malformed scan bounds.

## Controlled playtest still required

1. F7/M7 Simon: each round, repeated flash, manual and automatic input, failed
   input, delayed acknowledgment, start/reset, another player's button use,
   disable/re-enable and dungeon departure. Capture block update timing if a
   sequence is ambiguous or incomplete.
2. TP Maze: all rotations, normal plate teleports, visited/candidate colors,
   ordinary walking and AOTE/AOTV/Conduit/Wither Impact rejection. Hint ranking
   is not proof of a winning pad. An unknown ability with an identical packet
   signature cannot be distinguished without additional evidence.
3. Stationary and moving door/pillar overlays: discovery delay, all glass
   colors, iron bars, opened/removed doors, toggle changes, chunk unload and
   world switch. Compare frame times with these modules off/on.
4. Magical Map: unchanged map while teammates move/rotate, new rooms,
   checkmarks and doors, reveal/explored switches, Entrance and F1–F7/M1–M7.
5. Both editions: `/rot`, settings persistence and existing HUD/inventory,
   mining and Current Session smoke tests. Do not enable both JARs together.

Party Finder's functioning stats backend/exact schema, conditional F7/M7
timings, the wider solver/secret multiplayer matrix and the public Lite
release audit remain open. This checkpoint is ready for runtime testing,
not a claim that all dungeon work or public release review is finished.
