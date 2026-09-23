# Which JAR?

This repository builds **two** Fabric mods from the same source. They share
package `fi.rotclient`, author Rot Tools, icon, version `2.1.0+mc26.1.2`, and
the `/rot` command. They must not be **enabled** at the same time (each
`breaks` the other). You can keep both files in a Prism `mods/` folder and
enable the one you want.

| Edition | Display name | Fabric id | File | Catalog |
| --- | --- | --- | --- | --- |
| Lite | Rot Client | `rotclient` | `RotClient-2.1.0+mc26.1.2.jar` | **83** HUD / QoL parents |
| Automation | Rot Client+ | `rotclientplus` | `RotClientPlus-2.1.0+mc26.1.2.jar` | **135** parents (current full client) |

Skip `-sources.jar`, `-javadoc.jar`, and `-dev-unsigned.jar`. Verify
`SHA256SUMS.txt` when you download from GitHub.

## Rot Client (legit)

This is the HUD/QoL playtest edition. Trajectories, World Scanner, Mob
Highlight, the complete Dungeons catalog/runtime, Auto Sprint's input
override, and Terminal Click Trails are Plus-only. Etherwarp destination boxes respect block occlusion
in Lite, including when an older profile saved depth check off. Mixed dungeon
policy classes still require an edition audit before a public Lite release.

Older profiles retain moved Plus settings, some through shared compatibility
fields and others as opaque JSON, so switching editions does not erase them.
`verifyLegitJar` checks the packaged archive for the
implementation classes and identifiers already separated. Automated checks
do not replace a Lite Minecraft runtime playtest.

## Rot Client+ (automation)

This is the full 135-module client. Clickers, wardrobe automation, Ghosts,
Escrow Fix, Free Camera, Inventory Walk, auto terminals, Farm Keys, and the
other cheat-tagged options stay **opt-in and off by default**. Do not use
those features on Hypixel.

Plus settings persist in `rotclient-plus.json` so switching JARs in the same
instance does not mix cheat toggles into the legit `rotclient.json`.

## Playtest and releases

The 26.1.2 test build is published at the
[playtest](https://github.com/rot-tools/Rot-Client/releases/tag/playtest-26.1.2)
pre-release with the version-specific Lite JAR and `SHA256SUMS.txt`.
The `development` branch separately publishes 26.2 playtest JARs.
