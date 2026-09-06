package fi.rotclient.mixin;

import fi.rotclient.DungeonRuntime;
import fi.rotclient.ItemRarityRuntime;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Paints rarity behind hotbar items so the item texture is not tinted.
 */
@Mixin(Hud.class)
abstract class HudItemRarityMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void rotclient$rarityBehindHotbar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            DeltaTracker delta,
            Player player,
            ItemStack stack,
            int seed,
            CallbackInfo ci) {
        ItemRarityRuntime.paintHotbarBackground(graphics, x, y, stack);
    }

    @Inject(method = "extractSlot", at = @At("RETURN"))
    private void rotclient$maskCooldownHotbar(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            DeltaTracker delta,
            Player player,
            ItemStack stack,
            int seed,
            CallbackInfo ci) {
        DungeonRuntime.paintMaskOverlay(graphics, x, y, stack);
    }
}
