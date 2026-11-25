# Rot Client rebuild for Minecraft 26.2

Rot Tools is rebuilding the dashboard and HUD editor before bringing modules back one at a time. The current source build is `0.1.0-dev.13+mc26.2`. Auto Clicker recognizes Hypixel by default. The owner confirmed Daily Reward Claim in dev.12 and Auto Clicker in dev.13. [Illegal dev.13 is published on GitHub](https://github.com/rot-tools/Rot-Client/releases/tag/playtest-26.2-rebuild-dev.13). See [edition downloads](../docs/WHICH_JAR.md) for Legal dev.5 and the Plus and Lite references. See the [team handoff](TEAM_HANDOFF.md) for responsibilities and first tasks.

| Edition in Prism | Scope of the first playtest |
| --- | --- |
| **26.2 Mod Client Illegal** | Primary development edition for the owner's SkyBlock test server. Dashboard, HUD editor, Click GUI, Fullbright and Night, Auto Sprint, AutoCommands, Auto Loadout, Auto Clicker and Daily Reward Claim. |
| **26.2 Mod Client Legal** | Dashboard and HUD editor. Legal module ports start after the Illegal dashboard and the relevant module have been tested. |

The dashboard reuses Rot Client+'s drawing code, UI font, dashboard dimensions, tab strip and scroll behavior. The HUD landing page has a Hide switch and Settings button for each supported vanilla HUD layer. Click a visible element in the world editor to select it, then drag it, scroll to resize, or use **Position → Default**. Elements that are temporarily absent have a labeled positioning marker when opened through Settings. Illegal saves interface settings in `config/rotclientillegal/interface.json` and Legal uses `config/rotclientlegal/interface.json`.

## Build from source

Use Minecraft **26.2**, **Java 25**, Fabric Loader **0.19.3 or newer** and Fabric API **0.155.2+26.2 or newer for Minecraft 26.2**. From the repository root:

```powershell
cd rebuild-26.2
.\gradlew.bat build
```

On Linux and macOS, run `./gradlew build` from the same directory. The build includes `verifyEditions`, which checks Fabric metadata, verifies the shared classes, and checks that Illegal classes and mixins stay out of the Legal JAR. The playable files are:

- `build/libs/RotClientIllegal-0.1.0-dev.13+mc26.2.jar`
- `build/libs/RotClientLegal-0.1.0-dev.13+mc26.2.jar` (source build). The published Legal playtest is dev.5.

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

### Dev.13 Auto Clicker playtest

- With **Daily Reward Claim** enabled, open the Daily Reward from the server menu. The picker uses the official link from the book or the matching system message. Verify that an ordinary book and the **Open book** fallback still open normally.
- Bind the **Left key** to the left mouse button and **Right key** to the right mouse button. Hypixel and singleplayer are eligible by default. An additional test server can be set under **Customisation → Server and item filters**. Enable the module and the side you want to test. Check rate, release and native Attack and Use with the module off. The Ionic-style CPS accumulator, jitter and block hold are used here as well.

### Dev.10 Daily Reward Claim playtest

- Turn on **Interface → Daily Reward Claim** and trigger an official daily reward link. Confirm the cards show icons, titles and rarity. Compare **Streak** and **Best** with the official page.
- Select a reward and press **Claim** on a page that allows claiming in-game. When the page requires a browser step, use **Open website**. The original chat link remains available. The module is off by default.

### Dev.9 Auto Clicker playtest

- Open **Combat → Auto Clicker → Clicker settings**. Open **Left clicker** or **Right clicker**, assign an unused keyboard key or mouse button, and select 5–20 CPS. Under **Customisation**, set CPS jitter to 1–3 for Ionic-style variation or 0 for a fixed rate.
- Under **Customisation → Server and item filters**, you can enter an extra test server address, including its port when present. Enable Auto Clicker and the side you want to test.
- Hold a binding in the world and release it. Open a menu or switch window focus while holding it, then return to the game and verify a fresh press is required. Check block breaking, separate item filters, and the Dungeon Breaker guard. If Ionic is installed, turn off its Auto Clicker during this test.
- **Allow breaking blocks** switches to continuous vanilla Attack when a solid block is targeted. Turning it off keeps ordinary click pulses on block targets. Left and right whitelist entries match an item UUID, ID or exact display name.
- **Terminator only**, under **Server and item filters**, replaces the two regular bindings with repeated left clicks while holding vanilla Use with an item whose SkyBlock ID is `TERMINATOR`. It uses the left CPS setting.

### Dev.7 Illegal playtest

- Open **Utilities → AutoCommands → Edit shortcuts and popup style**. Try the warp search, shortcut editor, popup sliders and movable preview. Saved bindings and popup colors carry over.
- Open **Utilities → Auto Loadout → Edit timing and keybinds**. Set equip and close delays from 1 to 8 ticks. Max delay variety from 1 to 5 adds zero to the selected maximum to each delay. Use **Keybinds** to assign or clear any of the 12 slots.
- On the designated test server, switch on Auto Loadout and press one assigned key outside menus. Check an available loadout, a slot that's already selected and what happens if you close or change the menu. If Ionic is installed, disable its own Auto Loadout while testing the Rot version.
- The owner confirmed the Auto Loadout switch and automatic menu close in dev.7. The close step follows briefly refreshed Loadouts menus and never sends another equip click.

1. Fix the dashboard and HUD based on the owner's Illegal playtest.
2. Add or restore one module in Illegal. Stabilize it and obtain the owner's in-game confirmation.
3. Review which parts suit Legal. Move or implement only those parts, then test them in Legal.
4. Start the next module after the current one has passed its applicable playtests.

The integration branch is `development`. Shared UI lives under `src/main` and Illegal implementations under `src/illegal`. Use separate feature branches for Illegal and Legal work, and coordinate changes to the shared UI. Treat old Plus and Lite as references when restoring a specific feature. The repository's root build remains the legacy distribution.

The GitHub playtest includes the rebuild JARs and the preserved 2.0.11 Plus and Lite reference builds. The rebuild includes a shared rendering hook that hides status effects in the HUD and the inventory. Test both views with an effect active and with **Hide Status Effects** on and off. Modrinth and CurseForge are later destinations for Legal after its release review.

## Source provenance

The original Rot Client UI code and assets come from the 26.2 Plus project. Both JARs include the original UI font and its OFL notice. AutoCommands uses the separately developed MIT-licensed AutoCommands implementation. Its notice is included in the Illegal JAR.

Auto Loadout's menu compatibility was reviewed against Starred's Ionic
`0.0.2+26.2` binary and the [official Loadouts notes](https://hypixel.net/threads/hypixel-skyblock-0-26-skyblock-resource-pack-loadouts-security-quest-and-more.6117801/).
Its Rot Client implementation is written independently.

Auto Clicker uses the old 26.2 Plus behavior as an in-repository reference.
Starred's Ionic `0.0.2+26.2` binary was reviewed for its two independent click
rates, tick accumulator, block mode and item filters. The rebuild implements
those behaviors in Java using its own settings and Minecraft's input loop.
