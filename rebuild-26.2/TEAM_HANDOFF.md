# Rot Client 26.2 team handoff

The rebuild is the active Minecraft 26.2 development line. It uses Java 25
and Fabric and builds independently from the original Plus and Lite project.
Use the [four-edition GitHub playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest-26.2-rebuild-dev.7)
and enable one Rot Client edition at a time.

| Edition | Download version | Role |
| --- | --- | --- |
| **Illegal** | `0.1.0-dev.7+mc26.2` | Primary test-server edition. Shared UI plus five modules. |
| **Legal** | `0.1.0-dev.5+mc26.2` | Published dashboard and HUD foundation for the next development track. |
| **Plus** | `2.0.11+mc26.2` | Original full client and implementation reference. |
| **Lite** | `2.0.11+mc26.2` | Original HUD/QoL edition and implementation reference. |

## What is in the rebuild

- The new dashboard uses the original Plus font, theme, tabs and category
  navigation. The search suggests modules and vanilla HUD settings.
- The HUD editor supports visibility, dragging, wheel scaling and restoring
  the vanilla position and size for supported elements. **Hide Status Effects**
  also covers the player inventory effects panel.
- Illegal includes Click GUI, Fullbright and Night, Auto Sprint, AutoCommands
  and Auto Loadout. AutoCommands settings and preview use the Rot theme.
  Auto Loadout provides twelve keybind slots, 1–8 tick equip and close delays,
  and 1–5 tick maximum delay variety.
- The owner confirmed the Auto Loadout switch and the menu-close fix in
  Illegal dev.7. This is evidence for that behavior only. The broader
  dashboard, HUD and Legal regression checks still need their own playtests.
- The Legal foundation uses the shared dashboard and HUD and omits the five
  Illegal modules and legacy trackers. It is a GitHub playtest, with a separate
  review needed before any Modrinth or CurseForge submission.

## Suggested ownership

**I’ll keep developing Illegal on the designated test server. Could you take
the lead on Legal?** A good first task is to test the Legal dashboard and HUD
with a clean config, especially HUD placement, reset, persistence after a
restart, search and Hide Status Effects in the inventory. Record the edition,
build, steps and actual observations. Then fix or add one reviewed Legal
feature at a time.

Use separate feature branches from `development` and coordinate when changing
the shared UI. Run the edition checks before merging. Review features
individually before moving any from Illegal to Legal.

## Source and build

Open `rebuild-26.2/`. Shared UI is in `src/main/`; the five Illegal modules
and their input hooks are in `src/illegal/`. Legal currently builds from the
shared source set. If Legal needs its own implementation later, introduce a
separate Legal source set for it. Keep edition settings isolated under
`config/rotclientillegal/` and `config/rotclientlegal/`.

From `rebuild-26.2/`, run `./gradlew build` on Linux/macOS or
`.\gradlew.bat build` on Windows. The build runs tests and `verifyEditions`.
Use **Right Shift** or **`/rot`** to open the dashboard. The
[rebuild guide](README.md) contains the full test steps and the current
toolchain requirements.
