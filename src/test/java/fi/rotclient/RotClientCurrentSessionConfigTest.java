package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RotClientCurrentSessionConfigTest {
    @Test
    void defaultsCreateActiveSession1() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.normalize();
        assertEquals(1, config.displayNumber);
        assertEquals(RotClientCurrentSessionConfig.STATE_ACTIVE, config.state);
        assertFalse(config.sessionId.isBlank());
        assertTrue(config.items.isEmpty());
    }

    @Test
    void normalizePreservesUnknownItemIds() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.items.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                "custom_mystery_drop",
                "Mystery Drop",
                3L,
                SessionSourceType.UNATTRIBUTED.name(),
                null,
                "not_a_real_area",
                false,
                "UNSUPPORTED",
                0.0));
        config.normalize();
        assertEquals(1, config.items.size());
        assertEquals("CUSTOM_MYSTERY_DROP", config.items.get(0).itemId());
        assertEquals("Mystery Drop", config.items.get(0).displayName());
        assertEquals(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                config.items.get(0).areaId());
        assertEquals(
                SessionSourceType.UNATTRIBUTED.name(),
                config.items.get(0).sourceType());
    }

    @Test
    void copyIsIndependent() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        String id = config.sessionId;
        RotClientCurrentSessionConfig copy = config.copy();
        copy.sessionId = UUID.randomUUID().toString();
        copy.displayNumber = 9;
        assertEquals(id, config.sessionId);
        assertEquals(1, config.displayNumber);
        assertNotEquals(config.sessionId, copy.sessionId);
    }

    @Test
    void nextDisplayNumberHelperIncrements() {
        assertEquals(2, RotClientCurrentSessionConfig.nextDisplayNumberAfter(1));
        assertEquals(21, RotClientCurrentSessionConfig.nextDisplayNumberAfter(20));
    }

    @Test
    void serializeRoundTripPreservesDisplayNumber() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.displayNumber = 7;
        config.nextDisplayNumber = 8;
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.items.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                "GOLD_INGOT",
                "Gold Ingot",
                12L,
                SessionSourceType.MINING.name(),
                MiningClassification.OTHER.name(),
                SkyBlockArea.DWARVEN_MINES.id(),
                true,
                "RESOLVED_BAZAAR",
                120.5));
        String json = RotClientCurrentSessionStore.toJson(config);
        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson(json);
        assertEquals(7, loaded.displayNumber);
        assertEquals(8, loaded.nextDisplayNumber);
        assertEquals(RotClientCurrentSessionConfig.STATE_PAUSED, loaded.state);
        assertEquals(1, loaded.items.size());
        assertEquals(12L, loaded.items.get(0).quantity());
        assertEquals(120.5, loaded.items.get(0).resolvedGrossValue(), 0.0001);
        assertEquals(-1, loaded.lastMagicFind);
        assertEquals(0L, loaded.lastMagicFindAtMillis);
    }

    @Test
    void serializeRoundTripPreservesObservedMagicFind() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.lastMagicFind = 42;
        config.lastMagicFindAtMillis = 9_000L;
        String json = RotClientCurrentSessionStore.toJson(config);
        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson(json);
        assertEquals(42, loaded.lastMagicFind);
        assertEquals(9_000L, loaded.lastMagicFindAtMillis);
    }

    @Test
    void normalizeTreatsLegacyZeroMagicFindWithoutTimestampAsUnset() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.lastMagicFind = 0;
        config.lastMagicFindAtMillis = 0L;
        config.normalize();
        assertEquals(-1, config.lastMagicFind);
        assertEquals(0L, config.lastMagicFindAtMillis);
    }

    @Test
    void serializeRoundTripPreservesPauseAccounting() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.state = RotClientCurrentSessionConfig.STATE_PAUSED;
        config.pausedAtMillis = 5_000L;
        config.totalPausedMillis = 12_000L;

        String json = RotClientCurrentSessionStore.toJson(config);
        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson(json);

        assertEquals(RotClientCurrentSessionConfig.STATE_PAUSED, loaded.state);
        assertEquals(5_000L, loaded.pausedAtMillis);
        assertEquals(12_000L, loaded.totalPausedMillis);
    }

    @Test
    void unresolvedLegacyRowsCannotRetainResolvedGrossValue() {
        RotClientCurrentSessionConfig.SessionItemRecord unavailable =
                new RotClientCurrentSessionConfig.SessionItemRecord(
                        "OLD_ITEM", "Old Item", 2L,
                        SessionSourceType.MINING.name(),
                        MiningClassification.OTHER.name(),
                        SkyBlockArea.DWARVEN_MINES.id(),
                        true,
                        RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE
                                .name(),
                        99.0);
        RotClientCurrentSessionConfig.SessionItemRecord stale =
                new RotClientCurrentSessionConfig.SessionItemRecord(
                        "STALE_ITEM", "Stale Item", 2L,
                        SessionSourceType.MINING.name(),
                        MiningClassification.OTHER.name(),
                        SkyBlockArea.DWARVEN_MINES.id(),
                        true,
                        RotClientCurrentSessionConfig.PriceStatus.STALE.name(),
                        12.5);

        assertEquals(0.0, unavailable.resolvedGrossValue(), 0.0);
        assertEquals(12.5, stale.resolvedGrossValue(), 0.0);
    }

    @Test
    void schema2RoundTripPreservesAreaSegmentsAndPriceBasisTimestamp() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.currentAreaId = SkyBlockArea.CRYSTAL_HOLLOWS.id();
        config.areaSegments = List.of(
                new RotClientCurrentSessionConfig.AreaSegment(
                        SkyBlockArea.DWARVEN_MINES.id(), 1_000L, 2_000L),
                new RotClientCurrentSessionConfig.AreaSegment(
                        SkyBlockArea.CRYSTAL_HOLLOWS.id(), 2_000L, 0L));
        config.lastPriceBookObservedAtMillis = 2_500L;

        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson(
                        RotClientCurrentSessionStore.toJson(config));

        assertEquals(RotClientCurrentSessionConfig.SCHEMA_VERSION,
                loaded.schemaVersion);
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS.id(), loaded.currentAreaId);
        assertEquals(2, loaded.areaSegments.size());
        assertEquals(SkyBlockArea.CRYSTAL_HOLLOWS.id(),
                loaded.areaSegments.get(1).areaId());
        assertTrue(loaded.areaSegments.get(1).isOpen());
        assertEquals(2_500L, loaded.lastPriceBookObservedAtMillis);
    }

    @Test
    void legacyPausedSessionWithoutPausedAtMillisNeverFabricatesDuration() {
        // Pre-pause-accounting persisted payload: no pausedAtMillis field.
        String json = """
                {
                  "schemaVersion": 1,
                  "sessionId": "legacy-paused",
                  "displayNumber": 1,
                  "nextDisplayNumber": 2,
                  "startedAtMillis": 1000,
                  "state": "PAUSED",
                  "currentTargetId": "",
                  "items": [],
                  "areasVisited": [],
                  "targetSegments": []
                }
                """;
        long before = System.currentTimeMillis();
        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson(json);
        long after = System.currentTimeMillis();

        assertEquals(0L, loaded.totalPausedMillis);
        assertTrue(loaded.pausedAtMillis >= before
                && loaded.pausedAtMillis <= after);
    }

    @Test
    void malformedJsonRecoversToSession1() {
        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson("{not-json");
        assertEquals(1, loaded.displayNumber);
        assertEquals(RotClientCurrentSessionConfig.STATE_ACTIVE, loaded.state);
        assertTrue(loaded.items.isEmpty());
    }

    @Test
    void schemaMigrationPreservesValues() {
        String json = """
                {
                  "schemaVersion": 0,
                  "sessionId": "keep-me",
                  "displayNumber": 4,
                  "nextDisplayNumber": 5,
                  "startedAtMillis": 1000,
                  "state": "ACTIVE",
                  "currentTargetId": "GOLD",
                  "items": [],
                  "areasVisited": ["DWARVEN_MINES"],
                  "targetSegments": []
                }
                """;
        RotClientCurrentSessionConfig loaded =
                RotClientCurrentSessionStore.parseJson(json);
        assertEquals(RotClientCurrentSessionConfig.SCHEMA_VERSION,
                loaded.schemaVersion);
        assertEquals("keep-me", loaded.sessionId);
        assertEquals(4, loaded.displayNumber);
        assertEquals("GOLD", loaded.currentTargetId);
        assertEquals(SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(),
                loaded.currentAreaId);
        assertTrue(loaded.areaSegments.isEmpty());
        assertEquals(0L, loaded.lastPriceBookObservedAtMillis);
        assertEquals(
                List.of(SkyBlockArea.DWARVEN_MINES.id()),
                loaded.areasVisited);
    }
}
