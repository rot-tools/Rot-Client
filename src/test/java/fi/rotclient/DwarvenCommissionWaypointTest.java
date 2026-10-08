package fi.rotclient;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DwarvenCommissionWaypointTest {
    @Test void mapsOnlyIncompleteExplicitLocationsAndDeduplicates() {
        var rows = List.of(
                new CommissionDisplayPolicy.Commission("Rampart’s Quarry Titanium", 30, false),
                new CommissionDisplayPolicy.Commission("Rampart's Quarry Mithril", 50, false),
                new CommissionDisplayPolicy.Commission("Royal Mines Mithril", 100, false),
                new CommissionDisplayPolicy.Commission("Upper Mines Mithril", 0, true));
        assertEquals(List.of("Rampart's Quarry"), DwarvenWaypointPolicy.commissionDestinations(rows)
                .stream().map(DwarvenWaypointPolicy.Landmark::name).toList());
    }

    @Test void globalTasksUnknownRegionsAndNarrativeDoNotInventDestinations() {
        var rows = List.of(
                new CommissionDisplayPolicy.Commission("Mithril Miner", 10, false),
                new CommissionDisplayPolicy.Commission("Aquamarine Collector", 10, false),
                new CommissionDisplayPolicy.Commission("Not Upper Mines Mithril", 10, false));
        assertTrue(DwarvenWaypointPolicy.commissionDestinations(rows).isEmpty());
        assertTrue(DwarvenWaypointPolicy.commissionDestinations(null).isEmpty());
    }

    @Test void completedAndReplacedTasksLoseTheirDestination() {
        assertEquals("Goblin Burrows", DwarvenWaypointPolicy.commissionDestinations(List.of(
                new CommissionDisplayPolicy.Commission("Goblin Slayer", 25, false))).getFirst().name());
        assertTrue(DwarvenWaypointPolicy.commissionDestinations(List.of(
                new CommissionDisplayPolicy.Commission("Goblin Slayer", 100, true))).isEmpty());
        assertTrue(DwarvenWaypointPolicy.commissionDestinations(List.of()).isEmpty());
    }

    @Test void settingIsOptInWritableAndResetsWithItsParent() {
        QolUtilityConfig config = new QolUtilityConfig();
        assertFalse(config.readBoolean("qol.mining_helpers.commission_waypoints"));
        config.writeBoolean("qol.mining_helpers.commission_waypoints", true);
        assertTrue(config.readBoolean("qol.mining_helpers.commission_waypoints"));
        config.resetModuleToDefaults("qol.mining_helpers");
        assertFalse(config.readBoolean("qol.mining_helpers.commission_waypoints"));
    }
}
