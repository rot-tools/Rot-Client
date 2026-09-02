package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SkyBlockMiningReconciliationTest {
    @Test
    void reportsIntentionalMappingAndOfficialOnlyCollection() {
        SkyBlockMiningResourceRegistry mechanics =
                SkyBlockMiningResourceRegistry.parse(new StringReader("""
                        {
                          "schemaVersion": "mining-resources.v2",
                          "reviewedAt": "2026-08-09",
                          "sources": {
                            "hypixel": "https://api.hypixel.net/"
                          },
                          "resources": [{
                            "canonicalResourceId": "TITANIUM",
                            "displayName": "Titanium",
                            "kind": "SPECIAL_RESOURCE",
                            "hypixelItemId": "TITANIUM_ORE",
                            "bazaarProductId": "TITANIUM_ORE",
                            "droppedItemId": "TITANIUM_ORE",
                            "aliases": ["Titanium"],
                            "sackAliases": ["Titanium"],
                            "physicalBlockTokens": ["POLISHED_DIORITE"],
                            "enchantedItemId": "ENCHANTED_TITANIUM",
                            "areas": ["DWARVEN_MINES"],
                            "fortuneCategory": "DWARVEN_METAL",
                            "breakingPower": 5,
                            "blockStrength": 2000,
                            "baseYieldMin": 1,
                            "baseYieldMax": 1,
                            "requiresAreaContext": true,
                            "provenanceRefs": ["hypixel"]
                          }]
                        }
                        """));
        SkyBlockCanonicalDataset canonical = new SkyBlockCanonicalDataset(
                "canonical-items.v1",
                "2026-08-09T00:00:00Z",
                List.of(new SkyBlockCanonicalItem(
                        "TITANIUM",
                        "Titanium",
                        List.of("Titanium"),
                        "TITANIUM_ORE",
                        "TITANIUM_ORE",
                        List.of("DWARVEN_MINES"),
                        "MINING",
                        "OFFICIAL_HYPIXEL",
                        "OFFICIAL_HYPIXEL",
                        "HIGH",
                        List.of(),
                        List.of())),
                Set.of("TITANIUM_ORE"),
                List.of(),
                List.of());
        SkyBlockOfficialSnapshot official = new SkyBlockOfficialSnapshot(
                10L,
                Map.of("TITANIUM_ORE", new SkyBlockOfficialSnapshot.OfficialItem(
                        "TITANIUM_ORE", "Titanium", "STONE", "", null)),
                Set.of("TITANIUM_ORE"));
        SkyBlockOfficialCollectionsSnapshot collections =
                SkyBlockOfficialCollectionsSnapshot.parse("""
                        {
                          "success": true,
                          "lastUpdated": 20,
                          "collections": {
                            "MINING": {
                              "items": {"COAL": {}}
                            }
                          }
                        }
                        """);

        SkyBlockMiningReconciliation.Report report =
                SkyBlockMiningReconciliation.reconcile(
                        mechanics, canonical, official, collections);

        SkyBlockMiningReconciliation.Row titanium = report.rows().stream()
                .filter(row -> row.resourceId().equals("TITANIUM"))
                .findFirst().orElseThrow();
        assertTrue(titanium.statuses().contains(
                SkyBlockMiningReconciliation.Status.OFFICIAL_MATCH));
        assertTrue(titanium.statuses().contains(
                SkyBlockMiningReconciliation.Status.ALIAS_MATCH));
        assertTrue(titanium.statuses().contains(
                SkyBlockMiningReconciliation.Status.INTENTIONAL_MAPPING));
        assertFalse(titanium.statuses().contains(
                SkyBlockMiningReconciliation.Status.CONFLICT));
        assertTrue(titanium.canonicalItemCovered());

        SkyBlockMiningReconciliation.Row coal = report.rows().stream()
                .filter(row -> row.resourceId().equals("COAL"))
                .findFirst().orElseThrow();
        assertEquals(
                Set.of(
                        SkyBlockMiningReconciliation.Status.OFFICIAL_ONLY,
                        SkyBlockMiningReconciliation.Status.UNRESOLVED),
                coal.statuses());
    }

    @Test
    void reportRenderingIsStableForTheSameSnapshots() {
        SkyBlockMiningResourceRegistry mechanics =
                SkyBlockMiningResourceRegistry.bundled();
        SkyBlockCanonicalDataset canonical = SkyBlockCanonicalDatasetLoader
                .loadBundled().orElseThrow();
        SkyBlockOfficialSnapshot official = new SkyBlockOfficialSnapshot(
                100L, Map.of(), Set.of());
        SkyBlockOfficialCollectionsSnapshot collections =
                SkyBlockOfficialCollectionsSnapshot.parse("""
                        {"success":true,"lastUpdated":200,
                         "collections":{"MINING":{"items":{}}}}
                        """);
        SkyBlockMiningReconciliation.Report report =
                SkyBlockMiningReconciliation.reconcile(
                        mechanics, canonical, official, collections);

        String first = SkyBlockMiningReconciliationReport.render(
                report, mechanics, official, collections, 300L, 400L,
                false, List.of());
        String second = SkyBlockMiningReconciliationReport.render(
                report, mechanics, official, collections, 300L, 400L,
                false, List.of());

        assertEquals(first, second);
        assertTrue(first.contains("OFFICIAL_ONLY"));
        assertTrue(first.contains("Newest official source timestamp"));
        assertTrue(first.contains("official Bazaar lastUpdated: `400`"));
    }
}
