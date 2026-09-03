package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client glue for SkyBlock flavor extras.
 */
public final class SkyblockFlavorRuntime {
    private static boolean shaftAnnounced;
    private static String lastServerId = "";
    private static long lastServerMs;
    private static int lastQueuePosition = -1;
    private static long lastQueueMs;
    private static String ministerName = "";
    private static long lastDetectorPingMs;
    private static final List<Vec3> IMPLOSION_HOLDERS = new ArrayList<>();
    private static int implosionCacheTick = Integer.MIN_VALUE;

    private SkyblockFlavorRuntime() {
    }

    static void tick(Minecraft client) {
        PrizeSpinRuntime.tick(client);
        if (client == null || client.player == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        List<String> tab = CommissionDisplayRuntime.tabLines(client);
        SkyblockFlavorPolicy.parseMinister(tab).ifPresent(name -> ministerName = name);
        String area = SkyBlockAreaDetector.detect().displayName();
        boolean inShaft = SkyblockFlavorPolicy.isMineshaftArea(area);
        if (inShaft && extras.miningGlaciteEnabled && !shaftAnnounced) {
            shaftAnnounced = true;
            if (extras.miningGlaciteEnterTitle) {
                MiningLeftoverRuntime.showTitle("Glacite Mineshaft", true, false);
            }
            if (extras.miningGlaciteEnterChat) {
                localChat(SkyblockFlavorPolicy.shaftEnterLine(area));
            }
            if (extras.miningGlaciteEnterParty) {
                MiningLeftoverRuntime.sendPartyChat(SkyblockFlavorPolicy.shaftEnterLine(area));
            }
            if (extras.miningGlaciteKeyAnnounce) {
                String keys = SkyblockFlavorPolicy.keyAnnounceLine(
                        SkyblockFlavorPolicy.countUnlooted(MiningLeftoverRuntime.corpses()),
                        inventoryKeyCounts(client.player));
                if (!keys.isBlank()) {
                    localChat(keys);
                }
            }
        }
        if (!inShaft) {
            shaftAnnounced = false;
        }
    }

    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        String text = message.getString();
        PrizeSpinRuntime.onChat(text);
        long now = System.currentTimeMillis();
        SkyblockFlavorPolicy.parseServerId(text).ifPresent(id -> {
            if (qol.chatCommandsEnabled && qol.chatPreviousServer
                    && SkyblockFlavorPolicy.shouldAnnouncePreviousServer(
                            id, lastServerId, lastServerMs, now, qol.chatPreviousServerSeconds)) {
                localChat(SkyblockFlavorPolicy.previousServerLine(id, lastServerMs, now));
            }
            lastServerId = id;
            lastServerMs = now;
        });
        SkyblockFlavorPolicy.parseQueuePosition(text).ifPresent(position -> {
            if (qol.chatCommandsEnabled && qol.chatQueueEstimate) {
                SkyblockFlavorPolicy.estimateQueueSeconds(
                                lastQueuePosition, lastQueueMs, position, now)
                        .ifPresent(seconds -> localChat(
                                SkyblockFlavorPolicy.queueEstimateLine(position, seconds)));
            }
            lastQueuePosition = position;
            lastQueueMs = now;
        });
        if (MiningLeftoverPolicy.classifyChat(text) == MiningLeftoverPolicy.NotifyKind.MINESHAFT_PORTAL
                && extras.miningGlaciteEnabled
                && extras.miningGlacitePityChat) {
            String pity = SkyblockFlavorPolicy.pityChatLine(MiningLeftoverRuntime.pity());
            if (!pity.isBlank()) {
                localChat(pity);
            }
        }
        if (qol.renderOptimizerEnabled
                && extras.totemAnimation
                && SkyblockFlavorPolicy.isAbsorbChat(text)) {
            playTotem();
        }
    }

    public static Component rewriteChat(Component message) {
        if (message == null) {
            return null;
        }
        QolSkyblockExtras extras = extras();
        return SkyblockFlavorPolicy.rewriteScathaPetDrop(
                        extras.miningScathaEnabled && extras.miningScathaPetRarity,
                        message.getString())
                .map(text -> (Component) Component.literal(text))
                .orElse(message);
    }

