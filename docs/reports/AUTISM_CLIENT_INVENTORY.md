# AUTISM Client source inventory (2026-10-16)

The provided 5.0 source has **76 unique builtin modules**, **40 registered commands**, **98 macro action types**, and **242 configured mixin entries**. Hide is registered in both variant paths, so there are 77 registration calls. These are source counts, not counts of Rot ports.

This checkpoint adds eight module surfaces to Plus. AutoTool remains partial. All eight require Minecraft runtime tests. Existing Rot analogues are listed separately and do not count as completed AUTISM ports. Auth/account storage, LAN/MQTT/multi-client services and the macro engine are not imported.

The full machine-readable inventory is [here](autism-client-feature-inventory.json). Adapted implementation and bundled notices are GPL-3.0-only in Plus; Lite retains MIT. The source is the owner-provided source snapshot rather than an asserted upstream Git commit.

| Category | Module | Source | Rot status | Notes |
| --- | --- | --- | --- | --- |
| Misc | PanicMode | `src/main/java/autismclient/modules/BuiltinModules.java:306` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | Flight | `src/main/java/autismclient/modules/BuiltinModules.java:385` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | Sprint | `src/main/java/autismclient/modules/BuiltinModules.java:823` | Existing analogue; parity pending | Rot already has Auto Sprint; AUTISM feature parity is not asserted. |
| Movement | Parkour | `src/main/java/autismclient/modules/BuiltinModules.java:878` | Implemented; runtime pending | Adapted into Plus-only sources and Rot settings. Not a whole-client import. |
| Movement | Speed | `src/main/java/autismclient/modules/BuiltinModules.java:1093` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | BoatFly | `src/main/java/autismclient/modules/BoatFlyModule.java:11` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | EntityControl | `src/main/java/autismclient/modules/EntityControlModule.java:21` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | InvMove | `src/main/java/autismclient/modules/BuiltinModules.java:340` | Existing analogue; parity pending | Rot already has Inventory Walk; AUTISM feature parity is not asserted. |
| Movement | Sneak | `src/main/java/autismclient/modules/BuiltinModules.java:1374` | Implemented; runtime pending | Adapted into Plus-only sources and Rot settings. Not a whole-client import. |
| Movement | AirJump | `src/main/java/autismclient/modules/AirJumpModule.java:9` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | NoClip | `src/main/java/autismclient/modules/NoClipModule.java:11` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | Phase | `src/main/java/autismclient/modules/PhaseModule.java:29` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | Waypoints | `src/main/java/autismclient/modules/WaypointsModule.java:11` | Existing analogue; parity pending | Rot already has Waypoints; AUTISM feature parity is not asserted. |
| Player | FastUse | `src/main/java/autismclient/modules/BuiltinModules.java:1846` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | FastBreak | `src/main/java/autismclient/modules/BuiltinModules.java:2099` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | AimAssist | `src/main/java/autismclient/modules/BuiltinModules.java:2319` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | AutoClicker | `src/main/java/autismclient/modules/BuiltinModules.java:2718` | Existing analogue; parity pending | Rot already has Auto Clicker; AUTISM feature parity is not asserted. |
| Combat | TriggerBot | `src/main/java/autismclient/modules/BuiltinModules.java:2800` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | AutoTotem | `src/main/java/autismclient/modules/AutoTotemModule.java:68` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | BedDefender | `src/main/java/autismclient/modules/BedDefenderModule.java:57` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | Surround | `src/main/java/autismclient/modules/SurroundModule.java:50` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | AnchorAura | `src/main/java/autismclient/modules/AnchorAuraModule.java:60` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | CrystalAura | `src/main/java/autismclient/modules/CrystalAuraModule.java:62` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | AutoTrap | `src/main/java/autismclient/modules/AutoTrapModule.java:63` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | KillAura | `src/main/java/autismclient/modules/KillAuraModule.java:82` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | AutoFarm | `src/main/java/autismclient/modules/AutoFarmModule.java:67` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Combat | AutoArmor | `src/main/java/autismclient/modules/AutoArmorModule.java:47` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | AutoSign | `src/main/java/autismclient/modules/AutoSignModule.java:6` | Implemented; runtime pending | Four lines, edit-existing, automatic Done. Market automation sign entry takes precedence. |
| Player | AutoLogin | `src/main/java/autismclient/modules/AutoLoginModule.java:21` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | TpClick | `src/main/java/autismclient/modules/TpClickModule.java:16` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | AntiBot | `src/main/java/autismclient/modules/AntiBotModule.java:4` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | AntiVanish | `src/main/java/autismclient/modules/AntiVanishModule.java:72` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | AutoFish | `src/main/java/autismclient/modules/BuiltinModules.java:2897` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | InstantRebreak | `src/main/java/autismclient/modules/BuiltinModules.java:3852` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | AutoTool | `src/main/java/autismclient/modules/BuiltinModules.java:923` | Partial; runtime pending | Hotbar only. Main-inventory swap and upstream delay randomization are pending. |
| Movement | Scaffold | `src/main/java/autismclient/modules/ScaffoldModule.java:76` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | NoFall | `src/main/java/autismclient/modules/BuiltinModules.java:1515` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | NoInteract | `src/main/java/autismclient/modules/BuiltinModules.java:3921` | Implemented; runtime pending | Game-mode mining/use/attack and direct client-thread packets. Off-thread custom packet senders are not inspected; upstream unused Friends flag is not exposed. |
| Player | AirPlace | `src/main/java/autismclient/modules/AirPlaceModule.java:20` | Implemented; runtime pending | Bounded normal game-mode placement; guide/fill/color. Server acceptance requires playtest. |
| Player | GhostBlock | `src/main/java/autismclient/modules/GhostBlockModule.java:59` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | OffhandInteract | `src/main/java/autismclient/modules/BuiltinModules.java:4024` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | Spam | `src/main/java/autismclient/modules/BuiltinModules.java:4062` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |
| Misc | PayAll | `src/main/java/autismclient/modules/PayAllModule.java:14` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |
| Misc | BookBot | `src/main/java/autismclient/modules/BuiltinModules.java:4133` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |
| Misc | PacketCanceller | `src/main/java/autismclient/modules/BuiltinModules.java:4313` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |
| Misc | AutoReconnect | `src/main/java/autismclient/modules/BuiltinModules.java:4377` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Player | InventoryTweaks | `src/main/java/autismclient/modules/InventoryTweaksModule.java:45` | Existing analogue; parity pending | Rot already has Inventory overlay/protection; AUTISM feature parity is not asserted. |
| Misc | AntiHunger | `src/main/java/autismclient/modules/AntiHungerModule.java:10` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Movement | SafeWalk | `src/main/java/autismclient/modules/SafeWalkModule.java:18` | Implemented; runtime pending | Adapted into Plus-only sources and Rot settings. Not a whole-client import. |
| Render | SpawnerESP | `src/main/java/autismclient/modules/BuiltinModules.java:4848` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | BetterTooltips | `src/main/java/autismclient/modules/BuiltinModules.java:4384` | Existing analogue; parity pending | Rot already has Tooltip modules; AUTISM feature parity is not asserted. |
| Render | Fullbright | `src/main/java/autismclient/modules/BuiltinModules.java:4481` | Existing analogue; parity pending | Rot already has Fullbright; AUTISM feature parity is not asserted. |
| Render | FakeCoords | `src/main/java/autismclient/modules/BuiltinModules.java:1443` | Existing analogue; parity pending | Rot already has Privacy/name helpers; AUTISM feature parity is not asserted. |
| Render | Xray | `src/main/java/autismclient/modules/BuiltinModules.java:4554` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | World | `src/main/java/autismclient/modules/WorldModule.java:11` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | Viewmodel | `src/main/java/autismclient/modules/ViewmodelModule.java:8` | Existing analogue; parity pending | Rot already has Viewmodel; AUTISM feature parity is not asserted. |
| Render | Freecam | `src/main/java/autismclient/modules/BuiltinModules.java:4684` | Existing analogue; parity pending | Rot already has Free Camera; AUTISM feature parity is not asserted. |
| Render | FreeLook | `src/main/java/autismclient/modules/FreeLookModule.java:8` | Implemented; runtime pending | Front/back, pitch limit, hold/toggle key. Enable module to arm its hold bind. Free Camera takes precedence. |
| Render | ESP | `src/main/java/autismclient/modules/BuiltinModules.java:4738` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | Chams | `src/main/java/autismclient/modules/ChamsModule.java:10` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | ItemESP | `src/main/java/autismclient/modules/BuiltinModules.java:4764` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | Nametags | `src/main/java/autismclient/modules/NameTagsModule.java:6` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | Tracers | `src/main/java/autismclient/modules/BuiltinModules.java:4789` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | Trajectories | `src/main/java/autismclient/modules/TrajectoriesModule.java:20` | Existing analogue; parity pending | Rot already has Trajectories; AUTISM feature parity is not asserted. |
| Render | StorageESP | `src/main/java/autismclient/modules/BuiltinModules.java:4818` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | BlockESP | `src/main/java/autismclient/modules/BuiltinModules.java:4883` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | HoleESP | `src/main/java/autismclient/modules/HoleEspModule.java:21` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Render | NoRender | `src/main/java/autismclient/modules/NoRenderModule.java:8` | Existing analogue; parity pending | Rot already has Hide/render options; AUTISM feature parity is not asserted. |
| Player | XCarry | `src/main/java/autismclient/modules/BuiltinModules.java:361` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | HUD | `src/main/java/autismclient/modules/BuiltinModules.java:4908` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | AdminTools | `src/main/java/autismclient/modules/BuiltinModules.java:4944` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |
| Misc | GoldenLever | `src/main/java/autismclient/modules/GoldenLeverModule.java:20` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | NameCensor | `src/main/java/autismclient/modules/NameCensorModule.java:38` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | Teams | `src/main/java/autismclient/modules/TeamsModule.java:26` | Planned | Not ported. Needs dependency, setting and runtime review. |
| Misc | PingSpoof | `src/main/java/autismclient/modules/PingSpoofModule.java:6` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |
| Misc | Blink | `src/main/java/autismclient/modules/BlinkModule.java:4` | Planned | Separate review required. Flood/crash payloads, protocol size-limit bypasses and high-volume multi-client paths are excluded from this build. |

