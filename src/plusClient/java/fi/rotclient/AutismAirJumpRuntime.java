// SPDX-License-Identifier: GPL-3.0-only
// AirJumpModule from AUTISM Client, adapted by Rot Tools for Minecraft 26.3.
package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class AutismAirJumpRuntime {
    private static final AutismAirJumpPolicy STATE = new AutismAirJumpPolicy();
    private static Object world, player;
    private AutismAirJumpRuntime() {}

    private static boolean eligible(Minecraft c, Entity entity) {
        return c.isSameThread() && entity == c.player && c.player != null && c.level != null
                && c.gui.screen() == null && c.player.isAlive() && !c.player.isSpectator()
                && !c.player.isPassenger() && !c.player.getAbilities().flying && !c.player.isFallFlying()
                && !c.player.isInWater() && !c.player.isInLava() && !FreecamRuntime.active()
                && AutismModuleSettings.enabled(RotClientClient.qolConfigPublic(), AutismModuleSettings.JUMP);
    }

    public static void prepare(LivingEntity entity) {
        var c = Minecraft.getInstance();
        if (entity != c.player || !c.isSameThread()) return;
        if (!eligible(c, entity)) { clear(); return; }
        if (world != c.level || player != entity) { clear(); world = c.level; player = entity; }
        STATE.update(AutismModuleSettings.text(RotClientClient.qolConfigPublic(), AutismModuleSettings.JUMP + ".mode"),
                entity.onGround(), c.options.keyJump.isDown(), System.currentTimeMillis());
    }

    public static boolean mayJump(LivingEntity entity) {
        return eligible(Minecraft.getInstance(), entity) && !entity.onGround() && STATE.mayJump();
    }

    public static void jumped(LivingEntity entity) {
        if (eligible(Minecraft.getInstance(), entity)) STATE.jumped(entity.onGround());
    }

    public static Vec3 collision(Entity entity, Vec3 resolved) {
        var c = Minecraft.getInstance();
        if (!eligible(c, entity) || world != c.level || player != entity
                || !"GhostBlock".equals(AutismModuleSettings.text(RotClientClient.qolConfigPublic(), AutismModuleSettings.JUMP + ".mode"))) return resolved;
        Vec3 v = entity.getDeltaMovement();
        if (!STATE.ghostActive(System.currentTimeMillis(), Math.max(v.horizontalDistance(), Math.abs(v.y)))) return resolved;
        // Adapt at Entity.collide's result, after vanilla/Lithium block and entity collisions.
        // No block replacement, world scan or optional-mod class loading is required.
        double y = AutismAirJumpPolicy.clipDown(entity.getBoundingBox().minY, resolved.y);
        return y == resolved.y ? resolved : new Vec3(resolved.x, y, resolved.z);
    }

    static void clear() { STATE.reset(); world = player = null; }
}
