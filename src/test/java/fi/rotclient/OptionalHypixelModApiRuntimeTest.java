package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class OptionalHypixelModApiRuntimeTest {
    public interface OfficialHandlerShape { void handle(Object packet); }
    public static class OfficialApiShape {
        OfficialHandlerShape handler;
        public void createHandler(Class<?> type, OfficialHandlerShape handler) { this.handler = handler; }
    }
    public static class LegacyApiShape {
        java.util.function.Consumer<Object> handler;
        public void createHandler(Class<?> type, java.util.function.Consumer<Object> handler) { this.handler = handler; }
    }
    @Test void currentInterfaceReceivesPacketAndHasStableIdentity() throws Exception {
        var api = new OfficialApiShape();
        var packets = new java.util.ArrayList<Object>();
        OptionalHypixelModApiRuntime.registerInterfaceHandler(api, Object.class, OfficialHandlerShape.class, packets::add);
        Object packet = new Object();
        api.handler.handle(packet);
        assertEquals(List.of(packet), packets);
        assertTrue(api.handler.equals(api.handler));
        assertEquals(System.identityHashCode(api.handler), api.handler.hashCode());
    }
    @Test void historicalConsumerSignatureStillReceivesPackets() throws Exception {
        var api = new LegacyApiShape();
        var packets = new java.util.ArrayList<Object>();
        OptionalHypixelModApiRuntime.registerPacketHandler(api, Object.class, packets::add);
        api.handler.accept("location");
        assertEquals(List.of("location"), packets);
    }
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
