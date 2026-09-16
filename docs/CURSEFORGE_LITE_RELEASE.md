# Rot Client Lite CurseForge release gate

Status: **code and packaging validation passed; Minecraft runtime smoke testing
is still required before public publication**.

The current `2.0.1+mc26.2` build produces two separate playable editions:

- `RotClient-2.0.1+mc26.2.jar` — Rot Client Lite.
- `RotClientPlus-2.0.1+mc26.2.jar` — Rot Client+.

## Current edition separation

The repository keeps the required dual-edition architecture.

For the current Lite release candidate:

- Shared world overlays no longer contain direct `setAlwaysOnTop()` calls.
- Dungeon rendering no longer contains the old shared `throughWalls` path.
- Lite always respects normal depth/occlusion for reviewed shared ESP and
  waypoint rendering.
- Transparent dungeon-door replacement is implemented in Rot Client+ rather
  than Lite.
- Fog removal is Rot Client+ only.
- Eye-height modification is Rot Client+ only.
- Item-stack limit cancellation is Rot Client+ only.
- Fishing-hook synced-owner substitution is Rot Client+ only.
- Mining block-update cancellation is gated through the Plus flavor boundary.
- Local-player pose metadata filtering is gated through the Plus flavor
  boundary.
- Slayer background transparency remains shared.
- Network identity/concealment hooks remain removed. Both editions use normal
  Fabric client brand and registration behavior.

`verifyLegitJar` passes for the Lite artifact.

## Automated validation

The current candidate passes:

- Shared tests.
- Rot Client+ tests.
- Market Watch focused regression tests.
- `verifyLegitJar`.
- Dual-edition `build` and `check`.
- `git diff --check`.

Current playable artifact SHA-256 values:

- Rot Client Lite:
  `C32275B128470463DBBB5AC6B0E57460D32E88EEA815ACEDA0AB5E4C61DF548A`
- Rot Client+:
  `546A9B8B99BD0268DE01227873A73C1CA19D0E36D11317E7057EDDDCA7233414`

Automated validation proves packaging and code contracts. It does not replace
Minecraft runtime testing.

## Runtime validation before upload

Launch Minecraft 26.2 with **only Rot Client Lite enabled** and test at minimum:

- Startup with no mixin or resource-pack errors.
- `/rot` and the dashboard.
- Profile switching and legacy-profile migration.
- HUD editor and session HUDs.
- Market Watch Auction House and Bazaar pages.
- Storage overlay.
- Dungeon passive helpers and world overlays.
- Slayer highlights.
- Fishing and foraging highlights.
- Mining helpers.
- Reconnect and full client restart.
- Logs for mixin, rendering, networking, or configuration exceptions.

Then launch Rot Client+ separately and verify:

- Startup without duplicate mod/mixin errors.
- Plus-only settings load correctly.
- Representative Plus-only runtime behavior still works.
- Migrated Auto Clicker, Cheater Wardrobe, and Auto Superboom state remains
  intact.

## CurseForge publication checks

Before uploading the Lite JAR:

- Select Minecraft `26.2`.
- Select the correct Fabric loader compatibility.
- Declare Fabric API and other required dependencies accurately.
- Use version `2.0.1+mc26.2`.
- Include a meaningful changelog.
- Use an accurate project description and screenshots.
- Verify ownership/licensing for bundled assets.
- Do not claim affiliation with or endorsement by Hypixel.

CurseForge hosting approval and Minecraft server permission are separate
questions. Features used on multiplayer servers must still comply with that
server's rules. Rot Client's documentation should not imply that CurseForge
approval means Hypixel approval.

Rules checked 2026-09-16:
- CurseForge Moderation Policies.
- Hypixel Allowed Modifications.
