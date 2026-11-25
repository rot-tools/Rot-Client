package fi.rotclient.rebuild.illegal.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface AutoClickerKeyMappingAccessor {
    @Accessor("clickCount") int rotRebuild$getClickCount();
    @Accessor("clickCount") void rotRebuild$setClickCount(int count);
}
