package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.util.List;
import org.junit.jupiter.api.Test;

final class IotaKuudraPolicyTest {
    @Test
    void phaseChatTracksElleSupplyBuildStunAndDown() {
        assertEquals(IotaKuudraPolicy.Phase.NONE,
                IotaKuudraPolicy.phaseFromChat("Sending to server mc.hypixel.net..."));
        assertEquals(IotaKuudraPolicy.Phase.NONE,
                IotaKuudraPolicy.phaseFromChat("Starting in 4 seconds..."));
        assertEquals(IotaKuudraPolicy.Phase.SUPPLIES, IotaKuudraPolicy.phaseFromChat(
                "[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!"));
        assertEquals(IotaKuudraPolicy.Phase.BUILD, IotaKuudraPolicy.phaseFromChat(
                "[NPC] Elle: OMG! Great work collecting my supplies!"));
        assertEquals(IotaKuudraPolicy.Phase.EATEN, IotaKuudraPolicy.phaseFromChat(
                "[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!"));
        assertEquals(IotaKuudraPolicy.Phase.STUN,
                IotaKuudraPolicy.phaseFromChat("Player123 has been eaten by Kuudra!"));
        assertNull(IotaKuudraPolicy.phaseFromChat("Elle has been eaten by Kuudra!"));
        assertEquals(IotaKuudraPolicy.Phase.DPS,
                IotaKuudraPolicy.phaseFromChat("destroyed one of Kuudra's pods!"));
        assertEquals(IotaKuudraPolicy.Phase.SKIP, IotaKuudraPolicy.phaseFromChat(
                "[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!"));
        assertEquals(IotaKuudraPolicy.Phase.COMPLETED,
                IotaKuudraPolicy.phaseFromChat("KUUDRA DOWN!"));
        assertTrue(IotaKuudraPolicy.isDefeat("DEFEAT"));
        assertTrue(IotaKuudraPolicy.isDefeat("§cDEFEAT"));
        assertFalse(IotaKuudraPolicy.isDefeat("KUUDRA DOWN!"));
        assertTrue(IotaKuudraPolicy.supplyProgress("[||||           ] 23%").orElse(-1) == 23);
    }

    @Test
    void kuudraChestsParsePaidFreeAndProfitMath() {
        assertEquals(IotaKuudraPolicy.ChestKind.PAID, IotaKuudraPolicy.chestKind("Paid Chest"));
        assertEquals(IotaKuudraPolicy.ChestKind.FREE, IotaKuudraPolicy.chestKind("§aFree Chest"));
        assertEquals(IotaKuudraPolicy.ChestKind.NONE, IotaKuudraPolicy.chestKind("Kuudra"));
        assertEquals(IotaKuudraPolicy.ChestKind.PAID, IotaKuudraPolicy.chestReward("PAID CHEST REWARDS").orElse(IotaKuudraPolicy.ChestKind.NONE));
        assertEquals(IotaKuudraPolicy.ChestKind.FREE, IotaKuudraPolicy.chestReward("§6FREE CHEST REWARDS").orElse(IotaKuudraPolicy.ChestKind.NONE));
        assertTrue(IotaKuudraPolicy.isBuySlot(31));
        assertFalse(IotaKuudraPolicy.isBuySlot(50));
        assertTrue(IotaKuudraPolicy.isItemRerollSlot(50));
        assertTrue(IotaKuudraPolicy.isShardRerollSlot(51));
        assertEquals(IotaKuudraPolicy.RerollKind.ITEMS, IotaKuudraPolicy.rerollKind(
                50, "Reroll Chest", "Click to reroll"));
        assertEquals(IotaKuudraPolicy.RerollKind.NONE, IotaKuudraPolicy.rerollKind(
                50, "Reroll Chest", "You already rerolled this chest"));
        assertEquals(IotaKuudraPolicy.RerollKind.SHARD, IotaKuudraPolicy.rerollKind(
                51, "Reroll Shard", "Click to reroll this shard"));
        assertEquals(750_000L, IotaKuudraPolicy.rerollCost("Cost: 750,000 coins"));
        assertTrue(IotaKuudraPolicy.alreadyOpened("§cAlready opened!"));
        assertTrue(IotaKuudraPolicy.isBuyAction("Click to open!"));
        assertEquals(1_500_000L, IotaKuudraPolicy.loreCoins("Worth 1,500,000 coins"));
        assertEquals(2_000_000L, IotaKuudraPolicy.keyCost("Cost: 2,000,000 coins"));
        assertEquals(-500_000L, IotaKuudraPolicy.netProfit(
                IotaKuudraPolicy.ChestKind.PAID, 1_500_000L, 2_000_000L));
        assertEquals(1_500_000L, IotaKuudraPolicy.netProfit(
                IotaKuudraPolicy.ChestKind.FREE, 1_500_000L, 2_000_000L));
        assertEquals(11, IotaKuudraPolicy.decrementChests(12));
        assertEquals(0, IotaKuudraPolicy.decrementChests(0));
        assertEquals(90.0D, IotaKuudraPolicy.nextAverageSeconds(0.0D, 0, 90_000L), 1.0E-9);
        assertEquals(1_000_000L, IotaKuudraPolicy.hourlyRate(1_000_000L, 3_600_000L));
        long t0 = 1_000_000L;
        assertEquals(IotaKuudraPolicy.SessionExpire.Kind.NONE,
                IotaKuudraPolicy.expireSession(t0, 0L, 0L).kind());
        assertEquals(IotaKuudraPolicy.SessionExpire.Kind.WARN,
                IotaKuudraPolicy.expireSession(t0 + 301_000L, t0, 0L).kind());
        assertEquals(IotaKuudraPolicy.SessionExpire.Kind.RESET,
                IotaKuudraPolicy.expireSession(t0 + 1_261_000L, t0, t0).kind());
        assertTrue(IotaKuudraPolicy.sessionResetMessage().contains("20 minutes"));
        assertTrue(IotaKuudraPolicy.sessionWarningMessage(6L, 15L).contains("6 minutes"));
    }

