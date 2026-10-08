# Mining and Pet HUD repairs — 2026-10-01

This is the historical first mining checkpoint. Subsequent 26.3-only
[mining observation repairs](MINING_STATE_FIXES_2026-10-08.md) supersede the
timer/SkyMall/helper lifecycle details below. Earlier test counts and version
ports describe that checkpoint, not the current maintenance target.

These changes are compile-tested and unit-tested. They still require a Minecraft/Hypixel playtest in each supported version and edition. They do not establish Modrinth approval.

## Shared repairs (Lite and Plus)

- Pet HUD reads the cached complete tab snapshot, including actual player-list widget rows. Previously it read only header/footer and scoreboard. It accepts inline/split Pet widgets and observed XP fractions or percentages. Current observations replace cached progress; pet switches discard the previous pet's XP/held-item data, and level changes invalidate stale progress. No earned XP is invented when Hypixel supplies no progress row.
- Commission Display accepts split name/status rows within the commission widget. Commission observation also runs when the mob helper is enabled without the Display HUD. Mob highlights require a matching unfinished commission, a mining-area context, and line of sight. Nearby visible mob bodies are preferred over floating armor-stand nametags. Markers remain depth-tested.
- SkyMall accepts `New buff:` chat, inline/split tab widgets, and explicit current-perk lore from the HOTM menu. The HUD shows an honest unknown state until a perk is observed. A persistent screen hint renders over the HOTM screen rather than relying on a two-second world title. Observations expire after twenty minutes; joining another world clears them.
- Mining Helpers → **Dwarven Area Waypoints** displays 22 static public landmark/NPC names and distances in the Dwarven Mines, including The Forge. No hidden entity or ore scanning is involved. Additional modern areas (for example Fossil Research Center, Abandoned Quarry and Gates to the Mines) need verified anchors before claiming complete area coverage.
- The optional Hypixel Mod API bridge now supports the official `ClientboundPacketHandler` callback interface as well as a historical `Consumer` signature. The old reflection signature could silently fail registration and fall back to the scoreboard.

## Plus-only addition

Mining Helpers → **Puzzler Helper** interprets explicit NPC arrow sequences, outlines the target block, clears after another NPC response/world change, and expires after two minutes. It never mines or clicks for the player. Implementation, settings and catalog entry are physically Plus-owned; Lite's verifier checks the Plus class boundary.

## Remaining evidence and requests

- **Pickobulus now has a dedicated shared timer HUD, on/off settings, configurable fallback cooldown, observed-lore/server status, one-shot ready popup/sound, and a read-only projection of material breaks already accepted as Pickobulus by the tracker. Plus adds optional estimated candidate outlines/counts, radius/range/shape/through-wall switches and outline color. No preview credits a ledger. Exact Pickobulus preview is not implemented.** The server does not provide its future broken-block list. Radius, projectile collision and block eligibility need current in-game evidence before a prediction can be called exact. Older descriptions disagree about cube/sphere geometry. Do not ship an unverified footprint labelled exact.
- Pet XP requires the server's current XP/progress widget or a fresh pet-menu observation. Test with non-max-level pets; do not treat ordinary skill XP as pet XP without skill, rarity, item and perk multipliers.
- Test both commission HUD toggles, completion/claim/new commission, mob identity and occlusion, pet switch/level-up, SkyMall rollover, HOTM reopening, and waypoint island transitions.
- Trello's supplied card was not publicly readable during the audit. The owner's descriptions remain the bug-report source.

## Reference comparison and API audit

Original Rot implementations use observed game formats and public location facts; no upstream runtime implementation was copied.

- [Skytils MiningFeatures](https://github.com/Skytils/SkytilsMod/blob/1.x/src/main/kotlin/gg/skytils/skytilsmod/features/impl/mining/MiningFeatures.kt): Puzzler origin/orientation and NPC-arrow input behavior.
- [NEU DwarvenMinesWaypoints](https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/java/io/github/moulberry/notenoughupdates/miscfeatures/DwarvenMinesWaypoints.java): public landmark coordinates. [NEU SkyMallDisplay](https://github.com/NotEnoughUpdates/NotEnoughUpdates/blob/master/src/main/kotlin/io/github/moulberry/notenoughupdates/guifeatures/SkyMallDisplay.kt): `New buff:` observation.
- [SkyHanni CurrentPetApi](https://github.com/hannibal002/SkyHanni/blob/beta/src/main/java/at/hannibal2/skyhanni/api/pet/CurrentPetApi.kt): Pet identity/source priority and delayed tab assertion after GUI selection.
- [NoFrills CommissionHighlight](https://github.com/WhatYouThing/NoFrills/blob/main/src/main/java/nofrills/features/mining/CommissionHighlight.java): completed commissions are identified from GUI lore, not an HTTP endpoint.
- [SBA PetManager](https://github.com/Fix3dll/SkyblockAddons/blob/26.3/src/main/java/com/fix3dll/skyblockaddons/features/PetManager.java): menu/item-based pet identity and level observations. Existing client implementations do not prove exact current pet XP from ordinary skill XP.
- [Official Hypixel ModAPI](https://github.com/HypixelDev/ModAPI): checked current callback and location getter signatures. The repository's latest commit at audit time was `5245ef1f` (2026-03-24). No Minecraft-version-specific REST change was found.
- [Official REST documentation](https://api.hypixel.net/): live read-only checks of Bazaar, item resources, collection resources, skill resources and auction page 0 all returned HTTP 200 and `success:true` on 2026-10-01. This verifies availability and top-level schema only; it is not an exhaustive audit of every market item.

## Suggested later Dwarven QoL work

1. Commission location waypoints using the same static landmark registry, with user-selected targets and completed-task cleanup.
2. Forge queue completion timers from observed Forge menu entries, including restart-safe timestamps and explicit unknown states.
3. Daily commission bonus/HOTM progress HUD from actual menu/tab evidence.
4. Powder gain summaries and ability cooldowns based on observed server messages, integrated with canonical Current Session accounting.
5. Drill fuel warnings, Pickaxe Ability ready reminders, SkyMall rollover notices and user-created routes between public landmarks.

Keep automation, hidden-block scans and unverified server predictions out of Lite.

The catalog is now 84 Lite / 136 Plus parents. Pickobulus material-block statistics project existing accepted packet evidence; gemstone AoE, non-target ability attribution, exact server geometry and cooldown multipliers still require controlled runtime work. Existing Sack/Pristine quantities remain authoritative.

## Automated checkpoint

26.2: `build` passed with 2,429 shared tests and 208 Plus tests (zero failures/errors/skips), both client compilers, `verifyLegitJar` and `verifyDungeonJarBoundary`. The version ports are validated separately.