## Other systems still outside the module port

Commands: MacroCommand, SyncCommand, DelayCommand, SendCommand, VClipCommand, HClipCommand, TpCommand, CopyPosCommand, NbtCommand, ServerCommand, PluginsCommand, PrefixCommand, CommandsCommand, HelpCommand, BindsCommand, BindCommand, ClearCommand, DismountCommand, DisconnectCommand, SayCommand, DropCommand, ChangeSlotCommand, ClickSlotCommand, ClickItemCommand, GiveCommand, DamageCommand, IrcCommand, MatchmakingCommand, MultiCommand, ToggleCommand, ModulesCommand, OreSimCommand, GhostBlockCommand, FriendCommand, WaypointsCommand, GamemodeCommand, FakeGmCommand, XCarryCommand, RemoteViewCommand, NameScrapeCommand.

Macro action types: DELAY, PACKET, PACKET_CLICK, WAIT_PACKET, WAIT_HEALTH, WAIT_ITEM, WAIT_BLOCK, WAIT_GUI, CUSTOM_MENU, CLICK, ROTATE, USE_ITEM, INVENTORY, SEND_PACKET, CRAFT, SELECT_SLOT, XCARRY, DROP, ITEM, PICK_UP_ALL, TICK_SYNC, REVISION_SYNC, SERVER_TICK_SYNC, CLOSE_GUI, SWAP_SLOTS, WAIT_COOLDOWN, GO_TO, WAIT_POS, PAYLOAD, DISCONNECT, TOGGLE_MODULE, START_MACRO, STOP_MACRO, SNEAK, JUMP, SPRINT, MOVE, LOOK_AT_BLOCK, REPEAT, WAIT_CHAT, WAIT_ENTITY, WAIT_SLOT_CHANGE, OPEN_CONTAINER, INTERACT_ENTITY, DESYNC, RESTORE_GUI, SAVE_GUI, SEND_TOGGLE, DELAY_PACKETS, INVENTORY_AUDIT, STORE_ITEM, WAIT_SOUND, MINE, INSTA_BREAK, BREAK, PAY, SEND_CHAT, NBT_BOOK, WAIT_LAN_STEP, WAIT_MACRO_STEP, WAIT_WORLD_CHANGE, WAIT_POSITION_DELTA, WAIT_TELEPORT, WAIT_GAMEMODE_CHANGE, WAIT_MOVEMENT, RACE, REPORT, VCLIP, HCLIP, TP, PACKET_GATE, PACKET_BURST, CONTAINER_CLICK_SEQUENCE, ASSERT, USE_ITEM_PHASE, SEND_COMMAND_PACKET, END_PACKET_GATE, WAIT_PACKET_MATCH, WAIT_INVENTORY_PREDICATE, WAIT_DURABILITY, WAIT_FREE_SLOTS, WAIT_ENTITY_TARGET, WAIT_GUI_TYPE, BRANCH, FINALLY, MACRO_VARIABLES, CAPTURE_VALUE, FAKE_GAMEMODE, BUNDLE_DUPE_V2, ROLLBACK, PLACE, SIGN_EDIT, FPS, SPING, IF, FLOW, LABEL, MULTI.

The upstream GUI, HUD editor, add-on API, profiles, account/login providers, LAN coordination, MQTT matchmaking, multi-client controller, packet/logger tools, ore simulation and macro persistence/execution are separate systems. They are not implemented merely by adding a module with a matching name. Packet floods, crash payloads, global protocol size-limit bypasses and high-volume multi-client launching are not included in this checkpoint.

## Suggested next slices

1. Complete AutoTool inventory-transaction ownership and test the new normal/direct interaction filters.
2. Port AirJump with mode-specific collision/Lithium compatibility and extend the existing Auto Sprint instead of duplicating it.
3. Port bounded AutoArmor/AutoTotem transactions and InventoryTweaks sorting/protection after shared inventory ownership exists.
4. Review remaining movement, combat and rendering modules independently; preserve Free Camera, market purchases and dungeon ownership.
5. Treat macro/command systems as a separate engine with cancellation, budgets, persistence and a migration plan. Do not import the old client entrypoint.
