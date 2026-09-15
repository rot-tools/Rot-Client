package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class DianaPolicyTest {
    @Test
    void particlesClassifyStartMobTreasureEnchantAndFootstep() {
        assertEquals(
                DianaPolicy.ParticleKind.START,
                DianaPolicy.classifyParticle(new DianaPolicy.ParticlePacket(
                        "minecraft:enchanted_hit", 4, 0.01F, 0.5F, 0.1F, 0.5F)));
        assertEquals(
                DianaPolicy.ParticleKind.MOB,
                DianaPolicy.classifyParticle(new DianaPolicy.ParticlePacket(
                        "minecraft:crit", 3, 0.01F, 0.5F, 0.1F, 0.5F)));
        assertEquals(
                DianaPolicy.ParticleKind.TREASURE,
                DianaPolicy.classifyParticle(new DianaPolicy.ParticlePacket(
                        "minecraft:dripping_lava", 2, 0.01F, 0.35F, 0.1F, 0.35F)));
        assertEquals(
                DianaPolicy.ParticleKind.ENCHANT,
                DianaPolicy.classifyParticle(new DianaPolicy.ParticlePacket(
                        "minecraft:enchant", 5, 0.05F, 0.5F, 0.4F, 0.5F)));
        assertEquals(
                DianaPolicy.ParticleKind.FOOTSTEP,
                DianaPolicy.classifyParticle(new DianaPolicy.ParticlePacket(
                        "minecraft:crit", 1, 0.0F, 0.05F, 0.0F, 0.05F)));
        assertEquals(
                DianaPolicy.ParticleKind.SPADE_TRAIL,
                DianaPolicy.classifyParticle(new DianaPolicy.ParticlePacket(
                        "minecraft:dripping_lava", 2, -0.5F, 0.0F, 0.0F, 0.0F)));
        DianaPolicy.BurrowSample start = DianaPolicy.applyParticle(null, DianaPolicy.ParticleKind.START);
        start = DianaPolicy.applyParticle(start, DianaPolicy.ParticleKind.ENCHANT);
        start = DianaPolicy.applyParticle(start, DianaPolicy.ParticleKind.FOOTSTEP);
        assertTrue(start.complete());
        assertEquals(DianaPolicy.BurrowType.START, start.type());
        DianaPolicy.BurrowSample mob = DianaPolicy.applyParticle(null, DianaPolicy.ParticleKind.MOB);
        mob = DianaPolicy.applyParticle(mob, DianaPolicy.ParticleKind.ENCHANT);
        mob = DianaPolicy.applyParticle(mob, DianaPolicy.ParticleKind.FOOTSTEP);
        assertEquals(DianaPolicy.BurrowType.MOB, mob.type());
        DianaPolicy.BurrowSample treasure = DianaPolicy.applyParticle(null, DianaPolicy.ParticleKind.TREASURE);
        treasure = DianaPolicy.applyParticle(treasure, DianaPolicy.ParticleKind.ENCHANT);
        treasure = DianaPolicy.applyParticle(treasure, DianaPolicy.ParticleKind.FOOTSTEP);
        assertEquals(DianaPolicy.BurrowType.TREASURE, treasure.type());
        assertFalse(DianaPolicy.applyParticle(null, DianaPolicy.ParticleKind.ENCHANT).complete());
    }

    @Test
    void guessMathReconstructsCubicTrailNumerically() {
        double[] cx = {10.0, 2.0, 0.05, -0.001};
        double[] cy = {70.0, -0.4, 0.02, 0.0};
        double[] cz = {-5.0, 1.5, -0.03, 0.0005};
        List<DianaPolicy.Point> points = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            points.add(new DianaPolicy.Point(
                    DianaPolicy.evalCubic(cx, i),
                    DianaPolicy.evalCubic(cy, i),
                    DianaPolicy.evalCubic(cz, i)));
        }
        DianaPolicy.GuessResult result = DianaPolicy.guessBurrow(points).orElseThrow();
        DianaPolicy.Point deriv = new DianaPolicy.Point(cx[1], cy[1], cz[1]);
        double pitch = DianaPolicy.pitchFromDerivative(deriv);
        double control = Math.sqrt(24.0 * Math.sin(pitch - Math.PI) + 25.0);
        double length = Math.sqrt(cx[1] * cx[1] + cy[1] * cy[1] + cz[1] * cz[1]);
        double t = 3.0 * control / length;
        assertEquals(DianaPolicy.evalCubic(cx, t), result.location().x(), 1.0E-6);
        assertEquals(DianaPolicy.evalCubic(cy, t), result.location().y(), 1.0E-6);
        assertEquals(DianaPolicy.evalCubic(cz, t), result.location().z(), 1.0E-6);
        assertEquals(pitch, result.pitchRadians(), 1.0E-6);
        assertFalse(DianaPolicy.guessBurrow(points.subList(0, 3)).isPresent());
    }

    @Test
    void inquisitorNametagAndSpawnChatMatch() {
        assertEquals(
                DianaPolicy.RareMob.INQUISITOR,
                DianaPolicy.matchNametag("§6[Lv125] §6Minos Inquisitor").orElseThrow());
        assertEquals(
                DianaPolicy.RareMob.INQUISITOR,
                DianaPolicy.parseSpawn(
                                "§c§lWoah! §r§eYou dug out a §r§6Minos Inquisitor§r§e!")
                        .orElseThrow());
        assertEquals(
                DianaPolicy.RareMob.CHAMPION,
                DianaPolicy.parseSpawn("Oh! You dug out a Minos Champion!").orElseThrow());
        assertTrue(DianaPolicy.matchNametag("§5Sphinx").orElseThrow().rareHighlight());
    }

    @Test
    void dropChatTracksProfitItemsAndCoins() {
        assertEquals(
                "Crown of Greed",
                DianaPolicy.parseDrop("RARE DROP! You dug out a Crown of Greed (+42 ✯ Magic Find)!")
                        .orElseThrow()
                        .name());
        assertEquals(
                1,
                DianaPolicy.parseDrop("RARE DROP! Antique Remedies").orElseThrow().amount());
        assertEquals(
                "Daedalus Stick",
                DianaPolicy.parseDrop("RARE DROP! You dug out a Daedalus Stick").orElseThrow().name());
        assertEquals(
                "Minos Relic",
                DianaPolicy.parseDrop("RARE DROP! Minos Relic").orElseThrow().name());
        assertEquals(
                1_250_000,
                DianaPolicy.parseDrop("Wow! You dug out 1,250,000 coins!").orElseThrow().amount());
        Map<String, Integer> drops = DianaPolicy.applyDrop(Map.of(), new DianaPolicy.Drop("Crown of Greed", 1));
        assertEquals(1, drops.get("Crown of Greed"));
        DianaPolicy.DugChat dug = DianaPolicy.parseDug(
                        "You dug out a Griffin Burrow! (2/4)")
                .orElseThrow();
        assertEquals(2, dug.remaining());
        assertTrue(DianaPolicy.parseDug("You finished the Griffin burrow chain! (4/4)")
                .orElseThrow()
                .chainFinished());
    }

    @Test
    void griffinWarningAndCheatGatesStayOffByDefault() {
        assertTrue(DianaPolicy.shouldWarnGriffin(true, true, true, false, false));
        assertFalse(DianaPolicy.shouldWarnGriffin(true, true, true, true, false));
        assertFalse(DianaPolicy.shouldWarnGriffin(true, true, true, false, true));
        assertTrue(DianaPolicy.isGriffinPet("[Lvl 100] Griffin"));
        assertFalse(DianaPolicy.shouldPartyShare(
                false, false, DianaPolicy.RareMob.INQUISITOR));
        assertFalse(DianaPolicy.shouldPartyShare(
                true, false, DianaPolicy.RareMob.INQUISITOR));
        assertTrue(DianaPolicy.shouldPartyShare(
                true, true, DianaPolicy.RareMob.INQUISITOR));
        assertFalse(DianaPolicy.shouldAutoWarp(true, false));
        assertTrue(DianaPolicy.shouldAutoWarp(true, true));
        assertEquals(
                "x: 10, y: 72, z: -4 | Minos Inquisitor",
                DianaPolicy.partyShareLine(DianaPolicy.RareMob.INQUISITOR, 10, 72, -4));
        assertTrue(DianaPolicy.shouldMuteBuggedSpade(
                true, true, "music.game", 0.5F, 1.0F, false));
        assertFalse(DianaPolicy.shouldMuteBuggedSpade(
                true, true, "music.game", 1.0F, 1.0F, true));
        assertTrue(DianaPolicy.isDoingDiana(
                List.of("Burrows: 2/4"), true));
        assertFalse(DianaPolicy.isDoingDiana(
                List.of("Burrows: 2/4"), false));
        assertFalse(DianaPolicy.isDoingDiana(
                List.of("Purse: 1,000"), true));
        assertFalse(DianaPolicy.burrowIsStale(1_750L, 1_000L));
        assertTrue(DianaPolicy.burrowIsStale(1_751L, 1_000L));
        assertTrue(DianaPolicy.shouldHideDuplicate(
                true,
                "You dug out a Griffin Burrow! (1/4)",
                "You dug out a Griffin Burrow! (1/4)",
                50));
        assertTrue(DianaPolicy.nearestWarp(
                        new DianaPolicy.Point(-250, 130, 45),
                        new DianaPolicy.Point(0, 70, 0))
                .orElseThrow()
                .command()
                .equals("warp castle"));
        assertTrue(DianaPolicy.isDianaSpade("ANCESTRAL_SPADE", "Ancestral Spade"));
        assertFalse(DianaPolicy.isDianaSpade("DIAMOND_SHOVEL", "Diamond Shovel"));
    }
}
