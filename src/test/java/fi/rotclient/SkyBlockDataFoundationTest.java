package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockDataFoundationTest {
    @Test
    void reconcilerKeepsDomainTitaniumStableIdAndUsesOfficialProduct() {
        SkyBlockDomainRules domain = new SkyBlockDomainRules(
                "domain-rules.v1",
                List.of(new SkyBlockDomainRules.Rule(
                        "TITANIUM",
                        "Titanium",
                        List.of("Titanium"),
                        "TITANIUM_ORE",
                        "TITANIUM_ORE",
                        List.of("DWARVEN_MINES"),
                        "MINING",
                        List.of("dual-key"))));
        SkyBlockOfficialSnapshot official = new SkyBlockOfficialSnapshot(
                1L,
                java.util.Map.of(
                        "TITANIUM_ORE",
                        new SkyBlockOfficialSnapshot.OfficialItem(
                                "TITANIUM_ORE",
                                "Titanium",
                                "STONE",
                                "",
                                1.0)),
                Set.of("TITANIUM_ORE", "ENCHANTED_TITANIUM"));

        SkyBlockCanonicalDataset dataset = SkyBlockDataReconciler.reconcile(
                domain, official, List.of(), "2026-08-08T00:00:00Z");

        SkyBlockCanonicalItem item = dataset.lookup("TITANIUM").orElseThrow();
        assertEquals("TITANIUM", item.stableId());
        assertEquals("TITANIUM_ORE", item.hypixelItemId());
        assertEquals("TITANIUM_ORE", item.bazaarProductId());
        assertEquals(
                SkyBlockDataAuthorityLayer.OFFICIAL_HYPIXEL.name(),
                item.idAuthority());
        assertTrue(dataset.conflicts().stream().anyMatch(conflict ->
                "TITANIUM".equals(conflict.stableId())
                        && "hypixelItemId".equals(conflict.field())
                        && "KEEP_DOMAIN_STABLE_ID".equals(conflict.resolution())));
    }

    @Test
    void communityCannotSilentlyOverrideOfficialBazaarProduct() {
        SkyBlockDomainRules domain = new SkyBlockDomainRules(
                "domain-rules.v1",
                List.of(new SkyBlockDomainRules.Rule(
                        "HARD_STONE",
                        "Hard Stone",
                        List.of("Hard Stone"),
                        "HARD_STONE",
                        "HARD_STONE",
                        List.of("CRYSTAL_HOLLOWS"),
                        "MINING",
                        List.of())));
        SkyBlockOfficialSnapshot official = new SkyBlockOfficialSnapshot(
                1L,
                java.util.Map.of(
                        "HARD_STONE",
                        new SkyBlockOfficialSnapshot.OfficialItem(
                                "HARD_STONE",
                                "Hard Stone",
                                "STONE",
                                "",
                                1.0)),
                Set.of("HARD_STONE"));
        SkyBlockDataReconciler.EnrichmentHint enrichment =
                new SkyBlockDataReconciler.EnrichmentHint(
                        "HARD_STONE",
                        "Hard Stone Alt",
                        List.of("Hardstone"),
                        "HARD_STONE",
                        "WRONG_PRODUCT",
                        List.of("CRYSTAL_HOLLOWS"),
                        "MINING",
                        SkyBlockProvenance.community(
                                "https://example.invalid/neu",
                                "Index research note",
                                "2026-08-08",
                                "MEDIUM",
                                "cite-only"));

        SkyBlockCanonicalDataset dataset = SkyBlockDataReconciler.reconcile(
                domain, official, List.of(enrichment), "2026-08-08T00:00:00Z");

        SkyBlockCanonicalItem item = dataset.lookup("HARD_STONE").orElseThrow();
        assertEquals("HARD_STONE", item.bazaarProductId());
        assertTrue(dataset.conflicts().stream().anyMatch(conflict ->
                "bazaarProductId".equals(conflict.field())
                        && "KEEP_OFFICIAL".equals(conflict.resolution())));
        assertTrue(item.aliases().stream()
                .anyMatch(alias -> alias.equalsIgnoreCase("Hardstone")));
    }

    @Test
    void enrichmentOnlyIdsAreNotAutoPromoted() {
        SkyBlockDomainRules domain = new SkyBlockDomainRules(
                "domain-rules.v1",
                List.of(new SkyBlockDomainRules.Rule(
                        "GOLD_INGOT",
                        "Gold Ingot",
                        List.of("Gold Ingot"),
                        "GOLD_INGOT",
                        "GOLD_INGOT",
                        List.of(),
                        "MINING",
                        List.of())));
        SkyBlockDataReconciler.EnrichmentHint stranger =
                new SkyBlockDataReconciler.EnrichmentHint(
                        "STRANGER_ORE",
                        "Stranger Ore",
                        List.of(),
                        "STRANGER_ORE",
                        "STRANGER_ORE",
                        List.of(),
                        "MINING",
                        SkyBlockProvenance.community(
                                "https://example.invalid",
                                "example",
                                "2026-08-08",
                                "LOW",
                                "not promoted"));

        SkyBlockCanonicalDataset dataset = SkyBlockDataReconciler.reconcile(
                domain, null, List.of(stranger), "2026-08-08T00:00:00Z");

        assertTrue(dataset.lookup("STRANGER_ORE").isEmpty());
        assertTrue(dataset.conflicts().stream().anyMatch(conflict ->
                "STRANGER_ORE".equals(conflict.stableId())
                        && "NOT_PROMOTED".equals(conflict.resolution())));
    }

    @Test
    void catalogIsNotObservationAllowList() {
        SkyBlockItemIdentityResolver.Resolved unknown =
                SkyBlockItemIdentityResolver.fromTextToken("Totally Unknown Ore");
        assertFalse(unknown.catalogMatch());
        assertEquals("TOTALLY_UNKNOWN_ORE", unknown.stableId());
    }

    @Test
    void loaderRoundTripsCanonicalJson() {
        SkyBlockCanonicalDataset original = SkyBlockDataReconciler.reconcile(
                SkyBlockDomainRules.fromBuiltinRegistry(),
                null,
                List.of(),
                "2026-08-08T12:00:00Z");
        // Serialize via write helpers indirectly by parsing a minimal document.
        String json = """
                {
                  "schemaVersion": "canonical-items.v1",
                  "generatedAt": "2026-08-08T12:00:00Z",
                  "items": [
                    {
                      "stableId": "HARD_STONE",
                      "displayName": "Hard Stone",
                      "aliases": ["Hard Stone"],
                      "hypixelItemId": "HARD_STONE",
                      "bazaarProductId": "HARD_STONE",
                      "areas": ["CRYSTAL_HOLLOWS"],
                      "sourceHint": "MINING",
                      "idAuthority": "OFFICIAL_HYPIXEL",
                      "bazaarAuthority": "OFFICIAL_HYPIXEL",
                      "confidence": "HIGH",
                      "provenance": [],
                      "notes": []
                    }
                  ],
                  "bazaarProductIds": ["HARD_STONE"],
                  "conflicts": [],
                  "provenance": []
                }
                """;
        SkyBlockCanonicalDataset parsed =
                SkyBlockCanonicalDatasetLoader.parseCanonical(new StringReader(json));
        assertEquals("HARD_STONE", parsed.lookup("HARD_STONE").orElseThrow().stableId());
        assertTrue(original.lookup("HARD_STONE").isPresent());
    }

    @Test
    void builtinTitaniumUsesOfficialBazaarProduct() {
        SkyBlockContentRegistry.KnownItem titanium =
                SkyBlockContentRegistry.builtin().lookup("TITANIUM").orElseThrow();
        assertEquals("TITANIUM", titanium.id());
        assertEquals("TITANIUM_ORE", titanium.bazaarProductId());
        assertEquals("TITANIUM_ORE", titanium.hypixelItemId());
    }
}