    @Test
    void crateFromGiantPlacesSupplyOnYawBearing() {
        IotaKuudraPolicy.SupplyCrate crate = IotaKuudraPolicy.crateFromGiant(10.0D, -20.0D, 45.0F, 7);
        double yaw = Math.toRadians(45.0D + 130.0D);
        assertEquals(10.0D + 3.7D * Math.cos(yaw), crate.position().x(), 1.0E-9);
        assertEquals(-20.0D + 3.7D * Math.sin(yaw), crate.position().z(), 1.0E-9);
        assertEquals(75.0D, crate.position().y());
        assertEquals(7, crate.entityId());
        IotaKuudraPolicy.Aabb box = IotaKuudraPolicy.supplyBox(crate.position(), 1.0F);
        assertEquals(crate.position().x() + 0.5D - 0.5D, box.minX(), 1.0E-9);
        assertEquals(crate.position().y() - 1.0D, box.minY(), 1.0E-9);
        assertEquals(crate.position().z() + 1.5D - 0.5D, box.minZ(), 1.0E-9);
        assertEquals(crate.position().y(), box.maxY(), 1.0E-9);
    }

    @Test
    void bundledPilesLoadShopTriangleEqualsSlashAndX() {
        List<IotaKuudraPolicy.Pile> piles = IotaKuudraData.piles();
        assertEquals(6, piles.size());
        assertPile(piles, "Shop", -98.5D, 78.4D, -113.5D, 7);
        assertPile(piles, "Triangle", -94.5D, 78.4D, -106.5D, 6);
        assertPile(piles, "Equals", -106.5D, 78.4D, -99.5D, 5);
        assertPile(piles, "Slash", -98.5D, 78.4D, -99.5D, 4);
        assertPile(piles, "X Cannon", -110.5D, 78.4D, -106.5D, 2);
        assertPile(piles, "X", -106.5D, 78.4D, -113.5D, 1);
        IotaKuudraPolicy.Pile shop = piles.get(0);
        assertTrue(shop.isNearby(new IotaKuudraPolicy.Vec3d(-98.5D, 78.4D, -113.5D)));
        assertTrue(shop.isNearby(new IotaKuudraPolicy.Vec3d(-98.5D, 83.4D, -113.5D)));
        assertFalse(shop.isNearby(new IotaKuudraPolicy.Vec3d(-98.5D, 84.5D, -113.5D)));
        assertTrue(shop.isNoPrePile(7));
        assertFalse(shop.isNoPrePile(6));
    }

    @Test
    void bundledPearlsAndEtherwarpKeepExpectedCounts() {
        List<IotaKuudraPolicy.PearlArea> areas = IotaKuudraData.pearls();
        assertEquals(15, areas.size());
        IotaKuudraPolicy.PearlArea x = areas.stream()
                .filter(area -> area.name().equals("x"))
                .findFirst()
                .orElseThrow();
        assertTrue(x.containsPlayer(-140.0D, -140.0D));
        assertFalse(x.containsPlayer(0.0D, 0.0D));
        List<IotaKuudraPolicy.EtherCategory> categories = IotaKuudraData.etherwarp();
        assertEquals(6, categories.size());
        int waypoints = categories.stream().mapToInt(category -> category.waypoints().size()).sum();
        assertTrue(waypoints >= 10);
        IotaKuudraPolicy.EtherWaypoint skip = categories.get(0).waypoints().get(0);
        assertTrue(skip.shouldShowInPhase(IotaKuudraPolicy.Phase.DPS));
        assertFalse(skip.shouldShowInPhase(IotaKuudraPolicy.Phase.SUPPLIES));
    }

