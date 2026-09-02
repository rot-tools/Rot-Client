package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Development-only helper that turns sanitized tracking-summary style lines
 * into a reviewable fixture candidate. Never mutates the production catalog
 * and never auto-promotes trust.
 */
final class RuntimeFixtureCandidate {
    record ItemRow(
            String displayName,
            String resolvedId,
            String classification,
            boolean catalogMatch,
            long observedQuantity) {
    }

    record Candidate(
            String targetId,
            String areaId,
            List<ItemRow> items,
            List<String> unknownIds,
            List<String> reviewNotes) {
    }

    private RuntimeFixtureCandidate() {
    }

    static Candidate fromSummaryBody(String summaryBody) {
        String target = "UNKNOWN";
        String area = "UNKNOWN_SKYBLOCK_AREA";
        List<ItemRow> items = new ArrayList<>();
        Set<String> unknowns = new LinkedHashSet<>();
        List<String> notes = new ArrayList<>();
        if (summaryBody == null || summaryBody.isBlank()) {
            notes.add("empty_summary");
            return new Candidate(target, area, List.of(), List.of(), notes);
        }

        String display = "";
        String resolved = "";
        String classification = "";
        boolean catalogMatch = false;
        long qty = 0L;
        for (String rawLine : summaryBody.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.startsWith("target=")) {
                target = valueOf(line);
            } else if (line.startsWith("area=")) {
                area = valueOf(line);
            } else if (line.startsWith("DISPLAY NAME=")) {
                flush(items, unknowns, display, resolved, classification,
                        catalogMatch, qty);
                display = valueOf(line);
                resolved = "";
                classification = "";
                catalogMatch = false;
                qty = 0L;
            } else if (line.startsWith("RESOLVED ID=")) {
                resolved = valueOf(line);
            } else if (line.startsWith("CATALOG MATCH=")) {
                catalogMatch = "YES".equalsIgnoreCase(valueOf(line));
            } else if (line.startsWith("TOTAL OBSERVED QUANTITY=")) {
                qty = parseLong(valueOf(line));
            } else if (line.startsWith("OTHERS OBSERVATIONS=")
                    || line.startsWith("OTHERS COUNT=")) {
                if (parseLong(valueOf(line)) > 0L) {
                    classification = "OTHER_MINED";
                }
            } else if (line.startsWith("TARGET OBSERVATIONS=")
                    || line.startsWith("TARGET COUNT=")) {
                if (parseLong(valueOf(line)) > 0L) {
                    classification = "TARGET_MINED";
                }
            } else if (line.startsWith("NOTES=")
                    && !"NONE".equalsIgnoreCase(valueOf(line))) {
                notes.add(display + ":" + valueOf(line));
            }
        }
        flush(items, unknowns, display, resolved, classification,
                catalogMatch, qty);
        notes.add("human_research_verification_required");
        notes.add("do_not_auto_promote_to_catalog");
        return new Candidate(
                target,
                area,
                List.copyOf(items),
                List.copyOf(unknowns),
                List.copyOf(notes));
    }

    static String renderReviewText(Candidate candidate) {
        StringBuilder out = new StringBuilder();
        out.append("FIXTURE CANDIDATE (review only)\n");
        out.append("target=").append(candidate.targetId()).append('\n');
        out.append("area=").append(candidate.areaId()).append('\n');
        out.append("items=").append(candidate.items().size()).append('\n');
        for (ItemRow item : candidate.items()) {
            out.append("- ")
                    .append(item.displayName())
                    .append(" | ")
                    .append(item.resolvedId())
                    .append(" | ")
                    .append(item.classification())
                    .append(" | qty=")
                    .append(item.observedQuantity())
                    .append(" | catalog=")
                    .append(item.catalogMatch() ? "YES" : "NO")
                    .append('\n');
        }
        out.append("unknown_ids=")
                .append(String.join(",", candidate.unknownIds()))
                .append('\n');
        out.append("notes=")
                .append(String.join(";", candidate.reviewNotes()))
                .append('\n');
        return out.toString();
    }

    private static void flush(
            List<ItemRow> items,
            Set<String> unknowns,
            String display,
            String resolved,
            String classification,
            boolean catalogMatch,
            long qty) {
        if (display == null || display.isBlank()) {
            return;
        }
        String id = resolved == null || resolved.isBlank() ? "UNKNOWN" : resolved;
        String clazz = classification == null || classification.isBlank()
                ? "UNATTRIBUTED"
                : classification;
        items.add(new ItemRow(display, id, clazz, catalogMatch, qty));
        if (!catalogMatch) {
            unknowns.add(id.toUpperCase(Locale.ROOT));
        }
    }

    private static String valueOf(String line) {
        int eq = line.indexOf('=');
        if (eq < 0 || eq + 1 >= line.length()) {
            return "";
        }
        return line.substring(eq + 1).trim();
    }

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