    static void onDetectorHits(int previous, int next) {
        QolSkyblockExtras extras = extras();
        if (!extras.miningHelpersEnabled
                || !extras.miningHelpersDetectorSolver
                || !SkyblockFlavorPolicy.shouldPingDetector(previous, next)) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastDetectorPingMs < 750L) {
            return;
        }
        lastDetectorPingMs = now;
        MiningLeftoverRuntime.showTitle(
                "Treasure Found!",
                extras.miningHelpersDetectorTitle,
                extras.miningHelpersDetectorDing);
        if (!extras.miningHelpersDetectorTitle && extras.miningHelpersDetectorDing) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null) {
                client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.4F);
            }
        }
    }

    public static BlockState rewriteCarpet(BlockState original) {
        if (original == null || original.isAir()) {
            return null;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.miningHelpersEnabled || !extras.miningHelpersRedCarpets) {
            return null;
        }
        var key = BuiltInRegistries.BLOCK.getKey(original.getBlock());
        String id = key == null ? "" : key.toString();
        if (!SkyblockFlavorPolicy.recolorDwarvenCarpet(
                true,
                SkyblockFlavorPolicy.isDwarvenMines(SkyBlockAreaDetector.detect().displayName()),
                id)) {
            return null;
        }
        Identifier redId = Identifier.tryParse("minecraft:red_carpet");
        var redBlock = redId == null ? null : BuiltInRegistries.BLOCK.getValue(redId);
        if (redBlock == null || redBlock == Blocks.AIR) {
            return null;
        }
        BlockState red = redBlock.defaultBlockState();
        return red == original ? null : red;
    }

    static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        String area = SkyBlockAreaDetector.detect().displayName()
                + " "
                + MiningLeftoverRuntime.scoreboardText();
        boolean corpses = extras.miningGlaciteEnabled
                && extras.miningGlaciteCorpseWaypoints
                && SkyblockFlavorPolicy.isMineshaftArea(area);
        boolean rats = qol.worldScannerEnabled
                && qol.worldScannerRatHitboxes
                && SkyblockFlavorPolicy.isHubIsland(area);
        if (!corpses && !rats) {
            return;
        }
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        AABB search = client.player.getBoundingBox().inflate(48.0D, 24.0D, 48.0D);
        for (Entity entity : client.level.getEntities(client.player, search)) {
            if (corpses
                    && entity instanceof ArmorStand stand
                    && SkyblockFlavorPolicy.isCorpseStand(stand.getName().getString())) {
                Gizmos.cuboid(interpolatedBox(stand, partialTick, 0.15D), GizmoStyle.stroke(0xFF55FFFF, 2.0F))
                        .setAlwaysOnTop();
            }
            if (rats
                    && entity instanceof Zombie zombie
                    && SkyblockFlavorPolicy.isHubRat(true, zombie.isBaby())) {
                Gizmos.cuboid(interpolatedBox(zombie, partialTick, 0.08D), GizmoStyle.stroke(0xFFFFAA00, 2.0F))
                        .setAlwaysOnTop();
            }
        }
    }

    static boolean hideImplosion(String particleId, double x, double y, double z) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        if (!qol.renderOptimizerEnabled || !extras.hideImplosionParticles) {
            return false;
        }
        if (!SkyblockFlavorPolicy.isImplosionParticle(particleId)) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            return false;
        }
        refreshImplosionHolders(client);
        for (Vec3 holder : IMPLOSION_HOLDERS) {
            double dx = holder.x - x;
            double dy = holder.y - y;
            double dz = holder.z - z;
            if (SkyblockFlavorPolicy.hideImplosion(true, particleId, true, dx * dx + dy * dy + dz * dz)) {
                return true;
            }
        }
        return false;
    }

    private static void refreshImplosionHolders(Minecraft client) {
        int tick = client.player.tickCount;
        if (implosionCacheTick == tick) {
            return;
        }
        implosionCacheTick = tick;
        IMPLOSION_HOLDERS.clear();
        AABB search = client.player.getBoundingBox().inflate(8.0D);
        for (Player player : client.level.getEntitiesOfClass(Player.class, search)) {
            if (!SkyblockFlavorPolicy.isWitherBlade(
                    AutoClickerItemIdentity.skyBlockId(player.getMainHandItem()))) {
                continue;
            }
            IMPLOSION_HOLDERS.add(player.position());
        }
    }

    private static AABB interpolatedBox(Entity entity, float partialTick, double inflate) {
        EntityLerpPolicy.Offset offset = EntityLerpPolicy.renderOffset(
                entity.getX(), entity.getY(), entity.getZ(),
                entity.xo, entity.yo, entity.zo,
                partialTick);
        return entity.getBoundingBox().move(offset.x(), offset.y(), offset.z()).inflate(inflate);
    }

    public static Component rewriteNameTag(Component component) {
        if (component == null) {
            return null;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        if (!qol.renderOptimizerEnabled || !extras.mobIcons) {
            return component;
        }
        String original = component.getString();
        String rewritten = SkyblockFlavorPolicy.rewriteMobIcons(true, original);
        if (rewritten.equals(original)) {
            return component;
        }
        return Component.literal(rewritten);
    }

    public static boolean quickJoinEnabled() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return qol.chatCommandsEnabled && qol.chatQuickJoin;
    }

    public static String quickJoinLabel() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        return SkyblockFlavorPolicy.quickJoinLabel(qol.chatQuickJoinText, qol.chatQuickJoinIp);
    }

    public static String quickJoinIp() {
        return SkyblockFlavorPolicy.sanitizeQuickJoinIp(
                RotClientClient.qolConfigPublic().chatQuickJoinIp);
    }

    public static void connectQuickJoin(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        String ip = quickJoinIp();
        ServerData data = new ServerData("Rot Client", ip, ServerData.Type.OTHER);
        data.setResourcePackStatus(ServerData.ServerPackStatus.ENABLED);
        ConnectScreen.startConnecting(
                parent,
                client,
                ServerAddress.parseString(ip),
                data,
                false,
                null);
    }

    static String ministerName() {
        return ministerName;
    }

    private static void playTotem() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        client.particleEngine.createTrackingEmitter(player, ParticleTypes.TOTEM_OF_UNDYING, 30);
        client.level.playLocalSound(
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.TOTEM_USE,
                player.getSoundSource(),
                1.0F,
                1.0F,
                false);
    }

    private static Map<String, Integer> inventoryKeyCounts(LocalPlayer player) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (player == null) {
            return counts;
        }
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            String id = AutoClickerItemIdentity.skyBlockId(stack);
            if (id == null || id.isBlank()) {
                continue;
            }
            counts.merge(id.toUpperCase(), Math.max(1, stack.getCount()), Integer::sum);
        }
        return counts;
    }

    private static void localChat(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null && text != null && !text.isBlank()) {
            client.player.sendSystemMessage(Component.literal("§e" + text));
        }
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
