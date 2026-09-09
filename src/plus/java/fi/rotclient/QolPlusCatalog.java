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
        return List.copyOf(modules);
    }

    public static List<SettingDef> extraSettings(String moduleId) {
        if (moduleId == null) {
            return List.of();
        }
        return switch (moduleId) {
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
                    setting("qol.mining_helpers.call_king", "Call King", "Send /call mismyla after commission complete. Cheat, off by default.", SettingType.TOGGLE, "cheat")
            );
            case "qol.dungeon_hud" -> List.of(
                    setting("qol.dungeon_hud.map_mode", "Map Mode", "Explored follows the Magical Map. Reveal Hidden hashes loaded rooms and paints them behind wither and blood doors.", SettingType.ENUM, DungeonMapPolicy.MAP_MODES, "cheat"),
                    setting("qol.dungeon_hud.cheater_names", "Hidden Names", "Label hidden rooms when Map Mode is Reveal Hidden.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_hud.cheater_darken", "Darken Hidden", "Darken unopened rooms when Map Mode is Reveal Hidden.", SettingType.TOGGLE, "cheat"),
                    setting("qol.dungeon_hud.cheater_darken_factor", "Darken Factor", "0-1 multiplier for hidden tiles.", SettingType.NUMBER)
            );
            case "qol.dungeon_esp" -> List.of(
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
                    setting("qol.dungeon_party_join.auto_kick", "Auto Kick", "Send /p kick if PB, secrets, secret average, or MP is below the threshold. Cheat, off.", SettingType.TOGGLE, "cheat")
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
                    setting("qol.etherwarp.left_click_warp", "Left Click Warp", "Left-click an Etherwarp item to use it.", SettingType.TOGGLE, "lcew"),
                    setting("qol.etherwarp.shift_automatically", "Shift Automatically", "Hold sneak briefly when left-click warping while standing.", SettingType.TOGGLE)
            );
            case "qol.iota" -> List.of(
                    setting("qol.iota.auto_requeue", "Auto Requeue", "After Kuudra is defeated, join the same instance again. Serveri cheat.", SettingType.TOGGLE, "cheat"),
                    setting("qol.iota.toggle_left", "Toggle Left Click", "Latch left auto-click.", SettingType.KEYBIND, "left"),
                    setting("qol.iota.toggle_right", "Toggle Right Click", "Latch right auto-click.", SettingType.KEYBIND, "right")
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
            default -> List.of();
        };
    }
}

