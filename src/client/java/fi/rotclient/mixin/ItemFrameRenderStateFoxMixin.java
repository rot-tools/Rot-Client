package fi.rotclient.mixin;

import fi.rotclient.FoxItemFrameAccess;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemFrameRenderState.class)
abstract class ItemFrameRenderStateFoxMixin implements FoxItemFrameAccess {
    @Unique private boolean rotclient$foxReplaceItem;
    @Unique private float rotclient$foxU0;
    @Unique private float rotclient$foxV0;
    @Unique private float rotclient$foxU1 = 1.0F;
    @Unique private float rotclient$foxV1 = 1.0F;

    @Override
    public boolean rotclient$foxReplaceItem() {
        return rotclient$foxReplaceItem;
    }

    @Override
    public void rotclient$setFoxReplaceItem(boolean replace) {
        rotclient$foxReplaceItem = replace;
    }

    @Override
    public float rotclient$foxU0() {
        return rotclient$foxU0;
    }

    @Override
    public float rotclient$foxV0() {
        return rotclient$foxV0;
    }

    @Override
    public float rotclient$foxU1() {
        return rotclient$foxU1;
    }

    @Override
    public float rotclient$foxV1() {
        return rotclient$foxV1;
    }

    @Override
    public void rotclient$setFoxUv(float u0, float v0, float u1, float v1) {
        rotclient$foxU0 = u0;
        rotclient$foxV0 = v0;
        rotclient$foxU1 = u1;
        rotclient$foxV1 = v1;
    }
}
