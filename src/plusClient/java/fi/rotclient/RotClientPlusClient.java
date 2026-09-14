package fi.rotclient;

import net.fabricmc.api.ClientModInitializer;

/** Marks the Plus client entrypoint. Tick/command wiring lives in flavor hooks. */
public final class RotClientPlusClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        QolFlavorSupport.install(new RotClientPlusExtension());
        QolClientFlavorSupport.install(new RotClientPlusHooks());
    }
}
