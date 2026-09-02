package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fi.rotclient.TextAppearanceRuntime;
import net.minecraft.client.gui.font.glyphs.BakedSheetGlyph;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BakedSheetGlyph.class)
abstract class BakedSheetGlyphShadowMixin {
    @WrapOperation(
            method = "renderChar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/glyphs/BakedSheetGlyph;render(ZFFFLorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZI)V",
                    ordinal = 0))
    private void rotclient$fullShadow(
            BakedSheetGlyph instance,
            boolean italic,
            float x,
            float y,
            float z,
            Matrix4fc pose,
            VertexConsumer consumer,
            int color,
            boolean bold,
            int light,
            Operation<Void> original) {
        renderShadow(instance, italic, x, y, z, pose, consumer, color, bold, light, original);
    }

    @WrapOperation(
            method = "renderChar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/font/glyphs/BakedSheetGlyph;render(ZFFFLorg/joml/Matrix4fc;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZI)V",
                    ordinal = 1))
    private void rotclient$fullBoldShadow(
            BakedSheetGlyph instance,
            boolean italic,
            float x,
            float y,
            float z,
            Matrix4fc pose,
            VertexConsumer consumer,
            int color,
            boolean bold,
            int light,
            Operation<Void> original) {
        renderShadow(instance, italic, x, y, z, pose, consumer, color, bold, light, original);
    }

    private static void renderShadow(
            BakedSheetGlyph instance,
            boolean italic,
            float x,
            float y,
            float z,
            Matrix4fc pose,
            VertexConsumer consumer,
            int color,
            boolean bold,
            int light,
            Operation<Void> original) {
        if (!TextAppearanceRuntime.fullTextShadowEnabled()) {
            original.call(instance, italic, x, y, z, pose, consumer, color, bold, light);
            return;
        }
        float baseX = x - 1.0F;
        float baseY = y - 1.0F;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                original.call(
                        instance,
                        italic,
                        baseX + dx,
                        baseY + dy,
                        z,
                        pose,
                        consumer,
                        color,
                        bold,
                        light);
            }
        }
    }
}
