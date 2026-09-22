# Rot Client Lite Modrinth release gate

Status (2026-09-22): **not ready to publish**. Edition separation, the
feature-by-feature server-rule audit, Minecraft runtime testing, and
code/asset provenance review remain open.

Upload only `RotClient-2.0.1+mc26.2.jar` to the Lite project. Rot Client+ has
different functionality and must remain a separate project and artifact.

## Edition boundary

The Lite JAR keeps normal Fabric client identification and excludes many
Plus-only automation, scanner, camera, and mixin classes covered by the release
verifier. Dungeon ESP, terminal, and puzzle catalog parents are Plus-only.
Terminal click solutions and the Ice Path and Tic Tac Toe solver policies and
world scans now compile only into Plus. An older Plus profile cannot activate
Reveal Hidden dungeon-map mode in Lite; Lite reads it as Explored. Older Plus
settings remain in parts of the shared configuration schema for compatibility.

The shared `DungeonRuntime` still contains other puzzle/terminal helpers and
the Lite JAR still contains mixed dungeon policy classes. The current verifier
does not establish that all unfair functionality has been removed. Do not
upload this build as a public "legit" release.

`verifyLegitJar` checks the built Lite artifact for forbidden Plus classes,
resources, identifiers, and service registrations. It complements code review;
it does not prove multiplayer-server approval or runtime correctness.

## Open release blockers

1. Finish physically separating the remaining terminal, puzzle, and F7
   solver/overlay code from shared classes, especially `DungeonRuntime`,
   `DungeonPuzzlePolicy`, `DungeonPuzzleBoardPolicy`, and mixed F7 helpers.
   Audit all old-profile paths after extraction.
2. Review other Lite features against multiplayer rules. The current Lite
   catalog still exposes Hotkey Macros and Chat Commands, Leap Menu number-key
   actions, fishing creature ESP and Thunder sparks, Trophy Fishing geyser and
   sponge boxes, Slayer Highlights, and other world overlays. Some may need
   Plus-only placement or removal; a depth-tested box is not automatically an
   allowed HUD. Review the compiled behavior as well as its settings.
3. Run separate Minecraft 26.2 smoke tests for Lite and Plus, including old
   profile migration, dashboard/HUD navigation, dungeon map and menus,
   reconnect, and restart. Automated tests are not runtime evidence.
4. Establish code and asset provenance before a Modrinth submission. Git
   commit authorship does not reveal whether code or art was AI-generated.
   The maintainer has not established the human-vs-AI contribution ratio.
   Disclose substantial AI use; Modrinth does not permit public projects whose
   content is primarily AI-generated. Do not use AI-created or AI-edited
   project-page images. Verify rights for the icon, resource-pack textures,
   screenshots, and all other bundled assets.
5. After the above, review the exact uploaded JAR, version metadata,
   dependencies, description, changelog, credits, and disclosures. Publish
   Rot Client+ only as a separate project if its content is accepted there.

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

Rules checked 2026-09-22: [Modrinth Content Rules](https://modrinth.com/legal/rules),
[Modrinth AI disclosure guidance](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai),
and [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications).
