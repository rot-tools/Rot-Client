package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Giant;
import net.minecraft.world.entity.monster.cubemob.MagmaCube;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Minecraft bridge for the Kuudra Tools 3D waypoints and hitboxes. Policy and
 * bundled JSON stay Minecraft-free.
 */
public final class IotaKuudraRuntime {
    private static IotaKuudraPolicy.Phase phase = IotaKuudraPolicy.Phase.NONE;
    private static boolean inKuudra;
    private static final List<IotaKuudraPolicy.SupplyCrate> supplies = new ArrayList<>();
    private static final List<IotaKuudraPolicy.Pile> remainingPiles = new ArrayList<>();
    private static int missingPre;
    private static IotaKuudraPolicy.PearlArea pearlArea;
    private static int lastSupplyProgress;
    private static int lastSupplyProgressIndex = -1;
    private static long supplyProgressStartMs = -1L;
    private static long lastSupplyProgressUpdateMs = -1L;
    private static boolean stunPhase;
    private static boolean eaten;
    private static final List<IchorPool> ichorPools = new ArrayList<>();
    private static int highlightedGiantId = -1;
    private static int kuudraBossId = -1;
    private static IotaKuudraPolicy.AlertLevel giantAlert = IotaKuudraPolicy.AlertLevel.NONE;
    private static String overlayTitle = "";
    private static long overlayUntilMs;
    private static int tickCounter;
    private static long runStartedAtMs;
    private static PendingChest pendingChest;
    private static int chestWindowId = -1;
    private static boolean itemsRerolled;
    private static boolean shardRerolled;
    private static KuudraAlertPolicy.Snapshot alerts = KuudraAlertPolicy.Snapshot.idle();
    private static boolean announcedOwnFresh;

    private record PendingChest(IotaKuudraPolicy.ChestKind kind, long lootCoins, long costCoins) {
    }

    private record IchorPool(IotaKuudraPolicy.Vec3d center, long expiresAtMs) {
    }

    private record BuildPile(IotaKuudraPolicy.Vec3d position, String displayName, int progress) {
    }

    private IotaKuudraRuntime() {
    }

    static void clear() {
        phase = IotaKuudraPolicy.Phase.NONE;
        inKuudra = false;
        supplies.clear();
        remainingPiles.clear();
        missingPre = 0;
        pearlArea = null;
        resetPearlProgress();
        stunPhase = false;
        eaten = false;
        ichorPools.clear();
        highlightedGiantId = -1;
        kuudraBossId = -1;
        giantAlert = IotaKuudraPolicy.AlertLevel.NONE;
        overlayTitle = "";
        overlayUntilMs = 0L;
        tickCounter = 0;
        runStartedAtMs = 0L;
        pendingChest = null;
        chestWindowId = -1;
        itemsRerolled = false;
        shardRerolled = false;
        alerts = KuudraAlertPolicy.clearRun();
        announcedOwnFresh = false;
    }

    static String overlayTitle() {
        if (overlayUntilMs > 0L && System.currentTimeMillis() > overlayUntilMs) {
            overlayTitle = "";
        }
        return overlayTitle == null ? "" : overlayTitle;
    }

    static void onGameMessage(Component message) {
        if (message == null || !parentEnabled()) {
            return;
        }
        String raw = message.getString();
        if (IotaKuudraPolicy.isDefeat(raw) && phase.isRun()) {
            recordKuudraRun(false);
            applyPhase(IotaKuudraPolicy.Phase.NONE);
        }
        IotaKuudraPolicy.Phase next = IotaKuudraPolicy.phaseFromChat(raw);
        if (next != null) {
            applyPhase(next);
        }
        if (IotaKuudraPolicy.isInstanceTransfer(raw)) {
            ichorPools.clear();
        }
        if (extras().iotaIchorPool) {
            IotaKuudraPolicy.parseIchor(raw).ifPresent(parsed ->
                    ichorPools.add(new IchorPool(
                            IotaKuudraPolicy.ichorCenter(parsed),
                            System.currentTimeMillis() + IotaKuudraPolicy.ICHOR_DURATION_MS)));
        }
        if (IotaKuudraPolicy.isHumanCannonball(raw)) {
            stunPhase = true;
        }
        if (IotaKuudraPolicy.isPodDestroyed(raw)) {
            stunPhase = false;
            eaten = false;
        }
        IotaKuudraPolicy.parseNoPre(raw).ifPresent(call -> {
            missingPre = call.missingPreValue();
            if (extras().iotaKuudraTitles) {
                showAlertTitle(KuudraAlertPolicy.noPreTitle(call.canonicalPileName()));
            }
        });
        if (IotaKuudraPolicy.isSupplyPickup(raw) || IotaKuudraPolicy.isLocalSupplyDrop(raw)
                || IotaKuudraPolicy.SUPPLY_PLACE_PATTERN.matcher(raw).find()) {
            resetPearlProgress();
        }
        IotaKuudraPolicy.supplyProgress(raw).ifPresent(IotaKuudraRuntime::onSupplyProgress);
        IotaKuudraPolicy.chestReward(raw).ifPresent(IotaKuudraRuntime::confirmChest);
        applyAlertChat(raw);
    }

