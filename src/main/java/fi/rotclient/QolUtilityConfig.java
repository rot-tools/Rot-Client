package fi.rotclient;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Persisted QoL utility toggles and drawer settings. Auto Sprint / Camera /
 * Fullbright enabled flags remain on TrackerConfig for runtime wiring and are
 * synced by the client dashboard layer.
 */
final class QolUtilityConfig {
    boolean commandKeybindsEnabled;
    boolean wardrobeKeybindsEnabled;
    boolean loadoutKeybindsEnabled;
    boolean chatCommandsEnabled;
    boolean noCursorResetEnabled;
    boolean playerDisplayEnabled;
    boolean petKeybindsEnabled;
    boolean slotBindsEnabled;
    boolean performanceHudEnabled;
    boolean renderOptimizerEnabled;
    boolean hidePlayersEnabled;
    boolean playerSizeEnabled;
    boolean etherwarpEnabled;
    boolean clickGuiEnabled = true;

    // Auto Clicker (Serveri)
    boolean autoClickerEnabled;
    boolean autoClickerCpsHudEnabled;
    boolean autoClickerWhitelistOnly;
    boolean autoClickerAllowBreaking;
    boolean autoClickerBlockBreaker = true;
    boolean autoClickerTerminatorOnly = true;
    float autoClickerCps = AutoClickerPolicy.DEFAULT_CPS;
    boolean autoClickerEnableLeft = true;
    boolean autoClickerEnableRight = true;
    float autoClickerLeftCps = AutoClickerPolicy.DEFAULT_CPS;
    float autoClickerRightCps = AutoClickerPolicy.DEFAULT_CPS;
    String autoClickerLeftKeybind = "";
    String autoClickerRightKeybind = "";
    java.util.List<String> autoClickerLeftWhitelist = new java.util.ArrayList<>();
    java.util.List<String> autoClickerRightWhitelist = new java.util.ArrayList<>();

    boolean inventoryWalkEnabled;
    int inventoryWalkPingMs = InventoryWalkPolicy.DEFAULT_PING_MS;

    boolean inventoryOverlayEnabled = true;
    boolean inventoryOverlayEquipment = true;
    boolean inventoryOverlayHideRecipeBook = true;
    boolean inventoryOverlayHideStatusEffects = true;
    boolean inventoryOverlayPetSlot = true;
    boolean inventoryOverlayProtectDrops = true;
    boolean inventoryOverlayProtectSalvage = true;
    String inventoryOverlayProtectList = "";
    int inventoryOverlayPetOffsetX;
    int inventoryOverlayPetOffsetY;
    int inventoryChromePanel;
    int inventoryChromeHeader;
    int inventoryChromeMain;
    int inventoryChromeHotbar;
    int inventoryChromeBorder;

    boolean skillLevelsEnabled = true;
    int skillLevelsColor = SkillLevelOverlayPolicy.DEFAULT_LEVEL_COLOR;
    int skillLevelsMaxColor = SkillLevelOverlayPolicy.DEFAULT_MAX_COLOR;

    boolean petHudEnabled = true;

    boolean nameHiderEnabled;
    String nameHiderMode = NameHiderPolicy.MODE_SCRAMBLE;
    String nameHiderCustomName = "";

    boolean trajectoriesEnabled;
    boolean trajectoriesBows = true;
    boolean trajectoriesPearls = true;
    boolean trajectoriesBoxes = true;
    boolean trajectoriesLines = true;
    boolean trajectoriesDepth = true;
    int trajectoriesRange = TrajectoryPredictor.DEFAULT_RANGE;
    float trajectoriesWidth = 1.0F;
    float trajectoriesBoxSize = 0.5F;
    int trajectoriesColor = 0xFF00AAAA;
    boolean trajectoriesPlane;
    boolean trajectoriesEntities = true;
    float trajectoriesPlaneSize = 2.0F;

    boolean secretHitboxesEnabled;
    boolean secretHitboxesOnlyDungeons = true;
    boolean secretHitboxesLever;
    boolean secretHitboxesOldLever = true;
    boolean secretHitboxesButton;
    boolean secretHitboxesFlatButton;
    boolean secretHitboxesSkull;
    boolean secretHitboxesChests;
    boolean secretHitboxesOnlyTrappedChests;

    boolean worldScannerEnabled;
    boolean worldScannerOnlyHollows = true;
    boolean worldScannerCrystals = true;
    boolean worldScannerMobSpots = true;
    boolean worldScannerFairyGrottos = true;
    boolean worldScannerDragonNest;
    boolean worldScannerWormFishing;
    boolean worldScannerLavaEsp;
    boolean worldScannerWaterEsp;
    boolean worldScannerRatHitboxes = true;
    boolean worldScannerChatCoords;
    int worldScannerEspRange = WorldScannerPolicy.DEFAULT_ESP_RANGE;
    java.util.Map<String, WorldScannerEspSettings.Target> worldScannerTargets =
            WorldScannerEspSettings.defaults();

    boolean autoConversationEnabled;
    boolean autoConversationMulti = true;
    boolean autoConversationGreen = true;
    int autoConversationDelayTicks = AutoConversationPolicy.DEFAULT_DELAY_TICKS;

    boolean fishingHelperEnabled;
    boolean fishingHelperAutoPull = true;
    int fishingHelperPullDelay = FishingHelperPolicy.DEFAULT_PULL_DELAY;
    int fishingHelperPullVariance;
    boolean fishingHelperRecast;
    boolean fishingHelperRecastCheck;
    int fishingHelperRecastDelay = FishingHelperPolicy.DEFAULT_RECAST_DELAY;
    int fishingHelperRecastVariance;
    boolean fishingHelperBobberTimer = true;
    boolean fishingHelperBiteTitle = true;
    boolean fishingHelperBiteSound = true;
    boolean fishingHelperHookTimerHud = true;
    boolean fishingHelperHideHookNametag;
    float fishingHudX = 12.0F;
    float fishingHudY = 148.0F;

    boolean missingEnchantsEnabled;
    String missingEnchantsKeybind = "LEFT_SHIFT";
    boolean missingEnchantsShowUpgradable;
    boolean missingEnchantsShowConflicting;

    boolean commissionDisplayEnabled;
    String commissionDisplayTitle = CommissionDisplayPolicy.DEFAULT_TITLE;
    String commissionDisplayNone = CommissionDisplayPolicy.DEFAULT_NONE;
    String commissionDisplayRow = CommissionDisplayPolicy.DEFAULT_ROW;
    boolean commissionDisplayColoredPercent = true;
    float commissionHudX = 12.0F;
    float commissionHudY = 80.0F;

    boolean etherwarpLeftClickWarp;
    boolean etherwarpShiftAutomatically = true;

    boolean itemRarityEnabled;
    String itemRarityStyle = ItemRarityPolicy.STYLE_FILLED_OUTLINE;
    boolean itemRarityHotbar = true;
    float itemRarityFillAlpha = ItemRarityPolicy.DEFAULT_FILL_ALPHA;
    float itemRarityOutlineAlpha = ItemRarityPolicy.DEFAULT_OUTLINE_ALPHA;
    int itemRarityCommon = ItemRarityPolicy.DEFAULT_COMMON;
    int itemRarityUncommon = ItemRarityPolicy.DEFAULT_UNCOMMON;
    int itemRarityRare = ItemRarityPolicy.DEFAULT_RARE;
    int itemRarityEpic = ItemRarityPolicy.DEFAULT_EPIC;
    int itemRarityLegendary = ItemRarityPolicy.DEFAULT_LEGENDARY;
    int itemRarityMythic = ItemRarityPolicy.DEFAULT_MYTHIC;
    int itemRarityDivine = ItemRarityPolicy.DEFAULT_DIVINE;
    int itemRaritySpecial = ItemRarityPolicy.DEFAULT_SPECIAL;

    boolean mobHighlightEnabled;
    boolean mobHighlightRequireKey;
    String mobHighlightAddKey = "";
    boolean mobHighlightDepth = true;
    boolean mobHighlightTracers = true;
    int mobHighlightColor = 0xFFFF55FF;
    java.util.List<String> mobHighlightNames = new java.util.ArrayList<>();

    boolean customTooltipEnabled;
    boolean customTooltipInfinite = true;
    boolean customTooltipHorizontal = true;
    String customTooltipHorizontalKey = "LEFT_SHIFT";
    int customTooltipHorizontalSpeed = 8;
    boolean customTooltipVertical = true;
    int customTooltipVerticalSpeed = 8;
    boolean customTooltipReset;
    String customTooltipStyle = CustomTooltipPolicy.STYLE_SEPARATED;
    boolean customTooltipCenteredHeader = true;
    boolean customTooltipBorder = true;
    int customTooltipBorderWidth = 1;
    boolean customTooltipRarityBorder = true;
    int customTooltipBorderColor = 0xFF55FFFF;
    boolean customTooltipBackground = true;
    int customTooltipBackgroundColor = 0xE0101018;
    String customTooltipOnlyNameKey = "";
    boolean customTooltipShadows = true;

    // Performance HUD
    int performanceNameColor = 0xFFB8C7D8;
    int performanceValueColor = 0xFFF3F7FB;
    String performanceDirection = "Horizontal";
    boolean performanceShowFps = true;
    boolean performanceShowTps = true;
    boolean performanceShowPing = true;
    String performanceKeybind = "";

    // Render Optimizer
    boolean hideFallingBlocks;
    boolean hideLightning;
    boolean hideExperienceOrbs;
    boolean hideDeathAnimation;
    boolean hideArmorStands;
    boolean hideExplosionParticles;
    boolean hideArcherPassive;
    boolean hideHealerFairy;
    boolean hideSoulWeaver;
    boolean hideTentacleHead;
    boolean hideFireOverlay;
    String renderOptimizerKeybind = "";

    // Hide Players
    boolean hidePlayersOnlyDungeons;
    boolean hidePlayersHideAll;
    double hidePlayersDistance = 32.0D;
    String hidePlayersKeybind = "";

    // Player Size
    float playerSizeX = 1.0F;
    float playerSizeY = 1.0F;
    float playerSizeZ = 1.0F;
    String playerSizeKeybind = "";
    boolean playerAnimalsEnabled;
    String playerAnimalsScope = "Self";
    String playerAnimalsSpecies = "Cow";
    boolean playerAnimalsBaby;
    int playerAnimalsCollarColor = 0xFFFF5555;

    // Etherwarp
    boolean etherwarpShowGuess = true;
    int etherwarpColor = 0xFF35D167;
    boolean etherwarpShowFailed;
    int etherwarpFailColor = 0xFFFF5B5B;
    String etherwarpRenderStyle = "Outline";
    boolean etherwarpUseServerPosition = true;
    boolean etherwarpFullBlock = true;
    boolean etherwarpDepth = true;
    boolean etherwarpSounds = true;
    String etherwarpKeybind = "";

    // Click GUI
    boolean clickGuiChatNotifications = true;
    int clickGuiColor = 0xFFE11D48;
    boolean clickGuiRoundedBottoms = true;
    boolean clickGuiDeveloperMessage;
    String clickGuiKeybind = "";

    // Command keybinds (blank = unbound)
    String commandPetsKey = "";
    String commandStorageKey = "";
    String commandArmorWardrobeKey = "";
    String commandEquipWardrobeKey = "";
    String commandLoadoutsKey = "";
    String commandStatsKey = "";
    String commandDungeonHubKey = "";
    String commandPotionBagKey = "";
    String commandBindMacros = "";
    String commandBindSendMode = RingPolicy.MODE_SEND;
    String commandBindConflict = RingPolicy.STRATEGY_ASSERT;
    String commandBindActivation = RingPolicy.ACTIVATION_HOLD;
    int commandBindRatelimitCount = RingPolicy.DEFAULT_RATELIMIT_COUNT;
    int commandBindRatelimitTicks = RingPolicy.DEFAULT_RATELIMIT_TICKS;
    boolean commandBindRatelimitStrict;
    boolean commandBindRatelimitSp;
    boolean commandBindUseRatelimit = true;
    int commandBindLengthLimit = RingPolicy.DEFAULT_LENGTH_LIMIT;
    boolean commandBindAddHistory;
    boolean commandBindShowHud;

    // Wardrobe
    String wardrobeNextKey = "RIGHT";
    String wardrobePreviousKey = "LEFT";
    String wardrobeUnequipKey = "";
    boolean wardrobeDisableUnequip;
    boolean wardrobeCancelAll;
    String wardrobeOverrideKey = "LEFT_CONTROL";
    boolean wardrobeCancelRender;
    int wardrobePingMs = WardrobeKeybindPolicy.DEFAULT_PING_MS;
    boolean wardrobeUseHotbar = true;
    String wardrobeKeybindStyle = QolSkyblockExtras.STYLE_HOTBAR;
    boolean wardrobeSound;
    String wardrobeCustom1 = "";
    String wardrobeCustom2 = "";
    String wardrobeCustom3 = "";
    String wardrobeCustom4 = "";
    String wardrobeCustom5 = "";
    String wardrobeCustom6 = "";
    String wardrobeCustom7 = "";
    String wardrobeCustom8 = "";
    String wardrobeCustom9 = "";
    boolean wardrobeSwap;
    String wardrobeSwapKey = "";
    int wardrobeSwapSlotA = 1;
    int wardrobeSwapSlotB = 2;
    boolean wardrobeAutoClose;
    boolean wardrobeAutoEquip;
    boolean wardrobeMoveEquip;
    boolean wardrobeResetOpen = true;
    int wardrobeClickDelay = WardrobeKeybindPolicy.DEFAULT_CLICK_DELAY;
    int wardrobeCloseDelay = WardrobeKeybindPolicy.DEFAULT_CLOSE_DELAY;
    int wardrobeDelayVariance = 1;
    float wardrobeHudX = 12.0F;
    float wardrobeHudY = 48.0F;

    // Loadout
    String loadoutNextKey = "RIGHT";
    String loadoutPreviousKey = "LEFT";

    // Chat commands
    boolean chatEmotes = true;
    boolean chatPartyCommands = true;
    boolean chatGuildCommands = true;
    boolean chatPrivateCommands = true;
    boolean chatPreviousServer = true;
    int chatPreviousServerSeconds = SkyblockFlavorPolicy.DEFAULT_PREVIOUS_SERVER_SECONDS;
    boolean chatQueueEstimate = true;
    boolean chatQuickJoin;
    boolean chatSlotMachine = true;
    String chatQuickJoinText = SkyblockFlavorPolicy.DEFAULT_QUICK_JOIN_TEXT;
    String chatQuickJoinIp = SkyblockFlavorPolicy.DEFAULT_QUICK_JOIN_IP;
    String chatCommandsShortcuts = "";
    String chatCommandsRules = "";
    String chatCommandsKeybind = "";

    // No cursor reset
    int noCursorUnhookTimeoutMs = 150;
    String noCursorKeybind = "";

