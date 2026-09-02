package fi.rotclient.mixin;

import fi.rotclient.DungeonRuntime;
import fi.rotclient.DianaRuntime;
import fi.rotclient.FishingSuiteRuntime;
import fi.rotclient.ForagingRuntime;
import fi.rotclient.IotaKuudraRuntime;
import fi.rotclient.MiningLeftoverRuntime;
import fi.rotclient.MobHighlightRuntime;
import fi.rotclient.QolVisualRuntime;
import fi.rotclient.SlayerRuntime;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.gizmos.Gizmos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Etherwarp destination visualization via per-frame gizmos.
 */
@Mixin(LevelRenderer.class)
abstract class LevelRendererEtherwarpMixin {
    @Inject(method = "collectPerFrameRenderThreadGizmos", at = @At("RETURN"))
    private void rotclient$etherwarpGizmos(
            CallbackInfoReturnable<Gizmos.TemporaryCollection> cir) {
        QolVisualRuntime.renderEtherwarpGizmos();
        QolVisualRuntime.renderTrajectoryGizmos();
        QolVisualRuntime.renderWorldScannerGizmos();
        QolVisualRuntime.renderWaypointGizmos();
        IotaKuudraRuntime.renderGizmos();
        MobHighlightRuntime.renderGizmos();
        SlayerRuntime.renderGizmos();
        DungeonRuntime.renderGizmos();
        FishingSuiteRuntime.renderGizmos();
        MiningLeftoverRuntime.renderGizmos();
        DianaRuntime.renderGizmos();
        ForagingRuntime.renderGizmos();
    }
}
