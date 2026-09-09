package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonGoldorWiringTest {
    @Test
    void sharpShooterTermTimesAndCalloutsStayOnExistingParents() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/DungeonRuntime.java"), StandardCharsets.UTF_8);
        assertTrue(runtime.contains("DungeonGoldorPolicy.observeBlock"));
        assertTrue(runtime.contains("DungeonGoldorPolicy.observeWorld"));
        assertTrue(runtime.contains("DungeonGoldorPolicy.applyChat"));
        assertTrue(runtime.contains("dungeonF7SharpShooter"));
        assertTrue(runtime.contains("dungeonF7TermTimes"));
        assertTrue(runtime.contains("dungeonAnnouncePosition"));
        assertTrue(runtime.contains("dungeonHudLedge"));
        assertTrue(runtime.contains("resetTerminalPersonalBests"));
        assertTrue(runtime.contains("paintMaskOverlay"));
        assertTrue(runtime.contains("dungeonF7Predev"));
        assertTrue(runtime.contains("dungeonAnnounceLocation"));
        assertTrue(runtime.contains("dungeonAnnouncePlayerCount"));
        assertTrue(runtime.contains("onEntityMetadata"));
        assertTrue(runtime.contains("dungeonEspItems"));
        assertTrue(runtime.contains("dungeonEspSecretClicked"));
        assertTrue(runtime.contains("dungeonAnnounceKeyDrop"));
        assertTrue(runtime.contains("dungeonF7Pre4Complete"));
        assertTrue(runtime.contains("dungeonF7HideOtherTitles"));
        assertTrue(runtime.contains("dungeonF7HideAtSs"));
        assertTrue(runtime.contains("dungeonF7HideAfterLeap"));
        assertTrue(runtime.contains("dungeonHudMelodyOther"));
        assertTrue(runtime.contains("dungeonF7RelicHighlight"));
        assertTrue(runtime.contains("dungeonF7RelicBlockWrong"));
        assertTrue(runtime.contains("dungeonF7CrystalSpawn"));
        assertTrue(runtime.contains("shouldHideTeammate"));
        assertTrue(!runtime.contains("qol.dungeon_sharp"));

        String client = Files.readString(Path.of(
                "src/client/java/fi/rotclient/RotClientClient.java"), StandardCharsets.UTF_8);
        assertTrue(client.contains("DungeonRuntime.onBlockUpdate"));

        String mixin = Files.readString(Path.of(
                "src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixin.contains("DungeonRuntime.onEntityMetadata"));

        String catalog = Files.readString(Path.of(
                "src/main/java/fi/rotclient/QolUtilityCatalog.java"), StandardCharsets.UTF_8);
        assertTrue(catalog.contains("qol.dungeon_f7.sharp_shooter"));
        assertTrue(catalog.contains("qol.dungeon_f7.term_times"));
        assertTrue(catalog.contains("qol.dungeon_announce.position"));
        assertTrue(catalog.contains("qol.dungeon_hud.ledge"));
        assertTrue(catalog.contains("qol.dungeon_hud.mask_overlay"));
        assertTrue(catalog.contains("qol.dungeon_f7.predev"));
        assertTrue(catalog.contains("qol.dungeon_announce.player_count"));
        assertTrue(catalog.contains("qol.dungeon_announce.location"));
        assertTrue(catalog.contains("qol.dungeon_esp.items"));
        assertTrue(catalog.contains("qol.dungeon_esp.secret_clicked"));
        assertTrue(catalog.contains("qol.dungeon_announce.key_drop"));
        assertTrue(catalog.contains("qol.dungeon_f7.pre4_complete"));
        assertTrue(catalog.contains("qol.dungeon_f7.hide_other_titles"));
        assertTrue(catalog.contains("qol.dungeon_f7.hide_at_ss"));
        assertTrue(catalog.contains("qol.dungeon_f7.hide_after_leap"));
        assertTrue(catalog.contains("qol.dungeon_hud.melody_other"));
        assertTrue(catalog.contains("qol.dungeon_f7.relic_highlight"));
        String plusCatalog = Files.readString(Path.of(
                "src/plus/java/fi/rotclient/QolPlusCatalog.java"), StandardCharsets.UTF_8);
        assertTrue(plusCatalog.contains("qol.dungeon_f7.relic_block_wrong"));
        String leap = Files.readString(Path.of(
                "src/client/java/fi/rotclient/DungeonLeapOverlayRuntime.java"), StandardCharsets.UTF_8);
        assertTrue(leap.contains("fromLore"));
        assertTrue(leap.contains("statusLabel"));
        assertEquals(110, QolUtilityCatalog.modules().size());
    }
}
