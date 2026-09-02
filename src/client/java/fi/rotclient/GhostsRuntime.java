package fi.rotclient;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;

/**
 * Minecraft bridge for {@link GhostsPolicy}. Mixins query
 * {@link #decision(Entity)} each render.
 */
public final class GhostsRuntime {
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
                creeper.isInvisible());
    }

    public static boolean shouldSuppress(Entity entity) {
        return decision(entity).suppressEntity();
    }
}
