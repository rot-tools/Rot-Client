# Athen / Nebulune Slayer audit — 2026-10-08

This is a focused compatibility and regression update for the existing Slayer
modules. It is not a replacement of Rot's canonical Slayer engine. Source review
and automated regression coverage do not establish Minecraft runtime parity.

## Source checkpoints and licenses

| Repository | Reviewed source | Source date | License and status |
| --- | --- | --- | --- |
| [skies-starred/Athen](https://github.com/skies-starred/Athen) | default `master`, `21ada452fd8f2085174b87e267fc2cc96683c016` | 2026-10-07 | BSD-3-Clause; copyright 2025–2026 Starred |
| Athen `alpha-slayer` | `f6e700ddaf7f96cddda0288ce7e6140328e7482e` | 2026-10-05 | Earlier Slayer alpha branch; default master includes subsequent resolver/carry fixes |
| [skies-starred/Nebulune](https://github.com/skies-starred/Nebulune) | default `master`, `3191833087c36ec7085b5095c06402e8658bca99` | 2026-08-20 | BSD-3-Clause; copyright 2025 Starred; repository archived 2026-08-22 |

Nebulune's latest published GitHub release is `0.2.1`, dated 2026-08-03.
Its later default source adds 26.2 support; it does not provide a maintained
26.3 branch to copy wholesale. Athen default source was reviewed as the current
source checkpoint; the GitHub latest-release endpoint did not return a stable
release for this repository.

These repositories are **BSD-3-Clause, not MIT**. Redistribution is permitted
under their actual licenses. Full notices are preserved in
[Athen-LICENSE.txt](third-party/Athen-LICENSE.txt) and
[Nebulune-LICENSE.txt](third-party/Nebulune-LICENSE.txt), with pinned attribution
at derived implementation sites. The build also includes third-party notices
in the playable JARs. No upstream assets, updater, telemetry, or websocket
client was imported.

## Comparisons and implemented changes

### Dagger swapping — Plus only

Nebulune commit
[`fddba105`](https://github.com/skies-starred/Nebulune/commit/fddba1054f799fa45134801244ae69eecf89bcdf)
fixes repeated attunement clicks by clearing the pending swap after one use.
Rot's existing implementation instead retried every two ticks until new item
NBT arrived. That could toggle the dagger back while the server update was
delayed.

`SlayerAutomationPolicy.DaggerSwapState.finishHeldDagger` adapts the single-use
completion rule. An already-correct mode requires no click; missing/invalid
mode data cancels the attempt rather than guessing. Pending operations expire
after 40 client ticks. Opening a screen or losing the original target cancels
the pending action. Only a classified Inferno boss/demon can arm it; an
unowned boss cannot arm it.

Nebulune's
[`0a85c7d`](https://github.com/skies-starred/Nebulune/commit/0a85c7d748b66762d92a01d014a8d4e82196b3b3)
added item cooldown checks to Auto Soulcry. Rot already performs those checks
and has an additional local four-second gate; those existing checks remain.
Attack-based Soulcry now also refuses an open screen or an unknown boss owner,
including when the optional other-boss setting is enabled.

### Local Slayer spawn ownership — shared observation

Athen's
[`c956ac0`](https://github.com/skies-starred/Athen/commit/c956ac0bed72d6264bcd641679c0a75714c679ff)
and subsequent
[GenericSlayerBossResolver.kt](https://github.com/skies-starred/Athen/blob/21ada452fd8f2085174b87e267fc2cc96683c016/src/main/kotlin/foo/starred/athen/api/slayers/resolver/base/GenericSlayerBossResolver.kt)
handle the newer protocol where an owner hologram may be unavailable. The
resolver recognizes `SLAYER BOSS! The <name> <Roman tier> spawned!` as the local
player's server spawn announcement.

Rot now recognizes the same exact announcement contract and correlates it with
recent entity-add packets. The new fallback only binds when all of these agree:

- Exact, formatting-stripped server line, known boss name and valid tier.
- One fresh living body of the matching family, added within 500 ms of the
  announcement. The metadata resolution expires after 1.5 seconds.
- A direct matching name/tier armor stand at body ID + 1, spatially attached
  to that body. Nearby names alone cannot establish the new ownership claim.
- Matching role/family/tier and no explicit conflicting owner.
- No second eligible boss or unclassified fresh body that could be the boss.

Duplicate delivery does not extend or reopen the announcement. World changes,
quest lifecycle messages and feature resets clear pending ownership state;
removed/dead bodies lose their cached binding. The existing `Spawned by` / owner
hologram path stays intact. Shared code observes identity only; action code
remains physically in Plus.

Rot deliberately does not copy the upstream player-facing-direction heuristic
or its outbound Slayer-sharing websocket. Ambiguous evidence leaves the boss
unowned and suppresses ownership-dependent automation.

### Existing behavior confirmed, not reimplemented

Athen
[`d0b92db`](https://github.com/skies-starred/Athen/commit/d0b92db457f6588da377c5ec2fcf275d9a048ed3)
fixes Tarantula T5 carry double-counting by accepting the final phase only.
Rot already recognizes Conjoined Brood and suppresses first-phase kill/carry
completion in `SlayerFightPolicy` / `SlayerSessionEngine`; existing regression
tests cover this. No duplicate fix was added.

Athen's newer classifier rejects skull-prefix-only combat labels. Rot retains
its existing supported inputs because its legacy Bloodfiend protocol fixtures
include such labels. The new ownership fallback still requires the direct
boss label, matching family and explicit server spawn message; broadening it
to narrative text was not part of this change.

## Remaining gaps and runtime checks

- Current alpha/Hypixel wire formats must be observed on the user's server;
  matching game versions does not prove identical protocol data.
- Ownership fallback is conservative: simultaneous fresh bodies, delayed
  packets, changed nameplate IDs or missing metadata may prevent a binding.
  This is preferable to claiming another player's boss.
- Rift Bloodfiend body identity is not verified for the new spawn protocol;
  its legacy owner-tag path remains. No new body-family fallback was guessed.
- Tarantula T5 second-phase and cocoon re-spawns need observed ownership
  evidence. Athen's new cocoon owner resolver and remote-boss websocket were
  not imported. Missing owner tags without a unique new local announcement
  still leave those entities unowned.
- Unknown dagger mode now requires a valid subsequent observation before
  automation can proceed; it does not blindly right-click an item.
- Verify direct item and nested `ExtraAttributes.td_attune_mode` updates, one
  use per attunement change, delayed NBT, screen open, target death and relog.
- Verify a normal local Slayer spawn with an owner tag, then one using the
  new announcement without an owner tag; verify another player's concurrent
  spawn stays unowned.
- Verify Tarantula phase 1/2 still yields one kill and one carry completion.
- Run both edition compilers, shared/Plus tests and `verifyLegitJar` after
  integration. Automated coverage is implemented; the integrating agent
  records the final build result separately.

New regression coverage is in `SlayerSpawnPolicyTest` (11 tests) and
`SlayerAutomationRefreshPolicyTest` (3 tests): spoofed/quoted announcements,
duplicates, ambiguity, metadata delay, expired/reused IDs, wrong tier/species,
explicit foreign ownership, world reset, one-shot dagger use, missing mode,
delay and stale pending expiry.
