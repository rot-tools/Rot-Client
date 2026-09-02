package fi.rotclient;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Registers the converted dark SkyBlock packs as Fabric builtin packs and
 * keeps Minecraft's selected-pack list in sync with the QoL toggle.
 */
public final class CustomResourcePackRuntime {
    private static boolean registered;
    private static boolean reloading;

    private CustomResourcePackRuntime() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        FabricLoader.getInstance()
                .getModContainer("rotclient")
                .ifPresent(CustomResourcePackRuntime::registerPacks);
        registered = true;
    }

    private static void registerPacks(ModContainer container) {
        registerOne(
                container,
                CustomResourcePackPolicy.PACK_OVERWORLD,
                "Rot Client Dark Overworld");
        registerOne(
                container,
                CustomResourcePackPolicy.PACK_CRIMSON,
                "Rot Client Dark Crimson");
        registerOne(
                container,
                CustomResourcePackPolicy.PACK_END,
                "Rot Client Dark End");
        registerOne(
                container,
                CustomResourcePackPolicy.PACK_GAMEPLAY_FONT,
                "Rot Client Source Sans Gameplay Font");
    }

    private static void registerOne(ModContainer container, String packId, String title) {
        Identifier id = Identifier.fromNamespaceAndPath(
                "rotclient",
                packId.substring(packId.indexOf(':') + 1));
        ResourceLoader.registerBuiltinPack(
                id,
                container,
                Component.literal(title),
                PackActivationType.NORMAL);
    }

    public static boolean isReloading() {
        return reloading;
    }

    public static void tick(Minecraft client) {
        if (client == null || client.options == null || reloading) {
            return;
        }
        PackRepository repository = client.getResourcePackRepository();
        if (repository == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        List<String> desired = resolveAvailable(
                repository,
                CustomResourcePackPolicy.enabledPackIds(
                        extras.customResourcePackEnabled,
                        extras.customResourcePackOverworld,
                        extras.customResourcePackCrimson,
                        extras.customResourcePackEnd,
                        extras.customResourcePackGameplayFont));
        Collection<String> selected = repository.getSelectedIds();
        if (CustomResourcePackPolicy.selectionMatches(selected, desired)) {
            return;
        }
        apply(client, repository, desired);
    }

    private static List<String> resolveAvailable(
            PackRepository repository,
            List<String> desiredCanonical) {
        List<String> resolved = new ArrayList<>();
        for (String canonical : desiredCanonical) {
            String actual = resolveId(repository, canonical);
            if (actual != null) {
                resolved.add(actual);
            }
        }
        return resolved;
    }

    private static String resolveId(PackRepository repository, String canonical) {
        if (repository.isAvailable(canonical)) {
            return canonical;
        }
        String path = canonical.substring(canonical.indexOf(':') + 1);
        if (repository.isAvailable(path)) {
            return path;
        }
        for (String id : repository.getAvailableIds()) {
            if (id != null && CustomResourcePackPolicy.canonicalId(id).equals(canonical)) {
                return id;
            }
        }
        Pack pack = repository.getPack(canonical);
        return pack == null ? null : pack.getId();
    }

    private static void apply(
            Minecraft client,
            PackRepository repository,
            List<String> desired) {
        List<String> next = CustomResourcePackPolicy.applySelection(
                repository.getSelectedIds(),
                desired);
        repository.setSelected(next);
        client.options.updateResourcePacks(repository);
        reloading = true;
        client.reloadResourcePacks().whenComplete((ignored, error) -> reloading = false);
    }
}
