package fi.rotclient.rebuild;

import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class RebuildClient implements ClientModInitializer {
    static final String LEGAL_ID = "rotclientlegal";
    static final String TEST_ID = "rotclientillegal";
    static final ClientEdition EMPTY_EDITION = new ClientEdition() {};
    private static boolean testEdition;
    private static InterfaceSettings settings;
    private static List<ModuleCard> modules = List.of();

    @Override
    public void onInitializeClient() {
        FabricLoader loader = FabricLoader.getInstance();
        testEdition = loader.isModLoaded(TEST_ID);
        String id = testEdition ? TEST_ID : LEGAL_ID;
        settings = InterfaceSettings.load(loader.getConfigDir().resolve(id).resolve("interface.json"));
        List<ClientEdition> editions = loader.getEntrypoints("rot-rebuild-edition", ClientEdition.class);
        if (editions.size() != (testEdition ? 1 : 0)) {
            throw new IllegalStateException("Unexpected Rot Client edition entrypoints: " + editions.size());
        }
        ClientEdition edition = editions.isEmpty() ? EMPTY_EDITION : editions.getFirst();
        edition.initialize();
        modules = List.copyOf(edition.modules());
        HudLayerSettings.register();
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            var dashboardCommand = dispatcher.register(literal("rot").executes(context -> {
                Minecraft client = context.getSource().getClient();
                client.schedule(() -> openDashboard(client.gui.screen()));
                return 1;
            }));
            dispatcher.register(literal("rotclient").executes(dashboardCommand.getCommand()).redirect(dashboardCommand));
        });
    }

    public static void openDashboard(Screen parent) {
        Minecraft.getInstance().gui.setScreen(new DashboardScreen(parent));
    }

    static InterfaceSettings settings() {
        return settings;
    }

    public static boolean hideStatusEffects() {
        return settings != null && settings.hiddenVanillaLayers.contains(HudLayerSettings.STATUS_EFFECTS);
    }

    static List<ModuleCard> modules() {
        return modules;
    }

    static String editionName() {
        return testEdition ? "26.2 Mod Client Illegal" : "26.2 Mod Client Legal";
    }
}
