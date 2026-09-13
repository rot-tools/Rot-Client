package fi.rotclient.mixin;

import fi.rotclient.MapArtRenderStateAccess;
import net.minecraft.client.renderer.state.MapRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MapRenderState.class)
abstract class MapRenderStateMapArtMixin implements MapArtRenderStateAccess {
    @Unique private float rotclient$mapArtU0;
    @Unique private float rotclient$mapArtV0;
    @Unique private float rotclient$mapArtU1 = 1.0F;
    @Unique private float rotclient$mapArtV1 = 1.0F;

    @Override public float rotclient$mapArtU0() { return rotclient$mapArtU0; }
    @Override public float rotclient$mapArtV0() { return rotclient$mapArtV0; }
    @Override public float rotclient$mapArtU1() { return rotclient$mapArtU1; }
    @Override public float rotclient$mapArtV1() { return rotclient$mapArtV1; }

    @Override
    public void rotclient$setMapArtUv(float u0, float v0, float u1, float v1) {
        rotclient$mapArtU0 = u0;
        rotclient$mapArtV0 = v0;
        rotclient$mapArtU1 = u1;
        rotclient$mapArtV1 = v1;
    }
}
