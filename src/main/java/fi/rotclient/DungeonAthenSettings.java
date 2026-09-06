package fi.rotclient;

/**
 * Nested extras for Athen/Nebulune dungeon screenshot modules.
 * Gson persists this object on {@link QolSkyblockExtras}.
 */
final class DungeonAthenSettings {
    int superboomMinDelay = 1;
    int superboomMaxDelay = 3;
    int superboomSwapBackMin = 1;
    int superboomSwapBackMax = 3;
    String superboomSwapTo = "Original slot";
    int superboomCustomSlot = 1;
    String superboomExtraBlocks = "";
    boolean breakerInstamine;

    int closeChestMinDelay;
    int closeChestMaxDelay = 1;

    String qualityStyle = DungeonAthenPortPolicy.DEFAULT_QUALITY_STYLE;

    int termMinDelayMs = 80;
    int termMaxDelayMs = 160;
    String termOrder = "Closest";
    int termResyncTimeout = 800;
    int termFirstClickDelay = 350;
    boolean termDropKey;
    String termKeybindLeft = "";
    String termKeybindRight = "";
    boolean termRubixLeftOnly;
    String termMelodyKey1 = "1";
    String termMelodyKey2 = "2";
    String termMelodyKey3 = "3";
    String termMelodyKey4 = "4";
    double termUiScale = 1.0D;
    double termUiRoundness;
    double termUiPadding = 5.0D;
    double termUiSlotGap = 2.0D;
    double termUiMelodyGap = 2.0D;
    int termUiBgColor = 0xE0000000;
    int termUiBorderColor = 0xFF9B59B6;
    boolean termSlotsFill;
    double termSlotsRoundness;
    boolean termNumbersShowText = true;
    boolean termHideHeader;
    boolean termHideTitle;
    int termHeaderColor = 0xE0101018;
    String termClickSound = "block.note_block.pling";
    double termClickPitch = 1.0D;
    double termClickVolume = 1.0D;
    int termColorsSolutionColor = 0x8000FF00;
    int termNamesSolutionColor = 0x8000FF00;
    int termPanesSolutionColor = 0x8000FF00;
    int termMelodyFillColor = 0x80C084FC;
    int termMelodyOtherColor = 0x80101018;
    boolean termCheckClass;
    boolean termRenderText = true;
    boolean termDepthTest = true;
    String termHighlightStyle = "Outline";
    int termWaypointColor = 0xFF22D3EE;
    int termLeverColor = 0xFFFACC15;
    String s1t1 = "Tank";
    String s1t2 = "Tank";
    String s1t3 = "Mage";
    String s1t4 = "Mage";
    String s1right = "Archer";
    String s1left = "Archer";
    String s2t1 = "Tank";
    String s2t2 = "Mage";
    String s2t3 = "Berserk";
    String s2t4 = "Archer";
    String s2t5 = "Berserk";
    String s2right = "Archer";
    String s2left = "Healer";
    String s3t1 = "Tank";
    String s3t2 = "Healer";
    String s3t3 = "Berserk";
    String s3t4 = "Archer";
    String s3right = "Archer";
    String s3left = "Archer";
    String s4t1 = "Tank";
    String s4t2 = "Archer";
    String s4t3 = "Berserk";
    String s4t4 = "Healer";
    String s4right = "Healer";
    String s4left = "Healer";

    String termSimIp = "";

    boolean pfShowStats;
    boolean pfStack = true;
    boolean pfHighlight = true;
    int pfJoinableColor = 0x8022C55E;
    int pfDupeColor = 0x80FACC15;
    int pfBlockedColor = 0x80EF4444;
    int pfVcColor = 0x80A855F7;
    int pfPermColor = 0x8022D3EE;
    int pfCarryColor = 0x807F1D1D;

    boolean carryEnabled;
    boolean carryAnnounceParty = true;
    boolean carryShowStart = true;
    boolean carryWebhook;
    boolean carryWebhookEach;
    String carryWebhookUrl = "";
    boolean carryHighlight = true;
    int carryPlayerColor = 0xFF22D3EE;
    double carryLineWidth = 2.0D;
    boolean carryDisplay = true;
    boolean carryOnlyDungeons = true;
    String carryState = "";

    boolean hoverEnabled;
    int hoverMinDelay = 50;
    int hoverMaxDelay = 120;

    boolean partyJoinEnabled;
    boolean partyJoinStats = true;
    boolean partyJoinAutoKick;
    boolean partyJoinDetectFloor = true;
    String partyJoinRequiredPb = "5:30";
    String partyJoinRequiredSecrets = "50k";
    String partyJoinRequiredSecretAvg = "8.4";
    String partyJoinRequiredMp = "800";
    boolean partyJoinKickMessage = true;
    boolean partyJoinSendParty = true;
    int partyJoinMessageDelay = 5;

    boolean soulsandEnabled;

    boolean termClickEnabled;
    int termClickRadius = 4;
    int termClickThickness = 2;
    int termClickLeftColor = 0xFFC084FC;
    int termClickRightColor = 0xFFFDBA74;

    boolean watcherEnabled;
    boolean watcherBreakdown = true;
    boolean watcherSpawnedAll = true;
    boolean watcherSpeak = true;
    boolean watcherMove = true;
    boolean watcherBloodTimers = true;
    boolean watcherShowTicks = true;
    String watcherTextFast = "<red>Vroom!";
    String watcherTextNormal = "<red>Watcher!";
    String watcherTextSlow = "<red>Yawn...";
    String watcherTextVerySlow = "<red>Zzz...";
    float carryHudX = 12.0F;
    float carryHudY = 148.0F;
    float watcherHudX = 12.0F;
    float watcherHudY = 228.0F;

