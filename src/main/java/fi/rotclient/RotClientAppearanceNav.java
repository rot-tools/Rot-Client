package fi.rotclient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Pure helpers for Appearance color-group accordion IDs. Multi-expand;
 * unknown future IDs are ignored on normalize.
 */
final class RotClientAppearanceNav {
    static final String DASHBOARD_BASICS = "appearance.dashboard_basics";
    static final String DASHBOARD_TEXT = "appearance.dashboard_text";
    static final String HUD_BASICS = "appearance.hud_basics";
    static final String HUD_TEXT = "appearance.hud_text";
    static final String BUTTONS_BORDERS = "appearance.buttons_borders";

    private RotClientAppearanceNav() {
    }

    static List<String> defaultExpandedSections() {
        return List.of(
                DASHBOARD_BASICS,
                DASHBOARD_TEXT,
                HUD_BASICS,
                HUD_TEXT,
                BUTTONS_BORDERS);
    }

    static List<String> normalizeExpandedSections(Collection<String> raw) {
        if (raw == null) {
            return new ArrayList<>(defaultExpandedSections());
        }
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String id : raw) {
            String normalized = normalizeSectionId(id);
            if (normalized != null) {
                cleaned.add(normalized);
            }
        }
        if (raw.isEmpty()) {
            return new ArrayList<>();
        }
        if (cleaned.isEmpty()) {
            return new ArrayList<>(defaultExpandedSections());
        }
        return new ArrayList<>(cleaned);
    }

    static String normalizeSectionId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String id = raw.trim().toLowerCase(Locale.ROOT);
        return switch (id) {
            case DASHBOARD_BASICS, DASHBOARD_TEXT, HUD_BASICS, HUD_TEXT,
                    BUTTONS_BORDERS -> id;
            default -> null;
        };
    }

    static boolean isExpanded(Collection<String> expanded, String sectionId) {
        String id = normalizeSectionId(sectionId);
        if (id == null) {
            return false;
        }
        if (expanded == null) {
            return defaultExpandedSections().contains(id);
        }
        for (String candidate : expanded) {
            if (id.equals(normalizeSectionId(candidate))) {
                return true;
            }
        }
        return false;
    }

    static List<String> toggleSection(Collection<String> expanded, String sectionId) {
        String id = normalizeSectionId(sectionId);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (expanded == null) {
            set.addAll(defaultExpandedSections());
        } else {
            for (String candidate : expanded) {
                String normalized = normalizeSectionId(candidate);
                if (normalized != null) {
                    set.add(normalized);
                }
            }
        }
        if (id == null) {
            return new ArrayList<>(set);
        }
        if (!set.add(id)) {
            set.remove(id);
        }
        return new ArrayList<>(set);
    }
}
