package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RotClientEventBusTest {
    @Test
    void publishesOnlyToExactTypedSubscribersAndUnsubscribes() {
        RotClientEventBus bus = new RotClientEventBus();
        List<String> messages = new ArrayList<>();
        RotClientEventBus.Subscription subscription = bus.subscribe(
                RotClientDomainEvents.TextObserved.class,
                event -> messages.add(event.plainText()));
        bus.subscribe(
                RotClientDomainEvents.LocationChanged.class,
                event -> messages.add(event.parentAreaId()));

        assertEquals(1, bus.publish(new RotClientDomainEvents.TextObserved(
                RotClientDomainEvents.Source.FABRIC_MESSAGE,
                RotClientDomainEvents.TextKind.GAME,
                "hello",
                10L)));
        assertEquals(List.of("hello"), messages);

        subscription.close();
        assertEquals(0, bus.publish(new RotClientDomainEvents.TextObserved(
                null, null, "ignored", -1L)));
        assertEquals(0, bus.listenerCount(
                RotClientDomainEvents.TextObserved.class));
    }

    @Test
    void oneBrokenSubscriberDoesNotBlockTheOthers() {
        AtomicInteger errors = new AtomicInteger();
        AtomicInteger delivered = new AtomicInteger();
        RotClientEventBus bus = new RotClientEventBus(
                (event, error) -> errors.incrementAndGet());
        bus.subscribe(RotClientDomainEvents.DataReloaded.class, event -> {
            throw new IllegalStateException("broken feature");
        });
        bus.subscribe(
                RotClientDomainEvents.DataReloaded.class,
                event -> delivered.incrementAndGet());

        assertEquals(1, bus.publish(new RotClientDomainEvents.DataReloaded(
                null, "items", 1L, 2L)));
        assertEquals(1, errors.get());
        assertEquals(1, delivered.get());
    }

    @Test
    void eventRecordsNormalizeUntrustedBridgeValues() {
        RotClientDomainEvents.ScreenOpened event =
                new RotClientDomainEvents.ScreenOpened(
                        null, "  InventoryScreen  ", -2, -3, -4L);

        assertEquals(RotClientDomainEvents.Source.FABRIC_SCREEN, event.source());
        assertEquals("InventoryScreen", event.screenClass());
        assertEquals(0, event.width());
        assertEquals(0, event.height());
        assertEquals(0L, event.observedAtMillis());
    }
}
