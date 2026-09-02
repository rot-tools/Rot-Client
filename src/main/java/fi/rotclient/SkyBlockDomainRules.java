package fi.rotclient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Handwritten Rot Client domain rules. Separate from generated upstream
 * metadata. Domain stable IDs may differ from official Hypixel item IDs.
 */
final class SkyBlockDomainRules {
    record Rule(
            String stableId,
            String displayName,
            List<String> aliases,
            String hypixelItemIdHint,
            String bazaarProductIdHint,
            List<String> areas,
            String sourceHint,
            List<String> notes) {
        Rule {
            stableId = SkyBlockItemId.normalize(stableId);
            if (stableId.isEmpty()) {
                throw new IllegalArgumentException("stableId cannot be blank");
            }
            displayName = displayName == null || displayName.isBlank()
                    ? stableId
                    : displayName.trim();
            aliases = aliases == null ? List.of() : List.copyOf(aliases);
            hypixelItemIdHint = blankToNullId(hypixelItemIdHint);
            bazaarProductIdHint = blankToNullId(bazaarProductIdHint);
            areas = areas == null ? List.of() : List.copyOf(areas);
            sourceHint = sourceHint == null || sourceHint.isBlank()
                    ? null
                    : sourceHint.trim().toUpperCase(Locale.ROOT);
            notes = notes == null ? List.of() : List.copyOf(notes);
        }

        private static String blankToNullId(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            String normalized = SkyBlockItemId.normalize(value);
            return normalized.isEmpty() ? null : normalized;
        }
    }

    private final String schemaVersion;
    private final List<Rule> rules;

    SkyBlockDomainRules(String schemaVersion, List<Rule> rules) {
        this.schemaVersion = schemaVersion == null || schemaVersion.isBlank()
                ? "domain-rules.v1"
                : schemaVersion.trim();
        if (rules == null) {
            throw new IllegalArgumentException("rules cannot be null");
        }
        this.rules = List.copyOf(rules);
    }

    String schemaVersion() {
        return schemaVersion;
    }

    List<Rule> rules() {
        return rules;
    }

    /**
     * Seeds domain rules from the current handwritten registry builtins.
     * Explicit official/Bazaar hints encode known dual-key cases.
     */
    static SkyBlockDomainRules fromBuiltinRegistry() {
        List<Rule> rules = new ArrayList<>();
        for (SkyBlockContentRegistry.KnownItem item :
                SkyBlockContentRegistry.builtinKnownItems()) {
            String hypixelHint = item.id();
            String bazaarHint = item.bazaarProductId();
            List<String> notes = new ArrayList<>();
            // Official Hypixel item/Bazaar product for Titanium is TITANIUM_ORE.
            // Domain routing key remains TITANIUM (TrackedMaterial / selection).
            if ("TITANIUM".equals(item.id())) {
                hypixelHint = "TITANIUM_ORE";
                bazaarHint = "TITANIUM_ORE";
                notes.add(
                        "Domain stableId TITANIUM; official Hypixel/Bazaar id TITANIUM_ORE");
            } else if ("LAPIS_LAZULI".equals(item.id())) {
                hypixelHint = "INK_SACK:4";
                bazaarHint = "INK_SACK:4";
                notes.add(
                        "Domain stableId LAPIS_LAZULI; official Hypixel/Bazaar id INK_SACK:4");
            } else if (bazaarHint == null) {
                notes.add("No Bazaar product (currency or non-market item)");
            }
            // Builtin registry historically stored TITANIUM as bazaar id; prefer
            // the official product id for pricing identity.
            if ("TITANIUM".equals(item.bazaarProductId())) {
                bazaarHint = "TITANIUM_ORE";
            }
            rules.add(new Rule(
                    item.id(),
                    item.displayName(),
                    item.aliases(),
                    hypixelHint,
                    bazaarHint,
                    item.areas(),
                    item.sourceHint(),
                    notes));
        }
        return new SkyBlockDomainRules("domain-rules.v1", rules);
    }
}
