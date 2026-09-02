package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SlayerCarryPolicyTest {
    @Test void parsesCarryTradeAndInfersConfiguredPrice() {
        assertEquals("CarryUser", SlayerCarryPolicy.tradePlayer(
                "Trade completed with [VIP] CarryUser!").orElseThrow());
        double amount = SlayerCarryPolicy.receivedMillions("+ 2.6M coins").orElseThrow();
        assertTrue(SlayerCarryPolicy.infer(amount, "0.8", "1.3", "2", "3.5", "7")
                .stream().anyMatch(m -> m.type() == SlayerPolicy.SlayerType.VOIDGLOOM
                        && m.tier() == 4 && m.count() == 2));
    }

    @Test void webhookAllowsOnlyDiscordHttpsEndpoint() {
        assertEquals("", SlayerCarryPolicy.sanitizeWebhookUrl("http://discord.com/api/webhooks/1/a"));
        assertEquals("", SlayerCarryPolicy.sanitizeWebhookUrl("https://evil.example/api/webhooks/1/a"));
        assertEquals("", SlayerCarryPolicy.sanitizeWebhookUrl("https://discord.com/api/webhooks/"));
        assertEquals("https://discord.com/api/webhooks/1/a",
                SlayerCarryPolicy.sanitizeWebhookUrl("https://discord.com/api/webhooks/1/a"));
    }

    @Test void usesRngMeterFormula() {
        assertEquals(2.0D, SlayerCarryPolicy.rngChancePercent(500, 1000, 1.0D, 0), 0.0001D);
        assertEquals(4.0D, SlayerCarryPolicy.rngChancePercent(500, 1000, 1.0D, 100), 0.0001D);
        assertEquals(100.0D, SlayerCarryPolicy.rngChancePercent(1000, 1000, 0.01D, 0), 0.0001D);
    }
}
