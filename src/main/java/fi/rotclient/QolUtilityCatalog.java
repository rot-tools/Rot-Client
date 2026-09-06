package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Minecraft-free catalog of QoL & Settings utility modules and their settings.
 * Searchable labels live here so Settings search can surface parent modules.
 */
public final class QolUtilityCatalog {
    public enum Group {
        COMBAT("Combat"),
        SLAYER("Slayer"),
        EVENTS("Events"),
        FISHING("Fishing"),
        FORAGING("Foraging"),
        GARDEN("Garden"),
        GUI("GUI"),
        DUNGEONS("Dungeons"),
        KUUDRA("Kuudra"),
        MINING("Mining"),
        UTILITIES("Utilities"),
        HUD_DISPLAY("HUD & Display"),
        RENDER("Render"),
        INTERFACE("Interface");

        private final String title;

        Group(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        public static Group fromId(String raw) {
            if (raw == null || raw.isBlank()) {
                return COMBAT;
            }
            String needle = raw.trim();
            for (Group group : values()) {
                if (group.name().equalsIgnoreCase(needle)
                        || group.title.equalsIgnoreCase(needle)) {
                    return group;
                }
            }
            return COMBAT;
        }

        public String sidebarSubtitle() {
            return switch (this) {
                case COMBAT -> "Clicker, warp, ESP";
                case SLAYER -> "Boss HUD, alerts, carry";
                case EVENTS -> "Diana burrows and drops";
                case FISHING -> "Bite, creatures, trophy";
                case FORAGING -> "Trees, beacon, Galatea";
                case GARDEN -> "Farm keys";
                case GUI -> "Custom scoreboard and overlays";
                case DUNGEONS -> "Secrets and dungeon helpers";
                case KUUDRA -> "Waypoints, party, alerts";
                case MINING -> "Tracker, powder, scanner, commissions, Glacite";
                case UTILITIES -> "Keybinds, chat, market";
                case HUD_DISPLAY -> "Overlays, layout, appearance";
                case RENDER -> "Viewmodel and camera";
                case INTERFACE -> "Menus, storage, inventory";
            };
        }

        public String pageDescription() {
            return switch (this) {
                case COMBAT -> "Auto clicker, trajectories, Etherwarp, and combat ESP.";
                case SLAYER -> "Shared Slayer engine: HUD, fight helpers, drops, and carry.";
                case EVENTS -> "Griffin burrows, Diana mobs, profit HUD, and share helpers.";
                case FISHING -> "Auto-pull, sea creatures, hotspots, trophy, and fishing HUD for the Serveri.";
                case FORAGING -> "Galatea/Park/Torrhus tree HUD, audio mutes, temple/beacon helpers.";
                case GARDEN -> "Garden farming key remaps. Cheat options stay off by default.";
                case GUI -> "Custom SkyBlock sidebar, hide vanilla, and board placement.";
                case DUNGEONS -> "Secrets, ESP, terminals, puzzles, and dungeon HUD.";
                case KUUDRA -> "Kuudra waypoints, Fresh Tools, party commands, and fight HUDs.";
                case MINING -> "Mining Tracker, powder chests, session pages, scanner, commissions, Glacite, and helpers.";
                case UTILITIES -> "Keybinds, chat helpers, market guard, and everyday tools.";
                case HUD_DISPLAY -> "On-screen stats, HUD layout, appearance, custom cursor, and tooltip extras.";
                case RENDER -> "Local visual filters, viewmodel, scale, and camera.";
                case INTERFACE -> "Storage, inventory chrome, SkyBlock menus, and how the dashboard opens.";
            };
        }
    }

    public enum SettingType {
        TOGGLE,
        ENUM,
        NUMBER,
        COLOR,
        KEYBIND,
        TEXT,
        ACTION,
        SECTION,
        SQUARE
    }

    public record SettingDef(
            String id,
            String label,
            String description,
            SettingType type,
            List<String> enumOptions,
            List<String> searchAliases) {
        public SettingDef {
            id = id == null ? "" : id;
            label = label == null ? "" : label;
            description = description == null ? "" : description;
            type = type == null ? SettingType.TOGGLE : type;
            enumOptions = enumOptions == null ? List.of() : List.copyOf(enumOptions);
            searchAliases = searchAliases == null ? List.of() : List.copyOf(searchAliases);
        }
    }

    public record ModuleDef(
            String id,
            String name,
            String description,
            Group group,
            String section,
            boolean wip,
            boolean toggleable,
            boolean runtimeReady,
            List<SettingDef> settings,
            List<String> searchAliases) {
        public ModuleDef {
            id = id == null ? "" : id;
            name = name == null ? "" : name;
            description = description == null ? "" : description;
            group = group == null ? Group.UTILITIES : group;
            section = section == null ? "" : section.trim();
            settings = settings == null ? List.of() : List.copyOf(settings);
            searchAliases = searchAliases == null ? List.of() : List.copyOf(searchAliases);
        }

        public QolModuleEvidence.Status evidenceStatus() {
            return QolModuleEvidence.status(this);
        }
    }

    private static final List<ModuleDef> MODULES = buildModules();

    private QolUtilityCatalog() {
    }

    public static List<ModuleDef> modules() {
        return MODULES;
    }

    public static ModuleDef findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        String needle = id.trim().toLowerCase(Locale.ROOT);
        needle = switch (needle) {
            case "qol.missing_enchants", "qol.custom_tooltip",
                    "qol.info_tooltips", "qol.price_tooltips" -> "qol.item_tooltips";
            default -> needle;
        };
        for (ModuleDef module : MODULES) {
            if (module.id().equalsIgnoreCase(needle)) {
                return module;
            }
        }
        // Child setting id → parent module
        for (ModuleDef module : MODULES) {
            if (needle.startsWith(module.id().toLowerCase(Locale.ROOT) + ".")) {
                return module;
            }
            for (SettingDef setting : module.settings()) {
                if (setting.id().equalsIgnoreCase(needle)) {
                    return module;
                }
            }
        }
        return null;
    }

