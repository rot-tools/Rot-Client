# 26.3 F7 timer observation follow-up

Candidate 2.0.7, Plus dungeon runtime only. Lite receives the same shared UI baseline and remains free of live dungeon timer classes. Accounting/history unchanged; older Minecraft versions are not rebuilt.

## Compared

[Odin TickTimers.kt](https://github.com/odtheking/Odin/blob/833e0533ef9c47529b790612627a65618ebd5a58/src/main/kotlin/com/odtheking/odin/features/impl/boss/TickTimers.kt), BSD-3-Clause, uses nominal server ticks for Pad (20), Lightning (520), Storm PY (55) and Goldor (60). Its lightning starts at Storm's introductory line; Storm's death clears phase timers, and Core opening ends Goldor tracking. The preceding Noamm/Blade/Skyblocker comparisons remain documented in UPSTREAM_DUNGEON_AUDIT.md. These frameworks are not interchangeable with Rot's Fabric 26.3 runtime.

## Corrected

Rot had 20 seconds for Pad, 60 seconds for Goldor, the wrong lightning trigger at Storm death, prefix-only PY detection and a single timer slot that discarded overlapping countdowns. Durations now convert nominal tick counts at 50 ms/tick: 1s Pad, 26s Lightning, 2.75s PY, 3s Goldor. A separate immutable phase state owns independent deadlines. Storm intro opens both Pad and Lightning; PY no longer replaces Lightning. Storm death/Core opening clear older timers; duplicate/expired/earlier-phase chat cannot extend or rearm them. Unknown/prefixed chat and invalid clock ranges leave state unchanged.

The state is observed only in confidently detected F7/M7, resets outside dungeon and on the existing runtime reset, and live settings gate rendering. Timers show **~** because nominal wall-clock countdowns do not prove server tick rate or current conditional Minister timings. Existing Maxor/Storm-transition/Necron estimates remain estimates. This slice does not implement Odin's repeating Pad/Goldor cycle or full feature parity. No gameplay actions are added and no upstream implementation is copied.

## Validation

Java 25 dual build, 2,519 shared + 283 Plus tests, both edition verifiers and diff checks pass. Focused Plus tests cover concurrent deadlines, nominal tick units, exact messages, duplicate/expired delivery, phase transitions and invalid clocks. Minecraft runtime remains pending.

Manual: F7/M7 Storm intro should display independent Pad/Lightning countdowns; PY keeps Lightning visible. Storm death hides those entries; Goldor shows a nominal 3s countdown, Core opening clears it. Repeat/party-chat copies cannot restart timers. Leave/rejoin resets state; toggle each timer setting and verify Lite has no dungeon runtime. Current conditional boss-speed evidence and the wider dungeon audit remain open.
