package fi.rotclient;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PristineComponentIngressIdentityTest {
    @Test
    void repeatedDeliveryOfSameComponentKeepsIdentity() {
        Component message = Component.literal(
                "PRISTINE! You found Flawed Topaz Gemstone x13!");

        PristineComponentIngress.Parsed first =
                PristineComponentIngress.parse(message).orElseThrow();
        PristineComponentIngress.Parsed repeated =
                PristineComponentIngress.parse(message).orElseThrow();

        assertEquals(first.deliveryIdentity(), repeated.deliveryIdentity());
        assertEquals(GemstoneType.TOPAZ, first.reward().gemstone());
        assertEquals(13L, first.reward().flawedAmount());
        assertTrue(first.deliveryIdentity().startsWith("pristine-component:"));
    }

    @Test
    void separateEqualComponentsReceiveSeparateOccurrenceIdentities() {
        PristineComponentIngress.Parsed first = PristineComponentIngress.parse(
                Component.literal(
                        "PRISTINE! You found Flawed Topaz Gemstone x13!"))
                .orElseThrow();
        PristineComponentIngress.Parsed second = PristineComponentIngress.parse(
                Component.literal(
                        "PRISTINE! You found Flawed Topaz Gemstone x13!"))
                .orElseThrow();

        assertNotEquals(first.deliveryIdentity(), second.deliveryIdentity());
        assertEquals(first.reward().flawedAmount(), second.reward().flawedAmount());
    }
}
