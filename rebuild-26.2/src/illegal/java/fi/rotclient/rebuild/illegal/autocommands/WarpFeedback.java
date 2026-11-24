// SPDX-License-Identifier: MIT
// AutoCommands 2.2.0, copyright 2026 Pickaxe Popup contributors.
package fi.rotclient.rebuild.illegal.autocommands;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class WarpFeedback {
    private static final Pattern FORMATTING = Pattern.compile("(?i)\u00a7[0-9a-fk-orx]");
    private static final Pattern SPACES = Pattern.compile("[\\s\\p{Zs}]+");
    private static final Pattern AREA = Pattern.compile("(?i)^(?:[\u23e3\u0444\\p{Co}] +|Location: *)(.+)$");
    private static final Pattern ISLAND = Pattern.compile("(?i)^Area: *(.+)$");
    private static final Pattern SERVER = Pattern.compile("(?i)^\\d{1,2}/\\d{1,2}/\\d{2,4} +((?:mini|mega|m)\\d+[a-z0-9]*)$");
    private static final Set<String> PENDING_NAMES = Set.of("none", "unknown", "n/a", "loading", "loading...", "loading\u2026");
    private static final Pattern WARP_BLOCKED = Pattern.compile(
            "^(?:you (?:can(?:not|'t)|could(?: not|n't)|may not|are unable to) (?:warp|fast travel|travel|visit|join)\\b"
                    + "|(?:could(?: not|n't)|can(?:not|'t)|failed to|unable to) (?:warp|fast travel|travel|visit|connect|send you)\\b"
                    + "|(?:this|that) (?:warp|(?:fast travel )?destination) (?:is|has) (?:locked|unavailable|disabled|not|been disabled)\\b"
                    + "|(?:invalid|unknown) (?:warp|(?:fast travel )?destination)\\b"
                    + "|(?:warp|teleport|fast travel) (?:failed|cancelled|canceled|is on cooldown)\\b"
                    + "|you (?:must wait|may only|can only) [^.!?]*(?:warp|fast travel)\\b"
                    + "|you(?: are|'re) already connected to (?:that|this) server\\b"
                    + "|you were kicked while joining that server\\b|exception connecting\\b)");

    private WarpFeedback() { }

    static String area(String row) { return location(AREA, row); }
    static String island(String row) { return location(ISLAND, row); }
    static String server(String row) { return value(SERVER, row); }

    private static String location(Pattern pattern, String row) {
        String name = value(pattern, row);
        // SkyBlock can publish a placeholder sidebar or tab row while the new area loads.
        return name == null || PENDING_NAMES.contains(name.toLowerCase(Locale.ROOT)) ? null : name;
    }

    private static String value(Pattern pattern, String row) {
        var match = pattern.matcher(clean(row));
        if (!match.matches()) return null;
        String value = match.group(1).strip();
        return value.length() <= 90 && value.codePoints().anyMatch(Character::isLetter) ? value : null;
    }

    static String failure(String raw) {
        String line = clean(raw);
        String lowered = line.replace('\u2019', '\'').toLowerCase(Locale.ROOT);
        if (lowered.startsWith("you haven't unlocked this fast travel destination")
                || lowered.startsWith("you have not unlocked this fast travel destination")) return "Destination is locked";
        if (lowered.startsWith("unknown or incomplete command") || lowered.startsWith("unknown command")) return "Unknown warp command";
        if (lowered.equals("there is no escape!") || WARP_BLOCKED.matcher(lowered).find()) {
            return line.length() <= 94 ? line : "The server rejected the warp";
        }
        return null;
    }

    static String clean(String raw) {
        return SPACES.matcher(FORMATTING.matcher(raw).replaceAll("").replaceAll("\\p{Cf}", "")).replaceAll(" ").strip();
    }
}
