package fi.rotclient;

import java.time.Instant;
import java.util.StringJoiner;

/** Deterministic Markdown rendering for a mining reconciliation result. */
final class SkyBlockMiningReconciliationReport {
    private SkyBlockMiningReconciliationReport() {
    }

    static String render(
            SkyBlockMiningReconciliation.Report report,
            SkyBlockMiningResourceRegistry mechanics,
            SkyBlockOfficialSnapshot official,
            SkyBlockOfficialCollectionsSnapshot collections,
            long skillsLastUpdated,
            long bazaarLastUpdated,
            boolean usedNetwork,
            Iterable<String> warnings) {
        StringBuilder out = new StringBuilder();
        long sourceTimestamp = Math.max(
                Math.max(official.lastUpdated(), collections.lastUpdated()),
                Math.max(skillsLastUpdated, bazaarLastUpdated));
        out.append("# Rot Client Mining Data Reconciliation\n\n")
                .append("Newest official source timestamp: ")
                .append(Instant.ofEpochMilli(sourceTimestamp)).append("\n\n")
                .append("This is a developer review artifact. It does not mutate ")
                .append("canonical data and is not an observation allow-list.\n\n")
                .append("## Inputs\n\n")
                .append("- mechanics schema: `")
                .append(mechanics.schemaVersion()).append("`\n")
                .append("- mechanics reviewed: `")
                .append(mechanics.reviewedAt()).append("`\n")
                .append("- official items lastUpdated: `")
                .append(official.lastUpdated()).append("`\n")
                .append("- official collections lastUpdated: `")
                .append(collections.lastUpdated()).append("`\n")
                .append("- official skills lastUpdated: `")
                .append(skillsLastUpdated).append("`\n")
                .append("- official Bazaar lastUpdated: `")
                .append(bazaarLastUpdated).append("`\n")
                .append("- network used: `").append(usedNetwork).append("`\n\n")
                .append("## Summary\n\n")
                .append("- official Mining Collections: ")
                .append(report.officialCollectionCount()).append("\n")
                .append("- Rot collection rows: ")
                .append(report.rotCollectionCount()).append("\n")
                .append("- rows with canonical item coverage: ")
                .append(report.canonicalItemCoveredCount()).append(" / ")
                .append(report.rows().size()).append("\n\n")
                .append("| Status | Rows |\n| --- | ---: |\n");
        for (SkyBlockMiningReconciliation.Status status
                : SkyBlockMiningReconciliation.Status.values()) {
            out.append("| `").append(status).append("` | ")
                    .append(report.statusCounts().getOrDefault(status, 0))
                    .append(" |\n");
        }

        out.append("\n## Resource matrix\n\n")
                .append("| Resource | Collection | Hypixel item | Bazaar | ")
                .append("Canonical | Status | Missing facts | Conflicts |\n")
                .append("| --- | --- | --- | --- | --- | --- | --- | --- |\n");
        for (SkyBlockMiningReconciliation.Row row : report.rows()) {
            out.append("| `").append(cell(row.resourceId())).append("` | `")
                    .append(cell(row.collectionId())).append("` | `")
                    .append(cell(row.hypixelItemId())).append("` | `")
                    .append(cell(row.bazaarProductId())).append("` | ")
                    .append(row.canonicalItemCovered() ? "yes" : "no")
                    .append(" | ").append(join(row.statuses()))
                    .append(" | ").append(cell(String.join(", ", row.missingFacts())))
                    .append(" | ").append(cell(String.join(", ", row.conflicts())))
                    .append(" |\n");
        }

        StringJoiner warningText = new StringJoiner("\n");
        for (String warning : warnings) warningText.add("- " + warning);
        if (warningText.length() > 0) {
            out.append("\n## Cache warnings\n\n").append(warningText).append("\n");
        }
        return out.toString();
    }

    private static String join(Iterable<SkyBlockMiningReconciliation.Status> values) {
        StringJoiner joiner = new StringJoiner("<br>");
        java.util.Set<SkyBlockMiningReconciliation.Status> present =
                new java.util.HashSet<>();
        for (SkyBlockMiningReconciliation.Status value : values) present.add(value);
        for (SkyBlockMiningReconciliation.Status value
                : SkyBlockMiningReconciliation.Status.values()) {
            if (present.contains(value)) joiner.add("`" + value + "`");
        }
        return joiner.toString();
    }

    private static String cell(String value) {
        if (value == null || value.isBlank()) return "-";
        return value.replace("|", "\\|")
                .replace("\r", " ")
                .replace("\n", " ");
    }
}
