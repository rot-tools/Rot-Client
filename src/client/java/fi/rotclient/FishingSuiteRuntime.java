package fi.rotclient;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combined fishing suite: creatures, hotspots, trophy, bait/totem, and HUD.
 */
public final class FishingSuiteRuntime {
    private static final Map<Integer, LiveCreature> LIVE = new LinkedHashMap<>();
    private static FishingCreaturesPolicy.Creature lastCatch;
    private static boolean doubleHookPending;
    private static int autoDelay;
    private static int baitScanTicks;
    private static Integer baitRemaining;
    private static boolean goldenActive;
    private static int goldenHits;
    private static long goldenSinceMs;
    private static String lastTrophy = "";
    private static int titleTicks;
    private static String titleText = "";
    private static long lastCapNotifyAt;
    private static long lastTimerNotifyAt;
    private static List<FishingHotspotPolicy.Point> radarTrail = List.of();
    private static FishingHotspotPolicy.RadarGuess radarGuess;
    private static final List<FishingHotspotPolicy.Circle> HOTSPOTS = new ArrayList<>();
    private static String totemLine = "";
    private static String hookTimerLine = "";
    private static boolean biteTitleLatched;
    private static FishingHotspotPolicy.Point geyser;
    private static int geyserTicks;
    private static AABB spongeBox;
    private static final List<AABB> THUNDER_SPARKS = new ArrayList<>();
    private static Set<String> previousHotspotKeys = Set.of();
    private static String lastBaitName = "";

    private record LiveCreature(FishingCreaturesPolicy.Creature creature, int entityId, long spawnMs) {
    }

    private FishingSuiteRuntime() {
    }

    static void clear() {
        LIVE.clear();
        lastCatch = null;
        doubleHookPending = false;
        autoDelay = 0;
        baitRemaining = null;
        goldenActive = false;
        goldenHits = 0;
        lastTrophy = "";
        titleTicks = 0;
        titleText = "";
        radarTrail = List.of();
        radarGuess = null;
        HOTSPOTS.clear();
        totemLine = "";
        hookTimerLine = "";
        biteTitleLatched = false;
        geyser = null;
        geyserTicks = 0;
        spongeBox = null;
        THUNDER_SPARKS.clear();
        previousHotspotKeys = Set.of();
        lastBaitName = "";
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        if (qol == null) {
            return List.of();
        }
        QolSkyblockExtras extras = qol.extras();
        List<String> lines = new ArrayList<>();
        String helper = FishingHelperRuntime.hudLine();
        if (!helper.isBlank()) {
            lines.add(helper);
        }
        if (qol.fishingHelperEnabled && qol.fishingHelperHookTimerHud && !hookTimerLine.isBlank()) {
            lines.add(hookTimerLine);
        }
        lines.addAll(FishingCreaturesPolicy.hudLines(
                extras.fishingCreaturesEnabled && extras.fishingCreaturesHud,
                LIVE.size(),
                oldestAgeMs(),
                extras.fishingCreaturesTimerLength,
                lastCatch == null ? lastTrophy : lastCatch.name(),
                doubleHookPending));
        String bait = FishingToolsPolicy.baitHud(
                extras.fishingToolsEnabled && extras.fishingToolsBaitHud,
                baitRemaining,
                extras.fishingToolsNoBaitWarn);
        if (!bait.isBlank()) {
            lines.add(bait);
        }
        String golden = FishingTrophyPolicy.goldenHud(
                extras.fishingTrophyEnabled && extras.fishingTrophyGoldenTimer,
                goldenActive,
                goldenHits,
                goldenActive ? System.currentTimeMillis() - goldenSinceMs : 0L);
        if (!golden.isBlank()) {
            lines.add(golden);
        }
        if (extras.fishingToolsEnabled && extras.fishingToolsTotemHud && !totemLine.isBlank()) {
            lines.add(totemLine);
        }
        return lines;
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        if (qol == null) {
            return false;
        }
        QolSkyblockExtras extras = qol.extras();
        return qol.fishingHelperEnabled
                || extras.fishingCreaturesEnabled
                || extras.fishingTrophyEnabled
                || extras.fishingToolsEnabled;
    }

