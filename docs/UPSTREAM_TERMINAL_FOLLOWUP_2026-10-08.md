# Terminal follow-up — 26.3 only, 2026-10-08

This slice completes the previously deferred per-menu Rubix goal and
Starts With slot-acknowledgement integration for Rot Client+. It does not change
older Minecraft branches or put solver execution into Lite.

## Source and attribution

Odin `origin/main` was fetched again; the inspected head remains
`833e0533ef9c47529b790612627a65618ebd5a58` (2026-10-05), also the published
`0.3.6` release commit. The reference files are:

- [RubixHandler.kt](https://github.com/odtheking/Odin/blob/833e0533ef9c47529b790612627a65618ebd5a58/src/main/kotlin/com/odtheking/odin/utils/skyblock/dungeon/terminals/terminalhandler/RubixHandler.kt): per-terminal goal, circular distances and direction, including forward-only mode.
- [StartsWithHandler.kt](https://github.com/odtheking/Odin/blob/833e0533ef9c47529b790612627a65618ebd5a58/src/main/kotlin/com/odtheking/odin/utils/skyblock/dungeon/terminals/terminalhandler/StartsWithHandler.kt): per-slot clicked overrides and native-glint exceptions.
- `TerminalHandler.kt` in the same directory: packet-updated solutions and click protection.

The Java adaptations explicitly credit Copyright (c) 2025, odtheking and
BSD-3-Clause in `DungeonTerminalSessionPolicy` and `DungeonTerminalSolverPolicy`.
The complete notice remains in `docs/third-party/Odin-LICENSE.txt` and the
distributed notices. Rot adds container identity, complete-board readiness and
fresh-state checks rather than copying Odin's event/framework plumbing.

## Behavior

`DungeonTerminalSessionPolicy` owns bounded state for one menu and title.
Rubix waits until all nine known cycle panes are present, then selects and
retains one color goal until the menu/title changes, the module is disabled,
or the world resets. Left-only mode minimizes forward clicks when choosing
the goal; it no longer chooses a bidirectional solution and forces reverse
steps into left clicks. Changing the click mode later retains the current
goal. Incomplete snapshots do not choose a new goal or declare completion.

Auto Terms, queued/manual clicks, hover clicks, overlays, wrong-slot guards and
clicked-slot hiding now consume the same stateful solution. Existing Clone
right-click preservation and current/legacy terminal sizes remain in place.

Starts With records pending slots when an input is actually sent. Merely
adding a queued click does not confirm it. A matching applied server slot or
content packet can acknowledge the slot only when its 15-bit container state
ID advances, the item still matches the sent item and the server observation
is glinting. Unchanged, older, invalid or half-cycle-ambiguous state IDs cannot
confirm a click. A fresh non-glint rejection or replacement permits retry.
Pending entries expire after the existing configured resync timeout; a
confirmed entry stays excluded for that menu unless a fresh server correction
changes it. Other slots, containers, titles and player-inventory updates do
not acknowledge the active terminal.

Shared packet handlers forward bounded observations through no-op default
hooks. Only the Plus implementation owns pending/confirmed state and solver
execution. Real server callbacks expressly exclude the local terminal
simulator; its accepted local click result supplies separate explicit evidence.
Disabling the module clears goals and acknowledgements, including when a
correction arrives while disabled. Re-enabling binds fresh state to the menu.

The renamed InfiniBoom and InfiniLeap displays were checked: Superboom's
existing lookup uses the SkyBlock ID containing `SUPERBOOM`, and the existing
`leap` display fallback still matches InfiniLeap. No unrelated item-use change
was necessary in this slice.

## Files and verification

- New Plus `DungeonTerminalSessionPolicy` and its nine regression tests.
- Plus `DungeonTerminalSolverPolicy`, `DungeonRuntime`, `DungeonPlusRuntime`,
  `DungeonHoverTermsRuntime`, `DungeonPlusInputRuntime` and `TermSimRuntime`.
- Inert shared packet hook declarations and client packet forwarding, with
  Plus hook implementations.

Regression cases cover complete-board locking and subsequent corrections,
forward-only goal selection, a fresh native-glint acknowledgement, unchanged
and older-state resends, timeout retry, different containers/titles/slots,
fresh non-glint rejection and replacement, state-ID wrap/invalid values,
explicit simulator acceptance, and disable/correction/re-enable isolation.
The parent build supplies final compile/test/JAR-verifier results; these
automated cases do not constitute Minecraft runtime confirmation.

## Runtime limits

Some custom servers keep a constant state ID. On those servers Rot deliberately
does not label an unchanged intrinsically glowing item confirmed; it waits
briefly and permits retry. Verify real SetSlot/Content packets before changing
this guard. A fresh matching glint update is the supported confirmation
contract; it is not a separate Hypixel success API or transaction receipt.
The client cannot prove a server's behavior from a displayed native glint
alone.

Playtest Rubix with Queue/Clone and left-only mode on/off, Starts With native
glint items with delayed/rejected inputs, multiple same-title menus, module
disable/re-enable, and simulator latency. Packet corrections and server input
timing remain runtime-pending. Boss-bar/dragon timing, other upstream modules,
and wider module parity are separate remaining slices.
