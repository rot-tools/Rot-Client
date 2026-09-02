package fi.rotclient.mixin;

import fi.rotclient.QolVisualRuntime;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optional vanilla-model swap for mapped SkyBlock ids. Isolated and default
 * off. 26.2 {@code ItemStack} does not declare {@code get(DataComponentType)},
 * and {@code DataComponentHolder} is an interface, so this injects the class
 * that actually stores item components.
 */
@Mixin(PatchedDataComponentMap.class)
abstract class ItemStackLegacyTexturesMixin {
    @Inject(
            method = "get(Lnet/minecraft/core/component/DataComponentType;)Ljava/lang/Object;",
            at = @At("RETURN"),
            cancellable = true)
    private void rotclient$legacyItemModel(
            DataComponentType<?> type,
            CallbackInfoReturnable<Object> cir) {
        if (type != DataComponents.ITEM_MODEL) {
            return;
        }
        Object original = cir.getReturnValue();
        Identifier current = original instanceof Identifier id ? id : null;
        CustomData custom = ((PatchedDataComponentMap) (Object) this).get(DataComponents.CUSTOM_DATA);
        Identifier next = QolVisualRuntime.maybeLegacyItemModel(custom, current);
        if (next != null) {
            cir.setReturnValue(next);
        }
    }
}
