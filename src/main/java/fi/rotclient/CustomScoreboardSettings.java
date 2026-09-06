package fi.rotclient;

/**
 * Persisted Custom Scoreboard options. Nested on {@link QolSkyblockExtras}
 * so older configs keep loading.
 */
final class CustomScoreboardSettings {
    boolean enabled;
    String appearance = CustomScoreboardPolicy.defaultAppearanceText();
    boolean hideVanilla = true;
    boolean showDiff;
    boolean useCustomLines = true;
    boolean showUnclaimedBits;
    boolean showMaxPlayers;
    String powderDisplay = "Available";
    String numberFormat = "1,234,567";
    String numberLayout = "White label: color number";
    boolean time24h;
    boolean timeExact;
    boolean dateInLobby;
    String dateFormat = "MM/dd/yyyy";
    int lineSpacing = 10;
    String textAlign = "Left";
    boolean showProfileName;
    boolean cacheOnSwitch;
    boolean showOutsideSkyblock;
    String alignH = "Right";
    String alignV = "Center";
    int margin = 10;
    String arrowMode = "Number";
    boolean colorArrows;
    String chunkedStats = CustomScoreboardPolicy.defaultChunkedText();
    int maxStatsPerLine = 3;
    String eventPriority = CustomScoreboardPolicy.defaultEventText();
    boolean showAllEvents;
    boolean showMagicalPower = true;
    boolean compactTuning;
    int tuningAmount = 2;
    boolean showMayorPerks = true;
    boolean showMayorTime = true;
    boolean showExtraMayor = true;
    int maxParty = 5;
    boolean partyEverywhere;
    boolean showPartyLeader = true;
    String titleAlign = "Center";
    String customTitle = "&&6&&lSKYBLOCK";
    boolean useCustomTitle = true;
    boolean customTitleOutside;
    String footerAlign = "Left";
    String customFooter = "&&ewww.hypixel.net";
    String customAlphaFooter = "&&ealpha.hypixel.net";
    boolean bgEnabled = true;
    int bgColor = 0xA0101018;
    int bgBorder = 5;
    int bgRound = 8;
    boolean outline;
    int outlineThickness = 5;
    double outlineBlur = 0.7D;
    int outlineTop = 0xFFAF59FF;
    int outlineBottom = 0xFF7FEDFF;
    boolean customBgImage;
    int customBgOpacity = 100;
    boolean hideEmpty = true;
    boolean hideConsecutiveEmpty = true;
    boolean hideEmptyEdges = true;
    boolean hideIrrelevant = true;
    boolean unknownWarning;
    String keybind = "";
    float hudX = 12.0F;
    float hudY = 80.0F;
    float hudScale = 1.0F;

    CustomScoreboardPolicy.Options options() {
        return new CustomScoreboardPolicy.Options(
                CustomScoreboardPolicy.parseAppearance(appearance),
                CustomScoreboardPolicy.parseEvents(eventPriority),
                showAllEvents,
                hideVanilla,
                showDiff,
                useCustomLines,
                showUnclaimedBits,
                showMaxPlayers,
                CustomScoreboardPolicy.parsePowderMode(powderDisplay),
                CustomScoreboardPolicy.parseNumberStyle(numberFormat),
                CustomScoreboardPolicy.parseNumberLayout(numberLayout),
                time24h,
                timeExact,
                dateInLobby,
                dateFormat,
                lineSpacing,
                CustomScoreboardPolicy.parseAlign(textAlign),
                showProfileName,
                cacheOnSwitch,
                showOutsideSkyblock,
                CustomScoreboardPolicy.parseAlign(alignH),
                CustomScoreboardPolicy.parseVAlign(alignV),
                margin,
                CustomScoreboardPolicy.parseArrowMode(arrowMode),
                colorArrows,
                CustomScoreboardPolicy.parseChunked(chunkedStats),
                maxStatsPerLine,
                showMagicalPower,
                compactTuning,
                tuningAmount,
                showMayorPerks,
                showMayorTime,
                showExtraMayor,
                maxParty,
                partyEverywhere,
                showPartyLeader,
                CustomScoreboardPolicy.parseAlign(titleAlign),
                customTitle,
                useCustomTitle,
                customTitleOutside,
                CustomScoreboardPolicy.parseAlign(footerAlign),
                customFooter,
                customAlphaFooter,
                hideEmpty,
                hideConsecutiveEmpty,
                hideEmptyEdges,
                hideIrrelevant,
                unknownWarning);
    }

