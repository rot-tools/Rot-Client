package fi.rotclient.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractSignEditScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Only the Plus controller with a pending, matching sign intent uses this accessor. */
@Mixin(AbstractSignEditScreen.class)
public interface MarketTradeSignAccess {
    @Accessor("messages") String[] rotclient$marketLines();
    @Invoker("onDone") void rotclient$marketDone();
}
