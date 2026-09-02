package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

/** Pure duplicate-feature and high-risk mixin compatibility decisions. */
public final class ModCompatibilityPolicy {
    private static final Set<String> SKYBLOCK_OVERLAP_MODS = Set.of(
            "athen",
            "devonian",
            "nofrills",
            "sbo",
            "skyblockaddons",
            "skyblocker",
            "skyhanni",
            "skyocean");

    private ModCompatibilityPolicy() {
    }

    public static boolean shouldApplyMixin(
            String mixinClassName,
            Set<String> loadedModIds) {
        String mixin = mixinClassName == null ? "" : mixinClassName;
        Set<String> mods = normalize(loadedModIds);
        if (mixin.endsWith("DynamicFpsCheckForRenderMixin")) {
            return mods.contains("dynamic_fps");
        }
        if (mods.contains("skyocean")
                && (mixin.endsWith("LivingEntityPlayerAnimalsMixin")
                || mixin.endsWith("ItemModelResolverAnimationMixin"))) {
            return false;
        }
        return true;
    }

    public static List<String> overlappingSkyBlockMods(
            Set<String> loadedModIds) {
        Set<String> normalized = normalize(loadedModIds);
        List<String> overlaps = new ArrayList<>();
        for (String id : new TreeSet<>(SKYBLOCK_OVERLAP_MODS)) {
            if (normalized.contains(id)) {
                overlaps.add(id);
            }
        }
        return List.copyOf(overlaps);
    }

    public static String warning(List<String> overlaps) {
        if (overlaps == null || overlaps.isEmpty()) {
            return "";
        }
        return "Overlapping SkyBlock mods detected: "
                + String.join(", ", overlaps)
                + ". Review duplicate feature toggles; high-risk exact mixin "
                + "collisions are disabled automatically.";
    }

    private static Set<String> normalize(Set<String> ids) {
        Set<String> result = new TreeSet<>();
        if (ids == null) {
            return result;
        }
        for (String id : ids) {
            if (id != null && !id.isBlank()) {
                result.add(id.strip().toLowerCase(Locale.ROOT));
            }
        }
        return result;
    }
}
