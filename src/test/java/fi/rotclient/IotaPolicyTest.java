package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class IotaPolicyTest {
    private static final IotaPolicy.PartyCommandConfig ALL =
            new IotaPolicy.PartyCommandConfig(
                    true, true, true, true, true, true, true, true, true, true, true, true);

    @Test
    void featureToggleActivationRules() {
        assertTrue(IotaPolicy.shouldActivate(true, false));
        assertFalse(IotaPolicy.shouldActivate(true, true));
        assertTrue(IotaPolicy.shouldDeactivate(false, true));
        assertFalse(IotaPolicy.shouldDeactivate(false, false));
    }

    @Test
    void limboKickUsesHypixelKickChat() {
        assertTrue(IotaPolicy.isLimboKick(
                "§cA kick occurred in your connection, so you were put in the SkyBlock lobby!"));
        assertFalse(IotaPolicy.isLimboKick("You were spawned in Limbo."));
    }

    @Test
    void partyJoinMatchesJoinedTheParty() {
        assertTrue(IotaPolicy.isPartyJoin("Steve joined the party."));
        assertTrue(IotaPolicy.isPartyJoin("§eAlex joined the party!"));
        assertFalse(IotaPolicy.isPartyJoin("Steve left the party."));
    }

    @Test
    void partyWarpUsesWpAliasesWithoutLeaderCheck() {
        Optional<IotaPolicy.PartyAction> warp = IotaPolicy.partyCommand(
                "Party > [MVP+] Steve: !wp",
                true,
                ALL,
                "Henri",
                40,
                20.0F,
                12,
                8,
                1,
                91.5D,
                2_500_000L,
                1_200_000L);
        assertTrue(warp.isPresent());
        assertEquals(IotaPolicy.PartyActionKind.COMMAND, warp.get().kind());
        assertEquals("party warp", warp.get().payload());
    }

    @Test
    void partyKuudraTierMapsT1ToT5() {
        Optional<IotaPolicy.PartyAction> t5 = IotaPolicy.partyCommand(
                "Party > Henri: !t5",
                true,
                ALL,
                "Henri",
                12,
                20.0F,
                0,
                0,
                0,
                0.0D,
                0L,
                0L);
        assertEquals("joininstance KUUDRA_INFERNAL", t5.orElseThrow().payload());
    }

    @Test
    void partyPingRepliesInPartyChat() {
        Optional<IotaPolicy.PartyAction> ping = IotaPolicy.partyCommand(
                "Party > Steve: !ping",
                true,
                ALL,
                "Henri",
                87,
                19.8F,
                0,
                0,
                0,
                0.0D,
                0L,
                0L);
        assertEquals(IotaPolicy.PartyActionKind.PARTY_CHAT, ping.orElseThrow().kind());
        assertEquals("[Rot] 87ms", ping.orElseThrow().payload());
    }

    @Test
    void disabledChildCommandsDoNothing() {
        IotaPolicy.PartyCommandConfig none =
                new IotaPolicy.PartyCommandConfig(
                        true, false, false, false, false, false, false, false, false, false, false, false);
        assertTrue(IotaPolicy.partyCommand(
                "Party > Steve: !warp",
                true,
                none,
                "Henri",
                1,
                20.0F,
                0,
                0,
                0,
                0.0D,
                0L,
                0L).isEmpty());
    }

    @Test
    void terminatorCooldownAndIdMatchTerminator() {
        assertTrue(IotaPolicy.isTerminatorCooldownChat(
                "§cThis ability is on cooldown for 1s"));
        assertTrue(IotaPolicy.isTerminatorId("TERMINATOR"));
        assertTrue(IotaPolicy.isTerminatorId("skyblock:TERMINATOR"));
        assertTrue(IotaPolicy.shouldMuteTerminator(true, true, true, true));
        assertFalse(IotaPolicy.shouldMuteTerminator(true, true, true, false));
    }

    @Test
    void fishingHookIgnoresArmorStandPlusOne() {
        assertTrue(IotaPolicy.shouldIgnoreHookOwner(true, 40, true, 41));
        assertFalse(IotaPolicy.shouldIgnoreHookOwner(true, 40, true, 40));
        assertFalse(IotaPolicy.shouldIgnoreHookOwner(false, 40, true, 41));
    }

    @Test
    void arrowTrackerParsesQuiverLoreAndChat() {
        assertEquals(
                1240,
                IotaPolicy.parseArrowCountFromLore(
                        List.of("§7Arrows Remaining: §f1,240")).orElseThrow());
        IotaPolicy.ArrowNotice empty = IotaPolicy.arrowChatNotice(
                "Your quiver is now completely empty!").orElseThrow();
        assertTrue(empty.outOfArrows());
        IotaPolicy.ArrowNotice low = IotaPolicy.arrowChatNotice(
                "QUIVER! You only have 32 Flint Arrow left!").orElseThrow();
        assertEquals(32, low.count());
        assertEquals("Flint Arrow", low.type());
    }

    @Test
    void playersCannotSpoofServerLineTriggersByTypingThePhrase() {
        assertTrue(IotaPolicy.isLimboKick(IotaPolicy.LIMBO_MESSAGE));
        assertTrue(IotaPolicy.isLimboKick("§c" + IotaPolicy.LIMBO_MESSAGE));
        assertFalse(IotaPolicy.isLimboKick("[MVP+] Steve: " + IotaPolicy.LIMBO_MESSAGE));
        assertFalse(IotaPolicy.isLimboKick("Party > Steve: " + IotaPolicy.LIMBO_MESSAGE));

        assertTrue(IotaPolicy.isPartyJoin("[MVP+] Alex joined the party."));
        assertFalse(IotaPolicy.isPartyJoin("[MVP+] Steve: Alex joined the party."));
        assertFalse(IotaPolicy.isPartyJoin("Guild > Steve: Alex joined the party!"));

        assertTrue(IotaPolicy.arrowChatNotice("Your quiver is now completely empty!").isPresent());
        assertFalse(IotaPolicy.arrowChatNotice(
                "[MVP+] Steve: Your quiver is now completely empty!").isPresent());
    }

    @Test
    void tpsReplyNeverInventsANumber() {
        Optional<IotaPolicy.PartyAction> real = tpsCommand(19.6F);
        assertEquals("[Rot] 19.6", real.orElseThrow().payload());
        assertEquals("[Rot] TPS unavailable", tpsCommand(Float.NaN).orElseThrow().payload());
        assertEquals("[Rot] TPS unavailable", tpsCommand(0.0F).orElseThrow().payload());
    }

    private static Optional<IotaPolicy.PartyAction> tpsCommand(float tps) {
        return IotaPolicy.partyCommand(
                "Party > Steve: !tps",
                true,
                ALL,
                "Henri",
                40,
                tps,
                0,
                0,
                0,
                0.0D,
                0L,
                0L);
    }

    @Test
    void arrowHudHonorsOnlyShootingVisibility() {
        assertTrue(IotaPolicy.showArrowHud(
                true, true, true, false, IotaPolicy.VISIBILITY_ALWAYS));
        assertFalse(IotaPolicy.showArrowHud(
                true, true, true, false, IotaPolicy.VISIBILITY_ONLY_SHOOTING));
        assertTrue(IotaPolicy.showArrowHud(
                true, true, true, true, "ONLY_SHOOTING"));
        List<String> lines = IotaPolicy.arrowHudLines(
                new IotaPolicy.ArrowSnapshot("Flint Arrow", 12, false, true));
        assertEquals(1, lines.size());
        assertTrue(lines.getFirst().contains("Flint Arrow"));
    }

    @Test
    void coinFormatUsesCompactSuffixes() {
        assertEquals("2.50m", IotaPolicy.formatCoins(2_500_000L));
        assertEquals("-1.2k", IotaPolicy.formatCoins(-1200L));
    }
}
