package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Mob highlight: ESP boxes and tracers for named entities in a local list.
 */
public final class MobHighlightRuntime {
    private static boolean addKeyWasDown;

    private MobHighlightRuntime() {
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        if (!qol.mobHighlightEnabled || client == null || client.getWindow() == null) {
            addKeyWasDown = false;
            return;
        }
        long window = client.getWindow().handle();
        boolean down = QolKeybindNames.isBoundDown(window, qol.mobHighlightAddKey);
        if (down && !addKeyWasDown) {
            toggleLookedAt(client, qol);
        }
        addKeyWasDown = down;
    }

    public static void renderGizmos() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (!qol.mobHighlightEnabled || player == null || client.level == null) {
            return;
        }
        boolean keyUnbound = qol.mobHighlightAddKey == null || qol.mobHighlightAddKey.isBlank();
        boolean keyHeld = client.getWindow() != null
                && QolKeybindNames.isBoundDown(client.getWindow().handle(), qol.mobHighlightAddKey);
        if (qol.mobHighlightRequireKey && !keyUnbound && !keyHeld) {
            return;
        }
        if (qol.mobHighlightNames == null || qol.mobHighlightNames.isEmpty()) {
            return;
        }
        AABB search = player.getBoundingBox().inflate(64.0D);
        List<Entity> entities = new ArrayList<>();
        entities.addAll(client.level.getEntitiesOfClass(LivingEntity.class, search));
        entities.addAll(client.level.getEntitiesOfClass(ArmorStand.class, search));
        Vec3 eye = player.getEyePosition();
        for (Entity entity : entities) {
            if (entity == player || (entity instanceof Player && !(entity instanceof LocalPlayer))) {
                continue;
            }
            String name = entityName(entity);
            if (!MobHighlightPolicy.matches(name, qol.mobHighlightNames)) {
                continue;
            }
            AABB box = entity.getBoundingBox();
            var props = Gizmos.cuboid(
                    box,
                    GizmoStyle.strokeAndFill(
                            qol.mobHighlightColor, 2.0F, withAlpha(qol.mobHighlightColor, 0x44)));
            if (!qol.mobHighlightDepth) {
                props.setAlwaysOnTop();
            }
            if (qol.mobHighlightTracers) {
                var line = Gizmos.line(
                        eye,
                        box.getCenter(),
                        qol.mobHighlightColor);
                if (!qol.mobHighlightDepth) {
                    line.setAlwaysOnTop();
                }
            }
        }
    }

    private static void toggleLookedAt(Minecraft client, QolUtilityConfig qol) {
        if (client.hitResult == null || client.hitResult.getType() != HitResult.Type.ENTITY) {
            return;
        }
        Entity entity = ((EntityHitResult) client.hitResult).getEntity();
        String name = entityName(entity);
        if (name.isBlank()) {
            return;
        }
        if (MobHighlightPolicy.matches(name, qol.mobHighlightNames)) {
            qol.mobHighlightNames = new ArrayList<>(
                    MobHighlightPolicy.removeName(qol.mobHighlightNames, name));
        } else {
            qol.mobHighlightNames = new ArrayList<>(
                    MobHighlightPolicy.addName(qol.mobHighlightNames, name));
        }
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    private static String entityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        Component custom = entity.getCustomName();
        if (custom != null) {
            return custom.getString();
        }
        return entity.getName().getString();
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
