package fi.rotclient.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("hoveredSlot")
    Slot rotclient$hoveredSlot();

    @Accessor("leftPos")
    int rotclient$leftPos();

    @Accessor("topPos")
    int rotclient$topPos();
}
