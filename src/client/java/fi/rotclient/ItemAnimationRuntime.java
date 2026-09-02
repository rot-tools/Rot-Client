package fi.rotclient;

import net.minecraft.core.component.DataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

/** Builds render-only ItemStack copies for deterministic dye/skin frames. */
public final class ItemAnimationRuntime {
    private ItemAnimationRuntime() {
    }

    public static ItemStack renderStack(ItemStack original) {
        if (original == null || original.isEmpty()) {
            return original;
        }
        QolUtilityConfig config = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = config.extras();
        if (!extras.animationFixEnabled || (!extras.animationDyes && !extras.animationSkins)) {
            return original;
        }
        RotItemIndex.ItemDef item = RotItemIndex.find(SkyBlockItemData.marketId(original));
        if (item == null) {
            return original;
        }
        Minecraft client = Minecraft.getInstance();
        long gameTime = client == null || client.level == null
                ? 0L : client.level.getGameTime();
        ItemStack copy = null;
        if (extras.animationDyes && !item.dyeFrames().isEmpty()) {
            int frame = RotItemIndex.animationFrameIndex(gameTime, item.dyeFrames().size(), 4);
            copy = original.copy();
            copy.set(DataComponents.DYED_COLOR, new DyedItemColor(item.dyeFrames().get(frame)));
        }
        if (extras.animationSkins && !item.skinFrames().isEmpty()) {
            int frame = RotItemIndex.animationFrameIndex(gameTime, item.skinFrames().size(), 8);
            if (copy == null) {
                copy = original.copy();
            }
            try {
                copy.set(
                        DataComponents.ITEM_MODEL,
                        Identifier.parse(item.skinFrames().get(frame)));
            } catch (RuntimeException ignored) {
                // Invalid bundled frame IDs keep the original model.
            }
        }
        return copy == null ? original : copy;
    }
}
