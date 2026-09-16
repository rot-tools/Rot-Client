package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Lite-safe dungeon door boundary.
 *
 * Lite never replaces opaque world blocks with transparent blocks.
 * Rot Client+ provides the extended replacement implementation.
 */
public final class HateDoorsRuntime {
    private HateDoorsRuntime() {
    }

    public static void clear() {
        QolClientFlavorSupport.hooks().hateDoorsClear();
    }

    public static void remember(
            BlockPos pos,
            EmberDungeonPolicy.GlassTint tint) {
        QolClientFlavorSupport.hooks()
                .hateDoorsRemember(pos, tint);
    }

    public static void forget(BlockPos pos) {
        QolClientFlavorSupport.hooks()
                .hateDoorsForget(pos);
    }

    public static BlockState rewrite(
            BlockPos pos,
            BlockState original) {
        return QolClientFlavorSupport.hooks()
                .hateDoorsRewrite(pos, original);
    }

    static void scan(
            Minecraft client,
            QolSkyblockExtras extras) {
        QolClientFlavorSupport.hooks()
                .hateDoorsScan(client, extras);
    }

    static BlockState glassState(
            EmberDungeonPolicy.GlassTint tint) {
        return QolClientFlavorSupport.hooks()
                .hateDoorsGlassState(tint);
    }
}
