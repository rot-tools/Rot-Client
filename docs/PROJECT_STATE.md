# Current Rot Client project state

Updated 2026-11-26, Europe/Helsinki.

| Area | Current state |
| --- | --- |
| Active version | Minecraft 26.2 |
| Main development edition | Rot Client Illegal dev.21 |
| Legal edition | Shared dashboard and HUD foundation, dev.21 |
| Previous client line | Plus and Lite 2.0.11 preserved as references |
| Integration branch | development |
| Working feature branch | codex/26.2-client-rebuild |
| Build | Java 25, Gradle 9.5.1, Fabric Loader 0.19.3+, Fabric API 0.155.2+26.2 |
| Validation | 38 automated tests and edition checks pass |
| Publication | [Current playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest-26.2-rebuild-dev.21) |

Illegal contains nine module parents plus the shared dashboard and 14-layer
HUD editor. [The README lists each feature](../README.md).

The owner reported working Auto Healing timing in dev.20. The dev.21 missing-item
guard and dashboard-entry changes have automated verification and await a focused
gameplay check. Other owner-reported checks are listed in the README.

The next phase is to test the latest slice, fix reported issues, then restore
one selected feature at a time. The owner works on Illegal and the collaborator
on Legal. [Team workflow](../rebuild-26.2/TEAM_HANDOFF.md).

[Current roadmap](REBUILD_ROADMAP.md) · [Previous Plus/Lite state](archive/PLUS_LITE_PROJECT_STATE.md)
