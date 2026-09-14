package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.ClientBoundaryGuard;
import fi.rotclient.MapArtOverrideRuntime;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemFrameRenderer.class)
abstract class ItemFrameRendererMapArtMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void rotclient$mapArtPanel(
            ItemFrame frame, ItemFrameRenderState state, float tickProgress, CallbackInfo ci) {
        ClientBoundaryGuard.run(
                "MAP_ART_FRAME_LAYOUT",
                () -> MapArtOverrideRuntime.configureItemFrame(state, frame));
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/MapRenderer;render(Lnet/minecraft/client/renderer/state/MapRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ZI)V"))
    private void rotclient$replaceFramedMap(
            MapRenderer renderer,
            MapRenderState mapState,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            boolean showOnlyFrame,
            int lightCoords,
            Operation<Void> original,
            @Local(argsOnly = true) ItemFrameRenderState state) {
        boolean rendered = ClientBoundaryGuard.call(
                "FOX_ITEM_FRAME_MAP",
                () -> MapArtOverrideRuntime.renderFramedMap(
                        mapState, poseStack, collector, lightCoords, state.rotation),
                false);
        if (!rendered) {
            original.call(renderer, mapState, poseStack, collector, showOnlyFrame, lightCoords);
        }
    }

    @WrapOperation(
            method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void rotclient$replaceFramedItem(
            ItemStackRenderState item,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            int overlayCoords,
            int outlineColor,
            Operation<Void> original,
            @Local(argsOnly = true) ItemFrameRenderState state) {
        boolean rendered = ClientBoundaryGuard.call(
                "FOX_ITEM_FRAME_ITEM",
                () -> MapArtOverrideRuntime.renderItemFrameItem(
                        state, poseStack, collector, lightCoords),
                false);
        if (!rendered) {
            original.call(item, poseStack, collector, lightCoords, overlayCoords, outlineColor);
        }
    }
}
