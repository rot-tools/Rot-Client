package fi.rotclient.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import fi.rotclient.PlayerAnimalsRuntime;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.QuadrupedModel;
import net.minecraft.client.model.animal.feline.AdultFelineModel;
import net.minecraft.client.model.animal.cow.CowModel;
import net.minecraft.client.model.animal.pig.PigModel;
import net.minecraft.client.model.animal.wolf.AdultWolfModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityPlayerAnimalsMixin {
    /*
     * Do not use CowModel/PigModel directly here. Their submit bridges require
     * species-specific render states, while this redirect owns an
     * AvatarRenderState. One state-compatible model type for every shape keeps
     * the transformed method verifiable and avoids synthetic bridge casts.
     */
    private static final GenericQuadrupedModel ROTCLIENT_COW =
            new GenericQuadrupedModel(CowModel.createBodyLayer().bakeRoot());
    private static final GenericQuadrupedModel ROTCLIENT_PIG =
            new GenericQuadrupedModel(
                    PigModel.createBodyLayer(CubeDeformation.NONE).bakeRoot());
    private static final GenericQuadrupedModel ROTCLIENT_CAT =
            new GenericQuadrupedModel(LayerDefinition.create(
                    AdultFelineModel.createBodyMesh(CubeDeformation.NONE),
                    64,
                    32).bakeRoot());
    private static final GenericQuadrupedModel ROTCLIENT_WOLF =
            new GenericQuadrupedModel(LayerDefinition.create(
                    AdultWolfModel.createBodyLayer(CubeDeformation.NONE),
                    64,
                    32).bakeRoot());

    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
                    ordinal = 0))
    private void rotclient$submitAnimalBody(
            SubmitNodeCollector collector,
            Model<?> originalModel,
            Object state,
            PoseStack poseStack,
            RenderType originalType,
            int lightCoords,
            int overlayCoords,
            int tintedColor,
            TextureAtlasSprite sprite,
            int outlineColor,
            ModelFeatureRenderer.CrumblingOverlay crumblingOverlay) {
        if (!(state instanceof AvatarRenderState avatar)) {
            submitRaw(
                    collector, originalModel, state, poseStack, originalType,
                    lightCoords, overlayCoords, tintedColor, sprite,
                    outlineColor, crumblingOverlay);
            return;
        }
        PlayerAnimalsRuntime.Appearance appearance =
                PlayerAnimalsRuntime.appearance(avatar);
        if (appearance == null) {
            submitRaw(
                    collector, originalModel, state, poseStack, originalType,
                    lightCoords, overlayCoords, tintedColor, sprite,
                    outlineColor, crumblingOverlay);
            return;
        }
        GenericQuadrupedModel animal = switch (appearance.species()) {
            case "Pig" -> ROTCLIENT_PIG;
            case "Cat" -> ROTCLIENT_CAT;
            case "Wolf" -> ROTCLIENT_WOLF;
            default -> ROTCLIENT_COW;
        };
        poseStack.pushPose();
        if (appearance.baby()) {
            poseStack.translate(0.0F, 0.75F, 0.0F);
            poseStack.scale(0.55F, 0.55F, 0.55F);
        }
        collector.submitModel(
                animal, avatar, poseStack,
                animal.renderType(appearance.texture()),
                lightCoords, overlayCoords, -1, null,
                outlineColor, crumblingOverlay);
        if (appearance.felineOrWolf()) {
            collector.submitModel(
                    animal, avatar, poseStack,
                    animal.renderType(appearance.collarTexture()),
                    lightCoords, overlayCoords, appearance.collarColor(), null,
                    outlineColor, crumblingOverlay);
        }
        poseStack.popPose();
    }

    @Redirect(
            method = "submit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/layers/RenderLayer;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V"))
    private void rotclient$keepOnlyHeldItems(
            RenderLayer<?, ?> layer,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            EntityRenderState state,
            float yRot,
            float xRot) {
        if (state instanceof AvatarRenderState avatar
                && PlayerAnimalsRuntime.appearance(avatar) != null
                && !(layer instanceof PlayerItemInHandLayer<?, ?>)) {
            return;
        }
        submitLayer(layer, poseStack, collector, lightCoords, state, yRot, xRot);
    }

    @SuppressWarnings("unchecked")
    private static void submitLayer(
            RenderLayer<?, ?> layer,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int lightCoords,
            EntityRenderState state,
            float yRot,
            float xRot) {
        RenderLayer<EntityRenderState, EntityModel<? super EntityRenderState>> typed =
                (RenderLayer<EntityRenderState, EntityModel<? super EntityRenderState>>)
                        (RenderLayer<?, ?>) layer;
        typed.submit(poseStack, collector, lightCoords, state, yRot, xRot);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void submitRaw(
            SubmitNodeCollector collector,
            Model model,
            Object state,
            PoseStack poseStack,
            RenderType type,
            int light,
            int overlay,
            int tint,
            TextureAtlasSprite sprite,
            int outline,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        collector.submitModel(
                model, state, poseStack, type, light, overlay,
                tint, sprite, outline, crumbling);
    }

    private static final class GenericQuadrupedModel
            extends QuadrupedModel<LivingEntityRenderState> {
        private GenericQuadrupedModel(ModelPart root) {
            super(root);
        }
    }
}
