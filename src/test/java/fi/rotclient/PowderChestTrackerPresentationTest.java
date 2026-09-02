package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PowderChestTrackerPresentationTest {
    @Test
    void projectsOnlyCanonicalChestAndCurrencyRows() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.powderChestsOpened = 3L;
        config.items.add(row(
                "ROUGH_RUBY_GEM", "Rough Ruby Gemstone", 4L,
                SessionSourceType.CHEST));
        config.items.add(row(
                "GEMSTONE_POWDER", "Gemstone Powder", 900L,
                SessionSourceType.CURRENCY));
        config.items.add(row(
                "HARD_STONE", "Hard Stone", 64L,
                SessionSourceType.MINING));

        PowderChestTrackerPresentation presentation =
                PowderChestTrackerPresentation.from(config, true);

        assertTrue(presentation.enabled());
        assertEquals(3L, presentation.chestsOpened());
        assertEquals(1, presentation.lootRows().size());
        assertEquals(4L, presentation.lootRows().getFirst().quantity());
        assertEquals(1, presentation.currencyRows().size());
        assertEquals(900L,
                presentation.currencyRows().getFirst().quantity());
    }

    @Test
    void sameRewardAcrossAreasIsAggregatedForTheStandaloneView() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.items.add(row(
                "GEMSTONE_POWDER", "Gemstone Powder", 80L,
                SessionSourceType.CURRENCY));
        config.items.add(new RotClientCurrentSessionConfig.SessionItemRecord(
                "GEMSTONE_POWDER", "Gemstone Powder", 120L,
                SessionSourceType.CURRENCY.name(), null,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA.id(), true,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE.name(),
                0.0));

        PowderChestTrackerPresentation presentation =
                PowderChestTrackerPresentation.from(config, true);

        assertEquals(1, presentation.currencyRows().size());
        assertEquals(200L,
                presentation.currencyRows().getFirst().quantity());
    }

    @Test
    void mithrilPowderIsIndependentOfGemstonePowder() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.items.add(row(
                "GEMSTONE_POWDER", "Gemstone Powder", 400L,
                SessionSourceType.CURRENCY));
        config.items.add(row(
                "MITHRIL_POWDER", "Mithril Powder", 125L,
                SessionSourceType.CURRENCY));
        config.items.add(row(
                "MITHRIL_POWDER", "Mithril Powder", 25L,
                SessionSourceType.CURRENCY));

        PowderChestTrackerPresentation presentation =
                PowderChestTrackerPresentation.from(config, true);

        assertEquals(400L, presentation.gemstonePowder());
        assertEquals(150L, presentation.mithrilPowder());
    }

    @Test
    void enchantedHardStoneUsesOnlyChestLootRows() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.items.add(row(
                "ENCHANTED_HARD_STONE", "Enchanted Hard Stone", 8L,
                SessionSourceType.CHEST));
        config.items.add(row(
                "COMPACTED_HARD_STONE", "Compacted Hard Stone", 2L,
                SessionSourceType.CHEST));
        config.items.add(row(
                "ENCHANTED_HARD_STONE", "Enchanted Hard Stone", 5L,
                SessionSourceType.MINING));

        PowderChestTrackerPresentation presentation =
                PowderChestTrackerPresentation.from(config, true);

        assertEquals(10L, presentation.enchantedHardStone());
    }

    @Test
    void hourlyRatesUseActiveDurationAndStayZeroWithoutTime() {
        RotClientCurrentSessionConfig config =
                RotClientCurrentSessionConfig.defaults();
        config.powderChestsOpened = 6L;
        config.items.add(row(
                "GEMSTONE_POWDER", "Gemstone Powder", 1_200L,
                SessionSourceType.CURRENCY));
        config.items.add(row(
                "MITHRIL_POWDER", "Mithril Powder", 300L,
                SessionSourceType.CURRENCY));

        PowderChestTrackerPresentation withoutTime =
                PowderChestTrackerPresentation.from(config, true, 0L);
        assertEquals(0.0, withoutTime.chestsPerHour(), 0.0001);
        assertEquals(0.0, withoutTime.gemstonePowderPerHour(), 0.0001);
        assertEquals(0.0, withoutTime.mithrilPowderPerHour(), 0.0001);

        PowderChestTrackerPresentation oneHour =
                PowderChestTrackerPresentation.from(config, true, 3_600_000L);
        assertEquals(6.0, oneHour.chestsPerHour(), 0.0001);
        assertEquals(1_200.0, oneHour.gemstonePowderPerHour(), 0.0001);
        assertEquals(300.0, oneHour.mithrilPowderPerHour(), 0.0001);

        PowderChestTrackerPresentation thirtyMinutes =
                PowderChestTrackerPresentation.from(config, true, 1_800_000L);
        assertEquals(12.0, thirtyMinutes.chestsPerHour(), 0.0001);
        assertEquals(2_400.0, thirtyMinutes.gemstonePowderPerHour(), 0.0001);
        assertEquals(600.0, thirtyMinutes.mithrilPowderPerHour(), 0.0001);
    }

    private static RotClientCurrentSessionConfig.SessionItemRecord row(
            String id,
            String name,
            long quantity,
            SessionSourceType source) {
        return new RotClientCurrentSessionConfig.SessionItemRecord(
                id, name, quantity, source.name(), null,
                SkyBlockArea.CRYSTAL_HOLLOWS.id(), true,
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE.name(),
                0.0);
    }
}