    public static void onTitle(Component title) {
        if (title == null || !parentEnabled()) {
            return;
        }
        IotaKuudraPolicy.supplyProgress(title.getString()).ifPresent(IotaKuudraRuntime::onSupplyProgress);
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras extras = extras();
        if (client == null || client.player == null || client.level == null || !extras.iotaAddonsEnabled) {
            if (phase != IotaKuudraPolicy.Phase.NONE || !ichorPools.isEmpty()) {
                clear();
            }
            return;
        }
        boolean area = IotaKuudraPolicy.isKuudraArea(SkyBlockSidebar.text());
        if (inKuudra && !area) {
            clear();
            return;
        }
        if (area && !inKuudra) {
            inKuudra = true;
            resetPiles();
            ichorPools.clear();
        }
        inKuudra = area;
        if (area && phase == IotaKuudraPolicy.Phase.NONE) {
            IotaKuudraPolicy.phaseFromScoreboard(SkyBlockSidebar.text()).ifPresent(IotaKuudraRuntime::applyPhase);
        }
        if (extras.iotaBuildInfo) {
            KuudraAlertPolicy.parseBuild(SkyBlockSidebar.text()).ifPresent(info ->
                    alerts = KuudraAlertPolicy.applyBuild(alerts, info));
        }
        long now = System.currentTimeMillis();
        alerts = KuudraAlertPolicy.prune(alerts, now);
        if (alerts.ownFreshUntilMs() <= now) {
            announcedOwnFresh = false;
        }
        tickCounter++;
        if (tickCounter % 20 == 0) {
            expireProfitSession();
        }
        if (phase == IotaKuudraPolicy.Phase.SUPPLIES && tickCounter % 2 == 0) {
            scanSupplies(client);
            pearlArea = IotaKuudraPolicy.findPearlArea(
                    IotaKuudraData.pearls(),
                    pearlArea,
                    client.player.getX(),
                    client.player.getZ());
        }
        if (phase == IotaKuudraPolicy.Phase.SUPPLIES && tickCounter % 5 == 0) {
            scanCompletedPiles(client);
        }
        if (stunPhase && client.player.getY() < IotaKuudraPolicy.STUN_EATEN_Y) {
            stunPhase = false;
            eaten = true;
        }
        if (lastSupplyProgressIndex >= 0 && lastSupplyProgressUpdateMs > 0L
                && System.currentTimeMillis() - lastSupplyProgressUpdateMs > 900L) {
            resetPearlProgress();
        }
        if (overlayUntilMs > 0L && System.currentTimeMillis() > overlayUntilMs) {
            overlayTitle = "";
        }
        if (inKuudra && extras.iotaKuudraHitbox) {
            MagmaCube cube = findKuudra(client);
            kuudraBossId = cube == null ? -1 : cube.getId();
        } else {
            kuudraBossId = -1;
        }
    }

