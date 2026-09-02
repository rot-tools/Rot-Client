package fi.rotclient;

import java.util.Locale;
import java.util.Optional;

/**
 * Canonical text/stack identity enrichment for tracking diagnostics.
 * Never used as an allow-list — unknowns are preserved with
 * {@code catalogMatch=false}.
 */
final class SkyBlockItemIdentityResolver {
    static final String SCHEMA_VERSION = "tracking-identity-v1";
    static final String UNKNOWN = "UNKNOWN";

    private static final SkyBlockContentRegistry REGISTRY =
            SkyBlockContentRegistry.builtin();
    private static final SkyBlockMiningResourceRegistry MINING_REGISTRY =
            SkyBlockMiningResourceRegistry.bundled();

    private SkyBlockItemIdentityResolver() {
    }

    record Resolved(
            String stableId,
            String displayName,
            String hypixelId,
            String vanillaId,
            String bazaarId,
            boolean catalogMatch,
            String confidence) {
    }

    /**
     * Resolves a display name or Hypixel-style id token from text signals
     * (action bar, sack hover fragment). Unknown tokens are retained.
     */
    static Resolved fromTextToken(String rawToken) {
        String display = rawToken == null ? "" : rawToken.trim();
        if (display.isEmpty()) {
            return new Resolved(
                    UNKNOWN,
                    UNKNOWN,
                    UNKNOWN,
                    "",
                    "",
                    false,
                    "NONE");
        }
        Optional<SkyBlockContentRegistry.KnownItem> known =
                REGISTRY.lookupAlias(display);
        if (known.isPresent()) {
            return fromKnown(known.get(), display);
        }
        Optional<SkyBlockMiningResource> miningKnown =
                MINING_REGISTRY.lookupAlias(display);
        if (miningKnown.isPresent()) {
            return fromMiningKnown(miningKnown.get(), display);
        }
        String normalized = SkyBlockItemId.normalize(display);
        return new Resolved(
                normalized.isEmpty() ? UNKNOWN : normalized,
                display,
                UNKNOWN,
                "",
                "",
                false,
                "LOW");
    }

    static Resolved fromKnownId(String id, String displayFallback) {
        Optional<SkyBlockContentRegistry.KnownItem> known = REGISTRY.lookup(id);
        if (known.isPresent()) {
            return fromKnown(known.get(), displayFallback);
        }
        String normalized = SkyBlockItemId.normalize(id);
        String display = displayFallback == null || displayFallback.isBlank()
                ? (normalized.isEmpty() ? UNKNOWN : normalized)
                : displayFallback.trim();
        return new Resolved(
                normalized.isEmpty() ? UNKNOWN : normalized,
                display,
                UNKNOWN,
                "",
                "",
                false,
                "LOW");
    }

    static Resolved fromMaterialHint(
            String materialId,
            String displayName,
            String vanillaHint,
            String bazaarHint) {
        Resolved base = fromKnownId(materialId, displayName);
        if (base.catalogMatch()) {
            return new Resolved(
                    base.stableId(),
                    base.displayName(),
                    base.hypixelId(),
                    emptyTo(vanillaHint, base.vanillaId()),
                    emptyTo(bazaarHint, base.bazaarId()),
                    true,
                    "HIGH");
        }
        String stable = SkyBlockItemId.normalize(materialId);
        if (stable.isEmpty()) {
            stable = SkyBlockItemId.normalize(displayName);
        }
        if (stable.isEmpty()) {
            stable = UNKNOWN;
        }
        return new Resolved(
                stable,
                displayName == null || displayName.isBlank()
                        ? stable
                        : displayName.trim(),
                UNKNOWN,
                nullToEmpty(vanillaHint),
                nullToEmpty(bazaarHint),
                false,
                "MEDIUM");
    }

    private static Resolved fromKnown(
            SkyBlockContentRegistry.KnownItem item,
            String displayFallback) {
        return new Resolved(
                item.id(),
                item.displayName() == null || item.displayName().isBlank()
                        ? displayFallback
                        : item.displayName(),
                item.hypixelItemId() == null || item.hypixelItemId().isBlank()
                        ? item.id()
                        : item.hypixelItemId(),
                "",
                item.bazaarProductId() == null ? "" : item.bazaarProductId(),
                true,
                "HIGH");
    }

    private static Resolved fromMiningKnown(
            SkyBlockMiningResource resource,
            String displayFallback) {
        String display = resource.displayName() == null
                || resource.displayName().isBlank()
                ? displayFallback
                : resource.displayName();
        return new Resolved(
                resource.canonicalResourceId(),
                display,
                emptyTo(resource.hypixelItemId(),
                        resource.canonicalResourceId()),
                "",
                nullToEmpty(resource.bazaarProductId()),
                true,
                "HIGH");
    }

    private static String emptyTo(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.trim();
        }
        return nullToEmpty(fallback);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static String catalogMatchLabel(boolean match) {
        return match ? "YES" : "NO";
    }

    static String normalizeKey(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
