package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Minecraft bridge for {@link GhostsPolicy}. Mixins query
 * {@link #decision(Entity)} each render; gizmos draw Mist boxes the same
 * way NoFrills Ghost Vision does: standing-pose lerped AABB, fill plus a
 * 3px outline, charged overlay stripped.
 */
public final class GhostsRuntime {
    private static final double SCAN_RADIUS = 160.0D;

    private GhostsRuntime() {
    }

    public static GhostsPolicy.Decision decision(Entity entity) {
        if (!(entity instanceof Creeper creeper)) {
            return GhostsPolicy.Decision.NONE;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return GhostsPolicy.decide(
                extras.ghostsEnabled,
                extras.ghostsShowGhosts,
                extras.ghostsShowPowered,
                true,
                creeper.isInvisible(),
                onGhostIsland(),
                creeper.getY());
    }

    public static boolean shouldSuppress(Entity entity) {
        return decision(entity).suppressEntity();
    }

    /**
     * True for a Mist creeper while Ghosts is on. Used to strip the vanilla
     * charged overlay the same way NoFrills gates {@code isPowered}.
     */
    public static boolean isTrackedGhost(Entity entity) {
        if (!(entity instanceof Creeper creeper) || !creeper.isAlive()) {
            return false;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.ghostsEnabled) {
            return false;
        }
        return GhostsPolicy.isMistCreeper(true, onGhostIsland(), creeper.getY());
    }

    public static boolean shouldHideChargedOverlay(Entity entity) {
        if (!isTrackedGhost(entity)) {
            return false;
        }
        return !RotClientClient.qolConfigPublic().extras().ghostsShowPowered;
    }

    public static void renderGizmos() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.ghostsEnabled || !onGhostIsland()) {
            return;
        }
        GhostsPolicy.BoxPaint paint = GhostsPolicy.boxPaint(extras.ghostsHighlightStyle);
        if (!paint.draws()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.level == null) {
            return;
        }
        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        float width = GhostsPolicy.strokeWidth(paint);
        int stroke = GhostsPolicy.strokeArgb(paint, extras.ghostsOutlineColor, extras.ghostsFillColor);
        int fill = GhostsPolicy.fillArgb(paint, extras.ghostsFillColor);
        GizmoStyle style = GizmoStyle.strokeAndFill(stroke, width, fill);
        AABB search = player.getBoundingBox().inflate(SCAN_RADIUS);
        for (Creeper creeper : client.level.getEntitiesOfClass(Creeper.class, search, Entity::isAlive)) {
            if (!GhostsPolicy.isMistCreeper(true, true, creeper.getY())) {
                continue;
            }
            Gizmos.cuboid(lerpedStandingBox(creeper, partialTick), style).setAlwaysOnTop();
        }
    }

    private static AABB lerpedStandingBox(Creeper creeper, float partialTick) {
        Vec3 pos = new Vec3(
                EntityLerpPolicy.lerp(creeper.xo, creeper.getX(), partialTick),
                EntityLerpPolicy.lerp(creeper.yo, creeper.getY(), partialTick),
                EntityLerpPolicy.lerp(creeper.zo, creeper.getZ(), partialTick));
        return creeper.getDimensions(Pose.STANDING).makeBoundingBox(pos);
    }

    private static boolean onGhostIsland() {
        if (SkyBlockAreaDetector.detect() == SkyBlockArea.DWARVEN_MINES) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        StringBuilder text = new StringBuilder();
        text.append(MiningLeftoverRuntime.scoreboardText());
        List<String> tab = CommissionDisplayRuntime.tabLines(client);
        for (String line : tab) {
            text.append('\n').append(line);
        }
        return GhostsPolicy.boardLooksLikeGhostIsland(text.toString());
    }
}
