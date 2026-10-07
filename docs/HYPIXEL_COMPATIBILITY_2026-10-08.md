# Hypixel compatibility audit — 2026-10-08

This audit covers the current official API contracts and published gameplay
changes relevant to existing Rot modules. It is not a claim that every game
mechanic or every module has been tested in Minecraft.

## API evidence

Read-only probes on 2026-10-08 returned HTTP 200 and `success:true` for:

- `/v2/skyblock/bazaar`: `lastUpdated`, `products`.
- `/v2/resources/skyblock/items`: `lastUpdated`, `items`.
- `/v2/resources/skyblock/collections`: `lastUpdated`, `version`, `collections`.
- `/v2/resources/skyblock/skills`: `lastUpdated`, `version`, `skills`.
- `/v2/skyblock/auctions?page=0`: `page`, `totalPages`, `totalAuctions`,
  `lastUpdated`, `auctions`. The sampled snapshot had 48 pages.

These are top-level schema/availability checks. No API key, private profile,
or authenticated account data was used. No current REST schema break was
observed in these paths. They do not supply live pet XP, commission progress,
future ability block lists or server click authorization.

The official [Mod API](https://github.com/HypixelDev/ModAPI) remains at
`5245ef1f161883ae9152d719de29885bb09e3f57` (2026-03-24). Rot's existing
optional callback-interface adapter is retained; an absent API still falls
back to observed game data. The official
[Public API repository](https://github.com/HypixelDev/PublicAPI) remains at
`f928da471c2727caf3ea1c393631046b9570266f` (2025-09-21).

Rot did have retry defects independent of a schema change: Bazaar polling
kept its fixed interval after failures; auction `success:false`/invalid first
pages could bypass backoff; very large Retry-After seconds could overflow.
The update adds bounded failure backoff, HTTP-date Retry-After support and
overflow-safe parsing while retaining the last valid quote. See
[official API documentation](https://api.hypixel.net/) and
[HTTP Retry-After semantics](https://www.rfc-editor.org/rfc/rfc9110.html#name-retry-after).

## Gameplay changes and implementation

The [0.27.2 Minister Update](https://hypixel.net/threads/hypixel-skyblock-0-27-2-the-minister-update-greenhouse-qol-and-more.6159904/),
published 2026-10-07, confirms Melody's three rows and Numbers' ten entries.
Rot's default simulator now uses these sizes. Actual observed terminal
snapshots retain support for legacy four-row/fourteen-number servers.
Plus terminal corrections include exact color identity, unfinished-number
state, container-bound clicks and inherent item glint.

Slayer announcements/ownership contracts are also compared against current
Athen. The original `Spawned by` path remains. New local spawn messages may
resolve a unique fresh family/tier/nameplate match; missing or ambiguous
evidence does not assign an owner. Dagger actions are single-use and cancel
when their target/context becomes stale.

Pet identity remains open to new names and Special/Very Special rarity.
Observed XP is used rather than applying an old mob-XP formula. Commission
parsing accepts current split tasks without consuming unrelated widget data.

## Changes requiring more work or runtime evidence

- Faster Watcher/F7/M7 cutscenes, immediately respawning dragons and fewer
  Simon Says light sets require real runs before retuning duration estimates.
  Do not replace timings with guessed values.
- Fiesta duration/rewards, island weather, powder modifiers, Greenhouse
  mutations and new pet perks are server mechanics. Observed Sack/Pristine
  deltas remain accounting authority; this update does not calculate their
  rewards from outdated formulas or implement a new weather/Greenhouse HUD.
- InfiniBoom/InfiniLeap display names changed; prefer stable ExtraAttributes
  identifiers when an existing feature already uses them. No blanket string
  replacement or resource-pack rewrite is performed.
- The [October 5 patch](https://hypixel.net/threads/october-5-skyblock-patch-notes.6159925/)
  removes the reconfiguration screen on island transitions. Test reconnect,
  island changes, entity/queue cleanup and GUI closes explicitly.
- The [September 29 patch](https://hypixel.net/threads/september-29-skyblock-patch-notes.6158108/)
  changes Tiger/Fatal Tempo behavior; the client must not invent extra kills
  or XP from this behavior. The
  [September 17 patch](https://hypixel.net/threads/september-17-skyblock-patch-notes.6153488/)
  fixed drill mining-progress resets. Rot's observed mining ledger is preserved.
- Exact Pickobulus geometry, cooldown modifiers and complete area accounting
  remain the explicitly documented open work from the previous mining slice.

The owner's test server uses the same intended protocol. This is implemented
through observed data contracts, without introducing a separate server-address
branch or claiming that compilation proves runtime parity.
