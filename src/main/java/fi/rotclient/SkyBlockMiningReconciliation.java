package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Pure official-vs-Rot mining data reconciliation. */
final class SkyBlockMiningReconciliation {
    enum Status {
        OFFICIAL_MATCH,
        ALIAS_MATCH,
        INTENTIONAL_MAPPING,
        CONFLICT,
        OFFICIAL_ONLY,
        ROT_ONLY,
        COMMUNITY_REFERENCE_ONLY,
        LIVE_ONLY,
        UNRESOLVED
    }

    record Row(
            String resourceId,
            String collectionId,
            String hypixelItemId,
            String bazaarProductId,
            String officialName,
            Set<Status> statuses,
            List<String> missingFacts,
            List<String> conflicts,
            boolean canonicalItemCovered) {
        Row {
            resourceId = safe(resourceId);
            collectionId = safe(collectionId);
            hypixelItemId = safe(hypixelItemId);
            bazaarProductId = safe(bazaarProductId);
            officialName = safe(officialName);
            statuses = Set.copyOf(statuses);
            missingFacts = List.copyOf(missingFacts);
            conflicts = List.copyOf(conflicts);
        }
    }

    record Report(
            List<Row> rows,
            int officialCollectionCount,
            int rotCollectionCount,
            int canonicalItemCoveredCount,
            Map<Status, Integer> statusCounts) {
        Report {
            rows = List.copyOf(rows);
            statusCounts = Map.copyOf(statusCounts);
        }
    }

    private SkyBlockMiningReconciliation() {
    }

