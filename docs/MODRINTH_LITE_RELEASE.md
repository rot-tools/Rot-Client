# Rot Client Lite Modrinth release gate

Status: **edition separation remains in progress; Minecraft runtime smoke testing
and final project-page review are required before publication**.

Upload only `RotClient-2.0.1+mc26.2.jar` to the Lite project. Rot Client+ has
different functionality and must remain a separate project and artifact.

## Edition boundary

The Lite JAR keeps normal Fabric client identification and excludes the
Plus-only automation, scanners, extended visibility, dungeon ESP, terminal and
puzzle solvers, block-rewrite, packet filtering, camera, fog, item-stack, and
fishing-hook mixins covered by the release verifier and edition contracts. The dashboard also omits
controls whose runtime exists only in Plus. Older Plus settings remain in parts
of the shared configuration schema for profile compatibility. The moved dungeon
parents and reviewed F7 solver settings are hidden and gated in Lite, but the
shared dungeon runtime still needs a physical bytecode split and review before
the Lite artifact can be considered release-ready.

`verifyLegitJar` checks the built Lite artifact for forbidden Plus classes,
resources, identifiers, and service registrations. It complements code review;
it does not prove multiplayer-server approval or runtime correctness.

## Validation before upload

Run the shared and Plus tests, `check`, `clean build`, and `git diff --check`
with Java 25. Verify the SHA-256 values of the exact uploaded files.

Launch Minecraft 26.2 with only Lite enabled and test startup, `/rot`, dashboard
navigation, profile migration and switching, HUD editing, Market Watch,
storage, passive dungeon helpers, Slayer, fishing, foraging, mining, reconnect,
and a full restart. Inspect the log for mixin, rendering, networking, and
configuration exceptions. Launch Plus separately and verify representative
Plus-only controls and migrated settings.

## Project-page checks

- Use the correct Minecraft version, Fabric loader, dependencies, license,
  version number, changelog, credits, and Rot Tools ownership.
- Describe Lite behavior accurately and do not imply Hypixel endorsement.
- Complete all applicable content and generative-AI disclosures accurately.
- Do not use AI-generated project or gallery images.
- Verify the rights and attribution for every bundled asset.
- Keep Rot Client+ out of the Lite project because the editions have different
  functionality.

Rules checked 2026-09-16: [Modrinth Content Rules](https://modrinth.com/legal/rules),
[Modrinth AI disclosure guidance](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai),
and [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications).
