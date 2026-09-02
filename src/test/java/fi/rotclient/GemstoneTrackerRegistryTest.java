package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GemstoneTrackerRegistryTest {
    @Test
    void createsSeparateStateForEachGemstoneType() {
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry();

        Set<GemstoneTrackerState> states =
                new LinkedHashSet<>();

        for (GemstoneType gemstone : GemstoneType.values()) {
            GemstoneTrackerState state =
                    registry.state(gemstone);
            assertNotNull(state);
            states.add(state);
        }

        assertEquals(
                GemstoneType.values().length,
                states.size());
        assertNotSame(
                registry.state(GemstoneType.RUBY),
                registry.state(GemstoneType.JADE));
    }

    @Test
    void keepsRubyAndJadeDataIsolated() {
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry();

        GemstoneTrackerState rubyState =
                registry.state(GemstoneType.RUBY);
        GemstoneTrackerState jadeState =
                registry.state(GemstoneType.JADE);

        rubyState.recordGain(GemstoneTier.ROUGH, 12L);
        rubyState.recordBlock(1_000L);
        rubyState.addActiveMillis(400L);

        jadeState.recordGain(GemstoneTier.FLAWED, 3L);
        jadeState.recordBlock(2_000L);
        jadeState.addActiveMillis(800L);

        assertEquals(12L, rubyState.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(0L, jadeState.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(3L, jadeState.sessionLedger().quantity(GemstoneTier.FLAWED));
        assertEquals(0L, rubyState.sessionLedger().quantity(GemstoneTier.FLAWED));

        assertEquals(1L, rubyState.sessionBlocks);
        assertEquals(1L, jadeState.sessionBlocks);
        assertEquals(400L, rubyState.sessionActiveMillis);
        assertEquals(800L, jadeState.sessionActiveMillis);

        assertEquals(1L, rubyState.totalBlocks);
        assertEquals(1L, jadeState.totalBlocks);
        assertEquals(400L, rubyState.totalActiveMillis);
        assertEquals(800L, jadeState.totalActiveMillis);
    }

    @Test
    void usesProvidedBackingMapWithoutCopying() {
        Map<String, GemstoneTrackerState> backing =
                new LinkedHashMap<>();
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry(backing);

        GemstoneTrackerState rubyState =
                registry.state(GemstoneType.RUBY);

        assertSame(rubyState, backing.get(GemstoneType.RUBY.id()));

        rubyState.recordGain(GemstoneTier.ROUGH, 2L);

        assertEquals(
                2L,
                backing.get(GemstoneType.RUBY.id())
                        .sessionLedger()
                        .quantity(GemstoneTier.ROUGH));
    }

    @Test
    void normalizesAliasesToCanonicalKeysAndRepairsNullStates() {
        Map<String, GemstoneTrackerState> backing =
                new LinkedHashMap<>();
        GemstoneTrackerState aliasState =
                new GemstoneTrackerState();
        GemstoneTrackerState canonicalState =
                new GemstoneTrackerState();

        backing.put("ruby", aliasState);
        backing.put(GemstoneType.RUBY.id(), canonicalState);
        backing.put("jade", null);

        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry(backing);

        registry.normalize();

        assertFalse(backing.containsKey("ruby"));
        assertTrue(backing.containsKey(GemstoneType.RUBY.id()));
        assertSame(canonicalState, backing.get(GemstoneType.RUBY.id()));
        assertNotNull(backing.get(GemstoneType.JADE.id()));
        assertSame(
                backing.get(GemstoneType.JADE.id()),
                registry.state(GemstoneType.JADE));
    }

    @Test
    void rejectsNullGemstoneType() {
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry();

        assertThrows(
                IllegalArgumentException.class,
                () -> registry.state(null));
    }

    @Test
    void resetsOnlyTheRequestedGemstoneSession() {
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry();

        GemstoneTrackerState rubyState =
                registry.state(GemstoneType.RUBY);
        GemstoneTrackerState jadeState =
                registry.state(GemstoneType.JADE);

        rubyState.recordGain(GemstoneTier.FINE, 2L);
        rubyState.recordBlock(3_000L);
        rubyState.addActiveMillis(900L);

        jadeState.recordGain(GemstoneTier.ROUGH, 5L);
        jadeState.recordBlock(4_000L);
        jadeState.addActiveMillis(1_200L);

        registry.resetSession(GemstoneType.RUBY);

        assertEquals(0L, rubyState.sessionLedger().quantity(GemstoneTier.FINE));
        assertEquals(2L, rubyState.totalLedger().quantity(GemstoneTier.FINE));
        assertEquals(0L, rubyState.sessionBlocks);
        assertEquals(1L, rubyState.totalBlocks);
        assertEquals(0L, rubyState.sessionActiveMillis);
        assertEquals(900L, rubyState.totalActiveMillis);

        assertEquals(5L, jadeState.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(5L, jadeState.totalLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(1L, jadeState.sessionBlocks);
        assertEquals(1L, jadeState.totalBlocks);
        assertEquals(1_200L, jadeState.sessionActiveMillis);
        assertEquals(1_200L, jadeState.totalActiveMillis);
    }

    @Test
    void resetsAllGemstoneSessionsWithoutLosingLifetimeData() {
        GemstoneTrackerRegistry registry =
                new GemstoneTrackerRegistry();

        GemstoneTrackerState rubyState =
                registry.state(GemstoneType.RUBY);
        GemstoneTrackerState jadeState =
                registry.state(GemstoneType.JADE);
        GemstoneTrackerState sapphireState =
                registry.state(GemstoneType.SAPPHIRE);

        rubyState.recordGain(GemstoneTier.ROUGH, 4L);
        rubyState.recordBlock(5_000L);
        rubyState.addActiveMillis(300L);

        jadeState.recordGain(GemstoneTier.FLAWED, 2L);
        jadeState.recordBlock(6_000L);
        jadeState.addActiveMillis(500L);

        sapphireState.recordGain(GemstoneTier.FINE, 1L);
        sapphireState.recordBlock(7_000L);
        sapphireState.addActiveMillis(600L);

        registry.resetAllSessions();

        assertEquals(0L, rubyState.sessionLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(4L, rubyState.totalLedger().quantity(GemstoneTier.ROUGH));
        assertEquals(0L, rubyState.sessionBlocks);
        assertEquals(1L, rubyState.totalBlocks);
        assertEquals(0L, rubyState.sessionActiveMillis);
        assertEquals(300L, rubyState.totalActiveMillis);

        assertEquals(0L, jadeState.sessionLedger().quantity(GemstoneTier.FLAWED));
        assertEquals(2L, jadeState.totalLedger().quantity(GemstoneTier.FLAWED));
        assertEquals(0L, jadeState.sessionBlocks);
        assertEquals(1L, jadeState.totalBlocks);
        assertEquals(0L, jadeState.sessionActiveMillis);
        assertEquals(500L, jadeState.totalActiveMillis);

        assertEquals(0L, sapphireState.sessionLedger().quantity(GemstoneTier.FINE));
        assertEquals(1L, sapphireState.totalLedger().quantity(GemstoneTier.FINE));
        assertEquals(0L, sapphireState.sessionBlocks);
        assertEquals(1L, sapphireState.totalBlocks);
        assertEquals(0L, sapphireState.sessionActiveMillis);
        assertEquals(600L, sapphireState.totalActiveMillis);
    }
}
