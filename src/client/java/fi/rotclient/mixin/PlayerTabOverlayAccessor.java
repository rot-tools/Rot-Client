package fi.rotclient.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {
    @Accessor("header")
    Component rotclient$getHeader();

    @Accessor("footer")
    Component rotclient$getFooter();

    @Invoker("getNameForDisplay")
    Component rotclient$getNameForDisplay(PlayerInfo info);
}