    Boolean readBoolean(String id) {
        return switch (id) {
            case "qol.dungeon_f7.breaker_instamine" -> breakerInstamine;
            case "qol.dungeon_terminals.drop_key" -> termDropKey;
            case "qol.dungeon_terminals.rubix_left_only" -> termRubixLeftOnly;
            case "qol.dungeon_terminals.slots_fill" -> termSlotsFill;
            case "qol.dungeon_terminals.numbers_show_text" -> termNumbersShowText;
            case "qol.dungeon_terminals.hide_header" -> termHideHeader;
            case "qol.dungeon_terminals.hide_title" -> termHideTitle;
            case "qol.dungeon_terminals.check_class" -> termCheckClass;
            case "qol.dungeon_terminals.render_text" -> termRenderText;
            case "qol.dungeon_terminals.depth_test" -> termDepthTest;
            case "qol.dungeon_menus.pf_stats" -> pfShowStats;
            case "qol.dungeon_menus.pf_stack" -> pfStack;
            case "qol.dungeon_menus.pf_highlight" -> pfHighlight;
            case "qol.dungeon_carry" -> carryEnabled;
            case "qol.dungeon_carry.announce_party" -> carryAnnounceParty;
            case "qol.dungeon_carry.show_start" -> carryShowStart;
            case "qol.dungeon_carry.webhook" -> carryWebhook;
            case "qol.dungeon_carry.webhook_each" -> carryWebhookEach;
            case "qol.dungeon_carry.highlight" -> carryHighlight;
            case "qol.dungeon_carry.display" -> carryDisplay;
            case "qol.dungeon_carry.only_dungeons" -> carryOnlyDungeons;
            case "qol.dungeon_hover_terms" -> hoverEnabled;
            case "qol.dungeon_party_join" -> partyJoinEnabled;
            case "qol.dungeon_party_join.stats" -> partyJoinStats;
            case "qol.dungeon_party_join.auto_kick" -> partyJoinAutoKick;
            case "qol.dungeon_party_join.detect_floor" -> partyJoinDetectFloor;
            case "qol.dungeon_party_join.kick_message" -> partyJoinKickMessage;
            case "qol.dungeon_party_join.send_party" -> partyJoinSendParty;
            case "qol.dungeon_soulsand" -> soulsandEnabled;
            case "qol.dungeon_term_click" -> termClickEnabled;
            case "qol.dungeon_watcher" -> watcherEnabled;
            case "qol.dungeon_watcher.breakdown" -> watcherBreakdown;
            case "qol.dungeon_watcher.spawned_all" -> watcherSpawnedAll;
            case "qol.dungeon_watcher.speak" -> watcherSpeak;
            case "qol.dungeon_watcher.move" -> watcherMove;
            case "qol.dungeon_watcher.blood_timers" -> watcherBloodTimers;
            case "qol.dungeon_watcher.show_ticks" -> watcherShowTicks;
            default -> null;
        };
    }

    boolean writeBoolean(String id, boolean value) {
        switch (id) {
            case "qol.dungeon_f7.breaker_instamine" -> breakerInstamine = value;
            case "qol.dungeon_terminals.drop_key" -> termDropKey = value;
            case "qol.dungeon_terminals.rubix_left_only" -> termRubixLeftOnly = value;
            case "qol.dungeon_terminals.slots_fill" -> termSlotsFill = value;
            case "qol.dungeon_terminals.numbers_show_text" -> termNumbersShowText = value;
            case "qol.dungeon_terminals.hide_header" -> termHideHeader = value;
            case "qol.dungeon_terminals.hide_title" -> termHideTitle = value;
            case "qol.dungeon_terminals.check_class" -> termCheckClass = value;
            case "qol.dungeon_terminals.render_text" -> termRenderText = value;
            case "qol.dungeon_terminals.depth_test" -> termDepthTest = value;
            case "qol.dungeon_menus.pf_stats" -> pfShowStats = value;
            case "qol.dungeon_menus.pf_stack" -> pfStack = value;
            case "qol.dungeon_menus.pf_highlight" -> pfHighlight = value;
            case "qol.dungeon_carry.announce_party" -> carryAnnounceParty = value;
            case "qol.dungeon_carry.show_start" -> carryShowStart = value;
            case "qol.dungeon_carry.webhook" -> carryWebhook = value;
            case "qol.dungeon_carry.webhook_each" -> carryWebhookEach = value;
            case "qol.dungeon_carry.highlight" -> carryHighlight = value;
            case "qol.dungeon_carry.display" -> carryDisplay = value;
            case "qol.dungeon_carry.only_dungeons" -> carryOnlyDungeons = value;
            case "qol.dungeon_party_join.stats" -> partyJoinStats = value;
            case "qol.dungeon_party_join.auto_kick" -> partyJoinAutoKick = value;
            case "qol.dungeon_party_join.detect_floor" -> partyJoinDetectFloor = value;
            case "qol.dungeon_party_join.kick_message" -> partyJoinKickMessage = value;
            case "qol.dungeon_party_join.send_party" -> partyJoinSendParty = value;
            case "qol.dungeon_watcher.breakdown" -> watcherBreakdown = value;
            case "qol.dungeon_watcher.spawned_all" -> watcherSpawnedAll = value;
            case "qol.dungeon_watcher.speak" -> watcherSpeak = value;
            case "qol.dungeon_watcher.move" -> watcherMove = value;
            case "qol.dungeon_watcher.blood_timers" -> watcherBloodTimers = value;
            case "qol.dungeon_watcher.show_ticks" -> watcherShowTicks = value;
            default -> {
                return false;
            }
        }
        return true;
    }