    @Test
    void pearlShowAndBoxRespectHidePre() {
        IotaKuudraPolicy.PearlWaypoint hide = new IotaKuudraPolicy.PearlWaypoint(
                new IotaKuudraPolicy.Vec3d(1, 2, 3), 0x00FFFF, null, 6, 1, 0.4F, "35", true);
        assertFalse(hide.shouldShow(1));
        assertTrue(hide.shouldShow(6));
        assertTrue(hide.shouldShow(0));
        IotaKuudraPolicy.Aabb box = IotaKuudraPolicy.pearlBox(hide.target(), 0.4F, -3);
        double size = Math.max(0.05D, 0.4D * (1.0D - 0.3D));
        assertEquals(1.0D - size / 2.0D - 0.5D, box.minX(), 1.0E-6);
        assertEquals(2.0D + size, box.maxY(), 1.0E-6);
    }

    @Test
    void dynamicPearlOnlyMovesY() {
        IotaKuudraPolicy.Vec3d target = new IotaKuudraPolicy.Vec3d(-100.0D, 80.0D, -110.0D);
        IotaKuudraPolicy.Vec3d stand = new IotaKuudraPolicy.Vec3d(-90.0D, 76.0D, -100.0D);
        IotaKuudraPolicy.Vec3d player = new IotaKuudraPolicy.Vec3d(-88.0D, 78.0D, -96.0D);
        IotaKuudraPolicy.Vec3d moved = IotaKuudraPolicy.dynamicPearlTarget(
                target, player, stand, false, false);
        assertEquals(target.x(), moved.x(), 1.0E-9);
        assertEquals(target.z(), moved.z(), 1.0E-9);
        double height = (78.0D - 76.0D) * 0.81D + (-96.0D - -100.0D) * 0.31D
                + -((-88.0D - -90.0D) * 0.63D);
        assertEquals(target.y() - height, moved.y(), 1.0E-9);
    }

    @Test
    void stunOffsetAndPodsUseArenaAnchors() {
        assertEquals(new IotaKuudraPolicy.Vec3d(-152.5D, 27.0D, -172.5D),
                IotaKuudraPolicy.stunPod("LEFT_POD"));
        assertEquals(new IotaKuudraPolicy.Vec3d(-167.5D, 28.0D, -167.5D),
                IotaKuudraPolicy.stunPod("RIGHT_POD"));
        IotaKuudraPolicy.Vec3d player = new IotaKuudraPolicy.Vec3d(-160.0D, 49.0D, -185.0D);
        IotaKuudraPolicy.Vec3d pod = IotaKuudraPolicy.stunPod("LEFT_POD");
        IotaKuudraPolicy.Vec3d moved = IotaKuudraPolicy.stunWaypoint(pod, player, true, false);
        assertEquals(pod.x() + (player.x() - -161.0D), moved.x(), 1.0E-9);
        assertEquals(pod, IotaKuudraPolicy.stunWaypoint(pod, player, false, true));
        assertNull(IotaKuudraPolicy.stunWaypoint(pod, player, false, false));
    }

    @Test
    void ichorKuudraAndGiantAlertUseHealthAndChat() {
        IotaKuudraPolicy.IchorParse parsed = IotaKuudraPolicy.parseIchor(
                "Casting spell: Ichor Pool at (10, 20, -30)").orElseThrow();
        assertEquals(10, parsed.x());
        assertEquals(20, parsed.y());
        assertEquals(-30, parsed.z());
        assertEquals(10.5D, IotaKuudraPolicy.ichorCenter(parsed).x());
        assertEquals(20.05D, IotaKuudraPolicy.ichorCenter(parsed).y(), 1.0E-9);
        assertTrue(IotaKuudraPolicy.isKuudraBoss(30, 10000.0F, 1.0F));
        assertFalse(IotaKuudraPolicy.isKuudraBoss(29, 10000.0F, 1.0F));
        assertFalse(IotaKuudraPolicy.isKuudraBoss(30, 9999.0F, 1.0F));
        assertEquals(IotaKuudraPolicy.SpawnDirection.RIGHT, IotaKuudraPolicy.spawnDirection(-130.0D, -100.0D));
        assertEquals(IotaKuudraPolicy.SpawnDirection.FRONT, IotaKuudraPolicy.spawnDirection(-100.0D, -80.0D));
        assertEquals(IotaKuudraPolicy.SpawnDirection.LEFT, IotaKuudraPolicy.spawnDirection(-70.0D, -100.0D));
        assertEquals(IotaKuudraPolicy.SpawnDirection.BACK, IotaKuudraPolicy.spawnDirection(-100.0D, -140.0D));
        assertEquals(IotaKuudraPolicy.AlertLevel.PRIMARY, IotaKuudraPolicy.giantAlert(true, true));
        assertEquals(IotaKuudraPolicy.AlertLevel.SECONDARY, IotaKuudraPolicy.giantAlert(false, true));
        assertEquals(IotaKuudraPolicy.AlertLevel.NONE, IotaKuudraPolicy.giantAlert(false, false));
    }

