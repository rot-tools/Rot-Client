package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One example profile that ships with the client, read from a JSON file under
 * {@code assets/rotclient/profile-presets/}.
 *
 * A preset is data, not code: which modules to switch on, which child toggles
 * to set, and a HUD layout described as anchored stacks (see
 * {@link PresetHudLayout}). Everything not mentioned keeps the ordinary Rot
 * Client default, so an example is "defaults plus these HUDs".
 *
 * Examples only ever use modules from the shared catalog, so they behave the
 * same in Rot Client and Rot Client+, and none of them touches an automation
 * option.
 */
final class RotClientProfilePreset {
    /** Every example must include these two. */
    static final String CUSTOM_SCOREBOARD = "qol.custom_scoreboard";
    static final String PET_HUD = "qol.pet_hud";

    String id = "";
    String name = "";
    String summary = "";
    List<String> highlights = new ArrayList<>();
    List<String> modules = new ArrayList<>();
    Map<String, Boolean> settings = new java.util.LinkedHashMap<>();

    /** Standalone Mining Tracker HUD on/off (not a QoL module). */
    boolean miningTracker;

    /** Standalone Powder Chest HUD on/off (not a QoL module). */
    boolean powderChestHud;

    List<LayoutStack> layout = new ArrayList<>();

    static final class LayoutStack {
        String anchor = "";
        List<String> elements = new ArrayList<>();
    }

    /** Required by Gson; normalises whatever a hand-edited file left null. */
    void normalize() {
        id = id == null ? "" : id.trim();
        name = name == null ? "" : name.trim();
        summary = summary == null ? "" : summary.trim();
        if (highlights == null) {
            highlights = new ArrayList<>();
        }
        if (modules == null) {
            modules = new ArrayList<>();
        }
        if (settings == null) {
            settings = new java.util.LinkedHashMap<>();
        }
        if (layout == null) {
            layout = new ArrayList<>();
        }
        for (LayoutStack stack : layout) {
            if (stack == null) {
                continue;
            }
            stack.anchor = stack.anchor == null ? "" : stack.anchor.trim();
            if (stack.elements == null) {
                stack.elements = new ArrayList<>();
            }
        }
    }

    /**
     * Everything wrong with this preset, or an empty list when it is safe to
     * install. A preset with problems is never offered to the user.
     */
    List<String> problems() {
        List<String> problems = new ArrayList<>();

        if (id.isEmpty()) {
            problems.add("missing id");
        }
        if (!RotClientProfile.isValidName(name)) {
            problems.add("invalid name '" + name + "'");
        }

        for (String module : modules) {
            QolUtilityCatalog.ModuleDef def = QolUtilityCatalog.findById(module);
            if (def == null) {
                problems.add("unknown module '" + module + "'");
            } else if (!def.toggleable()) {
                problems.add("module '" + module + "' cannot be toggled");
            }
        }

        QolUtilityConfig probe = new QolUtilityConfig();
        for (String setting : settings.keySet()) {
            if (probe.readBoolean(setting) == null) {
                problems.add("unknown toggle '" + setting + "'");
            }
        }

        Set<String> placed = new HashSet<>();
        Set<PresetHudLayout.Anchor> centres = new HashSet<>();

        for (LayoutStack stack : layout) {
            if (stack == null) {
                problems.add("empty layout entry");
                continue;
            }
            PresetHudLayout.Anchor anchor = PresetHudLayout.Anchor.parse(stack.anchor);
            if (anchor == null) {
                problems.add("unknown anchor '" + stack.anchor + "'");
                continue;
            }
            if (!anchor.isLeft() && !anchor.isRight() && !centres.add(anchor)) {
                problems.add("more than one stack on " + anchor);
            }
            for (String element : stack.elements) {
                if (!PresetHudSizes.isKnown(element)) {
                    problems.add("unknown HUD element '" + element + "'");
                } else if (!placed.add(element)) {
                    problems.add("HUD element '" + element + "' placed twice");
                }
            }
        }

        return problems;
    }

    /** True when this preset lists the given module. */
    boolean enables(String moduleId) {
        return modules.contains(moduleId);
    }

    private List<PresetHudLayout.Stack> stacks() {
        List<PresetHudLayout.Stack> stacks = new ArrayList<>();
        for (LayoutStack stack : layout) {
            PresetHudLayout.Anchor anchor =
                    stack == null ? null : PresetHudLayout.Anchor.parse(stack.anchor);
            if (anchor != null) {
                stacks.add(new PresetHudLayout.Stack(anchor, stack.elements));
            }
        }
        return stacks;
    }

    /**
     * True when every HUD of this example fits a screen of this GUI-scaled
     * size without overlapping. On a smaller one the layout is still placed
     * on screen but some HUDs overlap.
     */
    boolean fits(int screenWidth, int screenHeight) {
        return PresetHudLayout.fits(
                stacks(), screenWidth, screenHeight, PresetHudSizes::of);
    }

    /**
     * Builds the settings for a new profile: Rot Client defaults, then this
     * preset's choices, then the HUD layout resolved for a screen of this
     * size (GUI-scaled pixels).
     */
    RotClientProfileSettings buildSettings(int screenWidth, int screenHeight) {
        RotClientProfileSettings settings =
                RotClientProfileSettings.defaults();

        QolUtilityConfig qol = settings.qolUtilities;

        for (String module : modules) {
            qol.setModuleEnabled(module, true);
        }

        for (Map.Entry<String, Boolean> entry : this.settings.entrySet()) {
            qol.writeBoolean(entry.getKey(), Boolean.TRUE.equals(entry.getValue()));
        }

        settings.miningTrackerEnabled = miningTracker;

        // Rot Client's default has the Powder Chest HUD on everywhere; an
        // example only shows it when it is a mining one.
        settings.powderChestHudEnabled = powderChestHud;

        for (PresetHudLayout.Placement placed : PresetHudLayout.resolve(
                stacks(), screenWidth, screenHeight, PresetHudSizes::of)) {

            switch (placed.id()) {
                case PresetHudSizes.MINING_TRACKER -> {
                    settings.miningHudX = placed.x();
                    settings.miningHudY = placed.y();
                }
                case PresetHudSizes.POWDER_CHEST -> {
                    settings.powderChestHudX = placed.x();
                    settings.powderChestHudY = placed.y();
                }
                default -> qol.setPose(placed.id(), placed.x(), placed.y());
            }
        }

        settings.normalize();
        return settings;
    }
}
