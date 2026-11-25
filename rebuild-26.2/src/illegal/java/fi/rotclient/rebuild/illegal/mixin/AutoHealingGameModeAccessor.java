package fi.rotclient.rebuild.illegal.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface AutoHealingGameModeAccessor {
    @Invoker("ensureHasSentCarriedItem")
    void rotRebuild$syncHeldItem();
}
