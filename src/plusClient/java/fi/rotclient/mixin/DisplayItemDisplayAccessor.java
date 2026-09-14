package fi.rotclient.mixin;

import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.ItemDisplay.class)
public interface DisplayItemDisplayAccessor {
    @Invoker("getItemStack")
    ItemStack rotclient$getItemStack();
}
