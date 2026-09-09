# Which JAR?

This repository builds **two** Fabric mods from the same source. They share
package `fi.rotclient`, author Rot Tools, icon, version `2.0.1+mc26.2`, and
the `/rot` command. They must not be **enabled** at the same time (each
`breaks` the other). You can keep both files in a Prism `mods/` folder and
enable the one you want.

| Edition | Display name | Fabric id | File | Catalog |
| --- | --- | --- | --- | --- |
| Legit | Rot Client | `rotclient` | `RotClient-2.0.1+mc26.2.jar` | **110** HUD / QoL parents |
| Automation | Rot Client+ | `rotclientplus` | `RotClientPlus-2.0.1+mc26.2.jar` | **131** parents (current full client) |

Skip `-sources.jar`, `-javadoc.jar`, and `-dev-unsigned.jar`. Verify
`SHA256SUMS.txt` when you download from GitHub.

## Rot Client (legit)

Use this when you want overlays, HUDs, waypoints, visual ESP, trackers, Term
Sim, World Scanner, puzzle/terminal **overlays**, Storage/Inventory, Custom
Scoreboard, Fullbright, loadouts, and wardrobe/pet **keybinds**. Automation
and cheat code is **not in the bytecode**.

## Rot Client+ (automation)

This is the previous 131-module client. Clickers, Free Camera, Inventory Walk,
auto terminals, Farm Keys, and the other cheat-tagged options stay **opt-in
and off by default**. Do not use those features on Hypixel.

Plus settings persist in `rotclient-plus.json` so switching JARs in the same
instance does not mix cheat toggles into the legit `rotclient.json`.

## Playtest and releases

Each green push to `development` replaces the
[playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest)
pre-release with **both** JARs and `SHA256SUMS.txt`. Versioned GitHub
Releases do the same.
