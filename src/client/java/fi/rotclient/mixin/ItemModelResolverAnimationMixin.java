package fi.rotclient.mixin;

import fi.rotclient.ItemAnimationRuntime;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
abstract class ItemModelResolverAnimationMixin {
    @ModifyVariable(
            method = "updateForTopItem",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private ItemStack rotclient$animatedRenderCopy(ItemStack original) {
        return ItemAnimationRuntime.renderStack(original);
    }
}
