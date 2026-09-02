package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

/**
 * Complete supported-target routing matrix. This intentionally distinguishes
 * selectable live targets from non-selectable observation-only materials.
 */
final class TrackerTargetMatrixTest {
    private static final List<TrackerSelection> MATERIAL_SELECTIONS = List.of(
            TrackerSelection.COAL,
            TrackerSelection.IRON,
            TrackerSelection.GOLD,
            TrackerSelection.LAPIS,
            TrackerSelection.REDSTONE,
            TrackerSelection.EMERALD,
            TrackerSelection.DIAMOND,
            TrackerSelection.QUARTZ,
            TrackerSelection.MITHRIL_TITANIUM,
            TrackerSelection.TUNGSTEN,
            TrackerSelection.UMBER);

    @Test
    void clientUiAndCommandsExposeExactlyEverySupportedSelection()
            throws IOException {
        assertEquals(
                MATERIAL_SELECTIONS.size() + GemstoneType.values().length,
                TrackerSelection.values().length);
        for (TrackerSelection selection : TrackerSelection.values()) {
            assertTrue(selection.supportsLiveTracking(), selection.id());
        }
        assertSame(TrackingTarget.TUNGSTEN,
                TrackingTarget.forMaterial(TrackedMaterial.TUNGSTEN));
        assertSame(TrackingTarget.UMBER,
                TrackingTarget.forMaterial(TrackedMaterial.UMBER));

        String ui = Files.readString(
                Path.of("src/client/java/fi/rotclient/MiningUiScreen.java"),
                StandardCharsets.UTF_8);
        assertTrue(ui.contains(
                "List.of(TrackerSelection.values())"));
        assertFalse(ui.contains("TrackerSelection.TUNGSTEN"));

        String client = Files.readString(
                Path.of("src/client/java/fi/rotclient/RotClientClient.java"),
                StandardCharsets.UTF_8);
        for (TrackerSelection selection : MATERIAL_SELECTIONS) {
            for (TrackedMaterial material
                    : selection.materialTarget().materials()) {
                String command = material == TrackedMaterial.TITANIUM
                        ? "titanium"
                        : material.displayName().toLowerCase();
                assertTrue(client.contains("literal(\"" + command + "\")"),
                        "command for " + selection.id());
            }
        }
        assertFalse(client.contains("tungstenWip"));
    }

    @Test
    void materialSelectionsRouteOnlyTheirExactAuthoritativeLedgers() {
        Map<TrackerSelection, List<TrackedMaterial>> expected = Map.ofEntries(
                Map.entry(TrackerSelection.COAL,
                        List.of(TrackedMaterial.COAL)),
                Map.entry(TrackerSelection.IRON,
                        List.of(TrackedMaterial.IRON)),
                Map.entry(TrackerSelection.GOLD,
                        List.of(TrackedMaterial.GOLD)),
                Map.entry(TrackerSelection.LAPIS,
                        List.of(TrackedMaterial.LAPIS)),
                Map.entry(TrackerSelection.REDSTONE,
                        List.of(TrackedMaterial.REDSTONE)),
                Map.entry(TrackerSelection.EMERALD,
                        List.of(TrackedMaterial.EMERALD)),
                Map.entry(TrackerSelection.DIAMOND,
                        List.of(TrackedMaterial.DIAMOND)),
                Map.entry(TrackerSelection.QUARTZ,
                        List.of(TrackedMaterial.QUARTZ)),
                Map.entry(TrackerSelection.MITHRIL_TITANIUM,
                        List.of(TrackedMaterial.MITHRIL,
                                TrackedMaterial.TITANIUM)),
                Map.entry(TrackerSelection.TUNGSTEN,
                        List.of(TrackedMaterial.TUNGSTEN)),
                Map.entry(TrackerSelection.UMBER,
                        List.of(TrackedMaterial.UMBER)));

        for (TrackerSelection selection : MATERIAL_SELECTIONS) {
            TrackerConfig config = new TrackerConfig();
            config.setSelectedSelection(selection);
            assertEquals(expected.get(selection), config.routedMaterials(),
                    selection.id());
            for (TrackedMaterial material : TrackedMaterial.values()) {
                assertEquals(
                        expected.get(selection).contains(material),
                        config.routesMaterial(material),
                        selection.id() + " -> " + material.id());
            }
        }
    }