    public static List<ModuleDef> modulesInGroup(Group group) {
        List<ModuleDef> out = new ArrayList<>();
        for (ModuleDef module : MODULES) {
            if (module.group() == group) {
                out.add(module);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Appearance and HUD Elements Editor live under Visuals, not as
     * Modules → HUD &amp; Display cards. Catalog ids stay in this group.
     */
    public static boolean hiddenFromGroupPage(ModuleDef module) {
        return module != null && VisualsLandingNavPolicy.hiddenFromGroupPage(module.id());
    }

    public static List<ModuleDef> modulesOnGroupPage(Group group) {
        List<ModuleDef> out = new ArrayList<>();
        for (ModuleDef module : modulesInGroup(group)) {
            if (!hiddenFromGroupPage(module)) {
                out.add(module);
            }
        }
        return List.copyOf(out);
    }

    /**
     * Sidebar order: combat and island gameplay first, then HUD/render/interface,
     * with Utilities last as the leftover toolbox.
     */
    private static final List<Group> SIDEBAR_ORDER = List.of(
            Group.COMBAT,
            Group.SLAYER,
            Group.EVENTS,
            Group.DUNGEONS,
            Group.KUUDRA,
            Group.MINING,
            Group.FISHING,
            Group.FORAGING,
            Group.GARDEN,
            Group.GUI,
            Group.HUD_DISPLAY,
            Group.RENDER,
            Group.INTERFACE,
            Group.UTILITIES);

    public static List<Group> sidebarPages() {
        List<Group> pages = new ArrayList<>();
        for (Group group : SIDEBAR_ORDER) {
            if (!modulesInGroup(group).isEmpty()) {
                pages.add(group);
            }
        }
        for (Group group : Group.values()) {
            if (!pages.contains(group) && !modulesInGroup(group).isEmpty()) {
                pages.add(group);
            }
        }
        return List.copyOf(pages);
    }

    public static boolean hasCheatTag(ModuleDef module) {
        if (module == null) {
            return false;
        }
        if (isGameplayCheatId(module.id())) {
            return true;
        }
        if (containsCheatToken(module.searchAliases())
                || containsCheatPhrase(module.description())) {
            return true;
        }
        for (SettingDef setting : module.settings()) {
            if (containsCheatToken(setting.searchAliases())
                    || containsCheatPhrase(setting.description())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gameplay automation and other illegal helpers. Auto Sprint stays off this
     * list: it only holds vanilla sprint, it does not click or solve for you.
     */
    static boolean isGameplayCheatId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        if (id.endsWith("_cheats")) {
            return true;
        }
        if (id.startsWith("qol.auto_") && !id.equals("qol.auto_sprint")) {
            return true;
        }
        return switch (id) {
            case "qol.cheater_wardrobe",
                    "qol.farm_keys",
                    "qol.inventory_walk",
                    "qol.freecam",
                    "qol.experiment_solver",
                    "qol.fishing_helper",
                    "qol.secret_hitboxes",
                    "qol.diana_share",
                    "qol.slayer_auto_start",
                    "qol.slayer_auto_soulcry",
                    "qol.dungeon_hover_terms",
                    "qol.dungeon_soulsand",
                    "qol.dungeon_party_join" -> true;
            default -> false;
        };
    }

    private static boolean containsCheatToken(List<String> aliases) {
        if (aliases == null) {
            return false;
        }
        for (String alias : aliases) {
            if (alias != null && alias.equalsIgnoreCase("cheat")) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsCheatPhrase(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        return lower.contains("cheat,")
                || lower.contains("cheat ")
                || lower.startsWith("cheat")
                || lower.contains("serveri cheat");
    }

    private static List<ModuleDef> buildModules() {
        List<ModuleDef> modules = new ArrayList<>();

        modules.add(module(
                "qol.custom_scoreboard",
                "Custom Scoreboard",
                "Rebuild the SkyBlock sidebar with your own row order, numbers, and panel.",
                Group.GUI,
                "Board",
                false,
                true,
                true,
                List.of("sidebar", "scoreboard", "custom board", "hypixel sidebar"),
                section("qol.custom_scoreboard.section_display", "Display"),
                setting("qol.custom_scoreboard.appearance", "Appearance", "One board slot per line. Use Title, Purse, Empty, Events, and the other slot names. Order is top to bottom.", SettingType.TEXT, "rows", "slots"),
                setting("qol.custom_scoreboard.reset_appearance", "Reset Appearance", "Restore the default slot list.", SettingType.ACTION, "reset rows"),
                setting("qol.custom_scoreboard.open_hud_editor", "Move Overlay", "Open the HUD editor to drag the board. Dragging switches alignment to Don't Align.", SettingType.ACTION, "position"),
                setting("qol.custom_scoreboard.hide_vanilla", "Hide Vanilla Sidebar", "Hide Minecraft's scoreboard while this board is showing. Default on.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.use_custom_lines", "Use Custom Lines", "Rebuild rows from the appearance list. Off keeps Hypixel's own lines inside Rot's panel.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.show_diff", "Show Number Diff", "After purse, bits, and similar values change, show a short (+1.2k) note for 5 seconds.", SettingType.TOGGLE, "delta"),
                setting("qol.custom_scoreboard.show_unclaimed_bits", "Unclaimed Bits", "Show unclaimed bits next to Bits when tab lists them.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.show_max_players", "Max Island Players", "Show current/max players when Hypixel lists a cap.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.show_outside_skyblock", "Show Outside SkyBlock", "Keep the panel on non-SkyBlock worlds using the vanilla sidebar text.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.cache_on_switch", "Cache On Island Switch", "Keep the last board for a few seconds after a world change so the panel does not blink empty.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.keybind", "Keybind", "Toggle this module. Blank means unbound.", SettingType.KEYBIND),
                section("qol.custom_scoreboard.section_numbers", "Numbers"),
                setting("qol.custom_scoreboard.powder_display", "Powder Display", "Available powder, total, or both.", SettingType.ENUM, CustomScoreboardPolicy.powderOptions()),
                setting("qol.custom_scoreboard.number_format", "Number Format", "Comma grouping or compact 1.2M.", SettingType.ENUM, CustomScoreboardPolicy.numberStyleOptions()),
                setting("qol.custom_scoreboard.number_layout", "Number Layout", "How labels and colored numbers sit on one row.", SettingType.ENUM, CustomScoreboardPolicy.numberLayoutOptions()),
                setting("qol.custom_scoreboard.time_24h", "SkyBlock Time 24h", "Use 24-hour SkyBlock clock when Time is in the appearance list.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.time_exact", "Exact Minutes", "Show SkyBlock minutes instead of rounding to the nearest 10.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.date_in_lobby", "Date In Lobby Line", "Prefix the lobby code with today's date.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.date_format", "Date Format", "Pattern for the lobby date prefix.", SettingType.ENUM, CustomScoreboardPolicy.dateFormatOptions()),
                section("qol.custom_scoreboard.section_align", "Alignment"),
                setting("qol.custom_scoreboard.line_spacing", "Line Spacing", "Extra pixels between rows. 10 is the vanilla-like default.", SettingType.NUMBER),
                setting("qol.custom_scoreboard.text_align", "Text Alignment", "Left, center, or right inside the panel.", SettingType.ENUM, CustomScoreboardPolicy.alignOptions()),
                setting("qol.custom_scoreboard.show_profile_name", "Show Profile Name", "Use the profile name from tab instead of Ironman/Bingo/Stranded.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.align_h", "Horizontal Align", "Pin the panel left, center, or right. Don't Align uses the HUD editor position.", SettingType.ENUM, CustomScoreboardPolicy.alignOptions()),
                setting("qol.custom_scoreboard.align_v", "Vertical Align", "Pin the panel top, center, or bottom. Don't Align uses the HUD editor position.", SettingType.ENUM, CustomScoreboardPolicy.valignOptions()),
                setting("qol.custom_scoreboard.margin", "Screen Margin", "Gap from the screen edge when alignment is pinned.", SettingType.NUMBER),
                section("qol.custom_scoreboard.section_arrows", "Arrows"),
                setting("qol.custom_scoreboard.arrow_mode", "Arrow Display", "Remaining count or percent of quiver.", SettingType.ENUM, CustomScoreboardPolicy.arrowOptions()),
                setting("qol.custom_scoreboard.color_arrows", "Color By Percent", "Tint the arrow line green/yellow/red from remaining percent.", SettingType.TOGGLE),
                section("qol.custom_scoreboard.section_stats", "Chunked Stats"),
                setting("qol.custom_scoreboard.chunked_stats", "Chunked Stats", "One stat per line: Health, Defense, Mana, Overflow, Speed, Vitality.", SettingType.TEXT),
                setting("qol.custom_scoreboard.max_stats_per_line", "Max Stats Per Line", "How many chunked stats share one row.", SettingType.NUMBER),
                section("qol.custom_scoreboard.section_events", "Events"),
                setting("qol.custom_scoreboard.event_priority", "Event Priority", "One event name per line. First match wins unless Show All Active is on.", SettingType.TEXT),
                setting("qol.custom_scoreboard.reset_events", "Reset Event Priority", "Restore the default event order.", SettingType.ACTION),
                setting("qol.custom_scoreboard.show_all_events", "Show All Active Events", "List every matching event instead of only the first.", SettingType.TOGGLE),
                section("qol.custom_scoreboard.section_maxwell", "Maxwell"),
                setting("qol.custom_scoreboard.show_magical_power", "Magical Power", "Show Magical Power above tunings.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.compact_tuning", "Compact Tuning", "Join tunings on one row.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.tuning_amount", "Tuning Amount", "How many tuning rows to keep when compact is off.", SettingType.NUMBER),
                section("qol.custom_scoreboard.section_mayor", "Mayor"),
                setting("qol.custom_scoreboard.show_mayor_perks", "Mayor Perks", "List mayor perks under the mayor name.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.show_mayor_time", "Next Mayor Time", "Show the remaining mayor term when tab lists it.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.show_extra_mayor", "Minister / Extra Mayor", "Show the minister line when present.", SettingType.TOGGLE),
                section("qol.custom_scoreboard.section_party", "Party"),
                setting("qol.custom_scoreboard.max_party", "Max Party Members", "How many party names to list before +N.", SettingType.NUMBER),
                setting("qol.custom_scoreboard.party_everywhere", "Party Everywhere", "Show party outside party islands.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.show_party_leader", "Show Party Leader", "Keep the leader star row.", SettingType.TOGGLE),
                section("qol.custom_scoreboard.section_title", "Title"),
                setting("qol.custom_scoreboard.title_align", "Title Alignment", "Alignment for the title row.", SettingType.ENUM, CustomScoreboardPolicy.alignOptions()),
                setting("qol.custom_scoreboard.use_custom_title", "Use Custom Title", "Replace SKYBLOCK with the custom title string.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.custom_title", "Custom Title", "Use && for colors (&&6&&lSKYBLOCK) and \\n for extra lines.", SettingType.TEXT),
                setting("qol.custom_scoreboard.custom_title_outside", "Custom Title Outside SkyBlock", "Also use the custom title off SkyBlock.", SettingType.TOGGLE),
                section("qol.custom_scoreboard.section_footer", "Footer"),
                setting("qol.custom_scoreboard.footer_align", "Footer Alignment", "Alignment for the footer row.", SettingType.ENUM, CustomScoreboardPolicy.alignOptions()),
                setting("qol.custom_scoreboard.custom_footer", "Custom Footer", "Footer text. && for colors, \\n for extra lines.", SettingType.TEXT),
                setting("qol.custom_scoreboard.custom_alpha_footer", "Alpha Footer", "Footer used on Hypixel Alpha.", SettingType.TEXT),
                section("qol.custom_scoreboard.section_background", "Background"),
                setting("qol.custom_scoreboard.bg_enabled", "Background", "Draw the rounded panel behind the text.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.bg_color", "Background Color", "Panel fill, including alpha.", SettingType.COLOR),
                setting("qol.custom_scoreboard.bg_border", "Border Padding", "Inner padding around the text.", SettingType.NUMBER),
                setting("qol.custom_scoreboard.bg_round", "Corner Radius", "Rounded-corner size.", SettingType.NUMBER),
                setting("qol.custom_scoreboard.outline", "Outline", "Draw a gradient outline around the panel.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.outline_thickness", "Outline Thickness", "Outline width in pixels.", SettingType.NUMBER),
                setting("qol.custom_scoreboard.outline_blur", "Outline Softness", "0 is sharp, 1 is softer.", SettingType.NUMBER),
                setting("qol.custom_scoreboard.outline_top", "Outline Top Color", "Gradient start at the top edge.", SettingType.COLOR),
                setting("qol.custom_scoreboard.outline_bottom", "Outline Bottom Color", "Gradient end at the bottom edge.", SettingType.COLOR),
                setting("qol.custom_scoreboard.custom_bg_image", "Custom Background Tint", "Apply the opacity slider to the fill. Image files are not loaded yet.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.custom_bg_opacity", "Background Opacity", "0-100 fill opacity when custom tint is on.", SettingType.NUMBER),
                section("qol.custom_scoreboard.section_hide", "Hide"),
                setting("qol.custom_scoreboard.hide_empty", "Hide Empty Lines", "Skip slots with no current value.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.hide_consecutive_empty", "Hide Consecutive Empty", "Do not stack two blank spacer rows.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.hide_empty_edges", "Hide Empty Edges", "Trim blank rows at the top and bottom.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.hide_irrelevant", "Hide Irrelevant Lines", "Skip Heat off Crimson, Motes off Rift, and similar location gates.", SettingType.TOGGLE),
                setting("qol.custom_scoreboard.unknown_warning", "Unknown Line Chat", "Print a chat note when a leftover sidebar line is not mapped.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.command_keybinds",
                "Hotkey Macros",
                "Macros plus one-press SkyBlock menu commands.",
                Group.UTILITIES,
                "Keybinds",
                false,
                true,
                true,
                List.of("pets", "storage", "wardrobe", "commands", "macros", "placeholders"),
                setting("qol.command_keybinds.pets", "Pets", "Sends the pets-menu command when you press this key. Blank means unbound.", SettingType.KEYBIND),
                setting("qol.command_keybinds.storage", "Storage", "Sends the storage command when you press this key. Blank means unbound.", SettingType.KEYBIND),
                setting("qol.command_keybinds.armor_wardrobe", "Armor Wardrobe", "Sends /armor to open the Armor Wardrobe.", SettingType.KEYBIND),
                setting("qol.command_keybinds.equip_wardrobe", "Equip Wardrobe", "Sends /equipment to open equipment slots.", SettingType.KEYBIND),
                setting("qol.command_keybinds.loadouts", "Loadouts", "Sends /loadout to open the loadout GUI.", SettingType.KEYBIND, "loadout", "equipment"),
                setting("qol.command_keybinds.stats", "Stats", "Sends the stats-menu command when you press this key.", SettingType.KEYBIND),
                setting("qol.command_keybinds.dungeon_hub", "Dungeon Hub", "Warps to the Dungeon Hub when you press this key.", SettingType.KEYBIND, "dungeon"),
                setting("qol.command_keybinds.potion_bag", "Potion Bag", "Opens the potion bag when you press this key.", SettingType.KEYBIND, "potion"),
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
                setting("qol.command_keybinds.show_hud", "Show HUD Message", "Show the sent macro as an overlay.", SettingType.TOGGLE)));

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
                "qol.chat_commands",
                "Chat Commands",
                "Party, guild, and private !command helpers plus outgoing emote tokens. Serveri convenience only.",
                Group.UTILITIES,
                "Chat",
                false,
                true,
                true,
                List.of("chat", "emotes", "party", "guild"),
                setting("qol.chat_commands.emotes", "Chat Emotes", "Replace tokens such as <3 in outgoing chat.", SettingType.TOGGLE, "emote"),
                setting("qol.chat_commands.party", "Party Commands", "Parse !commands in party chat.", SettingType.TOGGLE),
                setting("qol.chat_commands.guild", "Guild Commands", "Parse !commands in guild chat.", SettingType.TOGGLE),
                setting("qol.chat_commands.private", "Private Commands", "Parse !commands in private messages.", SettingType.TOGGLE),
                setting("qol.chat_commands.shortcuts", "Command Shortcuts", "One alias per line: ah=/ah or bz -> /bz. Typed as the first chat token.", SettingType.TEXT, "alias", "shortcut"),
                setting("qol.chat_commands.rules", "Chat Rules", "One per line: hide regex  or  replace regex => text. Invalid regex is skipped.", SettingType.TEXT, "filter", "hide"),
                setting("qol.chat_commands.previous_server", "Previous Server", "Local chat when you rejoin a Hypixel instance you were on recently.", SettingType.TOGGLE),
                setting("qol.chat_commands.previous_server_time", "Previous Server Time", "How long a server stays in the previous-server cache, in seconds.", SettingType.NUMBER, "360s"),
                setting("qol.chat_commands.queue_estimate", "Queue Estimate", "Estimate Hypixel queue wait from position samples.", SettingType.TOGGLE),
                setting("qol.chat_commands.quick_join", "Quick Join Button", "Add a Hypixel join button on the title screen.", SettingType.TOGGLE),
                setting("qol.chat_commands.quick_join_text", "Quick Join Text", "Title-screen button label. Use {ip} for the address.", SettingType.TEXT, "hypixel"),
                setting("qol.chat_commands.quick_join_ip", "Quick Join IP", "Server address the title-screen button connects to.", SettingType.TEXT, "ip"),
                setting("qol.chat_commands.slot_machine", "Prize Reel", "Play a local slot-style reel when Vanguard chat lists loot. Overlay only.", SettingType.TOGGLE, "vanguard", "spin"),
                setting("qol.chat_commands.keybind", "Keybind", "Toggle this module.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.no_cursor_reset",
                "No Cursor Reset",
                "Keep the mouse pointer where it was when you close one supported chest and open another, instead of snapping back to the center.",
                Group.INTERFACE,
                "GUI",
                false,
                true,
                true,
                List.of("cursor", "mouse", "unhook"),
                setting("qol.no_cursor_reset.unhook_timeout", "Unhook Timeout", "How long, in milliseconds, the cursor stay-put hook stays active after a supported GUI swap.", SettingType.NUMBER, "150ms", "timeout"),
                setting("qol.no_cursor_reset.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.player_display",
                "Player Display",
                "Custom SkyBlock stat HUDs and vanilla HUD hide options.",
                Group.HUD_DISPLAY,
                "Overlays",
                false,
                true,
                true,
                List.of("health", "mana", "defense", "ehp", "speed", "vitality", "overflow"),
                section("qol.player_display.section_general", "GENERAL"),
                setting("qol.player_display.show_icons", "Show Icons", "Show supported stat icons.", SettingType.TOGGLE, "icons"),
                setting("qol.player_display.hide_elements", "Hide Elements", "Hide selected vanilla HUD elements. Applies only while Player Display is enabled.", SettingType.SECTION),
                setting("qol.player_display.hide_vanilla_health", "Hide Vanilla Health", "Local hide of vanilla health bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_vanilla_food", "Hide Vanilla Food", "Local hide of vanilla food bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_vanilla_armor", "Hide Vanilla Armor", "Local hide of vanilla armor bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_vanilla_xp", "Hide Vanilla XP", "Local hide of vanilla experience level. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_bar", "Hide Action Bar Elements", "Hide selected SkyBlock action-bar stats. Applies only while Player Display is enabled.", SettingType.SECTION, "action bar"),
                setting("qol.player_display.hide_action_health", "Hide Action Health", "Strip health fragment from action bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_defense", "Hide Action Defense", "Strip defense fragment from action bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_mana", "Hide Action Mana", "Strip mana fragment from action bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_overflow", "Hide Action Overflow", "Strip overflow fragment from action bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_speed", "Hide Action Speed", "Strip speed fragment from action bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_vitality", "Hide Action Vitality", "Strip vitality fragment from action bar. Player Display must be on.", SettingType.TOGGLE),
                setting("qol.player_display.hide_action_location", "Hide Action Location", "Strip the SkyBlock zone name Hypixel inserts into the action bar when you change areas. Player Display must be on.", SettingType.TOGGLE),
                section("qol.player_display.section_huds", "HUD ELEMENTS"),
                setting("qol.player_display.health_hud", "Health HUD", "Show Health HUD.", SettingType.TOGGLE, "health"),
                setting("qol.player_display.mana_hud", "Mana HUD", "Show Mana HUD.", SettingType.TOGGLE, "mana"),
                setting("qol.player_display.overflow_mana_hud", "Overflow Mana HUD", "Show Overflow Mana HUD.", SettingType.TOGGLE),
                setting("qol.player_display.defense_hud", "Defense HUD", "Show Defense HUD.", SettingType.TOGGLE, "defense"),
                setting("qol.player_display.vitality_hud", "Vitality HUD", "Show Vitality HUD.", SettingType.TOGGLE, "vitality"),
                setting("qol.player_display.ehp_hud", "EHP HUD", "Show EHP HUD.", SettingType.TOGGLE, "ehp"),
                setting("qol.player_display.speed_hud", "Speed HUD", "Show Speed HUD.", SettingType.TOGGLE, "speed"),
                section("qol.player_display.section_colors", "COLORS"),
                setting("qol.player_display.health_color", "Health Color", "Health HUD color.", SettingType.COLOR),
                setting("qol.player_display.mana_color", "Mana Color", "Mana HUD color.", SettingType.COLOR),
                setting("qol.player_display.overflow_mana_color", "Overflow Mana Color", "Overflow Mana color.", SettingType.COLOR),
                setting("qol.player_display.defense_color", "Defense Color", "Defense HUD color.", SettingType.COLOR),
                setting("qol.player_display.vitality_color", "Vitality Color", "Vitality HUD color.", SettingType.COLOR),
                setting("qol.player_display.ehp_color", "EHP Color", "EHP HUD color.", SettingType.COLOR),
                setting("qol.player_display.speed_color", "Speed Color", "Speed HUD color.", SettingType.COLOR),
                setting("qol.player_display.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND),
                setting("qol.player_display.open_hud_editor", "Open HUD Elements Editor", "Move each Player Display element.", SettingType.ACTION, "position")));

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
                "qol.auto_sprint",
                "Auto Sprint",
                "Keep sprinting while moving forward.",
                Group.UTILITIES,
                "Movement",
                false,
                true,
                true,
                List.of("sprint", "run", "ctrl"),
                setting("qol.auto_sprint.keybind", "Keybind", "Optional shortcut.", SettingType.KEYBIND)));

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
                "qol.fishing_helper",
                "Fishing Helper",
                "Auto-pull on !!! bites and optional recast. Serveri fishing.",
                Group.FISHING,
                "Catch",
                false,
                true,
                true,
                List.of("fishing", "rod", "auto pull", "recast"),
                setting("qol.fishing_helper.auto_pull", "Auto Pull", "Right-click when a !!! hologram appears near the hook.", SettingType.TOGGLE),
                setting("qol.fishing_helper.pull_delay", "Delay", "Ticks to wait after a bite before pulling.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.fishing_helper.pull_variance", "Delay Variance", "Random extra pull delay ticks.", SettingType.NUMBER, "0", "ticks"),
                setting("qol.fishing_helper.recast", "Auto Recast", "Cast again after a successful pull.", SettingType.TOGGLE),
                setting("qol.fishing_helper.recast_check", "Recast Check", "If the rod is idle, use it so a hook exists.", SettingType.TOGGLE),
                setting("qol.fishing_helper.recast_delay", "Recast Delay", "Ticks to wait before recasting.", SettingType.NUMBER, "1", "ticks"),
                setting("qol.fishing_helper.recast_variance", "Delay Variance", "Random extra recast delay ticks.", SettingType.NUMBER, "0", "ticks"),
                setting("qol.fishing_helper.bobber_timer", "Bobber Timer", "HUD seconds since the hook was cast.", SettingType.TOGGLE),
                setting("qol.fishing_helper.bite_title", "Bite Title", "Show a reel-now title on !!! without requiring auto-pull.", SettingType.TOGGLE),
                setting("qol.fishing_helper.bite_sound", "Bite Sound", "Play a local sound with the bite title.", SettingType.TOGGLE),
                setting("qol.fishing_helper.hook_timer_hud", "Hook Timer HUD", "Show the SkyBlock hook countdown on the fishing HUD.", SettingType.TOGGLE),
                setting("qol.fishing_helper.hide_hook_nametag", "Hide Hook Nametag", "Hide the world !!! / countdown hologram and keep the HUD.", SettingType.TOGGLE),
                setting("qol.fishing_helper.open_hud_editor", "Edit Fishing HUD", "Move the combined fishing HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.fishing_creatures",
                "Sea Creatures",
                "Barn tracker: spawn chat, cap 10, 340s timer, rare ESP/announce, optional auto-attack on the Serveri.",
                Group.FISHING,
                "Creatures",
                false,
                true,
                true,
                List.of("sea creature", "jawbus", "thunder", "yeti", "barn", "double hook"),
                setting("qol.fishing_creatures.hud", "Creature HUD", "Show live count, barn timer, and last catch.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.cap_notify", "Cap Notify", "Title when 10 sea creatures are alive.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.timer_notify", "Timer Notify", "Title when the oldest creature reaches the barn timer.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.timer_length", "Barn Timer", "Seconds before the barn timer warning. Default 340.", SettingType.NUMBER, "340", "s"),
                setting("qol.fishing_creatures.min_rarity", "Alert Rarity", "Minimum rarity for titles, ESP, and party ping.", SettingType.ENUM,
                        FishingCreaturesPolicy.RARITIES, "rarity"),
                setting("qol.fishing_creatures.rare_announce", "Rare Title", "Local title when a rare enough creature spawns.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.rare_sound", "Rare Sound", "Local sound with the rare title.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.rare_party", "Party Ping", "Send /pc with the spawn line. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.fishing_creatures.rare_esp", "Rare ESP", "Box tracked rare nametags.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.esp_color", "ESP Color", "Sea-creature box color.", SettingType.COLOR),
                setting("qol.fishing_creatures.shorten_chat", "Compact Chat", "Replace long spawn chat with a short SC line.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.hide_common", "Hide Common Tags", "Hide common/uncommon sea-creature nametags in the world.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.auto_attack", "Auto Attack", "Left-click while looking at a tracked sea creature. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.fishing_creatures.auto_delay", "Auto Attack Delay", "Ticks between auto-attacks.", SettingType.NUMBER, "4", "ticks"),
                setting("qol.fishing_creatures.thunder_sparks", "Thunder Sparks", "Box nearby Thunder skull sparks while a Thunder is live.", SettingType.TOGGLE),
                setting("qol.fishing_creatures.open_hud_editor", "Edit Fishing HUD", "Move the combined fishing HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.fishing_hotspots",
                "Fishing Hotspots",
                "Hotspot circles plus hotspot-radar flame guess.",
                Group.FISHING,
                "World",
                false,
                true,
                true,
                List.of("hotspot", "radar", "fishing speed"),
                setting("qol.fishing_hotspots.circle", "Hotspot Circles", "Outline hotspot nametag locations.", SettingType.TOGGLE),
                setting("qol.fishing_hotspots.hide_particles", "Hide Hotspot Particles", "Suppress flame/dust near a known hotspot.", SettingType.TOGGLE),
                setting("qol.fishing_hotspots.radar", "Radar Solver", "Guess hotspot direction from still flame particles while holding a radar.", SettingType.TOGGLE),
                setting("qol.fishing_hotspots.tracer", "Radar Tracer", "Draw the guessed radar line.", SettingType.TOGGLE),
                setting("qol.fishing_hotspots.despawn", "Despawn Warning", "Title when a nearby hotspot nametag disappears.", SettingType.TOGGLE),
                setting("qol.fishing_hotspots.color", "Hotspot Color", "Circle and tracer color.", SettingType.COLOR)));

        modules.add(module(
                "qol.fishing_trophy",
                "Trophy Fishing",
                "Trophy-fish chat titles, optional low-rarity hide, and Golden Fish lava timer.",
                Group.FISHING,
                "World",
                false,
                true,
                true,
                List.of("trophy", "odger", "golden fish", "crimson"),
                setting("qol.fishing_trophy.titles", "Catch Titles", "Local title on trophy catches.", SettingType.TOGGLE),
                setting("qol.fishing_trophy.filter_chat", "Hide Low Rarity Chat", "Hide trophy chat below the minimum rarity.", SettingType.TOGGLE),
                setting("qol.fishing_trophy.min_rarity", "Minimum Rarity", "Chat filter and title floor.", SettingType.ENUM,
                        FishingTrophyPolicy.RARITIES, "bronze"),
                setting("qol.fishing_trophy.golden_timer", "Golden Fish Timer", "Track Golden Fish spawn, 3 weaken hits, and despawn.", SettingType.TOGGLE),
                setting("qol.fishing_trophy.geyser", "Volcano Geyser", "Box the Blazing Volcano geyser from cloud particles.", SettingType.TOGGLE),
                setting("qol.fishing_trophy.sponge", "Sulphur Sponge", "Box wet sponge within 4 blocks for Sulphur Skitter.", SettingType.TOGGLE),
                setting("qol.fishing_trophy.fillet", "Fillet Tooltip", "Show default Magmafish fillet amounts on trophy-fish items.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.fishing_visuals",
                "Fishing Visuals",
                "Hide other players' bobbers and chum holograms while you fish.",
                Group.FISHING,
                "Visuals",
                false,
                true,
                true,
                List.of("bobber", "chum", "hide fishers"),
                setting("qol.fishing_visuals.hide_other_bobbers", "Hide Other Bobbers", "Do not render other players' fishing hooks.", SettingType.TOGGLE),
                setting("qol.fishing_visuals.chum_hider", "Hide Other Chum", "Hide Chum / Chumcap holograms that are not yours.", SettingType.TOGGLE),
                setting("qol.fishing_visuals.mute_banshee", "Mute Banshee", "Mute Bayou Banshee ghast-ambient pitches.", SettingType.TOGGLE),
                setting("qol.fishing_visuals.mute_drake", "Mute Reindrake", "Mute totem-use sounds used by Reindrake.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.fishing_tools",
                "Fishing Tools",
                "Bait remaining HUD, thunder-bottle charge alert, and Totem of Corruption timer.",
                Group.FISHING,
                "Visuals",
                false,
                true,
                true,
                List.of("bait", "thunder bottle", "totem of corruption"),
                setting("qol.fishing_tools.bait_hud", "Bait HUD", "Show bait remaining from the held bait lore.", SettingType.TOGGLE),
                setting("qol.fishing_tools.no_bait_warn", "No-Bait Warning", "Show Bait none when no remaining line is found.", SettingType.TOGGLE),
                setting("qol.fishing_tools.bait_change", "Bait Change", "Title when the held bait item name changes.", SettingType.TOGGLE),
                setting("qol.fishing_tools.thunder_notify", "Bottle Charge", "Title when a thunder/storm/hurricane bottle finishes charging.", SettingType.TOGGLE),
                setting("qol.fishing_tools.totem_hud", "Totem HUD", "Show nearby Totem of Corruption remaining time.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.foraging_trees",
                "Foraging Trees",
                "Galatea/Torrhus tree progress HUD, hide tree-break bits, TREE GIFT HUD, and unmineable chat filter.",
                Group.FORAGING,
                "Trees",
                false,
                true,
                true,
                List.of("galatea", "fig", "mangrove", "helix", "tree gift"),
                setting("qol.foraging_trees.progress_hud", "Progress HUD", "Show FIG/MANGROVE/HELIX nametag percent.", SettingType.TOGGLE),
                setting("qol.foraging_trees.only_axe", "Only Holding Axe", "Hide progress unless a foraging axe is held.", SettingType.TOGGLE),
                setting("qol.foraging_trees.hide_bits", "Hide Tree Bits", "Hide custom-tree block displays.", SettingType.TOGGLE),
                setting("qol.foraging_trees.gift_hud", "Gift HUD", "Show last TREE GIFT contribution. Not a session ledger.", SettingType.TOGGLE),
                setting("qol.foraging_trees.hide_unmineable", "Hide Unmineable Chat", "Hide regenerating/too-tough tree chat.", SettingType.TOGGLE),
                setting("qol.foraging_trees.fell_title", "Fell Title", "Local title on TIMBER/WOODPECKER/PETALFALL.", SettingType.TOGGLE),
                setting("qol.foraging_trees.open_hud_editor", "Edit Foraging HUD", "Move the combined foraging HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.foraging_audio",
                "Foraging Audio",
                "Mute Galatea phantoms, tree-break creaking, fusion fireworks, and optional stereo pants.",
                Group.FORAGING,
                "Audio",
                false,
                true,
                true,
                List.of("phantom", "fusion", "creaking"),
                setting("qol.foraging_audio.mute_phantom", "Mute Phantoms", "Mute phantom sounds on Galatea.", SettingType.TOGGLE),
                setting("qol.foraging_audio.mute_tree_break", "Mute Tree Break", "Mute entity.creaking.death on tree fell.", SettingType.TOGGLE),
                setting("qol.foraging_audio.mute_break_galatea", "Break On Galatea Too", "Also mute tree-break on Galatea/Torrhus.", SettingType.TOGGLE),
                setting("qol.foraging_audio.mute_fusion", "Mute Fusion", "Mute loud firework blasts at the fusion machine.", SettingType.TOGGLE),
                setting("qol.foraging_audio.mute_stereo", "Mute Stereo Pants", "Mute note block sounds near Stereo Pants. Off by default.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.foraging_helpers",
                "Foraging Helpers",
                "Sweep HUD, Forest Temple terracotta, Moonglade beacon click hints, and Galatea/Torrhus highlights.",
                Group.FORAGING,
                "Helpers",
                false,
                true,
                true,
                List.of("sweep", "beacon", "forest temple", "honeyhive"),
                setting("qol.foraging_helpers.sweep_hud", "Sweep HUD", "Show Sweep from tab plus look-cluster wood.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.temple_solver", "Temple Solver", "Forest Temple floor turns and Desert Temple color order.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.beacon_hints", "Beacon Hints", "Color/speed/pitch remaining clicks in Tune Frequency.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.highlights", "Resource Highlights", "Box lushlilac, sea lumies, veilshroom, honeyhives.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.moonglade_beacon", "Moonglade Beacon Box", "Box the Moonglade beacon while it is ready.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.park_tutorial", "Park Tutorial", "Park foraging tutorial helper highlights.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.hunting_esp", "Hunting ESP", "Box hunting-box / invisibug-style nametags when present.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.frog_mask", "Frog Mask HUD", "Show a HUD line while wearing a Frog Mask on Galatea.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.lasso_hud", "Lasso HUD", "Show a HUD line while holding a lasso.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.cinderbat", "Cinderbat Highlight", "Box Cinderbat nametags. Works with Hunting ESP or on its own.", SettingType.TOGGLE, "cinderbat"),
                setting("qol.foraging_helpers.huntaxe_lock", "Huntaxe Lock", "Block dropping or salvaging the held hunting axe. Sneak to bypass.", SettingType.TOGGLE, "huntaxe"),
                setting("qol.foraging_helpers.shard_tracker", "Shard Tracker", "Count hunting shards gained this session on the foraging HUD.", SettingType.TOGGLE, "shard"),
                setting("qol.foraging_helpers.lasso_alert", "Lasso Alert", "Local sound when a huntable nametag is nearby while holding a lasso.", SettingType.TOGGLE, "lasso"),
                setting("qol.foraging_helpers.sea_lumies_min", "Sea Lumies Min", "Minimum pickles to highlight.", SettingType.NUMBER, "3"),
                setting("qol.foraging_helpers.hotf_hint", "HOTF Screen Hint", "Title while Heart of the Forest is open.", SettingType.TOGGLE),
                setting("qol.foraging_helpers.open_hud_editor", "Edit Foraging HUD", "Move the combined foraging HUD.", SettingType.ACTION, "position")));

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
                "qol.item_tooltips",
                "Item Tooltips",
                "Missing enchants, extra NBT info, price quotes, and tooltip scroll/style in one module.",
                Group.HUD_DISPLAY,
                "Tooltips",
                false,
                true,
                true,
                List.of("tooltip", "enchant", "price", "bazaar", "info", "museum"),
                section("qol.item_tooltips.section_missing", "MISSING ENCHANTS"),
                setting("qol.item_tooltips.missing", "Show Missing Enchants", "List enchants the hovered item still needs.", SettingType.TOGGLE),
                setting("qol.missing_enchants.keybind", "Missing Enchants Keybind", "Hold to show. Ctrl+left-click also pins the check on that item. Blank always shows.", SettingType.KEYBIND, "left shift"),
                setting("qol.missing_enchants.show_upgradable", "Show Upgradable", "Show applied enchants that can be upgraded to a higher supported level.", SettingType.TOGGLE),
                setting("qol.missing_enchants.show_conflicting", "Show Conflicting", "Include mutually exclusive enchants such as Smite while Sharpness is applied.", SettingType.TOGGLE),
                section("qol.item_tooltips.section_info", "ITEM INFO"),
                setting("qol.item_tooltips.info", "Show Info Lines", "Dungeon quality, created date, dye hex, museum, item id.", SettingType.TOGGLE),
                setting("qol.info_tooltips.dungeon_quality", "Dungeon Quality", "Show dungeon stat-boost quality and floor tier.", SettingType.TOGGLE),
                setting("qol.info_tooltips.dungeon_quality_style", "Quality Style", "Use #cur #max #floor plus & or <red> color codes. Blank keeps the old Quality line.", SettingType.TEXT),
                setting("qol.info_tooltips.created_date", "Created Date", "Show the item creation timestamp.", SettingType.TOGGLE),
                setting("qol.info_tooltips.hex_color", "Hex Color", "Show leather dye hex.", SettingType.TOGGLE, "dye"),
                setting("qol.info_tooltips.museum", "Museum Donated", "Show whether ExtraAttributes marks the item as donated.", SettingType.TOGGLE),
                setting("qol.info_tooltips.item_id", "Item ID", "Show the SkyBlock item id.", SettingType.TOGGLE),
                setting("qol.info_tooltips.star_count", "Dungeon Stars", "Show dungeon star count from ExtraAttributes.", SettingType.TOGGLE, "star"),
                setting("qol.info_tooltips.pet_candy", "Hidden Pet Candy", "Re-add the pet candy line Hypixel hides at max level.", SettingType.TOGGLE, "candy"),
                setting("qol.info_tooltips.revert_master_stars", "Revert Master Stars", "Rewrite ✪✪✪✪✪➊-style hover names back to gold plus red stacked stars.", SettingType.TOGGLE, "master star"),
                section("qol.item_tooltips.section_prices", "PRICES"),
                setting("qol.item_tooltips.prices", "Show Price Lines", "Lowest BIN, Bazaar, NPC, motes, and price paid.", SettingType.TOGGLE),
                setting("qol.price_tooltips.lowest_bin", "Lowest BIN", "Show the latest lowest BIN from public auction data.", SettingType.TOGGLE, "auction"),
                setting("qol.price_tooltips.bazaar", "Bazaar", "Show Bazaar buy and sell quotes.", SettingType.TOGGLE),
                setting("qol.price_tooltips.npc", "NPC Sell", "Show NPC coin sell price.", SettingType.TOGGLE),
                setting("qol.price_tooltips.motes", "Motes Sell", "Show Rift motes sell price.", SettingType.TOGGLE),
                setting("qol.price_tooltips.burgers", "Burger Count", "McGrubber burger bonus for motes (0-5).", SettingType.NUMBER),
                setting("qol.price_tooltips.price_paid", "Price Paid", "Remember Confirm Purchase cost by item UUID.", SettingType.TOGGLE),
                section("qol.item_tooltips.section_style", "TOOLTIP STYLE"),
                setting("qol.item_tooltips.style", "Custom Tooltip Style", "Scroll oversized tooltips and restyle the box.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.infinite", "Infinite Scroll", "Keep panning after the box is pinned to the screen; wrap at the pan limit.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.horizontal", "Horizontal Scroll", "Allow Shift-scroll sideways.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.horizontal_key", "Horizontal Keybind", "Hold to scroll horizontally.", SettingType.KEYBIND, "left shift"),
                setting("qol.custom_tooltip.horizontal_speed", "Horizontal Scroll Speed", "Pixels per wheel step.", SettingType.NUMBER, "8"),
                setting("qol.custom_tooltip.vertical", "Vertical Scroll", "Allow vertical tooltip scrolling.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.vertical_speed", "Vertical Scroll Speed", "Pixels per wheel step.", SettingType.NUMBER, "8"),
                setting("qol.custom_tooltip.reset", "Reset On Hover", "Reset scroll when hovering a new item.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.style", "Tooltip Style", "Separated keeps the name on its own row.", SettingType.ENUM,
                        List.of("Separated", "Combined")),
                setting("qol.custom_tooltip.centered_header", "Centered Header", "Center the item name.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.border", "Border", "Draw a tooltip border.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.border_width", "Border Width", "Border thickness.", SettingType.NUMBER, "1"),
                setting("qol.custom_tooltip.rarity_border", "Use Rarity Color", "Border follows item rarity.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.border_color", "Border Color", "Fallback border color.", SettingType.COLOR),
                setting("qol.custom_tooltip.background", "Background", "Draw a tooltip background.", SettingType.TOGGLE),
                setting("qol.custom_tooltip.background_color", "Background Color", "Tooltip fill color.", SettingType.COLOR),
                setting("qol.custom_tooltip.only_name_key", "Only Name Toggle", "Hold to hide lore and show only the name.", SettingType.KEYBIND),
                setting("qol.custom_tooltip.shadows", "Text Shadows", "Minecraft text drop shadows.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.storage_overlay",
                "Storage Overlay",
                "Replace Storage menus with a compact overview of every server-observed Ender Chest and Backpack page while the scoreboard says SKYBLOCK. Last-seen items persist across restart. The first Storage open loads unlocked pages through /enderchest and /backpack so cards remember item order. Later opens only reload pages you clicked last. Dashboard Reload Storage Pages still walks every unlocked page. Live item effects stay frozen until that page is selected.",
                Group.INTERFACE,
                "Inventory",
                false,
                true,
                true,
                List.of("storage", "ender chest", "backpack", "overview"),
                setting("qol.storage_overlay.clear_cache", "Clear Observed Pages", "Clear only local storage-page previews and reload them from real server menus.", SettingType.ACTION, "cache", "refresh"),
                setting("qol.storage_overlay.reload_pages", "Reload Storage Pages", "Open every unlocked Ender Chest and Backpack from the server. Later overlay opens only reload pages you clicked; use this to refresh all of them.", SettingType.ACTION, "cache", "reload", "refresh"),
                setting("qol.storage_overlay.open_item_search", "Item Search", "Search the bundled SkyBlock item index without opening a second durable ledger.", SettingType.ACTION, "items", "bazaar"),
                setting("qol.storage_overlay.craft_helper", "Craft Helper", "Show recursive recipe totals, owned vs missing, and Storage-page counts on item tooltips.", SettingType.TOGGLE, "recipe", "craft"),
                setting("qol.storage_overlay.museum_armor", "Museum Armor Hints", "Hint missing Museum armor pieces from the bundled set list.", SettingType.TOGGLE, "museum"),
                setting("qol.storage_overlay.search_query", "Search Query", "Type an item name. Matching slots in Storage and Inventory get a moving edge light in your chosen color. The overlay also has a search box. Blank disables search.", SettingType.TEXT, "search"),
                setting("qol.storage_overlay.clear_search", "Clear Search", "Clear the local storage search query.", SettingType.ACTION, "search", "clear"),
                setting("qol.storage_overlay.always_open", "Always Open Overlay", "Replace supported Storage pages with the compact overview whenever possible.", SettingType.TOGGLE),
                setting("qol.storage_overlay.outline_active", "Outline Active Page", "Outline the currently open server page.", SettingType.TOGGLE),
                setting("qol.storage_overlay.outline_color", "Outline Color", "Active page outline color.", SettingType.COLOR),
                setting("qol.storage_overlay.inactive_tooltips", "Inactive Page Tooltips", "Allow tooltips for cached items on pages other than the active page.", SettingType.TOGGLE),
                setting("qol.storage_overlay.columns", "Columns", "Maximum page-card columns.", SettingType.NUMBER),
                setting("qol.storage_overlay.height", "Storage Height", "Scrollable storage panel height.", SettingType.NUMBER),
                setting("qol.storage_overlay.retain_scroll", "Retain Scroll Position", "Keep the panel position between Storage openings.", SettingType.TOGGLE),
                setting("qol.storage_overlay.scroll_speed", "Scroll Speed", "Mouse-wheel movement per step.", SettingType.NUMBER),
                setting("qol.storage_overlay.invert_scroll", "Invert Scroll", "Reverse mouse-wheel direction inside the overlay.", SettingType.TOGGLE),
                setting("qol.storage_overlay.padding", "Padding", "Space between page cards.", SettingType.NUMBER),
                setting("qol.storage_overlay.margin", "Margin", "Space around the storage panel content.", SettingType.NUMBER),
                setting("qol.storage_overlay.block_item_scroll", "Block Scrolling on Items", "Leave the overlay stationary while hovering an item.", SettingType.TOGGLE),
                setting("qol.storage_overlay.highlight_search", "Highlight Search Results", "Draw a clockwise light around matching cached and live slots using Search Highlight Color.", SettingType.TOGGLE, "search"),
                setting("qol.storage_overlay.filter_search", "Filter Search Results", "Show matching cached pages and keep the active page visible for safe item movement. Local cache only.", SettingType.TOGGLE),
                setting("qol.storage_overlay.highlight_color", "Search Highlight Color", "Search-result highlight color.", SettingType.COLOR),
                setting("qol.storage_overlay.panel_color", "Storage Panel", "Background of the compact Storage window.", SettingType.COLOR),
                setting("qol.storage_overlay.card_color", "Storage Cards", "Background of each Ender Chest / Backpack card.", SettingType.COLOR),
                setting("qol.storage_overlay.card_active_color", "Open Storage Card", "Background of the currently open page.", SettingType.COLOR),
                setting("qol.storage_overlay.player_color", "Storage Inventory", "Background of the player inventory under Storage.", SettingType.COLOR)));

        modules.add(module(
                "qol.reward_claim",
                "Daily Reward Claim",
                "Open Hypixel daily-reward choices inside Rot Client instead of a browser. Off by default. If the Hypixel page is not skippable, Claim waits the posted ad duration instead of opening the ad.",
                Group.INTERFACE,
                "Inventory",
                false,
                true,
                true,
                List.of("daily reward", "claim reward", "hypixel rewards", "daily rewards"),
                setting("qol.reward_claim.hide_chat_link", "Hide Chat Link", "Hide the Hypixel claim URL from chat after the in-client picker opens.", SettingType.TOGGLE),
                setting("qol.reward_claim.block_browser", "Keep In Client", "Block the vanilla website-confirm screen while a claim is loading.", SettingType.TOGGLE),
                setting("qol.reward_claim.wait_for_ad", "Wait For Ad", "When Hypixel marks the page as not skippable, wait the posted ad duration before Claim is enabled.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.inventory_buttons",
                "Inventory Buttons",
                "Shortcut icons around inventories while the scoreboard says SKYBLOCK. Shift-left-drag an icon in-game to move it. Use Open Editor for command, icon, size, and anchors.",
                Group.INTERFACE,
                "Inventory",
                false,
                true,
                true,
                List.of("inventory", "buttons", "commands", "warps"),
                section("qol.inventory_buttons.section_general", "GENERAL"),
                setting("qol.inventory_buttons.hover_tooltip", "Hover Tooltip", "Show the command while hovering a button.", SettingType.TOGGLE),
                setting("qol.inventory_buttons.inventory_only", "Only Player Inventory", "Show buttons only on the survival inventory, not chests or Storage.", SettingType.TOGGLE),
                section("qol.inventory_buttons.section_editor", "EDITOR"),
                setting("qol.inventory_buttons.open_editor", "Open Editor", "Edit command, icon, size, and anchors. Shift-drag still moves them in-game.", SettingType.ACTION),
                section("qol.inventory_buttons.section_presets", "PRESETS"),
                setting("qol.inventory_buttons.save_preset", "Save Local Preset", "Save the current button layout locally.", SettingType.ACTION, "preset", "save"),
                setting("qol.inventory_buttons.load_preset", "Load Local Preset", "Replace the current layout with the saved preset.", SettingType.ACTION, "preset", "load"),
                setting("qol.inventory_buttons.simple_preset", "Simple Preset", "Storage, Pets, and Wardrobe.", SettingType.ACTION),
                setting("qol.inventory_buttons.warps_preset", "All Warps Preset", "Common SkyBlock warp shortcuts.", SettingType.ACTION)));

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
                "qol.inventory_overlay",
                "Inventory Overlay",
                "Four stacked bars between the player and crafting show necklace, cloak, belt, and gloves while the scoreboard says SKYBLOCK. Click any bar to open Stats & Equipment. The pet sits to the right of the bottom bar; Ctrl+left-click and drag to move it. Last seen equipment and the chosen pet are stored locally, like Storage Overlay, so they still show after a restart. The wrench at the top-right of inventory opens background colors. Click outside that panel or its X to close it. Beside the pet, matching 18×18 wells show an S (hover for live AH/BZ bag+armor+equipment+pet total) and an R that opens the Rot dashboard.",
                Group.INTERFACE,
                "Inventory",
                false,
                true,
                true,
                List.of("equipment", "recipe book", "pet slot", "inventory", "potion", "effects", "color", "wrench", "dashboard", "value"),
                setting("qol.inventory_overlay.equipment", "Equipment Slots", "Draw four stacked equipment bars. Last seen pieces stay after a restart. Empty bars show a + and any click opens Stats & Equipment. The vanilla off-hand slot is hidden.", SettingType.TOGGLE, "equipment"),
                setting("qol.inventory_overlay.hide_recipe_book", "Hide Recipe Book", "Remove the recipe-book button from survival inventory.", SettingType.TOGGLE, "recipe", "book"),
                setting("qol.inventory_overlay.hide_status_effects", "Hide Inventory Effects", "Hide the potion-effect panel on the right of the inventory screen, including Hypixel lobby.", SettingType.TOGGLE, "potion", "effects"),
                setting("qol.inventory_overlay.pet_slot", "Pet Slot", "Show the equipped pet to the right of the bottom equipment bar. The last chosen pet stays after a restart. Click to open Pets. Ctrl+left-click and drag to reposition.", SettingType.TOGGLE, "pet"),
                setting("qol.inventory_overlay.open_colors", "Inventory Colors", "Open the same color picker as the in-game wrench: panel, header, main, hotbar, and border.", SettingType.ACTION, "wrench", "color"),
                setting("qol.inventory_overlay.chrome_panel", "Whole Inventory", "Tint over the inventory panel. The player model and 2×2 crafting stay vanilla. Alpha 0 leaves vanilla.", SettingType.COLOR),
                setting("qol.inventory_overlay.chrome_header", "Top / Armor", "Tint over the armor column and the strip beside the player. Crafting stays vanilla.", SettingType.COLOR),
                setting("qol.inventory_overlay.chrome_main", "Main Inventory", "Tint over the 3x9 inventory rows.", SettingType.COLOR),
                setting("qol.inventory_overlay.chrome_hotbar", "Hotbar", "Tint over the hotbar row.", SettingType.COLOR),
                setting("qol.inventory_overlay.chrome_border", "Border", "Outline around the inventory.", SettingType.COLOR),
                setting("qol.inventory_overlay.protect_drops", "Protect Drops", "Block dropping starred, Legendary+ items, and names in the extra list. Sneak to bypass.", SettingType.TOGGLE, "item protection"),
                setting("qol.inventory_overlay.protect_salvage", "Protect Salvage", "Block clicking protected items in salvage / Hex menus. Sneak to bypass.", SettingType.TOGGLE, "salvage"),
                setting("qol.inventory_overlay.protect_list", "Protect Extra Names", "One item name per line. Also always locks starred and Legendary+ when the toggles above are on.", SettingType.TEXT, "protect", "lock")));

        modules.add(module(
                "qol.skill_levels",
                "Skill Levels",
                "Overlay current skill levels on the Your Skills chest. Optional tight dark background behind the digits. Maxed skills use Max Level Color.",
                Group.HUD_DISPLAY,
                "Skills",
                false,
                true,
                true,
                List.of("skills", "level", "your skills"),
                setting("qol.skill_levels.background", "Number Background", "Draw a tight dark box behind the level digits so they stay readable.", SettingType.TOGGLE, "background", "box"),
                setting("qol.skill_levels.level_color", "Level Color", "In-progress skill level color.", SettingType.COLOR),
                setting("qol.skill_levels.max_color", "Max Level Color", "Color for MAX LEVEL skills.", SettingType.COLOR, "max")));

        modules.add(module(
                "qol.slot_binds",
                "Slot Binds",
                "Bind a hotbar slot to an inventory slot, draw a connector, and shift-click to swap. Only while the scoreboard says SKYBLOCK.",
                Group.INTERFACE,
                "Inventory",
                false,
                true,
                true,
                List.of("slot", "bind", "inventory line"),
                setting("qol.slot_binds.bind_set_key", "Bind Set Key", "Press on two slots (one hotbar) to bind, or on a bound slot to remove.", SettingType.KEYBIND),
                setting("qol.slot_binds.bind_color", "Bind Color", "Connector line color.", SettingType.COLOR),
                setting("qol.slot_binds.line_width", "Line Width", "Connector width.", SettingType.NUMBER, "0.5"),
                setting("qol.slot_binds.line_display", "Line Display", "When to show binding lines.", SettingType.ENUM,
                        List.of("Hover", "Hover + Shift", "None"), "hover"),
                setting("qol.slot_binds.profile", "Profile", "Independent binding profile.", SettingType.ENUM,
                        List.of("Profile 1", "Profile 2", "Profile 3"), "profile")));

        modules.add(module(
                "qol.waypoints",
                "Waypoints",
                "Temporary world markers from party/chat coordinates and an optional look-target ping.",
                Group.UTILITIES,
                "World",
                false,
                true,
                true,
                List.of("waypoint", "ping", "party chat"),
                setting("qol.waypoints.from_party", "From Party Chat", "Create a marker when party chat contains x/y/z.", SettingType.TOGGLE),
                setting("qol.waypoints.from_all", "From All Chat", "Create a marker from non-party chat coordinates.", SettingType.TOGGLE),
                setting("qol.waypoints.personal", "Personal Waypoint", "Also mark your own coordinate pings.", SettingType.TOGGLE),
                setting("qol.waypoints.ping_dropdown", "Ping Location", "Off, or look-target ping with the keybind.", SettingType.ENUM,
                        List.of("Off", "Look Target"), "ping"),
                setting("qol.waypoints.keybind", "Ping Keybind", "Place a look-target waypoint when Ping Location is Look Target.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.fullbright",
                "Fullbright and Night",
                "Client lighting pack. Fullbright raises the lightmap. Always Night plays a vanilla dusk once, then parks a midnight sky and dark terrain. Local render only; it does not change the server. Only one mode is on unless Force both on is latched.",
                Group.RENDER,
                "Lighting",
                false,
                true,
                true,
                List.of("brightness", "gamma", "night vision", "always night", "moon", "sky", "dusk"),
                setting("qol.fullbright.use_fullbright", "Fullbright",
                        "Instant full lightmap so caves and dungeons stay readable.", SettingType.TOGGLE, "gamma"),
                setting("qol.fullbright.always_night", "Always Night",
                        "Vanilla-style night sky and terrain. Dusk plays only when you turn this on in a world; hub and server switches keep the parked moon.", SettingType.TOGGLE, "night", "moon"),
                setting("qol.fullbright.force_both", "Force both on",
                        "Square latch. Off keeps Fullbright and Always Night exclusive. On (red) lets both run: night sky plus a fullbright lightmap.", SettingType.SQUARE, "both")));

        modules.add(module(
                "qol.performance_hud",
                "Performance HUD",
                "FPS / TPS / Ping overlay. The card switch is this module. Show FPS / TPS / Ping are the HUD text bits. Open HUD Elements Editor focuses this overlay and fades the others.",
                Group.HUD_DISPLAY,
                "Overlays",
                false,
                true,
                true,
                List.of("fps", "tps", "ping", "performance"),
                setting("qol.performance_hud.name_color", "Name Color", "Color of FPS / TPS / Ping labels. RGB picker.", SettingType.COLOR, "fps"),
                setting("qol.performance_hud.value_color", "Value Color", "Color of the numeric FPS / TPS / Ping values. RGB picker.", SettingType.COLOR),
                setting("qol.performance_hud.direction", "Direction", "Stack the HUD bits left-to-right or top-to-bottom.", SettingType.ENUM,
                        List.of("Horizontal", "Vertical"), "horizontal", "vertical"),
                setting("qol.performance_hud.show_fps", "Show FPS", "Show FPS readout.", SettingType.TOGGLE, "fps"),
                setting("qol.performance_hud.show_tps", "Show TPS", "Show TPS readout.", SettingType.TOGGLE, "tps"),
                setting("qol.performance_hud.show_ping", "Show Ping", "Show Ping readout.", SettingType.TOGGLE, "ping"),
                setting("qol.performance_hud.show_background", "Show Background", "Draw the dark panel behind FPS / TPS / Ping. Off leaves the numbers only.", SettingType.TOGGLE, "background"),
                setting("qol.performance_hud.open_hud_editor", "Open HUD Elements Editor", "Move the Performance HUD.", SettingType.ACTION, "position"),
                setting("qol.performance_hud.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.mining_tracker",
                "Mining Tracker",
                "Ore and gemstone tracker: enable the overlay, pick HUD lines, and open the tracker page for material selection.",
                Group.MINING,
                "Tracker",
                false,
                true,
                true,
                List.of("mining tracker", "tracker", "ore tracker", "gemstone tracker"),
                setting("qol.mining_tracker.show_blocks", "Blocks", "Broken-block counter on the Mining Tracker HUD.", SettingType.TOGGLE, "blocks"),
                setting("qol.mining_tracker.show_raw", "Raw Material", "Raw material quantity on the HUD.", SettingType.TOGGLE, "raw"),
                setting("qol.mining_tracker.show_enchanted", "Enchanted Material", "Enchanted material quantity on the HUD.", SettingType.TOGGLE, "enchanted"),
                setting("qol.mining_tracker.show_session_profit", "Session Profit", "Estimated session value on the HUD.", SettingType.TOGGLE, "profit"),
                setting("qol.mining_tracker.show_unsold", "Unsold Value", "Unsold inventory value on the HUD.", SettingType.TOGGLE, "unsold"),
                setting("qol.mining_tracker.show_cph", "Coins / Hour", "Coins per hour on the HUD.", SettingType.TOGGLE, "cph"),
                setting("qol.mining_tracker.show_mph", "Material / Hour", "Material per hour on the HUD.", SettingType.TOGGLE, "mph"),
                setting("qol.mining_tracker.show_session_time", "Session Time", "Active session timer on the HUD.", SettingType.TOGGLE, "time"),
                setting("qol.mining_tracker.show_tool", "Active Tool", "Held mining tool on the HUD.", SettingType.TOGGLE, "tool"),
                setting("qol.mining_tracker.show_area", "Area / Location", "Live SkyBlock parent and sub-area on the HUD.", SettingType.TOGGLE, "area", "location"),
                setting("qol.mining_tracker.show_graph", "Rate Graph", "Mining rate sparkline on the HUD.", SettingType.TOGGLE, "graph"),
                setting("qol.mining_tracker.show_fortune", "Drop + Fortune", "Drop and Mining Fortune readout.", SettingType.TOGGLE, "fortune"),
                setting("qol.mining_tracker.show_bazaar", "Bazaar + Tax", "Bazaar pricing and tax on the HUD.", SettingType.TOGGLE, "bazaar"),
                setting("qol.mining_tracker.show_value_panel", "Value Panel", "Profit / session-value card on the HUD.", SettingType.TOGGLE, "value"),
                setting("qol.mining_tracker.show_hud_title", "HUD Title", "Mining Tracker title chrome.", SettingType.TOGGLE, "title"),
                setting("qol.mining_tracker.show_hud_status", "Status Pill", "HUD status pill.", SettingType.TOGGLE, "status"),
                setting("qol.mining_tracker.show_hud_version", "Version", "Mod version on the Mining Tracker HUD.", SettingType.TOGGLE),
                setting("qol.mining_tracker.show_auto_pause", "Auto-Pause Line", "Target auto-pause status line.", SettingType.TOGGLE, "pause"),
                setting("qol.mining_tracker.hud_background", "HUD Background", "Panel behind Mining Tracker text.", SettingType.TOGGLE, "background"),
                setting("qol.mining_tracker.show_target_heading", "Target Heading", "TARGET heading above item rows.", SettingType.TOGGLE),
                setting("qol.mining_tracker.show_other_section", "Others Section", "OTHERS aggregate row.", SettingType.TOGGLE, "others"),
                setting("qol.mining_tracker.show_target_value", "Target Value", "Target mined value line.", SettingType.TOGGLE),
                setting("qol.mining_tracker.show_other_value", "Others Value", "OTHERS value line under the value panel.", SettingType.TOGGLE),
                setting("qol.mining_tracker.show_total_mined", "Total Mined Value", "Combined target + others value line.", SettingType.TOGGLE, "total"),
                setting("qol.mining_tracker.open_hud_editor", "Open HUD Elements Editor", "Move and scale the Mining Tracker HUD.", SettingType.ACTION, "position"),
                setting("qol.mining_tracker.open_page", "Open Tracker", "Open the tracker page for material selection.", SettingType.ACTION, "page")));

        modules.add(module(
                "qol.powder_chest",
                "Powder Chest Tracker",
                "Standalone powder chest counter and overlay. Independent of the ore target on Mining Tracker.",
                Group.MINING,
                "Tracker",
                false,
                true,
                true,
                List.of("powder", "powder chest", "mithril powder", "gemstone powder"),
                setting("qol.powder_chest.hud", "Powder HUD", "Show the Powder Chest overlay.", SettingType.TOGGLE, "hud"),
                setting("qol.powder_chest.hud_background", "HUD Background", "Panel behind Powder Chest text.", SettingType.TOGGLE, "background"),
                setting("qol.powder_chest.open_hud_editor", "Open HUD Elements Editor", "Move and scale the Powder Chest HUD.", SettingType.ACTION, "position"),
                setting("qol.powder_chest.open_page", "Open Powder Page", "Open the Powder Chest tracker page.", SettingType.ACTION, "page")));

        modules.add(module(
                "qol.mining_session",
                "Mining Session",
                "Live Current Session analytics. Open the page for the full readout; it does not fit in a settings drawer.",
                Group.MINING,
                "Session",
                false,
                false,
                true,
                List.of("session", "analytics", "current session"),
                setting("qol.mining_session.open_page", "Open Analytics", "Open live Current Session analytics.", SettingType.ACTION, "page")));

        modules.add(module(
                "qol.mining_history",
                "Mining History",
                "Saved local session history. Open the page for the table; it does not fit in a settings drawer.",
                Group.MINING,
                "Session",
                false,
                false,
                true,
                List.of("history", "saved sessions"),
                setting("qol.mining_history.open_page", "Open History", "Open saved local session history.", SettingType.ACTION, "page")));

        modules.add(module(
                "qol.commission_display",
                "Commission Display",
                "On-screen mining commissions from the tab list in Dwarven Mines, Crystal Hollows, Glacite, and other commission areas.",
                Group.MINING,
                "Commissions",
                false,
                true,
                true,
                List.of("commission", "commissions", "dwarven"),
                setting("qol.commission_display.title", "General Title", "Header text. Color tags like <red> are supported.", SettingType.TEXT),
                setting("qol.commission_display.none", "None Available Text", "Shown when no commissions are parsed.", SettingType.TEXT),
                setting("qol.commission_display.row", "Commission Text", "Row template. Variables: #name, #progress.", SettingType.TEXT),
                setting("qol.commission_display.colored_percent", "Colored Percent", "Color the progress percent by completion.", SettingType.TOGGLE),
                setting("qol.commission_display.open_hud_editor", "Open HUD Elements Editor", "Move the Commission Display.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.mining_scatha",
                "Scatha Alerts",
                "Crystal Hollows worm and Scatha titles, 620-tick spawn cooldown, and optional pet-drop ping.",
                Group.MINING,
                "Hollows",
                false,
                true,
                true,
                List.of("scatha", "worm", "crystal hollows"),
                setting("qol.mining_scatha.titles", "Spawn Titles", "Local Scatha / Worm titles from nearby nametags.", SettingType.TOGGLE),
                setting("qol.mining_scatha.sounds", "Sounds", "Local note sounds with spawn and cooldown titles.", SettingType.TOGGLE),
                setting("qol.mining_scatha.cooldown", "Cooldown Alert", "Title when the 620-tick approaching cooldown ends.", SettingType.TOGGLE),
                setting("qol.mining_scatha.pet_drop", "Pet Drop Title", "Title on PET DROP! Scatha chat.", SettingType.TOGGLE),
                setting("qol.mining_scatha.pet_rarity", "Pet Drop Rarity", "Append Legendary/Epic/Rare to the Scatha PET DROP chat line.", SettingType.TOGGLE),
                setting("qol.mining_scatha.hud", "Cooldown HUD", "Show remaining worm spawn cooldown on the mining HUD.", SettingType.TOGGLE),
                setting("qol.mining_scatha.party", "Party Ping", "Send /pc Scatha or Worm. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.mining_scatha.open_hud_editor", "Edit Mining HUD", "Move the combined mining leftover HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.mining_events",
                "Mining Events",
                "Dwarven/Hollows event HUD from bossbar and STARTED/ENDED chat.",
                Group.MINING,
                "Hollows",
                false,
                true,
                true,
                List.of("goblin raid", "2x powder", "raffle", "sky mall"),
                setting("qol.mining_events.hud", "Event HUD", "Show the current mining event on the mining HUD.", SettingType.TOGGLE),
                setting("qol.mining_events.titles", "Start/End Titles", "Local titles when an event starts or ends.", SettingType.TOGGLE),
                setting("qol.mining_events.goblin_esp", "Goblin Raid ESP", "Box Superprotectron / raid goblins while Goblin Raid is active.", SettingType.TOGGLE),
                setting("qol.mining_events.open_hud_editor", "Edit Mining HUD", "Move the combined mining leftover HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.mining_glacite",
                "Glacite Mineshaft",
                "Pity HUD, corpse tab/loot, cold overlay, and optional party share for the Glacite mineshaft.",
                Group.MINING,
                "Glacite",
                false,
                true,
                true,
                List.of("mineshaft", "pity", "corpse", "glacite", "cold"),
                setting("qol.mining_glacite.pity_hud", "Pity HUD", "Show Glacite Mineshafts pity from the tab list.", SettingType.TOGGLE),
                setting("qol.mining_glacite.corpse_hud", "Corpse HUD", "Show Lapis/Umber/Tungsten/Vanguard loot state.", SettingType.TOGGLE),
                setting("qol.mining_glacite.cold_overlay", "Cold Overlay", "Tint the screen from scoreboard Cold.", SettingType.TOGGLE),
                setting("qol.mining_glacite.party_share", "Share Corpse Coords", "Send parsed corpse x/y/z on /pc. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.mining_glacite.shaft_party", "Announce Shaft", "Party-chat when a mineshaft portal is found. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.mining_glacite.pity_chat", "Pity Chat", "Local chat with current Glacite Mineshaft pity when a portal is found.", SettingType.TOGGLE),
                setting("qol.mining_glacite.enter_title", "Enter Title", "Title when you enter a Glacite Mineshaft.", SettingType.TOGGLE),
                setting("qol.mining_glacite.enter_chat", "Enter Chat", "Local chat when you enter a Glacite Mineshaft.", SettingType.TOGGLE),
                setting("qol.mining_glacite.enter_party", "Enter Party Chat", "Also send the enter line on /pc. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.mining_glacite.corpse_waypoints", "Corpse Waypoints", "Box nearby Frozen Corpse armor stands in a mineshaft.", SettingType.TOGGLE),
                setting("qol.mining_glacite.key_announce", "Key Announce", "Count Umber/Tungsten/Skeleton keys in your inventory when you enter a shaft.", SettingType.TOGGLE),
                setting("qol.mining_glacite.open_hud_editor", "Edit Mining HUD", "Move the combined mining leftover HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.mining_helpers",
                "Mining Helpers",
                "Fetchur and Fossil Muncher answers, Dwarven metal-detector chest boxes, Fossil Excavator heatmap, wishing-compass guess, drill fuel, pickaxe ability HUD, commission GUI complete tint, and mining pings.",
                Group.MINING,
                "Helpers",
                false,
                true,
                true,
                List.of("fetchur", "drill", "pickobulus", "fossil muncher", "mismyla", "metal detector", "wishing compass", "excavator"),
                setting("qol.mining_helpers.fetchur", "Fetchur Solver", "Replace Fetchur riddles with the item answer.", SettingType.TOGGLE),
                setting("qol.mining_helpers.fossil_muncher", "Fossil Muncher", "Replace Fossil Muncher riddles with the fossil name.", SettingType.TOGGLE),
                setting("qol.mining_helpers.drill_fuel", "Drill Fuel HUD", "Show Fuel remaining from held drill lore.", SettingType.TOGGLE),
                setting("qol.mining_helpers.ability_hud", "Ability HUD", "Show Pickobulus / mining ability status from tab.", SettingType.TOGGLE),
                setting("qol.mining_helpers.commission_gui", "Completed Highlight", "Mark COMPLETED books in the Commissions GUI.", SettingType.TOGGLE),
                setting("qol.mining_helpers.commission_mobs", "Commission Mobs", "Box tab-matching commission mobs in Dwarven Mines and Hollows.", SettingType.TOGGLE),
                setting("qol.mining_helpers.notify_portal", "Mineshaft Portal", "Title when a Glacite Mineshaft portal is found.", SettingType.TOGGLE),
                setting("qol.mining_helpers.notify_scrap", "Suspicious Scrap", "Title when Suspicious Scrap drops.", SettingType.TOGGLE),
                setting("qol.mining_helpers.notify_goblin", "Goblin Spawns", "Title on Golden / Diamond Goblin spawn chat.", SettingType.TOGGLE),
                setting("qol.mining_helpers.metal_distance", "Metal Detector Distance", "Show TREASURE distance from the action bar on the mining HUD.", SettingType.TOGGLE),
                setting("qol.mining_helpers.detector_solver", "Metal Detector Chests", "Box likely Dwarven Mines metal-detector chests from Keeper stands plus TREASURE meters. Serveri helper, off by default.", SettingType.TOGGLE),
                setting("qol.mining_helpers.detector_ding", "Detector Ding", "Play a note when the metal-detector solver collapses to one chest.", SettingType.TOGGLE),
                setting("qol.mining_helpers.detector_title", "Detector Title", "Show Treasure Found! when the metal-detector solver collapses to one chest.", SettingType.TOGGLE),
                setting("qol.mining_helpers.red_carpets", "Red Carpets", "Render Dwarven Mines wool carpets as red so paths stand out.", SettingType.TOGGLE),
                setting("qol.mining_helpers.fossil_excavator", "Fossil Excavator Heatmap", "Tint likely Fossil Excavator tiles from the current glass-pane board. Off by default.", SettingType.TOGGLE),
                setting("qol.mining_helpers.wishing_compass", "Wishing Compass Guess", "Record two compass uses and box their intersection in the Crystal Hollows. Off by default.", SettingType.TOGGLE),
                setting("qol.mining_helpers.call_king", "Call King", "Send /call mismyla after commission complete. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.mining_helpers.break_reset", "Break Reset Fix", "Ignore same-block mining updates that reset break progress.", SettingType.TOGGLE, "mining"),
                setting("qol.mining_helpers.gemstone_desync", "Gemstone Desync Fix", "Ignore gemstone glass flicker while you are mining that block.", SettingType.TOGGLE, "gemstone"),
                setting("qol.mining_helpers.open_hud_editor", "Edit Mining HUD", "Move the combined mining leftover HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.mining_hotm",
                "Heart of the Mountain",
                "HOTM screen detection plus the Sky Mall perk line from the tab list on the combined mining HUD.",
                Group.MINING,
                "Helpers",
                false,
                true,
                true,
                List.of("hotm", "heart of the mountain", "sky mall", "powder"),
                setting("qol.mining_hotm.sky_mall", "Sky Mall HUD", "Show the Sky Mall perk line on the mining HUD.", SettingType.TOGGLE),
                setting("qol.mining_hotm.screen_hint", "HOTM Screen Hint", "Local hint while Heart of the Mountain is open.", SettingType.TOGGLE),
                setting("qol.mining_hotm.open_hud_editor", "Edit Mining HUD", "Move the combined mining leftover HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.pet_hud",
                "Pet HUD",
                "On-screen readout of the currently equipped pet. Reads Pets GUI lore and the tab list.",
                Group.HUD_DISPLAY,
                "Overlays",
                false,
                true,
                true,
                List.of("pet", "pets", "hud", "dragon"),
                setting("qol.pet_hud.show_background", "Show Background", "Draw the dark panel behind Pet HUD text. Off leaves icon and text only.", SettingType.TOGGLE, "background"),
                setting("qol.pet_hud.open_hud_editor", "Open HUD Elements Editor", "Move the Pet HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.name_hider",
                "Hide Own Name",
                "Local-only: hide your Minecraft username on your screen. Other players and the server still see the real name.",
                Group.HUD_DISPLAY,
                "Overlays",
                false,
                true,
                true,
                List.of("name", "username", "nick", "hide name", "scramble"),
                setting("qol.name_hider.mode", "Mode", "Scramble replaces your name with cryptic glyphs. Custom uses the alias you type.", SettingType.ENUM,
                        List.of("Scramble", "Custom"), "scramble", "custom"),
                setting("qol.name_hider.custom_name", "Custom Name", "Click, type an alias, Enter to save. Right-click to clear. Used when Mode is Custom.", SettingType.TEXT, "alias", "nick")));

        modules.add(module(
                "qol.render_optimizer",
                "Render Optimizer",
                "Skip drawing noisy local entities and overlays you do not need. Render-only; other players still see the world normally.",
                Group.RENDER,
                "Optimizer",
                false,
                true,
                true,
                List.of("render", "optimizer", "particles"),
                setting("qol.render_optimizer.hide_falling_blocks", "Hide Falling Blocks", "Suppress falling-block render.", SettingType.TOGGLE, "falling"),
                setting("qol.render_optimizer.hide_lightning", "Hide Lightning", "Suppress lightning render.", SettingType.TOGGLE, "lightning"),
                setting("qol.render_optimizer.hide_xp_orbs", "Hide Experience Orbs", "Suppress XP orb render.", SettingType.TOGGLE, "experience", "xp"),
                setting("qol.render_optimizer.hide_death_animation", "Hide Death Animation", "Suppress death animation.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_dead_entities", "Hide Dead Entities", "Hide entity models after they have died.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_dead_poof", "Hide Dead Poof", "Hide the white poof when a mob dies.", SettingType.TOGGLE, "poof"),
                setting("qol.render_optimizer.hide_armor_stands", "Hide Armor Stands", "Carefully scoped armor-stand hide.", SettingType.TOGGLE, "armor stand"),
                setting("qol.render_optimizer.hide_explosion_particles", "Hide Explosion Particles", "Suppress explosion particles.", SettingType.TOGGLE, "explosion"),
                setting("qol.render_optimizer.hide_implosion_particles", "Hide Implosion Particles", "Hide nearby wither-blade implosion bursts.", SettingType.TOGGLE, "implosion", "hyperion"),
                setting("qol.render_optimizer.hide_break_particles", "Hide Break Particles", "Hide block-break particles.", SettingType.TOGGLE, "break"),
                setting("qol.render_optimizer.hide_empty_tooltips", "Hide Empty Tooltips", "Hide tooltips whose name is blank.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_boss_bar", "Hide Boss Bar", "Hide boss health bars.", SettingType.TOGGLE, "boss"),
                setting("qol.render_optimizer.hide_armor_bar", "Hide Armor Bar", "Hide the vanilla armor icons.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_food_bar", "Hide Food Bar", "Hide the vanilla hunger icons.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_fog", "Hide Fog", "Push environmental fog out of view.", SettingType.TOGGLE, "fog"),
                setting("qol.render_optimizer.hide_effect_display", "Hide Effect Display", "Hide status-effect icons.", SettingType.TOGGLE, "effects"),
                setting("qol.render_optimizer.hide_recipe_book", "Hide Recipe Book", "Hide the inventory recipe-book button.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_selected_item_name", "Hide Selected Item Name", "Hide the hotbar item name popup.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_archer_passive", "Hide Archer Passive", "Suppress archer passive FX.", SettingType.TOGGLE, "archer"),
                setting("qol.render_optimizer.hide_healer_fairy", "Hide Healer Fairy", "Suppress healer fairy FX.", SettingType.TOGGLE, "fairy", "healer"),
                setting("qol.render_optimizer.hide_soul_weaver", "Hide Soul Weaver", "Suppress soul weaver FX.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_tentacle_head", "Hide Tentacle Head", "Suppress tentacle head FX.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_fire_overlay", "Hide Fire Overlay", "Suppress first-person fire overlay.", SettingType.TOGGLE, "fire"),
                setting("qol.render_optimizer.hide_entity_fire", "Hide Entity Fire", "Hide fire on burning mobs.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_mage_beam", "Hide Mage Beam", "Hide dungeon mage beam fireworks.", SettingType.TOGGLE, "mage"),
                setting("qol.render_optimizer.hide_ice_spray", "Hide Ice Spray", "Hide Ice Spray Wand poof bursts.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_powder_coating", "Hide Powder Coating", "Hide Crystal Hollows powder coating dust.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_guided_sheep", "Hide Guided Sheep", "Hide dungeon Mage Guided Sheep.", SettingType.TOGGLE, "sheep"),
                setting("qol.render_optimizer.hide_bone_plating", "Hide Bone Plating", "Hide dungeon Bone Plating drops.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_tree_bits", "Hide Tree Bits", "Hide Foraging tree-break block displays.", SettingType.TOGGLE),
                setting("qol.render_optimizer.hide_nausea", "Hide Nausea", "Hide nausea distortion and overlay.", SettingType.TOGGLE),
                setting("qol.render_optimizer.vignette", "Vignette", "Which vignette overlay to hide.", SettingType.ENUM,
                        List.of("None", "Ambient", "Danger", "Both"), "vignette"),
                setting("qol.render_optimizer.hide_stuck_arrows", "Hide Stuck Arrows", "Hide arrows stuck in entities.", SettingType.TOGGLE, "arrow"),
                setting("qol.render_optimizer.hide_island_clouds", "Hide Island Clouds", "Hide clouds in Dwarven Mines, all mining islands, or always.", SettingType.ENUM,
                        List.of("Off", "Dwarven", "Mining", "Always"), "clouds"),
                setting("qol.render_optimizer.nether_fog", "Nether Fog Darkening", "Keep Crimson Isle fog dark while Night Vision is active.", SettingType.TOGGLE, "crimson"),
                setting("qol.render_optimizer.nether_fog_scale", "Nether Fog Scale", "How strong Crimson Isle fog stays with Night Vision. Lower is thicker.", SettingType.NUMBER),
                setting("qol.render_optimizer.totem_animation", "Totem Animation", "Play the totem burst when Bonzo/Spirit/Phoenix/Eye absorb chat fires.", SettingType.TOGGLE, "bonzo", "phoenix"),
                setting("qol.render_optimizer.mob_icons", "Mob Icon Labels", "Replace Hypixel nametag icon glyphs with readable labels such as [Undead].", SettingType.TOGGLE, "bestiary"),
                setting("qol.render_optimizer.armor_self", "Armor Self %", "Local worn-armor visibility for you. 0 hides armor. 1-99 still show it in this build.", SettingType.NUMBER, "transparent"),
                setting("qol.render_optimizer.armor_others", "Armor Others %", "Local worn-armor visibility for other players. 0 hides armor.", SettingType.NUMBER, "transparent"),
                setting("qol.render_optimizer.full_text_shadow", "Full Text Shadow", "Draw an eight-direction glyph shadow on HUD and GUI text.", SettingType.TOGGLE, "shadow", "font"),
                setting("qol.render_optimizer.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.hide_players",
                "Hide Players",
                "Hide other players by dungeon or distance rules. This only changes what you see; hitboxes and the server are unchanged.",
                Group.COMBAT,
                "ESP",
                false,
                true,
                true,
                List.of("hide players", "players", "dungeon"),
                setting("qol.hide_players.only_dungeons", "Only in Dungeons", "Apply only inside dungeons.", SettingType.TOGGLE, "dungeon"),
                setting("qol.hide_players.hide_all", "Hide All", "Hide all eligible remote players.", SettingType.TOGGLE),
                setting("qol.hide_players.distance", "Distance", "Distance threshold when Hide All is off.", SettingType.NUMBER),
                setting("qol.hide_players.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.player_size",
                "Player Size",
                "Scale how large other players look on your screen. Visual only: hitboxes, reach, and physics stay vanilla.",
                Group.RENDER,
                "Players",
                false,
                true,
                true,
                List.of("size", "scale"),
                setting("qol.player_size.x", "Size X", "Visual X scale.", SettingType.NUMBER),
                setting("qol.player_size.y", "Size Y", "Visual Y scale.", SettingType.NUMBER),
                setting("qol.player_size.z", "Size Z", "Visual Z scale.", SettingType.NUMBER),
                setting("qol.player_size.player_animals", "Player Animals", "Replace other players with client-only animal models. Scope and species stay local.", SettingType.TOGGLE, "pets", "animals"),
                setting("qol.player_size.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.trajectories",
                "Trajectories",
                "Predicted bow and ender-pearl flight path for the Serveri.",
                Group.COMBAT,
                "Aim",
                false,
                true,
                true,
                List.of("trajectory", "bow", "pearl", "arrow"),
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
                "qol.dungeon_hud",
                "Dungeon HUD",
                "Secrets, score, class, invincibility timers and a named dungeon map.",
                Group.DUNGEONS,
                "HUD",
                false,
                true,
                true,
                List.of("dungeon hud", "secrets", "score", "bonzo", "spirit mask", "terracotta"),
                section("qol.dungeon_hud.section_run", "Run HUD"),
                setting("qol.dungeon_hud.floor", "Floor", "Show F1-F7 or M1-M7.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.class", "Class", "Show your dungeon class.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.secrets", "Secrets", "Show secrets found / total.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.score", "Score", "Show the dungeon score line.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.cleared", "Cleared", "Show room-clear percent.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.invincibility", "Invincibility", "Bonzo, Spirit Mask and Phoenix proc and cooldown timers.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.mask_overlay", "Mask Cooldown Overlay", "Fill Bonzo and Spirit Mask inventory/hotbar slots while the item cooldown is running.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.mask_overlay_color", "Mask Overlay Color", "Cooldown fill color on Bonzo/Spirit Mask slots.", SettingType.COLOR),
                setting("qol.dungeon_hud.terracotta", "Terracotta Timer", "Sadan terracotta countdown.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.blessings", "Blessings", "Show Blessing of Power/Time/Life from chat.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.ragnarock", "Ragnarock", "Show cancelled axe and strength gain.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.melody", "Melody", "Show Melody column progress while in the terminal.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.melody_other", "Teammate Melody", "HUD when a teammate posts party % during Goldor terminals. Mage has melody! 2/4.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.quiz", "Quiz / Weirdos", "Show Oruo answers and Three Weirdos truth.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.score_overlay", "Score Overlay", "Large Score: N text beside the dungeon HUD.", SettingType.TOGGLE),
                section("qol.dungeon_hud.section_map", "Dungeon Map"),
                setting("qol.dungeon_hud.map", "Dungeon Map", "Live Magical Map paper: 16px rooms, 4px doors, names, secrets and player heads.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_mode", "Map Mode", "Explored follows the Magical Map. Reveal Hidden hashes loaded rooms and paints them behind wither and blood doors.", SettingType.ENUM, DungeonMapPolicy.MAP_MODES, "cheat"),
                setting("qol.dungeon_hud.map_doors", "Map Doors", "Draw wither, blood, fairy, entrance and opened-wither doors.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_players", "Map Players", "Draw player heads or class markers on the map.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.room_names", "Room Names", "Label hashed rooms (Waterfall, Fairy, Blood).", SettingType.TOGGLE),
                setting("qol.dungeon_hud.room_secrets", "Room Secrets", "Show found/total secrets on each named room.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.player_names", "Player Names", "Draw teammate names next to map markers.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.head_markers", "Head Markers", "Blit 8x8 player-skin faces on the dungeon map when 26.2 exposes the skin atlas.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.class_icons", "Class Icons", "Color map markers by dungeon class.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_extra", "Map Status Bar", "Secrets, crypts, score and deaths under the map.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.crypts", "Crypts", "Show crypt count on the map status bar.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.deaths", "Deaths", "Show death count on the map status bar.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_mimic", "Mimic Chip", "Show M: on the map status bar when Mimic is killed.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_puzzles", "Puzzle Chip", "Show P: on the map status bar when puzzles are green.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_hide_boss", "Hide Map In Boss", "Hide the dungeon map while the sidebar looks like a boss fight.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.map_scale", "Map Size", "Room tile size in pixels (12-40).", SettingType.NUMBER),
                section("qol.dungeon_hud.section_timers", "Timers"),
                setting("qol.dungeon_hud.f7_timers", "F7 Timers", "Storm, Goldor and Necron phase timers.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.puzzle_timer", "Puzzle Timer", "Count seconds while a dungeon puzzle is active.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.warp_cooldown", "Warp Cooldown", "30s countdown after someone enters a Catacombs floor.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.secret_spawn", "Secret Spawn", "1s tick after tab Time Elapsed updates, while secrets can spawn.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.explosive_shot", "Explosive Shot", "Show Archer Explosive Shot damage per enemy.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.unclaimed_chests", "Unclaimed Chests", "Show Dungeon Hub tab Unclaimed chests.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.chest_warning", "Chest Count Warning", "Local title when Hub unclaimed plus runs this session reach the chest warning count.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.chest_warning_count", "Chest Warning Count", "Warn at this many Hub unclaimed chests plus finished runs. Default 55.", SettingType.NUMBER),
                setting("qol.dungeon_hud.extra_stats", "Extra Stats", "Reprint a compact Extra Stats summary in the dungeon HUD and local chat.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.ledge", "Distance To Ledge", "Storm P2 distance to the yellow-pad ledge. Archer/Tank on F7, Archer/Mage on M7.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.ledge_yellow", "Ledge At Yellow Only", "Only show the ledge distance while standing on Storm's yellow pad.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.ledge_all", "Ledge All Classes", "Show ledge distance for every dungeon class.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.run_timers", "Run Timers", "Blood Rush, Blood Open and Boss Enter times from chat.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.show_split_pbs", "Show Split PBs", "Append local personal-best times next to dungeon splits.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.kuudra_splits", "Kuudra Splits", "Supply/Build/Eaten/Stun/DPS/Kill timers on this HUD. Off by default.", SettingType.TOGGLE),
                setting("qol.dungeon_hud.open_hud_editor", "Edit Dungeon HUD", "Move the Dungeon HUD and Magical Map.", SettingType.ACTION, "position"),
                setting("qol.dungeon_hud.reset_split_pbs", "Reset Split PBs", "Clear stored Blood Rush / Blood Open / Boss Enter personal bests.", SettingType.ACTION),
                setting("qol.dungeon_hud.reset_kuudra_pbs", "Reset Kuudra PBs", "Clear stored Kuudra split personal bests.", SettingType.ACTION),
                setting("qol.dungeon_hud.cheater_names", "Hidden Names", "Label hidden rooms when Map Mode is Reveal Hidden.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_hud.cheater_darken", "Darken Hidden", "Darken unopened rooms when Map Mode is Reveal Hidden.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_hud.cheater_darken_factor", "Darken Factor", "0-1 multiplier for hidden tiles.", SettingType.NUMBER)));

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
                setting("qol.dungeon_esp.secret_waypoints", "Secret Waypoints", "Box every shipped secret in the hashed room you are standing in, through walls. Chest/item/bat/essence/lever labels stay on top of blocks.", SettingType.TOGGLE),
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
                setting("qol.dungeon_esp.hate_doors", "I Hate Doors", "Rewrite wither/blood/entrance door blocks to stained glass on the client.", SettingType.TOGGLE),
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
                setting("qol.dungeon_esp.tracers", "Tracers", "Draw lines to dungeon ESP targets.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.depth", "Depth Check", "Hide boxes behind solid blocks.", SettingType.TOGGLE),
                setting("qol.dungeon_esp.ghost_block", "Ghost Block", "Client-side air the looked-at block. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.ghost_uayor", "Ghost Confirm", "Required Use-at-your-own-risk gate before ghosting.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.ghost_stonk", "Stonk Ghost", "Ghost the looked-at block when right-clicking a pickaxe.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.ghost_keybind", "Ghost Key", "Hold to ghost the looked-at block.", SettingType.KEYBIND),
                setting("qol.dungeon_esp.triggerbot", "TriggerBot", "Auto-click F7 crystals and dungeon secrets while looking at them. Not a PvP ragebot.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.trigger_crystal", "Crystal", "Take/place Energy Crystals.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.trigger_take", "Take Crystal", "Right-click crystals to pick them up.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.trigger_place", "Place Crystal", "Right-click to place a held crystal.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.trigger_secret", "Secret Click", "Click chests, skulls, levers and buttons.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_esp.trigger_delay", "Trigger Delay", "Milliseconds between TriggerBot clicks.", SettingType.NUMBER)));

        modules.add(module(
                "qol.dungeon_announce",
                "Dungeon Announce",
                "Party-chat Mimic / Prince / Bat / death lines, Melody party send, and a Blood Camp ready title.",
                Group.DUNGEONS,
                "Alerts",
                false,
                true,
                true,
                List.of("mimic", "prince", "bat", "blood camp", "watcher", "melody", "death"),
                setting("qol.dungeon_announce.mimic", "Mimic", "Send Mimic Killed! in party chat.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.prince", "Prince", "Send Prince Killed! in party chat.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.bat", "Bat", "Send Bat Killed! in party chat.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.blood", "Blood Camp", "Local title when The Watcher is done spawning.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.score_title", "270 Score Title", "Local title when dungeon score crosses the threshold.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.score_threshold", "Score Threshold", "Score that fires the title. Default is 270.", SettingType.NUMBER),
                setting("qol.dungeon_announce.f7", "F7 Titles", "Local titles for crystals, enrage and terminals.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.ragnarock", "Ragnarock", "Local title when the axe is cancelled.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.rooms", "Room Alerts", "Local title on puzzle fail.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.melody", "Melody Alert", "Local title while a Melody terminal is open.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.melody_party", "Melody Party", "Send the Melody message in party chat when the terminal opens.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.melody_message", "Melody Message", "Party text when Melody opens.", SettingType.TEXT),
                setting("qol.dungeon_announce.melody_progress", "Melody Progress", "Send Melody 25/50/75% in party chat as lime terracotta fills.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.death", "Death Party", "Send a party-chat line when someone becomes a ghost.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.death_message", "Death Message", "Use {player} for the ghosted name.", SettingType.TEXT),
                setting("qol.dungeon_announce.position", "Position Callouts", "Send At SS / EE2 / EE3 / Core in party chat when you enter those F7 boxes. Off by default.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.secret_chime", "Secret Chime", "Play a local note when the sidebar secret count increases.", SettingType.TOGGLE, "secret"),
                setting("qol.dungeon_announce.duplicate_class", "Duplicate Class Alert", "Local title when two party members share a dungeon class.", SettingType.TOGGLE, "dupe"),
                setting("qol.dungeon_announce.player_count", "Not Enough Players", "Local title on Starting in when the sidebar has fewer than five [A/B/H/M/T] class tags.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.location", "Location Notification", "Local HUD when party chat is At SS / EE2 / Inside Goldor Tunnel. Ignores your own callouts.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.key_drop", "Key Drop Alert", "Local HUD and orb sound when a Wither or Blood Key armor stand drops. Archer and Mage by default.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.key_drop_all", "Key Drop All Classes", "Show the key-drop HUD for every dungeon class.", SettingType.TOGGLE),
                setting("qol.dungeon_announce.auto_ult", "Auto Ultimate", "Send vanilla Q-drop (Hypixel class ultimate) on Maxor enrage, Goldor, Sadan giants and Livid start.", SettingType.TOGGLE, "cheat")));

        modules.add(module(
                "qol.dungeon_leap",
                "Leap Menu",
                "Highlight Spirit Leap slots by dungeon class, Archer first.",
                Group.DUNGEONS,
                "Menus",
                false,
                true,
                true,
                List.of("spirit leap", "ghost leap", "leap"),
                setting("qol.dungeon_leap.highlight", "Highlight Classes", "Tint leap heads by class order.", SettingType.TOGGLE),
                setting("qol.dungeon_leap.custom_gui", "Custom Leap GUI", "2x2 class overlay instead of only slot tints.", SettingType.TOGGLE),
                setting("qol.dungeon_leap.announce", "Announce Leap", "Local chat when you leap, using the message below.", SettingType.TOGGLE),
                setting("qol.dungeon_leap.counter", "Leap Counter", "Show how many teammates leaped into your F7 box.", SettingType.TOGGLE),
                setting("qol.dungeon_leap.keys", "Number Keys", "Press 1-4 in Spirit Leap to leap to class-sorted heads.", SettingType.TOGGLE),
                setting("qol.dungeon_leap.message", "Leap Message", "Use {name} for the leaped player.", SettingType.TEXT)));

        modules.add(module(
                "qol.dungeon_terminals",
                "Terminal Solver",
                "Highlight F7 terminal clicks. Optional auto-click is Serveri cheat.",
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
                setting("qol.dungeon_terminals.sounds", "Click Sounds", "Play a local note when Auto Terms or a solver click lands.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.click_sound", "Click Sound", "Minecraft sound id, for example block.note_block.pling.", SettingType.TEXT),
                setting("qol.dungeon_terminals.click_pitch", "Click Pitch", "Local click pitch.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.click_volume", "Click Volume", "Local click volume.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.complete_sounds", "Complete Sounds", "Play a local note when the remaining terminal solution is empty.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.stop_tooltips", "Stop Tooltips", "Hide item tooltips while a Floor 7 terminal solver is open.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.hide_clicked", "Hide Clicked", "Hide chest slots that are not part of the remaining solution.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.block_wrong_slots", "Block Wrong Slots", "Cancel chest clicks that are not the next solved slot. Sneak to override. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.human_order", "Human Click Order", "Prefer nearby terminal slots instead of list order.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.melody_keys", "Melody Keys", "Press 1-4 to click the live Melody rows (a 3-row chest still uses 1-3).", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.melody_key_1", "Melody Key 1", "Key for Melody row 1.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.melody_key_2", "Melody Key 2", "Key for Melody row 2.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.melody_key_3", "Melody Key 3", "Key for Melody row 3.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.melody_key_4", "Melody Key 4", "Key for Melody row 4.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.drop_key", "Drop Key Click", "Treat the drop key as a click on the hovered solver slot.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.keybind_left", "Left Click Bind", "Press to left-click the hovered solver slot. Blank means unbound.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.keybind_right", "Right Click Bind", "Press to right-click the hovered solver slot. Blank means unbound.", SettingType.KEYBIND),
                setting("qol.dungeon_terminals.protect", "Terminal Protection", "Block GUI close for a short time after a terminal opens.", SettingType.TOGGLE),
                setting("qol.dungeon_terminals.protect_ms", "Protect Time", "Milliseconds to keep the terminal open after it appears.", SettingType.NUMBER),
                setting("qol.dungeon_terminals.auto_numbers", "Auto Numbers", "Auto-click Click in order.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.auto_colors", "Auto Colors", "Auto-click Select all the items.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.auto_rubix", "Auto Rubix", "Auto-click Change all to same color.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.auto_panes", "Auto Panes", "Auto-click Correct all the panes.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.auto_starts", "Auto Starts With", "Auto-click What starts with.", SettingType.TOGGLE, "cheat"),
                section("qol.dungeon_terminals.section_queue", "QUEUE TERMS"),
                setting("qol.dungeon_terminals.queue", "Queue Terms", "Experimental click queue so terminal clicks register one-by-one. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_terminals.resync_timeout", "Resync Timeout", "Retry predicted or queued clicks if the terminal does not update within this many milliseconds.", SettingType.NUMBER),
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
                setting("qol.dungeon_terminals.depth_test", "Depth Test", "Hide waypoint boxes behind blocks.", SettingType.TOGGLE),
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

        modules.add(module(
                "qol.dungeon_termsim",
                "Terminal Simulator",
                "Local F7 terminal practice with Hypixel window titles. Auto Terms solves the sim and real chests.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("termsim", "terminal sim", "practice", "cheat"),
                setting("qol.dungeon_termsim.open", "Open Hub", "Open the Terminal Simulator menu.", SettingType.ACTION),
                setting("qol.dungeon_termsim.keybind", "Keybind", "Open the hub while in-game.", SettingType.KEYBIND),
                setting("qol.dungeon_termsim.ping", "Ping", "Simulated round-trip delay in milliseconds.", SettingType.NUMBER),
                setting("qol.dungeon_termsim.show_pbs", "Show PBs", "Show local personal-best times on hub dyes.", SettingType.TOGGLE),
                setting("qol.dungeon_termsim.ip", "Remote IP", "Stored only. Rot Client keeps the local /rot termsim hub and does not connect to a remote simulator.", SettingType.TEXT)));

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
                "qol.dungeon_f7",
                "F7 Boss",
                "Floor 7 helpers: crystals, terminals, Simon, I Hate Diorite, I4, relics, Wither ESP and tick timers for the Serveri.",
                Group.DUNGEONS,
                "F7",
                false,
                true,
                true,
                List.of("goldor", "storm", "necron", "simon says", "crystal", "melody", "diorite"),
                setting("qol.dungeon_f7.titles", "F7 Titles", "Crystal, enrage and terminal progress titles.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.title_crystal", "Crystal Titles", "Energy Crystal progress titles.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.title_wither", "Wither Titles", "Enrage titles.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.title_terminal", "Terminal Titles", "Terminal/device progress titles.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.title_gate", "Gate Titles", "Lever/gate progress titles.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_other_titles", "Hide Other Term Titles", "Do not show terminal/device/lever progress titles from other players in Goldor.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_titles_at_ss", "Hide Titles At SS", "Hide terminal/device/lever progress titles while you stand within 3 of (108, 119, 94).", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_titles_at_pre4", "Hide Titles At Pre-4", "Hide terminal/device/lever progress titles while you stand on the Goldor pad at (63-64, 127, 35-36).", SettingType.TOGGLE),
                setting("qol.dungeon_f7.title_crystal_text", "Crystal Title Text", "Use {current} and {total}. Blank keeps Crystals {current}/{total}.", SettingType.TEXT),
                setting("qol.dungeon_f7.title_wither_text", "Wither Title Text", "Use {name} for the wither. Blank keeps {name} Enraged.", SettingType.TEXT),
                setting("qol.dungeon_f7.title_terminal_text", "Terminal Title Text", "Use {name}, {player}, {current}, {total}. Blank keeps {name} {current}/{total}.", SettingType.TEXT),
                setting("qol.dungeon_f7.title_gate_text", "Gate Title Text", "Use {name}, {player}, {current}, {total}. Blank keeps {name} {current}/{total}.", SettingType.TEXT),
                setting("qol.dungeon_f7.timers", "Tick Timers", "Storm pad, lightning, Goldor and Necron timers.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_ticks", "Show Ticks", "Render countdowns as client ticks instead of seconds.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_symbol", "Show Symbol", "Append t or s after the countdown value.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_prefix", "Show Prefix", "Keep the timer label before the countdown value.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_maxor", "Maxor Start", "Timer from Maxor WELL WELL WELL.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_storm", "Storm Start", "Timer from Maxor TOO YOUNG TO DIE.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_pad", "Storm Pad Timer", "Storm pad countdown.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_lightning", "Storm PY / Lightning", "Storm lightning and PY timers.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_goldor", "Goldor Start", "Goldor and Core timers.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.timer_necron", "Necron Start", "Necron fight timer.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.crystals", "Maxors Crystals", "Crystal spawn/place HUD.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.maxor_stun", "Maxor Stun", "Countdown while Maxor is stunned after the crystal beam.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.storm_crush", "Storm Crush", "1s countdown after Storm's Oof crush.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.storm_lb", "Storm Last Breath", "Last 5s of Storm's 34s Last Breath window. Archer HUD.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.term_start", "Term Start", "5s countdown after Storm dies before Goldor terminals.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.crystal_spawn", "Spawn Timer", "34-tick HUD after Maxor beam or YOU TRICKED ME until crystals respawn.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.crystal_place", "Place Timer", "HUD time from Energy Crystal pickup to place.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.crystal_alert", "Place Alert", "Title while an Energy Crystal sits in the hotbar.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.melody_display", "Melody Display", "HUD column progress while Melody is open.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.wither_esp", "Wither ESP", "Per-boss Maxor/Storm/Goldor/Necron box colors.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.maxor_color", "Maxor", "Maxor box color.", SettingType.COLOR),
                setting("qol.dungeon_f7.storm_color", "Storm", "Storm box color.", SettingType.COLOR),
                setting("qol.dungeon_f7.goldor_color", "Goldor", "Goldor box color.", SettingType.COLOR),
                setting("qol.dungeon_f7.necron_color", "Necron", "Necron box color.", SettingType.COLOR),
                setting("qol.dungeon_f7.dragons", "Wither Dragons", "M7 dragon spray/arrow local titles.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_spray", "Send Ice Sprayed", "Local title when Ice Spray hits a dragon.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_arrows", "Send Arrows Hit", "Local title when arrows hit a dragon.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_health", "Dragon Health", "HUD remaining HP of nearby Wither King dragons.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_boxes", "Dragon Boxes", "Box the five M7 dragon spawn pads.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_tracers", "Dragon Tracers", "Draw lines to M7 dragon pads and nearby Wither King dragons.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_priority", "Dragon Priority", "HUD kill order. Paul moves Ice and Soul earlier.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_paul", "Paul", "Use the Paul split order Ice > Soul > Power > Flame > Apex.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.dragon_solo_class", "Solo Debuff", "Class shown on the dragon priority HUD.", SettingType.ENUM, DungeonF7Policy.DRAGON_SOLO_CLASSES),
                setting("qol.dungeon_f7.dragon_timer", "Dragon Spawn Timer", "HUD countdown after a Wither King dragon spawn.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.p3_display", "P3 Terminal Display", "HUD terminal/device/lever counts from P3 chat.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.sharp_shooter", "Sharp Shooter", "Box marked and current emerald blocks on the Goldor arrows device, and the next aim spots.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.sharp_aim", "Show Aim Positions", "Box up to three greedy double-shot aim spots. Off by default.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.sharp_complete", "Device Complete Alert", "Local title when you finish the Sharp Shooter device.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.sharp_marked_color", "Marked Color", "Already hit Sharp Shooter blocks.", SettingType.COLOR),
                setting("qol.dungeon_f7.sharp_target_color", "Target Color", "Current emerald Sharp Shooter block.", SettingType.COLOR),
                setting("qol.dungeon_f7.sharp_aim1_color", "First Aim Color", "Best aim that covers the current emerald.", SettingType.COLOR),
                setting("qol.dungeon_f7.sharp_aim2_color", "Second Aim Color", "Second greedy aim.", SettingType.COLOR),
                setting("qol.dungeon_f7.sharp_aim3_color", "Third Aim Color", "Third greedy aim.", SettingType.COLOR),
                setting("qol.dungeon_f7.term_times", "Terminal Times", "HUD and local chat section splits from Goldor terminal/device/lever chat.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.term_pbs", "Terminal PBs", "Store and show local personal bests next to Goldor section splits.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.reset_term_pbs", "Reset Terminal PBs", "Clear stored Goldor section and terminal-type personal bests.", SettingType.ACTION),
                setting("qol.dungeon_f7.predev", "Predev Timer", "Healer HUD from Maxor WELL WELL WELL until you leap after standing within 3 of (1, 77).", SettingType.TOGGLE),
                setting("qol.dungeon_f7.predev_all", "Predev All Classes", "Track predev for every class, not only Healer.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.ss_complete", "SS Device Complete", "Local title when you complete a device within 3 blocks of (108, 119, 94).", SettingType.TOGGLE),
                setting("qol.dungeon_f7.pre4_complete", "Pre-4 Device Complete", "Local title when you complete a device on the Goldor pad at (63-64, 127, 35-36).", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_at_ss", "Hide Players At SS", "Hide teammates standing within 3 of the Simon Says pad (108, 119, 94) in boss.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_at_ss_pre_terms", "Hide At SS Before Terms", "Only hide players at Simon Says before Goldor terminals start.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_after_leap", "Hide After Leap", "Hide teammates for 3s after you leap.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_after_leap_boss", "Hide After Leap In Boss", "Only hide teammates after leap while the sidebar looks like a boss fight.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.reset_predev_pb", "Reset Predev PB", "Clear the stored predev personal best.", SettingType.ACTION),
                setting("qol.dungeon_f7.goldor_frenzy", "Goldor Frenzy Timer", "HUD ticks for Goldor frenzy after Storm dies.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.purple_pad", "Purple Pad Timer", "HUD ticks for Storm purple-pad after ENERGY/THUNDER.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.simon", "Simon Says", "Box Simon buttons from the sea-lantern sequence on the device wall.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.simon_progress", "Progress Display", "HUD current Simon stage.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.simon_first_color", "First Color", "Next Simon button.", SettingType.COLOR),
                setting("qol.dungeon_f7.simon_second_color", "Second Color", "Second Simon button.", SettingType.COLOR),
                setting("qol.dungeon_f7.simon_other_color", "Other Color", "Rest of the Simon sequence.", SettingType.COLOR),
                setting("qol.dungeon_f7.simon_block_wrong", "Block Wrong Clicks", "Cancel clicks that are not the next Simon button. Sneak to override.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.simon_auto", "Auto Start", "Click the start button when looking at it.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.simon_trigger", "Simon Triggerbot", "Click the next Simon button when looking at it.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.simon_sounds", "Simon Sounds", "Play a local note when a Simon start or sequence button is used.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.hide_diorite", "I Hate Diorite", "Rewrite Maxor pillar diorite to stained glass on the client.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.arrow_align", "Arrow Align", "Show remaining clicks on the F7 arrow item-frame grid.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.arrow_block_wrong", "Block Wrong Clicks", "Cancel item-frame clicks that are not part of the Arrow Align solution. Sneak to override.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.i4", "I4 Helper", "Box remaining I4 sea lanterns at 64-68 126-130 50.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.i4_color", "Target Color", "Remaining I4 lantern color.", SettingType.COLOR),
                setting("qol.dungeon_f7.i4_predict", "Show Prediction", "Highlight the next I4 lantern.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.i4_predict_color", "Prediction Color", "Next I4 lantern color.", SettingType.COLOR),
                setting("qol.dungeon_f7.auto_i4", "Auto I4", "Aim and click remaining I4 lanterns. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.auto_i4_rotation", "I4 Rotation Time", "Milliseconds to interpolate look at the next I4 lantern. 0 snaps instantly.", SettingType.NUMBER),
                setting("qol.dungeon_f7.auto_i4_rod", "Auto Rod", "Use a fishing rod at Storm death tick 174 while on I4.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.auto_i4_mask", "Auto Mask", "Swap to Bonzo/Spirit Mask at Storm death tick 244 while on I4.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.auto_i4_leap", "Auto Leap", "Open Spirit Leap at tick 307 or after you complete the device.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.auto_i4_leap_melody", "Leap To Melody", "Prefer leaping to the player who opened Melody.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.auto_i4_leap_class", "Leap Priority", "Preferred class if Melody leap is off or missing.", SettingType.ENUM, DungeonPolicy.I4_LEAP_CLASSES),
                setting("qol.dungeon_f7.debuff", "Debuff Helper", "HUD countdown for F7 Ice Spray and Wither King dragon phases.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.debuff_auto", "Auto Debuff", "Use Ice Spray or Gravity Wand from the hotbar when a Debuff phase starts. Cheat, off by default.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.debuff_ice", "Auto Ice Spray", "Swap to Ice Spray Wand for the auto debuff.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.debuff_gravity", "Auto Gravity Wand", "Swap to Gravity Wand if Ice Spray is missing.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.gate", "Gate Highlight", "Box the P3 coal-block gates while they still exist.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.gate_color", "Outline", "P3 gate outline color.", SettingType.COLOR),
                setting("qol.dungeon_f7.relics", "M7 Relics", "Box relic spawn and cauldron pads.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.relic_look", "Relic Look", "Rotate toward the matching cauldron after you pick up a relic.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.relic_look_time", "Relic Look Time", "Milliseconds to interpolate look at the relic cauldron.", SettingType.NUMBER),
                setting("qol.dungeon_f7.relic_spawn", "Spawn Timer", "HUD until relics spawn after Necron.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.relic_spawn_ticks", "Relic Spawn Ticks", "Client ticks (50ms) until the relic HUD after Necron. Default 840 (42s).", SettingType.NUMBER),
                setting("qol.dungeon_f7.relic_beacon", "Relic Beacon", "Draw a vertical line from each relic cauldron.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.relic_place", "Place Timer", "HUD from relic pickup to cauldron.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.relic_highlight", "Held Relic Pad", "Box the matching cauldron after you pick up a relic.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.relic_block_wrong", "Block Wrong Relic", "Cancel clicks on the wrong relic cauldron, or a relic pad while not holding a relic or SkyBlock Menu. Sneak is not an override.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_f7.breaker_prevent_secrets", "Breaker Skip Secrets", "Do not mine secret blocks with Dungeon Breaker.", SettingType.TOGGLE),
                setting("qol.dungeon_f7.breaker_charges", "Breaker Charges HUD", "Show Dungeon Breaker Charges: N/M from lore.", SettingType.TOGGLE),
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
                setting("qol.dungeon_f7.superboom_blocks", "Extra Blocks", "Comma-separated extra breakable ids. Use /rot superboom add while looking at a block.", SettingType.TEXT)));

        modules.add(module(
                "qol.dungeon_menus",
                "Dungeon Menus",
                "Salvage overlay, Party Finder Cata filter and chest-profit coin highlight for Croesus.",
                Group.DUNGEONS,
                "Menus",
                false,
                true,
                true,
                List.of("salvage", "party finder", "croesus", "chest profit"),
                setting("qol.dungeon_menus.salvage", "Salvage Overlay", "Tint dungeon gear by base stat boost.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.salvage50_color", "50% Color", "Perfect 50% boost tint.", SettingType.COLOR),
                setting("qol.dungeon_menus.salvage_low_color", "Under 50 Color", "Lower boost tint.", SettingType.COLOR),
                setting("qol.dungeon_menus.party_finder", "Party Finder", "Tint parties at or above the Cata floor.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.party_cata", "Min Cata", "Minimum Catacombs level to highlight.", SettingType.NUMBER),
                setting("qol.dungeon_menus.pf_stats", "Show Stats", "Append cached SkyCrypt dungeon stats when the lore does not already show them.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.pf_stack", "Party Stack Size", "Keep vanilla party-head stack size visible.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.pf_highlight", "Highlight Parties", "Tint joinable, dupe, blocked, VC, perm, and carry parties from lore keywords.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.pf_joinable", "Joinable Color", "Lore looks joinable.", SettingType.COLOR),
                setting("qol.dungeon_menus.pf_dupe", "Dupe Color", "Duplicate class / dupe keyword.", SettingType.COLOR),
                setting("qol.dungeon_menus.pf_blocked", "Blocked Color", "Blocked or blacklist keyword.", SettingType.COLOR),
                setting("qol.dungeon_menus.pf_vc", "VC Color", "Voice-chat requirement.", SettingType.COLOR),
                setting("qol.dungeon_menus.pf_perm", "Perm Color", "Permanent party.", SettingType.COLOR),
                setting("qol.dungeon_menus.pf_carry", "Carry Color", "Carry listing.", SettingType.COLOR),
                setting("qol.dungeon_menus.chest_profit", "Chest Profit", "Croesus/chest overlay from lore coins plus shipped prices. Off by default.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.chest_spin", "Chest Prize Reel", "Local spinning reel over Wood/Gold/Diamond/Emerald/Obsidian/Bedrock reward chests. Overlay only; does not buy the chest.", SettingType.TOGGLE, "spin", "gambling"),
                setting("qol.dungeon_menus.include_essence", "Include Essence", "Count Wither/Undead essence from the shipped price table.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.include_cost", "Subtract Chest Cost", "Subtract the lore Cost: N Coins line from profit.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.compact_profit", "Compact Profit", "Only show chest name and net profit.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.profit_color", "Profit Color", "Coin-loot tint.", SettingType.COLOR),
                setting("qol.dungeon_menus.close_chest", "Close Chest", "Close a plain Chest GUI as soon as it opens. Off by default.", SettingType.TOGGLE),
                setting("qol.dungeon_menus.close_chest.mode", "Close Mode", "Auto closes on open. Any Key closes on the next key or click.", SettingType.ENUM, DungeonF7Policy.CLOSE_CHEST_MODES),
                setting("qol.dungeon_menus.close_chest_min", "Close Min Delay", "Minimum ticks before Auto close. Default 0.", SettingType.NUMBER),
                setting("qol.dungeon_menus.close_chest_max", "Close Max Delay", "Maximum ticks before Auto close. Default 1.", SettingType.NUMBER)));

        modules.add(module(
                "qol.dungeon_carry",
                "Dungeon Carry Tracker",
                "Track dungeon-floor carries, party progress, optional webhook, and a movable HUD.",
                Group.DUNGEONS,
                "Carry",
                false,
                true,
                true,
                List.of("dungeon carry", "floor carry", "dcarry"),
                setting("qol.dungeon_carry.announce_party", "Announce In Party", "Send party progress when a matching floor completes.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.show_start", "Show Start Message", "Local chat when a carry is added.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.webhook", "Discord Webhook", "Send carry progress to a user-configured Discord webhook. Off by default.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.webhook_each", "Send Each Run", "Send progress after every matching floor instead of completion only.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.webhook_url", "Webhook URL", "Stored only in the local ignored config. Never included in releases or diagnostics.", SettingType.TEXT),
                setting("qol.dungeon_carry.highlight", "Highlight Player", "Box the carried player in-world.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.player_color", "Player Color", "Carried-player box color.", SettingType.COLOR),
                setting("qol.dungeon_carry.line_width", "Line Width", "Carried-player outline width.", SettingType.NUMBER),
                setting("qol.dungeon_carry.open_manager", "Open Manager", "Open active dungeon carries by floor.", SettingType.ACTION),
                setting("qol.dungeon_carry.display", "Dungeon Carry Display", "Show active carry progress in a movable HUD.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.only_dungeons", "Only In Dungeons", "Hide the HUD outside dungeons.", SettingType.TOGGLE),
                setting("qol.dungeon_carry.open_hud_editor", "Open HUD Elements Editor", "Move the Dungeon Carry HUD.", SettingType.ACTION, "position")));

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
                "qol.dungeon_party_join",
                "Party Finder Join Stats",
                "Print SkyCrypt stats when someone joins from Party Finder. Optional delayed auto-kick. Cheat, off.",
                Group.DUNGEONS,
                "Menus",
                false,
                true,
                true,
                List.of("party finder join", "auto kick", "join stats"),
                setting("qol.dungeon_party_join.stats", "Print Stats", "Chat the cached SkyCrypt dungeon stats for the joining player.", SettingType.TOGGLE),
                setting("qol.dungeon_party_join.auto_kick", "Auto Kick", "Send /p kick if PB, secrets, secret average, or MP is below the threshold. Cheat, off.", SettingType.TOGGLE, "cheat"),
                setting("qol.dungeon_party_join.detect_floor", "Detect Floor", "Use the current dungeon floor when comparing personal bests.", SettingType.TOGGLE),
                setting("qol.dungeon_party_join.required_pb", "Required PB", "Kick if slower than this time, for example 5:30.", SettingType.TEXT),
                setting("qol.dungeon_party_join.required_secrets", "Required Secrets", "Kick if secrets are below this count, for example 50k.", SettingType.TEXT),
                setting("qol.dungeon_party_join.required_avg", "Required Secret Avg", "Kick if secret average is below this, for example 8.4.", SettingType.TEXT),
                setting("qol.dungeon_party_join.required_mp", "Required MP", "Kick if magical power is below this, for example 800.", SettingType.TEXT),
                setting("qol.dungeon_party_join.kick_message", "Kick Message", "Send a delayed party-chat kick reason.", SettingType.TOGGLE),
                setting("qol.dungeon_party_join.send_party", "Send In Party", "Use /pc for the kick message.", SettingType.TOGGLE),
                setting("qol.dungeon_party_join.message_delay", "Message Delay", "Ticks to wait before /p kick and the party message.", SettingType.NUMBER)));

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
                "qol.dungeon_watcher",
                "Watcher Helper",
                "Blood-camp speak/move/spawned timings, speed-bucket titles, and a movable HUD.",
                Group.DUNGEONS,
                "Blood",
                false,
                true,
                true,
                List.of("watcher", "blood camp", "blood timers"),
                setting("qol.dungeon_watcher.breakdown", "Send Breakdown", "Local chat for speak, move, and spawned timings.", SettingType.TOGGLE),
                setting("qol.dungeon_watcher.spawned_all", "Alert All Spawned", "Title when The Watcher finishes spawning.", SettingType.TOGGLE),
                setting("qol.dungeon_watcher.speak", "Alert Speak", "Title when The Watcher says Let's see how you can handle this.", SettingType.TOGGLE),
                setting("qol.dungeon_watcher.move", "Alert Move", "Title when The Watcher entity starts moving.", SettingType.TOGGLE),
                setting("qol.dungeon_watcher.blood_timers", "Blood Timers HUD", "Show speak/move/total timers.", SettingType.TOGGLE),
                setting("qol.dungeon_watcher.show_ticks", "Show Ticks", "Show timers as client ticks instead of seconds.", SettingType.TOGGLE),
                setting("qol.dungeon_watcher.text_fast", "Fast Text", "Alert text when speak is under 4s. Supports <red> tags.", SettingType.TEXT),
                setting("qol.dungeon_watcher.text_normal", "Normal Text", "Alert text for a normal blood camp.", SettingType.TEXT),
                setting("qol.dungeon_watcher.text_slow", "Slow Text", "Alert text for a slow blood camp.", SettingType.TEXT),
                setting("qol.dungeon_watcher.text_very_slow", "Very Slow Text", "Alert text for a very slow blood camp.", SettingType.TEXT),
                setting("qol.dungeon_watcher.open_hud_editor", "Open HUD Elements Editor", "Move the Watcher HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.world_scanner",
                "World Scanner",
                "Scan Crystal Hollows chunks for structures. Finds create a named waypoint (for example Mines of Divan) without scanning the whole world every frame.",
                Group.MINING,
                "Scanner",
                false,
                true,
                true,
                List.of("world scanner", "crystal hollows", "divan", "corleone", "fairy grotto"),
                worldScannerSettings()));

        modules.add(module(
                "qol.etherwarp",
                "Etherwarp",
                "Predicted Etherwarp destination plus a left-click warp helper.",
                Group.COMBAT,
                "Warp",
                false,
                true,
                true,
                List.of("etherwarp", "ether", "warp"),
                setting("qol.etherwarp.show_guess", "Show Guess", "Render predicted destination.", SettingType.TOGGLE),
                setting("qol.etherwarp.color", "Color", "Valid guess color.", SettingType.COLOR),
                setting("qol.etherwarp.show_failed", "Show When Failed", "Also show invalid destinations.", SettingType.TOGGLE),
                setting("qol.etherwarp.fail_color", "Fail Color", "Invalid destination color.", SettingType.COLOR),
                setting("qol.etherwarp.render_style", "Render Style", "Filled, outline, or both.", SettingType.ENUM,
                        EtherwarpPredictor.RENDER_STYLES, "outline"),
                setting("qol.etherwarp.full_block", "Full Block", "Highlight the whole destination cube instead of a thin top face.", SettingType.TOGGLE),
                setting("qol.etherwarp.use_server_position", "Use Server Position", "Use server-authoritative position.", SettingType.TOGGLE),
                setting("qol.etherwarp.depth", "Depth", "Respect depth vs visible-through.", SettingType.TOGGLE),
                setting("qol.etherwarp.sounds", "Sounds", "Local feedback sounds.", SettingType.TOGGLE),
                setting("qol.etherwarp.left_click_warp", "Left Click Warp", "Left-click an Etherwarp item to use it.", SettingType.TOGGLE, "lcew"),
                setting("qol.etherwarp.shift_automatically", "Shift Automatically", "Hold sneak briefly when left-click warping while standing.", SettingType.TOGGLE),
                setting("qol.etherwarp.keybind", "Keybind", "Toggle this module with a key. Blank means unbound.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.diana_burrows",
                "Diana Burrows",
                "Spade lava-trail guess (polynomial fit), START/MOB/TREASURE particle burrows, waypoints. Serveri.",
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
                "qol.item_rarity",
                "Item Rarity Background",
                "Tint inventory and hotbar slots by SkyBlock rarity. The card switch is this module; each rarity has its own color in Settings.",
                Group.RENDER,
                "Items",
                false,
                true,
                true,
                List.of("rarity", "item background", "hotbar"),
                setting("qol.item_rarity.style", "Render Style", "How the rarity tint is drawn.", SettingType.ENUM,
                        List.of("Filled outline", "Outline", "Filled")),
                setting("qol.item_rarity.hotbar", "Hotbar", "Also tint hotbar slots.", SettingType.TOGGLE),
                setting("qol.item_rarity.fill_alpha", "Filled Opacity", "How strong the rarity fill is. Lower keeps the item visible.", SettingType.NUMBER, "fill", "alpha"),
                setting("qol.item_rarity.outline_alpha", "Outline Opacity", "How strong the rarity outline is.", SettingType.NUMBER, "outline", "alpha"),
                setting("qol.item_rarity.common", "Common Color", "Slot tint for Common rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.uncommon", "Uncommon Color", "Slot tint for Uncommon rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.rare", "Rare Color", "Slot tint for Rare rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.epic", "Epic Color", "Slot tint for Epic rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.legendary", "Legendary Color", "Slot tint for Legendary rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.mythic", "Mythic Color", "Slot tint for Mythic rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.divine", "Divine Color", "Slot tint for Divine rarity. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.item_rarity.special", "Special Color", "Slot tint for Special rarity items. RGB picker with alpha.", SettingType.COLOR)));

        modules.add(module(
                "qol.mob_highlight",
                "Mob Highlight",
                "Highlight named mobs. Look at one and press Add Entity to remember it.",
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
                "qol.slayer_display",
                "Slayer Display",
                "Live boss, tier, owner, timer and attunement HUD from the shared Slayer engine.",
                Group.SLAYER,
                "HUD",
                false,
                true,
                true,
                List.of("slayer", "boss", "display", "hud", "timer"),
                setting("qol.slayer_display.kill_time", "Show Kill Time", "Keep the latest completed boss time on the HUD.", SettingType.TOGGLE),
                setting("qol.slayer_display.dynamic_size", "Dynamic Text Size", "Shrink only long Slayer HUD lines to keep the panel compact without truncation.", SettingType.TOGGLE),
                setting("qol.slayer_display.open_hud_editor", "Open HUD Elements Editor", "Move the Slayer HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.slayer_stats",
                "Slayer Stats",
                "Session boss count, hourly rate and kill-time statistics from the same Slayer engine.",
                Group.SLAYER,
                "HUD",
                false,
                true,
                true,
                List.of("slayer", "stats", "bosses per hour", "kill time"),
                setting("qol.slayer_stats.bosses_killed", "Bosses Killed", "Show personal Slayer boss kills.", SettingType.TOGGLE),
                setting("qol.slayer_stats.bosses_per_hour", "Bosses / Hour", "Show the current session rate.", SettingType.TOGGLE),
                setting("qol.slayer_stats.average_kill_time", "Average Kill Time", "Show mean owned-boss kill duration.", SettingType.TOGGLE),
                setting("qol.slayer_stats.session_time", "Session Time", "Show elapsed Slayer session time.", SettingType.TOGGLE),
                setting("qol.slayer_stats.reset_session", "Reset Slayer Session", "Clear local Slayer session totals and rare-drop counts. Settings and carry history are kept.", SettingType.ACTION, "reset"),
                setting("qol.slayer_stats.open_hud_editor", "Open HUD Elements Editor", "Move the Slayer Stats HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.slayer_time_messages",
                "Slayer Time Messages",
                "Report verified owned-boss kill time and quest completion from the shared Slayer lifecycle.",
                Group.SLAYER,
                "HUD",
                false,
                true,
                true,
                List.of("slayer", "time", "kill", "quest", "personal best"),
                setting("qol.slayer_time_messages.time_to_kill", "Time To Kill", "Send the owned boss kill time in local chat.", SettingType.TOGGLE),
                setting("qol.slayer_time_messages.personal_best", "Personal Best", "Mark a new saved fastest kill for the same Slayer family and tier.", SettingType.TOGGLE),
                setting("qol.slayer_time_messages.quest_complete", "Quest Complete", "Send spawn-to-completion time when the quest completion chat line arrives.", SettingType.TOGGLE),
                setting("qol.slayer_time_messages.compact", "Compact Messages", "Use a shorter local message format.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_progress",
                "Slayer Progress",
                "Show verified Slayer Combat XP from the action bar, sidebar, or tab and warn once when it crosses the configured boss-spawn threshold.",
                Group.SLAYER,
                "HUD",
                false,
                true,
                true,
                List.of("slayer", "combat xp", "progress", "boss", "spawn", "warning", "hud"),
                setting("qol.slayer_progress.show_remaining", "Show Remaining XP", "Include the Combat XP still needed to spawn the boss.", SettingType.TOGGLE),
                setting("qol.slayer_progress.boss_warning", "Boss Spawn Warning", "Play a local title and sound only when verified progress crosses the threshold.", SettingType.TOGGLE),
                setting("qol.slayer_progress.warning_repeat", "Repeat Higher Progress", "Repeat the local warning only when verified Combat XP reaches a higher completion.", SettingType.TOGGLE),
                setting("qol.slayer_progress.warning_percent", "Warning Percent", "Combat XP percentage that triggers the one-time local boss-spawn warning.", SettingType.NUMBER),
                setting("qol.slayer_progress.open_hud_editor", "Open HUD Elements Editor", "Move the Slayer Progress HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.slayer_highlights",
                "Slayer Highlights",
                "Highlight detected Slayer bosses, minibosses and Inferno demons. Boxes, fight markers and target lines stay on your own fight.",
                Group.SLAYER,
                "Fight view",
                false,
                true,
                true,
                List.of("slayer", "highlight", "boss", "miniboss", "demon", "esp"),
                setting("qol.slayer_highlights.only_mine", "Only For Mine", "Highlight only entities owned by the local player. Every Slayer family stays on your own boss.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.depth", "Depth Check", "Hide highlights behind solid blocks.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.target_lines", "Target Lines", "Draw a line from your view to verified Slayer targets.", SettingType.TOGGLE, "tracer", "line"),
                setting("qol.slayer_highlights.target_line_width", "Target Line Width", "Set the width of verified Slayer target lines.", SettingType.NUMBER, "tracer", "width"),
                setting("qol.slayer_highlights.target_line_distance", "Target Line Distance", "Do not draw target lines beyond this many blocks.", SettingType.NUMBER, "tracer", "range"),
                section("qol.slayer_highlights.section_boss", "BOSS"),
                setting("qol.slayer_highlights.boss", "Highlight Boss", "Highlight Slayer bosses.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.boss_color", "Boss Color", "Boss box color.", SettingType.COLOR),
                setting("qol.slayer_highlights.boss_width", "Boss Line Width", "Boss outline width.", SettingType.NUMBER),
                section("qol.slayer_highlights.section_miniboss", "MINIBOSS"),
                setting("qol.slayer_highlights.miniboss", "Highlight Miniboss", "Highlight Slayer minibosses.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.miniboss_color", "Miniboss Color", "Miniboss box color.", SettingType.COLOR),
                setting("qol.slayer_highlights.miniboss_width", "Miniboss Line Width", "Miniboss outline width.", SettingType.NUMBER),
                section("qol.slayer_highlights.section_demon", "INFERNO DEMON"),
                setting("qol.slayer_highlights.demon", "Highlight Demon", "Highlight Quazii and Typhoeus.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.demon_color", "Demon Color", "Inferno demon box color.", SettingType.COLOR),
                setting("qol.slayer_highlights.demon_width", "Demon Line Width", "Inferno demon outline width.", SettingType.NUMBER),
                section("qol.slayer_highlights.section_clutter", "CLUTTER"),
                setting("qol.slayer_highlights.hide_spawn_particles", "Hide Spawn Particles", "Hide enchant/witch spawn particles near recently killed Slayer-quest mobs.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.hide_damage_splash", "Hide Damage Splash", "Hide floating damage numbers near a live Slayer boss.", SettingType.TOGGLE),
                setting("qol.slayer_highlights.hide_mob_names", "Hide Spawn Nametags", "Hide full-HP spawn-kill mob nametags. Damaged, runic and boss names stay visible.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_miniboss_alert",
                "Miniboss Alert",
                "Alert when Hypixel announces that your Slayer miniboss spawned. Other players' minibosses are ignored.",
                Group.SLAYER,
                "Alerts",
                false,
                true,
                true,
                List.of("slayer", "miniboss", "alert", "spawn"),
                setting("qol.slayer_miniboss_alert.message", "Send Message", "Send a local chat notification.", SettingType.TOGGLE),
                setting("qol.slayer_miniboss_alert.title", "Show Title", "Show the alert above the hotbar.", SettingType.TOGGLE),
                setting("qol.slayer_miniboss_alert.distance", "Maximum Distance", "Kept for saved configs. Alerts now follow your spawn chat, not nearby foreign minibosses.", SettingType.NUMBER),
                setting("qol.slayer_miniboss_alert.text", "Alert Text", "Message for a regular Slayer miniboss.", SettingType.TEXT),
                setting("qol.slayer_miniboss_alert.big_text", "Big Miniboss Text", "Message for the strongest miniboss tier.", SettingType.TEXT)));

        modules.add(module(
                "qol.slayer_active_boss_transparency",
                "Active Boss Transparency",
                "Fade background entities locally while a verified owned or attacked carry Slayer boss is active.",
                Group.SLAYER,
                "Fight view",
                false,
                true,
                true,
                List.of("slayer", "boss", "transparency", "fade", "carry", "render"),
                setting("qol.slayer_active_boss_transparency.strength", "Transparency Strength", "How transparent background entities become during the verified boss fight.", SettingType.NUMBER),
                setting("qol.slayer_active_boss_transparency.other_players", "Other Players", "Also fade other players; the local player and tracked Slayer bosses stay visible.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_irrelevant_mobs",
                "Hide Irrelevant Mobs",
                "Fade non-target mob families locally during a verified owned or attacked carry Slayer boss fight.",
                Group.SLAYER,
                "Fight view",
                false,
                true,
                true,
                List.of("slayer", "mobs", "fade", "transparency", "quest", "render"),
                setting("qol.slayer_irrelevant_mobs.strength", "Transparency Strength", "How transparent non-target mob families become. Vampire is held back until its mob identities are verified.", SettingType.NUMBER)));

        modules.add(module(
                "qol.slayer_drops",
                "Slayer Drops Data",
                "Track rare Slayer drop chat lines and bosses since the latest rare drop.",
                Group.SLAYER,
                "Drops",
                false,
                true,
                true,
                List.of("slayer", "drops", "rng", "meter"),
                setting("qol.slayer_drops.bosses_since", "Bosses Since Last Drop", "Show owned bosses since the latest rare drop.", SettingType.TOGGLE),
                setting("qol.slayer_drops.show_chance", "Show Chance", "Show the verified RNG Meter chance in the HUD and local Meter updates.", SettingType.TOGGLE, "rng", "chance"),
                setting("qol.slayer_drops.detect_automatically", "Detect Automatically", "Follow the latest RNG Meter selection from chat, the Slayer / Voidgloom menu, and /rng. The choice is remembered per family across warps.", SettingType.TOGGLE),
                setting("qol.slayer_drops.rng_hud", "RNG Meter HUD", "Show verified selected drop, Stored XP and chance in a movable HUD.", SettingType.TOGGLE, "hud", "rng"),
                setting("qol.slayer_drops.rng_warn_empty", "Warn Empty", "Show a local warning when an observed Slayer RNG Meter update has no selected known drop.", SettingType.TOGGLE, "rng", "warning"),
                setting("qol.slayer_drops.rng_hide_chat", "Hide Chat", "Hide only the standard RNG Meter Stored XP chat update after a known selection is detected. Local HUD tracking continues.", SettingType.TOGGLE, "rng", "chat"),
                setting("qol.slayer_drops.open_rng_hud_editor", "Edit RNG Meter HUD", "Move the Slayer RNG Meter HUD.", SettingType.ACTION, "position"),
                setting("qol.slayer_drops.profit_hud", "Item Profit HUD", "Show Bazaar-valued Slayer drops. Drops without a live quote stay unpriced.", SettingType.TOGGLE, "hud", "profit"),
                setting("qol.slayer_drops.open_profit_hud_editor", "Edit Item Profit HUD", "Move the Slayer item profit HUD.", SettingType.ACTION, "position"),
                setting("qol.slayer_drops.profit_table", "Show Drop Rows", "Show the highest-valued recognized drops as individual rows in the Item Profit HUD.", SettingType.TOGGLE, "hud", "table"),
                setting("qol.slayer_drops.profit_items_shown", "Drop Rows Shown", "Maximum recognized drop rows shown in the Item Profit HUD.", SettingType.NUMBER, "hud", "rows"),
                setting("qol.slayer_drops.profit_per_hour", "Show Profit / Hour", "Show the current local Slayer-session value rate in the Item Profit HUD.", SettingType.TOGGLE, "hud", "hour"),
                setting("qol.slayer_drops.profit_hide_outside_inventory", "Hide Outside Inventory", "Show the read-only Slayer Item Profit HUD only while an inventory screen is open. The HUD editor is always available.", SettingType.TOGGLE, "hud", "visibility"),
                setting("qol.slayer_drops.ground_highlight", "Rare Drop Ground Highlight", "Outline recognized Slayer drops that are currently on the ground.", SettingType.TOGGLE, "world", "drops"),
                setting("qol.slayer_drops.ground_labels", "Rare Drop Ground Labels", "Show a local name and live Bazaar value label above recognized ground drops.", SettingType.TOGGLE, "world", "label"),
                setting("qol.slayer_drops.ground_label_minimum", "Ground Label Minimum", "Only show a ground label when its full stack has a live Bazaar value at or above this amount.", SettingType.NUMBER, "world", "price"),
                setting("qol.slayer_drops.price_in_chat", "Price In Chat", "Show a local message for a recognized rare drop when a live Bazaar quote is available.", SettingType.TOGGLE, "price", "chat"),
                setting("qol.slayer_drops.price_title", "High Value Title", "Show a local title when a recognized rare drop meets the live-price threshold.", SettingType.TOGGLE, "price", "alert"),
                setting("qol.slayer_drops.price_title_sound", "High Value Sound", "Play a local notification sound with a high-value drop title.", SettingType.TOGGLE, "price", "sound"),
                setting("qol.slayer_drops.price_title_minimum", "High Value Minimum", "Minimum live Bazaar instant-sell value required for the title.", SettingType.NUMBER, "price", "threshold"),
                setting("qol.slayer_drops.recent_highlight", "Recent Drop Highlight", "Tint the latest recognized drop in the Item Profit HUD.", SettingType.TOGGLE, "hud", "recent"),
                setting("qol.slayer_drops.open_filter_editor", "Drop Filter Editor", "Choose each Slayer drop that can reset the since-last counter and appear in alerts.", SettingType.ACTION)));

        modules.add(module(
                "qol.slayer_carry",
                "Slayer Carry Tracker",
                "Track per-player Slayer carry progress from detected boss owner, type, tier and death.",
                Group.SLAYER,
                "Carry",
                false,
                true,
                true,
                List.of("slayer", "carry", "manager", "party"),
                setting("qol.slayer_carry.announce_party", "Announce In Party", "Prepare party progress messages when a tracked carry completes.", SettingType.TOGGLE),
                setting("qol.slayer_carry.show_spawn_message", "Show Spawn Message", "Show a local message when a matching carry boss spawns.", SettingType.TOGGLE),
                setting("qol.slayer_carry.webhook", "Discord Webhook", "Send carry progress to a user-configured Discord webhook. Off by default.", SettingType.TOGGLE),
                setting("qol.slayer_carry.webhook_each", "Send Each Kill", "Send progress after every matching boss instead of completion only.", SettingType.TOGGLE),
                setting("qol.slayer_carry.webhook_url", "Webhook URL", "Stored only in the local ignored config. Never included in releases or diagnostics.", SettingType.TEXT),
                setting("qol.slayer_carry.void_t3_prices", "Voidgloom T3 Prices (M)", "Comma-separated accepted per-boss prices used to interpret completed trades.", SettingType.TEXT),
                setting("qol.slayer_carry.void_t4_prices", "Voidgloom T4 Prices (M)", "Comma-separated accepted per-boss prices used to interpret completed trades.", SettingType.TEXT),
                setting("qol.slayer_carry.inferno_t2_prices", "Inferno T2 Prices (M)", "Comma-separated accepted per-boss prices used to interpret completed trades.", SettingType.TEXT),
                setting("qol.slayer_carry.inferno_t3_prices", "Inferno T3 Prices (M)", "Comma-separated accepted per-boss prices used to interpret completed trades.", SettingType.TEXT),
                setting("qol.slayer_carry.inferno_t4_prices", "Inferno T4 Prices (M)", "Comma-separated accepted per-boss prices used to interpret completed trades.", SettingType.TEXT),
                setting("qol.slayer_carry.open_manager", "Open Manager", "Open active Slayer carries by boss family.", SettingType.ACTION),
                setting("qol.slayer_carry.display", "Slayer Carry Display", "Show active carry progress in a movable HUD.", SettingType.TOGGLE),
                setting("qol.slayer_carry.open_hud_editor", "Open HUD Elements Editor", "Move the Slayer Carry HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.slayer_cocoon_alert",
                "Cocoon Alert",
                "Alert and six-second HUD timer for the Vampire Slayer cocoon signal.",
                Group.SLAYER,
                "Alerts",
                false,
                true,
                true,
                List.of("slayer", "vampire", "cocoon", "alert", "timer"),
                setting("qol.slayer_cocoon_alert.show_alert", "Show Alert", "Show a title and play the configured local sound when cocooned.", SettingType.TOGGLE),
                setting("qol.slayer_cocoon_alert.message", "Alert Message", "Title text. Basic <red>, <aqua>, and other color tags are supported.", SettingType.TEXT),
                setting("qol.slayer_cocoon_alert.sound", "Alert Sound", "Minecraft sound id, for example block.note_block.pling.", SettingType.TEXT),
                setting("qol.slayer_cocoon_alert.pitch", "Pitch", "Local alert-sound pitch.", SettingType.NUMBER),
                setting("qol.slayer_cocoon_alert.volume", "Volume", "Local alert-sound volume.", SettingType.NUMBER),
                setting("qol.slayer_cocoon_alert.timer", "Cocoon Timer", "Show the six-second cocoon countdown in a movable HUD.", SettingType.TOGGLE),
                setting("qol.slayer_cocoon_alert.open_hud_editor", "Open HUD Elements Editor", "Move the Cocoon Timer HUD.", SettingType.ACTION, "position")));

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
                "qol.slayer_laser_hider",
                "Enderman Laser Hider",
                "Hide Guardian beam entities attached to other players' Voidgloom bosses.",
                Group.SLAYER,
                "Voidgloom",
                false,
                true,
                true,
                List.of("slayer", "voidgloom", "enderman", "laser", "guardian", "carry"),
                setting("qol.slayer_laser_hider.show_for_carries", "Show For Carries", "Also hide beams around a boss belonging to an active carry.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_attunement_display",
                "Attunement Display",
                "Movable HUD for the current owned Inferno boss attunement and shield count.",
                Group.SLAYER,
                "Blaze",
                false,
                true,
                true,
                List.of("slayer", "blaze", "inferno", "attunement", "display"),
                setting("qol.slayer_attunement_display.count", "Display Count", "Include the current attunement shield count.", SettingType.TOGGLE),
                setting("qol.slayer_attunement_display.open_hud_editor", "Open HUD Elements Editor", "Move the Attunement Display HUD.", SettingType.ACTION, "position")));

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
                "qol.slayer_sounds",
                "Slayer Sounds",
                "Local sound filters for Slayer fights.",
                Group.SLAYER,
                "Fight view",
                false,
                true,
                true,
                List.of("slayer", "voidgloom", "enderman", "sound"),
                setting("qol.slayer_sounds.disable_voidgloom", "Disable Voidgloom Sounds", "Suppress Enderman stare and scream sounds while enabled.", SettingType.TOGGLE),
                setting("qol.slayer_sounds.disable_vampire", "Disable Vampire Sounds", "Suppress bat and phantom noises during Vampire Slayer.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_vengeance",
                "Vengeance Timer",
                "Six-second Inferno Vengeance activation timer after the owned boss reaches ASHEN 7.",
                Group.SLAYER,
                "Blaze",
                false,
                true,
                true,
                List.of("slayer", "blaze", "inferno", "vengeance", "timer"),
                setting("qol.slayer_vengeance.compact", "Compact Display", "Show only the remaining value.", SettingType.TOGGLE),
                setting("qol.slayer_vengeance.use_ticks", "Use Ticks", "Show ticks instead of seconds.", SettingType.TOGGLE),
                setting("qol.slayer_vengeance.open_hud_editor", "Open HUD Elements Editor", "Move the Vengeance Timer HUD.", SettingType.ACTION, "position")));

        modules.add(module(
                "qol.slayer_vengeance_damage",
                "Vengeance Damage Tracker",
                "Report owned Inferno Vengeance hits of at least 500,000 damage from the boss damage marker.",
                Group.SLAYER,
                "Blaze",
                false,
                true,
                true,
                List.of("slayer", "inferno", "vengeance", "damage"),
                setting("qol.slayer_vengeance_damage.abbreviate", "Abbreviate Damage", "Display 1.25M instead of 1,250,000.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_big_drops",
                "Big Slayer Drops",
                "Temporarily enlarge known Slayer drops near the latest owned boss death.",
                Group.SLAYER,
                "Drops",
                false,
                true,
                true,
                List.of("slayer", "drops", "item", "scale", "rng"),
                setting("qol.slayer_big_drops.scale", "Scale", "Dropped-item model scale.", SettingType.NUMBER),
                setting("qol.slayer_big_drops.range", "Range Multiplier", "Detection radius multiplier around the boss death position.", SettingType.NUMBER),
                setting("qol.slayer_big_drops.unscale_seconds", "Unscale After", "Seconds before items return to normal scale.", SettingType.NUMBER),
                section("qol.slayer_big_drops.section_families", "ENABLE FOR"),
                setting("qol.slayer_big_drops.revenant", "Revenant", "Scale known Revenant drops.", SettingType.TOGGLE),
                setting("qol.slayer_big_drops.tarantula", "Tarantula", "Scale known Tarantula drops.", SettingType.TOGGLE),
                setting("qol.slayer_big_drops.sven", "Sven", "Scale known Sven drops.", SettingType.TOGGLE),
                setting("qol.slayer_big_drops.voidgloom", "Voidgloom", "Scale known Voidgloom drops.", SettingType.TOGGLE),
                setting("qol.slayer_big_drops.inferno", "Blaze", "Scale known Inferno drops.", SettingType.TOGGLE),
                setting("qol.slayer_big_drops.vampire", "Vampire", "Scale known Riftstalker drops.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_voidgloom",
                "Voidgloom Fight",
                "Yang Glyph beacons, Nukekubi skulls, phase HUD and particle filter for your own Voidgloom fight.",
                Group.SLAYER,
                "Voidgloom",
                false,
                true,
                true,
                List.of("slayer", "enderman", "voidgloom", "beacon", "yang glyph", "nukekubi"),
                setting("qol.slayer_voidgloom.highlight_beacon", "Highlight Beacon", "Outline flying and sitting Yang Glyph beacons.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.beacon_warning", "Beacon Warning", "Local Yang Glyph title when the boss throws a beacon.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.beacon_sound", "Beacon Sound", "Play a local pling with the Yang Glyph warning.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.beacon_line", "Line To Beacon", "Draw a line from your view to the beacon.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.beacon_path", "Beacon Path", "Draw the flying Yang Glyph trail.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.beacon_timer", "Beacon Timer", "Show the five-second explosion countdown on a sitting Yang Glyph.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.beacon_color", "Beacon Color", "Yang Glyph highlight color.", SettingType.COLOR),
                setting("qol.slayer_voidgloom.line_color", "Line Color", "Color of beacon lines and the flying trail.", SettingType.COLOR),
                setting("qol.slayer_voidgloom.line_width", "Line Width", "Width of beacon lines and the flying trail.", SettingType.NUMBER),
                setting("qol.slayer_voidgloom.highlight_held", "Held Beacon", "Outline a Voidgloom still carrying the Yang Glyph.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.highlight_nukekubi", "Highlight Nukekubi", "Outline Nukekubi skulls (eyes).", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.nukekubi_line", "Line To Nukekubi", "Draw a line to Nukekubi skulls.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.nukekubi_color", "Nukekubi Color", "Nukekubi highlight color.", SettingType.COLOR),
                setting("qol.slayer_voidgloom.world_labels", "World Labels", "Name flying beacons and Nukekubi skulls in the world.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.line_to_boss", "Line To Boss", "Draw a line from your view to your Voidgloom Seraph.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.boss_line_width", "Boss Line Width", "Width of the line to your Voidgloom Seraph.", SettingType.NUMBER),
                setting("qol.slayer_voidgloom.phase_display", "Phase Display", "Show Hits / Laser / Yang Glyph on the Slayer HUD.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.hits_display", "Hits Remaining", "Show the Hits-phase count from the boss hologram.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.laser_timer", "Laser Timer", "Eight-second countdown while the boss is in the laser phase.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.laser_health", "Health During Laser", "Keep showing Voidgloom HP on the Slayer HUD during the laser phase.", SettingType.TOGGLE),
                setting("qol.slayer_voidgloom.hide_particles", "Hide Particles", "Suppress smoke, flame and witch particles around your Voidgloom Seraph.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_revenant",
                "Revenant Fight",
                "BOOM warning and outlines for your own Revenant Horror fight.",
                Group.SLAYER,
                "Revenant",
                false,
                true,
                true,
                List.of("slayer", "revenant", "zombie", "boom", "atoned"),
                setting("qol.slayer_revenant.boom_display", "Boom Display", "Local title when the T5 hologram reads Boom!", SettingType.TOGGLE),
                setting("qol.slayer_revenant.boom_sound", "Boom Sound", "Play a local primed-TNT cue with the Boom warning.", SettingType.TOGGLE),
                setting("qol.slayer_revenant.boom_highlight", "Highlight Boom", "Outline the Boom hologram and the exploding boss.", SettingType.TOGGLE),
                setting("qol.slayer_revenant.boom_color", "Boom Color", "Boom highlight color.", SettingType.COLOR),
                setting("qol.slayer_revenant.world_labels", "World Labels", "Name the Boom hologram in the world.", SettingType.TOGGLE),
                setting("qol.slayer_revenant.line_to_boss", "Line To Boss", "Draw a line from your view to your Revenant Horror.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_tarantula",
                "Tarantula Fight",
                "Egg-sac outlines, hatchling lock, phase HUD and spider-sound mute for your own Tarantula Broodfather fight.",
                Group.SLAYER,
                "Tarantula",
                false,
                true,
                true,
                List.of("slayer", "tarantula", "spider", "egg sac", "hatchling"),
                setting("qol.slayer_tarantula.highlight_egg_sacs", "Highlight Egg Sacs", "Outline Broodfather egg sacs.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.egg_color", "Egg Sac Color", "Egg-sac highlight color.", SettingType.COLOR),
                setting("qol.slayer_tarantula.egg_hits", "Egg Hits Display", "Show remaining hits from the egg-sac timer hologram.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.highlight_invincible", "Mark When Invincible", "Highlight the boss during the hatchling lock.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.invincible_color", "Invincible Color", "Invincible-phase highlight color.", SettingType.COLOR),
                setting("qol.slayer_tarantula.invincible_text", "Text When Invincible", "Local title and HUD line: Kill hatchlings!", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.phase_display", "Phase Display", "Show Tara 5 phase 1/2 or 2/2 on the Slayer HUD.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.world_labels", "World Labels", "Name egg sacs and the hatchling lock in the world.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.mute_sounds", "Mute Spider Sounds", "Suppress spider and hatchling noises in Spider's Den, Crimson Isle or during a Tarantula fight.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.line_to_boss", "Line To Boss", "Draw a line from your view to your Tarantula Broodfather.", SettingType.TOGGLE),
                setting("qol.slayer_tarantula.boss_line_width", "Boss Line Width", "Width of the line to your Tarantula Broodfather.", SettingType.NUMBER)));

        modules.add(module(
                "qol.slayer_sven",
                "Sven Fight",
                "Pack-pup outlines, howl warning and wolf-sound mute for your own Sven Packmaster fight.",
                Group.SLAYER,
                "Sven",
                false,
                true,
                true,
                List.of("slayer", "sven", "wolf", "pup", "howl"),
                setting("qol.slayer_sven.highlight_pups", "Highlight Pups", "Outline Sven Pups summoned during the boss fight.", SettingType.TOGGLE),
                setting("qol.slayer_sven.pup_color", "Pup Color", "Sven Pup highlight color.", SettingType.COLOR),
                setting("qol.slayer_sven.pup_line", "Line To Pups", "Draw a line from your view to nearby Sven Pups.", SettingType.TOGGLE),
                setting("qol.slayer_sven.world_labels", "World Labels", "Name Sven Pups in the world.", SettingType.TOGGLE),
                setting("qol.slayer_sven.hide_pup_nametags", "Hide Pup Nametags", "Hide the floating Sven Pup hologram names.", SettingType.TOGGLE),
                setting("qol.slayer_sven.howl_warning", "Howl Warning", "Local title when the packmaster howls.", SettingType.TOGGLE),
                setting("qol.slayer_sven.mute_sounds", "Mute Wolf Sounds", "Suppress wolf noises in The Park, Hub or during a Sven fight.", SettingType.TOGGLE),
                setting("qol.slayer_sven.line_to_boss", "Line To Boss", "Draw a line from your view to your Sven Packmaster.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_vampire_markers",
                "Vampire Fight",
                "Ichor, Killer Spring, Twinclaws, Mania and Steak helpers for your own Riftstalker Bloodfiend fight.",
                Group.SLAYER,
                "Vampire",
                false,
                true,
                true,
                List.of("slayer", "vampire", "ichor", "spring", "twinclaws", "mania", "steak"),
                setting("qol.slayer_vampire_markers.blood_ichor", "Blood Ichor", "Outline Blood Ichor stands.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.ichor_color", "Ichor Color", "Blood Ichor highlight color.", SettingType.COLOR),
                setting("qol.slayer_vampire_markers.killer_spring", "Killer Spring", "Outline Killer Spring stands.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.spring_color", "Spring Color", "Killer Spring highlight color.", SettingType.COLOR),
                setting("qol.slayer_vampire_markers.twinclaws", "Twinclaws Warning", "Local title when Twinclaws is about to hit.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.twinclaws_delay", "Twinclaws Delay", "Delay the Twinclaws title in milliseconds.", SettingType.NUMBER),
                setting("qol.slayer_vampire_markers.mania", "Mania Warning", "Local title and HUD when Mania Circles are active.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.mania_timer", "Mania Timer", "Show remaining Mania Circles time on the Slayer HUD.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.steak_alert", "Steak Alert", "Local title when the boss is in steak range (below 20% HP).", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.steak_color", "Steak Color", "Boss outline color while steak is ready.", SettingType.COLOR),
                setting("qol.slayer_vampire_markers.world_labels", "World Labels", "Name Ichor, Spring, Twinclaws and Mania in the world.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.mute_sounds", "Mute Vampire Sounds", "Suppress bat, phantom, wither-spawn and guardian-curse noises in the Rift or during a Vampire fight.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.line_to_boss", "Line To Boss", "Draw a line from your view to your Riftstalker Bloodfiend.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.boss_line_width", "Boss Line Width", "Width of the line to your Bloodfiend.", SettingType.NUMBER),
                setting("qol.slayer_vampire_markers.ichor_beam", "Ichor Beacon Beam", "Draw a vertical beam on Blood Ichor.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.chalice", "Chalice Highlight", "Outline Stillgore chalices during a Vampire fight.", SettingType.TOGGLE),
                setting("qol.slayer_vampire_markers.chalice_color", "Chalice Color", "Chalice highlight color.", SettingType.COLOR),
                setting("qol.slayer_vampire_markers.effigies", "Effigy Waypoints", "Mark unbroken Stillgore effigies from the sidebar.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_inferno",
                "Inferno Fight",
                "Fire Pillar, Fire Pits, hellion colors and particle hide for your own Inferno Demonlord fight.",
                Group.SLAYER,
                "Blaze",
                false,
                true,
                true,
                List.of("slayer", "blaze", "inferno", "pillar", "hellion", "fire pit"),
                setting("qol.slayer_inferno.fire_pillar", "Pillar Display", "Title and HUD when a Fire Pillar hologram is about to explode, including named 5s stands.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.fire_pillar_sound", "Pillar Sound", "Play a local cue with the Fire Pillar warning.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.pillar_color", "Pillar Color", "Fire Pillar highlight color.", SettingType.COLOR),
                setting("qol.slayer_inferno.fire_pits", "Fire Pits", "Warn when Inferno T3/T4 enters the fire-pit phase.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.phase_display", "Phase Display", "Show Inferno 1/2 or 1/3 phase on the Slayer HUD from compact or full HP holograms.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.color_by_attunement", "Color By Attunement", "Outline your Inferno boss in the current hellion shield color.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.hide_chat", "Hide Attunement Chat", "Hide Hellion Shield and wrong-dagger chat lines.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.hide_particles", "Clear View", "Hide blaze flame/smoke particles and fireballs around your Inferno Demonlord.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.world_labels", "World Labels", "Name the Fire Pillar in the world.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.line_to_boss", "Line To Boss", "Draw a line from your view to your Inferno Demonlord.", SettingType.TOGGLE),
                setting("qol.slayer_inferno.gummy_warning", "Gummy Warning", "Warn when Habanero Tactics or Smoldering Tomb is active without a Re-Heated Gummy Polar Bear.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.slayer_quest_warning",
                "Quest Warning",
                "Warn when the sidebar Slayer quest does not match the mob family you are hitting.",
                Group.SLAYER,
                "Alerts",
                false,
                true,
                true,
                List.of("slayer", "quest", "warning", "wrong slayer"),
                setting("qol.slayer_quest_warning.title", "Show Title", "Show a local title on a quest mismatch.", SettingType.TOGGLE),
                setting("qol.slayer_quest_warning.chat", "Chat Message", "Also send a local chat line.", SettingType.TOGGLE)));

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
                "qol.viewmodel",
                "Viewmodel",
                "Offset, scale, rotate, and speed of the first-person held item.",
                Group.RENDER,
                "Items",
                false,
                true,
                true,
                List.of("viewmodel", "hand", "swing", "held item"),
                setting("qol.viewmodel.no_haste", "No Haste", "Ignore haste/fatigue on the swing animation.", SettingType.TOGGLE),
                setting("qol.viewmodel.no_equip", "No Equip Animation", "Skip the item-switch bob.", SettingType.TOGGLE),
                setting("qol.viewmodel.no_bow_swing", "No Bow Swing", "Stop the bow from swinging while drawn.", SettingType.TOGGLE),
                setting("qol.viewmodel.apply_to_hand", "Apply To Hand", "Also transform the empty hand.", SettingType.TOGGLE),
                setting("qol.viewmodel.swing_speed", "Swing Speed", "Custom swing duration. 0 keeps vanilla.", SettingType.NUMBER),
                setting("qol.viewmodel.offset_x", "Offset X", "Held-item X offset.", SettingType.NUMBER),
                setting("qol.viewmodel.offset_y", "Offset Y", "Held-item Y offset.", SettingType.NUMBER),
                setting("qol.viewmodel.offset_z", "Offset Z", "Held-item Z offset.", SettingType.NUMBER),
                setting("qol.viewmodel.scale_x", "Scale X", "Held-item X scale.", SettingType.NUMBER),
                setting("qol.viewmodel.scale_y", "Scale Y", "Held-item Y scale.", SettingType.NUMBER),
                setting("qol.viewmodel.scale_z", "Scale Z", "Held-item Z scale.", SettingType.NUMBER),
                setting("qol.viewmodel.rot_x", "Rotation X", "Held-item X rotation.", SettingType.NUMBER),
                setting("qol.viewmodel.rot_y", "Rotation Y", "Held-item Y rotation.", SettingType.NUMBER),
                setting("qol.viewmodel.rot_z", "Rotation Z", "Held-item Z rotation.", SettingType.NUMBER),
                setting("qol.viewmodel.swing_x", "Swing X", "Swing translate multiplier X.", SettingType.NUMBER),
                setting("qol.viewmodel.swing_y", "Swing Y", "Swing translate multiplier Y.", SettingType.NUMBER),
                setting("qol.viewmodel.swing_z", "Swing Z", "Swing translate multiplier Z.", SettingType.NUMBER)));

        modules.add(module(
                "qol.item_scale",
                "Item Scale",
                "Change the scale of items lying on the ground.",
                Group.RENDER,
                "Items",
                false,
                true,
                true,
                List.of("item scale", "dropped", "ground"),
                setting("qol.item_scale.scale", "Scale", "Dropped-item model scale.", SettingType.NUMBER)));

        modules.add(module(
                "qol.animation_fix",
                "Animation Fix",
                "Stops sneaking/swimming pose packets from playing the animation twice.",
                Group.UTILITIES,
                "Fixes",
                false,
                true,
                true,
                List.of("animation", "sneak", "swim", "dye", "skin"),
                setting("qol.animation_fix.dyes", "Animated Dyes", "Select deterministic local dye frames while rendering without changing ItemStack NBT.", SettingType.TOGGLE, "color"),
                setting("qol.animation_fix.skins", "Animated Skins", "Select deterministic local item-model frames while rendering without changing ItemStack NBT.", SettingType.TOGGLE, "model")));

        modules.add(module(
                "qol.disconnect_fix",
                "Disconnect Fix",
                "Catch corrupted entity-data packets that can disconnect Tarantula slayer.",
                Group.SLAYER,
                "Fixes",
                false,
                true,
                true,
                List.of("disconnect", "tarantula", "packet")));

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
                "qol.active_pet_highlight",
                "Active Pet Highlight",
                "Highlight the spawned pet in the Pets menu with a thin outline so the pet icon stays visible.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("pet", "pets menu"),
                setting("qol.active_pet_highlight.color", "Highlight Color", "Outline color for the active pet slot.", SettingType.COLOR)));

        modules.add(module(
                "qol.anvil_helper",
                "Anvil Helper",
                "Highlight matching enchanted books that can be combined in the SkyBlock Anvil.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("anvil", "enchant", "book"),
                setting("qol.anvil_helper.color", "Highlight Color", "Fill color for combinable books.", SettingType.COLOR)));

        modules.add(module(
                "qol.calendar_date",
                "Calendar Date",
                "Show the absolute date an event starts in the Calendar and Events menu.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("calendar", "date", "event", "starts in", "minister"),
                setting("qol.calendar_date.minister", "Minister In Calendar", "Show the current minister on Calendar and Events tooltips.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.experiment_solver",
                "Experiments Solver",
                "Track Chronomatron, Ultrasequencer, and Superpairs solutions in the Experimentation Table.",
                Group.INTERFACE,
                "Menus",
                false,
                true,
                true,
                List.of("experiment", "chronomatron", "ultrasequencer", "superpairs", "enchanting"),
                setting("qol.experiment_solver.chronomatron", "Chronomatron", "Remember and highlight the Chronomatron sequence.", SettingType.TOGGLE),
                setting("qol.experiment_solver.ultrasequencer", "Ultrasequencer", "Remember and highlight the Ultrasequencer order.", SettingType.TOGGLE),
                setting("qol.experiment_solver.superpairs", "Superpairs", "Remember revealed Superpairs items and their matches.", SettingType.TOGGLE),
                setting("qol.experiment_solver.block_wrong_clicks", "Block Wrong Clicks", "Swallow clicks on puzzle slots that are not the next correct slot.", SettingType.TOGGLE),
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
                "qol.escrow_fix",
                "Escrow Fix",
                "Reopen of AH/BZ after escrow chat closes the menu. Same chat lines on Serveri.",
                Group.UTILITIES,
                "Market",
                false,
                true,
                true,
                List.of("escrow", "auction house", "bazaar", "ah", "bz")));

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
                "qol.ghosts",
                "Ghosts",
                "Mist creepers in the Dwarven Mines. Highlight Style paints a fill/outline box; Show Powered Layer is off so the vanilla charged swirl does not cover it. Show Ghosts reveals the creeper body.",
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
                setting("qol.ghosts.show_powered", "Show Powered Layer", "Keep the vanilla charged overlay. Off matches Ghost Vision: only the colored box.", SettingType.TOGGLE),
                setting("qol.ghosts.keybind", "Keybind", "Toggle Ghosts.", SettingType.KEYBIND)));

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
                "qol.appearance",
                "Appearance",
                "Dashboard, HUD surface, chart, and background look. Opens a submenu of those pages. Mining Tracker overlay settings are on the Mining Tracker HUD button, not here.",
                Group.HUD_DISPLAY,
                "Visuals",
                false,
                false,
                true,
                List.of("appearance", "theme", "colors", "customizer", "look"),
                setting("qol.appearance.open_dashboard", "Dashboard", "Panels, sidebar, headers, and buttons.", SettingType.ACTION),
                setting("qol.appearance.open_colors", "Colors", "Text, HUD surfaces, and accents.", SettingType.ACTION),
                setting("qol.appearance.open_background", "Background", "Optional dashboard background image.", SettingType.ACTION),
                setting("qol.appearance.open_charts", "Charts", "Rate sparkline colors.", SettingType.ACTION),
                setting("qol.appearance.open_reset", "Reset", "Restore appearance defaults.", SettingType.ACTION)));

        modules.add(module(
                "qol.hud_layout",
                "HUD Elements Editor",
                "One editor for every Rot Client HUD. The card switch is this module. Each HUD still has its own on/off in its module. Open the editor to move, scale, and restyle backgrounds and text with the same RGB picker as other colors.",
                Group.HUD_DISPLAY,
                "Layout",
                false,
                true,
                true,
                List.of("hud editor", "layout", "overlay", "scale", "background"),
                setting("qol.hud_layout.show_background", "Show Background", "Draw a panel behind HUD text. Turn off for text-only overlays.", SettingType.TOGGLE),
                setting("qol.hud_layout.dim_unfocused", "Dim Other HUDs", "When you open a module's HUD editor, other HUDs stay visible but faded. You can still drag the faded ones.", SettingType.TOGGLE),
                setting("qol.hud_layout.background", "Background Color", "Default HUD panel fill. RGB picker with alpha.", SettingType.COLOR),
                setting("qol.hud_layout.text", "Text Color", "Default HUD text color. RGB picker.", SettingType.COLOR),
                setting("qol.hud_layout.scale", "HUD Scale", "Default size for QoL HUD panels. Mouse wheel or [ ] in the editor also scales the selected HUD.", SettingType.NUMBER),
                setting("qol.hud_layout.open_hud_editor", "Open HUD Elements Editor", "Show every HUD that is currently enabled and drag them on the world.", SettingType.ACTION, "position"),
                section("qol.hud_layout.section_vanilla", "Vanilla / Hypixel HUD"),
                setting("qol.hud_layout.hide_hotbar", "Hide Hotbar", "Hide the vanilla hotbar.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_health", "Hide Health Hearts", "Hide vanilla hearts.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_food", "Hide Hunger", "Hide vanilla hunger.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_armor", "Hide Armor Icons", "Hide vanilla armor icons.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_xp", "Hide XP Bar / Level", "Hide XP bar and level.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_air", "Hide Air Bubbles", "Hide air bubbles.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_mount", "Hide Mount Health", "Hide mount health.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_scoreboard", "Hide Scoreboard", "Hide the sidebar scoreboard.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_boss", "Hide Boss Bar", "Hide the boss bar.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_action", "Hide Action Bar", "Hide the action bar.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_item_name", "Hide Held Item Name", "Hide the held-item name popup.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_effects", "Hide Status Effects", "Hide status-effect icons.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_titles", "Hide Titles", "Hide title text.", SettingType.TOGGLE),
                setting("qol.hud_layout.hide_tab", "Hide Player Tab List", "Hide the player tab list.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.custom_cursor",
                "Custom Cursor",
                "Rot Client pointer while any game menu is open: size, fill, outline, accent, click pulse, and hold ring. Dashboard edges show a resize arrow like a browser window.",
                Group.HUD_DISPLAY,
                "Cursor",
                false,
                true,
                true,
                List.of("cursor", "mouse", "pointer", "resize"),
                setting("qol.custom_cursor.size", "Pointer Size", "How large the Rot pointer is. Drag the slider; it clamps so the pointer stays usable.", SettingType.NUMBER),
                setting("qol.custom_cursor.fill", "Fill Color", "Inside of the pointer. RGB picker.", SettingType.COLOR),
                setting("qol.custom_cursor.outline", "Outline Color", "Edge of the pointer. RGB picker.", SettingType.COLOR),
                setting("qol.custom_cursor.accent", "Accent Color", "Click flash and resize-arrow color.", SettingType.COLOR),
                setting("qol.custom_cursor.click_anim", "Click Pulse", "Pointer grows slightly on each click.", SettingType.TOGGLE),
                setting("qol.custom_cursor.hold_anim", "Hold Ring", "A ring grows while the mouse button is held.", SettingType.TOGGLE),
                setting("qol.custom_cursor.hide_vanilla", "Hide Vanilla Pointer", "Hide Minecraft's cursor so only the Rot pointer shows in menus.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.legacy_textures",
                "Legacy SkyBlock Textures",
                "Keep Hypixel's 26.2 SkyBlock resource pack for GUI fonts, HUD icons, and player-head skulls. When this module is on, custom 3D item models (Hyperion, Terminator, drills, armor, …) render as the old vanilla items. Off = new pack look. Default off.",
                Group.RENDER,
                "Items",
                false,
                true,
                true,
                List.of("legacy textures", "old items", "vanilla models", "hypixel pack", "retexture"),
                setting("qol.legacy_textures.items", "Replace Item Models", "Swap hypixel_skyblock custom models back to vanilla. Skulls and unmapped new-pack items stay on the server pack.", SettingType.TOGGLE)));

        modules.add(module(
                "qol.custom_resource_pack",
                "Dark SkyBlock Pack",
                "Built-in Rot Client dark world textures for overworld, Crimson Isle, and End. Off by default. Hidden from the vanilla resource-pack list — toggle it here. Works with Legacy SkyBlock Textures: that module remaps SkyBlock items to vanilla models, and this pack then paints those vanilla blocks/items dark.",
                Group.RENDER,
                "World",
                false,
                true,
                true,
                List.of("resource pack", "dark mode", "crimson isle", "end", "custom pack", "textures"),
                setting("qol.custom_resource_pack.overworld", "Dark Overworld", "Rot Client dark overworld block and item textures.", SettingType.TOGGLE),
                setting("qol.custom_resource_pack.crimson", "Dark Crimson Isle", "Rot Client dark Crimson Isle block textures.", SettingType.TOGGLE),
                setting("qol.custom_resource_pack.end", "Dark End", "Rot Client dark End island block textures.", SettingType.TOGGLE),
                setting("qol.custom_resource_pack.gameplay_font", "Gameplay Font", "Use the bundled Source Sans gameplay font with missing-glyph fallback.", SettingType.TOGGLE, "font", "source sans")));

        modules.add(module(
                "qol.iota",
                "Kuudra Tools",
                "Kuudra 3D waypoints and hitboxes, Fresh Tools and build HUDs, phase titles, party join/limbo, party !commands including !t1-!t5, arrow tracker, terminator/fishing mutes, fishing-hook fix, and toggle left/right click. Parent off by default.",
                Group.KUUDRA,
                "Kuudra",
                false,
                true,
                true,
                List.of("kuudra", "party", "arrows", "toggle click"),
                setting("qol.iota.party_join_sound", "Party Join Sound", "Play a note when someone joins the party.", SettingType.TOGGLE),
                setting("qol.iota.limbo_alert", "Limbo Alert", "Party ping and dragon growl after the SkyBlock lobby kick line.", SettingType.TOGGLE),
                setting("qol.iota.fix_fishing_hook", "Fix Fishing Hook", "Ignore the extra armor-stand owner that sticks the bobber.", SettingType.TOGGLE),
                setting("qol.iota.mute_fishing_cast", "Mute Fishing Cast", "Mute the fishing-bobber throw sound.", SettingType.TOGGLE),
                setting("qol.iota.mute_terminator", "Mute Terminator Cooldown", "Mute enderman-teleport cooldown sound and the ability-cooldown chat line while holding Terminator.", SettingType.TOGGLE, "terminator"),
                setting("qol.iota.party_commands", "Party Commands", "Run party !commands from party chat.", SettingType.TOGGLE),
                setting("qol.iota.party_warp", "!warp / !w / !wp", "Send /party warp.", SettingType.TOGGLE),
                setting("qol.iota.party_transfer", "!pt / !ptme / !transfer", "Transfer party to the requester.", SettingType.TOGGLE),
                setting("qol.iota.party_ping", "!ping", "Reply with ping in party chat.", SettingType.TOGGLE),
                setting("qol.iota.party_allinvite", "!allinvite / !allinv / !invites", "Toggle party all-invite.", SettingType.TOGGLE),
                setting("qol.iota.party_tps", "!tps", "Reply with server TPS.", SettingType.TOGGLE),
                setting("qol.iota.party_promote", "!promote", "Promote the requester.", SettingType.TOGGLE),
                setting("qol.iota.party_kick", "!kick", "Kick the named player.", SettingType.TOGGLE),
                setting("qol.iota.party_kuudra", "!t1-!t5", "Join the matching Kuudra instance.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.party_chests", "!chests", "Reply with chest-limit progress.", SettingType.TOGGLE),
                setting("qol.iota.party_runs", "!runs", "Reply with session run stats.", SettingType.TOGGLE),
                setting("qol.iota.party_profit", "!profit", "Reply with session net using Bazaar/BIN quotes (sell-order, Mage key mats, 20% essence pet, salvage armor). Lore coins are the fallback if quotes are missing. Resets after 21 minutes idle.", SettingType.TOGGLE),
                setting("qol.iota.arrow_tracker", "Arrow Tracker", "Show quiver type and remaining arrows on HUD.", SettingType.TOGGLE),
                setting("qol.iota.arrow_notifications", "Arrow Notifications", "Title, sound, and party line when arrows run out.", SettingType.TOGGLE),
                setting("qol.iota.arrow_visibility", "Arrow Tracker Visibility", "Always keeps the last quiver reading. Only Shooting shows it while a bow is held.", SettingType.ENUM,
                        IotaPolicy.ARROW_VISIBILITY_OPTIONS),
                setting("qol.iota.auto_requeue", "Auto Requeue", "After Kuudra is defeated, join the same instance again. Serveri cheat.", SettingType.TOGGLE, "cheat"),
                setting("qol.iota.supply_waypoints", "Supply Waypoints", "Crate boxes, beams, and giant-derived crate positions during supplies.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.supply_hitbox", "Supply Hitboxes", "Zombie interaction boxes on supply crates. Green inside 3 blocks.", SettingType.TOGGLE),
                setting("qol.iota.supply_pull_circle", "Supply Pull Circle", "5-block pull circle at the crate. Turns green when the bobber is inside.", SettingType.TOGGLE),
                setting("qol.iota.supply_giant_hitbox", "Supply Giant Hitbox", "DOUBLE PEARL alert when you clip the supply giant while pulling.", SettingType.TOGGLE),
                setting("qol.iota.pile_waypoints", "Pile Waypoints", "Supply-pile cubes and beams from bundled pile locations.", SettingType.TOGGLE),
                setting("qol.iota.pile_names", "Pile Names", "Label remaining piles. No-pre pile uses the green color.", SettingType.TOGGLE),
                setting("qol.iota.pearl_waypoints", "Pearl Waypoints", "Pearl boxes, timers, and stand-block outline from bundled pearl waypoints.", SettingType.TOGGLE),
                setting("qol.iota.build_waypoints", "Build Overlay", "PROGRESS armor-stand cubes and beams during build.", SettingType.TOGGLE),
                setting("qol.iota.stun_waypoints", "Stun Waypoints", "Cannonball stun pod box. Offset from the enter position until you drop below Y 50.", SettingType.TOGGLE),
                setting("qol.iota.stun_pod", "Stun Pod", "Right, Left, or Back pod. Default is Left.", SettingType.ENUM,
                        IotaKuudraPolicy.STUN_POD_OPTIONS),
                setting("qol.iota.ichor_pool", "Ichor Pool", "20-second radius-8 circle from party ichor-pool chat coords.", SettingType.TOGGLE),
                setting("qol.iota.kuudra_hitbox", "Kuudra Hitbox", "Outline the size-30 magma cube with max health at least 10000.", SettingType.TOGGLE),
                setting("qol.iota.etherwarp_helper", "Etherwarp Helper", "Etherwarp boxes for the current Kuudra phase.", SettingType.TOGGLE),
                setting("qol.iota.fresh_tools", "Fresh Tools HUD", "10-second HUD countdown when your Fresh Tools perk procs. Off by default.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.fresh_announce", "Announce Fresh", "Send FRESH in party chat when your perk procs. Off by default.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.fresh_party", "Party Fresh HUD", "Show teammates who typed FRESH, with a 10-second countdown. Off by default.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.build_info", "Build Info HUD", "Show Kuudra build percent and helper count. Off by default.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.kuudra_titles", "Kuudra Titles", "Local titles on Kuudra phase changes and no-pre calls. Off by default.", SettingType.TOGGLE, "kuudra"),
                setting("qol.iota.toggle_left", "Toggle Left Click", "Latch left auto-click.", SettingType.KEYBIND, "left"),
                setting("qol.iota.toggle_right", "Toggle Right Click", "Latch right auto-click.", SettingType.KEYBIND, "right")));

        modules.add(module(
                "qol.stall_market",
                "Market Guard",
                "Bazaar search, sell protection, angry co-op AH guard, and BIN overlay/highlights. Local GUI only.",
                Group.UTILITIES,
                "Market",
                false,
                true,
                true,
                List.of("bazaar", "auction house", "bin", "sell protection"),
                setting("qol.stall_market.bazaar_search", "Bazaar Search", "Click the Search slot and fill the sign. /rot bazaarsearch also works.", SettingType.TOGGLE, "bazaar"),
                setting("qol.stall_market.sell_protection", "Sell Protection", "Block high-value Sell Instantly / Sell Sacks / Sell Inventory clicks. Hold Ctrl to override.", SettingType.TOGGLE),
                setting("qol.stall_market.sell_threshold", "Sell Protection Threshold", "Coin amount above which sells are blocked. Default 1m.", SettingType.NUMBER, "1m"),
                setting("qol.stall_market.angry_coop", "Angry Co-op Protection", "Block claiming co-op auctions and Claim All on Manage Auctions / Your Bids. Hold Ctrl to override.", SettingType.TOGGLE, "auction"),
                setting("qol.stall_market.bin_overlay", "BIN Overlay", "Highlight Buy/Confirm slots and show BIN status on BIN Auction View / Confirm Purchase.", SettingType.TOGGLE, "bin"),
                setting("qol.stall_market.ah_highlight", "Auction Highlights", "Tint AH listings cheaper or more expensive than the latest lowest BIN.", SettingType.TOGGLE, "auction house"),
                setting("qol.stall_market.search_keybind", "Search Hovered", "Search the bazaar for the hovered or held item name.", SettingType.KEYBIND)));

        modules.add(module(
                "qol.click_gui",
                "Click GUI",
                "Rot Client window: Right Shift opens it. The card switch is this module. Accent color uses the RGB picker. Open HUD Elements Editor opens the world editor with every enabled HUD.",
                Group.INTERFACE,
                "Dashboard",
                false,
                true,
                true,
                List.of("click gui", "gui", "right shift"),
                setting("qol.click_gui.chat_notifications", "Chat Notifications", "Local chat when you toggle a module or open Rot UI.", SettingType.TOGGLE),
                setting("qol.click_gui.color", "Accent Color", "Dashboard highlight color. Same RGB picker as HUD colors.", SettingType.COLOR),
                setting("qol.click_gui.rounded_bottoms", "Rounded Panel Bottoms", "Round the bottom corners of Rot Client panels.", SettingType.TOGGLE),
                setting("qol.click_gui.open_hud_editor", "Open HUD Elements Editor", "Open the global HUD editor with every enabled overlay.", SettingType.ACTION),
                setting("qol.click_gui.developer_message", "Developer Message", "Optional local debug line. Leave off unless you are testing.", SettingType.TOGGLE),
                setting("qol.click_gui.keybind", "Open UI Key", "Opens the Rot Client dashboard (Overview, modules, look, mining). Blank uses Right Shift.", SettingType.KEYBIND, "right shift")));

        return List.copyOf(modules);
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

    private static ModuleDef module(
            String id,
            String name,
            String description,
            Group group,
            boolean wip,
            boolean toggleable,
            boolean runtimeReady,
            List<String> aliases,
            SettingDef... settings) {
        return module(id, name, description, group, "", wip, toggleable, runtimeReady, aliases, settings);
    }

    private static ModuleDef module(
            String id,
            String name,
            String description,
            Group group,
            String section,
            boolean wip,
            boolean toggleable,
            boolean runtimeReady,
            List<String> aliases,
            SettingDef... settings) {
        return new ModuleDef(
                id, name, description, group, section, wip, toggleable, runtimeReady,
                List.of(settings), aliases);
    }

    private static SettingDef setting(
            String id,
            String label,
            String description,
            SettingType type,
            String... aliases) {
        return new SettingDef(id, label, description, type, List.of(), List.of(aliases));
    }

    private static SettingDef setting(
            String id,
            String label,
            String description,
            SettingType type,
            List<String> enumOptions,
            String... aliases) {
        return new SettingDef(id, label, description, type, enumOptions, List.of(aliases));
    }

    private static SettingDef dungeonClassSetting(String id, String label) {
        return setting(
                id,
                label,
                "Assigned F7 P3 class for this terminal or lever.",
                SettingType.ENUM,
                DungeonAthenPortPolicy.DUNGEON_CLASSES);
    }

    private static SettingDef section(String id, String label) {
        return new SettingDef(id, label, "", SettingType.SECTION, List.of(), List.of());
    }
}
