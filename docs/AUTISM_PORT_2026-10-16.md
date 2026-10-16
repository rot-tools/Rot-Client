# AUTISM Client utilities port for 26.3

`2.0.11+mc26.3` adds eight opt-in module surfaces to Rot Client+. Lite keeps
84 catalog parents; Plus has 144. These are adapted implementations from the
owner-provided AUTISM Client 5.0 source snapshot, with explicit GPL origin
comments. The entire upstream client is not imported.

## Implemented in this checkpoint

| Plus module | Settings and behavior | Limits |
| --- | --- | --- |
| SafeWalk | Look-down threshold, ledge distance, min/max hold; collision probe and three-clear-tick release | On foot, on ground; no GUI/Free Camera |
| Parkour | One-tick jump at a predicted ledge while moving | On foot, not sneaking/flying/riding; physical jump preserved |
| Sneak | Vanilla input or outgoing packet mode | Transition release, no per-tick packet flood; reset on context loss |
| NoInteract | Block mining/use and entity use/attack lists, whitelist/blacklist, hand, named/baby filters | Normal game-mode actions and direct client-thread packets; no world reads on network workers. The upstream unused Friends flag is not exposed |
| AutoTool (Hotbar) | Delay, durability, Silk Touch, sneak requirement, switch-back and restore delay | Nine hotbar slots only; manual slot changes cancel ownership until release. Inventory swaps and upstream randomized delay remain pending |
| AutoSign | Four text lines, existing-text opt-in, automatic Done | Pixel-width trim; normal editor submission on the next tick. Market automation owns its sign entry |
| FreeLook | Front/back perspective, pitch limits, hold/toggle key | Enable module to arm a hold bind; unbound uses dashboard toggle. Free Camera takes precedence; original perspective restored on release/context loss |
| AirPlace | Range, guide, fill and color; use held main-hand block or offhand block with empty main hand | One normal game-mode placement attempt per four ticks; no mining/GUI/Free Camera/market conflict. Loaded replaceable target only. Server acceptance is authoritative |

Search `autism` in the Plus dashboard to find these modules. Every parent is
off and every bind is empty by default. Settings persist in the existing
opaque Plus config slot. Reset touches only the selected module. None of
these paths pre-credits Mining Tracker quantities.

The shared dashboard also stops applying the 24-character display-name limit
to every text field; registry lists/macros can use up to 2048 UTF-16 units.
Name Hider keeps its original limit. Fractional numeric slider steps remain
visible in the readout, including AirPlace range and SafeWalk edge distance.

## License and corresponding source

The original/Lite source remains MIT. The **combined Plus distribution** from
this checkpoint is **GPL-3.0-only**. Full GPL text, original MIT notice, upstream
credits and source hashes are packaged in Plus. GPL implementation/classes
and GPL notice resources are absent from the Lite JAR. See [third-party
notices](../THIRD_PARTY.md) and `licenses/plus/SOURCE.json`.

The corresponding modified implementation, resources and Gradle build scripts
are in the Git tag of each release; GitHub's source archive contains them.
Build with Java 25 and `./gradlew clean build`. No upstream accounts, tokens,
login providers, LAN/MQTT services or client entrypoint are imported.

## Validation and remaining work

Automated coverage checks registry matching/empty lists, hand modes, ledge
release/reset and backward clock, tool choice/durability/Silk Touch, manual
slot ownership, text bounds, sign existing-text protection, look angles,
config round-trip/reset, packaged license/edition boundaries and actual 26.3
mixin method/field descriptors. These checks do not apply the mixins in a
running game and are not a Minecraft runtime pass.

The [source inventory](reports/AUTISM_CLIENT_INVENTORY.md) lists **76 unique
builtin modules, 40 commands, 98 macro action types and 242 configured mixins**.
This checkpoint covers eight module surfaces, with AutoTool partial. The
other 68 are not claimed as ported, including modules with existing Rot
analogues. Macro/editor, commands, inventory transaction ownership, accounts
and multi-client services are separate work. Packet floods/crash payloads and
global protocol size-limit bypasses are not included.

Manual test order:

1. Launch Lite alone: dashboard, search, existing inventory/market HUD, mining
   and sessions. No AUTISM cards, implementation classes or GPL component.
2. Launch Plus alone: search `autism`; confirm all eight off, bind/save/reset
   each module, relaunch and switch profile.
3. On the controlled server, test ledges/slabs, Parkour pulse and physical jump,
   both Sneak modes; disable, open GUI, change world and activate Free Camera.
   Check that sneak/jump never stays held and normal keys remain usable.
4. Check NoInteract empty lists and both modes, both hands, named/baby entities;
   verify existing dungeon click protection still works.
5. Mine with multiple hotbar tools, Silk Touch and low durability. Change slot
   during the delay/mining/restore delay; the module must not take it back.
6. Place/edit normal and hanging signs. Check pixel trim, Auto Done off/on and
   existing text protection. Exercise market search with AutoSign on; its
   query must remain owned by market automation.
7. FreeLook front/back, pitch toggle, hold release, GUI/world changes, F5,
   Camera's front-view restriction and Free Camera interaction. Player yaw and
   pitch must remain unchanged while looking around.
8. AirPlace main/offhand rules, guide/color/range, four-tick cadence, denied
   placements, NoInteract filters and mining/GUI/market guards. The server's
   observed inventory/block changes remain authoritative.
9. Recheck existing dungeon/market flows and measure frame times. No runtime
   or FPS improvement is asserted from automated results.

Public Lite Modrinth/CurseForge publication remains gated by the existing
[release audit](MODRINTH_LITE_RELEASE.md); this Plus import does not clear it.