    @Test
    void everySelectableMaterialRecognizesInventoryActionBarAndSackForms() {
        MiningResourceCatalog catalog = new MiningResourceCatalog();

        for (TrackerSelection selection : MATERIAL_SELECTIONS) {
            for (TrackedMaterial material
                    : selection.materialTarget().materials()) {
                ActionBarGainParser.Gain actionBar =
                        ActionBarGainParser.parse(
                                "+37 " + material.rawItemName())
                                .orElseThrow();
                assertSame(material, actionBar.material(), material.id());
                assertEquals(37L, actionBar.quantity(), material.id());

                assertSame(material,
                        catalog.fromExactSackItem(material.rawItemName())
                                .orElseThrow().material(),
                        material.id() + " raw Sack");
                assertSame(material,
                        catalog.fromExactSackItem(material.enchantedItemName())
                                .orElseThrow().material(),
                        material.id() + " enchanted Sack");
                assertEquals(material.rawPerEnchanted(),
                        material.displayMultiplier(
                                material.enchantedItemName()),
                        material.id() + " inventory enchanted unit");

                if (material.enchantedBlockItemName() != null) {
                    assertSame(material,
                            catalog.fromExactSackItem(
                                            material.enchantedBlockItemName())
                                    .orElseThrow().material(),
                            material.id() + " enchanted block Sack");
                    assertEquals(
                            (long) material.rawPerEnchanted()
                                    * material.enchantedPerBlock(),
                            material.displayMultiplier(
                                    material.enchantedBlockItemName()),
                            material.id() + " inventory enchanted block");
                }
            }
        }
    }

    @Test
    void everyGemstoneAcceptsItsOwnBlocksAndLiveTiersButRejectsAnotherType() {
        GemstoneType[] gemstones = GemstoneType.values();
        for (int index = 0; index < gemstones.length; index++) {
            GemstoneType selected = gemstones[index];
            GemstoneType wrong = gemstones[(index + 1) % gemstones.length];
            TrackerConfig config = new TrackerConfig();
            config.setSelectedSelection(TrackerSelection.forGemstone(selected));
            config.enabled = true;

            assertTrue(GemstoneLiveAccounting.recordBlock(
                    config, selected, 1_000L + index), selected.id());
            assertTrue(GemstoneLiveAccounting.recordGain(
                    config, selected, GemstoneTier.ROUGH, 100L + index),
                    selected.id());
            assertTrue(GemstoneLiveAccounting.recordGain(
                    config, selected, GemstoneTier.FLAWED, 2L + index),
                    selected.id());
            assertFalse(GemstoneLiveAccounting.recordBlock(
                    config, wrong, 2_000L + index),
                    selected.id() + " rejected " + wrong.id());
            assertFalse(GemstoneLiveAccounting.recordGain(
                    config, wrong, GemstoneTier.ROUGH, 999L),
                    selected.id() + " rejected " + wrong.id());

            GemstoneTrackerState selectedState =
                    config.gemstoneState(selected);
            assertEquals(1L, selectedState.sessionBlocks, selected.id());
            assertEquals(100L + index,
                    selectedState.sessionLedger().quantity(GemstoneTier.ROUGH),
                    selected.id());
            assertEquals(2L + index,
                    selectedState.sessionLedger().quantity(GemstoneTier.FLAWED),
                    selected.id());
            assertEquals(0L,
                    config.gemstoneState(wrong).sessionBlocks,
                    wrong.id());
            assertEquals(0L,
                    config.gemstoneState(wrong).sessionLedger().totalItemCount(),
                    wrong.id());
        }
    }

    @Test
    void everySupportedTargetCreditsCanonicalRowsAndSurvivesRestart() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            RotClientCurrentSession current = new RotClientCurrentSession();
            long started = current.snapshotConfig().startedAtMillis;
            current.onTargetChanged(selection.id(), started + 1L);

