package fi.rotclient;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/** Static public landmarks, not an entity or hidden-block scanner. See docs/MINING_HUD_FIXES.md. */
final class DwarvenWaypointPolicy {
    record Landmark(String name, int x, int y, int z) {}
    static final List<Landmark> LANDMARKS = List.of(
            new Landmark("Dwarven Village", -37, 199, -122),
            new Landmark("Miner's Guild", -74, 220, -122),
            new Landmark("Fetchur", 85, 223, -120),
            new Landmark("Palace Bridge", 129, 186, 8),
            new Landmark("Royal Palace", 129, 194, 194),
            new Landmark("Puzzler", 181, 195, 135),
            new Landmark("Grand Library", 183, 195, 181),
            new Landmark("Barracks of Heroes", 93, 195, 181),
            new Landmark("Royal Mines", 178, 149, 71),
            new Landmark("Cliffside Veins", 40, 136, 17),
            new Landmark("Forge Basin", 0, 169, -2),
            new Landmark("The Forge", 0, 148, -69),
            new Landmark("Rampart's Quarry", -106, 147, 2),
            new Landmark("Far Reserve", -160, 148, 17),
            new Landmark("Upper Mines", -123, 170, -71),
            new Landmark("Goblin Burrows", -138, 143, 141),
            new Landmark("Great Ice Wall", 0, 127, 160),
            new Landmark("Aristocrat Passage", 129, 150, 137),
            new Landmark("Hanging Court", 91, 186, 129),
            new Landmark("Divan's Gateway", 0, 127, 87),
            new Landmark("Lava Springs", 57, 196, -15),
            new Landmark("The Mist", 0, 75, 82));

    /** Original selection over Rot's existing public coordinates; no upstream implementation copied.
     * Behavior comparison: Skyblocker CommissionLabels, f5cc8799 (see follow-up audit).
     * Generic mining tasks have no unique destination and deliberately receive no guessed label. */
    static List<Landmark> commissionDestinations(List<CommissionDisplayPolicy.Commission> commissions) {
        if (commissions == null || commissions.isEmpty()) return List.of();
        List<Landmark> destinations = new ArrayList<>();
        for (Landmark landmark : LANDMARKS) {
            String location = normalize(landmark.name());
            for (CommissionDisplayPolicy.Commission commission : commissions) {
                if (commission == null || commission.done() || commission.progressPercent() >= 100) continue;
                String task = normalize(commission.name());
                if (task.equals(location) || task.startsWith(location + " ")
                        || landmark.name().equals("Goblin Burrows") && task.equals("goblin slayer")) {
                    destinations.add(landmark);
                    break;
                }
            }
        }
        return List.copyOf(destinations);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace('’', '\'').trim().toLowerCase(Locale.ROOT);
    }
    private DwarvenWaypointPolicy() {}
}
