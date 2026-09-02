package fi.rotclient;

import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FortuneDetectorTest {
    @Test
    void parsesAllFourResourceSpecificFortuneCategories() {
        TrackerConfig config = new TrackerConfig();
        FortuneDetector detector = new FortuneDetector(config);

        detector.inspectMessage(Component.literal(
                "Mining Fortune: 1,000  Block Fortune: 200  "
                        + "Ore Fortune: 300  Dwarven Metal Fortune: 400  "
                        + "Gemstone Fortune: 500"));

        assertEquals(1_000.0, config.miningFortune);
        assertEquals(200.0, config.blockFortune);
        assertEquals(300.0, config.oreFortune);
        assertEquals(400.0, config.dwarvenMetalFortune);
        assertEquals(500.0, config.gemstoneFortune);
    }
}
