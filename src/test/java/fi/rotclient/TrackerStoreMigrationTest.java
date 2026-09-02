package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TrackerStoreMigrationTest {
    @Test
    void v4FlatGoldDataMovesToGoldLedgerAndPreservesGlobalSettings() {
        JsonObject legacy = JsonParser.parseString("""
                {
                  "dataVersion": 4,
                  "enabled": true,
                  "showBlocks": false,
                  "showRawGold": false,
                  "showEnchantedGold": true,
                  "showGoldPerHour": false,
                  "showSessionProfit": false,
                  "showUnsoldValue": false,
                  "showCoinsPerHour": false,
                  "showSessionTime": false,
                  "showActiveTool": false,
                  "showRateGraph": false,
                  "showDropAndFortune": false,
                  "showBazaarPrices": false,
                  "miningTrackerPanelOpen": true,
                  "x": 42.5,
                  "y": 81.25,
                  "scale": 1.75,
                  "bazaarTaxPercent": 1.75,
                  "miningFortune": 1450.5,
                  "oreFortune": 350.25,
                  "fortuneAuto": false,
                  "fortuneLastDetectedEpochMillis": 123456,
                  "fortuneSource": "Player Stats",
                  "totalBlocks": 9001,
                  "totalActiveMillis": 8002,
                  "sessionBlocks": 7003,
                  "sessionActiveMillis": 6004,
                  "lastBreakEpochMillis": 5005,
                  "lastGoldPrice": 4.25,
                  "lastGoldSellOfferPrice": 4.5,
                  "lastEnchantedGoldPrice": 680.75,
                  "lastPriceUpdateEpochMillis": 4006,
                  "sessionActualGold": 3007,
                  "totalActualGold": 2008,
                  "sessionInventoryRawGold": 1009,
                  "totalInventoryRawGold": 9010,
                  "sessionCompactBonusEnchanted": 11,
                  "totalCompactBonusEnchanted": 12,
                  "sessionSackRawGold": 13,
                  "totalSackRawGold": 14,
                  "sessionSackEnchantedGold": 15,
                  "totalSackEnchantedGold": 16,
                  "sessionCompactedGold": 17,
                  "totalCompactedGold": 18,
                  "sessionSoldRawGold": 19,
                  "sessionSoldEnchantedGold": 20,
                  "sessionRealizedGrossCoins": 2100.5,
                  "compactorRawRemainder": 22,
                  "lastActualGoldEpochMillis": 2300,
                  "actualGoldSource": "inventory + Mining Sacks",
                  "selectedMaterialId": "not-a-material"
                }
                """).getAsJsonObject();

        TrackerConfig config = TrackerStore.fromJson(legacy);

        MaterialTrackerState gold =
                config.state(TrackedMaterial.GOLD);
        MaterialTrackerState diamond =
                config.state(TrackedMaterial.DIAMOND);
        MaterialTrackerState mithril =
                config.state(TrackedMaterial.MITHRIL);
        MaterialTrackerState titanium =
                config.state(TrackedMaterial.TITANIUM);
        MaterialTrackerState tungsten =
                config.state(TrackedMaterial.TUNGSTEN);

        assertEquals(
                TrackerConfig.CURRENT_DATA_VERSION,
                config.dataVersion);
        assertSame(
                TrackedMaterial.GOLD,
                config.selectedMaterial());
        assertSame(
                TrackingTarget.GOLD,
                config.selectedTarget());
        assertEquals(
                TrackingTarget.GOLD.id(),
                config.selectedTargetId);

        assertTrue(config.enabled);
        assertFalse(config.showBlocks);
        assertFalse(config.showRawMaterial);
        assertTrue(config.showEnchantedMaterial);
        assertFalse(config.showMaterialPerHour);
        assertFalse(config.showSessionProfit);
        assertFalse(config.showUnsoldValue);
        assertFalse(config.showCoinsPerHour);
        assertFalse(config.showSessionTime);
        assertFalse(config.showActiveTool);
        assertFalse(config.showRateGraph);
        assertFalse(config.showDropAndFortune);
        assertFalse(config.showBazaarPrices);
        assertTrue(config.miningTrackerPanelOpen);

        assertSame(
                DashboardModule.MINING_TRACKER,
                config.selectedDashboardModule());

        assertEquals(42.5F, config.x);
        assertEquals(81.25F, config.y);
        assertEquals(1.75F, config.scale);
        assertEquals(1.75, config.bazaarTaxPercent);
        assertEquals(1450.5, config.miningFortune);
        assertEquals(350.25, config.oreFortune);
        assertFalse(config.fortuneAuto);
        assertEquals(
                123456,
                config.fortuneLastDetectedEpochMillis);
        assertEquals(
                "Player Stats",
                config.fortuneSource);

        assertEquals(9001, gold.totalBlocks);
        assertEquals(45_005, gold.totalBaseDrops);
        assertEquals(8002, gold.totalActiveMillis);
        assertEquals(7003, gold.sessionBlocks);
        assertEquals(35_015, gold.sessionBaseDrops);
        assertEquals(6004, gold.sessionActiveMillis);
        assertEquals(5005, gold.lastBreakEpochMillis);
        assertEquals(4.25, gold.lastRawPrice);
        assertEquals(4.5, gold.lastRawSellOfferPrice);
        assertEquals(680.75, gold.lastEnchantedPrice);
        assertEquals(4006, gold.lastPriceUpdateEpochMillis);
        assertEquals(3007, gold.sessionActualRawEquivalent);
        assertEquals(2008, gold.totalActualRawEquivalent);
        assertEquals(1009, gold.sessionInventoryRaw);
        assertEquals(9010, gold.totalInventoryRaw);
        assertEquals(11, gold.sessionCompactBonusEnchanted);
        assertEquals(12, gold.totalCompactBonusEnchanted);
        assertEquals(13, gold.sessionSackRaw);
        assertEquals(14, gold.totalSackRaw);
        assertEquals(15, gold.sessionSackEnchanted);
        assertEquals(16, gold.totalSackEnchanted);
        assertEquals(17, gold.sessionCompactedEnchanted);
        assertEquals(18, gold.totalCompactedEnchanted);
        assertEquals(19, gold.sessionSoldRaw);
        assertEquals(20, gold.sessionSoldEnchanted);
        assertEquals(2100.5, gold.sessionRealizedGrossCoins);
        assertEquals(22, gold.compactorRawRemainder);
        assertEquals(
                2300,
                gold.lastActualRawEquivalentEpochMillis);
        assertEquals(
                "inventory + Mining Sacks",
                gold.actualRawEquivalentSource);

        /*
         * Vanha Gold-tallennus ei saa luoda tavaraa muiden
         * materiaalien laatikoihin.
         */
        assertEquals(0, diamond.totalBlocks);
        assertEquals(0, mithril.totalBlocks);
        assertEquals(0, titanium.totalBlocks);
        assertEquals(0, tungsten.totalBlocks);
    }

    @Test
    void v7SerializationUsesTargetAndSeparateMaterialLedgers() {
        TrackerConfig config = new TrackerConfig();

        config.showRawMaterial = false;
        config.showEnchantedMaterial = false;
        config.showMaterialPerHour = false;
        config.selectedTargetId = TrackingTarget.DIAMOND.id();

        config.state(TrackedMaterial.DIAMOND).sessionBlocks = 27;

        JsonObject json = TrackerStore.toJson(config);

        assertEquals(
                TrackerConfig.CURRENT_DATA_VERSION,
                json.get("dataVersion").getAsInt());

        assertEquals(
                "DIAMOND",
                json.get("selectedTargetId").getAsString());

        assertFalse(json.has("selectedMaterialId"));
        assertTrue(json.has("showRawMaterial"));
        assertTrue(json.has("showEnchantedMaterial"));
        assertTrue(json.has("showMaterialPerHour"));

        assertFalse(json.has("showRawGold"));
        assertFalse(json.has("showEnchantedGold"));
        assertFalse(json.has("showGoldPerHour"));
        assertFalse(json.has("sessionBlocks"));
        assertFalse(json.has("lastGoldPrice"));
        assertFalse(json.has("sessionActualGold"));

        JsonObject states =
                json.getAsJsonObject("materialStates");

        assertTrue(states.has("GOLD"));
        assertTrue(states.has("DIAMOND"));
        assertTrue(states.has("MITHRIL"));
        assertTrue(states.has("TITANIUM"));
        assertTrue(states.has("TUNGSTEN"));

        assertEquals(
                27,
                states.getAsJsonObject("DIAMOND")
                        .get("sessionBlocks")
                        .getAsLong());
    }

    @Test
    void v8RoundTripPersistsGemstoneStatesAndCanonicalizesKeys() {
        TrackerConfig original = new TrackerConfig();

        GemstoneTrackerState ruby = original.gemstoneState(GemstoneType.RUBY);
        ruby.recordGain(GemstoneTier.ROUGH, 2);
        ruby.recordGain(GemstoneTier.FLAWLESS, 1);
        ruby.recordBlock(1234L);
        ruby.addActiveMillis(250L);

        JsonObject json = TrackerStore.toJson(original);
        JsonObject gemstoneStates = json.getAsJsonObject("gemstoneStates");

        assertTrue(gemstoneStates.has("RUBY"));
        assertFalse(gemstoneStates.has("ruby"));

        TrackerConfig restored = TrackerStore.fromJson(json);
        GemstoneTrackerState restoredRuby = restored.gemstoneState(GemstoneType.RUBY);

        assertEquals(1, restoredRuby.sessionBlocks);
        assertEquals(1, restoredRuby.totalBlocks);
        assertEquals(250, restoredRuby.sessionActiveMillis);
        assertEquals(250, restoredRuby.totalActiveMillis);
        assertEquals(2, restoredRuby.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(1, restoredRuby.totalLedger().quantity(GemstoneTier.FLAWLESS));
    }

    @Test
    void v8NormalizeRepairsNullOrNegativeGemstoneStateData() {
        TrackerConfig original = new TrackerConfig();
        original.gemstoneState(GemstoneType.JADE).sessionBlocks = -7;
        original.gemstoneState(GemstoneType.JADE).totalBlocks = -3;
        original.gemstoneState(GemstoneType.JADE).sessionActiveMillis = -11;
        original.gemstoneState(GemstoneType.JADE).totalActiveMillis = -5;
        original.gemstoneState(GemstoneType.JADE).lastBreakEpochMillis = -9;
        original.gemstoneState(GemstoneType.JADE).sessionLedger().normalize();
        original.gemstoneState(GemstoneType.JADE).totalLedger().normalize();

        JsonObject json = TrackerStore.toJson(original);
        JsonObject gemstoneStates = json.getAsJsonObject("gemstoneStates");
        JsonObject jade = new JsonObject();
        jade.addProperty("sessionBlocks", -4);
        jade.addProperty("totalBlocks", -2);
        jade.addProperty("sessionActiveMillis", -12);
        jade.addProperty("totalActiveMillis", -6);
        jade.addProperty("lastBreakEpochMillis", -8);
        gemstoneStates.add("jade", jade);

        TrackerConfig restored = TrackerStore.fromJson(json);
        GemstoneTrackerState restoredJade = restored.gemstoneState(GemstoneType.JADE);

        assertEquals(0, restoredJade.sessionBlocks);
        assertEquals(0, restoredJade.totalBlocks);
        assertEquals(0, restoredJade.sessionActiveMillis);
        assertEquals(0, restoredJade.totalActiveMillis);
        assertEquals(0, restoredJade.lastBreakEpochMillis);
        assertEquals(0, restoredJade.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(0, restoredJade.totalLedger().quantity(GemstoneTier.ROUGH));
    }

    @Test
    void selectionNormalizationCanonicalizesGemstonesAndMaterials() {
        TrackerConfig config = new TrackerConfig();
        config.setSelectedSelection(
                TrackerSelection.forGemstone(GemstoneType.RUBY));
        config.selectedTargetId = "gemstone_ruby";
        config.state(TrackedMaterial.GOLD).sessionBlocks = 5;
        config.state(TrackedMaterial.DIAMOND).sessionBlocks = 6;
        config.gemstoneState(GemstoneType.RUBY).recordGain(GemstoneTier.ROUGH, 2);

        config.normalize();

        assertEquals("GEMSTONE_RUBY", config.selectedTargetId);
        assertSame(TrackerSelection.RUBY, config.selectedSelection());
        assertSame(GemstoneType.RUBY, config.selectedGemstone());
        assertSame(TrackingTarget.GOLD, config.selectedTarget());
        assertSame(TrackedMaterial.GOLD, config.selectedMaterial());
        assertEquals(5, config.state(TrackedMaterial.GOLD).sessionBlocks);
        assertEquals(6, config.state(TrackedMaterial.DIAMOND).sessionBlocks);
        assertEquals(2, config.gemstoneState(GemstoneType.RUBY).sessionLedger().quantity(GemstoneTier.ROUGH));
    }

    @Test
    void setSelectedSelectionStoresCanonicalIdAndHandlesNull() {
        TrackerConfig config = new TrackerConfig();

        config.setSelectedSelection(TrackerSelection.forGemstone(GemstoneType.JADE));
        assertEquals("GEMSTONE_JADE", config.selectedTargetId);

        config.setSelectedSelection(null);
        assertEquals("GOLD", config.selectedTargetId);
        assertSame(TrackerSelection.GOLD, config.selectedSelection());
    }

    @Test
    void currentSelectionAliasesKeepStandaloneTungsten() {
        for (String legacyId : new String[]{
                "MITHRIL_TUNGSTEN",
                "MITHRIL",
                "TITANIUM"}) {
            TrackerConfig config = new TrackerConfig();
            config.selectedTargetId = legacyId;
            config.normalize();

            assertEquals(
                    TrackerSelection.MITHRIL_TITANIUM.id(),
                    config.selectedTargetId);
            assertSame(TrackerSelection.MITHRIL_TITANIUM, config.selectedSelection());
        }

        TrackerConfig tungsten = new TrackerConfig();
        tungsten.selectedTargetId = "TUNGSTEN";
        tungsten.normalize();

        assertEquals(TrackerSelection.TUNGSTEN.id(), tungsten.selectedTargetId);
        assertSame(TrackerSelection.TUNGSTEN, tungsten.selectedSelection());
    }

    @Test
    void preV10TungstenAliasMigratesWithoutMovingItsLedger() {
        JsonObject legacy = JsonParser.parseString("""
                {
                  "dataVersion": 9,
                  "selectedTargetId": "TUNGSTEN",
                  "materialStates": {
                    "TUNGSTEN": {
                      "sessionBlocks": 17,
                      "sessionBaseDrops": 29
                    }
                  }
                }
                """).getAsJsonObject();

        TrackerConfig migrated = TrackerStore.fromJson(legacy);

        assertSame(
                TrackerSelection.MITHRIL_TITANIUM,
                migrated.selectedSelection());
        assertEquals(17,
                migrated.state(TrackedMaterial.TUNGSTEN).sessionBlocks);
        assertEquals(29,
                migrated.state(TrackedMaterial.TUNGSTEN).sessionBaseDrops);
        assertEquals(0,
                migrated.state(TrackedMaterial.TITANIUM).sessionBlocks);
    }

    @Test
    void v10RoundTripKeepsStandaloneTungstenSelection() {
        TrackerConfig original = new TrackerConfig();
        original.setSelectedSelection(TrackerSelection.TUNGSTEN);
        original.state(TrackedMaterial.TUNGSTEN).sessionBlocks = 11;

        TrackerConfig restored = TrackerStore.fromJson(
                TrackerStore.toJson(original));

        assertSame(TrackerSelection.TUNGSTEN, restored.selectedSelection());
        assertEquals(11,
                restored.state(TrackedMaterial.TUNGSTEN).sessionBlocks);
    }

    @Test
    void unknownSelectionIdsNormalizeToGold() {
        TrackerConfig config = new TrackerConfig();
        config.selectedTargetId = "not-a-selection";

        config.normalize();

        assertEquals("GOLD", config.selectedTargetId);
        assertSame(TrackerSelection.GOLD, config.selectedSelection());
        assertNull(config.selectedGemstone());
    }

    @Test
    void futureSchemaRemainsMarkedNewerAcrossParseAndSerialization() {
        int futureVersion = TrackerConfig.CURRENT_DATA_VERSION + 7;
        JsonObject future = TrackerStore.toJson(new TrackerConfig());
        future.addProperty("dataVersion", futureVersion);
        future.addProperty("futureOwnedField", "keep the source file read-only");

        TrackerConfig parsed = TrackerStore.fromJson(future);
        JsonObject serialized = TrackerStore.toJson(parsed);

        assertEquals(futureVersion, parsed.dataVersion);
        assertEquals(futureVersion, serialized.get("dataVersion").getAsInt());
    }

    @Test
    void trackerStoreRoundTripPreservesGemstoneSelectionAndStateData() {
        TrackerConfig original = new TrackerConfig();
        original.setSelectedSelection(TrackerSelection.forGemstone(GemstoneType.AQUAMARINE));
        original.state(TrackedMaterial.GOLD).sessionBlocks = 9;
        original.state(TrackedMaterial.DIAMOND).totalBlocks = 3;
        original.gemstoneState(GemstoneType.AQUAMARINE).recordGain(GemstoneTier.FINE, 4);
        original.gemstoneState(GemstoneType.AQUAMARINE).recordBlock(42L);

        JsonObject json = TrackerStore.toJson(original);
        TrackerConfig restored = TrackerStore.fromJson(json);

        assertEquals("GEMSTONE_AQUAMARINE", restored.selectedTargetId);
        assertSame(TrackerSelection.AQUAMARINE, restored.selectedSelection());
        assertSame(GemstoneType.AQUAMARINE, restored.selectedGemstone());
        assertSame(TrackingTarget.GOLD, restored.selectedTarget());
        assertSame(TrackedMaterial.GOLD, restored.selectedMaterial());
        assertEquals(9, restored.state(TrackedMaterial.GOLD).sessionBlocks);
        assertEquals(3, restored.state(TrackedMaterial.DIAMOND).totalBlocks);
        assertEquals(4, restored.gemstoneState(GemstoneType.AQUAMARINE).sessionLedger().quantity(GemstoneTier.FINE));
        assertEquals(1, restored.gemstoneState(GemstoneType.AQUAMARINE).sessionBlocks);
    }

    @Test
    void resetSessionKeepsLifetimeTotalsAndPrices() {
        MaterialTrackerState state = new MaterialTrackerState();

        state.totalBlocks = 100;
        state.totalBaseDrops = 500;
        state.totalActualRawEquivalent = 200;
        state.lastRawPrice = 3.5;

        state.sessionBlocks = 7;
        state.sessionBaseDrops = 35;
        state.sessionActualRawEquivalent = 35;
        state.sessionRealizedGrossCoins = 99;
        state.compactorRawRemainder = 12;

        state.resetSession();

        assertEquals(100, state.totalBlocks);
        assertEquals(500, state.totalBaseDrops);
        assertEquals(200, state.totalActualRawEquivalent);
        assertEquals(3.5, state.lastRawPrice);

        assertEquals(0, state.sessionBlocks);
        assertEquals(0, state.sessionBaseDrops);
        assertEquals(0, state.sessionActualRawEquivalent);
        assertEquals(0, state.sessionRealizedGrossCoins);
        assertEquals(0, state.compactorRawRemainder);
        assertEquals("calibrating", state.actualRawEquivalentSource);
    }

    @Test
    void v7RoundTripKeepsEveryMaterialLedgerIndependent() {
        TrackerConfig original = new TrackerConfig();

        original.selectedTargetId =
                TrackingTarget.MITHRIL_TITANIUM.id();
        original.fullbrightEnabled = true;
        original.autoSprintEnabled = true;
        original.cameraEnabled = true;

        original.setSelectedDashboardModule(
                DashboardModule.QOL_SETTINGS);

        original.state(TrackedMaterial.GOLD)
                .sessionBlocks = 41;
        original.state(TrackedMaterial.GOLD)
                .sessionActualRawEquivalent = 205;

        original.state(TrackedMaterial.DIAMOND)
                .sessionBlocks = 17;
        original.state(TrackedMaterial.DIAMOND)
                .sessionActualRawEquivalent = 1_360;

        original.state(TrackedMaterial.MITHRIL)
                .sessionBlocks = 23;
        original.state(TrackedMaterial.MITHRIL)
                .sessionBaseDrops = 51;
        original.state(TrackedMaterial.MITHRIL)
                .sessionActualRawEquivalent = 2_040;

        original.state(TrackedMaterial.TITANIUM)
                .sessionBlocks = 11;
        original.state(TrackedMaterial.TITANIUM)
                .sessionBaseDrops = 22;
        original.state(TrackedMaterial.TITANIUM)
                .sessionActualRawEquivalent = 880;

        original.state(TrackedMaterial.TUNGSTEN)
                .sessionBlocks = 19;
        original.state(TrackedMaterial.TUNGSTEN)
                .sessionBaseDrops = 31;
        original.state(TrackedMaterial.TUNGSTEN)
                .sessionActualRawEquivalent = 1_240;

        /*
         * Round trip tarkoittaa:
         *
         * Java-olio → JSON → Java-olio
         */
        TrackerConfig restored =
                TrackerStore.fromJson(
                        TrackerStore.toJson(original));

        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                restored.selectedTarget());

        assertSame(
                TrackedMaterial.MITHRIL,
                restored.selectedMaterial());

        assertTrue(restored.fullbrightEnabled);
        assertTrue(restored.autoSprintEnabled);
        assertTrue(restored.cameraEnabled);

        assertSame(
                DashboardModule.QOL_SETTINGS,
                restored.selectedDashboardModule());

        assertFalse(restored.miningTrackerPanelOpen);

        assertEquals(
                41,
                restored.state(TrackedMaterial.GOLD)
                        .sessionBlocks);
        assertEquals(
                205,
                restored.state(TrackedMaterial.GOLD)
                        .sessionActualRawEquivalent);

        assertEquals(
                17,
                restored.state(TrackedMaterial.DIAMOND)
                        .sessionBlocks);
        assertEquals(
                1_360,
                restored.state(TrackedMaterial.DIAMOND)
                        .sessionActualRawEquivalent);

        assertEquals(
                23,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionBlocks);
        assertEquals(
                51,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionBaseDrops);
        assertEquals(
                2_040,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionActualRawEquivalent);

        assertEquals(
                11,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionBlocks);
        assertEquals(
                22,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionBaseDrops);
        assertEquals(
                880,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionActualRawEquivalent);

        assertEquals(
                19,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionBlocks);
        assertEquals(
                31,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionBaseDrops);
        assertEquals(
                1_240,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionActualRawEquivalent);

        /*
         * Tyhjennetään vain Mithrilin session tiedot.
         * Titaniumin ja Tungstenin pitää säilyä.
         */
        restored.state(TrackedMaterial.MITHRIL)
                .resetSession();

        assertEquals(
                0,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionBlocks);

        assertEquals(
                11,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionBlocks);

        assertEquals(
                19,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionBlocks);

        assertEquals(
                41,
                restored.state(TrackedMaterial.GOLD)
                        .sessionBlocks);

        assertEquals(
                17,
                restored.state(TrackedMaterial.DIAMOND)
                        .sessionBlocks);
    }

    @Test
    void invalidSelectedTargetFallsBackWithoutErasingLedgers() {
        TrackerConfig original = new TrackerConfig();

        original.selectedTargetId =
                "future-target-that-is-not-installed";

        original.state(TrackedMaterial.GOLD)
                .sessionBlocks = 12;
        original.state(TrackedMaterial.DIAMOND)
                .sessionBlocks = 34;
        original.state(TrackedMaterial.MITHRIL)
                .sessionBlocks = 56;
        original.state(TrackedMaterial.TITANIUM)
                .sessionBlocks = 67;
        original.state(TrackedMaterial.TUNGSTEN)
                .sessionBlocks = 78;

        JsonObject json = TrackerStore.toJson(original);
        json.addProperty("selectedTargetId", "invalid");

        TrackerConfig restored =
                TrackerStore.fromJson(json);

        assertSame(
                TrackedMaterial.GOLD,
                restored.selectedMaterial());

        assertEquals(
                12,
                restored.state(TrackedMaterial.GOLD)
                        .sessionBlocks);
        assertEquals(
                34,
                restored.state(TrackedMaterial.DIAMOND)
                        .sessionBlocks);
        assertEquals(
                56,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionBlocks);
        assertEquals(
                67,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionBlocks);
        assertEquals(
                78,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionBlocks);
    }

    @Test
    void v6MithrilTungstenTargetMigratesWithoutRelabelingTungsten() {
        TrackerConfig original = new TrackerConfig();

        original.state(TrackedMaterial.MITHRIL)
                .sessionBlocks = 56;
        original.state(TrackedMaterial.MITHRIL)
                .sessionActualRawEquivalent = 560;

        original.state(TrackedMaterial.TUNGSTEN)
                .sessionBlocks = 78;
        original.state(TrackedMaterial.TUNGSTEN)
                .sessionActualRawEquivalent = 780;

        JsonObject json = TrackerStore.toJson(original);

        /*
         * Tehdään muistissa keinotekoinen vanhan version tallennus.
         */
        json.addProperty("dataVersion", 6);
        json.addProperty(
                "selectedTargetId",
                "MITHRIL_TUNGSTEN");

        /*
         * Vanhan version tallennuksessa Titanium-laatikkoa
         * ei vielä ollut.
         */
        json.getAsJsonObject("materialStates")
                .remove("TITANIUM");

        TrackerConfig restored =
                TrackerStore.fromJson(json);

        assertSame(
                TrackingTarget.MITHRIL_TITANIUM,
                restored.selectedTarget());

        /*
         * Mithril-luvut säilyvät Mithrilillä.
         */
        assertEquals(
                56,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionBlocks);
        assertEquals(
                560,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionActualRawEquivalent);

        /*
         * Uusi Titanium-ledger alkaa tyhjänä.
         */
        assertEquals(
                0,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionBlocks);
        assertEquals(
                0,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionActualRawEquivalent);

        /*
         * Tungsten-luvut säilyvät Tungstenilla.
         */
        assertEquals(
                78,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionBlocks);
        assertEquals(
                780,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionActualRawEquivalent);
    }

    @Test
    void v5SeparateSelectionsMigrateToMithrilTitaniumTarget() {
        for (String legacySelection : new String[]{
                "MITHRIL",
                "TITANIUM",
                "TUNGSTEN"}) {

            TrackerConfig original = new TrackerConfig();

            original.state(TrackedMaterial.MITHRIL)
                    .sessionBlocks = 56;
            original.state(TrackedMaterial.TUNGSTEN)
                    .sessionBlocks = 78;

            JsonObject json = TrackerStore.toJson(original);

            json.addProperty("dataVersion", 5);
            json.remove("selectedTargetId");
            json.addProperty(
                    "selectedMaterialId",
                    legacySelection);

            json.getAsJsonObject("materialStates")
                    .remove("TITANIUM");

            TrackerConfig restored =
                    TrackerStore.fromJson(json);

            assertSame(
                    TrackingTarget.MITHRIL_TITANIUM,
                    restored.selectedTarget());

            assertEquals(
                    56,
                    restored.state(TrackedMaterial.MITHRIL)
                            .sessionBlocks);

            assertEquals(
                    0,
                    restored.state(TrackedMaterial.TITANIUM)
                            .sessionBlocks);

            assertEquals(
                    78,
                    restored.state(TrackedMaterial.TUNGSTEN)
                            .sessionBlocks);
        }
    }

    @Test
    void existingLedgersInferMissingWeightedBaseDrops() {
        TrackerConfig original = new TrackerConfig();

        original.state(TrackedMaterial.GOLD)
                .totalBlocks = 9;
        original.state(TrackedMaterial.GOLD)
                .sessionBlocks = 4;

        original.state(TrackedMaterial.DIAMOND)
                .totalBlocks = 7;
        original.state(TrackedMaterial.DIAMOND)
                .sessionBlocks = 3;

        JsonObject json = TrackerStore.toJson(original);
        JsonObject states =
                json.getAsJsonObject("materialStates");

        states.getAsJsonObject("GOLD")
                .remove("totalBaseDrops");
        states.getAsJsonObject("GOLD")
                .remove("sessionBaseDrops");

        states.getAsJsonObject("DIAMOND")
                .remove("totalBaseDrops");
        states.getAsJsonObject("DIAMOND")
                .remove("sessionBaseDrops");

        TrackerConfig restored =
                TrackerStore.fromJson(json);

        /*
         * Gold ja Diamond antavat tässä trackerissa
         * viisi base dropia per lohko.
         */
        assertEquals(
                45,
                restored.state(TrackedMaterial.GOLD)
                        .totalBaseDrops);

        assertEquals(
                20,
                restored.state(TrackedMaterial.GOLD)
                        .sessionBaseDrops);

        assertEquals(
                35,
                restored.state(TrackedMaterial.DIAMOND)
                        .totalBaseDrops);

        assertEquals(
                15,
                restored.state(TrackedMaterial.DIAMOND)
                        .sessionBaseDrops);

        assertEquals(
                0,
                restored.state(TrackedMaterial.MITHRIL)
                        .sessionBaseDrops);

        assertEquals(
                0,
                restored.state(TrackedMaterial.TITANIUM)
                        .sessionBaseDrops);

        assertEquals(
                0,
                restored.state(TrackedMaterial.TUNGSTEN)
                        .sessionBaseDrops);
    }

    @Test
    void v8DefaultUiAccentMigratesWithoutOverwritingCustomColor() {
        TrackerConfig original = new TrackerConfig();
        JsonObject legacyDefault = TrackerStore.toJson(original);
        legacyDefault.addProperty("dataVersion", 8);
        JsonObject defaultQol = legacyDefault.getAsJsonObject("qolUtilities");
        defaultQol.addProperty("clickGuiColor", 0xFFE33B3B);
        defaultQol.addProperty("slotBindColor", 0xFFE33B3B);

        TrackerConfig migrated = TrackerStore.fromJson(legacyDefault);
        assertEquals(0xFFE11D48, migrated.qolUtilities.clickGuiColor);
        assertEquals(0xFFE11D48, migrated.qolUtilities.slotBindColor);

        JsonObject custom = TrackerStore.toJson(original);
        custom.addProperty("dataVersion", 8);
        custom.getAsJsonObject("qolUtilities")
                .addProperty("clickGuiColor", 0xFF123456);
        assertEquals(
                0xFF123456,
                TrackerStore.fromJson(custom).qolUtilities.clickGuiColor);
    }

    @Test
    void powderChestTrackerDefaultsOnAndPersistsAnExplicitOffChoice() {
        JsonObject existingConfig = TrackerStore.toJson(new TrackerConfig());
        existingConfig.remove("powderChestTrackerEnabled");
        assertTrue(TrackerStore.fromJson(existingConfig)
                .powderChestTrackerEnabled);

        TrackerConfig disabled = new TrackerConfig();
        disabled.powderChestTrackerEnabled = false;
        assertFalse(TrackerStore.fromJson(TrackerStore.toJson(disabled))
                .powderChestTrackerEnabled);
    }

    @Test
    void missingSecretHitboxKeysRestorePlayableDefaults() {
        JsonObject json = TrackerStore.toJson(new TrackerConfig());
        JsonObject qol = json.getAsJsonObject("qolUtilities");
        qol.remove("secretHitboxesLever");
        qol.remove("secretHitboxesButton");
        qol.remove("secretHitboxesSkull");
        qol.addProperty("secretHitboxesChests", false);

        TrackerConfig restored = TrackerStore.fromJson(json);
        assertTrue(restored.qolUtilities.secretHitboxesLever);
        assertTrue(restored.qolUtilities.secretHitboxesButton);
        assertTrue(restored.qolUtilities.secretHitboxesSkull);
        assertFalse(restored.qolUtilities.secretHitboxesChests);

        TrackerConfig explicitOff = new TrackerConfig();
        explicitOff.qolUtilities.secretHitboxesLever = false;
        explicitOff.qolUtilities.secretHitboxesButton = false;
        explicitOff.qolUtilities.secretHitboxesSkull = false;
        TrackerConfig kept = TrackerStore.fromJson(TrackerStore.toJson(explicitOff));
        assertFalse(kept.qolUtilities.secretHitboxesLever);
        assertFalse(kept.qolUtilities.secretHitboxesButton);
        assertFalse(kept.qolUtilities.secretHitboxesSkull);
    }

    @Test
    void missingInventoryOverlayKeysRestorePlayableDefaults() {
        JsonObject json = TrackerStore.toJson(new TrackerConfig());
        JsonObject qol = json.getAsJsonObject("qolUtilities");
        qol.remove("inventoryOverlayEnabled");
        qol.remove("inventoryOverlayEquipment");
        qol.remove("inventoryOverlayHideRecipeBook");
        qol.remove("inventoryOverlayHideStatusEffects");
        qol.remove("inventoryOverlayPetSlot");
        qol.remove("skillLevelsEnabled");
        qol.remove("petHudEnabled");

        TrackerConfig restored = TrackerStore.fromJson(json);
        assertTrue(restored.qolUtilities.inventoryOverlayEnabled);
        assertTrue(restored.qolUtilities.inventoryOverlayEquipment);
        assertTrue(restored.qolUtilities.inventoryOverlayHideRecipeBook);
        assertTrue(restored.qolUtilities.inventoryOverlayHideStatusEffects);
        assertTrue(restored.qolUtilities.inventoryOverlayPetSlot);
        assertTrue(restored.qolUtilities.skillLevelsEnabled);
        assertTrue(restored.qolUtilities.petHudEnabled);

        TrackerConfig explicitOff = new TrackerConfig();
        explicitOff.qolUtilities.inventoryOverlayEnabled = false;
        explicitOff.qolUtilities.petHudEnabled = false;
        TrackerConfig kept = TrackerStore.fromJson(TrackerStore.toJson(explicitOff));
        assertFalse(kept.qolUtilities.inventoryOverlayEnabled);
        assertFalse(kept.qolUtilities.petHudEnabled);
    }
}
