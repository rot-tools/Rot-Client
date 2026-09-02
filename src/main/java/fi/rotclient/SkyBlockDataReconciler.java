package fi.rotclient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reconciles official Hypixel snapshots with handwritten domain rules and
 * optional enrichment. Never silently overrides explicit official facts.
 */
final class SkyBlockDataReconciler {
    record EnrichmentHint(
            String stableId,
            String displayName,
            List<String> aliases,
            String hypixelItemId,
            String bazaarProductId,
            List<String> areas,
            String sourceHint,
            SkyBlockProvenance provenance) {
        EnrichmentHint {
            stableId = SkyBlockItemId.normalize(stableId);
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            areas = areas == null ? List.of() : List.copyOf(areas);
        }
    }

    private SkyBlockDataReconciler() {
    }

    static SkyBlockCanonicalDataset reconcile(
            SkyBlockDomainRules domainRules,
            SkyBlockOfficialSnapshot official,
            List<EnrichmentHint> enrichment,
            String generatedAt) {
        if (domainRules == null) {
            throw new IllegalArgumentException("domainRules cannot be null");
        }
        String when = generatedAt == null || generatedAt.isBlank()
                ? Instant.now().toString()
                : generatedAt.trim();
        Map<String, EnrichmentHint> enrichmentById = indexEnrichment(enrichment);
        List<SkyBlockCanonicalItem> items = new ArrayList<>();
        List<SkyBlockDataConflict> conflicts = new ArrayList<>();
        LinkedHashSet<String> relevantBazaar = new LinkedHashSet<>();

        for (SkyBlockDomainRules.Rule rule : domainRules.rules()) {
            ReconciledRow row = reconcileOne(rule, official, enrichmentById.get(rule.stableId()), when);
            items.add(row.item());
            conflicts.addAll(row.conflicts());
            if (row.item().bazaarProductId() != null) {
                relevantBazaar.add(row.item().bazaarProductId());
            }
        }

        // Catalog is never an observation allow-list: enrichment-only IDs that
        // are absent from domain rules are recorded as conflicts/notes, not
        // auto-promoted into canonical truth without domain ownership.
        if (enrichment != null) {
            for (EnrichmentHint hint : enrichment) {
                if (hint == null || hint.stableId().isEmpty()) {
                    continue;
                }
                boolean owned = domainRules.rules().stream()
                        .anyMatch(rule -> rule.stableId().equals(hint.stableId()));
                if (!owned) {
                    conflicts.add(new SkyBlockDataConflict(
                            hint.stableId(),
                            "stableId",
                            "",
                            hint.stableId(),
                            SkyBlockDataAuthorityLayer.COMMUNITY_ENRICHMENT.name(),
                            "NOT_PROMOTED",
                            "Enrichment-only id was not auto-promoted into canonical dataset"));
                }
            }
        }

        Set<String> bazaarIndex = official == null
                ? relevantBazaar
                : official.bazaarProductIds();

        List<SkyBlockProvenance> datasetProvenance = new ArrayList<>();
        datasetProvenance.add(SkyBlockProvenance.rotDomain(
                when, "Handwritten Rot Client domain rules"));
        if (official != null) {
            datasetProvenance.add(SkyBlockProvenance.hypixel(
                    when,
                    "Official items lastUpdated=" + official.lastUpdated()
                            + "; itemCount=" + official.itemsById().size()
                            + "; bazaarProducts=" + official.bazaarProductIds().size()));
        }

        return new SkyBlockCanonicalDataset(
                "canonical-items.v1",
                when,
                items,
                bazaarIndex.isEmpty() ? relevantBazaar : bazaarIndex,
                conflicts,
                datasetProvenance);
    }

    private static Map<String, EnrichmentHint> indexEnrichment(
            List<EnrichmentHint> enrichment) {
        Map<String, EnrichmentHint> map = new LinkedHashMap<>();
        if (enrichment == null) {
            return map;
        }
        for (EnrichmentHint hint : enrichment) {
            if (hint == null || hint.stableId().isEmpty()) {
                continue;
            }
            map.putIfAbsent(hint.stableId(), hint);
        }
        return map;
    }

    private record ReconciledRow(
            SkyBlockCanonicalItem item,
            List<SkyBlockDataConflict> conflicts) {
    }

