package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockMiningRelationsTest {
    @Test
    void everyReviewedEnchantedRelationAcceptsRotAndOfficialIds() {
        SkyBlockItemRelations relations = SkyBlockItemRelations.builtin();
        for (SkyBlockMiningResource resource
                : SkyBlockMiningResourceRegistry.bundled().resources()) {
            if (resource.enchantedItemId() == null) continue;
            assertEquals(
                    resource.enchantedItemId(),
                    relations.enchantedFormOf(resource.canonicalResourceId())
                            .orElseThrow(),
                    resource.canonicalResourceId());
            if (resource.hypixelItemId() != null) {
                assertEquals(
                        resource.enchantedItemId(),
                        relations.enchantedFormOf(resource.hypixelItemId())
                                .orElseThrow(),
                        resource.hypixelItemId());
            }
        }
    }

    @Test
    void allTwelveGemstoneFamiliesHaveCompleteTierRelations() {
        SkyBlockItemRelations relations = SkyBlockItemRelations.builtin();
        String[] gems = {
                "RUBY", "AMBER", "SAPPHIRE", "JADE", "AMETHYST", "TOPAZ",
                "JASPER", "OPAL", "ONYX", "AQUAMARINE", "CITRINE", "PERIDOT"
        };
        String[] tiers = {"ROUGH", "FLAWED", "FINE", "FLAWLESS", "PERFECT"};
        for (String gem : gems) {
            for (int i = 0; i < tiers.length - 1; i++) {
                assertEquals(
                        tiers[i + 1] + "_" + gem + "_GEM",
                        relations.nextGemstoneTier(
                                tiers[i] + "_" + gem + "_GEM").orElseThrow());
            }
            assertTrue(relations.nextGemstoneTier(
                    "PERFECT_" + gem + "_GEM").isEmpty());
        }
    }

    @Test
    void onlyRuntimeObservedRawSackAliasesAreClaimed() {
        SkyBlockMiningResourceRegistry registry =
                SkyBlockMiningResourceRegistry.bundled();
        assertEquals(
                java.util.List.of("Mithril"),
                registry.lookup("MITHRIL").orElseThrow().sackAliases());
        assertEquals(
                java.util.List.of("Titanium"),
                registry.lookup("TITANIUM").orElseThrow().sackAliases());
        assertFalse(registry.lookup("GLACITE").orElseThrow()
                .sackAliases().contains("Glacite"));
    }
}
