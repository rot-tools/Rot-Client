package fi.rotclient.mixin;

import fi.rotclient.SkyBlockUtilityRuntime;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class TitleScreenQuickJoinMixin {
    @Unique
    private Button rotclient$quickJoin;

    @Unique
    private Button rotclient$multiplayer;

    @Inject(method = "init", at = @At("RETURN"))
    private void rotclient$addQuickJoin(CallbackInfo ci) {
        rotclient$quickJoin = null;
        rotclient$multiplayer = null;
        if (!SkyBlockUtilityRuntime.quickJoinEnabled()) {
            return;
        }
        Screen screen = (Screen) (Object) this;
        Button multiplayer = null;
        for (AbstractWidget widget : Screens.getWidgets(screen)) {
            if (widget instanceof Button button
                    && button.getMessage().getContents() instanceof TranslatableContents contents
                    && "menu.multiplayer".equals(contents.getKey())) {
                multiplayer = button;
                break;
            }
        }
        if (multiplayer == null) {
            return;
        }
        multiplayer.setWidth(98);
        String ip = SkyBlockUtilityRuntime.quickJoinIp();
        rotclient$multiplayer = multiplayer;
        rotclient$quickJoin = Button.builder(
                        Component.literal(SkyBlockUtilityRuntime.quickJoinLabel()),
                        button -> SkyBlockUtilityRuntime.connectQuickJoin(screen))
                .bounds(multiplayer.getX() + 102, multiplayer.getY(), 98, 20)
                .tooltip(Tooltip.create(Component.literal("Connects to " + ip)))
                .build();
        Screens.getWidgets(screen).add(rotclient$quickJoin);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void rotclient$keepQuickJoinAligned(CallbackInfo ci) {
        if (rotclient$quickJoin == null || rotclient$multiplayer == null) {
            return;
        }
        rotclient$quickJoin.setY(rotclient$multiplayer.getY());
        rotclient$quickJoin.setX(rotclient$multiplayer.getX() + 102);
    }
}
