# 26.3 mining observation fixes — 2026-10-08

This continues the [mining HUD work](MINING_HUD_FIXES.md), for Minecraft 26.3
Lite and Plus only. It repairs observation freshness and helper lifecycle.
Canonical Current Session, mining quantities and History remain unchanged.

## Source evidence

Direct official wiki Puzzler/HOTM URLs now redirect to Hypixel's
[July 2026 closure announcement](https://hypixel.net/threads/end-of-the-official-hypixel-wiki-july-2026.6112020/).
Cached search excerpts are historical; they cannot establish exact current
Pickobulus geometry or perk-adjusted cooldowns. Feature-specific comparisons
use pinned source formats and existing Rot fixtures, with original Rot
integration. No new exact physics claim or copied upstream assets is added.

## Validation boundary

Real server countdowns, SkyMall rollover, HOTM reopening, Puzzler NPC responses
and preview context changes require Minecraft testing. Preview counts do not
credit any mining ledger. Gameplay remains Ready for Runtime Test after
automated checks.

## Shared fixes (Lite and Plus)

- Identical cached Pickobulus countdown text no longer restarts its deadline.
  Server observations and locally projected readiness are distinguished. A
  just-observed use cannot immediately finish from the READY value cached
  before that use. Ready popups/sounds are bounded and emitted once.
- Pickobulus cooldown lore belongs to its explicit ability block, including
  the observed `⦾` header form. A different ability's cooldown is not borrowed.
  Disabling the module, changing world/profile/connection or moving the clock
  backwards clears its timer, last-use projection and popup. See the
  [Pickobulus lifecycle audit](PICKOBULUS_LIFECYCLE_AUDIT.md).
- HUD editor visibility remains configuration-based, so Pickobulus can be
  selected and dragged without initializing a live client. Runtime content
  reads enforce context separately; the enabled HUD also appears in the
  editor's element list.
- Last-use counts project the existing accepted break callbacks in their
  bounded window. A tracker selection change makes the target subtotal
  explicitly partial; no item quantity or session ledger is mutated.
- Sky Mall reads `Your Current Effect`/bullet/wrapped HOTM lore, rejects
  unrelated HOTF buffs and stops at other widget boundaries. Unchanged tab
  text and unchanged lore in the same menu do not refresh observation age.
  Day/reset/disabled evidence and world/profile changes invalidate it. HOTM
  hints can observe the perk independently of the HUD toggle. See the
  [HOTM observation audit](MINING_HOTM_OBSERVATIONS_2026-10-08.md).

## Plus fixes

Puzzler uses the observed ten-arrow NPC challenge and retains the reviewed
origin/orientation. Its target expires, clears across context changes and is
drawn as a depth-tested tile above the floor face. No client-world block is
replaced to display the answer.

The estimated Pickobulus footprint invalidates on context/aim changes, scans
only loaded blocks under a fixed budget and rejects overflowing coordinate
inputs. The selected tracker is captured for the scan; gemstone selection
does not inherit the material fallback to Gold. Ambiguous mining blocks use
the existing area evidence rules. Candidate counts remain estimates, not a
complete list of server-approved breaks or quantities.

See the [Puzzler and preview audit](UPSTREAM_MINING_PREVIEW_AUDIT_2026-10-08.md)
for source formats, candidate boundaries and unsupported locations.

Puzzler and the preview remain physically Plus-only. No new catalog parent,
automation, API endpoint or upstream implementation is added to Lite.

## Manual validation still required

Automated checkpoint: Java 25 `build` passes, including client compilation,
2,508 shared + 251 Plus tests, `verifyLegitJar`, `verifyDungeonJarBoundary` and
`check`; no test failures, errors or skips. `git diff --check` passes. The
prior baseline was 2,481 + 236 tests; this batch adds 42 net regressions,
including the no-runtime HUD-editor test. Version: `2.0.4+mc26.3`.

1. Test both 26.3 editions separately: startup, old-profile loading, dashboard
   and HUD editing, disable/re-enable, reconnect and profile switch. Check
   logs for exceptions and verify Plus-only controls remain absent from Lite.
2. Test Pickobulus with its actual tool lore and enabled ability tab widget.
   Verify a static cached countdown finishes without restarting, a fresh
   countdown corrects the estimate, READY before use is ignored, and a real
   ready message/pop-up occurs once. Compare the last-use projection with
   accepted tracker observations; include pause and target-switch cases.
3. Observe real Sky Mall current lore, wrapped Goblin text, a natural day
   rollover, HOTM reset/disabled state, hint-only mode and menu reopening.
   Unchanged cached text is not proof of a new selection or remaining buff
   duration.
4. In Plus, speak to Puzzler and check the marked floor tile, expiry and
   world/profile/module cleanup. Aim the Pickobulus preview while switching
   tools, areas, settings and material/gemstone targets. Calibrate actual
   ability geometry, cooldown modifiers and server break observations before
   making any exact footprint or complete AoE-accounting claim.

This maintenance batch does not resolve all remaining upstream modules or the
public [Lite release gate](MODRINTH_LITE_RELEASE.md). Earlier runtime evidence
does not automatically validate these new paths.
