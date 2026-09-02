package fi.rotclient.mixin;

import fi.rotclient.NameHiderRuntime;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Last-line local rewrite of painted 2D text so the player's own name is
 * hidden in HUD, screens, scoreboard, boss bars, and similar GUI paths.
 */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorNameHiderMixin {
    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private String rotclient$hideNameInString(String text) {
        return NameHiderRuntime.apply(text);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private String rotclient$hideNameInStringShadow(String text) {
        return NameHiderRuntime.apply(text);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$hideNameInComponent(Component text) {
        return NameHiderRuntime.apply(text);
    }

    @ModifyVariable(
            method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$hideNameInComponentShadow(Component text) {
        return NameHiderRuntime.apply(text);
    }

    @ModifyVariable(
            method = "textWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$hideNameInBackdrop(Component text) {
        return NameHiderRuntime.apply(text);
    }

    @ModifyVariable(
            method = "textWithWordWrap(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/FormattedText;IIII)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private FormattedText rotclient$hideNameInWordWrap(FormattedText text) {
        if (text instanceof Component component) {
            return NameHiderRuntime.apply(component);
        }
        return text;
    }

    @ModifyVariable(
            method = "centeredText(Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private String rotclient$hideNameInCenteredString(String text) {
        return NameHiderRuntime.apply(text);
    }

    @ModifyVariable(
            method = "centeredText(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$hideNameInCenteredComponent(Component text) {
        return NameHiderRuntime.apply(text);
    }
}
