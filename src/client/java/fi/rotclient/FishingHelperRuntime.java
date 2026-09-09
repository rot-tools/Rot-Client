package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.AABB;

/**
 * Fishing helper HUD. Auto-pull / recast live in {@link FishingPlusRuntime}.
 */
final class FishingHelperRuntime {
    private FishingHelperRuntime() {
    }

    static void clear() {
        // Plus auto-pull state is owned by FishingPlusRuntime.
    }

    static void tick(Minecraft client) {
        // Auto-pull / recast live in FishingPlusRuntime (Plus JAR only).
    }

    static String hudLine() {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        Minecraft client = Minecraft.getInstance();
        if (!qol.fishingHelperEnabled || !qol.fishingHelperBobberTimer
                || client == null || client.player == null) {
            return "";
        }
        FishingHook hook = client.player.fishing;
        if (hook == null || !hook.isAlive()) {
            return "";
        }
        return String.format(java.util.Locale.ROOT, "Bobber %.1fs", hook.tickCount / 20.0F);
    }

    static boolean biteNearby(LocalPlayer player, FishingHook hook) {
        if (player == null || player.level() == null || hook == null) {
            return false;
        }
        AABB box = hook.getBoundingBox().inflate(FishingHelperPolicy.BITE_RANGE);
        for (ArmorStand stand : player.level().getEntitiesOfClass(ArmorStand.class, box)) {
            String name = stand.getCustomName() == null
                    ? stand.getName().getString()
                    : stand.getCustomName().getString();
            if (FishingHelperPolicy.isBiteHologram(name)
                    && stand.distanceTo(hook) <= FishingHelperPolicy.BITE_RANGE) {
                return true;
            }
        }
        return false;
    }
}
