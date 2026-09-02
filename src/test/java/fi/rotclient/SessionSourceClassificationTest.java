package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class SessionSourceClassificationTest {
    @Test
    void sourceTypesParseWithFallback() {
        assertEquals(SessionSourceType.MINING, SessionSourceType.fromName("mining"));
        assertEquals(SessionSourceType.CHEST, SessionSourceType.fromName("CHEST"));
        assertEquals(SessionSourceType.MOB, SessionSourceType.fromName("MOB"));
        assertEquals(SessionSourceType.CURRENCY, SessionSourceType.fromName("CURRENCY"));
        assertEquals(
                SessionSourceType.UNATTRIBUTED,
                SessionSourceType.fromName("nope"));
    }

    @Test
    void miningClassificationNullable() {
        assertEquals(MiningClassification.TARGET, MiningClassification.fromName("TARGET"));
        assertEquals(MiningClassification.OTHER, MiningClassification.fromName("other"));
        assertNull(MiningClassification.fromName(null));
        assertNull(MiningClassification.fromName("CHEST"));
    }

    @Test
    void itemIdEqualityIgnoresDisplayName() {
        SkyBlockItemId a = SkyBlockItemId.of("gold_ingot", "Gold");
        SkyBlockItemId b = SkyBlockItemId.of("GOLD_INGOT", "Different");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals("GOLD_INGOT", a.id());
        assertNotEquals(
                SkyBlockItemId.of("DIAMOND"),
                SkyBlockItemId.of("GOLD_INGOT"));
    }

    @Test
    void engineCategoryMapsToSessionSource() {
        assertEquals(
                SessionSourceType.CHEST,
                mapCategory(MiningSessionCategory.CHEST_LOOT));
        assertEquals(
                SessionSourceType.CURRENCY,
                mapCategory(MiningSessionCategory.CURRENCY));
        assertEquals(
                SessionSourceType.MINING,
                mapCategory(MiningSessionCategory.OTHER_MINED));
        assertEquals(
                SessionSourceType.MINING,
                mapCategory(MiningSessionCategory.TARGET_MINED));
    }

    private static SessionSourceType mapCategory(MiningSessionCategory category) {
        return switch (category) {
            case TARGET_MINED, OTHER_MINED -> SessionSourceType.MINING;
            case CHEST_LOOT -> SessionSourceType.CHEST;
            case CURRENCY -> SessionSourceType.CURRENCY;
        };
    }
}