    private static ReconciledRow reconcileOne(
            SkyBlockDomainRules.Rule rule,
            SkyBlockOfficialSnapshot official,
            EnrichmentHint enrichment,
            String when) {
        List<SkyBlockDataConflict> conflicts = new ArrayList<>();
        List<SkyBlockProvenance> provenance = new ArrayList<>();
        provenance.add(SkyBlockProvenance.rotDomain(
                when, "Domain rule for " + rule.stableId()));

        String hypixelId = firstNonNull(rule.hypixelItemIdHint(), rule.stableId());
        String bazaarId = firstNonNull(rule.bazaarProductIdHint(), hypixelId);
        String display = rule.displayName();
        List<String> aliases = new ArrayList<>(rule.aliases());
        List<String> areas = new ArrayList<>(rule.areas());
        String sourceHint = rule.sourceHint();
        String idAuthority = SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name();
        String bazaarAuthority = SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name();
        String confidence = "HIGH";
        List<String> notes = new ArrayList<>(rule.notes());

        Optional<SkyBlockOfficialSnapshot.OfficialItem> officialItem =
                official == null ? Optional.empty() : official.item(hypixelId);
        if (officialItem.isEmpty() && official != null) {
            officialItem = official.item(rule.stableId());
        }

        if (officialItem.isPresent()) {
            SkyBlockOfficialSnapshot.OfficialItem oi = officialItem.get();
            hypixelId = oi.id();
            idAuthority = SkyBlockDataAuthorityLayer.OFFICIAL_HYPIXEL.name();
            provenance.add(SkyBlockProvenance.hypixel(
                    when, "Matched official item " + oi.id()));
            if (!oi.name().isBlank()
                    && !oi.name().equalsIgnoreCase(display)
                    && !aliasesContain(aliases, oi.name())) {
                // Keep domain display; surface official name as alias + conflict note.
                conflicts.add(new SkyBlockDataConflict(
                        rule.stableId(),
                        "displayName",
                        oi.name(),
                        display,
                        SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name(),
                        "KEEP_DOMAIN_DISPLAY_ADD_OFFICIAL_ALIAS",
                        "Official name retained as alias; domain display preserved"));
                if (!aliasesContain(aliases, oi.name())) {
                    aliases.add(oi.name());
                }
            }
            if (!oi.id().equals(rule.stableId())) {
                conflicts.add(new SkyBlockDataConflict(
                        rule.stableId(),
                        "hypixelItemId",
                        oi.id(),
                        rule.stableId(),
                        SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name(),
                        "KEEP_DOMAIN_STABLE_ID",
                        "Domain stableId differs from official Hypixel item id"));
            }
        } else if (official != null) {
            conflicts.add(new SkyBlockDataConflict(
                    rule.stableId(),
                    "hypixelItemId",
                    "",
                    hypixelId,
                    SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name(),
                    "UNVERIFIED_OFFICIAL_ID",
                    "No matching official Hypixel items entry at refresh time"));
            confidence = "MEDIUM";
        }

        if (official != null) {
            String candidate = firstNonNull(bazaarId, hypixelId);
            if (candidate != null && official.hasBazaarProduct(candidate)) {
                bazaarId = candidate;
                bazaarAuthority = SkyBlockDataAuthorityLayer.OFFICIAL_HYPIXEL.name();
            } else if (candidate != null
                    && official.hasBazaarProduct(hypixelId)
                    && !hypixelId.equals(candidate)) {
                conflicts.add(new SkyBlockDataConflict(
                        rule.stableId(),
                        "bazaarProductId",
                        hypixelId,
                        candidate,
                        SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name(),
                        "USE_OFFICIAL_PRODUCT",
                        "Domain bazaar hint missing on Bazaar; official product id used"));
                bazaarId = hypixelId;
                bazaarAuthority = SkyBlockDataAuthorityLayer.OFFICIAL_HYPIXEL.name();
            } else if (candidate != null && rule.bazaarProductIdHint() != null) {
                conflicts.add(new SkyBlockDataConflict(
                        rule.stableId(),
                        "bazaarProductId",
                        "",
                        candidate,
                        SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name(),
                        "KEEP_DOMAIN_HINT_UNVERIFIED",
                        "Bazaar product not present in official product index"));
            } else if (rule.bazaarProductIdHint() == null) {
                bazaarId = null;
                bazaarAuthority = SkyBlockDataAuthorityLayer.ROT_DOMAIN_RULE.name();
            }
        }

        if (enrichment != null) {
            provenance.add(enrichment.provenance());
            mergeEnrichmentGaps(
                    rule,
                    enrichment,
                    aliases,
                    areas,
                    conflicts,
                    notes);
            if (sourceHint == null && enrichment.sourceHint() != null) {
                sourceHint = enrichment.sourceHint();
            }
            if ((display == null || display.isBlank() || display.equals(rule.stableId()))
                    && enrichment.displayName() != null
                    && !enrichment.displayName().isBlank()) {
                display = enrichment.displayName();
            }
            // Never silently override official hypixel/bazaar when already official.
            if (enrichment.hypixelItemId() != null
                    && !enrichment.hypixelItemId().equals(hypixelId)
                    && SkyBlockDataAuthorityLayer.OFFICIAL_HYPIXEL.name()
                    .equals(idAuthority)) {
                conflicts.add(new SkyBlockDataConflict(
                        rule.stableId(),
                        "hypixelItemId",
                        hypixelId,
                        enrichment.hypixelItemId(),
                        SkyBlockDataAuthorityLayer.COMMUNITY_ENRICHMENT.name(),
                        "KEEP_OFFICIAL",
                        "Community hypixel id ignored because official fact exists"));
            }
            if (enrichment.bazaarProductId() != null
                    && bazaarId != null
                    && !enrichment.bazaarProductId().equals(bazaarId)
                    && SkyBlockDataAuthorityLayer.OFFICIAL_HYPIXEL.name()
                    .equals(bazaarAuthority)) {
                conflicts.add(new SkyBlockDataConflict(
                        rule.stableId(),
                        "bazaarProductId",
                        bazaarId,
                        enrichment.bazaarProductId(),
                        SkyBlockDataAuthorityLayer.COMMUNITY_ENRICHMENT.name(),
                        "KEEP_OFFICIAL",
                        "Community bazaar id ignored because official fact exists"));
            }
            if (bazaarId == null && enrichment.bazaarProductId() != null) {
                bazaarId = enrichment.bazaarProductId();
                bazaarAuthority =
                        SkyBlockDataAuthorityLayer.COMMUNITY_ENRICHMENT.name();
                notes.add("Bazaar product filled from community enrichment gap");
            }
        }

        SkyBlockCanonicalItem item = new SkyBlockCanonicalItem(
                rule.stableId(),
                display,
                aliases,
                hypixelId,
                bazaarId,
                areas,
                sourceHint,
                idAuthority,
                bazaarAuthority,
                confidence,
                provenance,
                notes);
        return new ReconciledRow(item, conflicts);
    }

