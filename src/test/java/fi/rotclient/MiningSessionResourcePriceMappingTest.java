package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class MiningSessionResourcePriceMappingTest {
    @Test
    void rawGoldMapsToRawBazaarProduct() {
        MiningSessionResource resource = MiningSessionResource.material(
                TrackedMaterial.GOLD.rawBazaarId(),
                TrackedMaterial.GOLD.rawItemName(),
                TrackedMaterial.GOLD);

        assertEquals(
                TrackedMaterial.GOLD.rawBazaarId(),
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .orElseThrow());
    }

    @Test
    void enchantedGoldMapsOnlyToEnchantedProduct() {
        MiningSessionResource resource = MiningSessionResource.material(
                TrackedMaterial.GOLD.enchantedBazaarId(),
                TrackedMaterial.GOLD.enchantedItemName(),
                TrackedMaterial.GOLD);

        assertEquals(
                TrackedMaterial.GOLD.enchantedBazaarId(),
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .orElseThrow());
    }

    @Test
    void rawAndEnchantedAreSeparateMappings() {
        MiningSessionResource raw = MiningSessionResource.material(
                TrackedMaterial.GOLD.rawBazaarId(),
                TrackedMaterial.GOLD.rawItemName(),
                TrackedMaterial.GOLD);
        MiningSessionResource enchanted = MiningSessionResource.material(
                TrackedMaterial.GOLD.enchantedBazaarId(),
                TrackedMaterial.GOLD.enchantedItemName(),
                TrackedMaterial.GOLD);

        assertFalse(
                MiningSessionResourcePriceMapping.bazaarProductId(raw)
                        .equals(MiningSessionResourcePriceMapping
                                .bazaarProductId(enchanted)));
    }

    @Test
    void allGemstoneTypesMapForRoughTier() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            MiningSessionResource resource = MiningSessionResource.gemstone(
                    gemstone.bazaarId(GemstoneTier.ROUGH),
                    gemstone.itemName(GemstoneTier.ROUGH),
                    gemstone,
                    GemstoneTier.ROUGH);

            assertEquals(
                    gemstone.bazaarId(GemstoneTier.ROUGH),
                    MiningSessionResourcePriceMapping.bazaarProductId(resource)
                            .orElseThrow(),
                    gemstone.name());
        }
    }

    @Test
    void allGemstoneTypesMapForFlawedTier() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            MiningSessionResource resource = MiningSessionResource.gemstone(
                    gemstone.bazaarId(GemstoneTier.FLAWED),
                    gemstone.itemName(GemstoneTier.FLAWED),
                    gemstone,
                    GemstoneTier.FLAWED);

            assertEquals(
                    gemstone.bazaarId(GemstoneTier.FLAWED),
                    MiningSessionResourcePriceMapping.bazaarProductId(resource)
                            .orElseThrow(),
                    gemstone.name());
        }
    }

    @Test
    void totalSupportedGemstoneMappingsIsTwentyFour() {
        assertEquals(
                24,
                MiningSessionResourcePriceMapping.pricedGemstoneProductIds()
                        .size());
    }

    @Test
    void fineGemstoneRemainsUnsupported() {
        assertUnsupportedGemstone(GemstoneTier.FINE);
    }

    @Test
    void flawlessGemstoneRemainsUnsupported() {
        assertUnsupportedGemstone(GemstoneTier.FLAWLESS);
    }

    @Test
    void perfectGemstoneRemainsUnsupported() {
        assertUnsupportedGemstone(GemstoneTier.PERFECT);
    }

    @Test
    void inconsistentGemstoneMetadataIsUnsupported() {
        MiningSessionResource resource = MiningSessionResource.gemstone(
                GemstoneType.RUBY.bazaarId(GemstoneTier.ROUGH),
                GemstoneType.TOPAZ.itemName(GemstoneTier.ROUGH),
                GemstoneType.TOPAZ,
                GemstoneTier.ROUGH);

        assertTrue(
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .isEmpty());
    }

    @Test
    void essenceRemainsUnsupported() {
        MiningSessionResource resource = MiningSessionResource.genericItem(
                "GOLD_ESSENCE",
                "Gold Essence");

        assertTrue(
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .isEmpty());
    }

    @Test
    void enchantedBlockRemainsUnsupported() {
        MiningSessionResource resource = MiningSessionResource.material(
                "ENCHANTED_GOLD_BLOCK",
                "Enchanted Gold Block",
                TrackedMaterial.GOLD);

        assertTrue(
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .isEmpty());
    }

    @Test
    void displayNameCannotCreateMapping() {
        MiningSessionResource resource = MiningSessionResource.material(
                "NOT_A_REAL_PRODUCT",
                "Gold Ingot",
                TrackedMaterial.GOLD);

        assertTrue(
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .isEmpty());
    }

    @Test
    void wishingCompassRemainsUnsupported() {
        MiningSessionResource resource = MiningSessionResource.genericItem(
                "WISHING_COMPASS",
                "Wishing Compass");

        assertTrue(
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .isEmpty());
    }

    @Test
    void currencyIsExcluded() {
        MiningSessionResource powder = MiningSessionResource.currency(
                "GEMSTONE_POWDER",
                "Gemstone Powder");

        assertFalse(MiningSessionResourcePriceMapping.isSupportedItemResource(
                MiningSessionCategory.CURRENCY,
                powder));
    }

    private static void assertUnsupportedGemstone(GemstoneTier tier) {
        MiningSessionResource resource = MiningSessionResource.gemstone(
                GemstoneType.RUBY.bazaarId(tier),
                GemstoneType.RUBY.itemName(tier),
                GemstoneType.RUBY,
                tier);

        assertTrue(
                MiningSessionResourcePriceMapping.bazaarProductId(resource)
                        .isEmpty(),
                tier.name());
    }
}
