package fi.rotclient.rebuild.illegal.mixin;

import fi.rotclient.rebuild.illegal.BlackGlassPack;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.server.packs.repository.Pack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPackSource.class)
abstract class BlackGlassPackSourceMixin {
    @Inject(method = "populatePackList", at = @At("RETURN"))
    private void rotRebuild$registerBlackGlass(BiConsumer<String, Function<String, Pack>> packs, CallbackInfo callback) {
        BlackGlassPack.addToPackList(packs);
    }
}
