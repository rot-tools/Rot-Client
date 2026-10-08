# Pickobulus observation lifecycle audit — 2026-10-08

Scope: Minecraft 26.3, shared cooldown HUD in Rot Client and Rot Client+.
This change does not credit items, change mining quantities, or move the
Plus-only block preview into Lite.

## Source evidence

The inspected pins are behavior references. No LGPL implementation was copied
or adapted into the shared policy or runtime.

| Project | Inspected commit | Relevant source | Evidence used |
| --- | --- | --- | --- |
| Skyblocker, LGPL-3.0 | `f5cc8799c6d9c0c0a75bcba525bc1487245c31b0` (2026-10-06) | [ItemAbility.java](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/utils/ItemAbility.java) | Ability headers can include `⦾ ` and two spaces before `RIGHT CLICK`. A cooldown belongs to its own ability block. |
| Skyblocker, LGPL-3.0 | Same pin | [PickobulusHelper.java](https://github.com/SkyblockerMod/Skyblocker/blob/f5cc8799c6d9c0c0a75bcba525bc1487245c31b0/src/main/java/de/hysky/skyblocker/skyblock/dwarven/PickobulusHelper.java) | The tab field is named `Pickobulus:`; `Available` is a ready status. Other named abilities must not supply Pickobulus status. |
| SkyHanni, LGPL-2.1 | `a7528349a2d759581471ac530351d7848b23784b` (2026-10-07) | [MiningApi.kt](https://github.com/hannibal002/SkyHanni/blob/a7528349a2d759581471ac530351d7848b23784b/src/main/java/at/hannibal2/skyhanni/data/MiningApi.kt) | Named use chat, a total destroyed-block result, and an explicit no-block result exist. A reported total cannot establish material identities or target quantities. |

The direct official wiki URLs redirect to a closure announcement at audit time.
Cached search excerpts are not proof of current cooldown modifiers, blast
geometry, or private-server behavior. No such constants were introduced here.

## Fixed behavior

- An unchanged cached tab countdown no longer restarts the deadline every tick.
  Only a changed valid named status changes the countdown. Cached readiness
  present before a use cannot immediately complete that new use.
- A countdown remains labelled `server observed` for a short freshness window.
  Local expiry without an explicit ready observation is labelled `estimated`.
  Lore/configured durations also remain estimated because perks can modify them.
- Cooldown lore requires an exact Pickobulus ability header, including the
  observed icon form. Other ability headers end its block. Descriptive mentions,
  conflicting headers/cooldowns, and malformed durations cannot borrow a
  different ability's cooldown.
- Immediate duplicate use notifications do not reset the last-use projection.
  Timer corrections preserve the one-notification-per-cycle rule. Initial ready
  status shows in the HUD without announcing a newly completed use.
- Deadline arithmetic rejects overflow. Clock reversal, disable/re-enable,
  disconnection, world/connection changes, leaving SkyBlock, and same-world
  profile changes clear timer, last-use counts and popup state. Use/ready chat
  outside a detected SkyBlock context is ignored.
- Existing accepted material-break callbacks remain the only input to the
  read-only last-use projection. Its five-second window is retained. Display
  counters use saturating `long` arithmetic. The target label freezes at use;
  a tracker switch during that window marks its target count partial instead
  of relabelling it as the new target's result.

## Validation and limits

`PickobulusLifecyclePolicyTest` adds 18 deterministic cases for cached and
changed countdowns, initial readiness, server-ready correction, stale authority,
duration/clock overflow, strict lore and tab evidence, duplicate uses, count
overflow/window bounds, tracker changes, popup expiry, and context/profile
changes. The existing Pickobulus parser, notification and config tests remain.
The parent task runs the complete shared/Plus build and JAR verification; no
Minecraft runtime validation is claimed by this document.

Exact preview geometry and target-material quantities still require controlled
server evidence. A server-reported total destroyed-block count is useful
presentation evidence but must not create mining ledger credits. This slice
does not add that total-result HUD or infer missing block identities. Gemstone
quantity accounting remains on its existing item/Sack provenance path.

Runtime checks still needed: genuine named use + tab countdown transitions,
cooldown modifiers, delayed/duplicate chat, module disable and profile/world
switches, ready popup/sound settings, and accepted-break projection while
changing the selected tracker. Unchanged text cannot prove a fresh independent
server event; absent fresh evidence, the timer deliberately remains estimated.
