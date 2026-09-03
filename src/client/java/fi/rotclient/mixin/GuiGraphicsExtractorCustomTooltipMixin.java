package fi.rotclient.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import fi.rotclient.CustomTooltipRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Minecraft 26.2 draws hover boxes in {@code tooltip} and then pins them to
 * the screen through {@code positionTooltip}. Wheel panning has to land on
 * that already-clamped point; wrapping the queued-frame setter never saw a
 * positioner local and aborted mixin apply.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorCustomTooltipMixin {
    @WrapOperation(
            method = "tooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;IILnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;Lnet/minecraft/resources/Identifier;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipPositioner;positionTooltip(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic rotclient$panAfterScreenClamp(
            ClientTooltipPositioner positioner,
            int screenWidth,
            int screenHeight,
            int mouseX,
            int mouseY,
            int tooltipWidth,
            int tooltipHeight,
            Operation<Vector2ic> original) {
        return CustomTooltipRuntime.afterScreenClamp(
                original.call(
                        positioner,
                        screenWidth,
                        screenHeight,
                        mouseX,
                        mouseY,
                        tooltipWidth,
                        tooltipHeight),
                screenWidth,
                screenHeight,
                tooltipWidth,
                tooltipHeight);
    }
}