    void copyFrom(CustomScoreboardSettings other) {
        if (other == null) {
            return;
        }
        enabled = other.enabled;
        appearance = other.appearance;
        hideVanilla = other.hideVanilla;
        showDiff = other.showDiff;
        useCustomLines = other.useCustomLines;
        showUnclaimedBits = other.showUnclaimedBits;
        showMaxPlayers = other.showMaxPlayers;
        powderDisplay = other.powderDisplay;
        numberFormat = other.numberFormat;
        numberLayout = other.numberLayout;
        time24h = other.time24h;
        timeExact = other.timeExact;
        dateInLobby = other.dateInLobby;
        dateFormat = other.dateFormat;
        lineSpacing = other.lineSpacing;
        textAlign = other.textAlign;
        showProfileName = other.showProfileName;
        cacheOnSwitch = other.cacheOnSwitch;
        showOutsideSkyblock = other.showOutsideSkyblock;
        alignH = other.alignH;
        alignV = other.alignV;
        margin = other.margin;
        arrowMode = other.arrowMode;
        colorArrows = other.colorArrows;
        chunkedStats = other.chunkedStats;
        maxStatsPerLine = other.maxStatsPerLine;
        eventPriority = other.eventPriority;
        showAllEvents = other.showAllEvents;
        showMagicalPower = other.showMagicalPower;
        compactTuning = other.compactTuning;
        tuningAmount = other.tuningAmount;
        showMayorPerks = other.showMayorPerks;
        showMayorTime = other.showMayorTime;
        showExtraMayor = other.showExtraMayor;
        maxParty = other.maxParty;
        partyEverywhere = other.partyEverywhere;
        showPartyLeader = other.showPartyLeader;
        titleAlign = other.titleAlign;
        customTitle = other.customTitle;
        useCustomTitle = other.useCustomTitle;
        customTitleOutside = other.customTitleOutside;
        footerAlign = other.footerAlign;
        customFooter = other.customFooter;
        customAlphaFooter = other.customAlphaFooter;
        bgEnabled = other.bgEnabled;
        bgColor = other.bgColor;
        bgBorder = other.bgBorder;
        bgRound = other.bgRound;
        outline = other.outline;
        outlineThickness = other.outlineThickness;
        outlineBlur = other.outlineBlur;
        outlineTop = other.outlineTop;
        outlineBottom = other.outlineBottom;
        customBgImage = other.customBgImage;
        customBgOpacity = other.customBgOpacity;
        hideEmpty = other.hideEmpty;
        hideConsecutiveEmpty = other.hideConsecutiveEmpty;
        hideEmptyEdges = other.hideEmptyEdges;
        hideIrrelevant = other.hideIrrelevant;
        unknownWarning = other.unknownWarning;
        keybind = other.keybind;
        hudX = other.hudX;
        hudY = other.hudY;
        hudScale = other.hudScale;
    }

    void resetAppearance() {
        appearance = CustomScoreboardPolicy.defaultAppearanceText();
    }

    void resetEvents() {
        eventPriority = CustomScoreboardPolicy.defaultEventText();
    }