    boolean setModuleEnabled(String id, boolean enabled) {
        return switch (id) {
            case "qol.dungeon_carry" -> {
                carryEnabled = enabled;
                yield true;
            }
            case "qol.dungeon_hover_terms" -> {
                hoverEnabled = enabled;
                yield true;
            }
            case "qol.dungeon_party_join" -> {
                partyJoinEnabled = enabled;
                yield true;
            }
            case "qol.dungeon_soulsand" -> {
                soulsandEnabled = enabled;
                yield true;
            }
            case "qol.dungeon_term_click" -> {
                termClickEnabled = enabled;
                yield true;
            }
            case "qol.dungeon_watcher" -> {
                watcherEnabled = enabled;
                yield true;
            }
            default -> false;
        };
    }

    Double readNumber(String id) {
        return switch (id) {
            case "qol.dungeon_f7.superboom_min_delay" -> (double) superboomMinDelay;
            case "qol.dungeon_f7.superboom_max_delay" -> (double) superboomMaxDelay;
            case "qol.dungeon_f7.superboom_swap_back_min" -> (double) superboomSwapBackMin;
            case "qol.dungeon_f7.superboom_swap_back_max" -> (double) superboomSwapBackMax;
            case "qol.dungeon_f7.superboom_custom_slot" -> (double) superboomCustomSlot;
            case "qol.dungeon_menus.close_chest_min" -> (double) closeChestMinDelay;
            case "qol.dungeon_menus.close_chest_max" -> (double) closeChestMaxDelay;
            case "qol.dungeon_terminals.min_delay_ms" -> (double) termMinDelayMs;
            case "qol.dungeon_terminals.max_delay_ms" -> (double) termMaxDelayMs;
            case "qol.dungeon_terminals.resync_timeout" -> (double) termResyncTimeout;
            case "qol.dungeon_terminals.first_click_delay" -> (double) termFirstClickDelay;
            case "qol.dungeon_terminals.ui_scale" -> termUiScale;
            case "qol.dungeon_terminals.ui_roundness" -> termUiRoundness;
            case "qol.dungeon_terminals.ui_padding" -> termUiPadding;
            case "qol.dungeon_terminals.ui_slot_gap" -> termUiSlotGap;
            case "qol.dungeon_terminals.ui_melody_gap" -> termUiMelodyGap;
            case "qol.dungeon_terminals.slots_roundness" -> termSlotsRoundness;
            case "qol.dungeon_terminals.click_pitch" -> termClickPitch;
            case "qol.dungeon_terminals.click_volume" -> termClickVolume;
            case "qol.dungeon_carry.line_width" -> carryLineWidth;
            case "qol.dungeon_hover_terms.min_delay" -> (double) hoverMinDelay;
            case "qol.dungeon_hover_terms.max_delay" -> (double) hoverMaxDelay;
            case "qol.dungeon_party_join.message_delay" -> (double) partyJoinMessageDelay;
            case "qol.dungeon_term_click.radius" -> (double) termClickRadius;
            case "qol.dungeon_term_click.thickness" -> (double) termClickThickness;
            default -> null;
        };
    }

    boolean writeNumber(String id, double value) {
        int rounded = (int) Math.round(value);
        switch (id) {
            case "qol.dungeon_f7.superboom_min_delay" ->
                    superboomMinDelay = DungeonAthenPortPolicy.clampSuperboomDelay(rounded);
            case "qol.dungeon_f7.superboom_max_delay" ->
                    superboomMaxDelay = DungeonAthenPortPolicy.clampSuperboomDelay(rounded);
            case "qol.dungeon_f7.superboom_swap_back_min" ->
                    superboomSwapBackMin = DungeonAthenPortPolicy.clampSuperboomDelay(rounded);
            case "qol.dungeon_f7.superboom_swap_back_max" ->
                    superboomSwapBackMax = DungeonAthenPortPolicy.clampSuperboomDelay(rounded);
            case "qol.dungeon_f7.superboom_custom_slot" ->
                    superboomCustomSlot = DungeonAthenPortPolicy.clampSlot(rounded);
            case "qol.dungeon_menus.close_chest_min" ->
                    closeChestMinDelay = DungeonAthenPortPolicy.clampChestDelay(rounded);
            case "qol.dungeon_menus.close_chest_max" ->
                    closeChestMaxDelay = DungeonAthenPortPolicy.clampChestDelay(rounded);
            case "qol.dungeon_terminals.min_delay_ms" ->
                    termMinDelayMs = DungeonAthenPortPolicy.clampTermDelayMs(rounded);
            case "qol.dungeon_terminals.max_delay_ms" ->
                    termMaxDelayMs = DungeonAthenPortPolicy.clampTermDelayMs(rounded);
            case "qol.dungeon_terminals.resync_timeout" ->
                    termResyncTimeout = DungeonAthenPortPolicy.clampResyncMs(rounded);
            case "qol.dungeon_terminals.first_click_delay" ->
                    termFirstClickDelay = DungeonAthenPortPolicy.clampFirstClickMs(rounded);
            case "qol.dungeon_terminals.ui_scale" ->
                    termUiScale = clamp(value, 0.5D, 2.0D);
            case "qol.dungeon_terminals.ui_roundness" ->
                    termUiRoundness = clamp(value, 0.0D, 20.0D);
            case "qol.dungeon_terminals.ui_padding" ->
                    termUiPadding = clamp(value, 0.0D, 20.0D);
            case "qol.dungeon_terminals.ui_slot_gap" ->
                    termUiSlotGap = clamp(value, 0.0D, 12.0D);
            case "qol.dungeon_terminals.ui_melody_gap" ->
                    termUiMelodyGap = clamp(value, 0.0D, 12.0D);
            case "qol.dungeon_terminals.slots_roundness" ->
                    termSlotsRoundness = clamp(value, 0.0D, 20.0D);
            case "qol.dungeon_terminals.click_pitch" ->
                    termClickPitch = clamp(value, 0.0D, 2.0D);
            case "qol.dungeon_terminals.click_volume" ->
                    termClickVolume = clamp(value, 0.0D, 1.0D);
            case "qol.dungeon_carry.line_width" ->
                    carryLineWidth = clamp(value, 0.5D, 6.0D);
            case "qol.dungeon_hover_terms.min_delay" ->
                    hoverMinDelay = DungeonAthenPortPolicy.clampHoverDelayMs(rounded);
            case "qol.dungeon_hover_terms.max_delay" ->
                    hoverMaxDelay = DungeonAthenPortPolicy.clampHoverDelayMs(rounded);
            case "qol.dungeon_party_join.message_delay" ->
                    partyJoinMessageDelay = Math.max(0, Math.min(40, rounded));
            case "qol.dungeon_term_click.radius" ->
                    termClickRadius = Math.max(1, Math.min(16, rounded));
            case "qol.dungeon_term_click.thickness" ->
                    termClickThickness = Math.max(1, Math.min(8, rounded));
            default -> {
                return false;
            }
        }
        return true;
    }

