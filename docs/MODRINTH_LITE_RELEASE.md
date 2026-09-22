# Rot Client Lite Modrinth release gate

Status (2026-09-23): **not ready to publish**. Edition separation, the
feature-by-feature server-rule audit, Minecraft runtime testing, and
code/asset provenance review remain open.

Upload only `RotClient-2.0.1+mc26.2.jar` to the Lite project. Rot Client+ has
different functionality and must remain a separate project and artifact.

## Edition boundary

The Lite JAR keeps normal Fabric client identification and excludes many
Plus-only automation, scanner, camera, and mixin classes covered by the release
verifier. The entire Dungeons catalog group and bundled Dungeons example
profile are Plus-only for now. Plus packages the full `DungeonRuntime` and
`DungeonPuzzlePolicy`; Lite packages small inert compatibility classes. The
puzzle-board solver and its four data files are absent from Lite. An older Plus
profile cannot activate the removed dungeon modules through Lite's catalog;
the reviewed Spirit Leap click overlay is also explicitly Plus-gated.

Lite still carries mixed policy classes such as `DungeonF7Policy` and
`DungeonGoldorPolicy`, plus shared configuration fields. The current verifier
does not establish that all unfair functionality has been removed. Do not
upload this build as a public "legit" release.

`verifyLegitJar` checks the built Lite artifact for forbidden Plus classes,
resources, identifiers, and service registrations. It complements code review;
it does not prove multiplayer-server approval or runtime correctness.

## Open release blockers

1. Audit the remaining mixed dungeon policies and configuration (`DungeonF7Policy`,
   `DungeonGoldorPolicy`, `DungeonAssistPolicy`, `DungeonLeftoverPolicy`, and
   related classes). Extract any remaining unfair algorithms or state-changing
   paths to Plus. Test old-profile paths after extraction. Restore individually
   reviewed passive HUD/map features to Lite later if desired.
2. Review other Lite features against multiplayer rules. The current Lite
   catalog still exposes Hotkey Macros and Chat Commands, fishing creature ESP
   and Thunder sparks, Trophy Fishing geyser and
   sponge boxes, Slayer Highlights, and other world overlays. Some may need
   Plus-only placement or removal; a depth-tested box is not automatically an
   allowed HUD. Review the compiled behavior as well as its settings.
3. Run separate Minecraft 26.2 smoke tests for Lite and Plus, including old
   profile migration, dashboard/HUD navigation, absence of Lite dungeon cards
   and preservation of the full Plus dungeon runtime,
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
storage, Slayer, fishing, foraging, mining, reconnect,
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

Rules checked 2026-09-23: [Modrinth Content Rules](https://modrinth.com/legal/rules),
[Modrinth AI disclosure guidance](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai),
and [Hypixel Allowed Modifications](https://support.hypixel.net/hc/en-us/articles/6472550754962-Hypixel-Allowed-Modifications).
