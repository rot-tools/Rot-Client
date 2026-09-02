package fi.rotclient;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * High-confidence item relationship enrichment from the Rot Client research
 * catalog (gemstone tier upgrades + enchanted forms). Enrichment only —
 * never an observation allow-list. Unknown IDs return empty Optionals.
 */
final class SkyBlockItemRelations {
    private static final SkyBlockItemRelations BUILTIN = new SkyBlockItemRelations(
            builtInEnchantedForms(),
            builtInGemstoneUpgrades());

    private final Map<String, String> enchantedFormByRawId;
    private final Map<String, String> nextGemstoneTier;

    SkyBlockItemRelations(
            Map<String, String> enchantedFormByRawId,
            Map<String, String> nextGemstoneTier) {
        this.enchantedFormByRawId = Collections.unmodifiableMap(
                new LinkedHashMap<>(enchantedFormByRawId));
        this.nextGemstoneTier = Collections.unmodifiableMap(
                new LinkedHashMap<>(nextGemstoneTier));
    }

    static SkyBlockItemRelations builtin() {
        return BUILTIN;
    }

    Optional<String> enchantedFormOf(String rawItemId) {
        String id = SkyBlockItemId.normalize(rawItemId);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(enchantedFormByRawId.get(id));
    }

    Optional<String> nextGemstoneTier(String gemItemId) {
        String id = SkyBlockItemId.normalize(gemItemId);
        if (id.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(nextGemstoneTier.get(id));
    }

    boolean hasKnownEnchantedForm(String rawItemId) {
        return enchantedFormOf(rawItemId).isPresent();
    }

    private static Map<String, String> builtInEnchantedForms() {
        Map<String, String> map = new LinkedHashMap<>();
        // Mining resources — enchantedFormId from research mining-resources.json
        // (HIGH confidence). Null BP/yield fields are intentionally omitted.
        for (SkyBlockMiningResource resource
                : SkyBlockMiningResourceRegistry.bundled().resources()) {
            String enchanted = resource.enchantedItemId();
            if (enchanted == null) continue;
            putAlias(map, resource.canonicalResourceId(), enchanted);
            putAlias(map, resource.collectionId(), enchanted);
            putAlias(map, resource.hypixelItemId(), enchanted);
            putAlias(map, resource.droppedItemId(), enchanted);
        }
        return map;
    }

    private static void putAlias(
            Map<String, String> map, String rawId, String enchantedId) {
        String normalized = SkyBlockItemId.normalize(rawId);
        if (!normalized.isEmpty()) map.put(normalized, enchantedId);
    }

    private static Map<String, String> builtInGemstoneUpgrades() {
        Map<String, String> map = new LinkedHashMap<>();
        String[] gems = {
                "RUBY", "AMBER", "SAPPHIRE", "JADE", "AMETHYST", "TOPAZ",
                "JASPER", "OPAL", "ONYX", "AQUAMARINE", "CITRINE", "PERIDOT"
        };
        String[] tiers = {"ROUGH", "FLAWED", "FINE", "FLAWLESS", "PERFECT"};
        for (String gem : gems) {
            for (int i = 0; i < tiers.length - 1; i++) {
                String from = tiers[i] + "_" + gem + "_GEM";
                String to = tiers[i + 1] + "_" + gem + "_GEM";
                map.put(from, to);
            }
        }
        return map;
    }
}
