package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class DianaWiringTest {
    @Test
    void runtimeSourceHasTickChatHudGizmoAndParticleHooks() throws Exception {
        String source = Files.readString(
                Path.of("src/client/java/fi/rotclient/DianaRuntime.java"),
                StandardCharsets.UTF_8);
        assertTrue(source.contains("static void tick(Minecraft client)"));
        assertTrue(source.contains("static void onChat(Component message)"));
        assertTrue(source.contains("static List<String> hudLines"));
        assertTrue(source.contains("static boolean hudVisible"));
        assertTrue(source.contains("static String overlayTitle"));
        assertTrue(source.contains("static boolean allowGameMessage"));
        assertTrue(source.contains("public static void renderGizmos()"));
        assertTrue(source.contains("public static void observeParticlePacket"));
        assertTrue(source.contains("public static void observeParticle("));
        assertTrue(source.contains("public static void onSpadeUse()"));
        assertTrue(source.contains("public static boolean shouldMuteSound"));
        assertTrue(source.contains("Serveri"));
        assertTrue(source.contains("pc "));
        assertTrue(source.contains("flags.autoWarp"));
        assertTrue(source.contains("shouldPartyShare(flags.share"));
        assertTrue(source.contains("dianaShareParty"));
        assertTrue(source.contains("dianaProfitHud"));
        int allowAt = source.indexOf("static boolean allowGameMessage");
        int onChatAt = source.indexOf("static void onChat");
        assertTrue(allowAt >= 0 && onChatAt > allowAt);
        assertFalse(source.substring(allowAt, onChatAt).contains("onChat("));
        assertTrue(source.contains("dianaShareAutoWarp"));
        assertFalse(source.contains("live Hypixel production"));
        String policy = Files.readString(
                Path.of("src/main/java/fi/rotclient/DianaPolicy.java"),
                StandardCharsets.UTF_8);
        assertTrue(policy.contains("classifyParticle"));
        assertTrue(policy.contains("guessBurrow"));
        assertTrue(policy.contains("shouldPartyShare"));
        assertTrue(policy.contains("shouldAutoWarp"));
        assertTrue(policy.contains("shouldMuteBuggedSpade"));
    }

    @Test
    void cheatTogglesDefaultFalseOnPolicy() {
        assertFalse(DianaPolicy.shouldPartyShare(
                true, false, DianaPolicy.RareMob.INQUISITOR));
        assertFalse(DianaPolicy.shouldAutoWarp(true, false));
        assertFalse(DianaPolicy.shouldPartyShare(
                false, true, DianaPolicy.RareMob.INQUISITOR));
    }

    @Test
    void particleMixinUses26_2HandleParticleEvent() throws Exception {
        String mixin = Files.readString(
                Path.of("src/client/java/fi/rotclient/mixin/ClientPacketListenerMixin.java"),
                StandardCharsets.UTF_8);
        assertTrue(mixin.contains("method = \"handleParticleEvent\""));
        assertFalse(mixin.contains("handleParticles"));
        assertTrue(mixin.contains("DianaRuntime.observeParticlePacket"));
    }
}
