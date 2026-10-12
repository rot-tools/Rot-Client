# Modern dungeon rooms and secrets — 2026-10-12

Candidate `2.0.8+mc26.3`, integration branch `port/26.3`. Dungeon implementation
and the new room data remain **Rot Client+ only**. Lite carries no new solver,
secret observation policy or modern room dataset. This checkpoint is automated
tested and ready for Minecraft testing; it is not full upstream feature parity.

## Sources actually inspected

Public default branches were fetched on 2026-10-12. The older local reference
checkouts were kept intact; comparisons used the fetched source revisions.

| Project | Inspected revision | Relevant findings |
| --- | --- | --- |
| [Odin](https://github.com/odtheking/Odin) | `b0058000511ef755dfc6379330a1d23404e08df2` | `MapScan`, `MapRenderer`, `WorldScan`, `SecretWaypoints`, Weirdos/TP/Blaze solvers; latest entrance pending-color fix and M7 dragon death observation |
| [OdinClient](https://github.com/skies-starred/OdinClient) | `77dba9854bc8b95120c256f14dcb043e9d1fb5ad` | F7 device and terminal paths; current Auto/Queue Terms warning stubs cannot replace an operational runtime |
| [NoammAddons](https://github.com/Noamm9/NoammAddons) | `4164abc38500e041a805e03ca0f01b46e37bd8a5` | `ScanUtils`, `UniqueRoom`, `RoomData`, `ActionBarParser`, `EventDispatcher`, `DungeonUtils`, Three Weirdos, Simon and terminal handlers |
| [Athen](https://github.com/skies-starred/Athen) | `c1f0314f423b11c8b9ab47b2a6e1d44152eec681` | Dungeon API phase/context boundaries and modern port changes; existing MIT/BSD provenance must be retained |
| [Nebulune](https://github.com/skies-starred/Nebulune) | `3191833087c36ec7085b5095c06402e8658bca99` | Archived reference; discontinued Soulsand behavior is not evidence of a newer working implementation |
| [Devonian](https://github.com/Synnerz/Devonian) | `1eee3a10c5bbde5acf92d15127605a408b7dd009` | Separate normal/master floor PBs, stale API cache correction, adjusted Blaze/TTT score, room rendering, TeamSecretsStats websocket requirement |

Odin/OdinClient/Athen/Nebulune are BSD-3-Clause, Noamm is CC0-1.0; they are
not all MIT. Devonian is GPL-3.0 and was compared for behavior/schema here;
its implementation was not transplanted. Existing full third-party notices
remain packaged. The actual adaptation in this checkpoint is Noamm's CC0
room/secret/Weirdos logic plus Rot's event integration and validation guards.

## Repaired paths

- The old numeric-block-ID core cannot identify Noamm's modern room cores.
  `DungeonModernRoomPolicy` now uses Noamm's Java `31 * hash + blockName.hashCode()`
  fold over the exact Y140–Y12 column, including its transient block exclusions.
  The matching [140-room data](https://github.com/Noamm9/NoammAddons/blob/3c7b32c471148899e68022cfd2afdfc843be91a1/rooms-modern.json)
  is copied verbatim and pinned. SHA-256:
  `2B286A4CAA266E4FD941D156E54A785B07D87CE36066A6C26A16480AF7FAE51C`.
  Legacy identification remains a fallback for existing test-server layouts.
- Rotation waits for the complete 1x1/1x2/1x3/1x4/2x2/L shape, reads the main
  tile's highest non-air/non-gold block through Y256, probes four outer corners
  or each L tile's corners, and applies Noamm's `360 - rotation` transform.
  Partial chunks, disconnected shapes and ambiguous markers remain pending.
- Unknown cores retry instead of becoming permanently rejected for the run.
  Failed attempts consume the scan budget; pending rotation scans are throttled.
- Party-wide sidebar secret increments no longer credit the local player's
  current room. The server's action-bar `N/M Secrets` count updates only an
  identified room whose total matches. Both Minecraft action-bar packet paths
  are handled; repeated counts do not add duplicate credits.
- Secret item waypoints hide on actual, typed, nearby pickup events, including
  another visible player's pickup. Bat waypoints use bat-death sound evidence.
  Entity disappearance and missing/unloaded blocks no longer imply collection.
  Chest/essence/lever hiding uses a matching local block interaction, as upstream
  does: this is an observed click, not proof that the server awarded a secret.
- Three Weirdos uses Noamm's complete speech patterns and rotated NPC +X chest
  offset at Y69. Party quotes and unrelated rooms cannot choose a chest. The
  previous nearest-chest guess and large chest scan have been removed.

Missing waypoint coordinates in upstream data stay missing. A room record or
secret count is not proof that every secret in that room has coordinates.

## Teammates and floors

The common room core/rotation algorithm is floor-independent. Rot's existing
sidebar model recognizes Entrance, F1–F7 and M1–M7. Boss/device behavior is a
separate layer and must be tested with its own floor/phase guards.

Server room counts include teammates, but do not identify each secret's finder.
Another player opening a remote chest is not a local block interaction. Exact
per-player or remote per-waypoint totals cannot be inferred from the global
sidebar. Devonian's TeamSecretsStats explicitly uses cooperating clients over
a websocket plus local Extra Stats. Rot does not add that external connection
or send party messages in this checkpoint. Map checkmarks/party markers and
existing terminal completion chat remain separate observed evidence.

## Hypixel / Party Finder API check

On 2026-10-12, items, collections, skills, Bazaar and auction page 0 under
Hypixel `/v2` all returned HTTP 200, `success=true` and their expected top-level
fields. PublicAPI head remains `f928da471c2727caf3ea1c393631046b9570266f`;
ModAPI head remains `5245ef1f161883ae9152d719de29885bb09e3f57`.
[Official API docs](https://api.hypixel.net/) still document v2 and rate limits.
No new Hypixel HTTP API break was found by these bounded checks. They do not
validate private profile data or all in-game packet formats.

The [Minister update](https://hypixel.net/threads/hypixel-skyblock-0-27-2-the-minister-update-greenhouse-qol-and-more.6159904/)
changes conditional boss timing, Simon playback and secret/key behavior. The
existing 10-number / 3-row terminal fixes remain present. New timing constants
are not guessed from another client's nominal countdown.

**Party Finder remains an open integration issue.** Rot currently requests
`https://sky.shiiyu.moe/api/v2/profile/{name}` and recursively guesses fields.
The current [SkyCrypt backend routes](https://github.com/SkyCryptWebsite/SkyCrypt-Backend/blob/dev/src/routes.go)
instead document `/api/stats/{uuid}` / `/api/playerStats/{uuid}/{profileId}`
with an [API-token middleware](https://github.com/SkyCryptWebsite/SkyCrypt-Backend/blob/dev/src/routes/auth.go).
Its deployment's compatibility routes were not established. A functioning,
approved stats backend and exact schema are required before claiming all stats.
Normal/master floor PBs, milliseconds/seconds and missing values need explicit
parsing; unavailable data must not be treated as confirmed zero for auto-kicks.
Devonian uses its own external dungeon API; copying its caller alone would not
establish that service's availability or credentials for Rot.

## Validation and next checks

Java 25 `build`, 2,519 shared + 300 Plus tests, `verifyLegitJar` and
`verifyDungeonJarBoundary` pass. New regressions cover the modern fold,
transient blocks, incomplete columns, pinned data, collision rejection,
all four rotations, multi-tile/L shapes, unloaded/ambiguous corners,
modern height scan, typed/nearby pickups, room count parsing and Weirdos.
The new packet injection method names were checked against the actual 26.3
Minecraft classes. Compilation/tests do not establish live Hypixel gameplay.

Controlled Minecraft testing still needs:

1. Plus on 26.3: several known rooms in every rotation; line/L/2x2 rooms;
   chest/item/essence/bat waypoints; unknown room and delayed chunk loading.
2. Two players in different rooms: party sidebar increases do not change the
   wrong room; current-room action-bar counts include the teammate; item pickup
   hides only the nearby correct waypoint; despawn/unload does not hide it.
3. Rejoin/new run and Entrance/F1/F3/F5/F7/M1/M7 controls: state reset, map
   calibration/checkmarks, head death/revival and F7-only device/timer boundaries.
4. Three Weirdos: all six winning claims and all four room rotations.
5. Lite startup: new Plus policy/data absent and existing shared UI unaffected.

Further implementation slices remain: Party Finder backend/schema; TP Maze
packet-driven links; Simon playback/input transitions; current conditional
F7/M7 boss durations; score projection and room renderer comparison with the
latest Odin/Devonian changes. Public Lite release/provenance gates remain open.
