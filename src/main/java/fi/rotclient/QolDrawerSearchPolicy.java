package fi.rotclient;

import java.util.*;

/** Search preserves section context and setting order; a hit never changes a setting value. */
public final class QolDrawerSearchPolicy {
    public static List<QolUtilityCatalog.SettingDef> filter(List<QolUtilityCatalog.SettingDef> settings, String query) {
        if (settings == null) return List.of();
        String normalized = normalize(query);
        if (normalized.isBlank()) return List.copyOf(settings);
        String[] terms = normalized.split("\\s+");
        var result = new ArrayList<QolUtilityCatalog.SettingDef>();
        QolUtilityCatalog.SettingDef section = null;
        boolean emitted = false;
        String sectionText = "";
        for (var setting : settings) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) {
                section = setting;
                sectionText = searchableText(setting);
                emitted = false;
                continue;
            }
            String text = searchableText(setting) + " " + sectionText;
            if (!matchesAllTerms(text, terms)) continue;
            if (section != null && !emitted) { result.add(section); emitted = true; }
            result.add(setting);
        }
        return List.copyOf(result);
    }

    private static String searchableText(QolUtilityCatalog.SettingDef setting) {
        return normalize(setting.id() + " " + setting.label() + " " + setting.description()
                + " " + String.join(" ", setting.searchAliases())
                + " " + String.join(" ", setting.enumOptions()));
    }

    private static boolean matchesAllTerms(String text, String[] terms) {
        for (String term : terms) if (!text.contains(term)) return false;
        return true;
    }
    private static String normalize(String text) {
        return java.text.Normalizer.normalize(text == null ? "" : text,java.text.Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replace('_',' ').replace('.',' ').replace('-',' ').trim();
    }
    private QolDrawerSearchPolicy() {}
}
