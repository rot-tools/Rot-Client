// SPDX-License-Identifier: GPL-3.0-only
// AUTISM Client AirPlace right-click delay accessor, adapted to Rot Client+.
package fi.rotclient.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface AutismUseDelayAccess {
    @Accessor("rightClickDelay") int rotclient$airPlaceDelay();
    @Accessor("rightClickDelay") void rotclient$airPlaceDelay(int delay);
}
