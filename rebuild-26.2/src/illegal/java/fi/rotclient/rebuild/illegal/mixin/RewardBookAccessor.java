package fi.rotclient.rebuild.illegal.mixin;

import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BookViewScreen.class)
public interface RewardBookAccessor {
    @Accessor("bookAccess")
    BookViewScreen.BookAccess rotRebuild$getBookAccess();
}