    public static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        QolSkyblockExtras extras = extras();
        if (!extras.iotaAddonsEnabled || player == null || client.level == null) {
            return;
        }
        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (phase == IotaKuudraPolicy.Phase.SUPPLIES) {
            renderSupplies(client, player, extras);
            renderPiles(extras);
            renderPearls(player, extras);
            renderGiantAlert(client, extras, partial);
        }
        if (phase == IotaKuudraPolicy.Phase.BUILD) {
            renderBuild(client, extras);
        }
        if (phase == IotaKuudraPolicy.Phase.BUILD
                || phase == IotaKuudraPolicy.Phase.STUN
                || phase == IotaKuudraPolicy.Phase.EATEN
                || phase == IotaKuudraPolicy.Phase.DPS) {
            renderStun(player, extras, partial);
        }
        if (phase.isRun()) {
            renderKuudraHitbox(client, extras);
        }
        if (IotaKuudraPolicy.etherwarpVisibleInPhase(phase)) {
            renderEtherwarp(player, extras);
        }
        renderIchor(extras);
    }

    private static void applyPhase(IotaKuudraPolicy.Phase next) {
        if (next == IotaKuudraPolicy.Phase.COMPLETED && phase.isRun()) {
            recordKuudraRun(true);
        }
        if (next == IotaKuudraPolicy.Phase.NONE || next == IotaKuudraPolicy.Phase.COMPLETED) {
            boolean keepIchor = next == IotaKuudraPolicy.Phase.COMPLETED;
            List<IchorPool> keep = keepIchor ? List.copyOf(ichorPools) : List.of();
            boolean titles = extras().iotaKuudraTitles && next != phase;
            String title = titles ? KuudraAlertPolicy.titleForPhase(next) : "";
            clear();
            phase = next;
            ichorPools.addAll(keep);
            if (!title.isBlank()) {
                showAlertTitle(title);
            }
            return;
        }
        if (next == IotaKuudraPolicy.Phase.SUPPLIES) {
            resetPiles();
            resetPearlProgress();
            stunPhase = false;
            eaten = false;
            if (runStartedAtMs <= 0L) {
                runStartedAtMs = System.currentTimeMillis();
            }
        }
        if (next != phase && extras().iotaKuudraTitles) {
            showAlertTitle(KuudraAlertPolicy.titleForPhase(next));
        }
        phase = next;
    }

    private static void resetPiles() {
        remainingPiles.clear();
        remainingPiles.addAll(IotaKuudraData.piles());
        missingPre = 0;
    }

    private static void resetPearlProgress() {
        lastSupplyProgress = 0;
        lastSupplyProgressIndex = -1;
        supplyProgressStartMs = -1L;
        lastSupplyProgressUpdateMs = -1L;
        highlightedGiantId = -1;
        giantAlert = IotaKuudraPolicy.AlertLevel.NONE;
    }

    private static void onSupplyProgress(int progress) {
        int previousIndex = lastSupplyProgressIndex;
        lastSupplyProgress = progress;
        lastSupplyProgressIndex = IotaKuudraPolicy.progressIndex(progress);
        long now = System.currentTimeMillis();
        lastSupplyProgressUpdateMs = now;
        if (progress <= 0) {
            supplyProgressStartMs = now;
        } else if (lastSupplyProgressIndex >= 0
                && (supplyProgressStartMs < 0L || previousIndex != lastSupplyProgressIndex)) {
            supplyProgressStartMs = now - IotaKuudraPolicy.targetTimeMs(lastSupplyProgressIndex);
        }
        if (extras().iotaPearlWaypoints
                && previousIndex >= 0
                && lastSupplyProgressIndex >= 0
                && previousIndex != lastSupplyProgressIndex) {
            tryPearlThrowAlert(previousIndex, lastSupplyProgressIndex);
        }
        updateGiantAlert(progress);
    }

    private static void scanSupplies(Minecraft client) {
        supplies.clear();
        for (Giant giant : client.level.getEntitiesOfClass(Giant.class, arenaSearch(client.player))) {
            if (giant.getY() < IotaKuudraPolicy.SUPPLY_CARRIER_MAX_Y
                    && holdingSkull(giant.getMainHandItem())) {
                supplies.add(IotaKuudraPolicy.crateFromGiant(
                        giant.getX(), giant.getZ(), giant.getYRot(), giant.getId()));
            }
        }
        if (missingPre == 0) {
            IotaKuudraPolicy.preSpotFromPlayer(new IotaKuudraPolicy.Vec3d(
                    client.player.getX(), client.player.getY(), client.player.getZ()))
                    .ifPresent(spot -> missingPre = spot.missingPreValue());
        }
    }

    private static void scanCompletedPiles(Minecraft client) {
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, arenaSearch(client.player))) {
            if (stand.getCustomName() == null) {
                continue;
            }
            if (!stand.getCustomName().getString().contains("SUPPLIES RECEIVED")) {
                continue;
            }
            IotaKuudraPolicy.Vec3d pos = new IotaKuudraPolicy.Vec3d(stand.getX(), stand.getY(), stand.getZ());
            remainingPiles.removeIf(pile -> pile.isNearby(pos));
        }
    }

    private static void renderSupplies(Minecraft client, LocalPlayer player, QolSkyblockExtras extras) {
        if (!extras.iotaSupplyWaypoints) {
            return;
        }
        List<Zombie> zombies = extras.iotaSupplyHitbox ? zombies(client) : List.of();
        for (IotaKuudraPolicy.SupplyCrate crate : supplies) {
            AABB box = aabb(IotaKuudraPolicy.supplyBox(crate.position(), IotaKuudraPolicy.SUPPLY_BOX_SIZE));
            drawBoth(box, IotaKuudraPolicy.SUPPLY_COLOR);
            drawBeam(box, IotaKuudraPolicy.SUPPLY_BEAM_HEIGHT, IotaKuudraPolicy.SUPPLY_COLOR);
            if (extras.iotaSupplyPullCircle) {
                IotaKuudraPolicy.Vec3d center = IotaKuudraPolicy.supplyPullCenter(crate.position());
                int color = pullActive(player, crate) ? IotaKuudraPolicy.GREEN : IotaKuudraPolicy.SUPPLY_COLOR;
                drawCircle(center, IotaKuudraPolicy.SUPPLY_PULL_RADIUS, IotaKuudraPolicy.SUPPLY_PULL_SEGMENTS, color);
            }
            if (extras.iotaSupplyHitbox) {
                for (Zombie zombie : zombies) {
                    if (zombie.position().distanceToSqr(new Vec3(
                            crate.position().x(), crate.position().y(), crate.position().z()))
                            >= IotaKuudraPolicy.SUPPLY_ZOMBIE_RANGE_SQ) {
                        continue;
                    }
                    int color = zombie.distanceTo(player) > IotaKuudraPolicy.SUPPLY_ZOMBIE_GREEN_DIST
                            ? IotaKuudraPolicy.SUPPLY_COLOR
                            : IotaKuudraPolicy.GREEN;
                    drawBoth(zombie.getBoundingBox(), color);
                }
            }
        }
    }

    private static boolean pullActive(LocalPlayer player, IotaKuudraPolicy.SupplyCrate crate) {
        FishingHook hook = player.fishing;
        if (hook == null) {
            return false;
        }
        Vec3 pos = hook.position();
        return IotaKuudraPolicy.bobberInsidePull(crate.position(), pos.x, pos.y, pos.z);
    }

    private static void renderPiles(QolSkyblockExtras extras) {
        if (!extras.iotaPileWaypoints) {
            return;
        }
        for (IotaKuudraPolicy.Pile pile : remainingPiles) {
            int color = pile.isNoPrePile(missingPre)
                    ? IotaKuudraPolicy.PILE_NO_PRE_COLOR
                    : IotaKuudraPolicy.PILE_NORMAL_COLOR;
            AABB box = aabb(IotaKuudraPolicy.unitCubeFromLowerCorner(pile.position()));
            drawBoth(box, color);
            drawBeam(box, IotaKuudraPolicy.PILE_BEAM_HEIGHT, color);
            if (extras.iotaPileNames) {
                label(pile.name(), pile.position().x(), pile.position().y() + IotaKuudraPolicy.PILE_NAME_Y,
                        pile.position().z(), IotaKuudraPolicy.withOpacity(color, 100.0F), 0.75F);
            }
        }
    }

    private static void renderPearls(LocalPlayer player, QolSkyblockExtras extras) {
        if (!extras.iotaPearlWaypoints || pearlArea == null) {
            return;
        }
        IotaKuudraPolicy.Vec3d standCenter = null;
        IotaKuudraPolicy.PearlWaypoint standWaypoint = null;
        for (IotaKuudraPolicy.PearlWaypoint waypoint : pearlArea.waypoints()) {
            if (waypoint.hasStandBlock()) {
                standCenter = waypoint.standBlock().add(new IotaKuudraPolicy.Vec3d(0.5D, 0.5D, 0.5D));
                standWaypoint = waypoint;
                break;
            }
        }
        IotaKuudraPolicy.Vec3d playerPos = new IotaKuudraPolicy.Vec3d(player.getX(), player.getY(), player.getZ());
        for (IotaKuudraPolicy.PearlWaypoint waypoint : pearlArea.waypoints()) {
            if (!waypoint.shouldShow(missingPre)) {
                continue;
            }
            IotaKuudraPolicy.Vec3d target = extras.iotaPearlWaypoints
                    ? IotaKuudraPolicy.dynamicPearlTarget(
                    waypoint.target(), playerPos, standCenter,
                    pearlArea.invertForwardBackward(), pearlArea.invertLeftRight())
                    : waypoint.target();
            if (IotaKuudraPolicy.skipPearlTarget(target)) {
                continue;
            }
            AABB box = aabb(IotaKuudraPolicy.pearlBox(target, waypoint.size(), IotaKuudraPolicy.PEARL_SIZE_ADJUSTMENT));
            int color = waypoint.colorRgb() == 0 ? IotaKuudraPolicy.PEARL_DEFAULT_COLOR : waypoint.colorRgb();
            if ((color & 0xFF000000) == 0) {
                color = IotaKuudraPolicy.applyAlpha(color, 1.0F);
            }
            boolean ready = false;
            int targetIndex = -1;
            String label = waypoint.label();
            if (label != null && !label.isEmpty()) {
                targetIndex = adjustedTargetIndex(label);
                if (targetIndex >= 0 && lastSupplyProgress > 0 && lastSupplyProgressIndex >= 0
                        && targetIndex - lastSupplyProgressIndex <= 0) {
                    color = IotaKuudraPolicy.GREEN;
                    ready = true;
                }
            }
            drawFilled(box, IotaKuudraPolicy.withOpacity(color, IotaKuudraPolicy.PEARL_SOLID_OPACITY));
            if (label != null && !label.isEmpty()) {
                String text = adjustedPercentage(label).replace("%", "");
                label(text, target.x() - 0.5D, target.y() - 1.1D, target.z() - 0.5D, 0xFFFFFFFF, 0.85F);
                long remaining = remainingTimerMs(targetIndex);
                if (remaining > 0L) {
                    label(String.format(java.util.Locale.ROOT, "%.1fs", remaining / 1000.0D),
                            target.x() - 0.5D, target.y() - 3.9D, target.z() - 0.5D,
                            timerColor(remaining, targetIndex), 0.72F);
                } else if (ready) {
                    label("READY", target.x() - 0.5D, target.y() - 3.9D, target.z() - 0.5D,
                            IotaKuudraPolicy.GREEN, 0.72F);
                }
            }
        }
        if (standWaypoint != null && standWaypoint.hasStandBlock()) {
            IotaKuudraPolicy.Vec3d stand = standWaypoint.standBlock();
            drawOutline(new AABB(stand.x(), stand.y(), stand.z(),
                    stand.x() + 1.0D, stand.y() + 1.0D, stand.z() + 1.0D),
                    standWaypoint.colorRgb() == 0
                            ? IotaKuudraPolicy.PEARL_DEFAULT_COLOR
                            : IotaKuudraPolicy.applyAlpha(standWaypoint.colorRgb(), 1.0F));
        }
    }

    private static void renderBuild(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.iotaBuildWaypoints) {
            return;
        }
        List<BuildPile> piles = new ArrayList<>();
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, arenaSearch(client.player))) {
            if (stand.getCustomName() == null) {
                continue;
            }
            String name = stand.getCustomName().getString();
            IotaKuudraPolicy.parseBuildProgress(name).ifPresent(progress ->
                    piles.add(new BuildPile(
                            new IotaKuudraPolicy.Vec3d(stand.getX(), stand.getY(), stand.getZ()),
                            name, progress)));
        }
        for (BuildPile pile : piles) {
            int color = IotaKuudraPolicy.withOpacity(
                    IotaKuudraPolicy.buildColor(pile.progress()), IotaKuudraPolicy.BUILD_OPACITY);
            AABB box = aabb(IotaKuudraPolicy.unitCubeFromLowerCorner(
                    new IotaKuudraPolicy.Vec3d(pile.position().x() - 0.5D, pile.position().y(), pile.position().z() - 0.5D)));
            drawBoth(box, color);
            drawBeam(box, IotaKuudraPolicy.BUILD_BEAM_HEIGHT, color);
            label(pile.displayName(), pile.position().x(), pile.position().y() + 2.0D, pile.position().z(),
                    IotaKuudraPolicy.buildColor(pile.progress()), 0.75F);
        }
    }

    private static void renderStun(LocalPlayer player, QolSkyblockExtras extras, float partial) {
        if (!extras.iotaStunWaypoints || (!stunPhase && !eaten)) {
            return;
        }
        IotaKuudraPolicy.Vec3d pod = IotaKuudraPolicy.stunPod(extras.iotaStunPod);
        double x = player.xo + (player.getX() - player.xo) * partial;
        double y = player.yo + (player.getY() - player.yo) * partial;
        double z = player.zo + (player.getZ() - player.zo) * partial;
        IotaKuudraPolicy.Vec3d waypoint = IotaKuudraPolicy.stunWaypoint(
                pod, new IotaKuudraPolicy.Vec3d(x, y, z), stunPhase, eaten);
        if (waypoint == null) {
            return;
        }
        drawOutline(aabb(IotaKuudraPolicy.stunBox(waypoint)), IotaKuudraPolicy.STUN_COLOR);
    }

    private static void renderKuudraHitbox(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.iotaKuudraHitbox || kuudraBossId < 0) {
            return;
        }
        if (!(client.level.getEntity(kuudraBossId) instanceof MagmaCube boss)) {
            return;
        }
        float partial = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        IotaKuudraPolicy.Aabb interpolated = IotaKuudraPolicy.interpolatedEntityBox(
                boss.getX(), boss.getY(), boss.getZ(),
                boss.xo, boss.yo, boss.zo,
                boss.getBbWidth(), boss.getBbHeight(), partial);
        drawOutline(aabb(interpolated), IotaKuudraPolicy.KUUDRA_HITBOX_COLOR);
    }

    private static void renderEtherwarp(LocalPlayer player, QolSkyblockExtras extras) {
        if (!extras.iotaEtherwarpHelper) {
            return;
        }
        Set<String> seen = new HashSet<>();
        IotaKuudraPolicy.Vec3d playerPos = new IotaKuudraPolicy.Vec3d(player.getX(), player.getY(), player.getZ());
        for (IotaKuudraPolicy.EtherCategory category : IotaKuudraData.etherwarp()) {
            if (!category.enabled()) {
                continue;
            }
            for (IotaKuudraPolicy.EtherWaypoint waypoint : category.waypoints()) {
                if (!waypoint.shouldShowInPhase(phase)) {
                    continue;
                }
                for (int i = 0; i < waypoint.positions().size(); i++) {
                    IotaKuudraPolicy.Vec3d pos = waypoint.positions().get(i);
                    if (waypoint.maxRenderDistance() > 0.0F
                            && playerPos.distanceToSqr(pos)
                            > waypoint.maxRenderDistance() * waypoint.maxRenderDistance()) {
                        continue;
                    }
                    String id = waypoint.name() + "_" + pos.x() + "_" + pos.y() + "_" + pos.z();
                    if (!seen.add(id)) {
                        continue;
                    }
                    AABB box = aabb(IotaKuudraPolicy.etherwarpBox(
                            waypoint.shape(), pos, waypoint.boxMin(), waypoint.boxMax()));
                    int color = IotaKuudraPolicy.applyAlpha(waypoint.colorForIndex(i), waypoint.alpha());
                    String style = waypoint.renderStyle() == null
                            ? "OUTLINE"
                            : waypoint.renderStyle().toUpperCase();
                    if ("SOLID".equals(style)) {
                        drawFilled(box, color);
                    } else {
                        if ("BOTH".equals(style)) {
                            drawFilled(box, IotaKuudraPolicy.applyAlpha(waypoint.colorForIndex(i), waypoint.alpha() * 0.5F));
                        }
                        int layers = IotaKuudraPolicy.outlineLayers(waypoint.lineWidth());
                        drawOutline(box, color);
                        for (int layer = 1; layer < layers; layer++) {
                            drawOutline(box.inflate(IotaKuudraPolicy.ETHERWARP_OUTLINE_STEP * layer), color);
                        }
                    }
                }
            }
        }
    }

    private static void renderIchor(QolSkyblockExtras extras) {
        if (!extras.iotaIchorPool) {
            return;
        }
        long now = System.currentTimeMillis();
        Iterator<IchorPool> iterator = ichorPools.iterator();
        while (iterator.hasNext()) {
            IchorPool pool = iterator.next();
            if (pool.expiresAtMs() <= now) {
                iterator.remove();
                continue;
            }
            drawCircle(pool.center(), IotaKuudraPolicy.ICHOR_RADIUS, IotaKuudraPolicy.ICHOR_SEGMENTS,
                    IotaKuudraPolicy.ICHOR_COLOR);
        }
    }

    private static void updateGiantAlert(int progress) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (!extras().iotaSupplyGiantHitbox || player == null || client.level == null
                || progress <= 0 || progress >= 100) {
            highlightedGiantId = -1;
            giantAlert = IotaKuudraPolicy.AlertLevel.NONE;
            return;
        }
        Vec3 eye = player.getEyePosition();
        AABB playerBox = player.getBoundingBox();
        Giant chosen = null;
        IotaKuudraPolicy.AlertLevel level = IotaKuudraPolicy.AlertLevel.NONE;
        for (IotaKuudraPolicy.SupplyCrate crate : supplies) {
            Entity entity = client.level.getEntity(crate.entityId());
            if (entity instanceof Giant giant) {
                IotaKuudraPolicy.AlertLevel next = alertFor(giant, eye, playerBox);
                if (better(next, level)) {
                    chosen = giant;
                    level = next;
                }
            }
        }
        if (chosen == null) {
            for (Giant giant : client.level.getEntitiesOfClass(Giant.class, arenaSearch(player))) {
                if (giant.getY() < IotaKuudraPolicy.SUPPLY_CARRIER_MAX_Y
                        && holdingSkull(giant.getMainHandItem())) {
                    IotaKuudraPolicy.AlertLevel next = alertFor(giant, eye, playerBox);
                    if (better(next, level)
                            || (next == level && next != IotaKuudraPolicy.AlertLevel.NONE
                            && (chosen == null || giant.distanceToSqr(player) < chosen.distanceToSqr(player)))) {
                        chosen = giant;
                        level = next;
                    }
                }
            }
        }
        if (chosen == null || level == IotaKuudraPolicy.AlertLevel.NONE) {
            highlightedGiantId = -1;
            giantAlert = IotaKuudraPolicy.AlertLevel.NONE;
            return;
        }
        if (level == IotaKuudraPolicy.AlertLevel.PRIMARY
                && (highlightedGiantId != chosen.getId() || giantAlert != IotaKuudraPolicy.AlertLevel.PRIMARY)) {
            overlayTitle = IotaKuudraPolicy.DOUBLE_PEARL_TITLE;
            overlayUntilMs = System.currentTimeMillis() + 1000L;
            if (client.gui != null && client.gui.hud != null) {
                client.gui.hud.setTitle(Component.literal(IotaKuudraPolicy.DOUBLE_PEARL_TITLE));
            }
            client.level.playSound(player, player.blockPosition(), SoundEvents.VILLAGER_NO,
                    SoundSource.PLAYERS, 1.0F, 1.15F);
        }
        highlightedGiantId = chosen.getId();
        giantAlert = level;
    }

    private static void renderGiantAlert(Minecraft client, QolSkyblockExtras extras, float partial) {
        if (!extras.iotaSupplyGiantHitbox || highlightedGiantId < 0
                || !(client.level.getEntity(highlightedGiantId) instanceof Giant giant)) {
            return;
        }
        IotaKuudraPolicy.Aabb interpolated = IotaKuudraPolicy.interpolatedEntityBox(
                giant.getX(), giant.getY(), giant.getZ(),
                giant.xo, giant.yo, giant.zo,
                giant.getBbWidth(), giant.getBbHeight(), partial);
        AABB box = aabb(interpolated).inflate(IotaKuudraPolicy.GIANT_ALERT_EXPAND);
        boolean primary = giantAlert == IotaKuudraPolicy.AlertLevel.PRIMARY;
        int fill = primary ? IotaKuudraPolicy.GIANT_PRIMARY_FILL : IotaKuudraPolicy.GIANT_SECONDARY_FILL;
        int outline = primary ? IotaKuudraPolicy.GIANT_PRIMARY_OUTLINE : IotaKuudraPolicy.GIANT_SECONDARY_OUTLINE;
        drawFilled(box, fill);
        int layers = primary ? IotaKuudraPolicy.GIANT_PRIMARY_LAYERS : IotaKuudraPolicy.GIANT_SECONDARY_LAYERS;
        drawOutline(box, outline);
        for (int i = 1; i < layers; i++) {
            drawOutline(box.inflate(IotaKuudraPolicy.GIANT_OUTLINE_STEP * i), outline);
        }
    }

    private static IotaKuudraPolicy.AlertLevel alertFor(Giant giant, Vec3 eye, AABB playerBox) {
        AABB box = giant.getBoundingBox();
        return IotaKuudraPolicy.giantAlert(box.contains(eye), box.intersects(playerBox));
    }

    private static boolean better(IotaKuudraPolicy.AlertLevel candidate, IotaKuudraPolicy.AlertLevel current) {
        return candidate.ordinal() > current.ordinal();
    }

    private static AABB arenaSearch(LocalPlayer player) {
        return player.getBoundingBox().inflate(128.0D, 64.0D, 128.0D);
    }

    private static MagmaCube findKuudra(Minecraft client) {
        MagmaCube best = null;
        double bestY = Double.NEGATIVE_INFINITY;
        for (MagmaCube cube : client.level.getEntitiesOfClass(MagmaCube.class, arenaSearch(client.player))) {
            if (cube.getSize() == IotaKuudraPolicy.KUUDRA_SIZE && cube.getY() > bestY) {
                best = cube;
                bestY = cube.getY();
            }
        }
        if (best == null || best.getHealth() <= 0.0F
                || !IotaKuudraPolicy.isKuudraBoss(best.getSize(), best.getMaxHealth(), best.getHealth())) {
            return null;
        }
        return best;
    }

    private static void tryPearlThrowAlert(int previousIndex, int currentIndex) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (currentIndex <= previousIndex || player == null || client.level == null || pearlArea == null) {
            return;
        }
        for (IotaKuudraPolicy.PearlWaypoint waypoint : pearlArea.waypoints()) {
            if (!waypoint.alert() || !waypoint.shouldShow(missingPre)
                    || waypoint.label() == null || waypoint.label().isEmpty()) {
                continue;
            }
            int target = adjustedTargetIndex(waypoint.label());
            if (target >= 0 && previousIndex < target && currentIndex == target) {
                client.level.playSound(player, player.blockPosition(),
                        SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.3F, 1.6F);
                return;
            }
        }
    }

    private static String adjustedPercentage(String label) {
        try {
            int value = Integer.parseInt(label);
            int index = IotaKuudraPolicy.SUPPLY_TICK_PERCENTAGES.indexOf(value);
            if (index < 0) {
                return label;
            }
            int clamped = Math.clamp(index, 0, IotaKuudraPolicy.SUPPLY_TICK_PERCENTAGES.size() - 1);
            return IotaKuudraPolicy.SUPPLY_TICK_PERCENTAGES.get(clamped) + "%";
        } catch (NumberFormatException ignored) {
            return label;
        }
    }

    private static int adjustedTargetIndex(String label) {
        String adjusted = adjustedPercentage(label);
        if (!adjusted.endsWith("%")) {
            return -1;
        }
        try {
            return IotaKuudraPolicy.SUPPLY_TICK_PERCENTAGES.indexOf(
                    Integer.parseInt(adjusted.substring(0, adjusted.length() - 1)));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static long remainingTimerMs(int targetIndex) {
        if (targetIndex < 0 || lastSupplyProgressIndex < 0 || supplyProgressStartMs < 0L) {
            return -1L;
        }
        if (lastSupplyProgressIndex >= targetIndex) {
            return -1L;
        }
        long elapsed = Math.max(0L, System.currentTimeMillis() - supplyProgressStartMs);
        long remaining = IotaKuudraPolicy.targetTimeMs(targetIndex) - elapsed;
        if (remaining <= 0L) {
            return -1L;
        }
        long snapped = remaining / 5L * 5L;
        return snapped > 0L ? snapped : -1L;
    }

    private static int timerColor(long remainingMs, int targetIndex) {
        long total = IotaKuudraPolicy.targetTimeMs(targetIndex);
        if (remainingMs <= 0L || total <= 0L) {
            return 0xFFFFFFFF;
        }
        double progress = Math.clamp(1.0D - (double) remainingMs / total, 0.0D, 1.0D);
        if (progress >= 0.75D) {
            return 0xFF55FF55;
        }
        if (progress >= 0.5D) {
            return 0xFFFFFF00;
        }
        return progress >= 0.25D ? 0xFFFFA500 : 0xFFFF5555;
    }

    private static List<Zombie> zombies(Minecraft client) {
        return client.level.getEntitiesOfClass(Zombie.class, arenaSearch(client.player));
    }

    private static boolean holdingSkull(ItemStack stack) {
        return stack != null && !stack.isEmpty()
                && (stack.is(Items.PLAYER_HEAD)
                || stack.is(Items.SKELETON_SKULL)
                || stack.is(Items.WITHER_SKELETON_SKULL)
                || stack.is(Items.ZOMBIE_HEAD)
                || stack.is(Items.CREEPER_HEAD)
                || stack.is(Items.PIGLIN_HEAD)
                || stack.is(Items.DRAGON_HEAD));
    }

    private static void drawBoth(AABB box, int color) {
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(color, 2.0F, color)).setAlwaysOnTop();
    }

    private static void drawOutline(AABB box, int color) {
        Gizmos.cuboid(box, GizmoStyle.stroke(color, 2.0F)).setAlwaysOnTop();
    }

    private static void drawFilled(AABB box, int color) {
        Gizmos.cuboid(box, GizmoStyle.strokeAndFill(color, 1.0F, color)).setAlwaysOnTop();
    }

    private static void drawBeam(AABB box, int height, int color) {
        Vec3 bottom = new Vec3(
                (box.minX + box.maxX) * 0.5D, box.minY, (box.minZ + box.maxZ) * 0.5D);
        Vec3 top = new Vec3(bottom.x, bottom.y + height, bottom.z);
        Gizmos.line(bottom, top, color, 2.0F).setAlwaysOnTop();
    }

    private static void drawCircle(IotaKuudraPolicy.Vec3d center, double radius, int segments, int color) {
        Vec3 previous = null;
        Vec3 first = null;
        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2.0D) * i / segments;
            Vec3 point = new Vec3(
                    center.x() + Math.cos(angle) * radius,
                    center.y(),
                    center.z() + Math.sin(angle) * radius);
            if (first == null) {
                first = point;
            }
            if (previous != null) {
                Gizmos.line(previous, point, color, 2.0F).setAlwaysOnTop();
            }
            previous = point;
        }
        if (previous != null && first != null) {
            Gizmos.line(previous, first, color, 2.0F).setAlwaysOnTop();
        }
    }

    private static void label(String text, double x, double y, double z, int color, float scale) {
        var props = Gizmos.billboardTextOverBlock(
                text, BlockPos.containing(x, y, z), 0, color, scale);
        props.setAlwaysOnTop();
    }

    private static AABB aabb(IotaKuudraPolicy.Aabb box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    public static void onChestSlotClicked(AbstractContainerScreen<?> screen, Slot slot) {
        if (screen == null || slot == null || !parentEnabled()) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        IotaKuudraPolicy.ChestKind kind = IotaKuudraPolicy.chestKind(title);
        if (kind == IotaKuudraPolicy.ChestKind.NONE) {
            return;
        }
        int windowId = screen.getMenu() == null ? -1 : screen.getMenu().containerId;
        if (windowId != chestWindowId) {
            chestWindowId = windowId;
            itemsRerolled = false;
            shardRerolled = false;
            pendingChest = null;
        }
        String hover = slot.getItem() == null || slot.getItem().isEmpty()
                ? ""
                : slot.getItem().getHoverName().getString();
        String clickedLore = lore(slot.getItem());
        IotaKuudraPolicy.RerollKind reroll = IotaKuudraPolicy.rerollKind(slot.index, hover, clickedLore);
        IotaKuudraProfitPolicy.Prices prices = livePrices();
        if (reroll == IotaKuudraPolicy.RerollKind.ITEMS && !itemsRerolled) {
            itemsRerolled = true;
            applyReroll(IotaKuudraProfitPolicy.preferLive(
                    prices.price(IotaKuudraProfitPolicy.KISMET_FEATHER),
                    IotaKuudraPolicy.rerollCost(clickedLore)));
            return;
        }
        if (reroll == IotaKuudraPolicy.RerollKind.SHARD && !shardRerolled) {
            shardRerolled = true;
            applyReroll(IotaKuudraProfitPolicy.preferLive(
                    prices.price(IotaKuudraProfitPolicy.WHEEL_OF_FATE),
                    IotaKuudraPolicy.rerollCost(clickedLore)));
            return;
        }
        if (!IotaKuudraPolicy.isBuySlot(slot.index)) {
            return;
        }
        if (IotaKuudraPolicy.alreadyOpened(clickedLore)
                || (!slot.getItem().isEmpty() && !IotaKuudraPolicy.isBuyAction(clickedLore))) {
            return;
        }
        var slots = screen.getMenu().slots;
        long loot = 0L;
        for (int index = IotaKuudraPolicy.CHEST_LOOT_FIRST;
                index <= IotaKuudraPolicy.CHEST_LOOT_LAST && index < slots.size();
                index++) {
            ItemStack stack = slots.get(index).getItem();
            loot += IotaKuudraProfitPolicy.preferLive(
                    liveItemValue(stack, prices),
                    IotaKuudraPolicy.loreCoins(lore(stack)));
        }
        boolean paid = kind == IotaKuudraPolicy.ChestKind.PAID
                && !IotaKuudraProfitPolicy.buySlotLooksFree(clickedLore);
        String infoLore = IotaKuudraPolicy.CHEST_INFO_SLOT < slots.size()
                ? lore(slots.get(IotaKuudraPolicy.CHEST_INFO_SLOT).getItem())
                : "";
        IotaKuudraProfitPolicy.KeyTier tier = IotaKuudraProfitPolicy.parseKeyTier(infoLore);
        long liveKey = IotaKuudraProfitPolicy.keyCost(tier, paid, true, prices);
        long loreKey = paid ? IotaKuudraPolicy.keyCost(clickedLore) : 0L;
        pendingChest = new PendingChest(
                kind,
                loot,
                IotaKuudraProfitPolicy.preferLive(liveKey, loreKey));
    }

    private static long liveItemValue(ItemStack stack, IotaKuudraProfitPolicy.Prices prices) {
        if (stack == null || stack.isEmpty() || prices == null) {
            return 0L;
        }
        String hover = stack.getHoverName() == null ? "" : stack.getHoverName().getString();
        String id = IotaKuudraProfitPolicy.resolveItemId(AutoClickerItemIdentity.skyBlockId(stack), hover);
        return IotaKuudraProfitPolicy.itemValue(
                id,
                bookEnchantId(stack),
                IotaKuudraProfitPolicy.resolveQuantity(hover, stack.getCount()),
                IotaKuudraProfitPolicy.countStars(hover),
                true,
                IotaKuudraProfitPolicy.DEFAULT_PET_BONUS,
                prices);
    }

    private static String bookEnchantId(ItemStack stack) {
        CompoundTag extra = SkyBlockItemData.extraAttributes(stack);
        if (extra == null) {
            return "";
        }
        Optional<CompoundTag> enchants = extra.getCompound("enchantments");
        if (enchants.isEmpty()) {
            return "";
        }
        CompoundTag tag = enchants.get();
        for (String key : tag.keySet()) {
            if (key == null || key.isBlank()) {
                continue;
            }
            int level = tag.getInt(key).orElse(0);
            return IotaKuudraProfitPolicy.enchantedBookPriceId(key, level);
        }
        return "";
    }

    private static IotaKuudraProfitPolicy.Prices livePrices() {
        SkyBlockMarketQuoteService.Quotes quotes = SkyBlockMarketQuoteService.current();
        return id -> IotaKuudraProfitPolicy.quotePrice(true, quotes.quote(id));
    }

    private static void confirmChest(IotaKuudraPolicy.ChestKind kind) {
        PendingChest pending = pendingChest;
        pendingChest = null;
        if (pending == null) {
            return;
        }
        IotaKuudraPolicy.ChestKind applied = kind == IotaKuudraPolicy.ChestKind.NONE ? pending.kind() : kind;
        QolSkyblockExtras extras = extras();
        extras.iotaChestCount = IotaKuudraPolicy.decrementChests(extras.iotaChestCount);
        extras.iotaProfitCoins += IotaKuudraPolicy.netProfit(applied, pending.lootCoins(), pending.costCoins());
        extras.iotaHourlyRateCoins = IotaKuudraPolicy.hourlyRate(extras.iotaProfitCoins, extras.iotaSessionDurationMs);
        markProfitActivity(extras);
    }

    private static void applyReroll(long costCoins) {
        QolSkyblockExtras extras = extras();
        extras.iotaProfitCoins -= Math.max(0L, costCoins);
        extras.iotaHourlyRateCoins = IotaKuudraPolicy.hourlyRate(extras.iotaProfitCoins, extras.iotaSessionDurationMs);
        markProfitActivity(extras);
    }

    private static void expireProfitSession() {
        QolSkyblockExtras extras = extras();
        IotaKuudraPolicy.SessionExpire expire = IotaKuudraPolicy.expireSession(
                System.currentTimeMillis(), extras.iotaLastActivityAt, extras.iotaLastSessionWarningAt);
        extras.iotaLastActivityAt = expire.lastActivityAt();
        extras.iotaLastSessionWarningAt = expire.lastWarningAt();
        if (expire.kind() == IotaKuudraPolicy.SessionExpire.Kind.RESET) {
            extras.iotaRunCount = 0;
            extras.iotaFailedRunCount = 0;
            extras.iotaAverageRunSeconds = 0.0D;
            extras.iotaProfitCoins = 0L;
            extras.iotaHourlyRateCoins = 0L;
            extras.iotaSessionDurationMs = 0L;
            notifyProfit(IotaKuudraPolicy.sessionResetMessage());
            TrackerStore.save(RotClientClient.trackerConfig());
        } else if (expire.kind() == IotaKuudraPolicy.SessionExpire.Kind.WARN) {
            notifyProfit(IotaKuudraPolicy.sessionWarningMessage(expire.idleMinutes(), expire.remainingMinutes()));
        }
    }

    private static void markProfitActivity(QolSkyblockExtras extras) {
        extras.iotaLastActivityAt = System.currentTimeMillis();
        extras.iotaLastSessionWarningAt = 0L;
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    private static void notifyProfit(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud == null || message == null || message.isBlank()) {
            return;
        }
        client.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
    }

    private static String lore(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ItemLore lore = stack.get(DataComponents.LORE);
        StringBuilder out = new StringBuilder();
        if (stack.getHoverName() != null) {
            out.append(stack.getHoverName().getString()).append('\n');
        }
        if (lore != null) {
            for (Component line : lore.lines()) {
                out.append(line.getString()).append('\n');
            }
        }
        return out.toString();
    }

    private static void recordKuudraRun(boolean completed) {
        QolSkyblockExtras extras = extras();
        extras.iotaChestCount = Math.min(60, extras.iotaChestCount + 1);
        long durationMs = runStartedAtMs > 0L
                ? Math.max(0L, System.currentTimeMillis() - runStartedAtMs)
                : 0L;
        extras.iotaSessionDurationMs += durationMs;
        runStartedAtMs = 0L;
        if (completed) {
            extras.iotaAverageRunSeconds = IotaKuudraPolicy.nextAverageSeconds(
                    extras.iotaAverageRunSeconds, extras.iotaRunCount, durationMs);
            extras.iotaRunCount++;
        } else {
            extras.iotaFailedRunCount++;
        }
        extras.iotaHourlyRateCoins = IotaKuudraPolicy.hourlyRate(extras.iotaProfitCoins, extras.iotaSessionDurationMs);
        markProfitActivity(extras);
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        QolSkyblockExtras extras = qol.extras();
        if (!hudVisible(qol)) {
            return List.of();
        }
        return KuudraAlertPolicy.hudLines(
                alerts,
                System.currentTimeMillis(),
                extras.iotaFreshTools,
                extras.iotaFreshParty,
                extras.iotaBuildInfo);
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        QolSkyblockExtras extras = qol.extras();
        return KuudraAlertPolicy.hudEnabled(
                extras.iotaAddonsEnabled,
                extras.iotaFreshTools,
                extras.iotaFreshParty,
                extras.iotaBuildInfo);
    }

    private static void applyAlertChat(String raw) {
        QolSkyblockExtras extras = extras();
        long now = System.currentTimeMillis();
        if (KuudraAlertPolicy.isOwnFresh(raw)) {
            alerts = KuudraAlertPolicy.applyOwnFresh(alerts, now);
            if (extras.iotaFreshTools || extras.iotaKuudraTitles) {
                showAlertTitle("FRESH");
            }
            if (extras.iotaFreshAnnounce && !announcedOwnFresh) {
                announcedOwnFresh = true;
                sendParty(KuudraAlertPolicy.PARTY_FRESH);
            }
        }
        KuudraAlertPolicy.partyFreshName(raw).ifPresent(name -> {
            if (!name.equalsIgnoreCase(localName())) {
                alerts = KuudraAlertPolicy.applyPartyFresh(alerts, name, now);
            }
        });
        KuudraAlertPolicy.parseBuild(raw).ifPresent(info ->
                alerts = KuudraAlertPolicy.applyBuild(alerts, info));
    }

    private static void showAlertTitle(String title) {
        if (title == null || title.isBlank()) {
            return;
        }
        overlayTitle = title;
        overlayUntilMs = System.currentTimeMillis() + 1_800L;
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.gui != null && client.gui.hud != null) {
            client.gui.hud.setTitle(Component.literal("§a" + title));
        }
    }

    private static void sendParty(String text) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || player.connection == null || text == null || text.isBlank()) {
            return;
        }
        player.connection.sendCommand("pc " + text);
    }

    private static String localName() {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        return player == null ? "" : player.getGameProfile().name();
    }

    private static boolean parentEnabled() {
        return extras().iotaAddonsEnabled;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
