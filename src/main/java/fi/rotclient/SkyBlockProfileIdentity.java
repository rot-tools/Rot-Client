package fi.rotclient;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Testable profile-name extraction from Hypixel's tab/scoreboard widgets. */
public final class SkyBlockProfileIdentity {
    public static final String UNKNOWN = "unknown";
    private static final Pattern FORMATTING = Pattern.compile("§.");
    private static final Pattern PROFILE = Pattern.compile(
            "(?i)(?:^|\\s)profile\\s*:\\s*([\\p{L}\\p{N}_ -]{1,32}?)(?:\\s{2,}|$)");

    private SkyBlockProfileIdentity() {
    }

    public static Optional<String> detect(List<String> lines) {
        return detectRaw(lines).map(SkyBlockProfileIdentity::canonicalize)
                .filter(canonical -> !UNKNOWN.equals(canonical));
    }

    public static Optional<String> detectRaw(List<String> lines) {
        if (lines == null) {
            return Optional.empty();
        }
        for (String line : lines) {
            String plain = FORMATTING.matcher(line == null ? "" : line)
                    .replaceAll("")
                    .strip();
            Matcher matcher = PROFILE.matcher(plain);
            if (matcher.find()) {
                String name = matcher.group(1).strip();
                if (!name.isBlank() && !UNKNOWN.equalsIgnoreCase(name)) {
                    return Optional.of(name);
                }
            }
        }
        return Optional.empty();
    }

    public static String canonicalize(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN;
        }
        String canonical = value.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}_-]+", "_")
                .replaceAll("^_+|_+$", "");
        return canonical.isBlank() ? UNKNOWN : canonical;
    }
}
