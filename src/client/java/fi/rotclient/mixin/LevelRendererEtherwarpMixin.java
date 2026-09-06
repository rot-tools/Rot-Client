package fi.rotclient.mixin;

import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.DungeonRuntime;
import fi.rotclient.DianaRuntime;
import fi.rotclient.FishingSuiteRuntime;
import fi.rotclient.ForagingRuntime;
import fi.rotclient.GhostsRuntime;
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
        ClientBoundaryGuard.run("ETHERWARP_GIZMOS", () -> QolVisualRuntime.renderEtherwarpGizmos());
        ClientBoundaryGuard.run("TRAJECTORY_GIZMOS", () -> QolVisualRuntime.renderTrajectoryGizmos());
        ClientBoundaryGuard.run("WORLD_SCANNER_GIZMOS", () -> QolVisualRuntime.renderWorldScannerGizmos());
        ClientBoundaryGuard.run("WAYPOINT_GIZMOS", () -> QolVisualRuntime.renderWaypointGizmos());
        ClientBoundaryGuard.run("IOTA_KUUDRA_GIZMOS", () -> IotaKuudraRuntime.renderGizmos());
        ClientBoundaryGuard.run("MOB_HIGHLIGHT_GIZMOS", () -> MobHighlightRuntime.renderGizmos());
        ClientBoundaryGuard.run("SLAYER_GIZMOS", () -> SlayerRuntime.renderGizmos());
        ClientBoundaryGuard.run("DUNGEON_GIZMOS", () -> DungeonRuntime.renderGizmos());
        ClientBoundaryGuard.run("FISHING_GIZMOS", () -> FishingSuiteRuntime.renderGizmos());
        ClientBoundaryGuard.run("MINING_LEFTOVER_GIZMOS", () -> MiningLeftoverRuntime.renderGizmos());
        ClientBoundaryGuard.run("DIANA_GIZMOS", () -> DianaRuntime.renderGizmos());
        ClientBoundaryGuard.run("FORAGING_GIZMOS", () -> ForagingRuntime.renderGizmos());
        ClientBoundaryGuard.run("GHOSTS_GIZMOS", () -> GhostsRuntime.renderGizmos());
    }
}
