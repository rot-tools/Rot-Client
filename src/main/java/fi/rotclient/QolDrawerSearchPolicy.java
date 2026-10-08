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
        for (var setting : settings) {
            if (setting.type() == QolUtilityCatalog.SettingType.SECTION) { section = setting; emitted = false; continue; }
            String text = normalize(setting.id() + " " + setting.label() + " " + setting.description()
                    + " " + String.join(" ",setting.searchAliases()) + " " + String.join(" ",setting.enumOptions())
                    + (section == null ? "" : " " + section.label() + " " + section.description()));
            if (!Arrays.stream(terms).allMatch(text::contains)) continue;
            if (section != null && !emitted) { result.add(section); emitted = true; }
            result.add(setting);
        }
        return List.copyOf(result);
    }
    private static String normalize(String text) {
        return java.text.Normalizer.normalize(text == null ? "" : text,java.text.Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replace('_',' ').replace('.',' ').trim();
    }
    private QolDrawerSearchPolicy() {}
}
