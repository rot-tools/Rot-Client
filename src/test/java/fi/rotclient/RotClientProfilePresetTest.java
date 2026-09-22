package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

final class RotClientProfilePresetTest {

    /**
     * GUI-scaled sizes a player can realistically have: 1080p at GUI scale
     * 3, 2 and 1, and a 720p window at scale 2. Layouts must be clean on all
     * of them. (Scale 4 and above is too small for a full HUD column and is
     * left to the HUD editor.)
     */
    private static final int[][] SCREENS = {
            {640, 360},
            {854, 480},
            {960, 540},
            {1280, 720},
            {1920, 1080}
    };

    /**
     * The Custom Scoreboard aligns itself to the right edge, vertically
     * centred, and example layouts never move it. Keep every other HUD out of
     * a generous box there.
     */
    private static final int BOARD_WIDTH = 170;
    private static final int BOARD_HEIGHT = 190;

    private static List<RotClientProfilePreset> all() {
        List<RotClientProfilePreset> presets = new ArrayList<>();
        for (String id : RotClientProfilePresets.indexedIds()) {
            RotClientProfilePreset preset = RotClientProfilePresets.read(id);
            assertNotNull(preset, "index lists '" + id + "' but it could not be read");
            presets.add(preset);
        }
        return presets;
    }

    /**
     * Mirrors {@code QolOverlayHud.isVisibleElement} for every HUD that has a
     * footprint. If a HUD becomes visible after applying a preset but is not
     * laid out, it would sit at its default position on top of something.
     */
    private static boolean visible(String id, RotClientProfileSettings s) {
        QolUtilityConfig q = s.qolUtilities;
        QolSkyblockExtras x = q.extras();
        return switch (id) {
            case PresetHudSizes.MINING_TRACKER -> s.miningTrackerEnabled;
            case PresetHudSizes.POWDER_CHEST -> s.powderChestHudEnabled;
            case "performance" -> q.performanceHudEnabled;
            case "pet" -> q.petHudEnabled;
            case "commission" -> q.commissionDisplayEnabled;
            case "health" -> q.playerDisplayEnabled && q.playerDisplayHealthHud;
            case "mana" -> q.playerDisplayEnabled && q.playerDisplayManaHud;
            case "overflow" -> q.playerDisplayEnabled && q.playerDisplayOverflowManaHud;
            case "defense" -> q.playerDisplayEnabled && q.playerDisplayDefenseHud;
            case "vitality" -> q.playerDisplayEnabled && q.playerDisplayVitalityHud;
            case "ehp" -> q.playerDisplayEnabled && q.playerDisplayEhpHud;
            case "speed" -> q.playerDisplayEnabled && q.playerDisplaySpeedHud;
            case "slayer" -> x.slayerDisplayEnabled;
            case "slayer_progress" -> x.slayerProgressEnabled;
            case "slayer_rng" -> x.slayerDropsEnabled && x.slayerDropsRngHud;
            case "slayer_stats" -> x.slayerStatsEnabled;
            case "slayer_carry" -> x.slayerCarryEnabled && x.slayerCarryDisplay;
            case "slayer_cocoon" -> x.slayerCocoonAlertEnabled && x.slayerCocoonTimer;
            case "slayer_attunement" -> x.slayerAttunementDisplayEnabled;
            case "slayer_vengeance" -> x.slayerVengeanceEnabled;
            case "dungeon" -> x.dungeonHudEnabled;
            // The timer toggle defaults to on, but DungeonWatcherRuntime.hudLines
            // draws nothing unless the Dungeon Watcher module itself is on.
            case "dungeon_watcher" -> x.athen().watcherEnabled
                    && x.athen().watcherBloodTimers;
            case "fishing" -> q.fishingHelperEnabled
                    || x.fishingCreaturesEnabled
                    || x.fishingTrophyEnabled
                    || x.fishingToolsEnabled;
            case "mining" -> x.miningScathaEnabled && x.miningScathaHud
                    || x.miningEventsEnabled && x.miningEventsHud
                    || x.miningGlaciteEnabled
                    && (x.miningGlacitePityHud || x.miningGlaciteCorpseHud)
                    || x.miningHelpersEnabled
                    && (x.miningHelpersAbilityHud
                    || x.miningHelpersDrillFuel
                    || x.miningHelpersMetalDistance)
                    || x.miningHotmEnabled && x.miningHotmSkyMall;
            default -> throw new AssertionError(
                    "no visibility rule for '" + id + "'; add it here");
        };
    }

