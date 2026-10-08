# Slayer cocoon lifecycle followup — Minecraft 26.3

Reviewed on 2026-10-08, following the first upstream refresh. This slice targets
both 26.3 editions through shared observation and keeps action code in Plus.
It does not change mining quantities or backport changes to older game versions.

## Current source checkpoint

A fresh fetch of [skies-starred/Athen](https://github.com/skies-starred/Athen)
advanced default `master` from `21ada452` to
`0869043ae54428e7761383a7f60f44d97cf206ac`, dated 2026-10-08.
The concrete Slayer change is
[`879d583861caad8454cbb8158306ce4c6ebb4db0`](https://github.com/skies-starred/Athen/commit/879d583861caad8454cbb8158306ce4c6ebb4db0),
also dated 2026-10-08.

Reviewed pinned source files:

- [CocoonedSlayerBossResolver.kt](https://github.com/skies-starred/Athen/blob/0869043ae54428e7761383a7f60f44d97cf206ac/src/main/kotlin/foo/starred/athen/api/slayers/resolver/impl/CocoonedSlayerBossResolver.kt)
- [GenericSlayerBossResolver.kt](https://github.com/skies-starred/Athen/blob/0869043ae54428e7761383a7f60f44d97cf206ac/src/main/kotlin/foo/starred/athen/api/slayers/resolver/base/GenericSlayerBossResolver.kt)
- [SlayerAPI.kt](https://github.com/skies-starred/Athen/blob/0869043ae54428e7761383a7f60f44d97cf206ac/src/main/kotlin/foo/starred/athen/api/slayers/SlayerAPI.kt)
- [SlayerBoss.kt](https://github.com/skies-starred/Athen/blob/0869043ae54428e7761383a7f60f44d97cf206ac/src/main/kotlin/foo/starred/athen/api/slayers/enums/type/impl/SlayerBoss.kt)

Athen remains BSD-3-Clause, copyright 2025–2026 Starred. The existing full
[Athen license notice](third-party/Athen-LICENSE.txt) is retained and packaged
by the build. The derived lifecycle policy carries a pinned inline attribution.

## Deterministic lifecycle implemented

Upstream's current cocoon fix no longer waits for an assumed reappearance time
and then selects an entity by position. It uses the local cocoon server message,
the previous owned boss family/tier, and the following quest-start server message.
Its pending state expires after 140 server ticks.

Rot adapts the message chain with stricter ownership evidence:

1. `YOU COCOONED YOUR SLAYER BOSS` must be an exact server line.
2. Exactly one still-active, verified local boss must exist in the canonical
   Slayer engine. Unknown owners, unrelated mobs and ambiguous active bosses
   do not provide a source.
3. That source body is suspended without awarding a kill or carry. Repeated
   death events and lingering old nameplates cannot re-register it. The bounded
   suppression set clears on world change.
4. An exact `SLAYER QUEST STARTED!` message and a matching visible quest
   family/tier provide the restart authority.
5. The existing recent entity-add/direct-nameplate resolver identifies the new
   body. It still requires a unique fresh matching candidate and rejects
   missing metadata, explicit foreign owners, old IDs and ambiguity.

The chain has a seven-second wall-clock eligibility bound, corresponding to
upstream's nominal 140-tick cap. This is a conservative expiry for client
evidence, not a claim that server TPS or the ability's actual duration is fixed.
Restart metadata still uses the existing 1.5-second resolution window, anchored
to the original restart message rather than a later HUD observation.

No nearest-body, player-facing-direction, last-spawned-body guess or outbound
websocket was imported. Duplicate chat cannot extend or reopen the chain;
accepted cocoon dedupe survives successful recovery for the original window
so a replay cannot suspend the newly registered body.
Quest completion/failure, mismatched quest metadata, expiry, relog and feature
reset invalidate pending recovery. The old `Spawned by` path remains supported.

## Accounting boundary and limitations

Suspension changes only the still-active observational boss identity. It does
not reverse already credited kills/carries, drops, RNG observations, personal
bests, published messages or durable history. Existing drop rows remain intact.

**If the death event has already been credited before the cocoon message, this
recovery path refuses to infer an owner or roll back history.** That packet
ordering remains unsupported until controlled runtime evidence establishes a
safe additional contract. The implementation does not claim every cocoon
packet order works.

Tarantula T5 second-phase ownership still requires an owner tag or authoritative
local spawn evidence. Existing phase-one kill/carry suppression is preserved;
the upstream remote-owner heuristic was not imported. Rift Bloodfiend's new
body identity remains unverified and its legacy owner-tag path remains in use.

The following need Minecraft runtime confirmation on the actual test server:

- Cocoon chat preceding the body death/removal event; no premature kill/carry.
- Exact restart message, matching sidebar quest and a new direct nameplate.
- Concurrent unrelated fresh bodies remain unowned, with no automation action.
- A late cocoon message leaves earlier credited history untouched.
- Quest failure/completion and relog clear recovery; legitimate final kills
  still increment once, and drops/RNG rows survive suspension.

## Regression coverage

`SlayerCocoonRecoveryPolicyTest` adds 13 shared tests covering the full restart
handoff, exact/spoofed chat, foreign/unknown/non-boss sources, wrong family/tier,
duplicates including post-recovery replay, metadata delay, expiry, reset,
backwards clock, no kill/carry credit
from suspended bodies, unchanged drops, one final resumed kill, late death
refusal and clearing suppression across worlds.

Sources and tests were prepared without running Gradle in the agent's task.
The integrating agent records the full 26.3 build and edition verifier results.