    Boolean readBoolean(String settingId) {
        return switch (settingId) {
            case "qol.custom_scoreboard.hide_vanilla" -> hideVanilla;
            case "qol.custom_scoreboard.show_diff" -> showDiff;
            case "qol.custom_scoreboard.use_custom_lines" -> useCustomLines;
            case "qol.custom_scoreboard.show_unclaimed_bits" -> showUnclaimedBits;
            case "qol.custom_scoreboard.show_max_players" -> showMaxPlayers;
            case "qol.custom_scoreboard.time_24h" -> time24h;
            case "qol.custom_scoreboard.time_exact" -> timeExact;
            case "qol.custom_scoreboard.date_in_lobby" -> dateInLobby;
            case "qol.custom_scoreboard.show_profile_name" -> showProfileName;
            case "qol.custom_scoreboard.cache_on_switch" -> cacheOnSwitch;
            case "qol.custom_scoreboard.show_outside_skyblock" -> showOutsideSkyblock;
            case "qol.custom_scoreboard.color_arrows" -> colorArrows;
            case "qol.custom_scoreboard.show_all_events" -> showAllEvents;
            case "qol.custom_scoreboard.show_magical_power" -> showMagicalPower;
            case "qol.custom_scoreboard.compact_tuning" -> compactTuning;
            case "qol.custom_scoreboard.show_mayor_perks" -> showMayorPerks;
            case "qol.custom_scoreboard.show_mayor_time" -> showMayorTime;
            case "qol.custom_scoreboard.show_extra_mayor" -> showExtraMayor;
            case "qol.custom_scoreboard.party_everywhere" -> partyEverywhere;
            case "qol.custom_scoreboard.show_party_leader" -> showPartyLeader;
            case "qol.custom_scoreboard.use_custom_title" -> useCustomTitle;
            case "qol.custom_scoreboard.custom_title_outside" -> customTitleOutside;
            case "qol.custom_scoreboard.bg_enabled" -> bgEnabled;
            case "qol.custom_scoreboard.outline" -> outline;
            case "qol.custom_scoreboard.custom_bg_image" -> customBgImage;
            case "qol.custom_scoreboard.hide_empty" -> hideEmpty;
            case "qol.custom_scoreboard.hide_consecutive_empty" -> hideConsecutiveEmpty;
            case "qol.custom_scoreboard.hide_empty_edges" -> hideEmptyEdges;
            case "qol.custom_scoreboard.hide_irrelevant" -> hideIrrelevant;
            case "qol.custom_scoreboard.unknown_warning" -> unknownWarning;
            default -> null;
        };
    }

    boolean writeBoolean(String settingId, boolean value) {
        switch (settingId) {
            case "qol.custom_scoreboard.hide_vanilla" -> hideVanilla = value;
            case "qol.custom_scoreboard.show_diff" -> showDiff = value;
            case "qol.custom_scoreboard.use_custom_lines" -> useCustomLines = value;
            case "qol.custom_scoreboard.show_unclaimed_bits" -> showUnclaimedBits = value;
            case "qol.custom_scoreboard.show_max_players" -> showMaxPlayers = value;
            case "qol.custom_scoreboard.time_24h" -> time24h = value;
            case "qol.custom_scoreboard.time_exact" -> timeExact = value;
            case "qol.custom_scoreboard.date_in_lobby" -> dateInLobby = value;
            case "qol.custom_scoreboard.show_profile_name" -> showProfileName = value;
            case "qol.custom_scoreboard.cache_on_switch" -> cacheOnSwitch = value;
            case "qol.custom_scoreboard.show_outside_skyblock" -> showOutsideSkyblock = value;
            case "qol.custom_scoreboard.color_arrows" -> colorArrows = value;
            case "qol.custom_scoreboard.show_all_events" -> showAllEvents = value;
            case "qol.custom_scoreboard.show_magical_power" -> showMagicalPower = value;
            case "qol.custom_scoreboard.compact_tuning" -> compactTuning = value;
            case "qol.custom_scoreboard.show_mayor_perks" -> showMayorPerks = value;
            case "qol.custom_scoreboard.show_mayor_time" -> showMayorTime = value;
            case "qol.custom_scoreboard.show_extra_mayor" -> showExtraMayor = value;
            case "qol.custom_scoreboard.party_everywhere" -> partyEverywhere = value;
            case "qol.custom_scoreboard.show_party_leader" -> showPartyLeader = value;
            case "qol.custom_scoreboard.use_custom_title" -> useCustomTitle = value;
            case "qol.custom_scoreboard.custom_title_outside" -> customTitleOutside = value;
            case "qol.custom_scoreboard.bg_enabled" -> bgEnabled = value;
            case "qol.custom_scoreboard.outline" -> outline = value;
            case "qol.custom_scoreboard.custom_bg_image" -> customBgImage = value;
            case "qol.custom_scoreboard.hide_empty" -> hideEmpty = value;
            case "qol.custom_scoreboard.hide_consecutive_empty" -> hideConsecutiveEmpty = value;
            case "qol.custom_scoreboard.hide_empty_edges" -> hideEmptyEdges = value;
            case "qol.custom_scoreboard.hide_irrelevant" -> hideIrrelevant = value;
            case "qol.custom_scoreboard.unknown_warning" -> unknownWarning = value;
            default -> {
                return false;
            }
        }
        return true;
    }

