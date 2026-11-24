# Rot Client rebuild for Minecraft 26.2

Rot Tools is rebuilding the dashboard and HUD editor before bringing modules back one at a time. The current local Illegal test candidate is `0.1.0-dev.6+mc26.2`. Choose **Illegal** for the first test on the designated test server. The [published GitHub playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest-26.2-rebuild-dev.5) contains dev.5 and the Plus and Lite reference builds.

| Edition in Prism | Scope of the first playtest |
| --- | --- |
| **26.2 Mod Client Illegal** | Primary development edition for the owner's SkyBlock test server. Dashboard, HUD editor, Click GUI, Fullbright and Night, Auto Sprint, AutoCommands and Auto Loadout. |
| **26.2 Mod Client Legal** | Dashboard and HUD editor. Legal module ports start after the Illegal dashboard and the relevant module have been tested. |

The dashboard reuses Rot Client+'s drawing code, UI font, dashboard dimensions, tab strip and scroll behavior. The HUD landing page has a Hide switch and Settings button for each supported vanilla HUD layer. Click a visible element in the world editor to select it, then drag it, scroll to resize, or use **Position → Default**. Elements that are temporarily absent have a labeled positioning marker when opened through Settings. Illegal saves interface settings in `config/rotclientillegal/interface.json` and Legal uses `config/rotclientlegal/interface.json`.

## Build from source

Use Minecraft **26.2**, **Java 25**, Fabric Loader **0.19.3 or newer** and Fabric API **0.155.2+26.2 or newer for Minecraft 26.2**. From the repository root:

```powershell
cd rebuild-26.2
.\gradlew.bat build
```

On Linux and macOS, run `./gradlew build` from the same directory. The build includes `verifyEditions`, which checks Fabric metadata, verifies the shared classes, and checks that Illegal classes and mixins stay out of the Legal JAR. The playable files are:

- `build/libs/RotClientIllegal-0.1.0-dev.6+mc26.2.jar`
- `build/libs/RotClientLegal-0.1.0-dev.6+mc26.2.jar`

## Prism playtest

1. Close Minecraft and open **26.2 Rot Client Playtest → Edit → Mods**.
2. Add the two candidate JARs if they are absent. Keep the existing Plus and Lite files in the instance.
3. Enable **one** Rot Client edition at a time. For the first pass, disable the old Plus and Lite entries and enable **26.2 Mod Client Illegal**. Fabric blocks combinations of editions.
4. Start Minecraft and open the dashboard with **Right Shift** or `/rot`. Use a local world or the owner's designated test server for the Illegal candidate.
5. Try tabs, the sidebar, module search, expandable cards, settings search, and Appearance controls. In the dashboard search, type **hotbar** and try both its suggestion and **Settings**. Check the Hide switch, drag and scroll on the hotbar, then try **Default**. Click another visible element to change selection. Restart to verify that each edition keeps its own layout.
6. Test the four starting features separately. Enable Click GUI's custom shortcut and check it opens and closes the dashboard. Check Fullbright and Night on and off, then assign an Auto Sprint key and verify one toggle per press. Enable AutoCommands in the module card and its popup in **Popup Style**. Add a shortcut and check that one press sends one command. Verify the warp feedback and popup appearance in the test environment.
7. Close Minecraft, switch to Legal, and repeat the dashboard and HUD checks. Its settings and available modules should be independent from Illegal.

Record the edition, JAR version, what you clicked, what you saw, and whether the result survived a restart. A Gradle build verifies the package contents. The in-game checklist verifies interaction and rendering.

## Development workflow

### Dev.6 Illegal playtest

- Open **Utilities → AutoCommands → Edit shortcuts and popup style**. Try the warp search, shortcut editor, popup sliders and movable preview. Saved bindings and popup colors carry over.
- Open **Utilities → Auto Loadout → Edit timing and keybinds**. Set equip and close delays from 1 to 8 ticks. Max delay variety from 1 to 5 adds zero to the selected maximum to each delay. Use **Keybinds** to assign or clear any of the 12 slots.
- On the designated test server, switch on Auto Loadout and press one assigned key outside menus. Check an available loadout, a slot that's already selected and what happens if you close or change the menu. If Ionic is installed, disable its own Auto Loadout while testing the Rot version.

1. Fix the dashboard and HUD based on the owner's Illegal playtest.
2. Add or restore one module in Illegal. Stabilize it and obtain the owner's in-game confirmation.
3. Review which parts suit Legal. Move or implement only those parts, then test them in Legal.
4. Start the next module after the current one has passed its applicable playtests.

The source branch is `codex/26.2-client-rebuild`. Shared UI lives under `src/main` and Illegal implementations under `src/illegal`. Treat old Plus and Lite as references when restoring a specific feature. The repository's root build remains the legacy distribution.

The GitHub playtest includes the rebuild JARs and the preserved 2.0.11 Plus and Lite reference builds. The rebuild includes a shared rendering hook that hides status effects in the HUD and the inventory. Test both views with an effect active and with **Hide Status Effects** on and off. Modrinth and CurseForge are later destinations for Legal after its release review.

## Source provenance

The original Rot Client UI code and assets come from the 26.2 Plus project. Both JARs include the original UI font and its OFL notice. AutoCommands uses the separately developed MIT-licensed AutoCommands implementation. Its notice is included in the Illegal JAR.

Auto Loadout's menu compatibility was reviewed against Starred's Ionic
`0.0.2+26.2` binary and the [official Loadouts notes](https://hypixel.net/threads/hypixel-skyblock-0-26-skyblock-resource-pack-loadouts-security-quest-and-more.6117801/).
Its Rot Client implementation is written independently.
