package fi.rotclient;

import java.util.ArrayList;
import java.util.List;

import static fi.rotclient.QolUtilityCatalog.Group;
import static fi.rotclient.QolUtilityCatalog.SettingDef;
import static fi.rotclient.QolUtilityCatalog.SettingType;
import static fi.rotclient.QolUtilityCatalog.ModuleDef;
import static fi.rotclient.QolUtilityCatalog.dungeonClassSetting;
import static fi.rotclient.QolUtilityCatalog.module;
import static fi.rotclient.QolUtilityCatalog.section;
import static fi.rotclient.QolUtilityCatalog.setting;

/** Plus-only catalog parents and mixed-module cheat settings. */
public final class QolPlusCatalog {
    private QolPlusCatalog() {
    }

    public static List<ModuleDef> extraModules() {
        List<ModuleDef> modules = new ArrayList<>();
        modules.add(module(
                "qol.eye_height_fix",
                "Eye Height Fix",
                "Visually revert sneaking eye height on islands without modern sneak support.",
                Group.RENDER,
                "Camera",
                false,
                true,
                true,
                List.of("eye height", "sneak")));
        modules.add(module(
                "qol.instant_sneak",
                "Instant Sneak",
                "Skip the smooth sneak/swim camera lerp.",
                Group.RENDER,
                "Camera",
                false,
                true,
                true,
                List.of("instant sneak", "sneak", "swim")));
        modules.add(module(
                "qol.item_count_fix",
                "Item Count Fix",
                "Keep the item count visible for unstackable SkyBlock stacks.",
                Group.INTERFACE,
                "Inventory",
                false,
                true,
                true,
                List.of("item count", "stack")));
        modules.add(module(
                "qol.dungeon_termsim",
                "Terminal Simulator",
                "Local F7 terminal practice with Hypixel-style window titles and personal-best times.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("termsim", "terminal sim", "practice"),
                setting("qol.dungeon_termsim.open", "Open Hub", "Open the Terminal Simulator menu.", SettingType.ACTION),
                setting("qol.dungeon_termsim.keybind", "Keybind", "Open the hub while in-game.", SettingType.KEYBIND),
                setting("qol.dungeon_termsim.ping", "Ping", "Simulated round-trip delay in milliseconds.", SettingType.NUMBER),
                setting("qol.dungeon_termsim.show_pbs", "Show PBs", "Show local personal-best times on hub dyes.", SettingType.TOGGLE),
                setting("qol.dungeon_termsim.ip", "Remote IP", "Stored only. Rot Client keeps the local /rot termsim hub and does not connect to a remote simulator.", SettingType.TEXT)));
        modules.add(module(
                "qol.auto_sprint",
                "Auto Sprint",
                "Keep sprinting while moving forward. Plus-only input assist.",
                Group.UTILITIES,
                "Movement",
                false,
                true,
                true,
                List.of("sprint", "run", "ctrl"),
                setting("qol.auto_sprint.keybind", "Keybind", "Optional shortcut.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.map_art_override",
                "Fox",
                "Locally replace maps, paintings, item-frame pictures, and large fixed item-display or block-display wall art with the bundled image or your own local image. Supports contiguous same-facing horizontal item-frame and item-display panels, including the Hypixel Hub 14x7 map wall whose frames use mixed rotation. Never changes map data, painting entities, or anything sent to a server.",
                Group.RENDER,
                "Local Visuals",
                false,
                true,
                true,
                List.of("map", "image", "item frame", "art", "local", "painting", "fox", "hub", "block display"),
                setting("qol.map_art_override.image_path", "Custom Local Image Path", "Optional path relative to .minecraft/config, or an absolute local path. Leave blank to use the image bundled with Rot Client+.", SettingType.TEXT, "bundled image"),
                setting("qol.map_art_override.stretch_frames", "Stretch Item-Frame Panels", "Fit one image over a contiguous same-facing horizontal map wall (contain, centered). The Hub spawn map is 14x7; leftover tiles and edges stay black. Mixed item-frame rotations stay one canvas. Single maps always use the full image.", SettingType.TOGGLE)));
        modules.add(module(
                "qol.escrow_fix",
                "Escrow Fix",
                "Reopen AH/BZ after an escrow chat line closes the menu. Plus automation, off by default.",
                Group.UTILITIES,
                "Market",
                false,
                true,
                true,
                List.of("escrow", "auction house", "bazaar", "ah", "bz")));
        modules.add(module(
                "qol.ghosts",
                "Ghosts",
                "Mist creeper reveal and highlight controls. Plus render helper, off by default.",
                Group.RENDER,
                "Players",
                false,
                true,
                true,
                List.of("ghosts", "creeper", "dwarven mines", "powered", "mist", "highlight"),
                setting("qol.ghosts.highlight_style", "Highlight Style", "Outline, filled, or both boxes around each Mist creeper.", SettingType.ENUM,
                        GhostsPolicy.HIGHLIGHT_STYLES, "both"),
                setting("qol.ghosts.fill_color", "Fill Color", "Filled-box color over each Mist creeper.", SettingType.COLOR),
                setting("qol.ghosts.outline_color", "Outline Color", "Box-outline color around each Mist creeper.", SettingType.COLOR),
                setting("qol.ghosts.show_ghosts", "Show Ghosts", "Show the creeper entities.", SettingType.TOGGLE),
                setting("qol.ghosts.show_powered", "Show Powered Layer", "Keep the vanilla charged overlay.", SettingType.TOGGLE),
                setting("qol.ghosts.keybind", "Keybind", "Toggle Ghosts.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.trajectories",
                "Trajectories",
                "Predicted bow and ender-pearl flight path. Plus visual assist, off by default.",
                Group.COMBAT,
                "Aim",
                false,
                true,
                true,
                List.of("trajectory", "bow", "pearl", "arrow", "cheat"),
                setting("qol.trajectories.bows", "Bows", "Show bow / Terminator trajectories.", SettingType.TOGGLE, "bow"),
                setting("qol.trajectories.pearls", "Pearls", "Show ender-pearl trajectories.", SettingType.TOGGLE, "pearl"),
                setting("qol.trajectories.lines", "Show Lines", "Draw the simulated path.", SettingType.TOGGLE),
                setting("qol.trajectories.boxes", "Show Hit Box", "Mark the predicted impact.", SettingType.TOGGLE),
                setting("qol.trajectories.depth", "Depth Check", "Hide the path behind blocks.", SettingType.TOGGLE),
                setting("qol.trajectories.range", "Solver Range", "How many ticks to simulate.", SettingType.NUMBER, "30"),
                setting("qol.trajectories.width", "Line Width", "Pixel thickness of the path line.", SettingType.NUMBER),
                setting("qol.trajectories.box_size", "Box Size", "Impact marker size.", SettingType.NUMBER),
                setting("qol.trajectories.plane", "Show Plane", "Draw a face-aligned impact plane at the impact point.", SettingType.TOGGLE),
                setting("qol.trajectories.entities", "Show Entities", "Stop the path on entity hits and box the target.", SettingType.TOGGLE),
                setting("qol.trajectories.plane_size", "Plane Size", "Impact plane scale.", SettingType.NUMBER),
                setting("qol.trajectories.color", "Color", "Trajectory color.", SettingType.COLOR)));
        modules.add(module(
                "qol.world_scanner",
                "World Scanner",
                "Scan Crystal Hollows chunks for structures and fluid ESP. Plus visual scanner, off by default.",
                Group.MINING,
                "Scanner",
                false,
                true,
                true,
                List.of("world scanner", "crystal hollows", "divan", "corleone", "fairy grotto", "cheat", "xray"),
                worldScannerSettings()));
        modules.add(module(
                "qol.auto_clicker",
                "Auto Clicker",
                "Left/right auto clicker for Serveri. CPS averages the set value with about ±20% jitter so it is not a metronome.",
                Group.COMBAT,
                "Clicker",
                false,
                true,
                true,
                List.of("auto clicker", "clicker", "cps", "left click", "right click", "cheat"),
                setting("qol.auto_clicker.whitelist_only", "Whitelist Only", "Only click while holding a whitelisted item. Add with /rot autoclicker add left|right.", SettingType.TOGGLE, "whitelist"),
                setting("qol.auto_clicker.allow_breaking", "Allow Breaking Blocks", "Hold-mine the targeted block while left auto-click is active. Off skips left clicks on blocks.", SettingType.TOGGLE, "break", "mining"),
                setting("qol.auto_clicker.block_breaker", "Block Dungeon Breaker", "Disable auto clicker while holding Dungeon Breaker.", SettingType.TOGGLE, "dungeon breaker"),
                setting("qol.auto_clicker.terminator_only", "Terminator Only", "On by default: only left-clicks while holding Terminator (NBT id TERMINATOR) and the use/right-click key. Turn off for normal left/right auto-click.", SettingType.TOGGLE, "terminator", "bow"),
                setting("qol.auto_clicker.cps", "Clicks Per Second", "Average CPS when only one side is enabled. Intervals jitter about ±20%.", SettingType.NUMBER, "5", "cps"),
                setting("qol.auto_clicker.enable_left", "Enable Left Click", "Auto left-click while the activation bind is held.", SettingType.TOGGLE, "left"),
                setting("qol.auto_clicker.enable_right", "Enable Right Click", "Auto right-click while the activation bind is held.", SettingType.TOGGLE, "right"),
                setting("qol.auto_clicker.left_cps", "Left Clicks Per Second", "Average left CPS when both sides are enabled. Intervals jitter about ±20%.", SettingType.NUMBER, "left cps"),
                setting("qol.auto_clicker.right_cps", "Right Clicks Per Second", "Average right CPS when both sides are enabled. Intervals jitter about ±20%.", SettingType.NUMBER, "right cps"),
                setting("qol.auto_clicker.left_keybind", "Left Activation", "Blank uses left mouse. Examples: LMB, MOUSE_LEFT, R.", SettingType.KEYBIND, "left bind"),
                setting("qol.auto_clicker.right_keybind", "Right Activation", "Blank uses right mouse. Examples: RMB, MOUSE_RIGHT.", SettingType.KEYBIND, "right bind"),
                setting("qol.auto_clicker.cps_hud", "CPS HUD", "Show live synthetic clicks per second. BLOCK HOLD means continuous mining input is active.", SettingType.TOGGLE, "hud", "cps"),
                setting("qol.auto_clicker.open_hud_editor", "Open HUD Elements Editor", "Move the Auto Clicker CPS HUD.", SettingType.ACTION, "position")));
        modules.add(module(
                "qol.auto_dojo",
                "Auto Dojo",
                "Dojo helper for the Serveri: aim Control skeletons, shoot Mastery wool, swap Discipline swords. Cheat, off by default.",
                Group.COMBAT,
                "Dojo",
                false,
                true,
                true,
                List.of("dojo", "control", "mastery", "discipline", "cheat"),
                setting("qol.auto_dojo.control", "Control", "Look at the Test of Control wither skeleton.", SettingType.TOGGLE, "cheat"),
                setting("qol.auto_dojo.control_predict", "Control Predict", "Ticks of skeleton velocity to lead.", SettingType.NUMBER),
                setting("qol.auto_dojo.mastery", "Mastery", "Draw and release on yellow Mastery wool.", SettingType.TOGGLE, "cheat"),
                setting("qol.auto_dojo.mastery_delay", "Mastery Delay", "Milliseconds left on yellow wool before release.", SettingType.NUMBER),
                setting("qol.auto_dojo.discipline", "Discipline", "Swap to the matching sword from the zombie's helmet.", SettingType.TOGGLE, "cheat"),
                setting("qol.auto_dojo.discipline_attack", "Discipline Attack", "Left-click when looking at the Discipline zombie.", SettingType.TOGGLE, "cheat")));
        modules.add(module(
                "qol.auto_conversation",
                "Auto Conversation",
                "Automatically click NPC dialogue options. Serveri convenience.",
                Group.UTILITIES,
                "Chat",
                false,
                true,
                true,
                List.of("npc", "dialogue", "conversation"),
                setting("qol.auto_conversation.multi", "Multi-option dialogues", "Also click when more than one option is present.", SettingType.TOGGLE),
                setting("qol.auto_conversation.green", "Check green color", "Only click the green (progress) option when present.", SettingType.TOGGLE),
                setting("qol.auto_conversation.delay", "Click delay", "Ticks to wait before sending the option command.", SettingType.NUMBER, "4", "ticks")));
        modules.add(module(
                "qol.foraging_cheats",
                "Foraging Cheats",
                "Optional auto beacon clicks, auto chop, and axe toss on the Serveri. Off by default.",
                Group.FORAGING,
                "Cheats",
                false,
                true,
                true,
                List.of("auto chop", "axe toss", "beacon"),
                setting("qol.foraging_cheats.auto_beacon", "Auto Beacon", "Click suggested Tune Frequency slots. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.foraging_cheats.auto_chop", "Auto Chop", "Left-click while looking at a custom-tree log. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.foraging_cheats.axe_toss", "Axe Toss", "Right-click throwable axes on large clusters. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.foraging_cheats.min_cluster", "Toss Min Cluster", "Minimum sweep cluster for axe toss.", SettingType.NUMBER, "5"),
                setting("qol.foraging_cheats.click_delay", "Click Delay", "Ticks between cheat clicks.", SettingType.NUMBER, "3", "ticks")));
        modules.add(module(
                "qol.inventory_walk",
                "Inventory Walk",
                "Move with WASD while a GUI is open. Skips chats and text fields. Jump, sprint and sneak stay off so the Serveri does not treat the click as a bad movement. After a click, walking waits for keepalive/ping plus 500 ms.",
                Group.UTILITIES,
                "Movement",
                false,
                true,
                true,
                List.of("inventory walk", "gui move", "wasd"),
                setting("qol.inventory_walk.ping", "Ping", "Walk while a keepalive arrived within this many milliseconds and no click is in-flight. After a click, walking also resumes 500 ms after a later keepalive.", SettingType.NUMBER, "200ms", "ping")));
        modules.add(module(
                "qol.secret_hitboxes",
                "Secret Hitboxes",
                "Expand dungeon secret block hitboxes. Lever, button, skull, and chest expansions start off; enable the ones you want. Only-in-dungeons is on by default.",
                Group.DUNGEONS,
                "Secrets",
                false,
                true,
                true,
                List.of("secret", "hitbox", "lever", "button", "skull", "chest"),
                setting("qol.secret_hitboxes.only_dungeons", "Only in Dungeons", "Apply only when the scoreboard looks like a dungeon. On by default.", SettingType.TOGGLE, "dungeon"),
                setting("qol.secret_hitboxes.lever", "Lever", "Extend lever hitboxes.", SettingType.TOGGLE),
                setting("qol.secret_hitboxes.old_lever", "1.8 Lever Hitbox", "Use the 1.8 lever shape instead of a full cube.", SettingType.TOGGLE),
                setting("qol.secret_hitboxes.button", "Button", "Extend button hitboxes.", SettingType.TOGGLE),
                setting("qol.secret_hitboxes.flat_button", "Flat Button Hitbox", "Use a thin button shape.", SettingType.TOGGLE),
                setting("qol.secret_hitboxes.skull", "Skulls", "Extend skull hitboxes.", SettingType.TOGGLE),
                setting("qol.secret_hitboxes.chests", "Chests", "Extend chest hitboxes.", SettingType.TOGGLE),
                setting("qol.secret_hitboxes.only_trapped", "Only Trapped Chests", "Chest expansion applies only to trapped chests.", SettingType.TOGGLE)));
        modules.add(module(
                "qol.dungeon_requeue",
                "Auto Requeue",
                "After Extra Stats, send /instancerequeue.",
                Group.DUNGEONS,
                "Automation",
                false,
                true,
                true,
                List.of("requeue", "instancerequeue", "extra stats"),
                setting("qol.dungeon_requeue.delay", "Delay", "Client ticks to wait after Extra Stats.", SettingType.NUMBER)));
        modules.add(module(
                "qol.dungeon_hover_terms",
                "Hover Terms",
                "Click a solved terminal slot when the cursor hovers it. Skips Melody. Cheat, off.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("hover terms", "hover click", "terminal hover"),
                setting("qol.dungeon_hover_terms.min_delay", "Min Delay", "Minimum milliseconds between hover clicks. Default 50.", SettingType.NUMBER, "cheat"),
                setting("qol.dungeon_hover_terms.max_delay", "Max Delay", "Maximum milliseconds between hover clicks. Default 120.", SettingType.NUMBER, "cheat")));
        modules.add(module(
                "qol.dungeon_soulsand",
                "Soulsand Triggerbot",
                "F7 P3: while holding soul sand, a chest, or an ender chest and looking at Y=105 stone bricks, right-click place. Cheat, off.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("soulsand", "soul sand", "p3 place")));
        modules.add(module(
                "qol.diana_share",
                "Diana Share",
                "Cheat: party /pc inquisitor coords and optional auto nearest hub warp. Serveri only. Off by default.",
                Group.EVENTS,
                "Diana",
                false,
                true,
                true,
                List.of("inquisitor", "party", "warp", "diana"),
                setting("qol.diana_share.party", "Party Coords", "Send /pc x/y/z | Minos Inquisitor. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.diana_share.auto_warp", "Auto Warp", "Send nearest /warp for the guess. Cheat, off by default.", SettingType.TOGGLE, "cheat")));
        modules.add(module(
                "qol.slayer_dagger_swap",
                "Dagger Swap",
                "Select and attune the correct Blaze Slayer dagger after attacking an Inferno demon.",
                Group.SLAYER,
                "Blaze",
                false,
                true,
                true,
                List.of("slayer", "blaze", "inferno", "dagger", "attunement", "ashen", "auric", "spirit", "crystal"),
                setting("qol.slayer_dagger_swap.delay", "Delay", "Ticks before changing to the required dagger.", SettingType.NUMBER),
                setting("qol.slayer_dagger_swap.variance", "Delay Variance", "Random additional delay from zero through this many ticks.", SettingType.NUMBER)));
        modules.add(module(
                "qol.slayer_auto_soulcry",
                "Auto Soulcry",
                "Use a Voidgloom katana Soulcry when fighting the selected boss, then wait until the 4s ability is ready again.",
                Group.SLAYER,
                "Voidgloom",
                false,
                true,
                true,
                List.of("slayer", "voidgloom", "katana", "soulcry", "mana", "automation"),
                setting("qol.slayer_auto_soulcry.check_mana", "Check Mana", "Require 200 mana, or 100 with Ultimate Wise, including Overflow Mana.", SettingType.TOGGLE),
                setting("qol.slayer_auto_soulcry.check_hitbox", "Check Boss Hitbox", "Tick-based use requires the crosshair to target the owned Voidgloom boss.", SettingType.TOGGLE),
                setting("qol.slayer_auto_soulcry.min_delay", "Minimum Delay", "Minimum tick-based delay before Soulcry is used.", SettingType.NUMBER),
                setting("qol.slayer_auto_soulcry.max_delay", "Maximum Delay", "Maximum tick-based delay before Soulcry is used.", SettingType.NUMBER),
                setting("qol.slayer_auto_soulcry.tick_based", "Tick Based", "Observe the held katana and target every client tick.", SettingType.TOGGLE),
                setting("qol.slayer_auto_soulcry.attack_based", "Attack Based", "Use Soulcry once when attacking a detected Voidgloom boss, then wait until the ability cooldown finishes.", SettingType.TOGGLE),
                setting("qol.slayer_auto_soulcry.other_bosses", "Work On Other Bosses", "Allow attack-based detection for another player's Voidgloom boss.", SettingType.TOGGLE)));
        modules.add(module(
                "qol.slayer_auto_start",
                "Auto Start Quest",
                "After SLAYER QUEST COMPLETE, send the Maddox restart command for the same family and tier.",
                Group.SLAYER,
                "Automation",
                false,
                true,
                true,
                List.of("slayer", "auto", "maddox", "start", "sven", "enderman"),
                setting("qol.slayer_auto_start.delay", "Delay", "Client ticks to wait after quest complete before sending the command.", SettingType.NUMBER),
                setting("qol.slayer_auto_start.block_not_spawnable", "Block Not Spawnable", "Ignore Maddox menu clicks on bosses that cannot spawn in this dimension.", SettingType.TOGGLE)));
        modules.add(module(
                "qol.auto_experiments",
                "Auto Experiments",
                "Automatic Chronomatron and Ultrasequencer clicking at the Experimentation Table. Serveri automation.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("auto experiments", "experimentation table", "chronomatron", "ultrasequencer"),
                setting("qol.auto_experiments.click_delay", "Click Delay", "Time in ms between automatic test clicks.", SettingType.NUMBER, "200", "ms"),
                setting("qol.auto_experiments.delay_variety", "Delay Variety", "Variance in delays.", SettingType.NUMBER, "50", "ms"),
                setting("qol.auto_experiments.auto_close", "Auto Close", "Automatically close the GUI after completing the experiment.", SettingType.TOGGLE),
                setting("qol.auto_experiments.serum_count", "Serum Count", "Consumed Metaphysical Serum count.", SettingType.NUMBER, "0"),
                setting("qol.auto_experiments.get_max_xp", "Get Max XP", "Solve Chronomatron to 15 and Ultrasequencer to 20 for max XP.", SettingType.TOGGLE)));
        modules.add(module(
                "qol.wardrobe_keybinds",
                "Wardrobe Keybinds",
                "Hypixel wardrobe binds: 1-9, pages, hidden auto-equip via /wd.",
                Group.UTILITIES,
                "Keybinds",
                false,
                true,
                true,
                List.of("wardrobe", "unequip", "auto equip"),
                setting("qol.wardrobe_keybinds.style", "Keybind Style", "Simple uses 1-9. Hotbar uses Minecraft hotbar keys. Custom uses the slot binds below.", SettingType.ENUM,
                        List.of("Simple", "Hotbar", "Custom"), "style"),
                setting("qol.wardrobe_keybinds.disable_unequip", "Prevent Unequip", "Do not click a slot that is already equipped.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.sound", "Sound Effect", "Play a local click sound when a wardrobe slot is used.", SettingType.TOGGLE, "sound"),
                setting("qol.wardrobe_keybinds.cancel_all", "Cancel All Other Clicks", "While the wardrobe is open, swallow keys that are not wardrobe binds.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.override", "Key Override", "Hold this key to allow other clicks when cancel-all is on.", SettingType.KEYBIND, "left control"),
                setting("qol.wardrobe_keybinds.cancel_render", "Cancel GUI Render", "Hide the wardrobe chest while it stays open for keybinds.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.ping", "Ping", "Minimum milliseconds between wardrobe clicks.", SettingType.NUMBER, "250", "ms"),
                setting("qol.wardrobe_keybinds.use_hotbar", "Use Hotbar Binds", "1-9 follow Minecraft hotbar keys.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.swap", "Swap Key", "Click the unequipped slot of a configured pair.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.swap_key", "Swap Keybind", "Key that triggers the swap pair.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.swap_a", "Swap Slot 1", "First swap slot, 1-9.", SettingType.NUMBER, "1"),
                setting("qol.wardrobe_keybinds.swap_b", "Swap Slot 2", "Second swap slot, 1-9.", SettingType.NUMBER, "2"),
                setting("qol.wardrobe_keybinds.next", "Next Page", "Next wardrobe page.", SettingType.KEYBIND, "right arrow"),
                setting("qol.wardrobe_keybinds.previous", "Previous Page", "Previous wardrobe page.", SettingType.KEYBIND, "left arrow"),
                setting("qol.wardrobe_keybinds.unequip", "Unequip", "Unequip wardrobe set.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.auto_close", "Auto Close After Use", "Close the wardrobe after a successful slot click.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.auto_equip", "Auto Equip", "Equip a slot with /wd without showing the GUI. Use at your own risk.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.move_equip", "Equip While Moving", "Allow WASD while hidden auto-equip runs. Increases detection risk.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.reset_open", "Reset On GUI Open", "Cancel a pending auto-equip if you open another GUI.", SettingType.TOGGLE),
                setting("qol.wardrobe_keybinds.click_delay", "Click Delay", "Ticks to wait after the hidden menu opens before clicking.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.wardrobe_keybinds.close_delay", "Close Delay", "Ticks to wait after the click before closing.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.wardrobe_keybinds.delay_variance", "Max Delay Variety", "Random extra ticks added to click and close delays.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.wardrobe_keybinds.open_hud_editor", "Display Text", "Move the Equipping HUD.", SettingType.ACTION, "position"),
                setting("qol.wardrobe_keybinds.custom_1", "Custom Slot 1", "Custom key for wardrobe slot 1.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_2", "Custom Slot 2", "Custom key for wardrobe slot 2.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_3", "Custom Slot 3", "Custom key for wardrobe slot 3.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_4", "Custom Slot 4", "Custom key for wardrobe slot 4.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_5", "Custom Slot 5", "Custom key for wardrobe slot 5.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_6", "Custom Slot 6", "Custom key for wardrobe slot 6.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_7", "Custom Slot 7", "Custom key for wardrobe slot 7.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_8", "Custom Slot 8", "Custom key for wardrobe slot 8.", SettingType.KEYBIND),
                setting("qol.wardrobe_keybinds.custom_9", "Custom Slot 9", "Custom key for wardrobe slot 9.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.cheater_wardrobe",
                "Wardrobe Swapper",
                "Hidden /wd auto-equip for Armor Sets. Bind any keyboard or mouse key to wardrobe slots 1-9; activation requires the player to be stationary by default.",
                Group.UTILITIES,
                "Keybinds",
                false,
                true,
                true,
                List.of("wardrobe swapper", "cheater wardrobe", "wardrobe", "auto equip", "wd"),
                setting("qol.cheater_wardrobe.stationary_only", "Require Stationary", "Start and complete hidden wardrobe swaps only while the player is not moving.", SettingType.TOGGLE),
                setting("qol.cheater_wardrobe.reset_open", "Reset On GUI Open", "Cancel a pending hidden swap if another GUI opens.", SettingType.TOGGLE),
                setting("qol.cheater_wardrobe.click_delay", "Click Delay", "Ticks to wait after the hidden menu opens before clicking.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.cheater_wardrobe.close_delay", "Close Delay", "Ticks to wait after the click before closing.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.cheater_wardrobe.delay_variance", "Max Delay Variety", "Random extra ticks added to click and close delays.", SettingType.NUMBER, "1", "ticks"),
                section("qol.cheater_wardrobe.slots", "Wardrobe Slot Binds"),
                setting("qol.cheater_wardrobe.slot_1", "Wardrobe 1", "Equip wardrobe slot 1 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_2", "Wardrobe 2", "Equip wardrobe slot 2 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_3", "Wardrobe 3", "Equip wardrobe slot 3 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_4", "Wardrobe 4", "Equip wardrobe slot 4 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_5", "Wardrobe 5", "Equip wardrobe slot 5 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_6", "Wardrobe 6", "Equip wardrobe slot 6 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_7", "Wardrobe 7", "Equip wardrobe slot 7 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_8", "Wardrobe 8", "Equip wardrobe slot 8 with this key.", SettingType.KEYBIND),
                setting("qol.cheater_wardrobe.slot_9", "Wardrobe 9", "Equip wardrobe slot 9 with this key.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.auto_harp",
                "Auto Harp",
                "Melody's Harp: middle-click quartz notes in the Harp GUI. Serveri automation with the same 9x6 chest layout as Hypixel.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("harp", "melody", "auto harp", "the park")));
        modules.add(module(
                "qol.auto_gfs",
                "Auto GFS",
                "Refill pearls, jerrys, Superboom, and leaps from sacks with /gfs. Mort start, timer, and Architect's First Draft on puzzle fail. Same sack ids and chat as Hypixel.",
                Group.DUNGEONS,
                "Automation",
                false,
                true,
                true,
                List.of("gfs", "get from sack", "pearl", "jerry", "leap", "superboom"),
                setting("qol.auto_gfs.in_skyblock", "In Skyblock", "Refill anywhere in SkyBlock.", SettingType.TOGGLE),
                setting("qol.auto_gfs.in_kuudra", "In Kuudra", "Also refill in Kuudra when In Skyblock is off.", SettingType.TOGGLE),
                setting("qol.auto_gfs.in_dungeon", "In Dungeon", "Also refill in dungeons when In Skyblock is off.", SettingType.TOGGLE),
                setting("qol.auto_gfs.refill_on_dungeon_start", "Refill on Dungeon Start", "Refill when Mort's dungeon-start chat fires.", SettingType.TOGGLE),
                setting("qol.auto_gfs.refill_on_timer", "Refill on Timer", "Refill on a repeating interval.", SettingType.TOGGLE),
                setting("qol.auto_gfs.timer_increments", "Timer Increments", "Seconds between timed refills.", SettingType.NUMBER, "5", "s"),
                setting("qol.auto_gfs.refill_pearl", "Refill Pearl", "Refill ender pearls to 16.", SettingType.TOGGLE),
                setting("qol.auto_gfs.refill_jerry", "Refill Jerry", "Refill inflatable jerrys to 64.", SettingType.TOGGLE),
                setting("qol.auto_gfs.refill_tnt", "Refill TNT", "Refill Superboom TNT to 64.", SettingType.TOGGLE),
                setting("qol.auto_gfs.refill_leap", "Refill Leaps", "Refill spirit leaps to 16.", SettingType.TOGGLE),
                setting("qol.auto_gfs.refill_twilight", "Refill Twilight", "Refill Twilight Arrow Poison to 64.", SettingType.TOGGLE),
                setting("qol.auto_gfs.auto_get_draft", "Auto Get Draft", "Get Architect's First Draft from sacks after a local puzzle fail.", SettingType.TOGGLE),
                setting("qol.auto_gfs.keybind", "Keybind", "Toggle Auto GFS.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.auto_sell",
                "Auto Sell",
                "Click matching dungeon drops in Trades and Booster Cookie menus. Same hover-name list as Hypixel.",
                Group.DUNGEONS,
                "Automation",
                false,
                true,
                true,
                List.of("auto sell", "trades", "cookie", "dungeon drops"),
                setting("qol.auto_sell.delay", "Delay", "Ticks between sell clicks.", SettingType.NUMBER, "6", "ticks"),
                setting("qol.auto_sell.randomization", "Randomization", "Random extra ticks added to the delay.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.auto_sell.click_type", "Click Type", "Shift, middle, or left click.", SettingType.ENUM,
                        AutoSellPolicy.CLICK_TYPES),
                setting("qol.auto_sell.list", "Sell list", "Comma-separated hover-name fragments to sell.", SettingType.TEXT),
                setting("qol.auto_sell.add_defaults", "Add defaults", "Add the default dungeon drop names to the sell list.", SettingType.ACTION),
                setting("qol.auto_sell.keybind", "Keybind", "Toggle Auto Sell.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.farm_keys",
                "Farm Keys",
                "Crop-farming key remap: temporarily replace attack and jump binds, optionally lock look. Cheat, off by default.",
                Group.GARDEN,
                "Garden",
                false,
                true,
                true,
                List.of("farm keys", "farming keys", "crop", "lock camera", "cheat"),
                setting("qol.farm_keys.attack", "Block Breaking", "Temporary attack/break bind while Farm Keys is on. Blank leaves vanilla.", SettingType.KEYBIND),
                setting("qol.farm_keys.jump", "Jump", "Temporary jump bind while Farm Keys is on. Blank leaves vanilla.", SettingType.KEYBIND),
                setting("qol.farm_keys.lock_camera", "Lock Camera", "Cancel mouse look while Farm Keys is on.", SettingType.TOGGLE, "cheat")));
        modules.add(module(
                "qol.freecam",
                "Free Camera",
                "Detach the camera from the standing player. WASD, jump, and sneak fly the camera. Serveri cheat.",
                Group.RENDER,
                "Camera",
                false,
                true,
                true,
                List.of("freecam", "free camera", "spectator camera", "cheat"),
                setting("qol.freecam.speed", "Speed", "Camera fly speed. Sprint doubles it.", SettingType.NUMBER),
                setting("qol.freecam.show_body", "Show Player Body", "Keep rendering the standing player.", SettingType.TOGGLE),
                setting("qol.freecam.collide", "Collide With Blocks", "Stop the camera on solid blocks. Off is noclip.", SettingType.TOGGLE),
                setting("qol.freecam.keybind", "Keybind", "Toggle Free Camera.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.camera",
                "Camera",
                "Use Minecraft's Toggle Perspective key for first/rear view; skip front view.",
                Group.RENDER,
                "Camera",
                false,
                true,
                true,
                List.of("camera", "perspective", "third person", "f5", "first person", "clip"),
                setting("qol.camera.clip", "Camera Clip", "Let third-person camera pass through blocks.", SettingType.TOGGLE, "cheat"),
                setting("qol.camera.custom_distance", "Custom Distance", "Replace vanilla third-person distance.", SettingType.TOGGLE, "cheat"),
                setting("qol.camera.distance", "Distance", "Third-person camera distance when Custom Distance is on. Default is 4.", SettingType.NUMBER)));
        modules.add(module(
                "qol.mob_highlight",
                "Mob Highlight",
                "Highlight named mobs. Look at one and press Add Entity to remember it. Plus ESP assist.",
                Group.COMBAT,
                "ESP",
                false,
                true,
                true,
                List.of("mob highlight", "esp", "tracer"),
                setting("qol.mob_highlight.highlight_key", "Highlight Key", "Require the add key instead of always highlighting.", SettingType.TOGGLE),
                setting("qol.mob_highlight.add_key", "Key To Add Entity", "Look at a nametag and press to add/remove it.", SettingType.KEYBIND),
                setting("qol.mob_highlight.depth", "Depth Check", "Hide boxes behind blocks.", SettingType.TOGGLE),
                setting("qol.mob_highlight.tracers", "Tracers", "Draw a line to highlighted mobs.", SettingType.TOGGLE),
                setting("qol.mob_highlight.color", "Color", "Box and tracer color for remembered nametags. RGB picker.", SettingType.COLOR)));
        modules.add(module(
                "qol.experiment_solver",
                "Experiments Solver",
                "Plus-only Chronomatron, Ultrasequencer, and Superpairs solution assists.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("experiment", "chronomatron", "ultrasequencer", "superpairs", "enchanting"),
                setting("qol.experiment_solver.chronomatron", "Chronomatron", "Remember and highlight the Chronomatron sequence.", SettingType.TOGGLE),
                setting("qol.experiment_solver.ultrasequencer", "Ultrasequencer", "Remember and highlight the Ultrasequencer order.", SettingType.TOGGLE),
                setting("qol.experiment_solver.superpairs", "Superpairs", "Remember revealed Superpairs items and their matches.", SettingType.TOGGLE),
                setting("qol.experiment_solver.hide_tooltip", "Hide Tooltips", "Hide item tooltips while an experiment GUI is open.", SettingType.TOGGLE),
                setting("qol.experiment_solver.hide_wrong_chronomatron", "Hide Wrong Chronomatron", "Hide Chronomatron slots that are not the next clicks.", SettingType.TOGGLE),
                setting("qol.experiment_solver.hide_wrong_ultrasequencer", "Hide Wrong Ultrasequencer", "Hide Ultrasequencer slots that are not the next clicks.", SettingType.TOGGLE),
                setting("qol.experiment_solver.private_island_only", "Private Island Only", "Only solve while the sidebar says Your Island.", SettingType.TOGGLE),
                setting("qol.experiment_solver.first_color", "Next Slot Color", "Fill color for the next slot to click.", SettingType.COLOR),
                setting("qol.experiment_solver.second_color", "Second Slot Color", "Fill color for the slot after next.", SettingType.COLOR),
                setting("qol.experiment_solver.matched_color", "Matched Color", "Fill color for a Superpairs pair revealed together.", SettingType.COLOR),
                setting("qol.experiment_solver.match_color", "Known Match Color", "Fill color for a revealed item whose twin is known.", SettingType.COLOR),
                setting("qol.experiment_solver.powerup_color", "Powerup Color", "Fill color for Superpairs powerups.", SettingType.COLOR)));
        modules.add(module(
                "qol.dungeon_term_click",
                "Terminal Click Trails",
                "Record left and right clicks in terminal GUIs and draw short trails.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("terminal click", "click trail"),
                setting("qol.dungeon_term_click.radius", "Trail Radius", "Circle radius in GUI pixels.", SettingType.NUMBER),
                setting("qol.dungeon_term_click.thickness", "Trail Thickness", "Trail stroke thickness.", SettingType.NUMBER),
                setting("qol.dungeon_term_click.left_color", "Left Color", "Left-click trail color.", SettingType.COLOR),
                setting("qol.dungeon_term_click.right_color", "Right Color", "Right-click trail color.", SettingType.COLOR)));

        modules.add(module(
                "qol.diana_burrows",
                "Diana Burrows",
                "Spade lava-trail guess (polynomial fit), START/MOB/TREASURE particle burrows, waypoints. Plus only.",
                Group.EVENTS,
                "Diana",
                false,
                true,
                true,
                List.of("diana", "burrow", "griffin", "spade", "mayor"),
                setting("qol.diana_burrows.guess", "Spade Guess", "Polynomial guess from Ancestral Spade ding lava particles.", SettingType.TOGGLE),
                setting("qol.diana_burrows.particles", "Particle Burrows", "Classify nearby START / MOB / TREASURE from particle combos.", SettingType.TOGGLE),
                setting("qol.diana_burrows.waypoints", "Waypoints", "Box guessed and found burrows.", SettingType.TOGGLE),
                setting("qol.diana_burrows.mute_spade", "Mute Bugged Spade", "Mute non-real music.* while digging (mutes the bugged spade sound).", SettingType.TOGGLE),
                setting("qol.diana_burrows.fix_chat", "Filter Duplicate Chat", "Hide duplicate Griffin burrow chat within 400ms.", SettingType.TOGGLE),
                setting("qol.diana_burrows.guess_color", "Guess Color", "Guess waypoint color.", SettingType.COLOR),
                setting("qol.diana_burrows.start_color", "Start Color", "Start burrow color.", SettingType.COLOR),
                setting("qol.diana_burrows.mob_color", "Mob Color", "Mob burrow color.", SettingType.COLOR),
                setting("qol.diana_burrows.treasure_color", "Treasure Color", "Treasure burrow color.", SettingType.COLOR),
                setting("qol.diana_burrows.open_hud_editor", "Edit Diana HUD", "Move the Diana HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.diana_mobs",
                "Diana Mobs",
                "Nametag ESP for Inquisitor and rare mythologicals, plus Griffin pet warning when digging without Griffin.",
                Group.EVENTS,
                "Diana",
                false,
                true,
                true,
                List.of("inquisitor", "griffin", "minos", "diana"),
                setting("qol.diana_mobs.rare_esp", "Rare ESP", "Box Inquisitor / Sphinx / Manticore / King Minos nametags.", SettingType.TOGGLE),
                setting("qol.diana_mobs.griffin_warn", "Griffin Warning", "Title if you dig without a Griffin pet.", SettingType.TOGGLE),
                setting("qol.diana_mobs.esp_color", "ESP Color", "Rare mob box color.", SettingType.COLOR)));

        modules.add(module(
                "qol.diana_profit",
                "Diana Profit",
                "Session-local drop HUD from dug chat (Crown of Greed, relics, coins). Not the mining Current Session ledger.",
                Group.EVENTS,
                "Diana",
                false,
                true,
                true,
                List.of("diana", "profit", "daedalus", "crown of greed"),
                setting("qol.diana_profit.hud", "Drop HUD", "Show session Diana drops.", SettingType.TOGGLE),
                setting("qol.diana_profit.open_hud_editor", "Edit Diana HUD", "Move the Diana HUD.", SettingType.ACTION, "position")));
        // Moved from the standard edition: these cancel vanilla actions or click menus for the player,
        // which Hypixel's modification rules disallow, so the standard jar must not offer them.
        modules.add(module(
                "qol.double_use_fix",
                "Double Use Fix",
                "Stops blaze daggers and fishing rods from activating twice on one click.",
                Group.UTILITIES,
                "Fixes",
                false,
                true,
                true,
                List.of("dagger", "fishing rod", "double use")));
        modules.add(module(
                "qol.pet_keybinds",
                "Pet Keybinds",
                "Page, unequip, and 1-9 pet binds while the Hypixel Pets GUI is open.",
                Group.UTILITIES,
                "Keybinds",
                false,
                true,
                true,
                List.of("pet", "pets"),
                setting("qol.pet_keybinds.unequip", "Unequip", "Unequip active pet.", SettingType.KEYBIND),
                setting("qol.pet_keybinds.next", "Next Page", "Next pets page.", SettingType.KEYBIND),
                setting("qol.pet_keybinds.previous", "Previous Page", "Previous pets page.", SettingType.KEYBIND),
                setting("qol.pet_keybinds.disable_unequip", "Disable Unequip", "Prevent accidental pet unequip.", SettingType.TOGGLE),
                setting("qol.pet_keybinds.close_if_equipped", "Close If Already Equipped", "Close pets UI if already equipped.", SettingType.TOGGLE),
                setting("qol.pet_keybinds.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));
        modules.add(module(
                "qol.loadout_keybinds",
                "Loadout Keybinds",
                "Page and 1-9 / 0 / - / = slot binds while the Hypixel Loadout GUI is open.",
                Group.UTILITIES,
                "Keybinds",
                false,
                true,
                true,
                List.of("loadout", "loadouts"),
                setting("qol.loadout_keybinds.next", "Next Page", "Next loadout page.", SettingType.KEYBIND),
                setting("qol.loadout_keybinds.previous", "Previous Page", "Previous loadout page.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.dungeon_puzzles",
                "Dungeon Puzzles",
                "Oruo quiz answers and a/b/c boxes, Three Weirdos truth chest, Blaze HP order, Ice Fill, Ice Path, Water Board, Boulder, TP Maze, Creeper Beams and Tic Tac Toe.",
                Group.DUNGEONS,
                "Puzzles",
                false,
                true,
                true,
                List.of("quiz", "oruo", "weirdos", "blaze", "trivia", "creeper", "tic tac toe", "ice path"),
                setting("qol.dungeon_puzzles.quiz", "Quiz", "Show the correct Oruo trivia answer.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.quiz_boxes", "Quiz Option Boxes", "Highlight the correct a/b/c block in the Quiz room.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.quiz_timer", "Quiz Timer", "Countdown for Oruo intro (11s) and each remaining question (5s).", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.weirdos", "Three Weirdos", "Mark the truthful NPC chest from chat.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.blaze", "Blaze Order", "Highlight the next blaze by max HP: lowest-first on ice, highest-first on magma.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.ice", "Ice Fill", "Trace the live Ice Fill floors from the hashed room layout.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.ice_path", "Ice Path", "Trace the silverfish Ice Path slides from the hashed room ice grid.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.ice_optimize", "Ice Fill Optimize", "Use the shorter hard Ice Fill paths when the floor identifier matches.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.water", "Water Board", "Highlight Water Board levers from the live wool/chest pattern.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.water_optimized", "Water Optimized", "Use the shorter Water Board lever sequence.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.boulder", "Boulder", "Show Boulder chest-button clicks from the live 7×6 grid.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.tp_maze", "TP Maze", "Follow Teleport Maze pads from the hashed room, then observed hops.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.creeper_beams", "Creeper Beams", "Highlight matching sea-lantern pairs in Creeper Beams.", SettingType.TOGGLE),
                setting("qol.dungeon_puzzles.tic_tac_toe", "Tic Tac Toe", "Mark the next winning stone-button on the item-frame board.", SettingType.TOGGLE)));
        modules.add(module(
                "qol.dungeon_esp",
                "Dungeon ESP",
                "Starred mobs, bats, Fels, Shadow Assassins, keys, mimic chests and teammates.",
                Group.DUNGEONS,
                "ESP",
                false,
                true,
                true,
                List.of("starred", "bat", "fels", "shadow assassin", "wither key", "mimic"),
                setting("qol.dungeon_esp.starred", "Starred Mobs", "Highlight ✯ dungeon mobs.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.starred_color", "Starred Color", "Starred mob box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.bats", "Secret Bats", "Highlight dungeon bats.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.bat_color", "Bat Color", "Bat box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.fels", "Fels", "Highlight Fels and Super Angry Archaeologist.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.fel_color", "Fel Color", "Fels box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.shadow", "Shadow Assassin", "Highlight Shadow Assassins.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.shadow_color", "Shadow Color", "Shadow Assassin box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.keys", "Wither / Blood Keys", "Highlight key holograms.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.key_color", "Wither Key Color", "Wither key box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.blood_key_color", "Blood Key Color", "Blood key box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.mimic", "Mimic", "Highlight Mimic chests and Prince.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.mimic_color", "Mimic Color", "Mimic box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.wither", "Withers", "Highlight Maxor, Storm, Goldor and Necron.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.wither_color", "Wither Color", "Wither box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.crystals", "Crystals / Relics", "Highlight Energy Crystals and M7 relics.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.crystal_color", "Crystal Color", "Crystal and relic box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.secrets", "Secret Tags", "Highlight secret-named holograms.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.secret_color", "Item Color", "Item/essence secret box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.chest_color", "Chest Color", "Chest secret box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.hide_collected", "Hide Collected", "Hide secret boxes after you click them, or when a chest/essence/redstone block is gone.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.secret_clicked", "Secret Clicked", "Box chests, skulls, levers and buttons after you click them.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.secret_clicked_color", "Clicked Color", "Box color for a clicked secret.", SettingType.COLOR),
                setting("qol.dungeon_esp.secret_clicked_locked_color", "Locked Color", "Box color after That chest is locked.", SettingType.COLOR),
                setting("qol.dungeon_esp.secret_clicked_seconds", "Clicked Seconds", "How long clicked-secret boxes stay. Default 7.", SettingType.NUMBER),
                setting("qol.dungeon_esp.secret_clicked_boss", "Clicked In Boss", "Also box clicks inside the boss room.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.items", "Item Highlight", "Box Revive Stone, Trap, Decoy, Architect's First Draft, Spirit Leap and other dungeon drops on the ground.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.iced_mobs", "Iced Mobs", "Highlight frozen dungeon mobs while Ice Spray is in the hotbar.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.fill", "Fill", "Fill ESP boxes.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.opacity", "Fill Opacity", "Fill strength 0-100. Default is 40.", SettingType.NUMBER, "opacity"),
                setting("qol.dungeon_esp.simon", "Simon Says", "Highlight the F7 Simon start button.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.simon_color", "Simon Color", "Simon start-button color.", SettingType.COLOR),
                setting("qol.dungeon_esp.hate_wither", "Wither Doors", "Tint coal wither doors.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.hate_wither_glass", "Wither Glass", "Stained-glass tint for wither doors.", SettingType.ENUM, EmberDungeonPolicy.glassTintNames()),
                setting("qol.dungeon_esp.hate_blood", "Blood Doors", "Tint blood doors.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.hate_blood_glass", "Blood Glass", "Stained-glass tint for blood doors.", SettingType.ENUM, EmberDungeonPolicy.glassTintNames()),
                setting("qol.dungeon_esp.hate_entrance", "Entrance Doors", "Tint entrance doors.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.hate_entrance_glass", "Entrance Glass", "Stained-glass tint for entrance doors.", SettingType.ENUM, EmberDungeonPolicy.glassTintNames()),
                setting("qol.dungeon_esp.livid", "Livid Solver", "Box the real Livid from the F5 ceiling wool.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.livid_color", "Livid Color", "Correct Livid box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.thorn", "F4 Thorn", "Highlight Thorn in F4/M4.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.thorn_color", "Thorn Color", "Thorn box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.spirit_bear", "Spirit Bear", "Highlight Spirit Bear in F4/M4.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.spirit_bear_color", "Spirit Bear Color", "Spirit Bear box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.doors", "Door Highlight", "Title when wither/blood keys spawn or doors open, plus Magical Map wither/blood/entrance boxes in the world. Depth Check hides through walls.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.blood_box", "Blood Camp Box", "3D box and line on Watcher blood mobs, not only a title.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.blood_box_color", "Blood Box Color", "Blood Camp box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.blood_line_color", "Blood Line Color", "Line from you to the blood mob.", SettingType.COLOR),
                setting("qol.dungeon_esp.teammates", "Teammates", "Highlight other players in the dungeon.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.teammate_color", "Teammate Color", "Teammate box color.", SettingType.COLOR),
                setting("qol.dungeon_esp.tracers", "Tracers", "Draw lines to dungeon ESP targets.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.dungeon_terminals",
                "Terminal Solver",
                "Highlight F7 terminal clicks.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("terminal", "panes", "starts with", "click in order", "melody"),
                setting("qol.dungeon_terminals.overlay", "Overlay", "Tint the correct terminal slots.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.color", "Generic Solution", "Correct-slot tint.", SettingType.COLOR),
                setting("qol.dungeon_terminals.numbers", "Numbers", "Overlay Click in order (14 panes on the live 7×2 grid).", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.numbers_show", "Numbers: Show Numbers", "Use 1st/2nd/3rd click colors instead of one tint.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.numbers_show_text", "Numbers: Show Text", "Keep numbered overlay tints instead of collapsing to one color.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.numbers_1_color", "Numbers: 1st Click", "First numbers click.", SettingType.COLOR),
                setting("qol.dungeon_terminals.numbers_2_color", "Numbers: 2nd Click", "Second numbers click.", SettingType.COLOR),
                setting("qol.dungeon_terminals.numbers_3_color", "Numbers: 3rd Click", "Third numbers click.", SettingType.COLOR),
                setting("qol.dungeon_terminals.rubix", "Rubix", "Overlay Change all to same color.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.rubix_pos_color", "Rubix: Positive (+)", "Left-click Rubix panes.", SettingType.COLOR),
                setting("qol.dungeon_terminals.rubix_neg_color", "Rubix: Negative (-)", "Right-click Rubix panes.", SettingType.COLOR),
                setting("qol.dungeon_terminals.rubix_left_only", "Rubix Left Click Only", "Send left clicks for Rubix even when the solver wants right-click.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.colors", "Colors", "Overlay Select all the items, including lime as green.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.colors_solution", "Colors Solution", "Select-all / Colors slot tint.", SettingType.COLOR),
                setting("qol.dungeon_terminals.panes", "Red-Green", "Overlay Correct all the panes on the live 3×5 grid.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.panes_solution", "Panes Solution", "Correct-all-panes slot tint.", SettingType.COLOR),
                setting("qol.dungeon_terminals.starts", "Start-With", "Overlay What starts with on the live 3×7 grid.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.names_solution", "Names Solution", "What-starts-with slot tint.", SettingType.COLOR),
                setting("qol.dungeon_terminals.melody", "Melody", "Solve Click the button on time.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.melody_column_color", "Melody: Column", "Correct Melody column.", SettingType.COLOR),
                setting("qol.dungeon_terminals.melody_indicator_color", "Melody: Indicator", "Ready-to-click Melody slot.", SettingType.COLOR),
                setting("qol.dungeon_terminals.melody_wrong_color", "Melody: Wrong", "Wrong Melody column.", SettingType.COLOR),
                setting("qol.dungeon_terminals.melody_fill", "Melody Fill", "Filled Melody pane tint.", SettingType.COLOR),
                setting("qol.dungeon_terminals.melody_other", "Melody Other", "Other Melody pane tint.", SettingType.COLOR),
                setting("qol.dungeon_terminals.sounds", "Click Sounds", "Play a local note when Auto Terms or a solver click lands.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.click_sound", "Click Sound", "Minecraft sound id, for example block.note_block.pling.", SettingType.TEXT),
                setting("qol.dungeon_terminals.click_pitch", "Click Pitch", "Local click pitch.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.click_volume", "Click Volume", "Local click volume.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.complete_sounds", "Complete Sounds", "Play a local note when the remaining terminal solution is empty.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.stop_tooltips", "Stop Tooltips", "Hide item tooltips while a Floor 7 terminal solver is open.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.hide_clicked", "Hide Clicked", "Hide chest slots that are not part of the remaining solution.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.melody_keys", "Melody Keys", "Press 1-4 to click the live Melody rows (a 3-row chest still uses 1-3).", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.melody_key_1", "Melody Key 1", "Key for Melody row 1.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.melody_key_2", "Melody Key 2", "Key for Melody row 2.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.melody_key_3", "Melody Key 3", "Key for Melody row 3.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.melody_key_4", "Melody Key 4", "Key for Melody row 4.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.protect", "Terminal Protection", "Block GUI close for a short time after a terminal opens.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.protect_ms", "Protect Time", "Milliseconds to keep the terminal open after it appears.", SettingType.NUMBER),
                section("qol.dungeon_terminals.section_overlay", "OVERLAY METRICS"),
                setting("qol.dungeon_terminals.hide_header", "Hide Header", "Hide the vanilla chest header while a terminal is open.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.hide_title", "Hide Title", "Hide the vanilla chest title while a terminal is open.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.header_color", "Header Color", "Reserved header tint when the header stays visible.", SettingType.COLOR),
                setting("qol.dungeon_terminals.ui_scale", "Overlay Scale", "Scale applied to vanilla slot overlay metrics.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.ui_roundness", "Overlay Roundness", "Corner roundness for vanilla slot overlays.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.ui_padding", "Overlay Padding", "Padding around highlighted vanilla slots.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.ui_slot_gap", "Slot Gap", "Gap between highlighted vanilla slots.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.ui_melody_gap", "Melody Gap", "Gap for Melody overlay slots.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.ui_bg", "Overlay Background", "Background tint for overlay metrics.", SettingType.COLOR),
                setting("qol.dungeon_terminals.ui_border", "Overlay Border", "Border tint for overlay metrics.", SettingType.COLOR),
                setting("qol.dungeon_terminals.slots_fill", "Fill Slots", "Fill highlighted vanilla slots instead of outline only.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.slots_roundness", "Slot Roundness", "Roundness applied to highlighted vanilla slots.", SettingType.NUMBER),
                section("qol.dungeon_terminals.section_waypoints", "TERMINAL WAYPOINTS"),
                setting("qol.dungeon_terminals.hitboxes", "Terminal Hitboxes", "Box Inactive Terminal armor stands at the F7 P3 locations.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.hitbox_color", "Hitbox Color", "Inactive terminal box color.", SettingType.COLOR),
                setting("qol.dungeon_terminals.check_class", "Check Dungeon Class", "Only show waypoints assigned to your current dungeon class.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.render_text", "Render Text", "Show S1 T1 / lever labels on waypoint boxes.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.highlight_style", "Highlight Style", "Outline, filled, or both.", SettingType.ENUM, DungeonAthenPortPolicy.HIGHLIGHT_STYLES),
                setting("qol.dungeon_terminals.waypoint_color", "Terminal Color", "P3 terminal waypoint color.", SettingType.COLOR),
                setting("qol.dungeon_terminals.lever_color", "Lever Color", "P3 lever waypoint color.", SettingType.COLOR),
                dungeonClassSetting("qol.dungeon_terminals.s1_term_1", "S1 Terminal 1"),
                dungeonClassSetting("qol.dungeon_terminals.s1_term_2", "S1 Terminal 2"),
                dungeonClassSetting("qol.dungeon_terminals.s1_term_3", "S1 Terminal 3"),
                dungeonClassSetting("qol.dungeon_terminals.s1_term_4", "S1 Terminal 4"),
                dungeonClassSetting("qol.dungeon_terminals.s1_right_lever", "S1 Right Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s1_left_lever", "S1 Left Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s2_term_1", "S2 Terminal 1"),
                dungeonClassSetting("qol.dungeon_terminals.s2_term_2", "S2 Terminal 2"),
                dungeonClassSetting("qol.dungeon_terminals.s2_term_3", "S2 Terminal 3"),
                dungeonClassSetting("qol.dungeon_terminals.s2_term_4", "S2 Terminal 4"),
                dungeonClassSetting("qol.dungeon_terminals.s2_term_5", "S2 Terminal 5"),
                dungeonClassSetting("qol.dungeon_terminals.s2_right_lever", "S2 Right Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s2_left_lever", "S2 Left Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s3_term_1", "S3 Terminal 1"),
                dungeonClassSetting("qol.dungeon_terminals.s3_term_2", "S3 Terminal 2"),
                dungeonClassSetting("qol.dungeon_terminals.s3_term_3", "S3 Terminal 3"),
                dungeonClassSetting("qol.dungeon_terminals.s3_term_4", "S3 Terminal 4"),
                dungeonClassSetting("qol.dungeon_terminals.s3_right_lever", "S3 Right Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s3_left_lever", "S3 Left Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s4_term_1", "S4 Terminal 1"),
                dungeonClassSetting("qol.dungeon_terminals.s4_term_2", "S4 Terminal 2"),
                dungeonClassSetting("qol.dungeon_terminals.s4_term_3", "S4 Terminal 3"),
                dungeonClassSetting("qol.dungeon_terminals.s4_term_4", "S4 Terminal 4"),
                dungeonClassSetting("qol.dungeon_terminals.s4_right_lever", "S4 Right Lever"),
                dungeonClassSetting("qol.dungeon_terminals.s4_left_lever", "S4 Left Lever")));

        return List.copyOf(modules);
    }

    public static List<SettingDef> extraSettings(String moduleId) {
        if (moduleId == null) {
            return List.of();
        }
        return switch (moduleId) {
            case "qol.render_optimizer" -> List.of(
                    setting("qol.render_optimizer.hide_fog", "Hide Fog", "Push environmental fog out of view.", SettingType.TOGGLE, "fog")
            );
            case "qol.fishing_hotspots" -> List.of(
                    setting("qol.fishing_hotspots.radar", "Radar Solver", "Guess hotspot direction from still flame particles while holding a radar.", SettingType.TOGGLE),
                    setting("qol.fishing_hotspots.tracer", "Radar Tracer", "Draw the guessed radar line.", SettingType.TOGGLE));
            case "qol.fishing_helper" -> List.of(
                    setting("qol.fishing_helper.auto_pull", "Auto Pull", "Right-click when a !!! hologram appears near the hook.", SettingType.TOGGLE),
                    setting("qol.fishing_helper.pull_delay", "Delay", "Ticks to wait after a bite before pulling.", SettingType.NUMBER, "1", "ticks"),
                    setting("qol.fishing_helper.pull_variance", "Delay Variance", "Random extra pull delay ticks.", SettingType.NUMBER, "0", "ticks"),
                    setting("qol.fishing_helper.recast", "Auto Recast", "Cast again after a successful pull.", SettingType.TOGGLE),
                    setting("qol.fishing_helper.recast_check", "Recast Check", "If the rod is idle, use it so a hook exists.", SettingType.TOGGLE),
                    setting("qol.fishing_helper.recast_delay", "Recast Delay", "Ticks to wait before recasting.", SettingType.NUMBER, "1", "ticks"),
                    setting("qol.fishing_helper.recast_variance", "Delay Variance", "Random extra recast delay ticks.", SettingType.NUMBER, "0", "ticks")
            );
            case "qol.fishing_creatures" -> List.of(
                    setting("qol.fishing_creatures.rare_party", "Party Ping", "Send /pc with the spawn line. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.fishing_creatures.auto_attack", "Auto Attack", "Left-click while looking at a tracked sea creature. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.fishing_creatures.auto_delay", "Auto Attack Delay", "Ticks between auto-attacks.", SettingType.NUMBER, "4", "ticks")
            );
            case "qol.mining_scatha" -> List.of(
                    setting("qol.mining_scatha.party", "Party Ping", "Send /pc Scatha or Worm. Cheat, off by default.", SettingType.TOGGLE, "cheat")
            );
            case "qol.mining_glacite" -> List.of(
                    setting("qol.mining_glacite.party_share", "Share Corpse Coords", "Send parsed corpse x/y/z on /pc. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.mining_glacite.shaft_party", "Announce Shaft", "Party-chat when a mineshaft portal is found. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.mining_glacite.enter_party", "Enter Party Chat", "Also send the enter line on /pc. Cheat, off by default.", SettingType.TOGGLE, "cheat")
            );
            case "qol.mining_helpers" -> List.of(
                    setting("qol.mining_helpers.call_king", "Call King", "Send /call mismyla after commission complete. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.mining_helpers.break_reset", "Break Reset Fix", "Ignore same-block mining updates that reset break progress.", SettingType.TOGGLE, "mining"),
                    setting("qol.mining_helpers.gemstone_desync", "Gemstone Desync Fix", "Ignore gemstone glass flicker while you are mining that block.", SettingType.TOGGLE, "gemstone")
            );
            case "qol.dungeon_hud" -> List.of(
                    setting("qol.dungeon_hud.melody", "Melody", "Show Melody column progress while in the terminal.", SettingType.TOGGLE),
                    setting("qol.dungeon_hud.quiz", "Quiz / Weirdos", "Show Oruo answers and Three Weirdos truth.", SettingType.TOGGLE),
                    setting("qol.dungeon_hud.map_mode", "Map Mode", "Explored follows the Magical Map. Reveal Hidden hashes loaded rooms and paints them behind wither and blood doors.", SettingType.ENUM, DungeonMapPolicy.MAP_MODES, "cheat"),
                    setting("qol.dungeon_hud.cheater_names", "Hidden Names", "Label hidden rooms when Map Mode is Reveal Hidden.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_hud.cheater_darken", "Darken Hidden", "Darken unopened rooms when Map Mode is Reveal Hidden.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_hud.cheater_darken_factor", "Darken Factor", "0-1 multiplier for hidden tiles.", SettingType.NUMBER)
            );
            case "qol.dungeon_esp" -> List.of(
                    setting("qol.dungeon_esp.hate_doors", "I Hate Doors", "Rewrite wither/blood/entrance door blocks to stained glass on the client.", SettingType.TOGGLE),
                    setting("qol.dungeon_esp.depth", "Depth Check", "Hide boxes behind solid blocks when enabled.", SettingType.TOGGLE),
                    setting("qol.dungeon_esp.secret_waypoints", "Secret Waypoints", "Box every shipped secret in the hashed room you are standing in, through walls. Chest/item/bat/essence/lever labels stay on top of blocks.", SettingType.TOGGLE),
                    setting("qol.dungeon_esp.ghost_block", "Ghost Block", "Client-side air the looked-at block. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.ghost_uayor", "Ghost Confirm", "Required Use-at-your-own-risk gate before ghosting.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.ghost_stonk", "Stonk Ghost", "Ghost the looked-at block when right-clicking a pickaxe.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.ghost_keybind", "Ghost Key", "Hold to ghost the looked-at block.", SettingType.KEYBIND),
                    setting("qol.dungeon_esp.triggerbot", "TriggerBot", "Auto-click F7 crystals and dungeon secrets while looking at them. Not a PvP ragebot.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.trigger_crystal", "Crystal", "Take/place Energy Crystals.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.trigger_take", "Take Crystal", "Right-click crystals to pick them up.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.trigger_place", "Place Crystal", "Right-click to place a held crystal.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.trigger_secret", "Secret Click", "Click chests, skulls, levers and buttons.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_esp.trigger_delay", "Trigger Delay", "Milliseconds between TriggerBot clicks.", SettingType.NUMBER)
            );
            case "qol.dungeon_announce" -> List.of(
                    setting("qol.dungeon_announce.auto_ult", "Auto Ultimate", "Send vanilla Q-drop (Hypixel class ultimate) on Maxor enrage, Goldor, Sadan giants and Livid start.", SettingType.TOGGLE, "cheat")
            );
            case "qol.dungeon_terminals" -> List.of(
                    setting("qol.dungeon_terminals.depth_test", "Depth Test", "Hide waypoint boxes behind blocks when enabled.", SettingType.TOGGLE),
                    section("qol.dungeon_terminals.section_auto", "AUTO TERMS"),
                    setting("qol.dungeon_terminals.auto", "Auto Click", "Click solved terminal slots locally without waiting for the chest to update. Cheat.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.delay", "Click Delay", "Legacy tick delay if min/max ms are equal to zero.", SettingType.NUMBER),
                    setting("qol.dungeon_terminals.min_delay_ms", "Min Delay", "Minimum milliseconds between auto clicks. Default 80.", SettingType.NUMBER),
                    setting("qol.dungeon_terminals.max_delay_ms", "Max Delay", "Maximum milliseconds between auto clicks. Default 160.", SettingType.NUMBER),
                    setting("qol.dungeon_terminals.order", "Click Order", "First, Random, Closest, or Furthest. Closest matches Human Click Order.", SettingType.ENUM, DungeonAthenPortPolicy.TERM_CLICK_ORDERS),
                    setting("qol.dungeon_terminals.first_click_delay", "First Click Delay", "Wait this many milliseconds after a terminal opens before the first click.", SettingType.NUMBER),
                    setting("qol.dungeon_terminals.clone", "Middle Click", "Send CLONE clicks instead of left-click.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.auto_melody", "Auto Melody", "Auto-click Melody when the column matches.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.melody_skip", "Melody Skip", "Queue extra Melody column clicks after the ready pane. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.melody_skip_mode", "Skip Mode", "Edges skips only columns 0 and 4. All skips every ready column.", SettingType.ENUM, DungeonPolicy.MELODY_SKIP_MODES),
                    setting("qol.dungeon_terminals.melody_skip_first_row", "Skip First Row", "Also skip from Melody row 0. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.block_wrong_slots", "Block Wrong Slots", "Cancel chest clicks that are not the next solved slot. Sneak to override. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.human_order", "Human Click Order", "Prefer nearby terminal slots instead of list order.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.drop_key", "Drop Key Click", "Treat the drop key as a click on the hovered solver slot.", SettingType.TOGGLE),
                    setting("qol.dungeon_terminals.keybind_left", "Left Click Bind", "Press to left-click the hovered solver slot. Blank means unbound.", SettingType.KEYBIND),
                    setting("qol.dungeon_terminals.keybind_right", "Right Click Bind", "Press to right-click the hovered solver slot. Blank means unbound.", SettingType.KEYBIND),
                    setting("qol.dungeon_terminals.auto_numbers", "Auto Numbers", "Auto-click Click in order.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.auto_colors", "Auto Colors", "Auto-click Select all the items.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.auto_rubix", "Auto Rubix", "Auto-click Change all to same color.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.auto_panes", "Auto Panes", "Auto-click Correct all the panes.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.auto_starts", "Auto Starts With", "Auto-click What starts with.", SettingType.TOGGLE, "cheat"),
                    section("qol.dungeon_terminals.section_queue", "QUEUE TERMS"),
                    setting("qol.dungeon_terminals.queue", "Queue Terms", "Experimental click queue so terminal clicks register one-by-one. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_terminals.resync_timeout", "Resync Timeout", "Retry predicted or queued clicks if the terminal does not update within this many milliseconds.", SettingType.NUMBER)
            );
            case "qol.dungeon_f7" -> List.of(
                    setting("qol.dungeon_f7.melody_display", "Melody Display", "HUD column progress while Melody is open.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.wither_esp", "Wither ESP", "Per-boss Maxor/Storm/Goldor/Necron box colors.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.maxor_color", "Maxor", "Maxor box color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.storm_color", "Storm", "Storm box color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.goldor_color", "Goldor", "Goldor box color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.necron_color", "Necron", "Necron box color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.dragon_boxes", "Dragon Boxes", "Box the five M7 dragon spawn pads.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.dragon_tracers", "Dragon Tracers", "Draw lines to M7 dragon pads and nearby Wither King dragons.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.gate", "Gate Highlight", "Box the P3 coal-block gates while they still exist.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.gate_color", "Outline", "P3 gate outline color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.relics", "M7 Relics", "Box relic spawn and cauldron pads.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.relic_beacon", "Relic Beacon", "Draw a vertical line from each relic cauldron.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.relic_highlight", "Held Relic Pad", "Box the matching cauldron after you pick up a relic.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.sharp_shooter", "Sharp Shooter", "Box marked and current emerald blocks on the Goldor arrows device, and the next aim spots.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.sharp_aim", "Show Aim Positions", "Box up to three greedy double-shot aim spots. Off by default.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.sharp_complete", "Device Complete Alert", "Local title when you finish the Sharp Shooter device.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.sharp_marked_color", "Marked Color", "Already hit Sharp Shooter blocks.", SettingType.COLOR),
                    setting("qol.dungeon_f7.sharp_target_color", "Target Color", "Current emerald Sharp Shooter block.", SettingType.COLOR),
                    setting("qol.dungeon_f7.sharp_aim1_color", "First Aim Color", "Best aim that covers the current emerald.", SettingType.COLOR),
                    setting("qol.dungeon_f7.sharp_aim2_color", "Second Aim Color", "Second greedy aim.", SettingType.COLOR),
                    setting("qol.dungeon_f7.sharp_aim3_color", "Third Aim Color", "Third greedy aim.", SettingType.COLOR),
                    setting("qol.dungeon_f7.simon", "Simon Says", "Box Simon buttons from the sea-lantern sequence on the device wall.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.simon_progress", "Progress Display", "HUD current Simon stage.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.simon_first_color", "First Color", "Next Simon button.", SettingType.COLOR),
                    setting("qol.dungeon_f7.simon_second_color", "Second Color", "Second Simon button.", SettingType.COLOR),
                    setting("qol.dungeon_f7.simon_other_color", "Other Color", "Rest of the Simon sequence.", SettingType.COLOR),
                    setting("qol.dungeon_f7.simon_sounds", "Simon Sounds", "Play a local note when a Simon start or sequence button is used.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.arrow_align", "Arrow Align", "Show remaining clicks on the F7 arrow item-frame grid.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.i4", "I4 Helper", "Box remaining I4 sea lanterns at 64-68 126-130 50.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.i4_color", "Target Color", "Remaining I4 lantern color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.i4_predict", "Show Prediction", "Highlight the next I4 lantern.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.i4_predict_color", "Prediction Color", "Next I4 lantern color.", SettingType.COLOR),
                    setting("qol.dungeon_f7.hide_diorite", "I Hate Diorite", "Rewrite Maxor pillar diorite to stained glass on the client.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.simon_block_wrong", "Block Wrong Clicks", "Cancel clicks that are not the next Simon button. Sneak to override.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.simon_auto", "Auto Start", "Click the start button when looking at it.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.simon_trigger", "Simon Triggerbot", "Click the next Simon button when looking at it.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.arrow_block_wrong", "Block Wrong Clicks", "Cancel item-frame clicks that are not part of the Arrow Align solution. Sneak to override.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.auto_i4", "Auto I4", "Aim and click remaining I4 lanterns. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.auto_i4_rotation", "I4 Rotation Time", "Milliseconds to interpolate look at the next I4 lantern. 0 snaps instantly.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.auto_i4_rod", "Auto Rod", "Use a fishing rod at Storm death tick 174 while on I4.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.auto_i4_mask", "Auto Mask", "Swap to Bonzo/Spirit Mask at Storm death tick 244 while on I4.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.auto_i4_leap", "Auto Leap", "Open Spirit Leap at tick 307 or after you complete the device.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.auto_i4_leap_melody", "Leap To Melody", "Prefer leaping to the player who opened Melody.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.auto_i4_leap_class", "Leap Priority", "Preferred class if Melody leap is off or missing.", SettingType.ENUM, DungeonPolicy.I4_LEAP_CLASSES),
                    setting("qol.dungeon_f7.debuff_auto", "Auto Debuff", "Use Ice Spray or Gravity Wand from the hotbar when a Debuff phase starts. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.debuff_ice", "Auto Ice Spray", "Swap to Ice Spray Wand for the auto debuff.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.debuff_gravity", "Auto Gravity Wand", "Swap to Gravity Wand if Ice Spray is missing.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.relic_look", "Relic Look", "Rotate toward the matching cauldron after you pick up a relic.", SettingType.TOGGLE),
                    setting("qol.dungeon_f7.relic_look_time", "Relic Look Time", "Milliseconds to interpolate look at the relic cauldron.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.relic_block_wrong", "Block Wrong Relic", "Cancel clicks on the wrong relic cauldron, or a relic pad while not holding a relic or SkyBlock Menu. Sneak is not an override.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.breaker_instamine", "Instamine During Fatigue", "While holding Dungeon Breaker with charges and Mining Fatigue, client-air the looked-at non-secret block. Cheat, off.", SettingType.TOGGLE, "cheat"),
                    section("qol.dungeon_f7.section_superboom", "AUTO SUPERBOOM"),
                    setting("qol.dungeon_f7.auto_superboom", "Auto Superboom", "On left-click, swap to Superboom TNT and use it on cracked/crypt secret walls. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.superboom_swap_back", "Superboom Swap Back", "Return to the previous hotbar slot after booming.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_f7.superboom_delay", "Superboom Delay", "Legacy tick delay if min/max are unused.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.superboom_min_delay", "Min Delay", "Minimum ticks after a Superboom click. Default 1.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.superboom_max_delay", "Max Delay", "Maximum ticks after a Superboom click. Default 3.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.superboom_swap_back_min", "Swap Back Min", "Minimum ticks before swapping back.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.superboom_swap_back_max", "Swap Back Max", "Maximum ticks before swapping back.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.superboom_swap_to", "Swap To", "Return to the original slot or a custom hotbar slot.", SettingType.ENUM, DungeonAthenPortPolicy.SUPERBOOM_SWAP_TO),
                    setting("qol.dungeon_f7.superboom_custom_slot", "Custom Slot", "Hotbar slot 1-9 when Swap To is Custom slot.", SettingType.NUMBER),
                    setting("qol.dungeon_f7.superboom_blocks", "Extra Blocks", "Comma-separated extra breakable ids. Use /rot superboom add while looking at a block.", SettingType.TEXT)
            );
            case "qol.dungeon_party_join" -> List.of(
                    setting("qol.dungeon_party_join.auto_kick", "Auto Kick", "Send /p kick if PB, secrets, secret average, or MP is below the threshold. Cheat, off.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_party_join.required_pb", "Required PB", "Kick if slower than this time, for example 5:30.", SettingType.TEXT),
                    setting("qol.dungeon_party_join.required_secrets", "Required Secrets", "Kick if secrets are below this count, for example 50k.", SettingType.TEXT),
                    setting("qol.dungeon_party_join.required_avg", "Required Secret Avg", "Kick if secret average is below this, for example 8.4.", SettingType.TEXT),
                    setting("qol.dungeon_party_join.required_mp", "Required MP", "Kick if magical power is below this, for example 800.", SettingType.TEXT),
                    setting("qol.dungeon_party_join.kick_message", "Kick Message", "Send a delayed party-chat kick reason.", SettingType.TOGGLE),
                    setting("qol.dungeon_party_join.send_party", "Send In Party", "Use /pc for the kick message.", SettingType.TOGGLE),
                    setting("qol.dungeon_party_join.message_delay", "Message Delay", "Ticks to wait before /p kick and the party message.", SettingType.NUMBER)
            );
            case "qol.dungeon_menus" -> List.of(
                    setting("qol.dungeon_menus.close_chest", "Close Chest", "Close a plain Chest GUI as soon as it opens. Off by default.", SettingType.TOGGLE),
                    setting("qol.dungeon_menus.close_chest.mode", "Close Mode", "Auto closes on open. Any Key closes on the next key or click.", SettingType.ENUM, DungeonF7Policy.CLOSE_CHEST_MODES),
                    setting("qol.dungeon_menus.close_chest_min", "Close Min Delay", "Minimum ticks before Auto close. Default 0.", SettingType.NUMBER),
                    setting("qol.dungeon_menus.close_chest_max", "Close Max Delay", "Maximum ticks before Auto close. Default 1.", SettingType.NUMBER)
            );
            case "qol.experiment_solver" -> List.of(
                    setting("qol.experiment_solver.block_wrong_clicks", "Block Wrong Clicks", "Swallow clicks on puzzle slots that are not the next correct slot.", SettingType.TOGGLE)
            );
            case "qol.etherwarp" -> List.of(
                    setting("qol.etherwarp.depth", "Depth Check", "Allow destination highlights through blocks when off. Plus only.", SettingType.TOGGLE, "cheat"),
                    setting("qol.etherwarp.left_click_warp", "Left Click Warp", "Left-click an Etherwarp item to use it.", SettingType.TOGGLE, "lcew"),
                    setting("qol.etherwarp.shift_automatically", "Shift Automatically", "Hold sneak briefly when left-click warping while standing.", SettingType.TOGGLE)
            );
            case "qol.iota" -> List.of(
                    setting("qol.iota.fix_fishing_hook", "Fix Fishing Hook", "Ignore the extra armor-stand owner that sticks the bobber.", SettingType.TOGGLE),
                    setting("qol.iota.auto_requeue", "Auto Requeue", "After Kuudra is defeated, join the same instance again. Serveri cheat.", SettingType.TOGGLE, "cheat"),
                    setting("qol.iota.toggle_left", "Toggle Left Click", "Latch left auto-click.", SettingType.KEYBIND, "left"),
                    setting("qol.iota.toggle_right", "Toggle Right Click", "Latch right auto-click.", SettingType.KEYBIND, "right")
            );
            case "qol.slayer_highlights" -> List.of(
                    setting("qol.slayer_highlights.depth", "Depth Check", "Hide highlights behind solid blocks when enabled.", SettingType.TOGGLE)
            );
            case "qol.command_keybinds" -> List.of(
                    section("qol.command_keybinds.section_macros", "Macros"),
                    setting("qol.command_keybinds.open_sequence_editor", "Sequence Editor", "Open the visual list/add/edit/delete editor for ordered hotkey actions and delays.", SettingType.ACTION, "macro editor", "sequence"),
                    setting("qol.command_keybinds.macros", "Macros", "One macro per line: KEY[+LIMIT] | message[,,message] | SEND/TYPE/EDIT/CYCLE/RANDOM/REPEAT | ASSERT/SUBMIT/VETO/AVOID | HOLD/VANILLA/RELEASE | spaceTicks | altKey | maxRepeats. Shorthand: P=/pets", SettingType.TEXT, "macros", "macro"),
                    setting("qol.command_keybinds.send_mode", "Default Send", "SEND chats or runs /commands. TYPE opens chat. EDIT selects %edit%. CYCLE/RANDOM/REPEAT walk the message list.", SettingType.ENUM, RingPolicy.SEND_MODES),
                    setting("qol.command_keybinds.conflict", "Default Conflict", "ASSERT also runs vanilla. SUBMIT skips if a Minecraft bind uses the key. VETO cancels vanilla. AVOID never fires from this handler.", SettingType.ENUM, RingPolicy.CONFLICT_STRATEGIES),
                    setting("qol.command_keybinds.activation", "Default Activation", "HOLD stays active while the key is down. VANILLA retriggers on repeat. RELEASE fires when the key comes up.", SettingType.ENUM, RingPolicy.ACTIVATION_TYPES),
                    setting("qol.command_keybinds.ratelimit_count", "Rate Limit Count", "Max macro activations inside the rate-limit window. Default 4.", SettingType.NUMBER, "4", "count"),
                    setting("qol.command_keybinds.ratelimit_ticks", "Rate Limit Ticks", "Window length in ticks. Default 20.", SettingType.NUMBER, "20", "ticks"),
                    setting("qol.command_keybinds.ratelimit_strict", "Strict Rate Limit", "Count blocked presses toward the limiter in strict mode.", SettingType.TOGGLE),
                    setting("qol.command_keybinds.ratelimit_sp", "Rate Limit Singleplayer", "Also apply the limiter in singleplayer. Default off.", SettingType.TOGGLE),
                    setting("qol.command_keybinds.use_ratelimit", "Use Rate Limit", "Apply the limiter to macros. Default on.", SettingType.TOGGLE),
                    setting("qol.command_keybinds.length_limit", "Length Limit", "Block SEND over this many characters. Default 256.", SettingType.NUMBER, "256", "chars"),
                    setting("qol.command_keybinds.add_history", "Add To History", "Add sent macros to chat history.", SettingType.TOGGLE),
                    setting("qol.command_keybinds.show_hud", "Show HUD Message", "Show the sent macro as an overlay.", SettingType.TOGGLE)
            );
            case "qol.stall_market" -> List.of(
                    setting("qol.stall_market.bazaar_search", "Bazaar Search", "Click the Search slot and fill the sign. /rot bazaarsearch also works.", SettingType.TOGGLE, "bazaar"),
                    setting("qol.stall_market.sell_protection", "Sell Protection", "Block high-value Sell Instantly / Sell Sacks / Sell Inventory clicks. Hold Ctrl to override.", SettingType.TOGGLE),
                    setting("qol.stall_market.sell_threshold", "Sell Protection Threshold", "Coin amount above which sells are blocked. Default 1m.", SettingType.NUMBER, "1m"),
                    setting("qol.stall_market.angry_coop", "Angry Co-op Protection", "Block claiming co-op auctions and Claim All on Manage Auctions / Your Bids. Hold Ctrl to override.", SettingType.TOGGLE, "auction"),
                    setting("qol.stall_market.search_keybind", "Search Hovered", "Search the bazaar for the hovered or held item name.", SettingType.KEYBIND)
            );
            case "qol.storage_overlay" -> List.of(
                    setting("qol.storage_overlay.reload_pages", "Reload Storage Pages", "Open every unlocked Ender Chest and Backpack from the server. Later overlay opens only reload pages you clicked; use this to refresh all of them.", SettingType.ACTION, "cache", "reload", "refresh")
            );
            default -> List.of();
        };
    }

    private static SettingDef[] worldScannerSettings() {
        List<SettingDef> settings = new ArrayList<>();
        settings.add(setting("qol.world_scanner.only_hollows", "Only Crystal Hollows", "Scan only when the area detector says Crystal Hollows.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.crystals", "Scan Crystals", "Master toggle for Goblin King, Goblin Queen, Mines of Divan, Precursor City, Jungle Temple, Khazad-dûm.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.mob_spots", "Scan Mob Spots", "Master toggle for Corleone, Key Guardian, Xalx, Pete, Odawa.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.fairy", "Scan Fairy Grottos", "Master toggle for Fairy Grotto finds.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.dragon", "Scan Dragon Nest", "Master toggle for Golden Dragon nest finds.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.worm", "Scan Worm Fishing", "Master toggle for worm-fishing lava spots.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.lava_esp", "Lava ESP", "Highlight nearby lava surfaces.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.water_esp", "Water ESP", "Highlight nearby water surfaces.", SettingType.TOGGLE));
        settings.add(setting("qol.world_scanner.rat_hitboxes", "Rat Hitboxes", "Box baby zombies in the Hub so rats are easier to click.", SettingType.TOGGLE, "rat"));
        settings.add(setting("qol.world_scanner.esp_range", "ESP Range", "Fluid ESP distance in blocks.", SettingType.NUMBER));
        for (WorldScannerEspSettings.Spec spec : WorldScannerEspSettings.TARGETS) {
            String prefix = "qol.world_scanner.target." + spec.id();
            settings.add(section(prefix, spec.label()));
            settings.add(setting(prefix + ".enabled", "Enable", "Scan and highlight " + spec.label() + ".", SettingType.TOGGLE));
            settings.add(setting(prefix + ".style", "Highlight Style", "Outline, filled, or both.", SettingType.ENUM, WorldScannerEspSettings.STYLES));
            settings.add(setting(prefix + ".color", "ESP Color", spec.label() + " highlight color.", SettingType.COLOR));
            settings.add(setting(prefix + ".tracer", "Tracer", "Draw a line to this find.", SettingType.TOGGLE));
            settings.add(setting(prefix + ".name", "Display Name", "Show the structure name and distance above the waypoint.", SettingType.TOGGLE));
            settings.add(setting(prefix + ".chat", "Send Coords In Chat", "Print coordinates when found.", SettingType.TOGGLE));
            settings.add(setting(prefix + ".notify", "Show Notification", "Also print a short found message.", SettingType.TOGGLE));
        }
        return settings.toArray(SettingDef[]::new);
    }
}

