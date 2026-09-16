# Which JAR?

This repository builds **two** Fabric mods from the same source. They share
package `fi.rotclient`, author Rot Tools, icon, version `2.0.1+mc26.2`, and
the `/rot` command. They must not be **enabled** at the same time (each
`breaks` the other). You can keep both files in a Prism `mods/` folder and
enable the one you want.

| Edition | Display name | Fabric id | File | Catalog |
| --- | --- | --- | --- | --- |
| Lite | Rot Client | `rotclient` | `RotClient-2.0.1+mc26.2.jar` | **96** HUD / QoL parents |
| Automation | Rot Client+ | `rotclientplus` | `RotClientPlus-2.0.1+mc26.2.jar` | **134** parents (current full client) |

Skip `-sources.jar`, `-javadoc.jar`, and `-dev-unsigned.jar`. Verify
`SHA256SUMS.txt` when you download from GitHub.

## Rot Client (legit)

This is the HUD/QoL playtest edition. Trajectories, World Scanner, and Mob
Highlight, Auto Sprint's input override, and Terminal Click Trails are Plus-only. Etherwarp destination boxes respect block occlusion
in Lite, including when an older profile saved depth check off. Dungeon
ESP/solvers still require an edition audit before a public Lite release.

Older profiles retain moved Plus settings as opaque JSON so switching editions
does not erase them. `verifyLegitJar` checks the packaged archive for the
implementation classes and identifiers already separated. Automated checks
do not replace a Lite Minecraft runtime playtest.

## Rot Client+ (automation)

This is the full 134-module client. Clickers, wardrobe automation, Ghosts,
Escrow Fix, Free Camera, Inventory Walk, auto terminals, Farm Keys, and the
other cheat-tagged options stay **opt-in and off by default**. Do not use
those features on Hypixel.

Plus settings persist in `rotclient-plus.json` so switching JARs in the same
instance does not mix cheat toggles into the legit `rotclient.json`.

## Playtest and releases

Each green push to `development` replaces the
[playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest)
pre-release with **both** JARs and `SHA256SUMS.txt`. Versioned GitHub
Releases do the same.