    @Test
    void colorsMatchPackedArgbDefaults() {
        assertEquals(new Color(0, 255, 255, 40).getRGB(), IotaKuudraPolicy.SUPPLY_COLOR);
        assertEquals(new Color(255, 255, 255, 52).getRGB(), IotaKuudraPolicy.PILE_NORMAL_COLOR);
        assertEquals(new Color(0, 255, 144, 50).getRGB(), IotaKuudraPolicy.PILE_NO_PRE_COLOR);
        assertEquals(new Color(0, 245, 255, 200).getRGB(), IotaKuudraPolicy.STUN_COLOR);
        assertEquals(new Color(255, 2, 2, 231).getRGB(), IotaKuudraPolicy.KUUDRA_HITBOX_COLOR);
        assertEquals(0xFFA80000, IotaKuudraPolicy.buildColor(10));
        assertEquals(0xFF7DDA58, IotaKuudraPolicy.buildColor(100));
        assertEquals(35, IotaKuudraPolicy.supplyProgress("[|||||     ] 35%").orElse(-1));
        assertEquals(2, IotaKuudraPolicy.progressIndex(17));
        assertEquals(600L, IotaKuudraPolicy.targetTimeMs(2));
    }

    @Test
    void noPrePartyChatAndEtherwarpBoxUseArenaRules() {
        assertEquals(6, IotaKuudraPolicy.parseNoPre("Party > Steve: no triangle")
                .orElseThrow().missingPreValue());
        assertEquals(2, IotaKuudraPolicy.parseNoPre("missing xcannon")
                .orElseThrow().missingPreValue());
        IotaKuudraPolicy.Aabb full = IotaKuudraPolicy.etherwarpBox(
                IotaKuudraPolicy.HighlightShape.FULL,
                new IotaKuudraPolicy.Vec3d(10.0D, 5.0D, -3.0D),
                null, null);
        assertEquals(9.5D, full.minX(), 1.0E-9);
        assertEquals(6.0D, full.maxY(), 1.0E-9);
        assertEquals(IotaKuudraPolicy.HighlightShape.SLAB_UPPER, IotaKuudraPolicy.parseShape("UPPER_SLAB"));
        assertTrue(IotaKuudraPolicy.isKuudraArea("SKYBLOCK\n⏣ Kuudra's Hollow"));
        assertTrue(IotaKuudraPolicy.isKuudraArea("Kuudra’s Hollow"));
        assertEquals(IotaKuudraPolicy.Phase.SUPPLIES,
                IotaKuudraPolicy.phaseFromScoreboard("Rescue supplies (3/6)").orElseThrow());
        assertEquals(20, IotaKuudraPolicy.parseBuildProgress("§ePROGRESS: §a20%").orElse(-1));
        assertEquals(new Color(0, 255, 0, 204).getRGB() & 0xFF000000,
                IotaKuudraPolicy.withOpacity(0x00FF00, 80.0F) & 0xFF000000);
    }

    @Test
    void pullCircleAndBobberUseFiveBlockRadius() {
        IotaKuudraPolicy.Vec3d crate = new IotaKuudraPolicy.Vec3d(0.0D, 75.0D, 0.0D);
        IotaKuudraPolicy.Vec3d center = IotaKuudraPolicy.supplyPullCenter(crate);
        assertEquals(0.5D, center.x());
        assertEquals(1.5D, center.z());
        assertTrue(IotaKuudraPolicy.bobberInsidePull(crate, 0.5D, 75.0D, 1.5D));
        assertFalse(IotaKuudraPolicy.bobberInsidePull(crate, 6.6D, 75.0D, 1.5D));
        assertFalse(IotaKuudraPolicy.bobberInsidePull(crate, 0.5D, 80.1D, 1.5D));
    }

    private static void assertPile(
            List<IotaKuudraPolicy.Pile> piles,
            String name,
            double x,
            double y,
            double z,
            int noPre) {
        IotaKuudraPolicy.Pile pile = piles.stream()
                .filter(entry -> entry.name().equals(name))
                .findFirst()
                .orElseThrow();
        assertEquals(x, pile.position().x());
        assertEquals(y, pile.position().y());
        assertEquals(z, pile.position().z());
        assertEquals(noPre, pile.noPreValue());
    }
}