    static boolean allowGameMessage(Component message) {
        if (message == null) {
            return true;
        }
        String stripped = FishingCreaturesPolicy.strip(message.getString());
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        FishingTrophyPolicy.Catch trophy = FishingTrophyPolicy.parseCatch(stripped);
        if (extras.fishingTrophyEnabled
                && FishingTrophyPolicy.shouldHideCatch(
                        extras.fishingTrophyFilterChat, trophy, extras.fishingTrophyMinRarity)) {
            onChat(message);
            return false;
        }
        FishingCreaturesPolicy.Creature spawn = FishingCreaturesPolicy.matchSpawn(stripped);
        if (extras.fishingCreaturesEnabled && extras.fishingCreaturesShortenChat && spawn != null) {
            boolean doubled = doubleHookPending || FishingCreaturesPolicy.isDoubleHook(stripped);
            onChat(message);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.literal(FishingCreaturesPolicy.compactLine(spawn, doubled)));
            }
            return false;
        }
        return true;
    }

    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        String stripped = FishingCreaturesPolicy.strip(message.getString());
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        Minecraft client = Minecraft.getInstance();

        FishingCreaturesPolicy.ChatSignal signal = FishingCreaturesPolicy.inspectChat(stripped);
        if (extras.fishingCreaturesEnabled) {
            if (signal.doubleHook()) {
                doubleHookPending = true;
            }
            if (signal.spawn() != null) {
                lastCatch = signal.spawn();
                if (FishingCreaturesPolicy.shouldTitle(
                        true,
                        extras.fishingCreaturesRareAnnounce,
                        lastCatch.rarity(),
                        extras.fishingCreaturesMinRarity)) {
                    flash(lastCatch.name(), extras.fishingCreaturesRareSound, client);
                }
                if (FishingCreaturesPolicy.shouldPartyAnnounce(
                        true,
                        extras.fishingCreaturesRareParty,
                        lastCatch,
                        extras.fishingCreaturesMinRarity)
                        && client.player != null
                        && client.player.connection != null) {
                    client.player.connection.sendCommand("pc " + lastCatch.spawn());
                }
            }
        }

        if (extras.fishingTrophyEnabled) {
            FishingTrophyPolicy.Catch trophy = FishingTrophyPolicy.parseCatch(stripped);
            if (trophy != null) {
                lastTrophy = trophy.name() + " " + trophy.rarity();
                if (FishingTrophyPolicy.shouldTitle(true, extras.fishingTrophyTitles, trophy)) {
                    flash(lastTrophy, true, client);
                }
            }
            FishingTrophyPolicy.GoldenEvent golden = FishingTrophyPolicy.goldenEvent(stripped);
            if (golden != FishingTrophyPolicy.GoldenEvent.NONE && extras.fishingTrophyGoldenTimer) {
                if (golden == FishingTrophyPolicy.GoldenEvent.SPAWN) {
                    goldenActive = true;
                    goldenSinceMs = System.currentTimeMillis();
                    goldenHits = 0;
                    flash("Golden Fish", true, client);
                } else if (golden == FishingTrophyPolicy.GoldenEvent.DESPAWN) {
                    goldenActive = false;
                } else {
                    goldenHits = FishingTrophyPolicy.nextGoldenHits(golden, goldenHits);
                }
            }
        }

        if (extras.fishingToolsEnabled
                && extras.fishingToolsThunderNotify
                && FishingToolsPolicy.isBottleChargedChat(stripped)) {
            flash("Bottle charged", true, client);
        }
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        if (client == null || client.player == null || client.level == null) {
            clear();
            return;
        }
        if (titleTicks > 0) {
            titleTicks--;
        }
        if (!hudVisible(qol) && !extras.fishingHotspotsEnabled) {
            return;
        }
        pruneDead(client);
        scanWorld(client, qol, extras);
        if (autoDelay > 0) {
            autoDelay--;
        }
        maybeAutoAttack(client, extras);
        maybeBiteTitle(client, qol);
        if (++baitScanTicks >= 10) {
            baitScanTicks = 0;
            scanBait(client, extras);
        }
        if (goldenActive
                && System.currentTimeMillis() - goldenSinceMs
                > FishingTrophyPolicy.GOLDEN_DESPAWN_SECONDS * 1000L) {
            goldenActive = false;
        }
    }

    public static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.level == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Vec3 eye = player.getEyePosition(partialTick);
        if (extras.fishingCreaturesEnabled && extras.fishingCreaturesRareEsp) {
            for (LiveCreature live : LIVE.values()) {
                Entity entity = client.level.getEntity(live.entityId());
                if (entity == null) {
                    continue;
                }
                if (!FishingCreaturesPolicy.shouldEsp(
                        true, true, live.creature(), extras.fishingCreaturesMinRarity)) {
                    continue;
                }
                if (FishingCreaturesPolicy.shouldHideCommonNametag(
                        extras.fishingCreaturesHideCommon, live.creature())) {
                    continue;
                }
                EntityLerpPolicy.Offset offset = EntityLerpPolicy.renderOffset(
                        entity.getX(), entity.getY(), entity.getZ(),
                        entity.xo, entity.yo, entity.zo,
                        partialTick);
                AABB box = entity.getBoundingBox().move(offset.x(), offset.y(), offset.z()).inflate(0.15D);
                Gizmos.cuboid(
                        box,
                        GizmoStyle.strokeAndFill(
                                extras.fishingCreaturesEspColor,
                                2.0F,
                                withAlpha(extras.fishingCreaturesEspColor, 0x44)))
                        .setAlwaysOnTop();
            }
        }
        if (extras.fishingHotspotsEnabled && extras.fishingHotspotsCircle) {
            for (FishingHotspotPolicy.Circle circle : HOTSPOTS) {
                AABB box = new AABB(
                        circle.x() - circle.radius(),
                        circle.y() - 0.2D,
                        circle.z() - circle.radius(),
                        circle.x() + circle.radius(),
                        circle.y() + 0.2D,
                        circle.z() + circle.radius());
                Gizmos.cuboid(
                        box,
                        GizmoStyle.stroke(extras.fishingHotspotsColor, 1.5F))
                        .setAlwaysOnTop();
            }
        }
        if (extras.fishingHotspotsEnabled
                && extras.fishingHotspotsTracer
                && radarGuess != null) {
            Gizmos.line(
                    eye,
                    new Vec3(
                            radarGuess.x() + radarGuess.dx(),
                            radarGuess.y() + radarGuess.dy(),
                            radarGuess.z() + radarGuess.dz()),
                    extras.fishingHotspotsColor)
                    .setAlwaysOnTop();
        }
        if (extras.fishingTrophyEnabled && extras.fishingTrophyGeyser && geyser != null) {
            AABB box = new AABB(
                    geyser.x() - 2.0D,
                    117.9D,
                    geyser.z() - 2.0D,
                    geyser.x() + 2.0D,
                    117.91D,
                    geyser.z() + 2.0D);
            Gizmos.cuboid(box, GizmoStyle.stroke(0xFF55FFFF, 2.0F)).setAlwaysOnTop();
        }
        if (extras.fishingTrophyEnabled && extras.fishingTrophySponge && spongeBox != null) {
            Gizmos.cuboid(spongeBox, GizmoStyle.stroke(0xFFFFFF55, 1.5F)).setAlwaysOnTop();
        }
        if (extras.fishingCreaturesEnabled && extras.fishingCreaturesThunderSparks) {
            for (AABB spark : THUNDER_SPARKS) {
                Gizmos.cuboid(spark, GizmoStyle.stroke(0xFFFFFF00, 1.5F)).setAlwaysOnTop();
            }
        }
    }

    static void appendTooltip(ItemStack stack, List<Component> lines) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.fishingTrophyEnabled || !extras.fishingTrophyFillet || stack == null || stack.isEmpty()) {
            return;
        }
        Integer fillet = FishingTrophyPolicy.filletMagmafish(
                AutoClickerItemIdentity.skyBlockId(stack), stack.getCount());
        if (fillet == null || lines == null) {
            return;
        }
        lines.add(Component.literal("Fillet " + fillet + " Magmafish (rarity default)")
                .withStyle(ChatFormatting.GOLD));
    }

    public static boolean shouldSuppressSound(String soundId, float pitch) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.fishingVisualsEnabled) {
            return false;
        }
        return FishingToolsPolicy.shouldMuteBanshee(extras.fishingVisualsMuteBanshee, soundId, pitch)
                || FishingToolsPolicy.shouldMuteDrake(extras.fishingVisualsMuteDrake, soundId);
    }

    static String overlayTitle() {
        return titleTicks > 0 ? titleText : "";
    }

    public static void observeParticle(ParticleOptions options, double x, double y, double z, double xs, double ys, double zs) {
        if (options == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        String key = id == null ? "" : id.toString();
        if (extras.fishingTrophyEnabled
                && extras.fishingTrophyGeyser
                && FishingTrophyPolicy.isGeyserCloud(key, y)) {
            geyser = new FishingHotspotPolicy.Point(x, y, z);
            geyserTicks = 80;
        }
        if (!extras.fishingHotspotsEnabled || !extras.fishingHotspotsRadar) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        String held = AutoClickerItemIdentity.skyBlockId(client.player.getMainHandItem());
        if (held == null || !held.toUpperCase().contains("HOTSPOT_RADAR")) {
            return;
        }
        if (!FishingHotspotPolicy.isRadarFlame(key, xs, ys, zs)) {
            return;
        }
        radarTrail = FishingHotspotPolicy.pushRadar(
                radarTrail, new FishingHotspotPolicy.Point(x, y, z));
        radarGuess = FishingHotspotPolicy.guess(radarTrail);
    }

    public static boolean shouldHideParticle(ParticleOptions options, double x, double y, double z) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (options == null || !extras.fishingHotspotsEnabled) {
            return false;
        }
        Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(options.getType());
        String key = id == null ? "" : id.toString().toLowerCase();
        if (extras.fishingTrophyEnabled
                && extras.fishingTrophyGeyser
                && geyser != null
                && FishingTrophyPolicy.isGeyserCloud(key, y)) {
            double gdx = x - geyser.x();
            double gdz = z - geyser.z();
            if (gdx * gdx + gdz * gdz <= 16.0D) {
                return true;
            }
        }
        boolean hotspotParticle = key.contains("flame") || key.contains("dust");
        if (!hotspotParticle) {
            return false;
        }
        boolean near = false;
        for (FishingHotspotPolicy.Circle circle : HOTSPOTS) {
            double dx = x - circle.x();
            double dz = z - circle.z();
            if (dx * dx + dz * dz <= circle.radius() * circle.radius()) {
                near = true;
                break;
            }
        }
        return FishingHotspotPolicy.shouldHideParticle(
                true, extras.fishingHotspotsHideParticles, near);
    }

    public static boolean shouldSuppressEntity(Entity entity) {
        if (entity == null) {
            return false;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (qol.fishingHelperEnabled
                && qol.fishingHelperHideHookNametag
                && player != null
                && entity instanceof ArmorStand stand) {
            FishingHook hook = player.fishing;
            String name = standName(stand);
            if (hook != null
                    && hook.isAlive()
                    && stand.distanceTo(hook) <= FishingHelperPolicy.BITE_RANGE + 1.0F
                    && FishingHelperPolicy.shouldHideHookNametag(true, true, name)) {
                return true;
            }
        }
        if (extras.fishingCreaturesEnabled
                && extras.fishingCreaturesHideCommon
                && entity instanceof ArmorStand commonStand) {
            String commonName = standName(commonStand);
            FishingCreaturesPolicy.Creature tagged = FishingCreaturesPolicy.matchNametag(commonName);
            if (FishingCreaturesPolicy.looksLikeCreatureHologram(commonName)
                    && FishingCreaturesPolicy.shouldHideCommonNametag(true, tagged)) {
                return true;
            }
        }
        if (!extras.fishingVisualsEnabled || player == null) {
            return false;
        }
        if (extras.fishingVisualsHideOtherBobbers && entity instanceof FishingHook hook) {
            return player.fishing != hook;
        }
        if (extras.fishingVisualsChumHider && entity instanceof ArmorStand stand) {
            String name = standName(stand).toLowerCase();
            if ((name.contains("chum") || name.contains("chumcap"))
                    && !name.contains(player.getGameProfile().name().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static void scanWorld(Minecraft client, QolUtilityConfig qol, QolSkyblockExtras extras) {
        LocalPlayer player = client.player;
        AABB search = player.getBoundingBox().inflate(48.0D);
        HOTSPOTS.clear();
        THUNDER_SPARKS.clear();
        spongeBox = null;
        totemLine = "";
        hookTimerLine = "";
        if (geyserTicks > 0) {
            geyserTicks--;
            if (geyserTicks == 0) {
                geyser = null;
            }
        }
        FishingHook hook = player.fishing;
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
            String name = standName(stand);
            if (qol.fishingHelperEnabled && hook != null && hook.isAlive()
                    && stand.distanceTo(hook) <= FishingHelperPolicy.BITE_RANGE + 1.0F) {
                Float seconds = FishingHelperPolicy.parseHookSeconds(name);
                if (seconds != null) {
                    hookTimerLine = seconds <= 0.0F ? "Hook !!!" : String.format("Hook %.1fs", seconds);
                }
            }
            if (extras.fishingCreaturesEnabled
                    && FishingCreaturesPolicy.looksLikeCreatureHologram(name)) {
                FishingCreaturesPolicy.Creature tagged = FishingCreaturesPolicy.matchNametag(name);
                boolean isNew = !LIVE.containsKey(stand.getId());
                LIVE.putIfAbsent(
                        stand.getId(),
                        new LiveCreature(tagged, stand.getId(), System.currentTimeMillis()));
                if (isNew) {
                    if (lastCatch != null && tagged.name().equals(lastCatch.name())) {
                        if (doubleHookPending) {
                            doubleHookPending = false;
                        } else {
                            lastCatch = null;
                        }
                    }
                    maybeCapAndTimer(extras, client);
                }
            }
            if (extras.fishingHotspotsEnabled && FishingHotspotPolicy.isHotspotNametag(name)) {
                HOTSPOTS.add(FishingHotspotPolicy.circleFromStand(
                        stand.getX(), stand.getY(), stand.getZ(), 8.0D));
            }
            if (extras.fishingToolsEnabled
                    && extras.fishingToolsTotemHud
                    && FishingToolsPolicy.isTotemNametag(name)) {
                Integer seconds = FishingToolsPolicy.parseTotemSeconds(name);
                if (seconds != null) {
                    totemLine = "Totem " + FishingCreaturesPolicy.formatSeconds(seconds);
                }
            }
            if (extras.fishingCreaturesEnabled
                    && extras.fishingCreaturesThunderSparks
                    && isThunderSpark(stand, name, player)) {
                THUNDER_SPARKS.add(stand.getBoundingBox().inflate(0.2D));
            }
        }
        Set<String> hotspotKeys = FishingHotspotPolicy.keys(HOTSPOTS);
        if (extras.fishingHotspotsEnabled
                && extras.fishingHotspotsDespawn
                && FishingHotspotPolicy.vanished(previousHotspotKeys, hotspotKeys)) {
            flash("Hotspot gone", true, client);
        }
        previousHotspotKeys = Set.copyOf(hotspotKeys);
        if (extras.fishingTrophyEnabled && extras.fishingTrophySponge) {
            spongeBox = nearestSponge(player);
        }
    }

    private static boolean isThunderSpark(ArmorStand stand, String name, LocalPlayer player) {
        if (FishingCreaturesPolicy.looksLikeCreatureHologram(name)) {
            return false;
        }
        ItemStack helmet = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (helmet.isEmpty() || !helmet.is(Items.PLAYER_HEAD)) {
            return false;
        }
        boolean thunderLive = false;
        for (LiveCreature live : LIVE.values()) {
            if ("Thunder".equals(live.creature().name())) {
                thunderLive = true;
                break;
            }
        }
        return thunderLive && player.distanceTo(stand) <= 12.0F;
    }

    private static AABB nearestSponge(LocalPlayer player) {
        BlockPos origin = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        AABB best = null;
        double bestDist = Double.MAX_VALUE;
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    if (dx * dx + dz * dz > 16) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    var state = player.level().getBlockState(cursor);
                    if (state.is(Blocks.SPONGE) || state.is(Blocks.WET_SPONGE)) {
                        double dist = player.distanceToSqr(
                                cursor.getX() + 0.5D, cursor.getY() + 0.5D, cursor.getZ() + 0.5D);
                        if (dist < bestDist) {
                            bestDist = dist;
                            best = new AABB(cursor);
                        }
                    }
                }
            }
        }
        return best;
    }

    private static void maybeCapAndTimer(QolSkyblockExtras extras, Minecraft client) {
        long now = System.currentTimeMillis();
        if (FishingCreaturesPolicy.shouldCapNotify(true, extras.fishingCreaturesCapNotify, LIVE.size())
                && now - lastCapNotifyAt > 2000L) {
            lastCapNotifyAt = now;
            flash("Sea creature cap", true, client);
        }
        if (FishingCreaturesPolicy.shouldTimerNotify(
                true,
                extras.fishingCreaturesTimerNotify,
                LIVE.size(),
                oldestAgeMs(),
                extras.fishingCreaturesTimerLength)
                && now - lastTimerNotifyAt > 2000L) {
            lastTimerNotifyAt = now;
            flash("Barn timer", true, client);
        }
    }

    private static void maybeAutoAttack(Minecraft client, QolSkyblockExtras extras) {
        Screen screen = client.gui == null ? null : client.gui.screen();
        boolean looking = false;
        if (client.hitResult != null && client.hitResult.getType() == HitResult.Type.ENTITY) {
            Entity hit = ((EntityHitResult) client.hitResult).getEntity();
            looking = LIVE.containsKey(hit.getId());
            if (!looking && client.level != null) {
                for (LiveCreature live : LIVE.values()) {
                    Entity hologram = client.level.getEntity(live.entityId());
                    if (hologram != null && hologram.distanceTo(hit) <= 4.0F) {
                        looking = true;
                        break;
                    }
                }
            }
        }
        if (FishingCreaturesPolicy.shouldAutoAttack(
                extras.fishingCreaturesEnabled,
                extras.fishingCreaturesAutoAttack,
                looking,
                screen != null && !screen.isPauseScreen())
                && autoDelay <= 0) {
            ClickPulseHelper.pulseAttack(client);
            autoDelay = FishingCreaturesPolicy.clampAutoDelay(extras.fishingCreaturesAutoDelay);
        }
    }

    private static void maybeBiteTitle(Minecraft client, QolUtilityConfig qol) {
        LocalPlayer player = client.player;
        boolean holdingRod = player.getMainHandItem().is(net.minecraft.world.item.Items.FISHING_ROD);
        FishingHook hook = player.fishing;
        boolean bite = hook != null && hook.isAlive() && biteNearby(player, hook);
        if (FishingHelperPolicy.shouldShowBiteTitle(
                qol.fishingHelperEnabled,
                qol.fishingHelperBiteTitle,
                holdingRod,
                hook != null && hook.isAlive(),
                bite)) {
            if (!biteTitleLatched) {
                flash("Reel!", qol.fishingHelperBiteSound, client);
                biteTitleLatched = true;
            }
        } else if (!bite) {
            biteTitleLatched = false;
        }
    }

    private static boolean biteNearby(LocalPlayer player, FishingHook hook) {
        AABB box = hook.getBoundingBox().inflate(FishingHelperPolicy.BITE_RANGE);
        for (ArmorStand stand : player.level().getEntitiesOfClass(ArmorStand.class, box)) {
            if (FishingHelperPolicy.isBiteHologram(standName(stand))
                    && stand.distanceTo(hook) <= FishingHelperPolicy.BITE_RANGE) {
                return true;
            }
        }
        return false;
    }

    private static void scanBait(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.fishingToolsEnabled) {
            baitRemaining = null;
            return;
        }
        LocalPlayer player = client.player;
        ItemStack stack = player.getMainHandItem();
        if (!FishingToolsPolicy.isBaitName(stack.getHoverName().getString())
                && !FishingToolsPolicy.isThunderBottleId(AutoClickerItemIdentity.skyBlockId(stack))) {
            stack = player.getOffhandItem();
        }
        List<String> lore = InventoryChromeRuntime.loreLines(stack);
        baitRemaining = FishingToolsPolicy.parseBaitRemaining(lore);
        String baitName = stack.getHoverName() == null ? "" : stack.getHoverName().getString();
        if (baitRemaining == null && FishingToolsPolicy.isBaitName(baitName)) {
            baitRemaining = stack.getCount();
        }
        if (extras.fishingToolsBaitChange && FishingToolsPolicy.isBaitName(baitName)) {
            if (FishingToolsPolicy.baitChanged(lastBaitName, baitName)) {
                flash("Bait " + FishingCreaturesPolicy.strip(baitName), true, client);
            }
            lastBaitName = baitName;
        } else if (!FishingToolsPolicy.isBaitName(baitName)) {
            lastBaitName = "";
        }
    }

    private static void pruneDead(Minecraft client) {
        Iterator<Map.Entry<Integer, LiveCreature>> it = LIVE.entrySet().iterator();
        while (it.hasNext()) {
            Entity entity = client.level.getEntity(it.next().getKey());
            if (entity == null || !entity.isAlive()) {
                it.remove();
            }
        }
    }

    private static long oldestAgeMs() {
        long now = System.currentTimeMillis();
        long oldest = 0L;
        for (LiveCreature live : LIVE.values()) {
            oldest = Math.max(oldest, now - live.spawnMs());
        }
        return oldest;
    }

    private static void flash(String text, boolean sound, Minecraft client) {
        titleText = text == null ? "" : text;
        titleTicks = 40;
        if (sound && client != null && client.player != null) {
            client.player.playSound(SoundEvents.ARROW_HIT_PLAYER, 0.8F, 1.2F);
        }
    }

    private static String standName(ArmorStand stand) {
        if (stand.getCustomName() != null) {
            return stand.getCustomName().getString();
        }
        return stand.getName().getString();
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