    String readEnum(String id) {
        return switch (id) {
            case "qol.dungeon_f7.superboom_swap_to" ->
                    DungeonAthenPortPolicy.normalizeSwapTo(superboomSwapTo);
            case "qol.dungeon_terminals.order" ->
                    DungeonAthenPortPolicy.normalizeTermOrder(termOrder);
            case "qol.dungeon_terminals.highlight_style" ->
                    DungeonAthenPortPolicy.normalizeHighlightStyle(termHighlightStyle);
            case "qol.dungeon_terminals.s1_term_1" -> DungeonAthenPortPolicy.normalizeDungeonClass(s1t1);
            case "qol.dungeon_terminals.s1_term_2" -> DungeonAthenPortPolicy.normalizeDungeonClass(s1t2);
            case "qol.dungeon_terminals.s1_term_3" -> DungeonAthenPortPolicy.normalizeDungeonClass(s1t3);
            case "qol.dungeon_terminals.s1_term_4" -> DungeonAthenPortPolicy.normalizeDungeonClass(s1t4);
            case "qol.dungeon_terminals.s1_right_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s1right);
            case "qol.dungeon_terminals.s1_left_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s1left);
            case "qol.dungeon_terminals.s2_term_1" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2t1);
            case "qol.dungeon_terminals.s2_term_2" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2t2);
            case "qol.dungeon_terminals.s2_term_3" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2t3);
            case "qol.dungeon_terminals.s2_term_4" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2t4);
            case "qol.dungeon_terminals.s2_term_5" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2t5);
            case "qol.dungeon_terminals.s2_right_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2right);
            case "qol.dungeon_terminals.s2_left_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s2left);
            case "qol.dungeon_terminals.s3_term_1" -> DungeonAthenPortPolicy.normalizeDungeonClass(s3t1);
            case "qol.dungeon_terminals.s3_term_2" -> DungeonAthenPortPolicy.normalizeDungeonClass(s3t2);
            case "qol.dungeon_terminals.s3_term_3" -> DungeonAthenPortPolicy.normalizeDungeonClass(s3t3);
            case "qol.dungeon_terminals.s3_term_4" -> DungeonAthenPortPolicy.normalizeDungeonClass(s3t4);
            case "qol.dungeon_terminals.s3_right_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s3right);
            case "qol.dungeon_terminals.s3_left_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s3left);
            case "qol.dungeon_terminals.s4_term_1" -> DungeonAthenPortPolicy.normalizeDungeonClass(s4t1);
            case "qol.dungeon_terminals.s4_term_2" -> DungeonAthenPortPolicy.normalizeDungeonClass(s4t2);
            case "qol.dungeon_terminals.s4_term_3" -> DungeonAthenPortPolicy.normalizeDungeonClass(s4t3);
            case "qol.dungeon_terminals.s4_term_4" -> DungeonAthenPortPolicy.normalizeDungeonClass(s4t4);
            case "qol.dungeon_terminals.s4_right_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s4right);
            case "qol.dungeon_terminals.s4_left_lever" -> DungeonAthenPortPolicy.normalizeDungeonClass(s4left);
            default -> null;
        };
    }

    boolean writeEnum(String id, String value) {
        switch (id) {
            case "qol.dungeon_f7.superboom_swap_to" ->
                    superboomSwapTo = DungeonAthenPortPolicy.normalizeSwapTo(value);
            case "qol.dungeon_terminals.order" ->
                    termOrder = DungeonAthenPortPolicy.normalizeTermOrder(value);
            case "qol.dungeon_terminals.highlight_style" ->
                    termHighlightStyle = DungeonAthenPortPolicy.normalizeHighlightStyle(value);
            case "qol.dungeon_terminals.s1_term_1" -> s1t1 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s1_term_2" -> s1t2 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s1_term_3" -> s1t3 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s1_term_4" -> s1t4 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s1_right_lever" -> s1right = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s1_left_lever" -> s1left = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_term_1" -> s2t1 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_term_2" -> s2t2 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_term_3" -> s2t3 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_term_4" -> s2t4 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_term_5" -> s2t5 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_right_lever" -> s2right = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s2_left_lever" -> s2left = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s3_term_1" -> s3t1 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s3_term_2" -> s3t2 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s3_term_3" -> s3t3 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s3_term_4" -> s3t4 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s3_right_lever" -> s3right = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s3_left_lever" -> s3left = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s4_term_1" -> s4t1 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s4_term_2" -> s4t2 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s4_term_3" -> s4t3 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s4_term_4" -> s4t4 = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s4_right_lever" -> s4right = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            case "qol.dungeon_terminals.s4_left_lever" -> s4left = DungeonAthenPortPolicy.normalizeDungeonClass(value);
            default -> {
                return false;
            }
        }
        return true;
    }

    Integer readColor(String id) {
        return switch (id) {
            case "qol.dungeon_terminals.ui_bg" -> termUiBgColor;
            case "qol.dungeon_terminals.ui_border" -> termUiBorderColor;
            case "qol.dungeon_terminals.header_color" -> termHeaderColor;
            case "qol.dungeon_terminals.colors_solution" -> termColorsSolutionColor;
            case "qol.dungeon_terminals.names_solution" -> termNamesSolutionColor;
            case "qol.dungeon_terminals.panes_solution" -> termPanesSolutionColor;
            case "qol.dungeon_terminals.melody_fill" -> termMelodyFillColor;
            case "qol.dungeon_terminals.melody_other" -> termMelodyOtherColor;
            case "qol.dungeon_terminals.waypoint_color" -> termWaypointColor;
            case "qol.dungeon_terminals.lever_color" -> termLeverColor;
            case "qol.dungeon_menus.pf_joinable" -> pfJoinableColor;
            case "qol.dungeon_menus.pf_dupe" -> pfDupeColor;
            case "qol.dungeon_menus.pf_blocked" -> pfBlockedColor;
            case "qol.dungeon_menus.pf_vc" -> pfVcColor;
            case "qol.dungeon_menus.pf_perm" -> pfPermColor;
            case "qol.dungeon_menus.pf_carry" -> pfCarryColor;
            case "qol.dungeon_carry.player_color" -> carryPlayerColor;
            case "qol.dungeon_term_click.left_color" -> termClickLeftColor;
            case "qol.dungeon_term_click.right_color" -> termClickRightColor;
            default -> null;
        };
    }

    boolean writeColor(String id, int argb) {
        switch (id) {
            case "qol.dungeon_terminals.ui_bg" -> termUiBgColor = argb;
            case "qol.dungeon_terminals.ui_border" -> termUiBorderColor = argb;
            case "qol.dungeon_terminals.header_color" -> termHeaderColor = argb;
            case "qol.dungeon_terminals.colors_solution" -> termColorsSolutionColor = argb;
            case "qol.dungeon_terminals.names_solution" -> termNamesSolutionColor = argb;
            case "qol.dungeon_terminals.panes_solution" -> termPanesSolutionColor = argb;
            case "qol.dungeon_terminals.melody_fill" -> termMelodyFillColor = argb;
            case "qol.dungeon_terminals.melody_other" -> termMelodyOtherColor = argb;
            case "qol.dungeon_terminals.waypoint_color" -> termWaypointColor = argb;
            case "qol.dungeon_terminals.lever_color" -> termLeverColor = argb;
            case "qol.dungeon_menus.pf_joinable" -> pfJoinableColor = argb;
            case "qol.dungeon_menus.pf_dupe" -> pfDupeColor = argb;
            case "qol.dungeon_menus.pf_blocked" -> pfBlockedColor = argb;
            case "qol.dungeon_menus.pf_vc" -> pfVcColor = argb;
            case "qol.dungeon_menus.pf_perm" -> pfPermColor = argb;
            case "qol.dungeon_menus.pf_carry" -> pfCarryColor = argb;
            case "qol.dungeon_carry.player_color" -> carryPlayerColor = argb;
            case "qol.dungeon_term_click.left_color" -> termClickLeftColor = argb;
            case "qol.dungeon_term_click.right_color" -> termClickRightColor = argb;
            default -> {
                return false;
            }
        }
        return true;
    }

    String readText(String id) {
        return switch (id) {
            case "qol.info_tooltips.dungeon_quality_style" ->
                    qualityStyle == null || qualityStyle.isBlank()
                            ? DungeonAthenPortPolicy.DEFAULT_QUALITY_STYLE : qualityStyle;
            case "qol.dungeon_f7.superboom_blocks" -> safe(superboomExtraBlocks);
            case "qol.dungeon_terminals.click_sound" ->
                    termClickSound == null || termClickSound.isBlank()
                            ? "block.note_block.pling" : termClickSound;
            case "qol.dungeon_termsim.ip" -> safe(termSimIp);
            case "qol.dungeon_carry.webhook_url" ->
                    DungeonCarryPolicy.sanitizeWebhookUrl(carryWebhookUrl);
            case "qol.dungeon_party_join.required_pb" ->
                    partyJoinRequiredPb == null || partyJoinRequiredPb.isBlank() ? "5:30" : partyJoinRequiredPb;
            case "qol.dungeon_party_join.required_secrets" ->
                    partyJoinRequiredSecrets == null || partyJoinRequiredSecrets.isBlank()
                            ? "50k" : partyJoinRequiredSecrets;
            case "qol.dungeon_party_join.required_avg" ->
                    partyJoinRequiredSecretAvg == null || partyJoinRequiredSecretAvg.isBlank()
                            ? "8.4" : partyJoinRequiredSecretAvg;
            case "qol.dungeon_party_join.required_mp" ->
                    partyJoinRequiredMp == null || partyJoinRequiredMp.isBlank() ? "800" : partyJoinRequiredMp;
            case "qol.dungeon_watcher.text_fast" ->
                    watcherTextFast == null || watcherTextFast.isBlank() ? "<red>Vroom!" : watcherTextFast;
            case "qol.dungeon_watcher.text_normal" ->
                    watcherTextNormal == null || watcherTextNormal.isBlank() ? "<red>Watcher!" : watcherTextNormal;
            case "qol.dungeon_watcher.text_slow" ->
                    watcherTextSlow == null || watcherTextSlow.isBlank() ? "<red>Yawn..." : watcherTextSlow;
            case "qol.dungeon_watcher.text_very_slow" ->
                    watcherTextVerySlow == null || watcherTextVerySlow.isBlank() ? "<red>Zzz..." : watcherTextVerySlow;
            default -> null;
        };
    }

    boolean writeText(String id, String value) {
        String stored = value == null ? "" : value.trim();
        switch (id) {
            case "qol.info_tooltips.dungeon_quality_style" ->
                    qualityStyle = stored.isBlank() ? DungeonAthenPortPolicy.DEFAULT_QUALITY_STYLE : stored;
            case "qol.dungeon_f7.superboom_blocks" -> superboomExtraBlocks = stored;
            case "qol.dungeon_terminals.click_sound" ->
                    termClickSound = stored.isBlank() ? "block.note_block.pling" : stored;
            case "qol.dungeon_termsim.ip" -> termSimIp = stored;
            case "qol.dungeon_carry.webhook_url" ->
                    carryWebhookUrl = DungeonCarryPolicy.sanitizeWebhookUrl(stored);
            case "qol.dungeon_party_join.required_pb" ->
                    partyJoinRequiredPb = stored.isBlank() ? "5:30" : stored;
            case "qol.dungeon_party_join.required_secrets" ->
                    partyJoinRequiredSecrets = stored.isBlank() ? "50k" : stored;
            case "qol.dungeon_party_join.required_avg" ->
                    partyJoinRequiredSecretAvg = stored.isBlank() ? "8.4" : stored;
            case "qol.dungeon_party_join.required_mp" ->
                    partyJoinRequiredMp = stored.isBlank() ? "800" : stored;
            case "qol.dungeon_watcher.text_fast" ->
                    watcherTextFast = stored.isBlank() ? "<red>Vroom!" : stored;
            case "qol.dungeon_watcher.text_normal" ->
                    watcherTextNormal = stored.isBlank() ? "<red>Watcher!" : stored;
            case "qol.dungeon_watcher.text_slow" ->
                    watcherTextSlow = stored.isBlank() ? "<red>Yawn..." : stored;
            case "qol.dungeon_watcher.text_very_slow" ->
                    watcherTextVerySlow = stored.isBlank() ? "<red>Zzz..." : stored;
            default -> {
                return false;
            }
        }
        return true;
    }

    String readKeybind(String id) {
        return switch (id) {
            case "qol.dungeon_terminals.keybind_left" -> safe(termKeybindLeft);
            case "qol.dungeon_terminals.keybind_right" -> safe(termKeybindRight);
            case "qol.dungeon_terminals.melody_key_1" -> blankTo(termMelodyKey1, "1");
            case "qol.dungeon_terminals.melody_key_2" -> blankTo(termMelodyKey2, "2");
            case "qol.dungeon_terminals.melody_key_3" -> blankTo(termMelodyKey3, "3");
            case "qol.dungeon_terminals.melody_key_4" -> blankTo(termMelodyKey4, "4");
            default -> null;
        };
    }

    boolean writeKeybind(String id, String value) {
        String stored = value == null ? "" : value.trim();
        switch (id) {
            case "qol.dungeon_terminals.keybind_left" -> termKeybindLeft = stored;
            case "qol.dungeon_terminals.keybind_right" -> termKeybindRight = stored;
            case "qol.dungeon_terminals.melody_key_1" -> termMelodyKey1 = stored.isBlank() ? "1" : stored;
            case "qol.dungeon_terminals.melody_key_2" -> termMelodyKey2 = stored.isBlank() ? "2" : stored;
            case "qol.dungeon_terminals.melody_key_3" -> termMelodyKey3 = stored.isBlank() ? "3" : stored;
            case "qol.dungeon_terminals.melody_key_4" -> termMelodyKey4 = stored.isBlank() ? "4" : stored;
            default -> {
                return false;
            }
        }
        return true;
    }

    String waypointClass(int nodeId) {
        return switch (nodeId) {
            case 1 -> s1t1;
            case 2 -> s1t2;
            case 3 -> s1t3;
            case 4 -> s1t4;
            case 5 -> s1right;
            case 6 -> s1left;
            case 7 -> s2t1;
            case 8 -> s2t2;
            case 9 -> s2t3;
            case 10 -> s2t4;
            case 11 -> s2t5;
            case 12 -> s2right;
            case 13 -> s2left;
            case 14 -> s3t1;
            case 15 -> s3t2;
            case 16 -> s3t3;
            case 17 -> s3t4;
            case 18 -> s3right;
            case 19 -> s3left;
            case 20 -> s4t1;
            case 21 -> s4t2;
            case 22 -> s4t3;
            case 23 -> s4t4;
            case 24 -> s4right;
            case 25 -> s4left;
            default -> "Tank";
        };
    }

    void copyModule(String moduleId, DungeonAthenSettings d) {
        if (d == null || !handlesModule(moduleId)) {
            return;
        }
        switch (moduleId) {
            case "qol.info_tooltips" -> qualityStyle = d.qualityStyle;
            case "qol.dungeon_f7" -> copyF7(d);
            case "qol.dungeon_terminals" -> copyTerms(d);
            case "qol.dungeon_termsim" -> termSimIp = d.termSimIp;
            case "qol.dungeon_menus" -> copyMenus(d);
            case "qol.dungeon_carry" -> copyCarry(d);
            case "qol.dungeon_hover_terms" -> {
                hoverEnabled = d.hoverEnabled;
                hoverMinDelay = d.hoverMinDelay;
                hoverMaxDelay = d.hoverMaxDelay;
            }
            case "qol.dungeon_party_join" -> copyJoin(d);
            case "qol.dungeon_soulsand" -> soulsandEnabled = d.soulsandEnabled;
            case "qol.dungeon_term_click" -> copyTermClick(d);
            case "qol.dungeon_watcher" -> copyWatcher(d);
            default -> {
            }
        }
    }

    boolean handlesModule(String moduleId) {
        return switch (moduleId) {
            case "qol.info_tooltips",
                 "qol.dungeon_f7",
                 "qol.dungeon_terminals",
                 "qol.dungeon_termsim",
                 "qol.dungeon_menus",
                 "qol.dungeon_carry",
                 "qol.dungeon_hover_terms",
                 "qol.dungeon_party_join",
                 "qol.dungeon_soulsand",
                 "qol.dungeon_term_click",
                 "qol.dungeon_watcher" -> true;
            default -> false;
        };
    }

    private void copyF7(DungeonAthenSettings d) {
        superboomMinDelay = d.superboomMinDelay;
        superboomMaxDelay = d.superboomMaxDelay;
        superboomSwapBackMin = d.superboomSwapBackMin;
        superboomSwapBackMax = d.superboomSwapBackMax;
        superboomSwapTo = d.superboomSwapTo;
        superboomCustomSlot = d.superboomCustomSlot;
        superboomExtraBlocks = d.superboomExtraBlocks;
        breakerInstamine = d.breakerInstamine;
    }

    private void copyTerms(DungeonAthenSettings d) {
        termMinDelayMs = d.termMinDelayMs;
        termMaxDelayMs = d.termMaxDelayMs;
        termOrder = d.termOrder;
        termResyncTimeout = d.termResyncTimeout;
        termFirstClickDelay = d.termFirstClickDelay;
        termDropKey = d.termDropKey;
        termKeybindLeft = d.termKeybindLeft;
        termKeybindRight = d.termKeybindRight;
        termRubixLeftOnly = d.termRubixLeftOnly;
        termMelodyKey1 = d.termMelodyKey1;
        termMelodyKey2 = d.termMelodyKey2;
        termMelodyKey3 = d.termMelodyKey3;
        termMelodyKey4 = d.termMelodyKey4;
        termUiScale = d.termUiScale;
        termUiRoundness = d.termUiRoundness;
        termUiPadding = d.termUiPadding;
        termUiSlotGap = d.termUiSlotGap;
        termUiMelodyGap = d.termUiMelodyGap;
        termUiBgColor = d.termUiBgColor;
        termUiBorderColor = d.termUiBorderColor;
        termSlotsFill = d.termSlotsFill;
        termSlotsRoundness = d.termSlotsRoundness;
        termNumbersShowText = d.termNumbersShowText;
        termHideHeader = d.termHideHeader;
        termHideTitle = d.termHideTitle;
        termHeaderColor = d.termHeaderColor;
        termClickSound = d.termClickSound;
        termClickPitch = d.termClickPitch;
        termClickVolume = d.termClickVolume;
        termColorsSolutionColor = d.termColorsSolutionColor;
        termNamesSolutionColor = d.termNamesSolutionColor;
        termPanesSolutionColor = d.termPanesSolutionColor;
        termMelodyFillColor = d.termMelodyFillColor;
        termMelodyOtherColor = d.termMelodyOtherColor;
        termCheckClass = d.termCheckClass;
        termRenderText = d.termRenderText;
        termDepthTest = d.termDepthTest;
        termHighlightStyle = d.termHighlightStyle;
        termWaypointColor = d.termWaypointColor;
        termLeverColor = d.termLeverColor;
        s1t1 = d.s1t1; s1t2 = d.s1t2; s1t3 = d.s1t3; s1t4 = d.s1t4;
        s1right = d.s1right; s1left = d.s1left;
        s2t1 = d.s2t1; s2t2 = d.s2t2; s2t3 = d.s2t3; s2t4 = d.s2t4; s2t5 = d.s2t5;
        s2right = d.s2right; s2left = d.s2left;
        s3t1 = d.s3t1; s3t2 = d.s3t2; s3t3 = d.s3t3; s3t4 = d.s3t4;
        s3right = d.s3right; s3left = d.s3left;
        s4t1 = d.s4t1; s4t2 = d.s4t2; s4t3 = d.s4t3; s4t4 = d.s4t4;
        s4right = d.s4right; s4left = d.s4left;
    }

    private void copyMenus(DungeonAthenSettings d) {
        closeChestMinDelay = d.closeChestMinDelay;
        closeChestMaxDelay = d.closeChestMaxDelay;
        pfShowStats = d.pfShowStats;
        pfStack = d.pfStack;
        pfHighlight = d.pfHighlight;
        pfJoinableColor = d.pfJoinableColor;
        pfDupeColor = d.pfDupeColor;
        pfBlockedColor = d.pfBlockedColor;
        pfVcColor = d.pfVcColor;
        pfPermColor = d.pfPermColor;
        pfCarryColor = d.pfCarryColor;
    }

    private void copyCarry(DungeonAthenSettings d) {
        carryEnabled = d.carryEnabled;
        carryAnnounceParty = d.carryAnnounceParty;
        carryShowStart = d.carryShowStart;
        carryWebhook = d.carryWebhook;
        carryWebhookEach = d.carryWebhookEach;
        carryWebhookUrl = d.carryWebhookUrl;
        carryHighlight = d.carryHighlight;
        carryPlayerColor = d.carryPlayerColor;
        carryLineWidth = d.carryLineWidth;
        carryDisplay = d.carryDisplay;
        carryOnlyDungeons = d.carryOnlyDungeons;
        carryState = d.carryState;
        carryHudX = d.carryHudX;
        carryHudY = d.carryHudY;
    }

    private void copyJoin(DungeonAthenSettings d) {
        partyJoinEnabled = d.partyJoinEnabled;
        partyJoinStats = d.partyJoinStats;
        partyJoinAutoKick = d.partyJoinAutoKick;
        partyJoinDetectFloor = d.partyJoinDetectFloor;
        partyJoinRequiredPb = d.partyJoinRequiredPb;
        partyJoinRequiredSecrets = d.partyJoinRequiredSecrets;
        partyJoinRequiredSecretAvg = d.partyJoinRequiredSecretAvg;
        partyJoinRequiredMp = d.partyJoinRequiredMp;
        partyJoinKickMessage = d.partyJoinKickMessage;
        partyJoinSendParty = d.partyJoinSendParty;
        partyJoinMessageDelay = d.partyJoinMessageDelay;
    }

    private void copyTermClick(DungeonAthenSettings d) {
        termClickEnabled = d.termClickEnabled;
        termClickRadius = d.termClickRadius;
        termClickThickness = d.termClickThickness;
        termClickLeftColor = d.termClickLeftColor;
        termClickRightColor = d.termClickRightColor;
    }

    private void copyWatcher(DungeonAthenSettings d) {
        watcherEnabled = d.watcherEnabled;
        watcherBreakdown = d.watcherBreakdown;
        watcherSpawnedAll = d.watcherSpawnedAll;
        watcherSpeak = d.watcherSpeak;
        watcherMove = d.watcherMove;
        watcherBloodTimers = d.watcherBloodTimers;
        watcherShowTicks = d.watcherShowTicks;
        watcherTextFast = d.watcherTextFast;
        watcherTextNormal = d.watcherTextNormal;
        watcherTextSlow = d.watcherTextSlow;
        watcherTextVerySlow = d.watcherTextVerySlow;
        watcherHudX = d.watcherHudX;
        watcherHudY = d.watcherHudY;
    }

    static QolNumberSettings.Spec numberSpec(String id) {
        if (id == null) {
            return null;
        }
        return switch (id) {
            case "qol.dungeon_f7.superboom_min_delay",
                 "qol.dungeon_f7.superboom_max_delay",
                 "qol.dungeon_f7.superboom_swap_back_min",
                 "qol.dungeon_f7.superboom_swap_back_max" ->
                    new QolNumberSettings.Spec(1.0D, 5.0D, 1.0D, true);
            case "qol.dungeon_f7.superboom_custom_slot" ->
                    new QolNumberSettings.Spec(1.0D, 9.0D, 1.0D, true);
            case "qol.dungeon_menus.close_chest_min",
                 "qol.dungeon_menus.close_chest_max" ->
                    new QolNumberSettings.Spec(0.0D, 5.0D, 1.0D, true);
            case "qol.dungeon_terminals.min_delay_ms",
                 "qol.dungeon_terminals.max_delay_ms" ->
                    new QolNumberSettings.Spec(0.0D, 500.0D, 10.0D, true);
            case "qol.dungeon_terminals.resync_timeout" ->
                    new QolNumberSettings.Spec(400.0D, 1000.0D, 10.0D, true);
            case "qol.dungeon_terminals.first_click_delay" ->
                    new QolNumberSettings.Spec(0.0D, 800.0D, 10.0D, true);
            case "qol.dungeon_terminals.ui_scale" ->
                    new QolNumberSettings.Spec(0.5D, 2.0D, 0.05D, true);
            case "qol.dungeon_terminals.ui_roundness",
                 "qol.dungeon_terminals.slots_roundness" ->
                    new QolNumberSettings.Spec(0.0D, 20.0D, 0.5D, true);
            case "qol.dungeon_terminals.ui_padding" ->
                    new QolNumberSettings.Spec(0.0D, 20.0D, 0.5D, true);
            case "qol.dungeon_terminals.ui_slot_gap",
                 "qol.dungeon_terminals.ui_melody_gap" ->
                    new QolNumberSettings.Spec(0.0D, 12.0D, 0.5D, true);
            case "qol.dungeon_terminals.click_pitch" ->
                    new QolNumberSettings.Spec(0.0D, 2.0D, 0.05D, true);
            case "qol.dungeon_terminals.click_volume" ->
                    new QolNumberSettings.Spec(0.0D, 1.0D, 0.05D, true);
            case "qol.dungeon_carry.line_width" ->
                    new QolNumberSettings.Spec(0.5D, 6.0D, 0.1D, true);
            case "qol.dungeon_hover_terms.min_delay",
                 "qol.dungeon_hover_terms.max_delay" ->
                    new QolNumberSettings.Spec(0.0D, 400.0D, 5.0D, true);
            case "qol.dungeon_party_join.message_delay" ->
                    new QolNumberSettings.Spec(0.0D, 40.0D, 1.0D, true);
            case "qol.dungeon_term_click.radius" ->
                    new QolNumberSettings.Spec(1.0D, 16.0D, 1.0D, true);
            case "qol.dungeon_term_click.thickness" ->
                    new QolNumberSettings.Spec(1.0D, 8.0D, 1.0D, true);
            default -> null;
        };
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static double clamp(double value, double min, double max) {
        if (!Double.isFinite(value)) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }
}
