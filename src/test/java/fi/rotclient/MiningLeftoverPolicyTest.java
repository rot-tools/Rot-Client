package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class MiningLeftoverPolicyTest {
    @Test
    void scathaAndWormNamesMatchPlain() {
        assertEquals(
                MiningLeftoverPolicy.WormKind.SCATHA,
                MiningLeftoverPolicy.wormKind("§c[Lv10] Scatha §c❤"));
        assertEquals(
                MiningLeftoverPolicy.WormKind.WORM,
                MiningLeftoverPolicy.wormKind("[Lv5] Worm ❤"));
        assertEquals(
                MiningLeftoverPolicy.WormKind.NONE,
                MiningLeftoverPolicy.wormKind("[Lv10] Goblin"));
        assertTrue(MiningLeftoverPolicy.withinWormAlertRadius(2, 4, 8));
        assertFalse(MiningLeftoverPolicy.withinWormAlertRadius(3, 0, 3));
    }

    @Test
    void wormCooldownTicksDownFromApproachingChat() {
        assertEquals(
                MiningLeftoverPolicy.NotifyKind.WORM_APPROACHING,
                MiningLeftoverPolicy.classifyChat(
                        "You hear the sound of something approaching..."));
        int next = MiningLeftoverPolicy.tickCooldown(1);
        assertEquals(0, next);
        assertTrue(MiningLeftoverPolicy.cooldownJustEnded(1, next));
        assertEquals(MiningLeftoverPolicy.WORM_COOLDOWN_TICKS, 620);
    }

    @Test
    void pityAbilityFuelAndTreasureParse() {
        Optional<MiningLeftoverPolicy.Pity> pity = MiningLeftoverPolicy.parsePity(
                List.of(" Glacite Mineshafts: 1,250/2,000"));
        assertTrue(pity.isPresent());
        assertEquals(1250, pity.get().current());
        assertEquals(750, pity.get().remaining());

        Optional<MiningLeftoverPolicy.PickaxeAbility> ability =
                MiningLeftoverPolicy.parseAbility(List.of("Pickobulus: READY"));
        assertTrue(ability.isPresent());
        assertTrue(ability.get().ready());

        Optional<MiningLeftoverPolicy.DrillFuel> fuel = MiningLeftoverPolicy.parseDrillFuel(
                List.of("§7Fuel: §e3,000§7/§e3,000"));
        assertTrue(fuel.isPresent());
        assertEquals(3000, fuel.get().remaining());

        Optional<MiningLeftoverPolicy.TreasureDistance> treasure =
                MiningLeftoverPolicy.parseTreasure("§3§lTREASURE: §b12.5m");
        assertTrue(treasure.isPresent());
        assertEquals(12.5D, treasure.get().meters(), 0.01D);
    }

    @Test
    void fetchurAndFossilMuncherSolveGateRiddles() {
        assertEquals(
                "Mithril",
                MiningLeftoverPolicy.solveFetchur("[NPC] Fetchur: its expensive minerals")
                        .orElseThrow());
        assertEquals(
                "Helix Fossil",
                MiningLeftoverPolicy.solveFossilMuncher(
                                "[NPC] Fossil Muncher: the fossil i want lived underwater")
                        .orElseThrow());
    }

    @Test
    void glaciteCorpseAndEvents() {
        assertEquals(
                MiningLeftoverPolicy.CorpseType.VANGUARD,
                MiningLeftoverPolicy.parseCorpseLoot("VANGUARD CORPSE LOOT!").orElseThrow());
        assertEquals(
                "x: 12, y: 64, z: -8",
                MiningLeftoverPolicy.parseCorpseCoords("x: 12, y: 64, z: -8")
                        .orElseThrow()
                        .partyLine());
        Map<MiningLeftoverPolicy.CorpseType, Boolean> tab =
                MiningLeftoverPolicy.parseCorpseTab(List.of(" Lapis: NOT LOOTED"));
        assertFalse(tab.get(MiningLeftoverPolicy.CorpseType.LAPIS));
        assertEquals(
                MiningLeftoverPolicy.MiningEvent.DOUBLE_POWDER,
                MiningLeftoverPolicy.parseEvent("2x Powder STARTED!"));
        assertEquals(
                MiningLeftoverPolicy.MiningEvent.GOBLIN_RAID,
                MiningLeftoverPolicy.parseEvent("PASSIVE EVENT Goblin Raid RUNNING FOR 1:00"));
    }

    @Test
    void notificationsHotmAndCommissionBooks() {
        assertEquals(
                MiningLeftoverPolicy.NotifyKind.MINESHAFT_PORTAL,
                MiningLeftoverPolicy.classifyChat(
                        "WOW! You found a Glacite Mineshaft portal!"));
        assertEquals(
                MiningLeftoverPolicy.NotifyKind.SUSPICIOUS_SCRAP,
                MiningLeftoverPolicy.classifyChat(
                        "EXCAVATOR! You found a Suspicious Scrap!"));
        assertEquals(
                MiningLeftoverPolicy.NotifyKind.COMMISSION_COMPLETE,
                MiningLeftoverPolicy.classifyChat(
                        "Mithril Miner Commission Complete! Visit the King"));
        assertTrue(MiningLeftoverPolicy.isHotmScreen("Heart of the Mountain"));
        assertTrue(MiningLeftoverPolicy.isCommissionsScreen("Commissions"));
        assertTrue(MiningLeftoverPolicy.isCompletedCommissionBook(List.of("COMPLETED")));
        assertTrue(MiningLeftoverPolicy.isCommissionMob("Goblin"));
        assertEquals("/call mismyla", MiningLeftoverPolicy.kingCallCommand());
        assertTrue(MiningLeftoverPolicy.isMiningArea("Crystal Hollows"));
    }

    @Test
    void combinedHudSkipsEmptySections() {
        List<String> lines = MiningLeftoverPolicy.hudLines(
                new MiningLeftoverPolicy.Pity(100, 2000),
                null,
                null,
                null,
                MiningLeftoverPolicy.MiningEvent.RAFFLE,
                40,
                MiningLeftoverPolicy.WormKind.NONE,
                null,
                Map.of());
        assertEquals(3, lines.size());
        assertTrue(lines.get(0).contains("Raffle"));
        assertTrue(lines.get(1).contains("Pity"));
        assertTrue(lines.get(2).contains("Worm CD"));
    }
}
