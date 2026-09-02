package fi.rotclient.mixin;

import fi.rotclient.IotaKuudraRuntime;
import fi.rotclient.NameHiderRuntime;
import fi.rotclient.RotClientClient;
import fi.rotclient.SkyBlockStatBarParser;
import net.minecraft.client.gui.Hud;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Optional;

/**
 * Local action-bar fragment suppression for Player Display hide settings.
 *
 * MC 26.2: overlay/action-bar text enters through
 * {@link Hud#setOverlayMessage(Component, boolean)}, not {@code Gui}.
 * Does not cancel unrelated overlay messages.
 */
@Mixin(Hud.class)
abstract class HudActionBarFilterMixin {
    @ModifyVariable(
            method = "setOverlayMessage(Lnet/minecraft/network/chat/Component;Z)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$filterSkyBlockStatFragments(Component message) {
        if (message == null) {
            return message;
        }
        RotClientClient.observePlayerDisplayText(message.getString());
        if (!RotClientClient.shouldFilterActionBar()) {
            return NameHiderRuntime.apply(message);
        }
        String raw = message.getString();
        Optional<String> filtered = SkyBlockStatBarParser.filterActionBar(
                raw,
                RotClientClient.hideActionHealth(),
                RotClientClient.hideActionDefense(),
                RotClientClient.hideActionMana(),
                RotClientClient.hideActionOverflow(),
                RotClientClient.hideActionSpeed(),
                RotClientClient.hideActionVitality(),
                RotClientClient.hideActionLocation());
        if (filtered.isEmpty()) {
            return Component.empty();
        }
        if (filtered.get().equals(raw)) {
            return NameHiderRuntime.apply(message);
        }
        return NameHiderRuntime.apply(Component.literal(filtered.get()));
    }

    @ModifyVariable(
            method = "setTitle(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$hideNameInTitle(Component title) {
        IotaKuudraRuntime.onTitle(title);
        return NameHiderRuntime.apply(title);
    }

    @ModifyVariable(
            method = "setSubtitle(Lnet/minecraft/network/chat/Component;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private Component rotclient$hideNameInSubtitle(Component subtitle) {
        IotaKuudraRuntime.onTitle(subtitle);
        return NameHiderRuntime.apply(subtitle);
    }
}