            if (selection.isMaterial()) {
                int offset = 0;
                for (TrackedMaterial material
                        : selection.materialTarget().materials()) {
                    assertTrue(TargetItemGainPipeline.creditMaterial(
                            current,
                            material,
                            10L + offset,
                            SkyBlockArea.DWARVEN_MINES,
                            started + 2L + offset),
                            selection.id() + " -> " + material.id());
                    offset++;
                }
            } else {
                assertTrue(TargetItemGainPipeline.creditGemstone(
                        current,
                        selection.gemstone(),
                        GemstoneTier.ROUGH,
                        20L,
                        SkyBlockArea.CRYSTAL_HOLLOWS,
                        started + 2L),
                        selection.id());
                assertTrue(TargetItemGainPipeline.creditGemstone(
                        current,
                        selection.gemstone(),
                        GemstoneTier.FLAWED,
                        3L,
                        SkyBlockArea.CRYSTAL_HOLLOWS,
                        started + 3L),
                        selection.id());
            }

            RotClientCurrentSessionConfig before = current.snapshotConfig();
            RotClientCurrentSessionConfig restored =
                    RotClientCurrentSessionStore.parseJson(
                            RotClientCurrentSessionStore.toJson(before));
            assertEquals(selection.id(), restored.currentTargetId,
                    selection.id());
            assertEquals(before.items, restored.items, selection.id());
            assertTrue(restored.items.stream().allMatch(row ->
                    row.source() == SessionSourceType.MINING
                            && row.miningClassification()
                            == MiningClassification.TARGET), selection.id());
        }
    }

    @Test
    void selectionSwitchesAndTrackerStoreRoundTripKeepEveryLedgerIsolated() {
        TrackerConfig config = new TrackerConfig();
        LinkedHashMap<GemstoneType, Long> expectedGemstoneAmounts =
                new LinkedHashMap<>();

        for (TrackerSelection selection : MATERIAL_SELECTIONS) {
            config.setSelectedSelection(selection);
            for (TrackedMaterial material : selection.materialTarget().materials()) {
                MaterialTrackerState state = config.state(material);
                long marker = 100L + material.ordinal();
                state.sessionBlocks = marker;
                state.sessionActualRawEquivalent = marker * 10L;
            }
        }
        for (GemstoneType gemstone : GemstoneType.values()) {
            config.setSelectedSelection(TrackerSelection.forGemstone(gemstone));
            config.enabled = true;
            long amount = 200L + gemstone.ordinal();
            expectedGemstoneAmounts.put(gemstone, amount);
            assertTrue(GemstoneLiveAccounting.recordGain(
                    config, gemstone, GemstoneTier.ROUGH, amount));
        }
        config.setSelectedSelection(TrackerSelection.PERIDOT);

        TrackerConfig restored = TrackerStore.fromJson(
                TrackerStore.toJson(config));
        assertSame(TrackerSelection.PERIDOT, restored.selectedSelection());
        for (TrackedMaterial material : TrackedMaterial.values()) {
            if (TrackingTarget.forMaterial(material) == null) {
                continue;
            }
            long marker = 100L + material.ordinal();
            assertEquals(marker,
                    restored.state(material).sessionBlocks, material.id());
            assertEquals(marker * 10L,
                    restored.state(material).sessionActualRawEquivalent,
                    material.id());
        }
        for (TrackedMaterial material : List.of(
                TrackedMaterial.HARD_STONE,
                TrackedMaterial.COBBLESTONE)) {
            assertEquals(0L, restored.state(material).sessionBlocks,
                    material.id());
        }
        for (Map.Entry<GemstoneType, Long> entry
                : expectedGemstoneAmounts.entrySet()) {
            assertEquals(entry.getValue().longValue(),
                    restored.gemstoneState(entry.getKey())
                            .sessionLedger().quantity(GemstoneTier.ROUGH),
                    entry.getKey().id());
        }
    }

    @Test
    void trackerStoreRoundTripsEverySelectableTargetId() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            TrackerConfig config = new TrackerConfig();
            config.setSelectedSelection(selection);
            TrackerConfig restored = TrackerStore.fromJson(
                    TrackerStore.toJson(config));
            assertSame(selection, restored.selectedSelection(), selection.id());
            assertEquals(selection.id(), restored.selectedTargetId,
                    selection.id());
        }
    }

    @Test
    void sessionResetClearsOnlyTheSelectedLedgerAcrossTheFullMatrix() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            TrackerConfig config = seededConfig();
            config.setSelectedSelection(selection);

            config.resetSelectedSessionState();

            for (TrackedMaterial material : TrackedMaterial.values()) {
                boolean shouldReset = selection.isMaterial()
                        && selection.materialTarget().includes(material);
                long expected = shouldReset ? 0L : 10L + material.ordinal();
                assertEquals(expected,
                        config.state(material).sessionBlocks,
                        selection.id() + " reset material " + material.id());
            }
            for (GemstoneType gemstone : GemstoneType.values()) {
                boolean shouldReset = selection.isGemstone()
                        && selection.gemstone() == gemstone;
                long expected = shouldReset ? 0L : 20L + gemstone.ordinal();
                assertEquals(expected,
                        config.gemstoneState(gemstone).sessionBlocks,
                        selection.id() + " reset gemstone " + gemstone.id());
                assertEquals(expected,
                        config.gemstoneState(gemstone)
                                .sessionLedger().quantity(GemstoneTier.ROUGH),
                        selection.id() + " reset quantity " + gemstone.id());
            }
        }
    }

    @Test
    void everySelectionProtectsTargetFamilyAndStillAcceptsCorrelatedOthers() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            Fixture fixture = fixture(selection);

            for (SackChangeParser.Change targetChange
                    : targetFamilyChanges(selection)) {
                fixture.clock.set(20L);
                SackItemGainPipeline.Outcome target =
                        SackItemGainPipeline.submit(
                                fixture.context(), targetChange);
                assertTrue(target.decision().targetFamilyProtected(),
                        selection.id() + " -> " + targetChange.itemName());
                assertFalse(target.creditedOthers(), selection.id());
            }

            fixture.engine.onConfirmedMaterialBreak(
                    TrackedMaterial.HARD_STONE, 1, 100L);
            fixture.clock.set(110L);
            SackItemGainPipeline.Outcome other = SackItemGainPipeline.submit(
                    fixture.context(),
                    sack("Hard Stone", 64L, "Mining Sack"));
            assertTrue(other.creditedOthers(), selection.id());
            assertEquals(64L,
                    otherQuantity(fixture.session, "HARD_STONE"),
                    selection.id());
            assertEquals(0L,
                    fixture.session.snapshotConfig().items.stream()
                            .filter(row -> row.miningClassification()
                                    == MiningClassification.TARGET)
                            .mapToLong(
                                    RotClientCurrentSessionConfig
                                            .SessionItemRecord::quantity)
                            .sum(),
                    "target Sack protection must not create canonical duplicates");
        }
    }

    @Test
    void everySelectionTracksInEveryKnownAndUnknownAreaWithoutLocationGating() {
        for (TrackerSelection selection : TrackerSelection.values()) {
            for (SkyBlockArea area : SkyBlockArea.values()) {
                Fixture fixture = fixture(selection, area);

                if (selection.isMaterial()) {
                    int offset = 0;
                    for (TrackedMaterial material
                            : selection.materialTarget().materials()) {
                        assertTrue(MiningBlockEvidencePolicy.allows(
                                        material.id(),
                                        SkyBlockLocation.of(area),
                                        true),
                                selection.id() + " detector in " + area.id());
                        assertTrue(TargetItemGainPipeline.creditMaterial(
                                fixture.session,
                                material,
                                10L + offset,
                                area,
                                20L + offset),
                                selection.id() + " target in " + area.id());
                        offset++;
                    }
                } else {
                    assertTrue(TargetItemGainPipeline.creditGemstone(
                            fixture.session,
                            selection.gemstone(),
                            GemstoneTier.ROUGH,
                            10L,
                            area,
                            20L),
                            selection.id() + " target in " + area.id());
                }

                fixture.engine.onConfirmedMaterialBreak(
                        TrackedMaterial.HARD_STONE, 1, 100L);
                fixture.clock.set(110L);
                SackItemGainPipeline.Outcome other = SackItemGainPipeline.submit(
                        fixture.context(),
                        sack("Hard Stone", 64L, "Mining Sack"));
                assertTrue(other.creditedOthers(),
                        selection.id() + " OTHERS in " + area.id());

                RotClientCurrentSessionConfig snapshot =
                        fixture.session.snapshotConfig();
                assertTrue(snapshot.items.stream().allMatch(row ->
                                area.id().equals(row.areaId())),
                        selection.id() + " retained area " + area.id());
                assertEquals(64L,
                        otherQuantity(fixture.session, "HARD_STONE"),
                        selection.id() + " OTHERS quantity in " + area.id());
            }
        }
    }

    private static Fixture fixture(TrackerSelection selection) {
        return fixture(
                selection,
                selection.isGemstone()
                        ? SkyBlockArea.CRYSTAL_HOLLOWS
                        : SkyBlockArea.DWARVEN_MINES);
    }

    private static Fixture fixture(
            TrackerSelection selection,
            SkyBlockArea area) {
        MiningSessionEngine engine = new MiningSessionEngine();
        engine.onDiagnosticStart(
                true,
                selection,
                baseline(selection, 10L),
                10L);
        RotClientCurrentSession session = new RotClientCurrentSession();
        session.ensureActiveForTracker(selection, 10L);
        return new Fixture(
                selection,
                engine,
                session,
                new MiningResourceCatalog(),
                new AtomicLong(10L),
                area);
    }

    private static TrackerConfig seededConfig() {
        TrackerConfig config = new TrackerConfig();
        for (TrackedMaterial material : TrackedMaterial.values()) {
            config.state(material).sessionBlocks = 10L + material.ordinal();
        }
        for (GemstoneType gemstone : GemstoneType.values()) {
            long marker = 20L + gemstone.ordinal();
            GemstoneTrackerState state = config.gemstoneState(gemstone);
            state.sessionBlocks = marker;
            state.recordGain(GemstoneTier.ROUGH, marker);
        }
        return config;
    }

    private static MiningSessionParity.LiveBaseline baseline(
            TrackerSelection selection,
            long at) {
        if (selection.isGemstone()) {
            EnumMap<GemstoneTier, Long> quantities =
                    new EnumMap<>(GemstoneTier.class);
            for (GemstoneTier tier : GemstoneTier.values()) {
                quantities.put(tier, 0L);
            }
            return MiningSessionParity.LiveBaseline.gemstone(
                    selection, quantities, 0L, 0L, at);
        }
        EnumMap<TrackedMaterial, Long> quantities =
                new EnumMap<>(TrackedMaterial.class);
        EnumMap<TrackedMaterial, Long> blocks =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            quantities.put(material, 0L);
            blocks.put(material, 0L);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection, quantities, blocks, at);
    }

    private static List<SackChangeParser.Change> targetFamilyChanges(
            TrackerSelection selection) {
        if (selection.isGemstone()) {
            return List.of(sack(
                    selection.gemstone().itemName(GemstoneTier.ROUGH),
                    5L,
                    "Gemstone Sack"));
        }
        List<SackChangeParser.Change> changes = new ArrayList<>();
        for (TrackedMaterial material
                : selection.materialTarget().materials()) {
            changes.add(sack(
                    material.rawItemName(), 5L, "Mining Sack"));
            changes.add(sack(
                    material.enchantedItemName(), 2L,
                    "Enchanted Mining Sack"));
            if (material.enchantedBlockItemName() != null) {
                changes.add(sack(
                        material.enchantedBlockItemName(), 1L,
                        "Enchanted Mining Sack"));
            }
        }
        return List.copyOf(changes);
    }

    private static SackChangeParser.Change sack(
            String itemName,
            long delta,
            String sackName) {
        return new SackChangeParser.Change(
                delta, itemName, List.of(sackName));
    }

    private static long otherQuantity(
            RotClientCurrentSession session,
            String itemId) {
        return session.snapshotConfig().items.stream()
                .filter(row -> itemId.equals(row.itemId()))
                .filter(row -> row.source() == SessionSourceType.MINING)
                .filter(row -> row.miningClassification()
                        == MiningClassification.OTHER)
                .mapToLong(
                        RotClientCurrentSessionConfig.SessionItemRecord::quantity)
                .sum();
    }

    private record Fixture(
            TrackerSelection selection,
            MiningSessionEngine engine,
            RotClientCurrentSession session,
            MiningResourceCatalog catalog,
            AtomicLong clock,
            SkyBlockArea area) {
        SackItemGainPipeline.Context context() {
            return new SackItemGainPipeline.Context(
                    engine,
                    session,
                    catalog,
                    () -> selection,
                    () -> area,
                    () -> true,
                    clock::get);
        }
    }
}
