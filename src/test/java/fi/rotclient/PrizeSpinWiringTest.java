package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class PrizeSpinWiringTest {
    @Test
    void prizeReelStaysOnExistingParentsAndIsWired() throws Exception {
        String catalog = Files.readString(Path.of("src/main/java/fi/rotclient/QolUtilityCatalog.java"));
        String extras = Files.readString(Path.of("src/main/java/fi/rotclient/QolSkyblockExtras.java"));
        String config = Files.readString(Path.of("src/main/java/fi/rotclient/QolUtilityConfig.java"));
        String mixin = Files.readString(
                Path.of("src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"));
        String runtime = Files.readString(Path.of("src/client/java/fi/rotclient/PrizeSpinRuntime.java"));
        assertTrue(catalog.contains("\"qol.dungeon_menus.chest_spin\""));
        assertTrue(catalog.contains("\"qol.chat_commands.slot_machine\""));
        assertTrue(extras.contains("dungeonMenusChestSpin"));
        assertTrue(extras.contains("case \"qol.dungeon_menus.chest_spin\""));
        assertTrue(config.contains("chatSlotMachine"));
        assertTrue(config.contains("case \"qol.chat_commands.slot_machine\""));
        assertTrue(mixin.contains("PrizeSpinRuntime.observe"));
        assertTrue(mixin.contains("PrizeSpinRuntime.renderChest"));
        assertTrue(mixin.contains("PrizeSpinRuntime.swallowClicks"));
        assertTrue(runtime.contains("Does not click slots"));
        assertEquals(131, QolUtilityCatalog.modules().size());
        String lower = (catalog + extras + config + mixin + runtime).toLowerCase();
        assertFalse(lower.contains("skyocean"));
        assertFalse(lower.contains("owdding"));
    }
}