    // Player display
    boolean playerDisplayShowIcons = true;
    boolean playerDisplayShowLabels = true;
    boolean playerDisplayShowMax = true;
    boolean playerDisplayHealthHud = true;
    boolean playerDisplayManaHud = true;
    boolean playerDisplayOverflowManaHud = true;
    boolean playerDisplayDefenseHud = true;
    boolean playerDisplayVitalityHud;
    boolean playerDisplayEhpHud;
    boolean playerDisplaySpeedHud = true;
    int playerDisplayHealthColor = 0xFFFF5B5B;
    int playerDisplayManaColor = 0xFF4C9EFF;
    int playerDisplayOverflowManaColor = 0xFF7AD7FF;
    int playerDisplayDefenseColor = 0xFF35D167;
    int playerDisplayVitalityColor = 0xFFFFC857;
    int playerDisplayEhpColor = 0xFFE33B3B;
    int playerDisplaySpeedColor = 0xFFF6ECEC;
    String playerDisplayKeybind = "";

    // Pet keybinds
    String petUnequipKey = "";
    String petNextKey = "";
    String petPreviousKey = "";
    boolean petDisableUnequip;
    boolean petCloseIfAlreadyEquipped;
    String petModuleKeybind = "";

    // Slot binds
    String slotBindSetKey = "";
    int slotBindColor = 0xFFE11D48;
    float slotBindLineWidth = 0.5F;
    String slotBindLineDisplay = "Hover";
    String slotBindProfile = "Profile 1";
    Map<String, Map<String, Integer>> slotBindProfiles = new LinkedHashMap<>();

    boolean waypointsEnabled;
    boolean waypointsFromParty = true;
    boolean waypointsFromAll;
    boolean waypointsPersonal;
    String waypointsPingMode = WaypointPolicy.PING_OFF;
    String waypointsKeybind = "";

    // Shared optional keybinds for runtime-ready modules
    String autoSprintKeybind = "";
    String cameraKeybind = "";

    // Movable QoL HUD poses (screen pixels)
    float performanceHudX = 12.0F;
    float performanceHudY = 220.0F;
    float performanceHudScale = 1.0F;
    float healthHudX = 12.0F;
    float healthHudY = 250.0F;
    float manaHudX = 12.0F;
    float manaHudY = 268.0F;
    float overflowManaHudX = 12.0F;
    float overflowManaHudY = 286.0F;
    float defenseHudX = 12.0F;
    float defenseHudY = 304.0F;
    float vitalityHudX = 12.0F;
    float vitalityHudY = 322.0F;
    float ehpHudX = 12.0F;
    float ehpHudY = 340.0F;
    float speedHudX = 12.0F;
    float speedHudY = 358.0F;
    float petHudX = 12.0F;
    float petHudY = 80.0F;
    float slayerHudX = 12.0F;
    float slayerHudY = 400.0F;
    float slayerProgressHudX = 172.0F;
    float slayerProgressHudY = 468.0F;
    float slayerRngHudX = 332.0F;
    float slayerRngHudY = 468.0F;
    float slayerProfitHudX = 492.0F;
    float slayerProfitHudY = 468.0F;
    float slayerStatsHudX = 172.0F;
    float slayerStatsHudY = 400.0F;
    float slayerCarryHudX = 332.0F;
    float slayerCarryHudY = 400.0F;
    float slayerCocoonHudX = 492.0F;
    float slayerCocoonHudY = 400.0F;
    float slayerAttunementHudX = 652.0F;
    float slayerAttunementHudY = 400.0F;
    float slayerVengeanceHudX = 652.0F;
    float slayerVengeanceHudY = 436.0F;
    float autoClickerHudX = 12.0F;
    float autoClickerHudY = 382.0F;
    float dungeonHudX = 172.0F;
    float dungeonHudY = 12.0F;

    QolSkyblockExtras extras = new QolSkyblockExtras();

    QolSkyblockExtras extras() {
        if (extras == null) {
            extras = new QolSkyblockExtras();
        }
        return extras;
    }

    // Player Display hide settings (local render only)
    boolean playerDisplayHideVanillaHealth;
    boolean playerDisplayHideVanillaFood;
    boolean playerDisplayHideVanillaArmor;
    boolean playerDisplayHideVanillaXp;
    boolean playerDisplayHideActionHealth;
    boolean playerDisplayHideActionDefense;
    boolean playerDisplayHideActionMana;
    boolean playerDisplayHideActionOverflow;
    boolean playerDisplayHideActionSpeed;
    boolean playerDisplayHideActionVitality;
    boolean playerDisplayHideActionLocation = true;

    boolean isModuleEnabled(String moduleId) {
        if (moduleId == null) {
            return false;
        }
        return switch (moduleId) {
            case "qol.command_keybinds" -> commandKeybindsEnabled;
            case "qol.wardrobe_keybinds" -> wardrobeKeybindsEnabled;
            case "qol.loadout_keybinds" -> loadoutKeybindsEnabled;
            case "qol.chat_commands" -> chatCommandsEnabled;
            case "qol.no_cursor_reset" -> noCursorResetEnabled;
            case "qol.player_display" -> playerDisplayEnabled;
            case "qol.pet_keybinds" -> petKeybindsEnabled;
            case "qol.slot_binds" -> slotBindsEnabled;
            case "qol.waypoints" -> waypointsEnabled;
            case "qol.performance_hud" -> performanceHudEnabled;
            case "qol.render_optimizer" -> renderOptimizerEnabled;
            case "qol.hide_players" -> hidePlayersEnabled;
            case "qol.player_size" -> playerSizeEnabled;
            case "qol.etherwarp" -> etherwarpEnabled;
            case "qol.click_gui" -> clickGuiEnabled;
            case "qol.auto_clicker" -> autoClickerEnabled;
            case "qol.inventory_walk" -> inventoryWalkEnabled;
            case "qol.inventory_overlay" -> inventoryOverlayEnabled;
            case "qol.skill_levels" -> skillLevelsEnabled;
            case "qol.pet_hud" -> petHudEnabled;
            case "qol.name_hider" -> nameHiderEnabled;
            case "qol.trajectories" -> trajectoriesEnabled;
            case "qol.secret_hitboxes" -> secretHitboxesEnabled;
            case "qol.world_scanner" -> worldScannerEnabled;
            case "qol.auto_conversation" -> autoConversationEnabled;
            case "qol.fishing_helper" -> fishingHelperEnabled;
            case "qol.item_tooltips" ->
                    missingEnchantsEnabled
                            || customTooltipEnabled
                            || extras().isModuleEnabled("qol.info_tooltips")
                            || extras().isModuleEnabled("qol.price_tooltips");
            case "qol.missing_enchants" -> missingEnchantsEnabled;
            case "qol.commission_display" -> commissionDisplayEnabled;
            case "qol.item_rarity" -> itemRarityEnabled;
            case "qol.mob_highlight" -> mobHighlightEnabled;
            case "qol.custom_tooltip" -> customTooltipEnabled;
            case "qol.fullbright", "qol.auto_sprint", "qol.camera" -> false;
            default -> extras().isModuleEnabled(moduleId);
        };
    }

    void setModuleEnabled(String moduleId, boolean enabled) {
        if (moduleId == null) {
            return;
        }
        switch (moduleId) {
            case "qol.command_keybinds" -> commandKeybindsEnabled = enabled;
            case "qol.wardrobe_keybinds" -> wardrobeKeybindsEnabled = enabled;
            case "qol.loadout_keybinds" -> loadoutKeybindsEnabled = enabled;
            case "qol.chat_commands" -> chatCommandsEnabled = enabled;
            case "qol.no_cursor_reset" -> noCursorResetEnabled = enabled;
            case "qol.player_display" -> playerDisplayEnabled = enabled;
            case "qol.pet_keybinds" -> petKeybindsEnabled = enabled;
            case "qol.slot_binds" -> slotBindsEnabled = enabled;
            case "qol.waypoints" -> waypointsEnabled = enabled;
            case "qol.performance_hud" -> performanceHudEnabled = enabled;
            case "qol.render_optimizer" -> renderOptimizerEnabled = enabled;
            case "qol.hide_players" -> hidePlayersEnabled = enabled;
            case "qol.player_size" -> playerSizeEnabled = enabled;
            case "qol.etherwarp" -> etherwarpEnabled = enabled;
            case "qol.click_gui" -> clickGuiEnabled = enabled;
            case "qol.auto_clicker" -> autoClickerEnabled = enabled;
            case "qol.inventory_walk" -> inventoryWalkEnabled = enabled;
            case "qol.inventory_overlay" -> inventoryOverlayEnabled = enabled;
            case "qol.skill_levels" -> skillLevelsEnabled = enabled;
            case "qol.pet_hud" -> petHudEnabled = enabled;
            case "qol.name_hider" -> nameHiderEnabled = enabled;
            case "qol.trajectories" -> trajectoriesEnabled = enabled;
            case "qol.secret_hitboxes" -> secretHitboxesEnabled = enabled;
            case "qol.world_scanner" -> worldScannerEnabled = enabled;
            case "qol.auto_conversation" -> autoConversationEnabled = enabled;
            case "qol.fishing_helper" -> fishingHelperEnabled = enabled;
            case "qol.item_tooltips" -> {
                missingEnchantsEnabled = enabled;
                customTooltipEnabled = enabled;
                extras().setModuleEnabled("qol.info_tooltips", enabled);
                extras().setModuleEnabled("qol.price_tooltips", enabled);
            }
            case "qol.missing_enchants" -> missingEnchantsEnabled = enabled;
            case "qol.commission_display" -> commissionDisplayEnabled = enabled;
            case "qol.item_rarity" -> itemRarityEnabled = enabled;
            case "qol.mob_highlight" -> mobHighlightEnabled = enabled;
            case "qol.custom_tooltip" -> customTooltipEnabled = enabled;
            default -> extras().setModuleEnabled(moduleId, enabled);
        }
    }

    boolean toggleBooleanSetting(String settingId) {
        Boolean current = readBoolean(settingId);
        if (current == null) {
            return false;
        }
        writeBoolean(settingId, !current);
        return true;
    }

