# Dungeon upstream audit — 2026-10-08

This is one bounded update to existing Rot Client+ dungeon modules. It is not
an assertion that every feature from four upstream mods has been imported or
that the private test server has been observed to match every Hypixel packet.
The user described the test server as equivalent; the repaired paths continue
to use normal observed Minecraft container state. No new authentication,
websocket, party-message transmission, or upstream update downloader was added.

## Repositories, revisions and licenses

The public default branches and latest GitHub releases were fetched on
2026-10-08. Release timestamps are GitHub UTC timestamps.

| Repository | Default branch / inspected commit | Commit date | Latest published release | Release commit | License actually present |
| --- | --- | --- | --- | --- | --- |
| [odtheking/Odin](https://github.com/odtheking/Odin) | `main` / `833e0533ef9c47529b790612627a65618ebd5a58` | 2026-10-05 | `0.3.6`, published 2026-10-06 14:28:33 UTC | same as default | BSD-3-Clause, Copyright (c) 2025, odtheking |
| [skies-starred/OdinClient](https://github.com/skies-starred/OdinClient) | `master` / `77dba9854bc8b95120c256f14dcb043e9d1fb5ad` | 2026-09-15 | `0.3.4-r1`, published 2026-09-15 20:56:03 UTC | same as default | BSD-3-Clause, Copyright (c) 2026, Starred |
| [Noamm9/NoammAddons](https://github.com/Noamm9/NoammAddons) | `26.1.2` / `6d0c95a5e994e9acc1d043c7eb2cdac916809a31` | 2026-10-07 | `1.2.9`, published 2026-10-06 19:24:18 UTC | `4244f12ec1768a4b46d71ebcd53d2bc4f18ca1f8` | CC0-1.0 |
| [BladeMasterGabe/blade-addons](https://github.com/BladeMasterGabe/blade-addons) | `new` / `fda86776dce1fab15d4aa0b6e1d7faf1b2bf574c` | 2026-10-07 | `v2.3.5`, published 2026-02-17 20:50:41 UTC | `d968d9ad0c59321a67b7a9a4e09e3c5fde55524f` | CC0-1.0 |

These repositories are not all MIT-licensed. Full unmodified license texts are
retained in `docs/third-party/{Odin,OdinClient,NoammAddons,BladeAddons}-LICENSE.txt`.
The attributed Odin adaptations retain its copyright, license conditions and
disclaimer; packaged third-party notices are handled by the Gradle distribution
configuration. The BSD names are acknowledgements, not endorsements.

Noamm's default branch has 19 changed files relative to its latest release;
they include new boss-bar F7 observation, Pet changes, door/map handling and
expired Mojang-session messaging. Blade's default branch has 220 changed files
relative to the February release, including a substantial Minecraft port and
notification refactor. A latest branch is not the same thing as a validated
release binary. This slice does not transplant their frameworks or session/API
authentication code.

## What was compared

Odin sources below are under
`src/main/kotlin/com/odtheking/odin/utils/skyblock/dungeon/terminals/terminalhandler/`:

- `SelectAllHandler.kt:13`: color-specific name prefixes; excludes black border
  panes and selected/glinting items.
- `NumbersHandler.kt:14`: unfinished red pane identity, stack count ordering,
  and a click guard that permits the next number only.
- `StartsWithHandler.kt:16` and `:37`: intrinsic-glint item exceptions and
  server-update acknowledgement of clicked slots.
- `RubixHandler.kt:19`: actual pane colors, a color goal locked per terminal,
  and opposite-direction clicks.
- `MelodyHandler.kt:13`: current three-row buttons 16, 25 and 34.
- `TerminalHandler.kt:53`: live-solution membership and first-click protection.

Odin's `features/impl/boss/termsim/NumbersSim.kt` and `MelodySim.kt` also show
the current ten-number / three-row practice layout. The official
[Hypixel 0.27.2 announcement](https://hypixel.net/threads/hypixel-skyblock-0-27-2-the-minister-update-greenhouse-qol-and-more.6159904/)
independently confirms the reductions from fourteen to ten numbers and four
to three Melody rows. Rot's default constants/practice layouts were updated
in the wider refresh; observed legacy 14-number/4-row servers remain supported.

Noamm sources under
`src/main/kotlin/com/github/noamm9/features/impl/floor7/terminals/impl/` were
cross-checked: `ColorsTerminal.kt:15`, `NumberTerminal.kt:13`,
`StartWithTerminal.kt:16`, `RubixTerminal.kt:17`, and `MelodyTerminal.kt:15`.
Its `TerminalClick.kt` explicitly preserves the opposite Rubix direction as a
right PICKUP click while ordinary clone clicks use button 2. Rot continues
to send through Minecraft's game-mode container input handler, not Noamm's
raw packet/session hash code.

OdinClient's current `features/impl/floor7/AutoTerms.kt` and `QueueTerms.kt`
contain warning-only module declarations; they are not new operational
solvers to copy. `SimonSays.kt` was inspected for its button/context, opt-in
configuration and bounded delayed clicks. Rot retains its own existing Simon
state machine; no unconditional auto-start/party output was imported.

Blade sources inspected include `features/dungeon/f7/terms/MelodyNotification.java`,
`terms/device/DeviceNotifier.java`, `features/dungeon/KeyNotification.java`,
`AutoRequeue.java`, and `utils/dungeon/Phase.java`. Its current Melody
notification still displays `/4` and derives progress from 25% steps, so copying
that implementation would reintroduce a stale post-patch assumption. Rot's
existing context-gated device/key helpers and archive/Extra Stats-aware requeue
are retained. Upstream's newest source is a comparison source, not automatic
proof that every current behavior is correct.

## Implemented repairs

All solver execution and click changes remain in `src/plus` or
`src/plusClient`. Shared Melody parsing only projects observed state; Lite's
solver extension remains inert.

| Rot file / region | Before | After |
| --- | --- | --- |
| `src/plus/java/fi/rotclient/DungeonTerminalSolverPolicy.java:146` (`itemMatchesSelectColor`, `colorKeywords`) | Substring matches mixed Blue/Light Blue, Gray/Light Gray and Green/Lime; item IDs could override the displayed puzzle color. | Attributed adaptation of Odin's prefix mapping, cross-checked with Noamm. Color names remain distinct; renamed black border panes and selected items are excluded. |
| Same file `:180` (`solveNumbers`) | Any numeric item name could be treated as unfinished, including completed green panes; very large digit names could throw during parsing. | Only unfinished red glass/panes with bounded valid stack counts qualify. Sort by server count, then slot for stable ties; support both 10 and 14 positions. |
| Same file (`supportsTitle`, `solveStartsWith`, `rubixIndex`) | Broad title substrings / color substrings could produce solutions for unrelated menu titles or non-cycle items. | Complete known terminal titles, single-letter Starts With, actual pane identities and exact Rubix cycle colors. |
| `src/plusClient/java/fi/rotclient/DungeonRuntime.java:2477` (`snapshot`) | A short chest's first 54 menu slots could include player inventory; all glinting native items were marked selected. | Bound to actual `ChestMenu` container size. Attributed native-glint exceptions apply only to Starts With. |
| `src/main/java/fi/rotclient/DungeonPolicy.java:494` (`parseMelody`, `melodyPlayRows`) | Footer/player panes could supply pointer/target data; black fourth-row footer panes inferred a nonexistent fourth play row. | Only top-row target / playable interior pointer count; ambiguous duplicate evidence waits. Only play panes and colored buttons infer rows. Three-row default and explicit legacy four-row evidence remain usable. |
| `src/main/java/fi/rotclient/DungeonBladePolicy.java` (`melodyProgressParty`, `melodyTeammateHud`) and Plus caller | Local three-row progress still announced 25/50%; other players were always displayed as `/4`. | Local progress uses observed rows (33/66% current, 25/50/75% explicit legacy). Third-party percentages are displayed directly because their row count is unknown. Existing opt-in/deduplication gates remain. |
| `src/plusClient/java/fi/rotclient/DungeonRuntime.java:1369`, `:3967` (enqueue/session) | Identical titles in two different container instances could share stale queue/prediction state. | Container identity plus title scopes queued clicks. Closing/nonterminal screens discard pending actions; a transient absent screen retains only first-click timing for the same container. Duplicate queued actions are suppressed. |
| `src/plusClient/java/fi/rotclient/DungeonPlusRuntime.java:195` (auto click) | Nearest/human ordering could choose number 2 before number 1. | Numbers retain the server's numeric ordering; other unordered terminals retain configured ordering. |
| Same file `:643` (queue flush), new `DungeonTerminalClickPolicy` | Clone mode changed required Rubix right-clicks into clone clicks; stale solved slots and out-of-order numbers could be flushed. | Right click stays button 1/PICKUP; clone-left stays button 2/CLONE. Validate next number, current unsolved slot/direction and bounded Melody buttons before queue use. |
| `DungeonTerminalClickRuntime`, `DungeonTerminalClickSettings` | Trails could linger into another same-title GUI; manually edited radius/thickness could be unbounded. | Clear on menu changes/disable; bound radius 1–16 and thickness 1–8 during reads and writes. |

Line references describe this update's working diff; subsequent merges can
shift lines. Method names identify the intended regions.

## Validation and what remains

Sixteen new Plus regression cases and three shared progress cases cover color separation, renamed borders,
selected-state/count authority, invalid digit names, current/legacy terminal
sizes, unrelated titles, foreign Rubix colors, Melody footer/duplicate evidence,
same-title container isolation, right-click preservation, next-number guards,
bounded Melody Skip and corrupted trail settings. Existing terminal tests now
use real numeric stack counts rather than contradictory name/count fixtures.
Progress tests preserve both current three-row and explicit legacy four-row
announcements, and confirm that teammate percentages do not invent row counts.
The root build runs compilation, shared/Plus tests and edition-verifier tasks;
their final results belong in the overall upstream-refresh report.

Minecraft runtime confirmation remains necessary. Test current Numbers and
Melody, Colors with both Blue/Light Blue and Green/Lime, Starts With native-glint
items, and Rubix with Clone + Queue on/off. Open a second terminal of the same
type while a click is pending and confirm no old action crosses windows. Check
the 26.3 mouse/color-picker/view-model fix remains unaffected.

Further slices remain, rather than being silently claimed implemented:

The 26.3 follow-up implements the first item below with fresh-state guards;
see [terminal follow-up](UPSTREAM_TERMINAL_FOLLOWUP_2026-10-08.md) for the
implementation, tests and remaining server-confirmation limits. This audit's
original remaining list is retained as the earlier checkpoint.

- Odin's locked Rubix goal and packet-update-based acknowledgement for native
  glint Starts With items. Rot retains its existing timed prediction/resync,
  which needs delayed-packet/native-glint playtesting before replacing it.
- Blade/Noamm boss-bar death/start observations, dragon scheduling and precise
  phase timers. The official patch says phase/cutscene delays became faster;
  it does not provide enough numbers to invent replacement timer constants.
- Any desired modules not already present in Rot. No entire GUI framework,
  room websocket, authentication client, updater or whole-mod package was
  imported by this dungeon slice.
- Stable SkyBlock item IDs remain the preferred identity for renamed
  InfiniLeap/InfiniBoom displays; do not substitute visual labels for IDs.

Source comparison does not by itself establish Hypixel permission or Modrinth
eligibility. These automation/solver fixes are Plus-only, and Lite must continue
to pass its physical JAR exclusion verifier.

## Roster follow-up in the 2.0.6 dashboard/map candidate

Odin DungeonUtils.kt tab grammar at 833e0533 is adapted into EmberDungeonPolicy
with copyright/BSD attribution and the packaged full notice. Rot accepts plus-rank prefixes,
bounds usernames and validates observed class/DEAD/EMPTY states. Legacy class/name rows remain
supported through narrow full-line patterns. Skyblocker DungeonPlayerManager was compared
for ordered rosters and ghost state; its LGPL implementation was not copied.
