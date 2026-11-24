# Rot Client rebuild for Minecraft 26.2

Rot Tools is rebuilding the dashboard and HUD editor before bringing modules back one at a time. The first local playtest is `0.1.0-dev.1+mc26.2`. Playtest reports from the owner decide when builds can be published.

| Edition in Prism | Scope of the first playtest |
| --- | --- |
| **26.2 Mod Client Illegal** | Primary development edition for the owner's SkyBlock test server. Dashboard, HUD editor, Click GUI, Fullbright and Night, Auto Sprint and AutoCommands. |
| **26.2 Mod Client Legal** | Dashboard and HUD editor. Legal module ports start after the Illegal dashboard and the relevant module have been tested. |

The dashboard starts from the existing Rot Client+ palette and category names. The sidebar, search, Appearance controls, and HUD editing and preview are rebuilt for this project. Use the HUD editor to arrange a preview and choose which supported vanilla HUD layers show in game. The category list gives future modules a place to go. The former trackers and their overlays, statistics, and settings are outside this first build. Illegal saves interface settings in `config/rotclientillegal/interface.json` and Legal uses `config/rotclientlegal/interface.json`.

## Build from source

Use Minecraft **26.2**, **Java 25**, Fabric Loader **0.19.3 or newer** and Fabric API **0.155.2+26.2 or newer for Minecraft 26.2**. From the repository root:

```powershell
cd rebuild-26.2
.\gradlew.bat build
```

On Linux and macOS, run `./gradlew build` from the same directory. The build includes `verifyEditions`, which checks Fabric metadata, verifies the shared classes, and checks that Illegal classes and mixins stay out of the Legal JAR. The playable files are:

- `build/libs/RotClientIllegal-0.1.0-dev.1+mc26.2.jar`
- `build/libs/RotClientLegal-0.1.0-dev.1+mc26.2.jar`

## Prism playtest

1. Close Minecraft and open **26.2 Rot Client Playtest → Edit → Mods**.
2. Add the two candidate JARs if they are absent. Keep the existing Plus and Lite files in the instance.
3. Enable **one** Rot Client edition at a time. For the first pass, disable the old Plus and Lite entries and enable **26.2 Mod Client Illegal**. Fabric blocks combinations of editions.
4. Start Minecraft and open the dashboard with **Right Shift** or `/rot`. Use a local world or the owner's designated test server for the Illegal candidate.
5. Try the sidebar, search, category changes, and Appearance controls. Open the HUD editor, move a preview element, change its size or visibility, then close and reopen the editor. Restart and check that preferences were saved.
6. Test the four starting features separately. Enable Click GUI's custom shortcut and check it opens and closes the dashboard. Check Fullbright and Night on and off, then assign an Auto Sprint key and verify one toggle per press. Enable AutoCommands in the module card and its popup in **Popup Style**. Add a shortcut and check that one press sends one command. Verify the warp feedback and popup appearance in the test environment.
7. Close Minecraft, switch to Legal, and repeat the dashboard and HUD checks. Its settings and available modules should be independent from Illegal.

Record the edition, JAR version, what you clicked, what you saw, and whether the result survived a restart. A Gradle build verifies the package contents. The in-game checklist verifies interaction and rendering.

## Development workflow

1. Fix the dashboard and HUD based on the owner's Illegal playtest.
2. Add or restore one module in Illegal. Stabilize it and obtain the owner's in-game confirmation.
3. Review which parts suit Legal. Move or implement only those parts, then test them in Legal.
4. Start the next module after the current one has passed its applicable playtests.

The source branch is `codex/26.2-client-rebuild`. Shared UI lives under `src/main` and Illegal implementations under `src/illegal`. Treat old Plus and Lite as references when restoring a specific feature. The repository's root build remains the legacy distribution.

The first pair of rebuild JARs is for local playtesting. GitHub receives the source and this build guide. GitHub downloads wait for the owner's confirmation. Modrinth and CurseForge are later destinations for Legal after its release review.

## Source provenance

The rebuild uses the original Rot Client project's visual identity, category names, and selected 26.2 features. The existing implementation is in the repository's root `src` tree. AutoCommands uses the separately developed MIT-licensed AutoCommands implementation. Its notice is included in the Illegal JAR.
