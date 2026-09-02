package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.object.projectile.ArrowModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.StuckInBodyLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StuckInBodyLayer.class)
abstract class StuckInBodyLayerMixin {
    @Shadow
    @Final
    private Model<?> model;

    @Inject(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/AvatarRenderState;FF)V",
            at = @At("HEAD"),
            cancellable = true)
    private void rotclient$hideStuckArrows(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            AvatarRenderState state,
            float f,
            float g,
            CallbackInfo ci) {
        if (QolVisualRuntime.shouldHideStuckArrows() && this.model instanceof ArrowModel) {
            ci.cancel();
        }
    }
}
