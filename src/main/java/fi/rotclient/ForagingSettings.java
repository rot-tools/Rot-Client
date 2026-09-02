package fi.rotclient;

/**
 * Catalog IDs for the four foraging parents. Runtime reads the same IDs through
 * {@link QolUtilityConfig}.
 */
public final class ForagingSettings {
    public final boolean trees;
    public final boolean progressHud;
    public final boolean gifts;
    public final boolean hideUnmineable;
    public final boolean onlyHoldingAxe;
    public final boolean audio;
    public final boolean mutePhantom;
    public final boolean muteBreak;
    public final boolean muteBreakOnGalatea;
    public final boolean muteFusion;
    public final boolean muteStereo;
    public final boolean helpers;
    public final boolean sweepHud;
    public final boolean beaconAlert;
    public final boolean highlights;
    public final boolean huntingEsp;
    public final boolean cheats;
    public final boolean autoChop;
    public final boolean autoBeacon;
    public final boolean axeToss;

    public ForagingSettings(
            boolean trees,
            boolean progressHud,
            boolean gifts,
            boolean hideUnmineable,
            boolean onlyHoldingAxe,
            boolean audio,
            boolean mutePhantom,
            boolean muteBreak,
            boolean muteBreakOnGalatea,
            boolean muteFusion,
            boolean muteStereo,
            boolean helpers,
            boolean sweepHud,
            boolean beaconAlert,
            boolean highlights,
            boolean huntingEsp,
            boolean cheats,
            boolean autoChop,
            boolean autoBeacon,
            boolean axeToss) {
        this.trees = trees;
        this.progressHud = progressHud;
        this.gifts = gifts;
        this.hideUnmineable = hideUnmineable;
        this.onlyHoldingAxe = onlyHoldingAxe;
        this.audio = audio;
        this.mutePhantom = mutePhantom;
        this.muteBreak = muteBreak;
        this.muteBreakOnGalatea = muteBreakOnGalatea;
        this.muteFusion = muteFusion;
        this.muteStereo = muteStereo;
        this.helpers = helpers;
        this.sweepHud = sweepHud;
        this.beaconAlert = beaconAlert;
        this.highlights = highlights;
        this.huntingEsp = huntingEsp;
        this.cheats = cheats;
        this.autoChop = autoChop;
        this.autoBeacon = autoBeacon;
        this.axeToss = axeToss;
    }

    static ForagingSettings from(QolUtilityConfig qol) {
        if (qol == null) {
            return disabled();
        }
        boolean trees = qol.isModuleEnabled("qol.foraging_trees");
        boolean audio = qol.isModuleEnabled("qol.foraging_audio");
        boolean helpers = qol.isModuleEnabled("qol.foraging_helpers");
        boolean cheats = qol.isModuleEnabled("qol.foraging_cheats");
        return new ForagingSettings(
                trees,
                flag(qol, "qol.foraging_trees.progress_hud", true),
                flag(qol, "qol.foraging_trees.gift_hud", true),
                flag(qol, "qol.foraging_trees.hide_unmineable", true),
                flag(qol, "qol.foraging_trees.only_axe", false),
                audio,
                flag(qol, "qol.foraging_audio.mute_phantom", true),
                flag(qol, "qol.foraging_audio.mute_tree_break", true),
                flag(qol, "qol.foraging_audio.mute_break_galatea", true),
                flag(qol, "qol.foraging_audio.mute_fusion", true),
                flag(qol, "qol.foraging_audio.mute_stereo", true),
                helpers,
                flag(qol, "qol.foraging_helpers.sweep_hud", true),
                flag(qol, "qol.foraging_helpers.beacon_hints", true),
                flag(qol, "qol.foraging_helpers.highlights", true),
                flag(qol, "qol.foraging_helpers.hunting_esp", true),
                cheats,
                flag(qol, "qol.foraging_cheats.auto_chop", false),
                flag(qol, "qol.foraging_cheats.auto_beacon", false),
                flag(qol, "qol.foraging_cheats.axe_toss", false));
    }

    static ForagingSettings disabled() {
        return new ForagingSettings(
                false, false, false, false, true,
                false, false, false, false, false, false,
                false, false, false, false, false,
                false, false, false, false);
    }

    boolean any() {
        return trees || audio || helpers || cheats;
    }

    private static boolean flag(QolUtilityConfig qol, String id, boolean whenMissing) {
        Boolean value = qol.readBoolean(id);
        return value == null ? whenMissing : value;
    }
}
