package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlayerChatSpoofTest {
    @Test
    void questSignalsOnlyComeFromServerLines() {
        assertEquals(SlayerPolicy.QuestSignal.STARTED, SlayerPolicy.questSignal("  §5§lSLAYER QUEST STARTED!"));
        assertEquals(SlayerPolicy.QuestSignal.COMPLETED, SlayerPolicy.questSignal("  §a§lSLAYER QUEST COMPLETE!"));
        assertEquals(SlayerPolicy.QuestSignal.FAILED, SlayerPolicy.questSignal("  SLAYER QUEST FAILED!"));
        assertEquals(SlayerPolicy.QuestSignal.NONE, SlayerPolicy.questSignal("§7[MVP+] Bob§f: SLAYER QUEST COMPLETE!"));
        assertEquals(SlayerPolicy.QuestSignal.NONE, SlayerPolicy.questSignal("Bob: slayer quest started!"));
    }

    @Test
    void dropsOnlyComeFromServerLines() {
        assertEquals("Judgement Core",
                SlayerPolicy.dropObservation("§9§lVERY RARE DROP! §f(§9Judgement Core§f)").orElseThrow().displayName());
        assertTrue(SlayerPolicy.dropObservation("[MVP+] Bob: RARE DROP! (Judgement Core)").isEmpty());
        assertTrue(SlayerPolicy.dropObservation("Bob: CRAZY RARE DROP! (Judgement Core)").isEmpty());
    }

    @Test
    void gummyAndRngMeterChatIgnorePlayerText() {
        assertTrue(SlayerPolishPolicy.isGummyConsumeChat("You consumed a Re-heated Gummy Polar Bear!"));
        assertFalse(SlayerPolishPolicy.isGummyConsumeChat("Bob: I have a re-heated gummy polar bear"));
        assertEquals(12_345L,
                SlayerRngMeterPolicy.chatStoredXp("Slayer RNG Meter - 12,345 Stored XP").orElseThrow());
        assertTrue(SlayerRngMeterPolicy.chatStoredXp("Bob: RNG Meter - 12,345 Stored XP").isEmpty());
        assertTrue(SlayerRngMeterPolicy.chatSelection("You set your Zombie RNG Meter to drop Foul Flesh!").isPresent());
        assertTrue(SlayerRngMeterPolicy.chatSelection("Bob: you set your Zombie RNG Meter to drop Foul Flesh!").isEmpty());
    }

    @Test
    void sidebarTextIsOrderedAndSharedWithinATick() throws Exception {
        String sidebar = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SkyBlockSidebar.java"), StandardCharsets.UTF_8);
        assertTrue(sidebar.contains("CustomScoreboardLines.orderSidebar"));
        assertTrue(sidebar.contains("CACHE_NANOS"));
    }

    @Test
    void slayerRenderPassDoesNotQueryTheWorld() throws Exception {
        String runtime = Files.readString(Path.of(
                "src/client/java/fi/rotclient/SlayerRuntime.java"), StandardCharsets.UTF_8);
        int render = runtime.indexOf("private static void renderGizmosInternal()");
        int end = runtime.indexOf("private static void scanEntities(", render);
        String body = runtime.substring(render, end);
        assertFalse(body.contains("getEntities("));
        assertFalse(body.contains("unbrokenEffigies("));
        assertFalse(body.contains("slayerUnitPrices("));
        assertFalse(body.contains("ENGINE.snapshot("));
    }
}
