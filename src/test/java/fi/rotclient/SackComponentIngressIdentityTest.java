package fi.rotclient;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class SackComponentIngressIdentityTest {
    @Test
    void repeatedDeliveryOfSameComponentKeepsIdentity() {
        Component message = Component.literal("[Sacks] +10 items (Last 9s.)");

        SackComponentIngress.Batch first =
                SackComponentIngress.parse(message).orElseThrow();
        SackComponentIngress.Batch repeated =
                SackComponentIngress.parse(message).orElseThrow();

        assertEquals(first.deliveryIdentity(), repeated.deliveryIdentity());
        assertEquals(9_000L, first.coveredBatchMillis());
    }

    @Test
    void separateEqualComponentsReceiveSeparateOccurrenceIdentities() {
        SackComponentIngress.Batch first = SackComponentIngress.parse(
                Component.literal("[Sacks] +10 items (Last 9s.)")).orElseThrow();
        SackComponentIngress.Batch second = SackComponentIngress.parse(
                Component.literal("[Sacks] +10 items (Last 9s.)")).orElseThrow();

        assertNotEquals(first.deliveryIdentity(), second.deliveryIdentity());
    }
}
