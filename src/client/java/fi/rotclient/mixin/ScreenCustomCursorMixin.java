package fi.rotclient.mixin;

import fi.rotclient.CustomCursorRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Screen.class, priority = 2000)
abstract class ScreenCustomCursorMixin {
    @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("RETURN"))
    private void rotclient$drawCustomCursor(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (client != null) {
            CustomCursorRuntime.render(graphics, client.font, mouseX, mouseY);
        }
    }
}