    private static Set<String> laidOut(RotClientProfilePreset preset) {
        Set<String> ids = new TreeSet<>();
        for (RotClientProfilePreset.LayoutStack stack : preset.layout) {
            ids.addAll(stack.elements);
        }
        return ids;
    }

    @Test
    void everyIndexedPresetLoadsAndIsValid() {
        List<String> indexed = RotClientProfilePresets.indexedIds();
        assertFalse(indexed.isEmpty());

        List<String> loaded = new ArrayList<>();
        for (RotClientProfilePreset preset : RotClientProfilePresets.bundled()) {
            loaded.add(preset.id);
        }

        // The loader silently skips a broken preset; this is where it fails.
        assertEquals(indexed, loaded, "a bundled preset failed to load");

        for (RotClientProfilePreset preset : all()) {
            assertEquals(List.of(), preset.problems(),
                    preset.id + " has problems");
        }
    }

    @Test
    void idsAndNamesAreUniqueAndNamesFitAProfile() {
        Set<String> ids = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (RotClientProfilePreset preset : all()) {
            assertTrue(ids.add(preset.id.toLowerCase()), "duplicate id " + preset.id);
            assertTrue(names.add(preset.name.toLowerCase()), "duplicate name " + preset.name);
            assertTrue(RotClientProfile.isValidName(preset.name));
            assertFalse(preset.summary.isBlank(), preset.id + " needs a summary");
            assertFalse(preset.highlights.isEmpty(), preset.id + " needs highlights");
        }
    }

    @Test
    void everyExampleHasTheCustomScoreboardAndThePetHud() {
        for (RotClientProfilePreset preset : all()) {
            assertTrue(preset.enables(RotClientProfilePreset.CUSTOM_SCOREBOARD),
                    preset.id + " must include the Custom Scoreboard");
            assertTrue(preset.enables(RotClientProfilePreset.PET_HUD),
                    preset.id + " must include the Pet HUD");
            assertTrue(laidOut(preset).contains("pet"),
                    preset.id + " must place the Pet HUD");
        }
    }

    @Test
    void applyingAnExampleTurnsOnExactlyWhatItPromises() {
        for (RotClientProfilePreset preset : all()) {
            RotClientProfileSettings s = preset.buildSettings(960, 540);

            for (String module : preset.modules) {
                assertTrue(s.qolUtilities.isModuleEnabled(module),
                        preset.id + ": " + module + " is not enabled");
            }
            for (var entry : preset.settings.entrySet()) {
                assertEquals(entry.getValue(),
                        s.qolUtilities.readBoolean(entry.getKey()),
                        preset.id + ": " + entry.getKey());
            }
            assertEquals(preset.miningTracker, s.miningTrackerEnabled);
            assertEquals(preset.powderChestHud, s.powderChestHudEnabled);
        }
    }

    @Test
    void everyVisibleHudIsPlacedAndEveryPlacedHudIsVisible() {
        for (RotClientProfilePreset preset : all()) {
            RotClientProfileSettings s = preset.buildSettings(960, 540);
            Set<String> placed = laidOut(preset);

            for (String id : PresetHudSizes.ids()) {
                assertEquals(placed.contains(id), visible(id, s),
                        preset.id + ": HUD '" + id + "' is "
                                + (visible(id, s) ? "visible but not placed"
                                : "placed but not visible"));
            }
        }
    }

    @Test
    void onlyThePetAndPowderChestHudsAreOnInAFreshProfile() {
        // Keeps the visible-vs-placed test above honest: it is only
        // meaningful if we know which HUDs a fresh profile already shows.
        // The examples place the Pet HUD themselves and switch the Powder
        // Chest HUD off unless they are mining ones. If a default changes,
        // this fails and the examples need a look.
        RotClientProfileSettings defaults = RotClientProfileSettings.defaults();

        Set<String> onByDefault = new TreeSet<>();
        for (String id : PresetHudSizes.ids()) {
            if (visible(id, defaults)) {
                onByDefault.add(id);
            }
        }

        assertEquals(new TreeSet<>(List.of("pet", PresetHudSizes.POWDER_CHEST)),
                onByDefault);
    }

