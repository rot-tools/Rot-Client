package fi.rotclient.rebuild.illegal.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CustomData.class)
public interface LegacyTextureDataAccessor {
    /** Read-only model lookup. Callers preserve the item component. */
    @Accessor("tag")
    CompoundTag rotRebuild$getLegacyModelData();
}
