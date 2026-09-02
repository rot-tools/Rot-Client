package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GemstoneDiagnosticObserverTest {
    @Test
    void matchesExactGemstoneItemNames() {
        assertEquals(
                new GemstoneDiagnosticObserver.GemstoneItem(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH),
                GemstoneDiagnosticObserver.matchItemName(
                        "Rough Ruby Gemstone"));

        assertEquals(
                new GemstoneDiagnosticObserver.GemstoneItem(
                        GemstoneType.AQUAMARINE,
                        GemstoneTier.FLAWLESS),
                GemstoneDiagnosticObserver.matchItemName(
                        "Flawless Aquamarine Gemstone"));
    }

    @Test
    void rejectsUnrelatedOrIncompleteNames() {
        assertNull(
                GemstoneDiagnosticObserver.matchItemName(
                        "Enchanted Diamond"));

        assertNull(
                GemstoneDiagnosticObserver.matchItemName(
                        "Ruby Crystal"));

        assertNull(
                GemstoneDiagnosticObserver.matchItemName(
                        "Rough Ruby"));
    }

    @Test
    void parsesOnlyGemstoneSackEntries() {
        List<GemstoneDiagnosticObserver.GemstoneSackEntry>
                entries =
                GemstoneDiagnosticObserver.parseAddedItems(
                        "Added items:\n"
                                + "+1,234 Rough Ruby Gemstone "
                                + "(Gemstone Sack)\n"
                                + "+2 Fine Jade Gemstone "
                                + "(Gemstone Sack)\n"
                                + "+5 Gold Ingot "
                                + "(Mining Sack)");

        assertEquals(
                List.of(
                        new GemstoneDiagnosticObserver
                                .GemstoneSackEntry(
                                GemstoneType.RUBY,
                                GemstoneTier.ROUGH,
                                1_234L),
                        new GemstoneDiagnosticObserver
                                .GemstoneSackEntry(
                                GemstoneType.JADE,
                                GemstoneTier.FINE,
                                2L)),
                entries);
    }

    @Test
    void reportsPositiveNegativeAndTierIsolatedDeltas() {
        Map<GemstoneType, EnumMap<GemstoneTier, Long>>
                before =
                GemstoneDiagnosticObserver.emptySnapshot();

        Map<GemstoneType, EnumMap<GemstoneTier, Long>>
                after =
                GemstoneDiagnosticObserver.emptySnapshot();

        before.get(GemstoneType.RUBY)
                .put(GemstoneTier.ROUGH, 100L);

        before.get(GemstoneType.JADE)
                .put(GemstoneTier.FLAWED, 8L);

        after.get(GemstoneType.RUBY)
                .put(GemstoneTier.ROUGH, 140L);

        after.get(GemstoneType.JADE)
                .put(GemstoneTier.FLAWED, 3L);

        after.get(GemstoneType.RUBY)
                .put(GemstoneTier.FINE, 2L);

        assertEquals(
                List.of(
                        new GemstoneDiagnosticObserver
                                .GemstoneInventoryDelta(
                                GemstoneType.RUBY,
                                GemstoneTier.ROUGH,
                                100L,
                                140L,
                                40L),
                        new GemstoneDiagnosticObserver
                                .GemstoneInventoryDelta(
                                GemstoneType.RUBY,
                                GemstoneTier.FINE,
                                0L,
                                2L,
                                2L),
                        new GemstoneDiagnosticObserver
                                .GemstoneInventoryDelta(
                                GemstoneType.JADE,
                                GemstoneTier.FLAWED,
                                8L,
                                3L,
                                -5L)),
                GemstoneDiagnosticObserver.diffSnapshots(
                        before,
                        after));
    }

    @Test
    void diagnosticHoverLinesPreserveSpacingAndUnicode() {
        String nonBreakingSpace = "\u00A0";

        List<String> lines =
                GemstoneDiagnosticObserver
                        .diagnosticHoverLines(
                                "Added items:\n"
                                        + "  +123"
                                        + nonBreakingSpace
                                        + "Rough Ruby Gemstone\n"
                                        + "\n"
                                        + " +5 Gold Ingot");

        assertEquals(
                List.of(
                        "Added items:",
                        "  +123"
                                + nonBreakingSpace
                                + "Rough Ruby Gemstone",
                        " +5 Gold Ingot"),
                lines);
    }

    @Test
    void diagnosticCodePointsExposeInvisibleSeparators() {
        String value =
                "+123\u00A0Rough Ruby";

        String codePoints =
                GemstoneDiagnosticObserver
                        .diagnosticCodePoints(value);

        org.junit.jupiter.api.Assertions.assertTrue(
                codePoints.contains("U+002B"));

        org.junit.jupiter.api.Assertions.assertTrue(
                codePoints.contains("U+00A0"));
    }
    @Test
    void stripsHypixelPrivateUseGemstoneIcons() {
        assertEquals(
                "  +140  Flawed Ruby Gemstone "
                        + "(Gemstones Sack)",
                GemstoneDiagnosticObserver.stripSackIcons(
                        "  +140 \uE010 Flawed Ruby Gemstone "
                                + "(Gemstones Sack)"));

        assertEquals(
                "  +56  Rough Topaz Gemstone "
                        + "(Gemstones Sack)",
                GemstoneDiagnosticObserver.stripSackIcons(
                        "  +56 \uE01C Rough Topaz Gemstone "
                                + "(Gemstones Sack)"));
    }

    @Test
    void parsesActualHypixelGemstoneSackFormat() {
        List<GemstoneDiagnosticObserver.GemstoneSackEntry>
                entries =
                GemstoneDiagnosticObserver.parseAddedItems(
                        "Added items:\n"
                                + "  +140 \uE010 "
                                + "Flawed Ruby Gemstone "
                                + "(Gemstones Sack)\n"
                                + "  +1,464 \uE010 "
                                + "Rough Ruby Gemstone "
                                + "(Gemstones Sack)\n"
                                + "  +56 \uE01C "
                                + "Rough Topaz Gemstone "
                                + "(Gemstones Sack)");

        assertEquals(
                List.of(
                        new GemstoneDiagnosticObserver
                                .GemstoneSackEntry(
                                GemstoneType.RUBY,
                                GemstoneTier.FLAWED,
                                140L),
                        new GemstoneDiagnosticObserver
                                .GemstoneSackEntry(
                                GemstoneType.RUBY,
                                GemstoneTier.ROUGH,
                                1_464L),
                        new GemstoneDiagnosticObserver
                                .GemstoneSackEntry(
                                GemstoneType.TOPAZ,
                                GemstoneTier.ROUGH,
                                56L)),
                entries);
    }}