    private static void mergeEnrichmentGaps(
            SkyBlockDomainRules.Rule rule,
            EnrichmentHint enrichment,
            List<String> aliases,
            List<String> areas,
            List<SkyBlockDataConflict> conflicts,
            List<String> notes) {
        if (enrichment.aliases() != null) {
            for (String alias : enrichment.aliases()) {
                if (alias == null || alias.isBlank()) {
                    continue;
                }
                if (!aliasesContain(aliases, alias)
                        && !alias.equalsIgnoreCase(rule.displayName())) {
                    aliases.add(alias.trim());
                    notes.add("Alias added from enrichment: " + alias.trim());
                }
            }
        }
        if (enrichment.areas() != null) {
            for (String area : enrichment.areas()) {
                if (area == null || area.isBlank()) {
                    continue;
                }
                String normalized = area.trim().toUpperCase();
                if (!areas.contains(normalized)) {
                    // Area expansion is enrichment; do not remove domain areas.
                    areas.add(normalized);
                    notes.add("Area added from enrichment: " + normalized);
                }
            }
        }
        if (enrichment.displayName() != null
                && !enrichment.displayName().isBlank()
                && !enrichment.displayName().equalsIgnoreCase(rule.displayName())) {
            conflicts.add(new SkyBlockDataConflict(
                    rule.stableId(),
                    "displayName",
                    rule.displayName(),
                    enrichment.displayName(),
                    SkyBlockDataAuthorityLayer.COMMUNITY_ENRICHMENT.name(),
                    "KEEP_DOMAIN_DISPLAY",
                    "Community display name not silently overriding domain display"));
        }
    }

    private static boolean aliasesContain(List<String> aliases, String candidate) {
        for (String alias : aliases) {
            if (alias != null && alias.equalsIgnoreCase(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonNull(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        if (b != null && !b.isBlank()) {
            return b;
        }
        return null;
    }
}