    Boolean readBoolean(String settingId) {
        if (settingId == null) {
            return null;
        }
        Boolean target = readWorldScannerTargetBoolean(settingId);
        if (target != null) {
            return target;
        }
        return switch (settingId) {
            case "qol.render_optimizer.hide_falling_blocks" -> hideFallingBlocks;
            case "qol.render_optimizer.hide_lightning" -> hideLightning;
            case "qol.render_optimizer.hide_xp_orbs" -> hideExperienceOrbs;
            case "qol.render_optimizer.hide_death_animation" -> hideDeathAnimation;
            case "qol.render_optimizer.hide_armor_stands" -> hideArmorStands;
            case "qol.render_optimizer.hide_explosion_particles" -> hideExplosionParticles;
            case "qol.render_optimizer.hide_archer_passive" -> hideArcherPassive;
            case "qol.render_optimizer.hide_healer_fairy" -> hideHealerFairy;
            case "qol.render_optimizer.hide_soul_weaver" -> hideSoulWeaver;
            case "qol.render_optimizer.hide_tentacle_head" -> hideTentacleHead;
            case "qol.render_optimizer.hide_fire_overlay" -> hideFireOverlay;
            case "qol.performance_hud.show_fps" -> performanceShowFps;
            case "qol.performance_hud.show_tps" -> performanceShowTps;
            case "qol.performance_hud.show_ping" -> performanceShowPing;
            case "qol.hide_players.only_dungeons" -> hidePlayersOnlyDungeons;
            case "qol.hide_players.hide_all" -> hidePlayersHideAll;
            case "qol.player_size.player_animals" -> playerAnimalsEnabled;
            case "qol.player_size.player_animals_baby" -> playerAnimalsBaby;
            case "qol.etherwarp.show_guess" -> etherwarpShowGuess;
            case "qol.etherwarp.show_failed" -> etherwarpShowFailed;
            case "qol.etherwarp.use_server_position" -> etherwarpUseServerPosition;
            case "qol.etherwarp.full_block" -> etherwarpFullBlock;
            case "qol.etherwarp.depth" -> etherwarpDepth;
            case "qol.etherwarp.sounds" -> etherwarpSounds;
            case "qol.click_gui.chat_notifications" -> clickGuiChatNotifications;
            case "qol.click_gui.rounded_bottoms" -> clickGuiRoundedBottoms;
            case "qol.click_gui.developer_message" -> clickGuiDeveloperMessage;
            case "qol.auto_clicker.whitelist_only" -> autoClickerWhitelistOnly;
            case "qol.auto_clicker.cps_hud" -> autoClickerCpsHudEnabled;
            case "qol.auto_clicker.allow_breaking" -> autoClickerAllowBreaking;
            case "qol.auto_clicker.block_breaker" -> autoClickerBlockBreaker;
            case "qol.auto_clicker.terminator_only" -> autoClickerTerminatorOnly;
            case "qol.auto_clicker.enable_left" -> autoClickerEnableLeft;
            case "qol.auto_clicker.enable_right" -> autoClickerEnableRight;
            case "qol.trajectories.bows" -> trajectoriesBows;
            case "qol.trajectories.pearls" -> trajectoriesPearls;
            case "qol.trajectories.lines" -> trajectoriesLines;
            case "qol.trajectories.boxes" -> trajectoriesBoxes;
            case "qol.trajectories.depth" -> trajectoriesDepth;
            case "qol.trajectories.plane" -> trajectoriesPlane;
            case "qol.trajectories.entities" -> trajectoriesEntities;
            case "qol.secret_hitboxes.only_dungeons" -> secretHitboxesOnlyDungeons;
            case "qol.secret_hitboxes.lever" -> secretHitboxesLever;
            case "qol.secret_hitboxes.old_lever" -> secretHitboxesOldLever;
            case "qol.secret_hitboxes.button" -> secretHitboxesButton;
            case "qol.secret_hitboxes.flat_button" -> secretHitboxesFlatButton;
            case "qol.secret_hitboxes.skull" -> secretHitboxesSkull;
            case "qol.secret_hitboxes.chests" -> secretHitboxesChests;
            case "qol.secret_hitboxes.only_trapped" -> secretHitboxesOnlyTrappedChests;
            case "qol.world_scanner.only_hollows" -> worldScannerOnlyHollows;
            case "qol.world_scanner.crystals" -> worldScannerCrystals;
            case "qol.world_scanner.mob_spots" -> worldScannerMobSpots;
            case "qol.world_scanner.fairy" -> worldScannerFairyGrottos;
            case "qol.world_scanner.dragon" -> worldScannerDragonNest;
            case "qol.world_scanner.worm" -> worldScannerWormFishing;
            case "qol.world_scanner.lava_esp" -> worldScannerLavaEsp;
            case "qol.world_scanner.water_esp" -> worldScannerWaterEsp;
            case "qol.world_scanner.rat_hitboxes" -> worldScannerRatHitboxes;
            case "qol.world_scanner.chat" -> worldScannerChatCoords;
            case "qol.wardrobe_keybinds.disable_unequip" -> wardrobeDisableUnequip;
            case "qol.wardrobe_keybinds.cancel_all" -> wardrobeCancelAll;
            case "qol.wardrobe_keybinds.cancel_render" -> wardrobeCancelRender;
            case "qol.wardrobe_keybinds.use_hotbar" -> wardrobeUseHotbar;
            case "qol.wardrobe_keybinds.swap" -> wardrobeSwap;
            case "qol.wardrobe_keybinds.auto_close" -> wardrobeAutoClose;
            case "qol.wardrobe_keybinds.auto_equip" -> wardrobeAutoEquip;
            case "qol.wardrobe_keybinds.move_equip" -> wardrobeMoveEquip;
            case "qol.wardrobe_keybinds.reset_open" -> wardrobeResetOpen;
            case "qol.chat_commands.emotes" -> chatEmotes;
            case "qol.chat_commands.party" -> chatPartyCommands;
            case "qol.chat_commands.guild" -> chatGuildCommands;
            case "qol.chat_commands.private" -> chatPrivateCommands;
            case "qol.chat_commands.previous_server" -> chatPreviousServer;
            case "qol.chat_commands.queue_estimate" -> chatQueueEstimate;
            case "qol.chat_commands.quick_join" -> chatQuickJoin;
            case "qol.chat_commands.slot_machine" -> chatSlotMachine;
            case "qol.waypoints.from_party" -> waypointsFromParty;
            case "qol.waypoints.from_all" -> waypointsFromAll;
            case "qol.waypoints.personal" -> waypointsPersonal;
            case "qol.player_display.show_icons" -> playerDisplayShowIcons;
            case "qol.player_display.show_labels" -> playerDisplayShowLabels;
            case "qol.player_display.show_max" -> playerDisplayShowMax;
            case "qol.player_display.health_hud" -> playerDisplayHealthHud;
            case "qol.player_display.mana_hud" -> playerDisplayManaHud;
            case "qol.player_display.overflow_mana_hud" -> playerDisplayOverflowManaHud;
            case "qol.player_display.defense_hud" -> playerDisplayDefenseHud;
            case "qol.player_display.vitality_hud" -> playerDisplayVitalityHud;
            case "qol.player_display.ehp_hud" -> playerDisplayEhpHud;
            case "qol.player_display.speed_hud" -> playerDisplaySpeedHud;
            case "qol.pet_keybinds.disable_unequip" -> petDisableUnequip;
            case "qol.pet_keybinds.close_if_equipped" -> petCloseIfAlreadyEquipped;
            case "qol.inventory_overlay.equipment" -> inventoryOverlayEquipment;
            case "qol.inventory_overlay.hide_recipe_book" -> inventoryOverlayHideRecipeBook;
            case "qol.inventory_overlay.hide_status_effects" -> inventoryOverlayHideStatusEffects;
            case "qol.inventory_overlay.pet_slot" -> inventoryOverlayPetSlot;
            case "qol.inventory_overlay.protect_drops" -> inventoryOverlayProtectDrops;
            case "qol.inventory_overlay.protect_salvage" -> inventoryOverlayProtectSalvage;
            case "qol.player_display.hide_vanilla_health" -> playerDisplayHideVanillaHealth;
            case "qol.player_display.hide_vanilla_food" -> playerDisplayHideVanillaFood;
            case "qol.player_display.hide_vanilla_armor" -> playerDisplayHideVanillaArmor;
            case "qol.player_display.hide_vanilla_xp" -> playerDisplayHideVanillaXp;
            case "qol.player_display.hide_action_health" -> playerDisplayHideActionHealth;
            case "qol.player_display.hide_action_defense" -> playerDisplayHideActionDefense;
            case "qol.player_display.hide_action_mana" -> playerDisplayHideActionMana;
            case "qol.player_display.hide_action_overflow" -> playerDisplayHideActionOverflow;
            case "qol.player_display.hide_action_speed" -> playerDisplayHideActionSpeed;
            case "qol.player_display.hide_action_vitality" -> playerDisplayHideActionVitality;
            case "qol.player_display.hide_action_location" -> playerDisplayHideActionLocation;
            case "qol.auto_conversation.multi" -> autoConversationMulti;
            case "qol.auto_conversation.green" -> autoConversationGreen;
            case "qol.fishing_helper.auto_pull" -> fishingHelperAutoPull;
            case "qol.fishing_helper.recast" -> fishingHelperRecast;
            case "qol.fishing_helper.recast_check" -> fishingHelperRecastCheck;
            case "qol.fishing_helper.bobber_timer" -> fishingHelperBobberTimer;
            case "qol.fishing_helper.bite_title" -> fishingHelperBiteTitle;
            case "qol.fishing_helper.bite_sound" -> fishingHelperBiteSound;
            case "qol.fishing_helper.hook_timer_hud" -> fishingHelperHookTimerHud;
            case "qol.fishing_helper.hide_hook_nametag" -> fishingHelperHideHookNametag;
            case "qol.commission_display.colored_percent" -> commissionDisplayColoredPercent;
            case "qol.etherwarp.left_click_warp" -> etherwarpLeftClickWarp;
            case "qol.etherwarp.shift_automatically" -> etherwarpShiftAutomatically;
            case "qol.item_rarity.hotbar" -> itemRarityHotbar;
            case "qol.mob_highlight.highlight_key" -> mobHighlightRequireKey;
            case "qol.mob_highlight.depth" -> mobHighlightDepth;
            case "qol.mob_highlight.tracers" -> mobHighlightTracers;
            case "qol.custom_tooltip.infinite" -> customTooltipInfinite;
            case "qol.custom_tooltip.horizontal" -> customTooltipHorizontal;
            case "qol.custom_tooltip.vertical" -> customTooltipVertical;
            case "qol.custom_tooltip.reset" -> customTooltipReset;
            case "qol.custom_tooltip.centered_header" -> customTooltipCenteredHeader;
            case "qol.custom_tooltip.border" -> customTooltipBorder;
            case "qol.custom_tooltip.rarity_border" -> customTooltipRarityBorder;
            case "qol.custom_tooltip.background" -> customTooltipBackground;
            case "qol.custom_tooltip.shadows" -> customTooltipShadows;
            case "qol.missing_enchants.show_upgradable" -> missingEnchantsShowUpgradable;
            case "qol.missing_enchants.show_conflicting" -> missingEnchantsShowConflicting;
            case "qol.wardrobe_keybinds.sound" -> wardrobeSound;
            case "qol.command_keybinds.ratelimit_strict" -> commandBindRatelimitStrict;
            case "qol.command_keybinds.ratelimit_sp" -> commandBindRatelimitSp;
            case "qol.command_keybinds.use_ratelimit" -> commandBindUseRatelimit;
            case "qol.command_keybinds.add_history" -> commandBindAddHistory;
            case "qol.command_keybinds.show_hud" -> commandBindShowHud;
            case "qol.item_tooltips.missing" -> missingEnchantsEnabled;
            case "qol.item_tooltips.style" -> customTooltipEnabled;
            case "qol.item_tooltips.info" -> extras().isModuleEnabled("qol.info_tooltips");
            case "qol.item_tooltips.prices" -> extras().isModuleEnabled("qol.price_tooltips");
            default -> extras().readBoolean(settingId);
        };
    }

    void writeBoolean(String settingId, boolean value) {
        if (settingId == null) {
            return;
        }
        if (writeWorldScannerTargetBoolean(settingId, value)) {
            return;
        }
        switch (settingId) {
            case "qol.render_optimizer.hide_falling_blocks" -> hideFallingBlocks = value;
            case "qol.render_optimizer.hide_lightning" -> hideLightning = value;
            case "qol.render_optimizer.hide_xp_orbs" -> hideExperienceOrbs = value;
            case "qol.render_optimizer.hide_death_animation" -> hideDeathAnimation = value;
            case "qol.render_optimizer.hide_armor_stands" -> hideArmorStands = value;
            case "qol.render_optimizer.hide_explosion_particles" -> hideExplosionParticles = value;
            case "qol.render_optimizer.hide_archer_passive" -> hideArcherPassive = value;
            case "qol.render_optimizer.hide_healer_fairy" -> hideHealerFairy = value;
            case "qol.render_optimizer.hide_soul_weaver" -> hideSoulWeaver = value;
            case "qol.render_optimizer.hide_tentacle_head" -> hideTentacleHead = value;
            case "qol.render_optimizer.hide_fire_overlay" -> hideFireOverlay = value;
            case "qol.performance_hud.show_fps" -> performanceShowFps = value;
            case "qol.performance_hud.show_tps" -> performanceShowTps = value;
            case "qol.performance_hud.show_ping" -> performanceShowPing = value;
            case "qol.hide_players.only_dungeons" -> hidePlayersOnlyDungeons = value;
            case "qol.hide_players.hide_all" -> hidePlayersHideAll = value;
            case "qol.player_size.player_animals" -> playerAnimalsEnabled = value;
            case "qol.player_size.player_animals_baby" -> playerAnimalsBaby = value;
            case "qol.etherwarp.show_guess" -> etherwarpShowGuess = value;
            case "qol.etherwarp.show_failed" -> etherwarpShowFailed = value;
            case "qol.etherwarp.use_server_position" -> etherwarpUseServerPosition = value;
            case "qol.etherwarp.full_block" -> etherwarpFullBlock = value;
            case "qol.etherwarp.depth" -> etherwarpDepth = value;
            case "qol.etherwarp.sounds" -> etherwarpSounds = value;
            case "qol.etherwarp.left_click_warp" -> etherwarpLeftClickWarp = value;
            case "qol.etherwarp.shift_automatically" -> etherwarpShiftAutomatically = value;
            case "qol.click_gui.chat_notifications" -> clickGuiChatNotifications = value;
            case "qol.click_gui.rounded_bottoms" -> clickGuiRoundedBottoms = value;
            case "qol.click_gui.developer_message" -> clickGuiDeveloperMessage = value;
            case "qol.auto_clicker.whitelist_only" -> autoClickerWhitelistOnly = value;
            case "qol.auto_clicker.cps_hud" -> autoClickerCpsHudEnabled = value;
            case "qol.auto_clicker.allow_breaking" -> autoClickerAllowBreaking = value;
            case "qol.auto_clicker.block_breaker" -> autoClickerBlockBreaker = value;
            case "qol.auto_clicker.terminator_only" -> autoClickerTerminatorOnly = value;
            case "qol.auto_clicker.enable_left" -> autoClickerEnableLeft = value;
            case "qol.auto_clicker.enable_right" -> autoClickerEnableRight = value;
            case "qol.trajectories.bows" -> trajectoriesBows = value;
            case "qol.trajectories.pearls" -> trajectoriesPearls = value;
            case "qol.trajectories.lines" -> trajectoriesLines = value;
            case "qol.trajectories.boxes" -> trajectoriesBoxes = value;
            case "qol.trajectories.depth" -> trajectoriesDepth = value;
            case "qol.trajectories.plane" -> trajectoriesPlane = value;
            case "qol.trajectories.entities" -> trajectoriesEntities = value;
            case "qol.secret_hitboxes.only_dungeons" -> secretHitboxesOnlyDungeons = value;
            case "qol.secret_hitboxes.lever" -> secretHitboxesLever = value;
            case "qol.secret_hitboxes.old_lever" -> secretHitboxesOldLever = value;
            case "qol.secret_hitboxes.button" -> secretHitboxesButton = value;
            case "qol.secret_hitboxes.flat_button" -> secretHitboxesFlatButton = value;
            case "qol.secret_hitboxes.skull" -> secretHitboxesSkull = value;
            case "qol.secret_hitboxes.chests" -> secretHitboxesChests = value;
            case "qol.secret_hitboxes.only_trapped" -> secretHitboxesOnlyTrappedChests = value;
            case "qol.world_scanner.only_hollows" -> worldScannerOnlyHollows = value;
            case "qol.world_scanner.crystals" -> worldScannerCrystals = value;
            case "qol.world_scanner.mob_spots" -> worldScannerMobSpots = value;
            case "qol.world_scanner.fairy" -> worldScannerFairyGrottos = value;
            case "qol.world_scanner.dragon" -> worldScannerDragonNest = value;
            case "qol.world_scanner.worm" -> worldScannerWormFishing = value;
            case "qol.world_scanner.lava_esp" -> worldScannerLavaEsp = value;
            case "qol.world_scanner.water_esp" -> worldScannerWaterEsp = value;
            case "qol.world_scanner.rat_hitboxes" -> worldScannerRatHitboxes = value;
            case "qol.world_scanner.chat" -> worldScannerChatCoords = value;
            case "qol.wardrobe_keybinds.disable_unequip" -> wardrobeDisableUnequip = value;
            case "qol.wardrobe_keybinds.cancel_all" -> wardrobeCancelAll = value;
            case "qol.wardrobe_keybinds.cancel_render" -> wardrobeCancelRender = value;
            case "qol.wardrobe_keybinds.use_hotbar" -> {
                wardrobeUseHotbar = value;
                if (value) {
                    wardrobeKeybindStyle = QolSkyblockExtras.STYLE_HOTBAR;
                } else if (QolSkyblockExtras.STYLE_HOTBAR.equals(
                        QolSkyblockExtras.normalizeStyle(wardrobeKeybindStyle))) {
                    wardrobeKeybindStyle = QolSkyblockExtras.STYLE_SIMPLE;
                }
            }
            case "qol.wardrobe_keybinds.swap" -> wardrobeSwap = value;
            case "qol.wardrobe_keybinds.auto_close" -> wardrobeAutoClose = value;
            case "qol.wardrobe_keybinds.auto_equip" -> wardrobeAutoEquip = value;
            case "qol.wardrobe_keybinds.move_equip" -> wardrobeMoveEquip = value;
            case "qol.wardrobe_keybinds.reset_open" -> wardrobeResetOpen = value;
            case "qol.chat_commands.emotes" -> chatEmotes = value;
            case "qol.chat_commands.party" -> chatPartyCommands = value;
            case "qol.chat_commands.guild" -> chatGuildCommands = value;
            case "qol.chat_commands.private" -> chatPrivateCommands = value;
            case "qol.chat_commands.previous_server" -> chatPreviousServer = value;
            case "qol.chat_commands.queue_estimate" -> chatQueueEstimate = value;
            case "qol.chat_commands.quick_join" -> chatQuickJoin = value;
            case "qol.chat_commands.slot_machine" -> chatSlotMachine = value;
            case "qol.waypoints.from_party" -> waypointsFromParty = value;
            case "qol.waypoints.from_all" -> waypointsFromAll = value;
            case "qol.waypoints.personal" -> waypointsPersonal = value;
            case "qol.player_display.show_icons" -> playerDisplayShowIcons = value;
            case "qol.player_display.show_labels" -> playerDisplayShowLabels = value;
            case "qol.player_display.show_max" -> playerDisplayShowMax = value;
            case "qol.player_display.health_hud" -> playerDisplayHealthHud = value;
            case "qol.player_display.mana_hud" -> playerDisplayManaHud = value;
            case "qol.player_display.overflow_mana_hud" -> playerDisplayOverflowManaHud = value;
            case "qol.player_display.defense_hud" -> playerDisplayDefenseHud = value;
            case "qol.player_display.vitality_hud" -> playerDisplayVitalityHud = value;
            case "qol.player_display.ehp_hud" -> playerDisplayEhpHud = value;
            case "qol.player_display.speed_hud" -> playerDisplaySpeedHud = value;
            case "qol.pet_keybinds.disable_unequip" -> petDisableUnequip = value;
            case "qol.pet_keybinds.close_if_equipped" -> petCloseIfAlreadyEquipped = value;
            case "qol.inventory_overlay.equipment" -> inventoryOverlayEquipment = value;
            case "qol.inventory_overlay.hide_recipe_book" -> inventoryOverlayHideRecipeBook = value;
            case "qol.inventory_overlay.hide_status_effects" -> inventoryOverlayHideStatusEffects = value;
            case "qol.inventory_overlay.pet_slot" -> inventoryOverlayPetSlot = value;
            case "qol.inventory_overlay.protect_drops" -> inventoryOverlayProtectDrops = value;
            case "qol.inventory_overlay.protect_salvage" -> inventoryOverlayProtectSalvage = value;
            case "qol.player_display.hide_vanilla_health" -> playerDisplayHideVanillaHealth = value;
            case "qol.player_display.hide_vanilla_food" -> playerDisplayHideVanillaFood = value;
            case "qol.player_display.hide_vanilla_armor" -> playerDisplayHideVanillaArmor = value;
            case "qol.player_display.hide_vanilla_xp" -> playerDisplayHideVanillaXp = value;
            case "qol.player_display.hide_action_health" -> playerDisplayHideActionHealth = value;
            case "qol.player_display.hide_action_defense" -> playerDisplayHideActionDefense = value;
            case "qol.player_display.hide_action_mana" -> playerDisplayHideActionMana = value;
            case "qol.player_display.hide_action_overflow" -> playerDisplayHideActionOverflow = value;
            case "qol.player_display.hide_action_speed" -> playerDisplayHideActionSpeed = value;
            case "qol.player_display.hide_action_vitality" -> playerDisplayHideActionVitality = value;
            case "qol.player_display.hide_action_location" -> playerDisplayHideActionLocation = value;
            case "qol.auto_conversation.multi" -> autoConversationMulti = value;
            case "qol.auto_conversation.green" -> autoConversationGreen = value;
            case "qol.fishing_helper.auto_pull" -> fishingHelperAutoPull = value;
            case "qol.fishing_helper.recast" -> fishingHelperRecast = value;
            case "qol.fishing_helper.recast_check" -> fishingHelperRecastCheck = value;
            case "qol.fishing_helper.bobber_timer" -> fishingHelperBobberTimer = value;
            case "qol.fishing_helper.bite_title" -> fishingHelperBiteTitle = value;
            case "qol.fishing_helper.bite_sound" -> fishingHelperBiteSound = value;
            case "qol.fishing_helper.hook_timer_hud" -> fishingHelperHookTimerHud = value;
            case "qol.fishing_helper.hide_hook_nametag" -> fishingHelperHideHookNametag = value;
            case "qol.commission_display.colored_percent" -> commissionDisplayColoredPercent = value;
            case "qol.item_rarity.hotbar" -> itemRarityHotbar = value;
            case "qol.mob_highlight.highlight_key" -> mobHighlightRequireKey = value;
            case "qol.mob_highlight.depth" -> mobHighlightDepth = value;
            case "qol.mob_highlight.tracers" -> mobHighlightTracers = value;
            case "qol.custom_tooltip.infinite" -> customTooltipInfinite = value;
            case "qol.custom_tooltip.horizontal" -> customTooltipHorizontal = value;
            case "qol.custom_tooltip.vertical" -> customTooltipVertical = value;
            case "qol.custom_tooltip.reset" -> customTooltipReset = value;
            case "qol.custom_tooltip.centered_header" -> customTooltipCenteredHeader = value;
            case "qol.custom_tooltip.border" -> customTooltipBorder = value;
            case "qol.custom_tooltip.rarity_border" -> customTooltipRarityBorder = value;
            case "qol.custom_tooltip.background" -> customTooltipBackground = value;
            case "qol.custom_tooltip.shadows" -> customTooltipShadows = value;
            case "qol.missing_enchants.show_upgradable" -> missingEnchantsShowUpgradable = value;
            case "qol.missing_enchants.show_conflicting" -> missingEnchantsShowConflicting = value;
            case "qol.wardrobe_keybinds.sound" -> wardrobeSound = value;
            case "qol.command_keybinds.ratelimit_strict" -> commandBindRatelimitStrict = value;
            case "qol.command_keybinds.ratelimit_sp" -> commandBindRatelimitSp = value;
            case "qol.command_keybinds.use_ratelimit" -> commandBindUseRatelimit = value;
            case "qol.command_keybinds.add_history" -> commandBindAddHistory = value;
            case "qol.command_keybinds.show_hud" -> commandBindShowHud = value;
            case "qol.item_tooltips.missing" -> missingEnchantsEnabled = value;
            case "qol.item_tooltips.style" -> customTooltipEnabled = value;
            case "qol.item_tooltips.info" -> extras().setModuleEnabled("qol.info_tooltips", value);
            case "qol.item_tooltips.prices" -> extras().setModuleEnabled("qol.price_tooltips", value);
            default -> extras().writeBoolean(settingId, value);
        }
    }

