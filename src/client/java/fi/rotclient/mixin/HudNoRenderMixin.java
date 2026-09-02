package fi.rotclient.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import fi.rotclient.QolVisualRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
abstract class HudNoRenderMixin {
    @Inject(method = "extractPlayerHealth", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideVanillaHealth(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (QolVisualRuntime.hideVanillaHealth()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractArmor", at = @At("HEAD"), cancellable = true)
    private static void rotclient$hideArmorBar(
            GuiGraphicsExtractor graphics,
            Player player,
            int yLineBase,
            int numHealthRows,
            int healthRowHeight,
            int xLeft,
            CallbackInfo ci) {
        if (QolVisualRuntime.hideArmorBar()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractFood", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideFoodBar(
            GuiGraphicsExtractor graphics,
            Player player,
            int yLineBase,
            int xRight,
            CallbackInfo ci) {
        if (QolVisualRuntime.hideFoodBar()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractEffects", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideEffects(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        if (QolVisualRuntime.hideEffectDisplay()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractSelectedItemName", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideSelectedName(GuiGraphicsExtractor graphics, CallbackInfo ci) {
        if (QolVisualRuntime.hideSelectedItemName()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractConfusionOverlay", at = @At("HEAD"), cancellable = true)
    private void rotclient$hideNausea(GuiGraphicsExtractor graphics, float strength, CallbackInfo ci) {
        if (QolVisualRuntime.shouldHideNausea()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "extractVignette",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blit(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIFFIIIII)V"),
            cancellable = true)
    private void rotclient$hideVignette(
            GuiGraphicsExtractor graphics,
            Entity entity,
            CallbackInfo ci,
            @Local(ordinal = 0) float borderWarningStrength) {
        if (QolVisualRuntime.shouldHideVignette(borderWarningStrength > 0.0F)) {
            ci.cancel();
        }
    }
}
