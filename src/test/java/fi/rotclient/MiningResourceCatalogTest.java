package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MiningResourceCatalogTest {
    private final MiningResourceCatalog catalog = new MiningResourceCatalog();

    @Test
    void everyMaterialHasStableRawIdentity() {
        for (TrackedMaterial material : TrackedMaterial.values()) {
            MiningResourceCatalog.ResourceDefinition definition =
                    catalog.fromMaterial(material).orElseThrow();
            assertEquals(material, definition.material());
            assertEquals(material.rawBazaarId(), definition.resource().resourceId());
        }
    }

    @Test
    void hardStoneIsKnownAndRuntimeSupported() {
        MiningResourceCatalog.ResourceDefinition raw =
                catalog.fromMaterial(TrackedMaterial.HARD_STONE)
                        .orElseThrow();
        MiningResourceCatalog.ResourceDefinition enchanted =
                catalog.fromExactSackItem("Enchanted Hard Stone")
                        .orElseThrow();

        assertEquals("HARD_STONE", raw.resource().resourceId());
        assertEquals(
                "ENCHANTED_HARD_STONE",
                enchanted.resource().resourceId());
        assertEquals(TrackedMaterial.HARD_STONE, raw.material());
        assertEquals(576, TrackedMaterial.HARD_STONE.rawPerEnchanted());
        assertTrue(raw.exactQuantitySupported());
        assertTrue(raw.directBreakFamilySupported());
        assertTrue(raw.shadowAcceptanceSupported());
        assertTrue(catalog.familyFor(
                TrackedMaterial.HARD_STONE).isPresent());
    }

    @Test
    void cobblestoneIsKnownAndRuntimeSupported() {
        MiningResourceCatalog.ResourceDefinition raw =
                catalog.fromMaterial(TrackedMaterial.COBBLESTONE)
                        .orElseThrow();
        MiningResourceCatalog.ResourceDefinition enchanted =
                catalog.fromExactSackItem("Enchanted Cobblestone")
                        .orElseThrow();

        assertEquals("COBBLESTONE", raw.resource().resourceId());
        assertEquals(
                "ENCHANTED_COBBLESTONE",
                enchanted.resource().resourceId());
        assertEquals(TrackedMaterial.COBBLESTONE, raw.material());
        assertTrue(raw.exactQuantitySupported());
        assertTrue(raw.directBreakFamilySupported());
        assertTrue(raw.shadowAcceptanceSupported());
        assertTrue(catalog.familyFor(
                TrackedMaterial.COBBLESTONE).isPresent());
    }

    @Test
    void tungstenAndUmberAreRuntimeSupported() {
        for (TrackedMaterial material : List.of(
                TrackedMaterial.TUNGSTEN,
                TrackedMaterial.UMBER)) {
            MiningResourceCatalog.ResourceDefinition definition =
                    catalog.fromMaterial(material).orElseThrow();

            assertTrue(definition.known(), material.id());
            assertTrue(definition.exactQuantitySupported(), material.id());
            assertTrue(definition.directBreakFamilySupported(), material.id());
            assertTrue(definition.shadowAcceptanceSupported(), material.id());
            assertTrue(catalog.familyFor(material).isPresent(), material.id());
        }
    }

    @Test
    void everyGemstoneHasDirectBreakFamily() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            MiningSessionDirectBreakTracker.FamilyKey family =
                    catalog.familyFor(gemstone).orElseThrow();
            assertEquals(gemstone, family.gemstone());
        }
    }

    @Test
    void everyGemstoneTierMapsExactly() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            for (GemstoneTier tier : GemstoneTier.values()) {
                MiningResourceCatalog.ResourceDefinition definition =
                        catalog.fromExactSackItem(
                                gemstone.itemName(tier)).orElseThrow();
                assertEquals(gemstone, definition.gemstone());
                assertEquals(tier, definition.gemstoneTier());
                assertEquals(gemstone.bazaarId(tier),
                        definition.resource().resourceId());
            }
        }
    }

    @Test
    void pristineFlawedMappingIsExact() {
        MiningResourceCatalog.ResourceDefinition definition =
                catalog.fromGemstone(
                        GemstoneType.TOPAZ,
                        GemstoneTier.FLAWED).orElseThrow();

        assertEquals("FLAWED_TOPAZ_GEM", definition.resource().resourceId());
        assertTrue(definition.shadowAcceptanceSupported());
    }

    @Test
    void onlyRoughAndFlawedGemstonesAreShadowAccepted() {
        for (GemstoneType gemstone : GemstoneType.values()) {
            assertTrue(catalog.fromGemstone(gemstone, GemstoneTier.ROUGH)
                    .orElseThrow().shadowAcceptanceSupported());
            assertTrue(catalog.fromGemstone(gemstone, GemstoneTier.FLAWED)
                    .orElseThrow().shadowAcceptanceSupported());
            assertFalse(catalog.fromGemstone(gemstone, GemstoneTier.FINE)
                    .orElseThrow().shadowAcceptanceSupported());
            assertFalse(catalog.fromGemstone(gemstone, GemstoneTier.FLAWLESS)
                    .orElseThrow().shadowAcceptanceSupported());
            assertFalse(catalog.fromGemstone(gemstone, GemstoneTier.PERFECT)
                    .orElseThrow().shadowAcceptanceSupported());
        }
    }

    @Test
    void unknownAndPartialNamesDoNotMatch() {
        assertTrue(catalog.fromExactSackItem("Unknown Ore").isEmpty());
        assertTrue(catalog.fromExactSackItem("Rough Ruby").isEmpty());
        assertTrue(catalog.fromExactSackItem("Ruby Gemstone").isEmpty());
        assertTrue(catalog.fromExactSackItem("Gold").isEmpty());
    }

    @Test
    void normalizationIsLimitedToCaseAndWhitespace() {
        assertTrue(catalog.fromExactSackItem(
                "  ROUGH   RUBY   GEMSTONE ").isPresent());
        assertTrue(catalog.fromExactSackItem("gold ingot").isPresent());
        assertTrue(catalog.fromExactSackItem("Rough-Ruby-Gemstone").isEmpty());
    }

    @Test
    void verifiedMaterialAliasesMapExactly() {
        assertEquals("GOLD_INGOT", catalog.fromExactSackItem("Gold Ingot")
                .orElseThrow().resource().resourceId());
        assertEquals("ENCHANTED_GOLD", catalog.fromExactSackItem(
                "Enchanted Gold Ingot").orElseThrow().resource().resourceId());
        assertEquals("TITANIUM_ORE", catalog.fromExactSackItem("Titanium")
                .orElseThrow().resource().resourceId());
        assertEquals("REDSTONE", catalog.fromExactSackItem("Redstone Dust")
                .orElseThrow().resource().resourceId());
    }

    @Test
    void duplicateAliasesAreRejected() {
        MiningResourceCatalog.ResourceDefinition gold = definition(
                TrackedMaterial.GOLD, "SHARED");
        MiningResourceCatalog.ResourceDefinition diamond = definition(
                TrackedMaterial.DIAMOND, "shared");

        assertThrows(
                IllegalArgumentException.class,
                () -> new MiningResourceCatalog(List.of(gold, diamond)));
    }

    @Test
    void displayNameDoesNotBecomeIdentity() {
        MiningSessionResource first = MiningSessionResource.material(
                "GOLD_INGOT", "Gold Ingot", TrackedMaterial.GOLD);
        MiningSessionResource renamed = MiningSessionResource.material(
                "GOLD_INGOT", "Renamed", TrackedMaterial.GOLD);

        assertEquals(first, renamed);
        assertEquals(first.hashCode(), renamed.hashCode());
    }

    @Test
    void genericAndTypedResourcesRemainDifferent() {
        MiningSessionResource typed = catalog.fromMaterial(TrackedMaterial.GOLD)
                .orElseThrow().resource();
        MiningSessionResource generic = MiningSessionResource.genericItem(
                typed.resourceId(), typed.displayName());

        assertNotEquals(typed, generic);
    }

    @Test
    void unknownMaterialNeverFallsBackToGold() {
        assertTrue(catalog.fromMaterial(null).isEmpty());
        assertTrue(catalog.fromGemstone(null, GemstoneTier.ROUGH).isEmpty());
    }

    @Test
    void catalogCollectionsAreImmutable() {
        assertThrows(
                UnsupportedOperationException.class,
                () -> catalog.definitions().clear());
        MiningResourceCatalog.ResourceDefinition ruby =
                catalog.fromGemstone(
                        GemstoneType.RUBY,
                        GemstoneTier.ROUGH).orElseThrow();
        assertThrows(
                UnsupportedOperationException.class,
                () -> ruby.aliases().add("Another Ruby"));
    }

    private static MiningResourceCatalog.ResourceDefinition definition(
            TrackedMaterial material,
            String alias) {
        return new MiningResourceCatalog.ResourceDefinition(
                MiningSessionResource.material(
                        material.rawBazaarId(),
                        material.rawItemName(),
                        material),
                material,
                null,
                null,
                MiningSessionDirectBreakTracker.FamilyKey.material(material),
                true,
                true,
                true,
                true,
                Set.of(alias));
    }
}