    String readEnum(String settingId) {
        if (settingId == null) {
            return "";
        }
        String target = readWorldScannerTargetEnum(settingId);
        if (target != null) {
            return target;
        }
        return switch (settingId) {
            case "qol.performance_hud.direction" -> performanceDirection;
            case "qol.etherwarp.render_style" -> EtherwarpPredictor.normalizeRenderStyle(etherwarpRenderStyle);
            case "qol.slot_binds.line_display" -> slotBindLineDisplay;
            case "qol.slot_binds.profile" -> SlotBindsPolicy.normalizeProfile(slotBindProfile);
            case "qol.waypoints.ping_dropdown" -> WaypointPolicy.normalizePingMode(waypointsPingMode);
            case "qol.name_hider.mode" -> NameHiderPolicy.normalizeMode(nameHiderMode);
            case "qol.item_rarity.style" -> ItemRarityPolicy.normalizeStyle(itemRarityStyle);
            case "qol.player_size.player_animals_scope" -> playerAnimalsScope;
            case "qol.player_size.player_animals_species" ->
                    PlayerAnimalPolicy.normalizeSpecies(playerAnimalsSpecies);
            case "qol.custom_tooltip.style" -> CustomTooltipPolicy.normalizeStyle(customTooltipStyle);
            case "qol.wardrobe_keybinds.style" ->
                    QolSkyblockExtras.normalizeStyle(wardrobeKeybindStyle);
            case "qol.command_keybinds.send_mode" ->
                    RingPolicy.normalizeSendMode(commandBindSendMode);
            case "qol.command_keybinds.conflict" ->
                    RingPolicy.normalizeConflict(commandBindConflict);
            case "qol.command_keybinds.activation" ->
                    RingPolicy.normalizeActivation(commandBindActivation);
            default -> {
                String extra = extras().readEnum(settingId);
                yield extra == null ? "" : extra;
            }
        };
    }

    boolean cycleEnum(String settingId, java.util.List<String> options) {
        if (settingId == null || options == null || options.isEmpty()) {
            return false;
        }
        String current = readEnum(settingId);
        int index = Math.max(0, options.indexOf(current));
        String next = options.get((index + 1) % options.size());
        return writeEnum(settingId, next);
    }

    boolean writeEnum(String settingId, String value) {
        if (settingId == null || value == null) {
            return false;
        }
        switch (settingId) {
            case "qol.performance_hud.direction" -> performanceDirection = value;
            case "qol.etherwarp.render_style" ->
                    etherwarpRenderStyle = EtherwarpPredictor.normalizeRenderStyle(value);
            case "qol.slot_binds.line_display" -> slotBindLineDisplay = value;
            case "qol.slot_binds.profile" ->
                    slotBindProfile = SlotBindsPolicy.normalizeProfile(value);
            case "qol.waypoints.ping_dropdown" ->
                    waypointsPingMode = WaypointPolicy.normalizePingMode(value);
            case "qol.name_hider.mode" -> nameHiderMode = NameHiderPolicy.normalizeMode(value);
            case "qol.item_rarity.style" -> itemRarityStyle = ItemRarityPolicy.normalizeStyle(value);
            case "qol.player_size.player_animals_scope" -> playerAnimalsScope = value;
            case "qol.player_size.player_animals_species" ->
                    playerAnimalsSpecies = PlayerAnimalPolicy.normalizeSpecies(value);
            case "qol.custom_tooltip.style" ->
                    customTooltipStyle = CustomTooltipPolicy.normalizeStyle(value);
            case "qol.wardrobe_keybinds.style" -> {
                wardrobeKeybindStyle = QolSkyblockExtras.normalizeStyle(value);
                wardrobeUseHotbar = QolSkyblockExtras.STYLE_HOTBAR.equals(wardrobeKeybindStyle);
            }
            case "qol.command_keybinds.send_mode" ->
                    commandBindSendMode = RingPolicy.normalizeSendMode(value);
            case "qol.command_keybinds.conflict" ->
                    commandBindConflict = RingPolicy.normalizeConflict(value);
            case "qol.command_keybinds.activation" ->
                    commandBindActivation = RingPolicy.normalizeActivation(value);
            default -> {
                if (extras().writeEnum(settingId, value)) {
                    return true;
                }
                return writeWorldScannerTargetEnum(settingId, value);
            }
        }
        return true;
    }

    String readText(String settingId) {
        if (settingId == null) {
            return "";
        }
        return switch (settingId) {
            case "qol.name_hider.custom_name" ->
                    nameHiderCustomName == null ? "" : nameHiderCustomName;
            case "qol.commission_display.title" ->
                    commissionDisplayTitle == null ? "" : commissionDisplayTitle;
            case "qol.commission_display.none" ->
                    commissionDisplayNone == null ? "" : commissionDisplayNone;
            case "qol.commission_display.row" ->
                    commissionDisplayRow == null ? "" : commissionDisplayRow;
            case "qol.command_keybinds.macros" ->
                    commandBindMacros == null ? "" : commandBindMacros;
            case "qol.chat_commands.shortcuts" ->
                    chatCommandsShortcuts == null ? "" : chatCommandsShortcuts;
            case "qol.chat_commands.rules" ->
                    chatCommandsRules == null ? "" : chatCommandsRules;
            case "qol.chat_commands.quick_join_text" ->
                    chatQuickJoinText == null ? "" : chatQuickJoinText;
            case "qol.chat_commands.quick_join_ip" ->
                    chatQuickJoinIp == null ? "" : chatQuickJoinIp;
            case "qol.inventory_overlay.protect_list" ->
                    inventoryOverlayProtectList == null ? "" : inventoryOverlayProtectList;
            default -> {
                String extra = extras().readText(settingId);
                yield extra == null ? "" : extra;
            }
        };
    }

    boolean writeText(String settingId, String value) {
        if (settingId == null) {
            return false;
        }
        switch (settingId) {
            case "qol.name_hider.custom_name" ->
                    nameHiderCustomName = NameHiderPolicy.sanitizeCustomName(value);
            case "qol.commission_display.title" ->
                    commissionDisplayTitle = value == null ? "" : value;
            case "qol.commission_display.none" ->
                    commissionDisplayNone = value == null ? "" : value;
            case "qol.commission_display.row" ->
                    commissionDisplayRow = value == null ? "" : value;
            case "qol.command_keybinds.macros" ->
                    commandBindMacros = value == null ? "" : value;
            case "qol.chat_commands.shortcuts" ->
                    chatCommandsShortcuts = value == null ? "" : value;
            case "qol.chat_commands.rules" ->
                    chatCommandsRules = value == null ? "" : value;
            case "qol.chat_commands.quick_join_text" ->
                    chatQuickJoinText = value == null || value.isBlank()
                            ? SkyblockFlavorPolicy.DEFAULT_QUICK_JOIN_TEXT
                            : value.trim();
            case "qol.chat_commands.quick_join_ip" ->
                    chatQuickJoinIp = SkyblockFlavorPolicy.sanitizeQuickJoinIp(value);
            case "qol.inventory_overlay.protect_list" ->
                    inventoryOverlayProtectList = value == null ? "" : value;
            default -> {
                return extras().writeText(settingId, value);
            }
        }
        return true;
    }

    Double readNumber(String settingId) {
        if (settingId == null) {
            return null;
        }
        Double target = readWorldScannerTargetNumber(settingId);
        if (target != null) {
            return target;
        }
        return switch (settingId) {
            case "qol.hide_players.distance" -> hidePlayersDistance;
            case "qol.player_size.x" -> (double) playerSizeX;
            case "qol.player_size.y" -> (double) playerSizeY;
            case "qol.player_size.z" -> (double) playerSizeZ;
            case "qol.no_cursor_reset.unhook_timeout" -> (double) noCursorUnhookTimeoutMs;
            case "qol.slot_binds.line_width" -> (double) slotBindLineWidth;
            case "qol.auto_clicker.cps" -> (double) autoClickerCps;
            case "qol.auto_clicker.left_cps" -> (double) autoClickerLeftCps;
            case "qol.auto_clicker.right_cps" -> (double) autoClickerRightCps;
            case "qol.inventory_walk.ping" -> (double) inventoryWalkPingMs;
            case "qol.trajectories.range" -> (double) trajectoriesRange;
            case "qol.trajectories.width" -> (double) trajectoriesWidth;
            case "qol.trajectories.box_size" -> (double) trajectoriesBoxSize;
            case "qol.trajectories.plane_size" -> (double) trajectoriesPlaneSize;
            case "qol.world_scanner.esp_range" -> (double) worldScannerEspRange;
            case "qol.chat_commands.previous_server_time" -> (double) chatPreviousServerSeconds;
            case "qol.auto_conversation.delay" -> (double) autoConversationDelayTicks;
            case "qol.fishing_helper.pull_delay" -> (double) fishingHelperPullDelay;
            case "qol.fishing_helper.pull_variance" -> (double) fishingHelperPullVariance;
            case "qol.fishing_helper.recast_delay" -> (double) fishingHelperRecastDelay;
            case "qol.fishing_helper.recast_variance" -> (double) fishingHelperRecastVariance;
            case "qol.item_rarity.fill_alpha" -> (double) itemRarityFillAlpha;
            case "qol.item_rarity.outline_alpha" -> (double) itemRarityOutlineAlpha;
            case "qol.custom_tooltip.horizontal_speed" -> (double) customTooltipHorizontalSpeed;
            case "qol.custom_tooltip.vertical_speed" -> (double) customTooltipVerticalSpeed;
            case "qol.custom_tooltip.border_width" -> (double) customTooltipBorderWidth;
            case "qol.wardrobe_keybinds.ping" -> (double) wardrobePingMs;
            case "qol.wardrobe_keybinds.swap_a" -> (double) wardrobeSwapSlotA;
            case "qol.wardrobe_keybinds.swap_b" -> (double) wardrobeSwapSlotB;
            case "qol.wardrobe_keybinds.click_delay" -> (double) wardrobeClickDelay;
            case "qol.wardrobe_keybinds.close_delay" -> (double) wardrobeCloseDelay;
            case "qol.wardrobe_keybinds.delay_variance" -> (double) wardrobeDelayVariance;
            case "qol.command_keybinds.ratelimit_count" -> (double) commandBindRatelimitCount;
            case "qol.command_keybinds.ratelimit_ticks" -> (double) commandBindRatelimitTicks;
            case "qol.command_keybinds.length_limit" -> (double) commandBindLengthLimit;
            default -> extras().readNumber(settingId);
        };
    }

