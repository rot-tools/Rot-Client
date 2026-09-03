package fi.rotclient.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerCursorAccessor {
    @Mutable
    @Accessor("xpos")
    void rotclient$setXpos(double x);

    @Mutable
    @Accessor("ypos")
    void rotclient$setYpos(double y);

    @Mutable
    @Accessor("mouseGrabbed")
    void rotclient$setMouseGrabbed(boolean grabbed);
}
