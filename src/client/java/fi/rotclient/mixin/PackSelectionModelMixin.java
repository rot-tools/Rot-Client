package fi.rotclient.mixin;

import fi.rotclient.CustomResourcePackPolicy;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.stream.Stream;

/**
 * Dark SkyBlock packs are toggled from Rot Client QoL, so they stay out of the
 * vanilla resource-pack screen (and its leftover author/description text).
 */
@Mixin(PackSelectionModel.class)
abstract class PackSelectionModelMixin {
    @Inject(method = "getUnselected", at = @At("RETURN"), cancellable = true)
    private void rotclient$hideUnselected(
            CallbackInfoReturnable<Stream<PackSelectionModel.Entry>> cir) {
        filterOurs(cir);
    }

    @Inject(method = "getSelected", at = @At("RETURN"), cancellable = true)
    private void rotclient$hideSelected(
            CallbackInfoReturnable<Stream<PackSelectionModel.Entry>> cir) {
        filterOurs(cir);
    }

    private static void filterOurs(
            CallbackInfoReturnable<Stream<PackSelectionModel.Entry>> cir) {
        Stream<PackSelectionModel.Entry> stream = cir.getReturnValue();
        if (stream == null) {
            return;
        }
        cir.setReturnValue(stream.filter(entry ->
                entry == null || !CustomResourcePackPolicy.isOurPack(entry.getId())));
    }
}