    boolean nudgeNumber(String settingId, boolean increase) {
        Double current = readNumber(settingId);
        QolNumberSettings.Spec spec = QolNumberSettings.spec(settingId);
        if (current == null || spec == null) {
            return false;
        }
        double next = spec.snap(current + (increase ? spec.step() : -spec.step()));
        return writeNumber(settingId, next);
    }

    boolean writeNumber(String settingId, double value) {
        if (settingId == null) {
            return false;
        }
        switch (settingId) {
            case "qol.hide_players.distance" -> hidePlayersDistance = value;
            case "qol.player_size.x" -> playerSizeX = (float) value;
            case "qol.player_size.y" -> playerSizeY = (float) value;
            case "qol.player_size.z" -> playerSizeZ = (float) value;
            case "qol.no_cursor_reset.unhook_timeout" ->
                    noCursorUnhookTimeoutMs = (int) Math.round(value);
            case "qol.slot_binds.line_width" ->
                    slotBindLineWidth = SlotBindsPolicy.clampLineWidth((float) value);
            case "qol.auto_clicker.cps" -> autoClickerCps = AutoClickerPolicy.clampCps((float) value);
            case "qol.auto_clicker.left_cps" ->
                    autoClickerLeftCps = AutoClickerPolicy.clampCps((float) value);
            case "qol.auto_clicker.right_cps" ->
                    autoClickerRightCps = AutoClickerPolicy.clampCps((float) value);
            case "qol.inventory_walk.ping" ->
                    inventoryWalkPingMs = InventoryWalkPolicy.clampPingMs((int) Math.round(value));
            case "qol.wardrobe_keybinds.ping" ->
                    wardrobePingMs = WardrobeKeybindPolicy.clampPingMs((int) Math.round(value));
            case "qol.wardrobe_keybinds.swap_a" ->
                    wardrobeSwapSlotA = WardrobeKeybindPolicy.clampSlotIndex((int) Math.round(value));
            case "qol.wardrobe_keybinds.swap_b" ->
                    wardrobeSwapSlotB = WardrobeKeybindPolicy.clampSlotIndex((int) Math.round(value));
            case "qol.wardrobe_keybinds.click_delay" ->
                    wardrobeClickDelay = WardrobeKeybindPolicy.clampDelayTicks((int) Math.round(value));
            case "qol.wardrobe_keybinds.close_delay" ->
                    wardrobeCloseDelay = WardrobeKeybindPolicy.clampDelayTicks((int) Math.round(value));
            case "qol.wardrobe_keybinds.delay_variance" ->
                    wardrobeDelayVariance = WardrobeKeybindPolicy.clampVariance((int) Math.round(value));
            case "qol.command_keybinds.ratelimit_count" ->
                    commandBindRatelimitCount =
                            RingPolicy.clampRatelimitCount((int) Math.round(value));
            case "qol.command_keybinds.ratelimit_ticks" ->
                    commandBindRatelimitTicks =
                            RingPolicy.clampRatelimitTicks((int) Math.round(value));
            case "qol.command_keybinds.length_limit" ->
                    commandBindLengthLimit =
                            RingPolicy.clampLengthLimit((int) Math.round(value));
            case "qol.trajectories.range" ->
                    trajectoriesRange = TrajectoryPredictor.clampRange((int) Math.round(value));
            case "qol.trajectories.width" -> trajectoriesWidth = (float) clamp(value, 0.1D, 5.0D);
            case "qol.trajectories.box_size" ->
                    trajectoriesBoxSize = (float) clamp(value, 0.5D, 3.0D);
            case "qol.trajectories.plane_size" ->
                    trajectoriesPlaneSize = (float) clamp(value, 0.5D, 8.0D);
            case "qol.world_scanner.esp_range" ->
                    worldScannerEspRange = WorldScannerPolicy.clampEspRange((int) Math.round(value));
            case "qol.chat_commands.previous_server_time" ->
                    chatPreviousServerSeconds = SkyblockFlavorPolicy.clampPreviousServerSeconds((int) Math.round(value));
            case "qol.auto_conversation.delay" ->
                    autoConversationDelayTicks = AutoConversationPolicy.clampDelayTicks((int) Math.round(value));
            case "qol.fishing_helper.pull_delay" ->
                    fishingHelperPullDelay = FishingHelperPolicy.clampDelay((int) Math.round(value));
            case "qol.fishing_helper.pull_variance" ->
                    fishingHelperPullVariance = FishingHelperPolicy.clampDelay((int) Math.round(value));
            case "qol.fishing_helper.recast_delay" ->
                    fishingHelperRecastDelay = FishingHelperPolicy.clampDelay((int) Math.round(value));
            case "qol.fishing_helper.recast_variance" ->
                    fishingHelperRecastVariance = FishingHelperPolicy.clampDelay((int) Math.round(value));
            case "qol.item_rarity.fill_alpha" ->
                    itemRarityFillAlpha = (float) clamp(value, 0.0D, 1.0D);
            case "qol.item_rarity.outline_alpha" ->
                    itemRarityOutlineAlpha = (float) clamp(value, 0.0D, 1.0D);
            case "qol.custom_tooltip.horizontal_speed" ->
                    customTooltipHorizontalSpeed = (int) Math.round(clamp(value, 1.0D, 32.0D));
            case "qol.custom_tooltip.vertical_speed" ->
                    customTooltipVerticalSpeed = (int) Math.round(clamp(value, 1.0D, 32.0D));
            case "qol.custom_tooltip.border_width" ->
                    customTooltipBorderWidth = (int) Math.round(clamp(value, 1.0D, 4.0D));
            default -> {
                if (extras().writeNumber(settingId, value)) {
                    return true;
                }
                return writeWorldScannerTargetNumber(settingId, value);
            }
        }
        return true;
    }

    Integer readColor(String settingId) {
        if (settingId == null) {
            return null;
        }
        return switch (settingId) {
            case "qol.performance_hud.name_color" -> performanceNameColor;
            case "qol.performance_hud.value_color" -> performanceValueColor;
            case "qol.etherwarp.color" -> etherwarpColor;
            case "qol.etherwarp.fail_color" -> etherwarpFailColor;
            case "qol.click_gui.color" -> clickGuiColor;
            case "qol.slot_binds.bind_color" -> slotBindColor;
            case "qol.player_display.health_color" -> playerDisplayHealthColor;
            case "qol.player_display.mana_color" -> playerDisplayManaColor;
            case "qol.player_display.overflow_mana_color" -> playerDisplayOverflowManaColor;
            case "qol.player_display.defense_color" -> playerDisplayDefenseColor;
            case "qol.player_display.vitality_color" -> playerDisplayVitalityColor;
            case "qol.player_display.ehp_color" -> playerDisplayEhpColor;
            case "qol.player_display.speed_color" -> playerDisplaySpeedColor;
            case "qol.trajectories.color" -> trajectoriesColor;
            case "qol.skill_levels.level_color" -> skillLevelsColor;
            case "qol.skill_levels.max_color" -> skillLevelsMaxColor;
            case "qol.item_rarity.common" -> itemRarityCommon;
            case "qol.item_rarity.uncommon" -> itemRarityUncommon;
            case "qol.item_rarity.rare" -> itemRarityRare;
            case "qol.item_rarity.epic" -> itemRarityEpic;
            case "qol.item_rarity.legendary" -> itemRarityLegendary;
            case "qol.item_rarity.mythic" -> itemRarityMythic;
            case "qol.item_rarity.divine" -> itemRarityDivine;
            case "qol.item_rarity.special" -> itemRaritySpecial;
            case "qol.mob_highlight.color" -> mobHighlightColor;
            case "qol.player_size.player_animals_collar" -> playerAnimalsCollarColor;
            case "qol.custom_tooltip.border_color" -> customTooltipBorderColor;
            case "qol.custom_tooltip.background_color" -> customTooltipBackgroundColor;
            case "qol.inventory_overlay.chrome_panel" -> inventoryChromePanel;
            case "qol.inventory_overlay.chrome_header" -> inventoryChromeHeader;
            case "qol.inventory_overlay.chrome_main" -> inventoryChromeMain;
            case "qol.inventory_overlay.chrome_hotbar" -> inventoryChromeHotbar;
            case "qol.inventory_overlay.chrome_border" -> inventoryChromeBorder;
            default -> {
                Integer extra = extras().readColor(settingId);
                yield extra != null ? extra : readWorldScannerTargetColor(settingId);
            }
        };
    }

    boolean writeColor(String settingId, int argb) {
        if (settingId == null) {
            return false;
        }
        switch (settingId) {
            case "qol.performance_hud.name_color" -> performanceNameColor = argb;
            case "qol.performance_hud.value_color" -> performanceValueColor = argb;
            case "qol.etherwarp.color" -> etherwarpColor = argb;
            case "qol.etherwarp.fail_color" -> etherwarpFailColor = argb;
            case "qol.click_gui.color" -> clickGuiColor = argb;
            case "qol.slot_binds.bind_color" -> slotBindColor = argb;
            case "qol.player_display.health_color" -> playerDisplayHealthColor = argb;
            case "qol.player_display.mana_color" -> playerDisplayManaColor = argb;
            case "qol.player_display.overflow_mana_color" -> playerDisplayOverflowManaColor = argb;
            case "qol.player_display.defense_color" -> playerDisplayDefenseColor = argb;
            case "qol.player_display.vitality_color" -> playerDisplayVitalityColor = argb;
            case "qol.player_display.ehp_color" -> playerDisplayEhpColor = argb;
            case "qol.player_display.speed_color" -> playerDisplaySpeedColor = argb;
            case "qol.trajectories.color" -> trajectoriesColor = argb;
            case "qol.skill_levels.level_color" -> skillLevelsColor = argb;
            case "qol.skill_levels.max_color" -> skillLevelsMaxColor = argb;
            case "qol.item_rarity.common" -> itemRarityCommon = argb;
            case "qol.item_rarity.uncommon" -> itemRarityUncommon = argb;
            case "qol.item_rarity.rare" -> itemRarityRare = argb;
            case "qol.item_rarity.epic" -> itemRarityEpic = argb;
            case "qol.item_rarity.legendary" -> itemRarityLegendary = argb;
            case "qol.item_rarity.mythic" -> itemRarityMythic = argb;
            case "qol.item_rarity.divine" -> itemRarityDivine = argb;
            case "qol.item_rarity.special" -> itemRaritySpecial = argb;
            case "qol.mob_highlight.color" -> mobHighlightColor = argb;
            case "qol.player_size.player_animals_collar" -> playerAnimalsCollarColor = argb;
            case "qol.custom_tooltip.border_color" -> customTooltipBorderColor = argb;
            case "qol.custom_tooltip.background_color" -> customTooltipBackgroundColor = argb;
            case "qol.inventory_overlay.chrome_panel" -> inventoryChromePanel = argb;
            case "qol.inventory_overlay.chrome_header" -> inventoryChromeHeader = argb;
            case "qol.inventory_overlay.chrome_main" -> inventoryChromeMain = argb;
            case "qol.inventory_overlay.chrome_hotbar" -> inventoryChromeHotbar = argb;
            case "qol.inventory_overlay.chrome_border" -> inventoryChromeBorder = argb;
            default -> {
                if (extras().writeColor(settingId, argb)) {
                    return true;
                }
                return writeWorldScannerTargetColor(settingId, argb);
            }
        }
        return true;
    }

    String readKeybind(String settingId) {
        if (settingId == null) {
            return "";
        }
        String value = switch (settingId) {
            case "qol.performance_hud.keybind" -> performanceKeybind;
            case "qol.render_optimizer.keybind" -> renderOptimizerKeybind;
            case "qol.hide_players.keybind" -> hidePlayersKeybind;
            case "qol.player_size.keybind" -> playerSizeKeybind;
            case "qol.etherwarp.keybind" -> etherwarpKeybind;
            case "qol.click_gui.keybind" -> clickGuiKeybind;
            case "qol.auto_sprint.keybind" -> autoSprintKeybind;
            case "qol.auto_clicker.left_keybind" -> autoClickerLeftKeybind;
            case "qol.auto_clicker.right_keybind" -> autoClickerRightKeybind;
            case "qol.camera.keybind" -> cameraKeybind;
            case "qol.command_keybinds.pets" -> commandPetsKey;
            case "qol.command_keybinds.storage" -> commandStorageKey;
            case "qol.command_keybinds.armor_wardrobe" -> commandArmorWardrobeKey;
            case "qol.command_keybinds.equip_wardrobe" -> commandEquipWardrobeKey;
            case "qol.command_keybinds.loadouts" -> commandLoadoutsKey;
            case "qol.command_keybinds.stats" -> commandStatsKey;
            case "qol.command_keybinds.dungeon_hub" -> commandDungeonHubKey;
            case "qol.command_keybinds.potion_bag" -> commandPotionBagKey;
            case "qol.wardrobe_keybinds.next" -> wardrobeNextKey;
            case "qol.wardrobe_keybinds.previous" -> wardrobePreviousKey;
            case "qol.wardrobe_keybinds.unequip" -> wardrobeUnequipKey;
            case "qol.wardrobe_keybinds.override" -> wardrobeOverrideKey;
            case "qol.wardrobe_keybinds.swap_key" -> wardrobeSwapKey;
            case "qol.wardrobe_keybinds.custom_1" -> wardrobeCustom1;
            case "qol.wardrobe_keybinds.custom_2" -> wardrobeCustom2;
            case "qol.wardrobe_keybinds.custom_3" -> wardrobeCustom3;
            case "qol.wardrobe_keybinds.custom_4" -> wardrobeCustom4;
            case "qol.wardrobe_keybinds.custom_5" -> wardrobeCustom5;
            case "qol.wardrobe_keybinds.custom_6" -> wardrobeCustom6;
            case "qol.wardrobe_keybinds.custom_7" -> wardrobeCustom7;
            case "qol.wardrobe_keybinds.custom_8" -> wardrobeCustom8;
            case "qol.wardrobe_keybinds.custom_9" -> wardrobeCustom9;
            case "qol.loadout_keybinds.next" -> loadoutNextKey;
            case "qol.loadout_keybinds.previous" -> loadoutPreviousKey;
            case "qol.chat_commands.keybind" -> chatCommandsKeybind;
            case "qol.no_cursor_reset.keybind" -> noCursorKeybind;
            case "qol.player_display.keybind" -> playerDisplayKeybind;
            case "qol.pet_keybinds.unequip" -> petUnequipKey;
            case "qol.pet_keybinds.next" -> petNextKey;
            case "qol.pet_keybinds.previous" -> petPreviousKey;
            case "qol.pet_keybinds.keybind" -> petModuleKeybind;
            case "qol.slot_binds.bind_set_key" -> slotBindSetKey;
            case "qol.waypoints.keybind" -> waypointsKeybind;
            case "qol.missing_enchants.keybind" -> missingEnchantsKeybind;
            case "qol.mob_highlight.add_key" -> mobHighlightAddKey;
            case "qol.custom_tooltip.horizontal_key" -> customTooltipHorizontalKey;
            case "qol.custom_tooltip.only_name_key" -> customTooltipOnlyNameKey;
            default -> extras().readKeybind(settingId);
        };
        return value == null ? "" : value;
    }

