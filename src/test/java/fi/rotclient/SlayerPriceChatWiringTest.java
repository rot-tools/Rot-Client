package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerPriceChatWiringTest {
    @Test
    void rareDropAlertsAreOptInAndRequireALiveQuote() throws Exception {
        String source = Files.readString(Path.of("src/client/java/fi/rotclient/SlayerRuntime.java"));

        assertTrue(source.contains("announcePricedDrop(entry, settings)"));
        assertTrue(source.contains("(!settings.slayerDropsPriceInChat && !settings.slayerDropsPriceTitle)"));
        assertTrue(source.contains("BigDecimal price = slayerUnitPrices().get(entry.skyBlockId());"));
        assertTrue(source.contains("price == null || price.signum() <= 0 || client.player == null"));
        assertTrue(source.contains("price.compareTo(BigDecimal.valueOf(settings.slayerDropsPriceTitleMinimum)) >= 0"));
    }
}