    Double readNumber(String settingId) {
        QolNumberSettings.Spec spec = CustomScoreboardPolicy.numberSpec(settingId);
        Double raw = switch (settingId) {
            case "qol.custom_scoreboard.line_spacing" -> (double) lineSpacing;
            case "qol.custom_scoreboard.margin" -> (double) margin;
            case "qol.custom_scoreboard.max_stats_per_line" -> (double) maxStatsPerLine;
            case "qol.custom_scoreboard.tuning_amount" -> (double) tuningAmount;
            case "qol.custom_scoreboard.max_party" -> (double) maxParty;
            case "qol.custom_scoreboard.bg_border" -> (double) bgBorder;
            case "qol.custom_scoreboard.bg_round" -> (double) bgRound;
            case "qol.custom_scoreboard.outline_thickness" -> (double) outlineThickness;
            case "qol.custom_scoreboard.outline_blur" -> outlineBlur;
            case "qol.custom_scoreboard.custom_bg_opacity" -> (double) customBgOpacity;
            default -> null;
        };
        if (raw == null) {
            return null;
        }
        return spec == null ? raw : spec.clamp(raw);
    }

    boolean writeNumber(String settingId, double value) {
        QolNumberSettings.Spec spec = CustomScoreboardPolicy.numberSpec(settingId);
        double clamped = spec == null ? value : spec.clamp(value);
        switch (settingId) {
            case "qol.custom_scoreboard.line_spacing" -> lineSpacing = (int) Math.round(clamped);
            case "qol.custom_scoreboard.margin" -> margin = (int) Math.round(clamped);
            case "qol.custom_scoreboard.max_stats_per_line" -> maxStatsPerLine = (int) Math.round(clamped);
            case "qol.custom_scoreboard.tuning_amount" -> tuningAmount = (int) Math.round(clamped);
            case "qol.custom_scoreboard.max_party" -> maxParty = (int) Math.round(clamped);
            case "qol.custom_scoreboard.bg_border" -> bgBorder = (int) Math.round(clamped);
            case "qol.custom_scoreboard.bg_round" -> bgRound = (int) Math.round(clamped);
            case "qol.custom_scoreboard.outline_thickness" -> outlineThickness = (int) Math.round(clamped);
            case "qol.custom_scoreboard.outline_blur" -> outlineBlur = clamped;
            case "qol.custom_scoreboard.custom_bg_opacity" -> customBgOpacity = (int) Math.round(clamped);
            default -> {
                return false;
            }
        }
        return true;
    }

    String readEnum(String settingId) {
        return switch (settingId) {
            case "qol.custom_scoreboard.powder_display" -> powderDisplay;
            case "qol.custom_scoreboard.number_format" -> numberFormat;
            case "qol.custom_scoreboard.number_layout" -> numberLayout;
            case "qol.custom_scoreboard.date_format" -> dateFormat;
            case "qol.custom_scoreboard.text_align" -> textAlign;
            case "qol.custom_scoreboard.align_h" -> alignH;
            case "qol.custom_scoreboard.align_v" -> alignV;
            case "qol.custom_scoreboard.arrow_mode" -> arrowMode;
            case "qol.custom_scoreboard.title_align" -> titleAlign;
            case "qol.custom_scoreboard.footer_align" -> footerAlign;
            default -> null;
        };
    }