    boolean writeKeybind(String settingId, String value) {
        if (settingId == null) {
            return false;
        }
        String stored = value == null ? "" : value.trim();
        switch (settingId) {
            case "qol.performance_hud.keybind" -> performanceKeybind = stored;
            case "qol.render_optimizer.keybind" -> renderOptimizerKeybind = stored;
            case "qol.hide_players.keybind" -> hidePlayersKeybind = stored;
            case "qol.player_size.keybind" -> playerSizeKeybind = stored;
            case "qol.etherwarp.keybind" -> etherwarpKeybind = stored;
            case "qol.click_gui.keybind" -> clickGuiKeybind = stored;
            case "qol.auto_sprint.keybind" -> autoSprintKeybind = stored;
            case "qol.auto_clicker.left_keybind" -> autoClickerLeftKeybind = stored;
            case "qol.auto_clicker.right_keybind" -> autoClickerRightKeybind = stored;
            case "qol.camera.keybind" -> cameraKeybind = stored;
            case "qol.command_keybinds.pets" -> commandPetsKey = stored;
            case "qol.command_keybinds.storage" -> commandStorageKey = stored;
            case "qol.command_keybinds.armor_wardrobe" -> commandArmorWardrobeKey = stored;
            case "qol.command_keybinds.equip_wardrobe" -> commandEquipWardrobeKey = stored;
            case "qol.command_keybinds.loadouts" -> commandLoadoutsKey = stored;
            case "qol.command_keybinds.stats" -> commandStatsKey = stored;
            case "qol.command_keybinds.dungeon_hub" -> commandDungeonHubKey = stored;
            case "qol.command_keybinds.potion_bag" -> commandPotionBagKey = stored;
            case "qol.wardrobe_keybinds.next" -> wardrobeNextKey = stored;
            case "qol.wardrobe_keybinds.previous" -> wardrobePreviousKey = stored;
            case "qol.wardrobe_keybinds.unequip" -> wardrobeUnequipKey = stored;
            case "qol.wardrobe_keybinds.override" -> wardrobeOverrideKey = stored;
            case "qol.wardrobe_keybinds.swap_key" -> wardrobeSwapKey = stored;
            case "qol.wardrobe_keybinds.custom_1" -> wardrobeCustom1 = stored;
            case "qol.wardrobe_keybinds.custom_2" -> wardrobeCustom2 = stored;
            case "qol.wardrobe_keybinds.custom_3" -> wardrobeCustom3 = stored;
            case "qol.wardrobe_keybinds.custom_4" -> wardrobeCustom4 = stored;
            case "qol.wardrobe_keybinds.custom_5" -> wardrobeCustom5 = stored;
            case "qol.wardrobe_keybinds.custom_6" -> wardrobeCustom6 = stored;
            case "qol.wardrobe_keybinds.custom_7" -> wardrobeCustom7 = stored;
            case "qol.wardrobe_keybinds.custom_8" -> wardrobeCustom8 = stored;
            case "qol.wardrobe_keybinds.custom_9" -> wardrobeCustom9 = stored;
            case "qol.loadout_keybinds.next" -> loadoutNextKey = stored;
            case "qol.loadout_keybinds.previous" -> loadoutPreviousKey = stored;
            case "qol.chat_commands.keybind" -> chatCommandsKeybind = stored;
            case "qol.no_cursor_reset.keybind" -> noCursorKeybind = stored;
            case "qol.player_display.keybind" -> playerDisplayKeybind = stored;
            case "qol.pet_keybinds.unequip" -> petUnequipKey = stored;
            case "qol.pet_keybinds.next" -> petNextKey = stored;
            case "qol.pet_keybinds.previous" -> petPreviousKey = stored;
            case "qol.pet_keybinds.keybind" -> petModuleKeybind = stored;
            case "qol.slot_binds.bind_set_key" -> slotBindSetKey = stored;
            case "qol.waypoints.keybind" -> waypointsKeybind = stored;
            case "qol.missing_enchants.keybind" -> missingEnchantsKeybind = stored;
            case "qol.mob_highlight.add_key" -> mobHighlightAddKey = stored;
            case "qol.custom_tooltip.horizontal_key" -> customTooltipHorizontalKey = stored;
            case "qol.custom_tooltip.only_name_key" -> customTooltipOnlyNameKey = stored;
            default -> {
                return extras().writeKeybind(settingId, stored);
            }
        }
        return true;
    }

    String displayKeybind(String settingId) {
        String raw = readKeybind(settingId);
        return raw.isBlank() ? "Not Bound" : raw;
    }

    Map<Integer, Integer> slotBindsForActiveProfile() {
        return slotBindsForProfile(slotBindProfile);
    }