    @Test
    void nonMiningExamplesDoNotShowThePowderChestHud() {
        for (RotClientProfilePreset preset : all()) {
            RotClientProfileSettings s = preset.buildSettings(960, 540);

            assertEquals(laidOut(preset).contains(PresetHudSizes.POWDER_CHEST),
                    s.powderChestHudEnabled, preset.id);
        }
    }

    @Test
    void everyExampleFitsFromAHalfDecentWindowUp() {
        for (RotClientProfilePreset preset : all()) {
            for (int[] screen : SCREENS) {
                if (screen[0] >= 854) {
                    assertTrue(preset.fits(screen[0], screen[1]),
                            preset.id + " does not fit " + screen[0] + "x" + screen[1]);
                }
            }
        }
    }

    @Test
    void layoutsAreOnScreenAndNeverOverlapWhereTheyFit() {
        for (RotClientProfilePreset preset : all()) {
            for (int[] screen : SCREENS) {
                int w = screen[0];
                int h = screen[1];
                List<PresetHudLayout.Placement> placements = resolved(preset, w, h);
                String where = preset.id + " at " + w + "x" + h;

                assertEquals(laidOut(preset).size(), placements.size(), where);

                PresetHudLayout.Placement board = new PresetHudLayout.Placement(
                        "custom_scoreboard",
                        w - BOARD_WIDTH,
                        (h - BOARD_HEIGHT) / 2,
                        BOARD_WIDTH,
                        BOARD_HEIGHT);

                for (PresetHudLayout.Placement a : placements) {
                    // Always on screen, fitting or not.
                    assertTrue(a.x() >= 0 && a.y() >= 0
                                    && a.x() + a.width() <= w
                                    && a.y() + a.height() <= h,
                            where + ": " + a.id() + " is off screen " + a);
                }

                // A layout that does not fit is clamped and will overlap;
                // that is reported to the player, so only assert the rest
                // where it is promised.
                if (!preset.fits(w, h)) {
                    continue;
                }

                for (int i = 0; i < placements.size(); i++) {
                    PresetHudLayout.Placement a = placements.get(i);

                    assertFalse(a.overlaps(board),
                            where + ": " + a.id() + " overlaps the scoreboard");

                    for (int j = i + 1; j < placements.size(); j++) {
                        assertFalse(a.overlaps(placements.get(j)),
                                where + ": " + a.id() + " overlaps "
                                        + placements.get(j).id());
                    }
                }
            }
        }
    }

    @Test
    void theMiningExamplePutsThePowderChestInTheTopRightCorner() {
        RotClientProfilePreset mining = RotClientProfilePresets.findById("mining");
        assertNotNull(mining);

        for (int[] screen : SCREENS) {
            RotClientProfileSettings s = mining.buildSettings(screen[0], screen[1]);
            PresetHudLayout.Size size = PresetHudSizes.of(PresetHudSizes.POWDER_CHEST);

            assertEquals(screen[0] - PresetHudLayout.MARGIN - size.width(),
                    s.powderChestHudX, 0.001F, "x at " + screen[0]);
            assertEquals(PresetHudLayout.MARGIN, s.powderChestHudY, 0.001F,
                    "y at " + screen[0]);
        }
    }

    @Test
    void theMiningLayoutIsKnownToNeedMoreThanASmallWindow() {
        // Tracker + pet column + Powder Chest are 652px wide with margins.
        // At GUI scale 3 on 1080p (640 wide) they cannot all fit, and the
        // Examples page says so before anything is added.
        RotClientProfilePreset mining = RotClientProfilePresets.findById("mining");
        assertNotNull(mining);

        assertFalse(mining.fits(640, 360));
        assertTrue(mining.fits(652, 360));
        assertFalse(mining.fits(651, 360));
    }

    @Test
    void appliedPositionsAreExactlyTheResolvedOnes() {
        for (RotClientProfilePreset preset : all()) {
            RotClientProfileSettings s = preset.buildSettings(960, 540);

            for (PresetHudLayout.Placement p : resolved(preset, 960, 540)) {
                float[] actual = switch (p.id()) {
                    case PresetHudSizes.MINING_TRACKER ->
                            new float[] {s.miningHudX, s.miningHudY};
                    case PresetHudSizes.POWDER_CHEST ->
                            new float[] {s.powderChestHudX, s.powderChestHudY};
                    default -> s.qolUtilities.pose(p.id());
                };

                assertEquals(p.x(), actual[0], 0.001F, preset.id + " " + p.id() + " x");
                assertEquals(p.y(), actual[1], 0.001F, preset.id + " " + p.id() + " y");
            }
        }
    }