    static Report reconcile(
            SkyBlockMiningResourceRegistry mechanics,
            SkyBlockCanonicalDataset canonical,
            SkyBlockOfficialSnapshot official,
            SkyBlockOfficialCollectionsSnapshot collections) {
        List<Row> rows = new ArrayList<>();
        Set<String> representedCollections = new LinkedHashSet<>();

        for (SkyBlockMiningResource resource : mechanics.resources()) {
            if (resource.isCollection() && resource.collectionId() != null) {
                representedCollections.add(resource.collectionId());
            }
            rows.add(reconcileResource(
                    resource, mechanics.sources(), canonical, official, collections));
        }

        for (String officialOnly : collections.miningCollectionIds()) {
            if (!representedCollections.contains(officialOnly)) {
                rows.add(new Row(
                        officialOnly,
                        officialOnly,
                        "",
                        "",
                        "",
                        Set.of(Status.OFFICIAL_ONLY, Status.UNRESOLVED),
                        List.of("Rot mining mechanics row"),
                        List.of(),
                        false));
            }
        }

        rows.sort(Comparator.comparing(Row::resourceId));
        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) counts.put(status, 0);
        for (Row row : rows) {
            for (Status status : row.statuses()) {
                counts.put(status, counts.get(status) + 1);
            }
        }
        int covered = (int) rows.stream().filter(Row::canonicalItemCovered).count();
        return new Report(
                rows,
                collections.miningCollectionIds().size(),
                representedCollections.size(),
                covered,
                counts);
    }

    private static Row reconcileResource(
            SkyBlockMiningResource resource,
            Map<String, String> sources,
            SkyBlockCanonicalDataset canonical,
            SkyBlockOfficialSnapshot official,
            SkyBlockOfficialCollectionsSnapshot collections) {
        Set<Status> statuses = new LinkedHashSet<>();
        List<String> missing = new ArrayList<>();
        List<String> conflicts = new ArrayList<>();

        boolean collectionMatch = !resource.isCollection()
                || collections.miningCollectionIds().contains(resource.collectionId());
        if (!collectionMatch) {
            conflicts.add("collectionId absent from official MINING collections");
        }

        SkyBlockOfficialSnapshot.OfficialItem officialItem =
                resource.hypixelItemId() == null
                        ? null
                        : official.item(resource.hypixelItemId()).orElse(null);
        boolean itemMatch = resource.hypixelItemId() == null || officialItem != null;
        if (!itemMatch) {
            conflicts.add("hypixelItemId absent from official items");
        }

        boolean bazaarMatch = resource.bazaarProductId() == null
                || official.hasBazaarProduct(resource.bazaarProductId());
        if (!bazaarMatch) {
            conflicts.add("bazaarProductId absent from official Bazaar");
        }

        if (collectionMatch && itemMatch && bazaarMatch) {
            statuses.add(Status.OFFICIAL_MATCH);
        }
        if (!conflicts.isEmpty()) statuses.add(Status.CONFLICT);

        String officialName = officialItem == null ? "" : officialItem.name();
        if (!officialName.isBlank()
                && resource.aliases().stream()
                .anyMatch(alias -> alias.equalsIgnoreCase(officialName))) {
            statuses.add(Status.ALIAS_MATCH);
        }
        if (isIntentionalMapping(resource)) {
            statuses.add(Status.INTENTIONAL_MAPPING);
        }

        boolean canonicalCovered = canonical.items().stream().anyMatch(item ->
                item.stableId().equals(resource.canonicalResourceId())
                        || (resource.hypixelItemId() != null
                        && resource.hypixelItemId().equals(item.hypixelItemId())));
        if (!canonicalCovered) {
            missing.add("canonical item mapping");
        }

        collectMissingMechanics(resource, missing);
        if (resource.provenanceRefs().isEmpty()) {
            missing.add("provenance");
        }

        boolean hasOfficialReference = resource.provenanceRefs().stream()
                .anyMatch(ref -> ref.startsWith("hypixel-")
                        || ref.startsWith("wiki-"));
        boolean hasCommunityReference = resource.provenanceRefs().stream()
                .map(sources::get)
                .filter(value -> value != null)
                .anyMatch(value -> value.toLowerCase(Locale.ROOT)
                        .contains("github.com"));
        if (hasCommunityReference && !hasOfficialReference) {
            statuses.add(Status.COMMUNITY_REFERENCE_ONLY);
        }
        if ("LIVE_OBSERVED".equals(resource.evidenceState())
                && !collectionMatch && officialItem == null) {
            statuses.add(Status.LIVE_ONLY);
        }
        if (!resource.isCollection() && officialItem == null) {
            statuses.add(Status.ROT_ONLY);
        }
        if (!missing.isEmpty() || !conflicts.isEmpty()) {
            statuses.add(Status.UNRESOLVED);
        }

        return new Row(
                resource.canonicalResourceId(),
                resource.collectionId(),
                resource.hypixelItemId(),
                resource.bazaarProductId(),
                officialName,
                statuses,
                missing,
                conflicts,
                canonicalCovered);
    }

    private static boolean isIntentionalMapping(SkyBlockMiningResource resource) {
        if (resource.hypixelItemId() != null
                && !resource.canonicalResourceId().equals(resource.hypixelItemId())) {
            return true;
        }
        return resource.collectionId() != null
                && !resource.canonicalResourceId().equals(resource.collectionId());
    }

    private static void collectMissingMechanics(
            SkyBlockMiningResource resource,
            List<String> missing) {
        if (resource.physicalBlockTokens().isEmpty()) {
            missing.add("physical block evidence");
        }
        if (resource.sackAliases().isEmpty()) {
            missing.add("verified Sack aliases");
        }
        if (resource.bazaarProductId() == null
                && !"GEMSTONE_COLLECTION".equals(resource.canonicalResourceId())) {
            missing.add("Bazaar mapping");
        }
        if (resource.fortuneCategory() == MiningFortuneCategory.UNKNOWN) {
            missing.add("Fortune category");
        }
        if (resource.breakingPower() == null) missing.add("Breaking Power");
        if (resource.blockStrength() == null) missing.add("block strength");
        if (resource.baseYieldMin() == null || resource.baseYieldMax() == null) {
            missing.add("base yield");
        }
        if (resource.requiresAreaContext() && resource.areas().isEmpty()) {
            missing.add("required area/context");
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
