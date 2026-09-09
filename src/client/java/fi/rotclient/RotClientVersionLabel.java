package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;

import java.util.Optional;

/**
 * Authoritative user-facing Rot Client version labels for HUD and
 * dashboard surfaces. Prefer Fabric mod metadata; never hardcode a release
 * number into UI strings.
 */
final class RotClientVersionLabel {
    static final String MOD_ID = "rotclient";
    static final String FALLBACK_VERSION = "unavailable";

    private static volatile String cachedResolvedVersion;

    private RotClientVersionLabel() {
    }

    static String resolveInstalledVersion() {
        String cached = cachedResolvedVersion;
        if (cached != null) {
            return cached;
        }
        String resolved = sanitizeVersion(readFabricVersion().orElse(null));
        cachedResolvedVersion = resolved;
        return resolved;
    }

    /** Example: {@code Rot Client v2.0.0+mc26.2} */
    static String brandLabel() {
        return brandLabel(resolveInstalledVersion());
    }

    static String brandLabel(String version) {
        return QolFlavorSupport.productName() + " v" + sanitizeVersion(version);
    }

    /** Example: {@code v1.10.0+mc26.2} */
    static String shortLabel() {
        return shortLabel(resolveInstalledVersion());
    }

    static String shortLabel(String version) {
        return "v" + sanitizeVersion(version);
    }

    static String gemstoneHudFooter() {
        return brandLabel() + " | GEMSTONE";
    }

    static String gemstoneHudFooter(String version) {
        return brandLabel(version) + " | GEMSTONE";
    }

    static Optional<String> readFabricVersion() {
        try {
            Optional<String> version = FabricLoader.getInstance()
                    .getModContainer(QolFlavorSupport.modId())
                    .map(container -> container.getMetadata()
                            .getVersion()
                            .getFriendlyString())
                    .map(RotClientVersionLabel::trimToNull);
            if (version.isPresent()) {
                return version;
            }
            return FabricLoader.getInstance()
                    .getModContainer(MOD_ID)
                    .map(container -> container.getMetadata()
                            .getVersion()
                            .getFriendlyString())
                    .map(RotClientVersionLabel::trimToNull);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    static String sanitizeVersion(String version) {
        String trimmed = trimToNull(version);
        if (trimmed == null) {
            return FALLBACK_VERSION;
        }
        StringBuilder result = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch < 0x20 || ch == 0x7F) {
                continue;
            }
            result.append(ch);
        }
        String sanitized = result.toString().trim();
        if (sanitized.isEmpty()) {
            return FALLBACK_VERSION;
        }
        if (sanitized.length() > 64) {
            return sanitized.substring(0, 64);
        }
        return sanitized;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