    Map<Integer, Integer> slotBindsForProfile(String profile) {
        String key = SlotBindsPolicy.normalizeProfile(profile);
        Map<String, Integer> raw = slotBindProfiles == null
                ? null
                : slotBindProfiles.get(key);
        Map<Integer, Integer> out = new LinkedHashMap<>();
        if (raw == null) {
            return out;
        }
        for (Map.Entry<String, Integer> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            try {
                out.put(Integer.parseInt(entry.getKey()), entry.getValue());
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    void putSlotBindsForActiveProfile(Map<Integer, Integer> binds) {
        String key = SlotBindsPolicy.normalizeProfile(slotBindProfile);
        if (slotBindProfiles == null) {
            slotBindProfiles = new LinkedHashMap<>();
        }
        Map<String, Integer> stored = new LinkedHashMap<>();
        if (binds != null) {
            for (Map.Entry<Integer, Integer> entry : binds.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }
                stored.put(Integer.toString(entry.getKey()), entry.getValue());
            }
        }
        if (stored.isEmpty()) {
            slotBindProfiles.remove(key);
        } else {
            slotBindProfiles.put(key, stored);
        }
    }

    Map<String, Object> snapshotForTests() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("performanceDirection", performanceDirection);
        map.put("hideLightning", hideLightning);
        map.put("playerSizeX", playerSizeX);
        map.put("noCursorUnhookTimeoutMs", noCursorUnhookTimeoutMs);
        return map;
    }

    void normalizeHudPoses() {
        performanceHudScale = clampScale(performanceHudScale);
        performanceHudX = clampPos(performanceHudX);
        performanceHudY = clampPos(performanceHudY);
        healthHudX = clampPos(healthHudX);
        healthHudY = clampPos(healthHudY);
        manaHudX = clampPos(manaHudX);
        manaHudY = clampPos(manaHudY);
        overflowManaHudX = clampPos(overflowManaHudX);
        overflowManaHudY = clampPos(overflowManaHudY);
        defenseHudX = clampPos(defenseHudX);
        defenseHudY = clampPos(defenseHudY);
        vitalityHudX = clampPos(vitalityHudX);
        vitalityHudY = clampPos(vitalityHudY);
        ehpHudX = clampPos(ehpHudX);
        ehpHudY = clampPos(ehpHudY);
        speedHudX = clampPos(speedHudX);
        speedHudY = clampPos(speedHudY);
        noCursorUnhookTimeoutMs = NoCursorResetPolicy.clampTimeoutMs(
                noCursorUnhookTimeoutMs);
        hidePlayersDistance = HidePlayersPolicy.clampDistance(hidePlayersDistance);
        playerSizeX = PlayerSizePolicy.clamp(playerSizeX);
        playerSizeY = PlayerSizePolicy.clamp(playerSizeY);
        playerSizeZ = PlayerSizePolicy.clamp(playerSizeZ);
        if (performanceDirection == null || performanceDirection.isBlank()) {
            performanceDirection = "Horizontal";
        }
        commissionHudX = clampPos(commissionHudX);
        commissionHudY = clampPos(commissionHudY);
        wardrobeHudX = clampPos(wardrobeHudX);
        wardrobeHudY = clampPos(wardrobeHudY);
        slayerHudX = clampPos(slayerHudX);
        slayerHudY = clampPos(slayerHudY);
        slayerProgressHudX = clampPos(slayerProgressHudX);
        slayerProgressHudY = clampPos(slayerProgressHudY);
        slayerRngHudX = clampPos(slayerRngHudX);
        slayerRngHudY = clampPos(slayerRngHudY);
        slayerProfitHudX = clampPos(slayerProfitHudX);
        slayerProfitHudY = clampPos(slayerProfitHudY);
        slayerStatsHudX = clampPos(slayerStatsHudX);
        slayerStatsHudY = clampPos(slayerStatsHudY);
        slayerCarryHudX = clampPos(slayerCarryHudX);
        slayerCarryHudY = clampPos(slayerCarryHudY);
        slayerCocoonHudX = clampPos(slayerCocoonHudX);
        slayerCocoonHudY = clampPos(slayerCocoonHudY);
        slayerAttunementHudX = clampPos(slayerAttunementHudX);
        slayerAttunementHudY = clampPos(slayerAttunementHudY);
        slayerVengeanceHudX = clampPos(slayerVengeanceHudX);
        slayerVengeanceHudY = clampPos(slayerVengeanceHudY);
        autoClickerHudX = clampPos(autoClickerHudX);
        autoClickerHudY = clampPos(autoClickerHudY);
        dungeonHudX = clampPos(dungeonHudX);
        dungeonHudY = clampPos(dungeonHudY);
        wardrobePingMs = WardrobeKeybindPolicy.clampPingMs(wardrobePingMs);
        wardrobeSwapSlotA = WardrobeKeybindPolicy.clampSlotIndex(wardrobeSwapSlotA);
        wardrobeSwapSlotB = WardrobeKeybindPolicy.clampSlotIndex(wardrobeSwapSlotB);
        wardrobeClickDelay = WardrobeKeybindPolicy.clampDelayTicks(wardrobeClickDelay);
        wardrobeCloseDelay = WardrobeKeybindPolicy.clampDelayTicks(wardrobeCloseDelay);
        wardrobeDelayVariance = WardrobeKeybindPolicy.clampVariance(wardrobeDelayVariance);
        ensureWorldScannerTargets();
    }

    void migrateLegacyDefaultPalette() {
        if (performanceNameColor == 0xFFC69797) {
            performanceNameColor = 0xFFB8C7D8;
        }
        if (performanceValueColor == 0xFFF6ECEC) {
            performanceValueColor = 0xFFF3F7FB;
        }
        if (clickGuiColor == 0xFFE33B3B || clickGuiColor == 0xFF4CC9B0) {
            clickGuiColor = 0xFFE11D48;
        }
        if (slotBindColor == 0xFFE33B3B || slotBindColor == 0xFF4CC9B0) {
            slotBindColor = 0xFFE11D48;
        }
    }

    float[] pose(String elementId) {
        return switch (elementId == null ? "" : elementId) {
            case "performance" -> new float[] {performanceHudX, performanceHudY, performanceHudScale};
            case "health" -> new float[] {healthHudX, healthHudY, 1.0F};
            case "mana" -> new float[] {manaHudX, manaHudY, 1.0F};
            case "overflow" -> new float[] {overflowManaHudX, overflowManaHudY, 1.0F};
            case "defense" -> new float[] {defenseHudX, defenseHudY, 1.0F};
            case "vitality" -> new float[] {vitalityHudX, vitalityHudY, 1.0F};
            case "ehp" -> new float[] {ehpHudX, ehpHudY, 1.0F};
            case "speed" -> new float[] {speedHudX, speedHudY, 1.0F};
            case "pet" -> new float[] {petHudX, petHudY, 1.0F};
            case "commission" -> new float[] {commissionHudX, commissionHudY, 1.0F};
            case "wardrobe" -> new float[] {wardrobeHudX, wardrobeHudY, 1.0F};
            case "slayer" -> new float[] {slayerHudX, slayerHudY, 1.0F};
            case "slayer_progress" -> new float[] {slayerProgressHudX, slayerProgressHudY, 1.0F};
            case "slayer_rng" -> new float[] {slayerRngHudX, slayerRngHudY, 1.0F};
            case "slayer_profit" -> new float[] {slayerProfitHudX, slayerProfitHudY, 1.0F};
            case "slayer_stats" -> new float[] {slayerStatsHudX, slayerStatsHudY, 1.0F};
            case "slayer_carry" -> new float[] {slayerCarryHudX, slayerCarryHudY, 1.0F};
            case "slayer_cocoon" -> new float[] {slayerCocoonHudX, slayerCocoonHudY, 1.0F};
            case "slayer_attunement" -> new float[] {slayerAttunementHudX, slayerAttunementHudY, 1.0F};
            case "slayer_vengeance" -> new float[] {slayerVengeanceHudX, slayerVengeanceHudY, 1.0F};
            case "auto_clicker" -> new float[] {autoClickerHudX, autoClickerHudY, 1.0F};
            case "dungeon" -> new float[] {dungeonHudX, dungeonHudY, 1.0F};
            case "fishing" -> new float[] {fishingHudX, fishingHudY, 1.0F};
            case "mining" -> new float[] {extras().miningHudX, extras().miningHudY, 1.0F};
            case "diana" -> new float[] {extras().dianaHudX, extras().dianaHudY, 1.0F};
            case "foraging" -> new float[] {extras().foragingHudX, extras().foragingHudY, 1.0F};
            case "iota_arrows" -> new float[] {extras().iotaArrowHudX, extras().iotaArrowHudY, 1.0F};
            case "kuudra_alerts" -> new float[] {extras().iotaAlertHudX, extras().iotaAlertHudY, 1.0F};
            case "stall_bin" -> new float[] {extras().stallBinHudX, extras().stallBinHudY, 1.0F};
            default -> new float[] {12.0F, 12.0F, 1.0F};
        };
    }

    void setPose(String elementId, float x, float y) {
        switch (elementId == null ? "" : elementId) {
            case "performance" -> {
                performanceHudX = clampPos(x);
                performanceHudY = clampPos(y);
            }
            case "health" -> {
                healthHudX = clampPos(x);
                healthHudY = clampPos(y);
            }
            case "mana" -> {
                manaHudX = clampPos(x);
                manaHudY = clampPos(y);
            }
            case "overflow" -> {
                overflowManaHudX = clampPos(x);
                overflowManaHudY = clampPos(y);
            }
            case "defense" -> {
                defenseHudX = clampPos(x);
                defenseHudY = clampPos(y);
            }
            case "vitality" -> {
                vitalityHudX = clampPos(x);
                vitalityHudY = clampPos(y);
            }
            case "ehp" -> {
                ehpHudX = clampPos(x);
                ehpHudY = clampPos(y);
            }
            case "speed" -> {
                speedHudX = clampPos(x);
                speedHudY = clampPos(y);
            }
            case "pet" -> {
                petHudX = clampPos(x);
                petHudY = clampPos(y);
            }
            case "commission" -> {
                commissionHudX = clampPos(x);
                commissionHudY = clampPos(y);
            }
            case "wardrobe" -> {
                wardrobeHudX = clampPos(x);
                wardrobeHudY = clampPos(y);
            }
            case "slayer" -> {
                slayerHudX = clampPos(x);
                slayerHudY = clampPos(y);
            }
            case "slayer_progress" -> {
                slayerProgressHudX = clampPos(x);
                slayerProgressHudY = clampPos(y);
            }
            case "slayer_rng" -> {
                slayerRngHudX = clampPos(x);
                slayerRngHudY = clampPos(y);
            }
            case "slayer_profit" -> {
                slayerProfitHudX = clampPos(x);
                slayerProfitHudY = clampPos(y);
            }
            case "slayer_stats" -> {
                slayerStatsHudX = clampPos(x);
                slayerStatsHudY = clampPos(y);
            }
            case "slayer_carry" -> {
                slayerCarryHudX = clampPos(x);
                slayerCarryHudY = clampPos(y);
            }
            case "slayer_cocoon" -> {
                slayerCocoonHudX = clampPos(x);
                slayerCocoonHudY = clampPos(y);
            }
            case "slayer_attunement" -> {
                slayerAttunementHudX = clampPos(x);
                slayerAttunementHudY = clampPos(y);
            }
            case "slayer_vengeance" -> {
                slayerVengeanceHudX = clampPos(x);
                slayerVengeanceHudY = clampPos(y);
            }
            case "auto_clicker" -> {
                autoClickerHudX = clampPos(x);
                autoClickerHudY = clampPos(y);
            }
            case "dungeon" -> {
                dungeonHudX = clampPos(x);
                dungeonHudY = clampPos(y);
            }
            case "fishing" -> {
                fishingHudX = clampPos(x);
                fishingHudY = clampPos(y);
            }
            case "mining" -> {
                extras().miningHudX = clampPos(x);
                extras().miningHudY = clampPos(y);
            }
            case "diana" -> {
                extras().dianaHudX = clampPos(x);
                extras().dianaHudY = clampPos(y);
            }
            case "foraging" -> {
                extras().foragingHudX = clampPos(x);
                extras().foragingHudY = clampPos(y);
            }
            case "iota_arrows" -> {
                extras().iotaArrowHudX = clampPos(x);
                extras().iotaArrowHudY = clampPos(y);
            }
            case "kuudra_alerts" -> {
                extras().iotaAlertHudX = clampPos(x);
                extras().iotaAlertHudY = clampPos(y);
            }
            case "stall_bin" -> {
                extras().stallBinHudX = clampPos(x);
                extras().stallBinHudY = clampPos(y);
            }
            default -> {
            }
        }
    }

    private static float clampPos(float value) {
        if (!Float.isFinite(value)) {
            return 12.0F;
        }
        return Math.max(0.0F, value);
    }

    /**
     * Resets one module's settings (and its enable flag when owned here) to
     * constructor defaults. Does not touch other modules or Appearance layout.
     */
    boolean resetModuleToDefaults(String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return false;
        }
        QolUtilityConfig d = new QolUtilityConfig();
        switch (moduleId) {
            case "qol.command_keybinds" -> {
                commandKeybindsEnabled = d.commandKeybindsEnabled;
                commandPetsKey = d.commandPetsKey;
                commandStorageKey = d.commandStorageKey;
                commandArmorWardrobeKey = d.commandArmorWardrobeKey;
                commandEquipWardrobeKey = d.commandEquipWardrobeKey;
                commandLoadoutsKey = d.commandLoadoutsKey;
                commandStatsKey = d.commandStatsKey;
                commandDungeonHubKey = d.commandDungeonHubKey;
                commandPotionBagKey = d.commandPotionBagKey;
                commandBindMacros = d.commandBindMacros;
                commandBindSendMode = d.commandBindSendMode;
                commandBindConflict = d.commandBindConflict;
                commandBindActivation = d.commandBindActivation;
                commandBindRatelimitCount = d.commandBindRatelimitCount;
                commandBindRatelimitTicks = d.commandBindRatelimitTicks;
                commandBindRatelimitStrict = d.commandBindRatelimitStrict;
                commandBindRatelimitSp = d.commandBindRatelimitSp;
                commandBindUseRatelimit = d.commandBindUseRatelimit;
                commandBindLengthLimit = d.commandBindLengthLimit;
                commandBindAddHistory = d.commandBindAddHistory;
                commandBindShowHud = d.commandBindShowHud;
            }
            case "qol.wardrobe_keybinds" -> {
                wardrobeKeybindsEnabled = d.wardrobeKeybindsEnabled;
                wardrobeNextKey = d.wardrobeNextKey;
                wardrobePreviousKey = d.wardrobePreviousKey;
                wardrobeUnequipKey = d.wardrobeUnequipKey;
                wardrobeDisableUnequip = d.wardrobeDisableUnequip;
                wardrobeCancelAll = d.wardrobeCancelAll;
                wardrobeOverrideKey = d.wardrobeOverrideKey;
                wardrobeCancelRender = d.wardrobeCancelRender;
                wardrobePingMs = d.wardrobePingMs;
                wardrobeUseHotbar = d.wardrobeUseHotbar;
                wardrobeKeybindStyle = d.wardrobeKeybindStyle;
                wardrobeSound = d.wardrobeSound;
                wardrobeCustom1 = d.wardrobeCustom1;
                wardrobeCustom2 = d.wardrobeCustom2;
                wardrobeCustom3 = d.wardrobeCustom3;
                wardrobeCustom4 = d.wardrobeCustom4;
                wardrobeCustom5 = d.wardrobeCustom5;
                wardrobeCustom6 = d.wardrobeCustom6;
                wardrobeCustom7 = d.wardrobeCustom7;
                wardrobeCustom8 = d.wardrobeCustom8;
                wardrobeCustom9 = d.wardrobeCustom9;
                wardrobeSwap = d.wardrobeSwap;
                wardrobeSwapKey = d.wardrobeSwapKey;
                wardrobeSwapSlotA = d.wardrobeSwapSlotA;
                wardrobeSwapSlotB = d.wardrobeSwapSlotB;
                wardrobeAutoClose = d.wardrobeAutoClose;
                wardrobeAutoEquip = d.wardrobeAutoEquip;
                wardrobeMoveEquip = d.wardrobeMoveEquip;
                wardrobeResetOpen = d.wardrobeResetOpen;
                wardrobeClickDelay = d.wardrobeClickDelay;
                wardrobeCloseDelay = d.wardrobeCloseDelay;
                wardrobeDelayVariance = d.wardrobeDelayVariance;
                wardrobeHudX = d.wardrobeHudX;
                wardrobeHudY = d.wardrobeHudY;
            }
            case "qol.loadout_keybinds" -> {
                loadoutKeybindsEnabled = d.loadoutKeybindsEnabled;
                loadoutNextKey = d.loadoutNextKey;
                loadoutPreviousKey = d.loadoutPreviousKey;
            }
            case "qol.chat_commands" -> {
                chatCommandsEnabled = d.chatCommandsEnabled;
                chatEmotes = d.chatEmotes;
                chatPartyCommands = d.chatPartyCommands;
                chatGuildCommands = d.chatGuildCommands;
                chatPrivateCommands = d.chatPrivateCommands;
                chatPreviousServer = d.chatPreviousServer;
                chatPreviousServerSeconds = d.chatPreviousServerSeconds;
                chatQueueEstimate = d.chatQueueEstimate;
                chatQuickJoin = d.chatQuickJoin;
                chatSlotMachine = d.chatSlotMachine;
                chatQuickJoinText = d.chatQuickJoinText;
                chatQuickJoinIp = d.chatQuickJoinIp;
                chatCommandsShortcuts = d.chatCommandsShortcuts;
                chatCommandsRules = d.chatCommandsRules;
                chatCommandsKeybind = d.chatCommandsKeybind;
            }
            case "qol.no_cursor_reset" -> {
                noCursorResetEnabled = d.noCursorResetEnabled;
                noCursorUnhookTimeoutMs = d.noCursorUnhookTimeoutMs;
                noCursorKeybind = d.noCursorKeybind;
            }
            case "qol.player_display" -> {
                playerDisplayEnabled = d.playerDisplayEnabled;
                playerDisplayShowIcons = d.playerDisplayShowIcons;
                playerDisplayShowLabels = d.playerDisplayShowLabels;
                playerDisplayShowMax = d.playerDisplayShowMax;
                playerDisplayHideVanillaHealth = d.playerDisplayHideVanillaHealth;
                playerDisplayHideVanillaFood = d.playerDisplayHideVanillaFood;
                playerDisplayHideVanillaArmor = d.playerDisplayHideVanillaArmor;
                playerDisplayHideVanillaXp = d.playerDisplayHideVanillaXp;
                playerDisplayHideActionHealth = d.playerDisplayHideActionHealth;
                playerDisplayHideActionDefense = d.playerDisplayHideActionDefense;
                playerDisplayHideActionMana = d.playerDisplayHideActionMana;
                playerDisplayHideActionOverflow = d.playerDisplayHideActionOverflow;
                playerDisplayHideActionSpeed = d.playerDisplayHideActionSpeed;
                playerDisplayHideActionVitality = d.playerDisplayHideActionVitality;
                playerDisplayHideActionLocation = d.playerDisplayHideActionLocation;
                playerDisplayHealthHud = d.playerDisplayHealthHud;
                playerDisplayManaHud = d.playerDisplayManaHud;
                playerDisplayOverflowManaHud = d.playerDisplayOverflowManaHud;
                playerDisplayDefenseHud = d.playerDisplayDefenseHud;
                playerDisplayVitalityHud = d.playerDisplayVitalityHud;
                playerDisplayEhpHud = d.playerDisplayEhpHud;
                playerDisplaySpeedHud = d.playerDisplaySpeedHud;
                playerDisplayHealthColor = d.playerDisplayHealthColor;
                playerDisplayManaColor = d.playerDisplayManaColor;
                playerDisplayOverflowManaColor = d.playerDisplayOverflowManaColor;
                playerDisplayDefenseColor = d.playerDisplayDefenseColor;
                playerDisplayVitalityColor = d.playerDisplayVitalityColor;
                playerDisplayEhpColor = d.playerDisplayEhpColor;
                playerDisplaySpeedColor = d.playerDisplaySpeedColor;
                playerDisplayKeybind = d.playerDisplayKeybind;
            }
            case "qol.pet_keybinds" -> {
                petKeybindsEnabled = d.petKeybindsEnabled;
                petUnequipKey = d.petUnequipKey;
                petNextKey = d.petNextKey;
                petPreviousKey = d.petPreviousKey;
                petDisableUnequip = d.petDisableUnequip;
                petCloseIfAlreadyEquipped = d.petCloseIfAlreadyEquipped;
                petModuleKeybind = d.petModuleKeybind;
            }
            case "qol.auto_sprint" -> autoSprintKeybind = d.autoSprintKeybind;
            case "qol.slot_binds" -> {
                slotBindsEnabled = d.slotBindsEnabled;
                slotBindSetKey = d.slotBindSetKey;
                slotBindColor = d.slotBindColor;
                slotBindLineWidth = d.slotBindLineWidth;
                slotBindLineDisplay = d.slotBindLineDisplay;
                slotBindProfile = d.slotBindProfile;
                slotBindProfiles = new LinkedHashMap<>();
            }
            case "qol.waypoints" -> {
                waypointsEnabled = d.waypointsEnabled;
                waypointsFromParty = d.waypointsFromParty;
                waypointsFromAll = d.waypointsFromAll;
                waypointsPersonal = d.waypointsPersonal;
                waypointsPingMode = d.waypointsPingMode;
                waypointsKeybind = d.waypointsKeybind;
            }
            case "qol.performance_hud" -> {
                performanceHudEnabled = d.performanceHudEnabled;
                performanceNameColor = d.performanceNameColor;
                performanceValueColor = d.performanceValueColor;
                performanceDirection = d.performanceDirection;
                performanceShowFps = d.performanceShowFps;
                performanceShowTps = d.performanceShowTps;
                performanceShowPing = d.performanceShowPing;
                performanceKeybind = d.performanceKeybind;
            }
            case "qol.render_optimizer" -> {
                renderOptimizerEnabled = d.renderOptimizerEnabled;
                hideFallingBlocks = d.hideFallingBlocks;
                hideLightning = d.hideLightning;
                hideExperienceOrbs = d.hideExperienceOrbs;
                hideDeathAnimation = d.hideDeathAnimation;
                hideArmorStands = d.hideArmorStands;
                hideExplosionParticles = d.hideExplosionParticles;
                hideArcherPassive = d.hideArcherPassive;
                hideHealerFairy = d.hideHealerFairy;
                hideSoulWeaver = d.hideSoulWeaver;
                hideTentacleHead = d.hideTentacleHead;
                hideFireOverlay = d.hideFireOverlay;
                renderOptimizerKeybind = d.renderOptimizerKeybind;
                extras().resetModule("qol.render_optimizer");
            }
            case "qol.hide_players" -> {
                hidePlayersEnabled = d.hidePlayersEnabled;
                hidePlayersOnlyDungeons = d.hidePlayersOnlyDungeons;
                hidePlayersHideAll = d.hidePlayersHideAll;
                hidePlayersDistance = d.hidePlayersDistance;
                hidePlayersKeybind = d.hidePlayersKeybind;
            }
            case "qol.player_size" -> {
                playerSizeEnabled = d.playerSizeEnabled;
                playerSizeX = d.playerSizeX;
                playerSizeY = d.playerSizeY;
                playerSizeZ = d.playerSizeZ;
                playerSizeKeybind = d.playerSizeKeybind;
                playerAnimalsEnabled = d.playerAnimalsEnabled;
                playerAnimalsScope = d.playerAnimalsScope;
                playerAnimalsSpecies = d.playerAnimalsSpecies;
                playerAnimalsBaby = d.playerAnimalsBaby;
                playerAnimalsCollarColor = d.playerAnimalsCollarColor;
            }
            case "qol.etherwarp" -> {
                etherwarpEnabled = d.etherwarpEnabled;
                etherwarpShowGuess = d.etherwarpShowGuess;
                etherwarpColor = d.etherwarpColor;
                etherwarpShowFailed = d.etherwarpShowFailed;
                etherwarpFailColor = d.etherwarpFailColor;
                etherwarpRenderStyle = d.etherwarpRenderStyle;
                etherwarpUseServerPosition = d.etherwarpUseServerPosition;
                etherwarpFullBlock = d.etherwarpFullBlock;
                etherwarpDepth = d.etherwarpDepth;
                etherwarpSounds = d.etherwarpSounds;
                etherwarpKeybind = d.etherwarpKeybind;
                etherwarpLeftClickWarp = d.etherwarpLeftClickWarp;
                etherwarpShiftAutomatically = d.etherwarpShiftAutomatically;
            }
            case "qol.camera" -> cameraKeybind = d.cameraKeybind;
            case "qol.click_gui" -> {
                clickGuiEnabled = d.clickGuiEnabled;
                clickGuiChatNotifications = d.clickGuiChatNotifications;
                clickGuiColor = d.clickGuiColor;
                clickGuiRoundedBottoms = d.clickGuiRoundedBottoms;
                clickGuiDeveloperMessage = d.clickGuiDeveloperMessage;
                clickGuiKeybind = d.clickGuiKeybind;
            }
            case "qol.auto_clicker" -> {
                autoClickerEnabled = d.autoClickerEnabled;
                autoClickerCpsHudEnabled = d.autoClickerCpsHudEnabled;
                autoClickerWhitelistOnly = d.autoClickerWhitelistOnly;
                autoClickerAllowBreaking = d.autoClickerAllowBreaking;
                autoClickerBlockBreaker = d.autoClickerBlockBreaker;
                autoClickerTerminatorOnly = d.autoClickerTerminatorOnly;
                autoClickerCps = d.autoClickerCps;
                autoClickerEnableLeft = d.autoClickerEnableLeft;
                autoClickerEnableRight = d.autoClickerEnableRight;
                autoClickerLeftCps = d.autoClickerLeftCps;
                autoClickerRightCps = d.autoClickerRightCps;
                autoClickerLeftKeybind = d.autoClickerLeftKeybind;
                autoClickerRightKeybind = d.autoClickerRightKeybind;
                autoClickerLeftWhitelist = new java.util.ArrayList<>(d.autoClickerLeftWhitelist);
                autoClickerRightWhitelist = new java.util.ArrayList<>(d.autoClickerRightWhitelist);
            }
            case "qol.inventory_walk" -> {
                inventoryWalkEnabled = d.inventoryWalkEnabled;
                inventoryWalkPingMs = d.inventoryWalkPingMs;
            }
            case "qol.inventory_overlay" -> {
                inventoryOverlayEnabled = d.inventoryOverlayEnabled;
                inventoryOverlayEquipment = d.inventoryOverlayEquipment;
                inventoryOverlayHideRecipeBook = d.inventoryOverlayHideRecipeBook;
                inventoryOverlayHideStatusEffects = d.inventoryOverlayHideStatusEffects;
                inventoryOverlayPetSlot = d.inventoryOverlayPetSlot;
                inventoryOverlayProtectDrops = d.inventoryOverlayProtectDrops;
                inventoryOverlayProtectSalvage = d.inventoryOverlayProtectSalvage;
                inventoryOverlayProtectList = d.inventoryOverlayProtectList;
                inventoryOverlayPetOffsetX = d.inventoryOverlayPetOffsetX;
                inventoryOverlayPetOffsetY = d.inventoryOverlayPetOffsetY;
                inventoryChromePanel = d.inventoryChromePanel;
                inventoryChromeHeader = d.inventoryChromeHeader;
                inventoryChromeMain = d.inventoryChromeMain;
                inventoryChromeHotbar = d.inventoryChromeHotbar;
                inventoryChromeBorder = d.inventoryChromeBorder;
            }
            case "qol.skill_levels" -> {
                skillLevelsEnabled = d.skillLevelsEnabled;
                skillLevelsColor = d.skillLevelsColor;
                skillLevelsMaxColor = d.skillLevelsMaxColor;
            }
            case "qol.pet_hud" -> {
                petHudEnabled = d.petHudEnabled;
                petHudX = d.petHudX;
                petHudY = d.petHudY;
            }
            case "qol.name_hider" -> {
                nameHiderEnabled = d.nameHiderEnabled;
                nameHiderMode = d.nameHiderMode;
                nameHiderCustomName = d.nameHiderCustomName;
            }
            case "qol.trajectories" -> {
                trajectoriesEnabled = d.trajectoriesEnabled;
                trajectoriesBows = d.trajectoriesBows;
                trajectoriesPearls = d.trajectoriesPearls;
                trajectoriesBoxes = d.trajectoriesBoxes;
                trajectoriesLines = d.trajectoriesLines;
                trajectoriesDepth = d.trajectoriesDepth;
                trajectoriesRange = d.trajectoriesRange;
                trajectoriesWidth = d.trajectoriesWidth;
                trajectoriesBoxSize = d.trajectoriesBoxSize;
                trajectoriesColor = d.trajectoriesColor;
                trajectoriesPlane = d.trajectoriesPlane;
                trajectoriesEntities = d.trajectoriesEntities;
                trajectoriesPlaneSize = d.trajectoriesPlaneSize;
            }
            case "qol.secret_hitboxes" -> {
                secretHitboxesEnabled = d.secretHitboxesEnabled;
                secretHitboxesOnlyDungeons = d.secretHitboxesOnlyDungeons;
                secretHitboxesLever = d.secretHitboxesLever;
                secretHitboxesOldLever = d.secretHitboxesOldLever;
                secretHitboxesButton = d.secretHitboxesButton;
                secretHitboxesFlatButton = d.secretHitboxesFlatButton;
                secretHitboxesSkull = d.secretHitboxesSkull;
                secretHitboxesChests = d.secretHitboxesChests;
                secretHitboxesOnlyTrappedChests = d.secretHitboxesOnlyTrappedChests;
            }
            case "qol.world_scanner" -> {
                worldScannerEnabled = d.worldScannerEnabled;
                worldScannerOnlyHollows = d.worldScannerOnlyHollows;
                worldScannerCrystals = d.worldScannerCrystals;
                worldScannerMobSpots = d.worldScannerMobSpots;
                worldScannerFairyGrottos = d.worldScannerFairyGrottos;
                worldScannerDragonNest = d.worldScannerDragonNest;
                worldScannerWormFishing = d.worldScannerWormFishing;
                worldScannerLavaEsp = d.worldScannerLavaEsp;
                worldScannerWaterEsp = d.worldScannerWaterEsp;
                worldScannerRatHitboxes = d.worldScannerRatHitboxes;
                worldScannerChatCoords = d.worldScannerChatCoords;
                worldScannerEspRange = d.worldScannerEspRange;
                worldScannerTargets = WorldScannerEspSettings.defaults();
            }
            case "qol.auto_conversation" -> {
                autoConversationEnabled = d.autoConversationEnabled;
                autoConversationMulti = d.autoConversationMulti;
                autoConversationGreen = d.autoConversationGreen;
                autoConversationDelayTicks = d.autoConversationDelayTicks;
            }
            case "qol.fishing_helper" -> {
                fishingHelperEnabled = d.fishingHelperEnabled;
                fishingHelperAutoPull = d.fishingHelperAutoPull;
                fishingHelperPullDelay = d.fishingHelperPullDelay;
                fishingHelperPullVariance = d.fishingHelperPullVariance;
                fishingHelperRecast = d.fishingHelperRecast;
                fishingHelperRecastCheck = d.fishingHelperRecastCheck;
                fishingHelperRecastDelay = d.fishingHelperRecastDelay;
                fishingHelperRecastVariance = d.fishingHelperRecastVariance;
                fishingHelperBobberTimer = d.fishingHelperBobberTimer;
                fishingHelperBiteTitle = d.fishingHelperBiteTitle;
                fishingHelperBiteSound = d.fishingHelperBiteSound;
                fishingHelperHookTimerHud = d.fishingHelperHookTimerHud;
                fishingHelperHideHookNametag = d.fishingHelperHideHookNametag;
                fishingHudX = d.fishingHudX;
                fishingHudY = d.fishingHudY;
            }
            case "qol.missing_enchants" -> {
                missingEnchantsEnabled = d.missingEnchantsEnabled;
                missingEnchantsKeybind = d.missingEnchantsKeybind;
                missingEnchantsShowUpgradable = d.missingEnchantsShowUpgradable;
                missingEnchantsShowConflicting = d.missingEnchantsShowConflicting;
            }
            case "qol.commission_display" -> {
                commissionDisplayEnabled = d.commissionDisplayEnabled;
                commissionDisplayTitle = d.commissionDisplayTitle;
                commissionDisplayNone = d.commissionDisplayNone;
                commissionDisplayRow = d.commissionDisplayRow;
                commissionDisplayColoredPercent = d.commissionDisplayColoredPercent;
                commissionHudX = d.commissionHudX;
                commissionHudY = d.commissionHudY;
            }
            case "qol.item_rarity" -> {
                itemRarityEnabled = d.itemRarityEnabled;
                itemRarityStyle = d.itemRarityStyle;
                itemRarityHotbar = d.itemRarityHotbar;
                itemRarityFillAlpha = d.itemRarityFillAlpha;
                itemRarityOutlineAlpha = d.itemRarityOutlineAlpha;
                itemRarityCommon = d.itemRarityCommon;
                itemRarityUncommon = d.itemRarityUncommon;
                itemRarityRare = d.itemRarityRare;
                itemRarityEpic = d.itemRarityEpic;
                itemRarityLegendary = d.itemRarityLegendary;
                itemRarityMythic = d.itemRarityMythic;
                itemRarityDivine = d.itemRarityDivine;
                itemRaritySpecial = d.itemRaritySpecial;
            }
            case "qol.mob_highlight" -> {
                mobHighlightEnabled = d.mobHighlightEnabled;
                mobHighlightRequireKey = d.mobHighlightRequireKey;
                mobHighlightAddKey = d.mobHighlightAddKey;
                mobHighlightDepth = d.mobHighlightDepth;
                mobHighlightTracers = d.mobHighlightTracers;
                mobHighlightColor = d.mobHighlightColor;
                mobHighlightNames = new java.util.ArrayList<>();
            }
            case "qol.item_tooltips" -> {
                missingEnchantsEnabled = d.missingEnchantsEnabled;
                missingEnchantsKeybind = d.missingEnchantsKeybind;
                missingEnchantsShowUpgradable = d.missingEnchantsShowUpgradable;
                missingEnchantsShowConflicting = d.missingEnchantsShowConflicting;
                customTooltipEnabled = d.customTooltipEnabled;
                customTooltipInfinite = d.customTooltipInfinite;
                customTooltipHorizontal = d.customTooltipHorizontal;
                customTooltipHorizontalKey = d.customTooltipHorizontalKey;
                customTooltipHorizontalSpeed = d.customTooltipHorizontalSpeed;
                customTooltipVertical = d.customTooltipVertical;
                customTooltipVerticalSpeed = d.customTooltipVerticalSpeed;
                customTooltipReset = d.customTooltipReset;
                customTooltipStyle = d.customTooltipStyle;
                customTooltipCenteredHeader = d.customTooltipCenteredHeader;
                customTooltipBorder = d.customTooltipBorder;
                customTooltipBorderWidth = d.customTooltipBorderWidth;
                customTooltipRarityBorder = d.customTooltipRarityBorder;
                customTooltipBorderColor = d.customTooltipBorderColor;
                customTooltipBackground = d.customTooltipBackground;
                customTooltipBackgroundColor = d.customTooltipBackgroundColor;
                customTooltipOnlyNameKey = d.customTooltipOnlyNameKey;
                customTooltipShadows = d.customTooltipShadows;
                extras().resetModule("qol.info_tooltips");
                extras().resetModule("qol.price_tooltips");
            }
            case "qol.custom_tooltip" -> {
                customTooltipEnabled = d.customTooltipEnabled;
                customTooltipInfinite = d.customTooltipInfinite;
                customTooltipHorizontal = d.customTooltipHorizontal;
                customTooltipHorizontalKey = d.customTooltipHorizontalKey;
                customTooltipHorizontalSpeed = d.customTooltipHorizontalSpeed;
                customTooltipVertical = d.customTooltipVertical;
                customTooltipVerticalSpeed = d.customTooltipVerticalSpeed;
                customTooltipReset = d.customTooltipReset;
                customTooltipStyle = d.customTooltipStyle;
                customTooltipCenteredHeader = d.customTooltipCenteredHeader;
                customTooltipBorder = d.customTooltipBorder;
                customTooltipBorderWidth = d.customTooltipBorderWidth;
                customTooltipRarityBorder = d.customTooltipRarityBorder;
                customTooltipBorderColor = d.customTooltipBorderColor;
                customTooltipBackground = d.customTooltipBackground;
                customTooltipBackgroundColor = d.customTooltipBackgroundColor;
                customTooltipOnlyNameKey = d.customTooltipOnlyNameKey;
                customTooltipShadows = d.customTooltipShadows;
            }
            default -> {
                return extras().resetModule(moduleId);
            }
        }
        return true;
    }

