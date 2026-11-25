package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
abstract class AutoClickerMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"))
    private void rotRebuild$observeRelease(long window, MouseButtonInfo button, int action, CallbackInfo callback) {
        if (action != GLFW.GLFW_RELEASE) return;
        Minecraft client = Minecraft.getInstance();
        if (window == client.getWindow().handle()) IllegalEdition.autoClickerMouseReleased(client, button.button());
    }
}
