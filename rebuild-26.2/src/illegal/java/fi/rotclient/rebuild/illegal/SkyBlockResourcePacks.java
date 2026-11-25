package fi.rotclient.rebuild.illegal;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

final class SkyBlockResourcePacks {
    private SkyBlockResourcePacks() {}

    static void register() {
        FabricLoader.getInstance().getModContainer("rotclientillegal").ifPresent(container -> {
            register(container, "dark_overworld", "Rot Client Dark Overworld made by Rot-Tools");
            register(container, "dark_crimson", "Rot Client Dark Crimson Isle made by Rot-Tools");
            register(container, "dark_end", "Rot Client Dark End made by Rot-Tools");
        });
    }

    private static void register(ModContainer container, String path, String title) {
        ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("rotclient_rebuild", path), container,
                Component.literal(title), PackActivationType.NORMAL);
    }

    static void openSelection(Screen parent) {
        Minecraft client = Minecraft.getInstance();
        client.gui.setScreen(new PackSelectionScreen(client.getResourcePackRepository(), repository -> {
            client.options.updateResourcePacks(repository);
            client.gui.setScreen(parent);
        }, client.getResourcePackDirectory(), Component.translatable("resourcePack.title")));
    }
}
