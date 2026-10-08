package fi.rotclient;

import java.util.*;

/** Packet coordinates are signed half-pixels around the map centre, not unsigned map pixels. */
final class DungeonMapObservationPolicy {
    static int pixel(int encoded) {
        return encoded < -128 || encoded > 127 ? -1 : Math.floorDiv(encoded + 128, 2);
    }
    static List<String> livingRoster(List<String> rosterLines, Collection<String> roster, String self) {
        var living = new LinkedHashSet<String>();
        for (String name : roster) {
            if (name == null || name.isBlank() || DungeonMapPolicy.samePlayerName(name,self)) continue;
            boolean dead = rosterLines.stream().map(CommissionDisplayPolicy::normalizeLine).anyMatch(line ->
                    line.contains("(DEAD)") && java.util.regex.Pattern.compile("(?i)(?<![A-Z0-9_])"
                            + java.util.regex.Pattern.quote(name) + "(?![A-Z0-9_])").matcher(line).find());
            if (!dead) living.add(name);
        }
        return List.copyOf(living);
    }
    static List<DungeonMapPolicy.MapDecorationHint> knownMarkers(
            List<DungeonMapPolicy.MapDecorationHint> hints, List<String> living) {
        // Named markers carry their own evidence. Do not label dead/foreign players.
        return hints.stream().filter(h -> h.selfMarker() || h.name().isBlank()
                || living.stream().anyMatch(n -> DungeonMapPolicy.samePlayerName(n, h.name())))
                .toList();
    }
    static List<String> unambiguousOrder(List<DungeonMapPolicy.MapDecorationHint> hints, List<String> living) {
        var remaining = living.stream().filter(n -> hints.stream().noneMatch(h ->
                !h.name().isBlank() && DungeonMapPolicy.samePlayerName(n, h.name()))).toList();
        long unnamed = hints.stream().filter(h -> !h.selfMarker() && h.name().isBlank()).count();
        return unnamed == remaining.size() ? living : List.of();
    }
    static boolean buffer(byte[] colors) { return colors != null && colors.length == 128 * 128; }
    private DungeonMapObservationPolicy() {}
}