    boolean writeEnum(String settingId, String value) {
        String stored = value == null ? "" : value.trim();
        switch (settingId) {
            case "qol.custom_scoreboard.powder_display" ->
                    powderDisplay = labelOr(stored, CustomScoreboardPolicy.powderOptions(), "Available");
            case "qol.custom_scoreboard.number_format" ->
                    numberFormat = labelOr(stored, CustomScoreboardPolicy.numberStyleOptions(), "1,234,567");
            case "qol.custom_scoreboard.number_layout" ->
                    numberLayout = labelOr(stored, CustomScoreboardPolicy.numberLayoutOptions(),
                            "White label: color number");
            case "qol.custom_scoreboard.date_format" ->
                    dateFormat = labelOr(stored, CustomScoreboardPolicy.dateFormatOptions(), "MM/dd/yyyy");
            case "qol.custom_scoreboard.text_align" ->
                    textAlign = labelOr(stored, CustomScoreboardPolicy.alignOptions(), "Left");
            case "qol.custom_scoreboard.align_h" ->
                    alignH = labelOr(stored, CustomScoreboardPolicy.alignOptions(), "Right");
            case "qol.custom_scoreboard.align_v" ->
                    alignV = labelOr(stored, CustomScoreboardPolicy.valignOptions(), "Center");
            case "qol.custom_scoreboard.arrow_mode" ->
                    arrowMode = labelOr(stored, CustomScoreboardPolicy.arrowOptions(), "Number");
            case "qol.custom_scoreboard.title_align" ->
                    titleAlign = labelOr(stored, CustomScoreboardPolicy.alignOptions(), "Center");
            case "qol.custom_scoreboard.footer_align" ->
                    footerAlign = labelOr(stored, CustomScoreboardPolicy.alignOptions(), "Left");
            default -> {
                return false;
            }
        }
        return true;
    }

    String readText(String settingId) {
        return switch (settingId) {
            case "qol.custom_scoreboard.appearance" -> appearance == null ? "" : appearance;
            case "qol.custom_scoreboard.chunked_stats" -> chunkedStats == null ? "" : chunkedStats;
            case "qol.custom_scoreboard.event_priority" -> eventPriority == null ? "" : eventPriority;
            case "qol.custom_scoreboard.custom_title" -> customTitle == null ? "" : customTitle;
            case "qol.custom_scoreboard.custom_footer" -> customFooter == null ? "" : customFooter;
            case "qol.custom_scoreboard.custom_alpha_footer" ->
                    customAlphaFooter == null ? "" : customAlphaFooter;
            default -> null;
        };
    }

    boolean writeText(String settingId, String value) {
        String stored = value == null ? "" : value;
        switch (settingId) {
            case "qol.custom_scoreboard.appearance" -> appearance = stored;
            case "qol.custom_scoreboard.chunked_stats" -> chunkedStats = stored;
            case "qol.custom_scoreboard.event_priority" -> eventPriority = stored;
            case "qol.custom_scoreboard.custom_title" -> customTitle = stored;
            case "qol.custom_scoreboard.custom_footer" -> customFooter = stored;
            case "qol.custom_scoreboard.custom_alpha_footer" -> customAlphaFooter = stored;
            default -> {
                return false;
            }
        }
        return true;
    }

    Integer readColor(String settingId) {
        return switch (settingId) {
            case "qol.custom_scoreboard.bg_color" -> bgColor;
            case "qol.custom_scoreboard.outline_top" -> outlineTop;
            case "qol.custom_scoreboard.outline_bottom" -> outlineBottom;
            default -> null;
        };
    }

    boolean writeColor(String settingId, int argb) {
        switch (settingId) {
            case "qol.custom_scoreboard.bg_color" -> bgColor = argb;
            case "qol.custom_scoreboard.outline_top" -> outlineTop = argb;
            case "qol.custom_scoreboard.outline_bottom" -> outlineBottom = argb;
            default -> {
                return false;
            }
        }
        return true;
    }

    private static String labelOr(String value, java.util.List<String> options, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        for (String option : options) {
            if (option.equalsIgnoreCase(value.trim())) {
                return option;
            }
        }
        return fallback;
    }
}
