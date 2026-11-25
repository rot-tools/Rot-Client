package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.IllegalEdition;
import fi.rotclient.rebuild.illegal.LegacySkyBlockTextures;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemModelResolver.class)
abstract class LegacyItemModelMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("STORE"), ordinal = 0)
    private Identifier rotRebuild$legacyModel(Identifier original, ItemStackRenderState output, ItemStack item) {
        if (!IllegalEdition.legacyTexturesEnabled() || !LegacySkyBlockTextures.isSkyBlockModel(original)) return original;
        var custom = item.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return original;
        var tag = ((LegacyTextureDataAccessor) (Object) custom).rotRebuild$getLegacyModelData();
        return LegacySkyBlockTextures.renderModel(original, tag);
    }
}
