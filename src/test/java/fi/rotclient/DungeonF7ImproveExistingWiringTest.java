package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonF7ImproveExistingWiringTest {
    @Test
    void solverSoundsDragonsAndRelicsStayOnExistingParents() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/DungeonRuntime.java"), StandardCharsets.UTF_8);
        assertTrue(runtime.contains("melodyLeapName = name"));
        assertTrue(runtime.contains("shouldHideTerminalTooltip"));
        assertTrue(runtime.contains("shouldHideTerminalSlot"));
        assertTrue(runtime.contains("shouldCancelTerminalSlot"));
        assertTrue(runtime.contains("shouldCancelEntityUse"));
        assertTrue(runtime.contains("dungeonF7DragonTracers"));
        assertTrue(runtime.contains("dungeonF7RelicBeacon"));
        assertTrue(runtime.contains("relicSpawnMillis"));
        assertTrue(runtime.contains("formatCountdown"));
        assertTrue(runtime.contains("DungeonF7Policy.observeSimon"));
        assertTrue(runtime.contains("if (!title.equals(lastTermTitle))"));
        assertTrue(runtime.contains("simonButtonForLantern"));
        assertTrue(!runtime.contains("qol.dungeon_terminals.first_click_delay"));

        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/AbstractContainerScreenInventoryOverlayMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixin.contains("DungeonRuntime.shouldHideTerminalTooltip"));
        assertTrue(mixin.contains("QolClientFlavorSupport.hooks().shouldCancelTerminalSlot"));
        int detect = mixin.indexOf("DungeonPolicy.detectTerminal(");
        int enqueue = mixin.indexOf("DungeonRuntime.enqueueTerminalClick(");
        assertTrue(detect >= 0);
        assertTrue(enqueue > detect);

        String gameMode = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/MultiPlayerGameModeMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(gameMode.contains("onBlockUsed"));
        String plusGameMode = Files.readString(Path.of(
                "src/plusClient/java/fi/rotclient/mixin/MultiPlayerGameModePlusMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(plusGameMode.contains("shouldCancelEntityUse"));
        assertTrue(plusGameMode.contains("shouldCancelBlockUse"));
    }
}