    @Test
    void leftAnchoredHudsStayPutAndRightAnchoredOnesFollowTheEdge() {
        RotClientProfilePreset preset = RotClientProfilePresets.findById("mining");
        assertNotNull(preset);

        RotClientProfileSettings small = preset.buildSettings(960, 540);
        RotClientProfileSettings large = preset.buildSettings(1920, 1080);

        // Pinned to the top-left corner, so the same at any window size...
        assertEquals(small.miningHudX, large.miningHudX);
        assertEquals(small.miningHudY, large.miningHudY);
        assertEquals(small.qolUtilities.pose("pet")[0], large.qolUtilities.pose("pet")[0]);

        // ...while the Powder Chest moves with the right edge.
        assertEquals(1920 - 960, large.powderChestHudX - small.powderChestHudX, 0.001F);
        assertEquals(small.powderChestHudY, large.powderChestHudY);
    }

    @Test
    void examplesNeverTouchAutomationOptions() {
        for (RotClientProfilePreset preset : all()) {
            List<String> ids = new ArrayList<>(preset.modules);
            ids.addAll(preset.settings.keySet());

            for (String id : ids) {
                assertFalse(id.contains("auto"), preset.id + " enables " + id);
                assertFalse(id.contains("cheat"), preset.id + " enables " + id);
            }
            for (var entry : preset.settings.entrySet()) {
                assertTrue(entry.getValue() != null, preset.id + " " + entry.getKey());
            }
        }
    }

    @Test
    void everyExampleUsesOnlySharedCatalogModules() {
        // Plus-only modules live in a different catalog, so findById on the
        // shared one is the Lite-safety check.
        for (RotClientProfilePreset preset : all()) {
            for (String module : preset.modules) {
                assertNotNull(QolUtilityCatalog.findById(module),
                        preset.id + " uses non-shared module " + module);
            }
        }
    }

    @Test
    void aBuiltProfileSurvivesTheNormalCopyAndJsonRoundTrip() {
        for (RotClientProfilePreset preset : all()) {
            RotClientProfile profile = RotClientProfile.create(preset.name);
            profile.settings = preset.buildSettings(960, 540);

            RotClientProfile copy = profile.duplicate(preset.name + " copy");

            assertTrue(copy.settings.qolUtilities.isModuleEnabled(
                    RotClientProfilePreset.CUSTOM_SCOREBOARD), preset.id);
            assertTrue(copy.settings.qolUtilities.isModuleEnabled(
                    RotClientProfilePreset.PET_HUD), preset.id);
            assertEquals(profile.settings.miningTrackerEnabled,
                    copy.settings.miningTrackerEnabled);
        }
    }

    @Test
    void aHandEditedBrokenPresetIsRejectedNotInstalled() {
        RotClientProfilePreset bad = new RotClientProfilePreset();
        bad.id = "bad";
        bad.name = "Bad";
        bad.modules.add("qol.does_not_exist");
        bad.settings.put("qol.nope.toggle", true);

        RotClientProfilePreset.LayoutStack stack = new RotClientProfilePreset.LayoutStack();
        stack.anchor = "SIDEWAYS";
        stack.elements.add("pet");
        bad.layout.add(stack);

        RotClientProfilePreset.LayoutStack twice = new RotClientProfilePreset.LayoutStack();
        twice.anchor = "TOP_LEFT";
        twice.elements.add("pet");
        twice.elements.add("pet");
        twice.elements.add("not_a_hud");
        bad.layout.add(twice);

        String problems = String.join("|", bad.problems());
        assertTrue(problems.contains("unknown module"), problems);
        assertTrue(problems.contains("unknown toggle"), problems);
        assertTrue(problems.contains("unknown anchor"), problems);
        assertTrue(problems.contains("placed twice"), problems);
        assertTrue(problems.contains("unknown HUD element"), problems);
    }

    private static List<PresetHudLayout.Placement> resolved(
            RotClientProfilePreset preset, int w, int h) {
        List<PresetHudLayout.Stack> stacks = new ArrayList<>();
        for (RotClientProfilePreset.LayoutStack stack : preset.layout) {
            stacks.add(new PresetHudLayout.Stack(
                    PresetHudLayout.Anchor.parse(stack.anchor), stack.elements));
        }
        return PresetHudLayout.resolve(stacks, w, h, PresetHudSizes::of);
    }
}
