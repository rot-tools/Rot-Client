package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Mob highlight: match armor-stand / entity names from a local list. */
public final class MobHighlightPolicy {
    private MobHighlightPolicy() {
    }

    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        return ChatTextPolicy.stripFormatting(raw)
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    public static boolean matches(String entityName, List<String> catalog) {
        String needle = normalizeName(entityName);
        if (needle.isEmpty() || catalog == null) {
            return false;
        }
        for (String entry : catalog) {
            String hay = normalizeName(entry);
            if (hay.isEmpty()) {
                continue;
            }
            if (needle.equals(hay) || needle.contains(hay) || hay.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public static List<String> addName(List<String> catalog, String name) {
        String normalized = name == null ? "" : name.trim();
        if (normalized.isBlank()) {
            return catalog == null ? List.of() : List.copyOf(catalog);
        }
        List<String> out = new ArrayList<>();
        if (catalog != null) {
            out.addAll(catalog);
        }
        for (String existing : out) {
            if (normalizeName(existing).equals(normalizeName(normalized))) {
                return List.copyOf(out);
            }
        }
        out.add(normalized);
        return List.copyOf(out);
    }

    public static List<String> removeName(List<String> catalog, String name) {
        if (catalog == null || catalog.isEmpty()) {
            return List.of();
        }
        String needle = normalizeName(name);
        List<String> out = new ArrayList<>();
        for (String existing : catalog) {
            if (!normalizeName(existing).equals(needle)) {
                out.add(existing);
            }
        }
        return List.copyOf(out);
    }
}
