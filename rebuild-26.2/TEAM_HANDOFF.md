# Team handoff for Minecraft 26.2

Updated 2026-11-26.

## Message for the collaborator

We are actively developing the Minecraft 26.2 rebuild of Rot Client. I will
continue implementing and playtesting the Illegal edition. Please take ownership
of the Legal edition.

Both editions share the dashboard, theme, search and HUD editor. Illegal currently
contains nine module parents. Legal dev.21 contains the shared UI/HUD foundation.
The previous Plus and Lite 2.0.11 builds remain available as references.

Start from development and open rebuild-26.2 as your VS Code project. Shared UI
and HUD code lives in src/main. Illegal implementation code and mixins live in
src/illegal. The root Gradle project is the earlier Plus/Lite build.

First, test Legal dev.21 with a clean configuration. Verify /rot and /rotclient,
search, category navigation, HUD visibility, dragging, scaling, Default, undo/redo,
inventory status-effect hiding and persistence. Record actual game results.

For each proposed Legal feature, review its behavior and suitability, implement
the appropriate version, and playtest it separately. A passing Illegal test alone
does not validate Legal. Coordinate shared UI edits and fetch before integration.

Use a codex/ feature branch from development. Run gradlew.bat build inside
rebuild-26.2 with Java 25. This runs the tests and verifyEditions. Merge reviewed
work back into development. Keep main for tested stable releases.

The latest development slice adds Auto Healing timing and missing-item guards,
and replaces the default Right Shift dashboard shortcut with /rot, /rotclient
and the optional Click GUI binding. The build passes 38 tests. The owner's
dev.20 timing test was positive. Dev.21's new missing-item and command behavior
still needs a focused gameplay pass.

[Downloads](https://github.com/rot-tools/Rot-Client/releases/tag/playtest-26.2-rebuild-dev.21) · [Feature list](../README.md) · [Roadmap](../docs/REBUILD_ROADMAP.md)

## Build and source map

| Location inside rebuild-26.2 | Purpose |
| --- | --- |
| src/main/java/fi/rotclient/rebuild | Shared dashboard, rendering, search, HUD configuration and entrypoint |
| src/illegal/java/fi/rotclient/rebuild/illegal | Illegal module implementations, settings and runtime hooks |
| src/test/java | Automated tests |
| build.gradle | Edition packaging and verifier |
| build/libs | Generated playable JARs |

Use the original resource ZIP with the optional mscBlackGlassPack Gradle property
when reproducing the owner's bundled local build. Public builds use the local
resource-pack fallback. [Build instructions](README.md)
