package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OptionalHypixelModApiRuntimeTest {
    @Test
    void locationHintsPreferModeAndMapWithoutInventingValues() {
        var packet = new OptionalHypixelModApiRuntime.LocationPacket(
                " mini123 ",
                " mega42 ",
                " mining_3 ",
                " Dwarven Mines ");

        assertEquals(
                List.of("mining_3", "Dwarven Mines", "mega42"),
                OptionalHypixelModApiRuntime.locationHints(packet));
        assertEquals("mini123", packet.serverName());
    }

    @Test
    void absentPacketProducesNoLocationHints() {
        assertTrue(OptionalHypixelModApiRuntime.locationHints(null).isEmpty());
        assertTrue(OptionalHypixelModApiRuntime.locationHints(
                new OptionalHypixelModApiRuntime.LocationPacket(
                        null, null, null, null)).isEmpty());
    }
}