    void ensureWorldScannerTargets() {
        Map<String, WorldScannerEspSettings.Target> defaults = WorldScannerEspSettings.defaults();
        if (worldScannerTargets == null) {
            worldScannerTargets = defaults;
            return;
        }
        for (WorldScannerEspSettings.Spec spec : WorldScannerEspSettings.TARGETS) {
            worldScannerTargets.putIfAbsent(spec.id(), defaults.get(spec.id()));
        }
    }

    WorldScannerEspSettings.Target worldScannerTarget(String id) {
        ensureWorldScannerTargets();
        return worldScannerTargets.computeIfAbsent(
                id, key -> new WorldScannerEspSettings.Target(WorldScannerEspSettings.defaultColor(key)));
    }

    private Boolean readWorldScannerTargetBoolean(String settingId) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null) {
            return null;
        }
        WorldScannerEspSettings.Target target = worldScannerTarget(parts[0]);
        return switch (parts[1]) {
            case "enabled" -> target.enabled;
            case "tracer" -> target.tracer;
            case "name" -> target.displayName;
            case "chat" -> target.chatCoords;
            case "notify" -> target.notify;
            default -> null;
        };
    }

    private boolean writeWorldScannerTargetBoolean(String settingId, boolean value) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null) {
            return false;
        }
        WorldScannerEspSettings.Target target = worldScannerTarget(parts[0]);
        switch (parts[1]) {
            case "enabled" -> {
                target.enabled = value;
                if ("fairy".equals(parts[0])) {
                    worldScannerFairyGrottos = value;
                } else if ("dragon".equals(parts[0])) {
                    worldScannerDragonNest = value;
                } else if ("worm".equals(parts[0])) {
                    worldScannerWormFishing = value;
                }
            }
            case "tracer" -> target.tracer = value;
            case "name" -> target.displayName = value;
            case "chat" -> target.chatCoords = value;
            case "notify" -> target.notify = value;
            default -> {
                return false;
            }
        }
        return true;
    }

    private String readWorldScannerTargetEnum(String settingId) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null || !"style".equals(parts[1])) {
            return null;
        }
        return WorldScannerEspSettings.normalizeStyle(worldScannerTarget(parts[0]).highlightStyle);
    }

    private boolean writeWorldScannerTargetEnum(String settingId, String value) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null || !"style".equals(parts[1])) {
            return false;
        }
        worldScannerTarget(parts[0]).highlightStyle = WorldScannerEspSettings.normalizeStyle(value);
        return true;
    }

    private Double readWorldScannerTargetNumber(String settingId) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null) {
            return null;
        }
        WorldScannerEspSettings.Target target = worldScannerTarget(parts[0]);
        return switch (parts[1]) {
            case "name_scale" -> (double) target.nameScale;
            case "opacity" -> (double) target.backgroundOpacity;
            default -> null;
        };
    }

    private boolean writeWorldScannerTargetNumber(String settingId, double value) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null) {
            return false;
        }
        WorldScannerEspSettings.Target target = worldScannerTarget(parts[0]);
        switch (parts[1]) {
            case "name_scale" -> target.nameScale = (float) clamp(value, 0.5D, 2.0D);
            case "opacity" -> target.backgroundOpacity = (float) clamp(value, 0.0D, 1.0D);
            default -> {
                return false;
            }
        }
        return true;
    }

    private Integer readWorldScannerTargetColor(String settingId) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null || !"color".equals(parts[1])) {
            return null;
        }
        return worldScannerTarget(parts[0]).colorArgb;
    }

    private boolean writeWorldScannerTargetColor(String settingId, int argb) {
        String[] parts = WorldScannerEspSettings.splitSetting(settingId);
        if (parts == null || !"color".equals(parts[1])) {
            return false;
        }
        worldScannerTarget(parts[0]).colorArgb = argb;
        return true;
    }

    private static float clampScale(float value) {
        if (!Float.isFinite(value)) {
            return 1.0F;
        }
        return Math.max(0.5F, Math.min(2.5F, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
