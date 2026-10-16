// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface AutismToolGameModeAccess {
    @Accessor("destroyBlockPos") BlockPos rotclient$autismDestroyPos();
}
