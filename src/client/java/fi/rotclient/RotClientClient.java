package fi.rotclient;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.loader.api.FabricLoader;
import fi.rotclient.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.InteractionResult;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class RotClientClient implements ClientModInitializer {
    public static final long PAUSE_AFTER_MILLIS = 60_000L;
    private static final long RESOURCE_OBSERVATION_WINDOW_MILLIS = 5_000L;

    static {
        RotClientLegacyDataMigrator.migrateIfNeeded(
                FabricLoader.getInstance().getConfigDir());
        RotClientTheme.apply(RotClientAppearanceStore.load());
        RotClientBackgroundManager.ensureBackgroundsDirectory();
    }

    private static final TrackerConfig CONFIG = TrackerStore.load();
    private static final RotClientHud HUD = new RotClientHud(CONFIG);
    private static final PowderChestHud POWDER_CHEST_HUD =
            new PowderChestHud(CONFIG);
    private static final QolOverlayHud QOL_HUD = new QolOverlayHud(CONFIG);
    private static final NoCursorResetController NO_CURSOR_RESET =
            new NoCursorResetController();
    private static final RotClientWorkspace WORKSPACE =
            new RotClientWorkspace();

    private static final RotClientProfileManager SETTINGS_PROFILES =
            new RotClientProfileManager();

    private static final RotClientProfileController SETTINGS_PROFILE_CONTROLLER =
            new RotClientProfileController(
                    SETTINGS_PROFILES,
                    CONFIG);

    private static final RotClientCurrentSession CURRENT_SESSION =
            new RotClientCurrentSession();
    private static final RotClientEventBus DOMAIN_EVENTS =
            new RotClientEventBus();
    private static String currentProfileId = SkyBlockProfileIdentity.UNKNOWN;
    private static boolean clickGuiKeyWasDown;
    private static boolean developerMessageShown;

    static {
        WORKSPACE.loadFromDisk();
        SETTINGS_PROFILES.loadFromDisk();
        CURRENT_SESSION.loadFromDisk();
    }

    /** Exposed for optional Mod Menu config screen factory. */
    static TrackerConfig trackerConfig() {
        return CONFIG;
    }

    /** Exposed for optional Mod Menu config screen factory. */
    static RotClientHud hud() {
        return HUD;
    }

    static QolOverlayHud qolHud() {
        return QOL_HUD;
    }

    static PowderChestHud powderChestHud() {
        return POWDER_CHEST_HUD;
    }

    static PowderChestTrackerPresentation powderChestTrackerPresentation() {
        long now = System.currentTimeMillis();
        return PowderChestTrackerPresentation.from(
                CURRENT_SESSION.snapshotConfig(),
                CONFIG.powderChestTrackerEnabled,
                CURRENT_SESSION.activeDurationMillis(now));
    }

    static void setPowderChestTrackerEnabled(boolean enabled) {
        if (CONFIG.powderChestTrackerEnabled == enabled) {
            return;
        }
        CONFIG.powderChestTrackerEnabled = enabled;
        if (!enabled) {
            SESSION_ENGINE.onPowderChestTrackerDisabled(
                    System.currentTimeMillis());
        }
        save();
    }

    private static boolean activeSettingsProfileWantsMiningTracker() {
        RotClientProfile active =
                SETTINGS_PROFILES.activeProfile();

        return active != null
                && active.settings != null
                && active.settings.miningTrackerEnabled;
    }

    static void setPowderChestHudEnabled(boolean enabled) {
        if (CONFIG.powderChestHudEnabled == enabled) return;
        CONFIG.powderChestHudEnabled = enabled;
        save();
    }

    static void reconcileSettingsProfileRuntime(
            RotClientProfileSettings previousSettings,
            RotClientProfileSettings targetSettings) {

        /*
         * Mining Tracker is runtime-sensitive.
         * Use the normal setter so SESSION_ENGINE and Current Session stay synced.
         */
        if (targetSettings != null) {
            setTrackerEnabled(
                    targetSettings.miningTrackerEnabled);
        }

        /*
         * Powder Chest Tracker needs cleanup when a profile disables it.
         */
        if (previousSettings != null
                && previousSettings.powderChestTrackerEnabled
                && !CONFIG.powderChestTrackerEnabled) {

            SESSION_ENGINE.onPowderChestTrackerDisabled(
                    System.currentTimeMillis());
        }

        /*
         * Profile switching can replace HUD positions/scales immediately.
         */
        HUD.clampToScreen();
        POWDER_CHEST_HUD.clampToScreen();

        /*
         * apply() already wrote lighting flags. Replay Always Night runtime
         * from the previous profile so dusk/snap is not skipped.
         */
        if (targetSettings != null) {
            boolean nightWas = previousSettings != null
                    && previousSettings.alwaysNightEnabled;
            boolean worldLoaded = Minecraft.getInstance() != null
                    && Minecraft.getInstance().level != null;
            FullbrightNightRuntime.onAlwaysNightChanged(
                    nightWas,
                    targetSettings.alwaysNightEnabled,
                    worldLoaded);
        }

        /*
         * Keep camera changes visually immediate.
         */
        enforceCameraPerspective();
    }

    public static NoCursorResetController noCursorReset() {
        return NO_CURSOR_RESET;
    }

    static RotClientWorkspace workspace() {
        return WORKSPACE;
    }

    static RotClientProfileManager settingsProfiles() {
        return SETTINGS_PROFILES;
    }

    static RotClientProfileController settingsProfileController() {
        return SETTINGS_PROFILE_CONTROLLER;
    }

    public static RotClientEventBus domainEvents() {
        return DOMAIN_EVENTS;
    }

    public static String currentProfileId() {
        return currentProfileId;
    }
    private static final Map<TrackedMaterial, MiningBreakDetector> BREAK_DETECTORS =
            createBreakDetectors();
    private static final Map<TrackedMaterial, MiningGainDetector> GAIN_DETECTORS =
            createGainDetectors();
    private static final MiningSessionEngine SESSION_ENGINE =
            new MiningSessionEngine();
    private static final MiningResourceCatalog SACK_RESOURCE_CATALOG =
            new MiningResourceCatalog();
    private static final MiningSessionAnalyticsController SESSION_ANALYTICS =
            new MiningSessionAnalyticsController(
                    SESSION_ENGINE,
                    text -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.keyboardHandler == null) {
                            throw new IllegalStateException(
                                    "Clipboard unavailable");
                        }
                        client.keyboardHandler.setClipboard(text);
                    },
                    RotClientClient::shadowLiveBaseline,
                    System::currentTimeMillis);
    private static MiningHudOtherSummary cachedHudOtherSummary;
    private static long cachedHudOtherSummaryAtMillis;
    private static final MiningSessionHistoryController SESSION_HISTORY =
            new MiningSessionHistoryController(
                    SESSION_ANALYTICS,
                    SESSION_ENGINE,
                    text -> {
                        Minecraft client = Minecraft.getInstance();
                        if (client.keyboardHandler == null) {
                            throw new IllegalStateException(
                                    "Clipboard unavailable");
                        }
                        client.keyboardHandler.setClipboard(text);
                    },
                    System::currentTimeMillis,
                    MiningSessionHistoryStore.defaultPath(),
                    CURRENT_SESSION);
    private static final CurrentSessionStartNewCoordinator.RecoveryResult
            START_NEW_RECOVERY_RESULT =
            CurrentSessionStartNewCoordinator.recoverPending(
                    CURRENT_SESSION,
                    SESSION_HISTORY,
                    CurrentSessionStartNewJournal.defaultPath(),
                    System.currentTimeMillis());
    private static boolean startNewRecoveryNoticePending =
            START_NEW_RECOVERY_RESULT
                    == CurrentSessionStartNewCoordinator.RecoveryResult
                    .COMPLETED_CURRENT_PUBLISH
                    || START_NEW_RECOVERY_RESULT
                    == CurrentSessionStartNewCoordinator.RecoveryResult
                    .INVALID_JOURNAL
                    || START_NEW_RECOVERY_RESULT
                    == CurrentSessionStartNewCoordinator.RecoveryResult
                    .HISTORY_UNAVAILABLE
                    || START_NEW_RECOVERY_RESULT
                    == CurrentSessionStartNewCoordinator.RecoveryResult
                    .CURRENT_PUBLISH_FAILED
                    || START_NEW_RECOVERY_RESULT
                    == CurrentSessionStartNewCoordinator.RecoveryResult
                    .CLEANUP_FAILED;

    private static final GemstoneGainDetector GEMSTONE_GAIN_DETECTOR =
            new GemstoneGainDetector(
                    new GemstoneGainDetector.Listener() {
                        @Override
                        public void onBlock(
                                GemstoneType gemstone,
                                long epochMillis) {
                            onGemstoneBlockObserved(
                                    gemstone,
                                    epochMillis);
                        }

                        @Override
                        public void onGain(
                                GemstoneType gemstone,
                                GemstoneTier tier,
                                long amount,
                                String source) {
                            onGemstoneGainObserved(
                                    gemstone,
                                    tier,
                                    amount,
                                    source);
                        }
                    });
    private static final GemstoneDiagnosticObserver
            GEMSTONE_DIAGNOSTIC_OBSERVER =
            new GemstoneDiagnosticObserver();
    private static final MobLootInventoryDetector MOB_LOOT_DETECTOR =
            new MobLootInventoryDetector(RotClientClient::creditMobLoot);
    private static final PlayerStateService PLAYER_STATE =
            new PlayerStateService();
    private static final MagicFindDetector MAGIC_FIND_DETECTOR =
            new MagicFindDetector(PLAYER_STATE);
    private static final long RANGED_COMBAT_CONTEXT_MILLIS = 8_000L;
    private static long lastLocalRangedCombatMillis = -1L;
    private static final Map<TrackedMaterial, SaleObservationGate> SALE_GATES =
            createSaleGates();
    private static final FortuneDetector FORTUNE_DETECTOR =
            new FortuneDetector(CONFIG);
    private static final BazaarTaxDetector BAZAAR_TAX_DETECTOR =
            new BazaarTaxDetector();
    private static volatile BazaarPriceService.MarketPrices marketPrices;
    private static volatile long marketPricesObservedAtMillis = -1L;
    private static long lastAutosave;

    @Override
    public void onInitializeClient() {
        CONFIG.normalize();
        applyActiveSettingsProfileOnStartup();
        CustomResourcePackRuntime.register();

        TrackingRuntimeTrace.installRuntimeStateProvider(() -> {
            RotClientCurrentSessionConfig session =
                    CURRENT_SESSION.snapshotConfig();

            String state = session.isPaused()
                    ? "PAUSED"
                    : (session.isActive() ? "RUNNING" : "UNKNOWN");

            return new TrackingRuntimeTrace.RuntimeState(
                    selectedSelection().id(),
                    state,
                    session.sessionId,
                    SESSION_ENGINE.isCollectionActive() ? "ACTIVE" : "OFF",
                    CONFIG.enabled ? "ENABLED" : "DISABLED");
        });

        /*
         * Without an active settings profile, preserve the legacy behavior:
         * Mining Tracker starts disabled every launch.
         *
         * With an active settings profile, the profile owns the Mining Tracker
         * enabled state. Start from OFF first so setTrackerEnabled(true) performs
         * the full runtime transition and does not inherit stale rotclient.json
         * state.
         */
        boolean restoreProfileTracker =
                activeSettingsProfileWantsMiningTracker();

        CONFIG.enabled = false;
        prepareLaunchState();

        if (restoreProfileTracker) {
            setTrackerEnabled(true);
        }

        /*
         * Keep rotclient.json aligned even when the active profile wants the tracker
         * disabled and setTrackerEnabled(false) therefore was unnecessary.
         */
        TrackerStore.save(CONFIG);

        // Persistent Current Session stays ACTIVE across restarts. Auto-start the
        // ephemeral diagnostics engine so OTHER/chest collection continues without
        // a manual Session Analytics Start.
        ensureCurrentSessionCollection(System.currentTimeMillis());
        new BazaarPriceService().start(prices -> Minecraft.getInstance().execute(() ->
                ClientBoundaryGuard.run("BAZAAR_PRICE_APPLY", () -> {
                    long observedAtMillis = System.currentTimeMillis();
                    MiningSessionBazaarPriceCache.publish(prices, observedAtMillis);
                    marketPrices = prices;
                    marketPricesObservedAtMillis = observedAtMillis;
                    if (isMaterialSelection()) {
                        applyMaterialPrices(
                                prices,
                                marketPricesObservedAtMillis);
                    }
                    TrackerStore.save(CONFIG);
                })));

        MarketWatchAuctionHouseService.start();
        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("rotclient", "tracker"),
                (graphics, delta) -> ClientBoundaryGuard.run(
                        "HUD_RENDER",
                        () -> {
                            if (!pauseMenuHidesHud()
                                    && !StorageOverlayRuntime.isOverlayOpen()) {
                                HUD.render(graphics, delta);
                            }
                        }));

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath(
                        "rotclient", "powder_chest_tracker"),
                (graphics, delta) -> ClientBoundaryGuard.run(
                        "POWDER_CHEST_HUD_RENDER",
                        () -> {
                            if (!pauseMenuHidesHud()
                                    && !StorageOverlayRuntime.isOverlayOpen()) {
                                POWDER_CHEST_HUD.render(graphics);
                            }
                        }));

        HudElementRegistry.addLast(
                Identifier.fromNamespaceAndPath("rotclient", "qol_overlay"),
                (graphics, delta) -> ClientBoundaryGuard.run(
                        "QOL_HUD_RENDER",
                        () -> {
                            if (!StorageOverlayRuntime.isOverlayOpen()) {
                                QOL_HUD.render(graphics);
                            }
                        }));

        registerVanillaHudHides();

        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            SkyBlockTooltipRuntime.append(stack, lines);
            MissingEnchantsRuntime.append(stack, lines);
            FishingSuiteRuntime.appendTooltip(stack, lines);
            StallMarketRuntime.appendTooltip(stack, lines);
        });
        SkyBlockMarketQuoteService.start();
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            ClientBoundaryGuard.run(
                    "AUTO_CLICKER",
                    () -> AutoClickerRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "IOTA",
                    () -> IotaRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "STALL_MARKET",
                    () -> StallMarketRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "AUTO_EXPERIMENTS",
                    () -> AutoExperimentsRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "AUTO_HARP",
                    () -> AutoHarpRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "AUTO_GFS",
                    () -> AutoGfsRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "AUTO_SELL",
                    () -> AutoSellRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "QOL_MODULE_KEYBINDS",
                    () -> QolModuleKeybindRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "ETHERWARP_HELPER",
                    () -> EtherwarpHelperRuntime.tick(client));
        });
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            ClientBoundaryGuard.run(
                    "SLAYER_ATTACK",
                    () -> SlayerRuntime.onAttack(entity));
            return InteractionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            TrackingRuntimeTrace.callbackAlive("ClientTick");
            ClientBoundaryGuard.run(
                    "HOT_PATH_CACHE",
                    () -> {
                        NameHiderRuntime.beginTick();
                        QolVisualRuntime.beginTick();
                    });
            ClientBoundaryGuard.run(
                    "HYPIXEL_MOD_API",
                    () -> OptionalHypixelModApiRuntime.tick(
                            RotClientClient::onOfficialLocation,
                            System.currentTimeMillis()));
            ClientBoundaryGuard.run(
                    "MOD_COMPATIBILITY",
                    () -> CompatibilityWarningRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "CAMERA_ENFORCE",
                    RotClientClient::enforceCameraPerspective);
            ClientBoundaryGuard.run(
                    "FREECAM",
                    () -> FreecamRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "CLICK_GUI_KEY",
                    () -> tickClickGuiKey(client));
            ClientBoundaryGuard.run(
                    "NO_CURSOR_RESET",
                    () -> restoreStorageCursor(client));
            ClientBoundaryGuard.run(
                    "INVENTORY_WALK",
                    () -> InventoryWalkRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "INVENTORY_CHROME",
                    () -> InventoryChromeRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "STORAGE_OVERLAY",
                    () -> StorageOverlayRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "CUSTOM_CURSOR",
                    () -> CustomCursorRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "CUSTOM_RESOURCE_PACK",
                    () -> CustomResourcePackRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "ITEM_TOOLS",
                    () -> ItemToolsRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "UI_FRAME_PACER",
                    () -> RotClientUiClock.keepInputFresh(client));
            ClientBoundaryGuard.run(
                    "COMMAND_KEYBINDS",
                    () -> RingKeybindsRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "WARDROBE_KEYBINDS",
                    () -> {
                        MenuKeybindRuntime.tick(client);
                        WardrobeAutoEquipRuntime.tick(client);
                    });
            ClientBoundaryGuard.run(
                    "WORLD_SCANNER",
                    () -> WorldScannerRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "AUTO_CONVERSATION",
                    () -> AutoConversationRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "FISHING_HELPER",
                    () -> FishingHelperRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "FISHING_SUITE",
                    () -> FishingSuiteRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "COMMISSION_DISPLAY",
                    () -> CommissionDisplayRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "MINING_LEFTOVER",
                    () -> MiningLeftoverRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "DIANA",
                    () -> DianaRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "FORAGING",
                    () -> ForagingRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "MOB_HIGHLIGHT",
                    () -> MobHighlightRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "SLAYER_RUNTIME",
                    () -> SlayerRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "FARM_KEYS",
                    () -> FarmKeysRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "DUNGEON_RUNTIME",
                    () -> DungeonRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "AUTO_DOJO",
                    () -> AutoDojoRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "TRAJECTORIES",
                    () -> TrajectoryRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "CHAT_COMMANDS",
                    () -> ChatCommandsRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "WAYPOINTS",
                    () -> WaypointRuntime.tick(client));
            ClientBoundaryGuard.run(
                    "SLOT_BINDS",
                    () -> SlotBindsRuntime.tick(client));
            // External Minecraft world/UI reads only — not a whole-tick swallow.
            ClientBoundaryGuard.run(
                    "AIM_EVIDENCE",
                    () -> SharedMiningAimEvidence.tick(client));
            ClientBoundaryGuard.run(
                    "GEMSTONE_DIAG",
                    () -> GEMSTONE_DIAGNOSTIC_OBSERVER.tick(client));
            ClientBoundaryGuard.run(
                    "MOB_LOOT_INVENTORY",
                    () -> MOB_LOOT_DETECTOR.observe(
                            client,
                            mobLootCollectionAllowed(),
                            "client-tick"));
            ClientBoundaryGuard.run(
                    "MOB_LOOT_RANGED",
                    () -> noteLocalRangedUse(client));
            if (CURRENT_SESSION.isActive()) {
                ClientBoundaryGuard.run(
                        "MAGIC_FIND_DETECT",
                        () -> {
                            MAGIC_FIND_DETECTOR.tick(client);
                            int magicFind = PLAYER_STATE.latest().magicFind();
                            if (magicFind >= 0) {
                                CURRENT_SESSION.noteMagicFind(
                                        magicFind,
                                        System.currentTimeMillis());
                            }
                        });
            }

            if (isMaterialSelection()
                    || SESSION_ENGINE.isCollectionActive()) {
                ClientBoundaryGuard.run("GAIN_DETECTORS", () -> {
                    for (MiningGainDetector detector : GAIN_DETECTORS.values()) {
                        detector.tick(client);
                    }
                });
            }

            ClientBoundaryGuard.run("GEMSTONE_GAIN", () ->
                    GEMSTONE_GAIN_DETECTOR.tick(
                            client,
                            isGemstoneTrackingActive()
                                    || SESSION_ENGINE.isObservationEnabled()
                                    || SESSION_ENGINE.isCollectionActive()));
            List<TrackedMaterial> activeBreakDetectors = new ArrayList<>();
            if (SESSION_ENGINE.isObservationEnabled()
                    || SESSION_ENGINE.isCollectionActive()) {
                for (TrackedMaterial material : TrackedMaterial.values()) {
                    if (SESSION_ENGINE.supportsMaterialBreak(material)) {
                        activeBreakDetectors.add(material);
                    }
                }
            } else if (CONFIG.enabled && isMaterialSelection()) {
                activeBreakDetectors.addAll(selectedMaterials());
            }
            if (!activeBreakDetectors.isEmpty()) {
                ClientBoundaryGuard.run("BREAK_DETECTORS", () -> {
                    MiningBreakScanFrame frame =
                            MiningBreakScanFrame.capture(client);
                    MiningBreakScanProfiler.observe(
                            frame, activeBreakDetectors.size());
                    for (TrackedMaterial material : activeBreakDetectors) {
                        BREAK_DETECTORS.get(material).tick(
                                client,
                                frame,
                                CONFIG.enabled && tracksMaterial(material));
                    }
                });
            }
            if (CONFIG.enabled && isMaterialSelection()) {
                ClientBoundaryGuard.run(
                        "FORTUNE_DETECT",
                        () -> FORTUNE_DETECTOR.tick(client));
            }
            ClientBoundaryGuard.run(
                    "BAZAAR_TAX",
                    () -> BAZAAR_TAX_DETECTOR.tick(client));

            long now = System.currentTimeMillis();
            ClientBoundaryGuard.run(
                    "AREA_DETECT",
                    () -> updateDetectedArea(client, now));
            SESSION_ENGINE.onDiagnosticTick(now);
            CURRENT_SESSION.heartbeat(now);
            syncCurrentSessionFromEngine(now);
            CURRENT_SESSION.flushIfDirty(now, false);
            showPendingCurrentSessionPersistenceWarning(client);
            if (now - lastAutosave >= 10_000L) {
                ClientBoundaryGuard.run(
                        "TRACKER_AUTOSAVE",
                        RotClientClient::save);
                lastAutosave = now;
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            StorageOverlayRuntime.flushForShutdown();
            InventoryChromeRuntime.flushForShutdown();
            long now = System.currentTimeMillis();
            TrackerSelection selection =
                    selectedSelection();

            if (selection.isMaterial()) {
                TrackingTarget target =
                        selection.materialTarget();
                if (target.isCombined()) {
                    persistCombinedActiveTime(now);
                    CONFIG.combinedLastBreakEpochMillis = 0;
                }
                for (TrackedMaterial material : target.materials()) {
                    persistActiveTime(material, now);
                    CONFIG.state(material).lastBreakEpochMillis = 0;
                }
            } else {
                GemstoneTrackerState state =
                        CONFIG.gemstoneState(
                                selection.gemstone());

                state.persistActiveTime(
                        now,
                        PAUSE_AFTER_MILLIS);

                state.lastBreakEpochMillis =
                        0L;
            }

            save();
            syncCurrentSessionFromEngine(now);
            // Pause Current Session so offline wall-clock is excluded from
            // active duration; stop only the ephemeral diagnostics engine.
            CURRENT_SESSION.pauseForOffline(now);
            CURRENT_SESSION.flushIfDirty(now, true);
            GEMSTONE_GAIN_DETECTOR.reset();
            SESSION_ENGINE.onDiagnosticStop(now);
            DiagnosticRecorder.stop();
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            long now = System.currentTimeMillis();
            onShadowWorldChanged();
            if (CURRENT_SESSION.resumeAfterOffline(now)) {
                ensureCurrentSessionCollection(now);
            }
            showPendingCurrentSessionPersistenceWarning(client);
            showPendingStartNewRecoveryNotice(client);
            enforceCameraPerspective();
            WaypointRuntime.clear();
            ChatCommandsRuntime.clear();
            IotaRuntime.clear();
            StallMarketRuntime.clear();
            SlotBindsRuntime.clearPending();
            MiningLeftoverRuntime.clear();
            DianaRuntime.clear();
            ForagingRuntime.clear();
            FishingSuiteRuntime.clear();
            InventoryChromeRuntime.loadCache();
            StorageOverlayRuntime.onJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            long now = System.currentTimeMillis();
            syncCurrentSessionFromEngine(now);
            if (SESSION_ENGINE.isCollectionActive()) {
                SESSION_ANALYTICS.stop();
            }
            CURRENT_SESSION.pauseForOffline(now);
            CURRENT_SESSION.flushIfDirty(now, true);
            onShadowWorldChanged();
            WorldScannerRuntime.clear();
            MiningLeftoverRuntime.clear();
            DianaRuntime.clear();
            ForagingRuntime.clear();
            FishingSuiteRuntime.clear();
            ChatCommandsRuntime.clear();
            IotaRuntime.clear();
            StallMarketRuntime.clear();
            WaypointRuntime.clear();
            SlotBindsRuntime.clearPending();
            InventoryChromeRuntime.flushForShutdown();
            InventoryChromeRuntime.clear();
            enforceCameraPerspective();
        });
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) ->
                ClientBoundaryGuard.run(
                        "WORLD_SCANNER_CHUNK",
                        () -> {
                            if (world instanceof net.minecraft.client.multiplayer.ClientLevel level
                                    && chunk instanceof net.minecraft.world.level.chunk.LevelChunk loaded) {
                                WorldScannerRuntime.onChunkLoad(level, loaded);
                            }
                        }));
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
                ClientBoundaryGuard.run(
                        overlay ? "ACTIONBAR_MESSAGE" : "GAME_MESSAGE",
                        () -> {
                            TrackingRuntimeTrace.callbackAlive(
                                    overlay ? "ActionBar" : "GameMessage");
                            String plain = message == null
                                    ? ""
                                    : message.getString().trim();
                            DOMAIN_EVENTS.publish(
                                    new RotClientDomainEvents.TextObserved(
                                            RotClientDomainEvents.Source.FABRIC_MESSAGE,
                                            overlay
                                                    ? RotClientDomainEvents.TextKind.ACTION_BAR
                                                    : RotClientDomainEvents.TextKind.GAME,
                                            plain,
                                            System.currentTimeMillis()));
                            TrackingRuntimeTrace.gameMessage(
                                    overlay,
                                    ActionBarGainParser.normalizePrefix(plain));
                            if (overlay) {
                                SlayerRuntime.onActionBar(message);
                                observeActionBarMessage(message);
                                observeMobLootOverlay(message);
                                if (message != null) {
                                    QOL_HUD.statsTracker().observeActionBar(
                                            message.getString());
                                    IotaKuudraRuntime.onTitle(message);
                                }
                            }
                            if (isMaterialSelection()) {
                                FORTUNE_DETECTOR.inspectMessage(message);
                            }
                            GEMSTONE_GAIN_DETECTOR.inspectMessage(
                                    message,
                                    isGemstoneTrackingActive());
                            MiningLeftoverRuntime.onChat(message, overlay);
                            if (!overlay) {
                                SlayerRuntime.onChat(message);
                                DungeonRuntime.onChat(message);
                                IotaRuntime.onGameMessage(message);
                                if (!IotaRuntime.consumedPartyCommand()) {
                                    ChatCommandsRuntime.onGameMessage(message);
                                }
                                WaypointRuntime.onGameMessage(message);
                                RingKeybindsRuntime.rememberChat(message);
                                EscrowFixRuntime.onChat(message);
                                AutoGfsRuntime.onChat(message);
                                FishingSuiteRuntime.onChat(message);
                                MiningLeftoverRuntime.onChat(message, overlay);
                                DianaRuntime.onChat(message);
                                ForagingRuntime.onChat(message);
                            }
                        }));

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) {
                return true;
            }
            String plain = message == null ? "" : message.getString();
            RewardClaimRuntime.onChat(plain);
            if (RewardClaimRuntime.shouldHideMessage(plain)) {
                return false;
            }
            if (IotaRuntime.shouldMuteTerminatorChat(plain)) {
                return false;
            }
            if (!FishingSuiteRuntime.allowGameMessage(message)) {
                return false;
            }
            if (!MiningLeftoverRuntime.allowGameMessage(message)) {
                MiningLeftoverRuntime.onChat(message, false);
                return false;
            }
            if (!DianaRuntime.allowGameMessage(message)) {
                return false;
            }
            if (!ForagingRuntime.allowGameMessage(message)) {
                ForagingRuntime.onChat(message);
                return false;
            }
            return ClientBoundaryGuard.call("SLAYER_ALLOW_GAME", () -> {
                if (SlayerRuntime.shouldHideRngMeterChat(message)) {
                    // GAME is not fired for canceled messages. Observe first so the
                    // canonical Slayer session and optional local HUD stay current.
                    SlayerRuntime.onChat(message);
                    return false;
                }
                return !SlayerRuntime.shouldHideInfernoChat(message);
            }, true);
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            DOMAIN_EVENTS.publish(new RotClientDomainEvents.ScreenOpened(
                    RotClientDomainEvents.Source.FABRIC_SCREEN,
                    screen == null ? "" : screen.getClass().getName(),
                    width,
                    height,
                    System.currentTimeMillis()));
            WardrobeAutoEquipRuntime.onScreenOpened(screen);
        });
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                StallMarketRuntime.onScreenOpened(screen));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                AutoExperimentsRuntime.onScreenOpened(screen));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                AutoHarpRuntime.onScreenOpened(screen));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) ->
                registerTooltipAndStorageScroll(screen));
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof PauseScreen pauseScreen)) return;
            if (pauseScreen.showsPauseMenu()) {
                addPauseMenuButton(client, screen, width, height);
            }
            ScreenMouseEvents.allowMouseClick(screen).register((current, event) ->
                    ClientBoundaryGuard.call(
                            "HUD_EDITOR_CLICK",
                            () -> {
                                if (event.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                                        || !event.hasShiftDown()) {
                                    return true;
                                }
                                if (QOL_HUD.beginDrag(event.x(), event.y())) {
                                    return false;
                                }
                                if (POWDER_CHEST_HUD.beginDrag(
                                        event.x(), event.y())) {
                                    return false;
                                }
                                return !HUD.beginDrag(event.x(), event.y());
                            },
                            true));
            ScreenMouseEvents.allowMouseDrag(screen).register(
                    (current, event, dx, dy) ->
                            ClientBoundaryGuard.call(
                                    "HUD_EDITOR_DRAG",
                                    () -> !(QOL_HUD.dragTo(event.x(), event.y())
                                            || POWDER_CHEST_HUD.dragTo(
                                            event.x(), event.y())
                                            || HUD.dragTo(event.x(), event.y())),
                                    true));
            ScreenMouseEvents.allowMouseRelease(screen).register(
                    (current, event) ->
                            ClientBoundaryGuard.call(
                                    "HUD_EDITOR_RELEASE",
                                    () -> {
                                        if (event.button()
                                                != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                                            return true;
                                        }
                                        boolean qol = QOL_HUD.endDrag();
                                        boolean powder =
                                                POWDER_CHEST_HUD.endDrag();
                                        boolean mining = HUD.endDrag();
                                        return !(qol || powder || mining);
                                    },
                                    true));
            ScreenMouseEvents.allowMouseScroll(screen).register(
                    (current, mouseX, mouseY, horizontal, vertical) ->
                            ClientBoundaryGuard.call(
                                    "HUD_EDITOR_SCROLL",
                                    () -> {
                                        boolean shift = GLFW.glfwGetKey(
                                                client.getWindow().handle(),
                                                GLFW.GLFW_KEY_LEFT_SHIFT)
                                                == GLFW.GLFW_PRESS
                                                || GLFW.glfwGetKey(
                                                client.getWindow().handle(),
                                                GLFW.GLFW_KEY_RIGHT_SHIFT)
                                                == GLFW.GLFW_PRESS;
                                        return !(shift && (
                                                POWDER_CHEST_HUD.onScroll(
                                                        mouseX,
                                                        mouseY,
                                                        vertical)
                                                || HUD.onScroll(
                                                        mouseX,
                                                        mouseY,
                                                        vertical)));
                                    },
                                    true));
            ScreenEvents.remove(screen).register(current ->
                    ClientBoundaryGuard.run(
                            "HUD_EDITOR_CLOSE",
                            () -> {
                                HUD.setEditorOpen(false);
                                POWDER_CHEST_HUD.setEditorOpen(false);
                                QOL_HUD.setEditorOpen(false);
                            }));
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildCommandTree("rot", null));
            dispatcher.register(buildCommandTree("rotclient", "rotclient"));
            dispatcher.register(buildCommandTree("miningtracker", "miningtracker"));
            dispatcher.register(literal("MiningTracker")
                    .executes(context -> runCommand(
                            context.getSource(),
                            "MiningTracker",
                            RotClientClient::openMiningUi)));
            dispatcher.register(literal("miningui")
                    .executes(context -> runCommand(
                            context.getSource(),
                            "miningui",
                            RotClientClient::openMiningUi)));
            dispatcher.register(literal("termsim")
                    .executes(context -> openTermSim(context.getSource()))
                    .then(argument("ping", IntegerArgumentType.integer(0, 500))
                            .executes(context -> openTermSimPing(
                                    context.getSource(),
                                    IntegerArgumentType.getInteger(context, "ping")))));
        });
    }

    @FunctionalInterface
    private interface CommandAction {
        int run(FabricClientCommandSource source);
    }

    /**
     * Runs a command action. When {@code legacyAlias} is non-null, emits exactly
     * one alias-specific deprecation notice before executing.
     */
    private static int runCommand(
            FabricClientCommandSource source,
            String legacyAlias,
            CommandAction action) {
        if (legacyAlias != null) {
            source.sendFeedback(Component.literal(
                    RotClientCommandNotices.deprecationNotice(legacyAlias)));
        }
        return action.run(source);
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommandTree(
            String rootName,
            String legacyAlias) {
        return literal(rootName)
                .executes(context -> runCommand(
                        context.getSource(),
                        legacyAlias,
                        RotClientClient::openMiningUi))
                .then(literal("ui")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::openMiningUi)))
                .then(literal("qol")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::openQolUi)))
                .then(literal("help")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::rotClientHelp)))
                .then(literal("edit")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::openEditor)))
                .then(literal("layout")
                        .then(literal("reset")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> resetLayout(source, true, true)))
                                .then(literal("ui")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> resetLayout(source, true, false))))
                                .then(literal("hud")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> resetLayout(source, false, true))))))
                .then(literal("toggle")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::toggle)))
                .then(literal("reset")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::resetSession)))
                .then(literal("status")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::status)))
                .then(literal("shadow")
                        .then(literal("status")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::shadowStatus))))
                .then(literal("autoclicker")
                        .then(literal("add")
                                .then(literal("left")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> autoClickerAdd(source, true))))
                                .then(literal("right")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> autoClickerAdd(source, false)))))
                        .then(literal("remove")
                                .then(literal("left")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> autoClickerRemove(source, true))))
                                .then(literal("right")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> autoClickerRemove(source, false)))))
                        .then(literal("list")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::autoClickerList))))
                .then(literal("bazaarsearch")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                source -> {
                                    StallMarketRuntime.search("");
                                    return 1;
                                }))
                        .then(argument("item", StringArgumentType.greedyString())
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> {
                                            StallMarketRuntime.search(
                                                    StringArgumentType.getString(context, "item"));
                                            return 1;
                                        }))))
                .then(literal("session")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::sessionHelp))
                        .then(literal("help")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionHelp)))
                        .then(literal("start")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionStart)))
                        .then(literal("stop")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionStop)))
                        .then(literal("reset")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionReset)))
                        .then(literal("status")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionStatus)))
                        .then(literal("copy")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionCopy)))
                        .then(literal("save")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::sessionSave))))
                .then(literal("history")
                        .executes(context -> runCommand(
                                context.getSource(),
                                legacyAlias,
                                RotClientClient::historyHelp))
                        .then(literal("help")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::historyHelp)))
                        .then(literal("list")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::historyList)))
                        .then(literal("open")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> historyUsage(source, "open")))
                                .then(argument(
                                        "indexOrId",
                                        StringArgumentType.word())
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> historyOpen(
                                                        source,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "indexOrId"))))))
                        .then(literal("copy")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> historyUsage(source, "copy")))
                                .then(argument(
                                        "indexOrId",
                                        StringArgumentType.word())
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> historyCopy(
                                                        source,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "indexOrId"))))))
                        .then(literal("delete")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> historyUsage(
                                                source,
                                                "delete")))
                                .then(argument(
                                        "indexOrId",
                                        StringArgumentType.word())
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> historyDelete(
                                                        source,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "indexOrId"))))))
                        .then(literal("clear")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::historyClear))
                                .then(literal("confirm")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> historyUsage(
                                                        source,
                                                        "clear confirm")))
                                        .then(argument(
                                                "token",
                                                StringArgumentType.word())
                                                .executes(context -> runCommand(
                                                        context.getSource(),
                                                        legacyAlias,
                                                        source ->
                                                                historyClearConfirm(
                                                                        source,
                                                                        StringArgumentType
                                                                                .getString(
                                                                                        context,
                                                                                        "token"))))))))
                .then(literal("slayer")
                        .executes(context -> runCommand(
                                context.getSource(), legacyAlias, RotClientClient::slayerHelp))
                        .then(literal("status")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias,
                                        RotClientClient::slayerStatus)))
                        .then(literal("stats")
                                .then(literal("reset")
                                        .executes(context -> runCommand(
                                                context.getSource(), legacyAlias,
                                                RotClientClient::slayerStatsReset))))
                        .then(literal("carry")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias,
                                        RotClientClient::slayerCarryManager))
                                .then(literal("manager")
                                        .executes(context -> runCommand(
                                                context.getSource(), legacyAlias,
                                                RotClientClient::slayerCarryManager)))
                                .then(literal("list")
                                        .executes(context -> runCommand(
                                                context.getSource(), legacyAlias,
                                                RotClientClient::slayerCarryList)))
                                .then(literal("add")
                                        .then(argument("player", StringArgumentType.word())
                                                .then(argument("count", IntegerArgumentType.integer(1, 10_000))
                                                        .then(argument("type", StringArgumentType.word())
                                                                .then(argument("tier", IntegerArgumentType.integer(0, 5))
                                                                        .executes(context -> runCommand(
                                                                                context.getSource(), legacyAlias,
                                                                                source -> slayerCarryAdd(
                                                                                        source,
                                                                                        StringArgumentType.getString(context, "player"),
                                                                                        IntegerArgumentType.getInteger(context, "count"),
                                                                                        StringArgumentType.getString(context, "type"),
                                                                                        IntegerArgumentType.getInteger(context, "tier")))))))))
                                .then(literal("remove")
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(context -> runCommand(
                                                        context.getSource(), legacyAlias,
                                                        source -> slayerCarryRemove(
                                                                source,
                                                                StringArgumentType.getString(context, "player"))))))
                                .then(literal("complete")
                                        .then(argument("player", StringArgumentType.word())
                                                .executes(context -> runCommand(
                                                        context.getSource(), legacyAlias,
                                                        source -> slayerCarryComplete(
                                                                source,
                                                                StringArgumentType.getString(context, "player"))))))))
                .then(literal("termsim")
                        .executes(context -> runCommand(
                                context.getSource(), legacyAlias, RotClientClient::openTermSim))
                        .then(argument("ping", IntegerArgumentType.integer(0, 500))
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> openTermSimPing(
                                                source,
                                                IntegerArgumentType.getInteger(context, "ping"))))))
                .then(literal("superboom")
                        .then(literal("add")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias, RotClientClient::superboomAdd)))
                        .then(literal("remove")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias, RotClientClient::superboomRemove)))
                        .then(literal("list")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias, RotClientClient::superboomList))))
                .then(literal("dcarry")
                        .executes(context -> runCommand(
                                context.getSource(), legacyAlias, RotClientClient::dungeonCarryManager))
                        .then(literal("list")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias, RotClientClient::dungeonCarryList)))
                        .then(literal("history")
                                .executes(context -> runCommand(
                                        context.getSource(), legacyAlias, RotClientClient::dungeonCarryHistory)))
                        .then(literal("add")
                                .then(argument("player", StringArgumentType.word())
                                        .then(argument("count", IntegerArgumentType.integer(1, 10_000))
                                                .then(argument("floor", StringArgumentType.word())
                                                        .executes(context -> runCommand(
                                                                context.getSource(), legacyAlias,
                                                                source -> dungeonCarryAdd(
                                                                        source,
                                                                        StringArgumentType.getString(context, "player"),
                                                                        IntegerArgumentType.getInteger(context, "count"),
                                                                        StringArgumentType.getString(context, "floor"))))))))
                        .then(literal("remove")
                                .then(argument("player", StringArgumentType.word())
                                        .executes(context -> runCommand(
                                                context.getSource(), legacyAlias,
                                                source -> dungeonCarryRemove(
                                                        source,
                                                        StringArgumentType.getString(context, "player")))))))
                .then(literal("target")
                        .then(literal("coal")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.COAL))))
                        .then(literal("iron")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.IRON))))
                        .then(literal("gold")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.GOLD))))
                        .then(literal("lapis")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.LAPIS))))
                        .then(literal("redstone")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.REDSTONE))))
                        .then(literal("emerald")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.EMERALD))))
                        .then(literal("diamond")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.DIAMOND))))
                        .then(literal("quartz")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.QUARTZ))))
                        .then(literal("mithril")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.MITHRIL_TITANIUM))))
                        .then(literal("titanium")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.MITHRIL_TITANIUM))))
                        .then(literal("tungsten")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.TUNGSTEN))))
                        .then(literal("umber")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setTarget(
                                                source,
                                                TrackingTarget.UMBER)))))
                .then(literal("record")
                        .then(literal("start")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::startRecording)))
                        .then(literal("stop")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::stopRecording))))
                .then(literal("debug")
                        .then(literal("tracking")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::trackingDebugStatus))
                                .then(literal("on")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                RotClientClient::trackingDebugOn)))
                                .then(literal("off")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                RotClientClient::trackingDebugOff)))
                                .then(literal("clear")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                RotClientClient::trackingDebugClear)))
                                .then(literal("status")
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                RotClientClient::trackingDebugStatus)))))
                .then(literal("fortune")
                        .then(literal("auto")
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        RotClientClient::autoFortune)))
                        .then(argument(
                                "mining",
                                DoubleArgumentType.doubleArg(0, 100_000))
                                .executes(context -> runCommand(
                                        context.getSource(),
                                        legacyAlias,
                                        source -> setFortune(
                                                source,
                                                DoubleArgumentType.getDouble(
                                                        context, "mining"),
                                                0)))
                                .then(argument(
                                        "material",
                                        DoubleArgumentType.doubleArg(
                                                0, 100_000))
                                        .executes(context -> runCommand(
                                                context.getSource(),
                                                legacyAlias,
                                                source -> setFortune(
                                                        source,
                                                        DoubleArgumentType.getDouble(
                                                                context, "mining"),
                                                        DoubleArgumentType.getDouble(
                                                                context, "material")))))));
    }

    private static int slayerHelp(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(
                "Rot Client Slayer: /rot slayer status | carry manager | add <player> <count> <type> <tier 0-5> | remove <player> | list | complete <player> | stats reset"));
        return 1;
    }

    private static int slayerStatus(FabricClientCommandSource source) {
        long now = System.currentTimeMillis();
        SlayerSessionEngine.Snapshot snapshot = SlayerRuntime.engine().snapshot(now);
        source.sendFeedback(Component.literal(
                "Rot Client Slayer · " + snapshot.questState()
                        + " · bosses " + snapshot.bossesKilled()
                        + " · active entities " + snapshot.activeBosses().size()
                        + " · carries " + snapshot.carries().size()));
        source.sendFeedback(Component.literal(
                "Average kill " + SlayerRuntime.duration(snapshot.averageKillDurationMillis())
                        + " · last kill " + SlayerRuntime.duration(snapshot.lastKillDurationMillis())
                        + " · bosses since rare drop " + snapshot.bossesSinceLastDrop()));
        if (snapshot.dropCounts().isEmpty()) {
            source.sendFeedback(Component.literal("Rare drops: none observed in this run."));
        } else {
            snapshot.dropCounts().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                    .forEach(entry -> source.sendFeedback(Component.literal(
                            "- " + entry.getKey() + " · " + entry.getValue())));
        }
        return 1;
    }

    private static int slayerStatsReset(FabricClientCommandSource source) {
        SlayerRuntime.engine().resetStats(System.currentTimeMillis());
        source.sendFeedback(Component.literal("Rot Client Slayer stats reset."));
        return 1;
    }

    private static int slayerCarryManager(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.gui == null ? null : client.gui.screen();
        client.gui.setScreen(new SlayerCarryManagerScreen(parent));
        return 1;
    }

    private static int slayerCarryAdd(
            FabricClientCommandSource source,
            String player,
            int count,
            String typeName,
            int tier) {
        Optional<SlayerPolicy.SlayerType> type = SlayerPolicy.slayerType(typeName);
        if (type.isEmpty()) {
            source.sendError(Component.literal(
                    "Unknown Slayer type. Use rev, tara, sven, void, blaze or vamp."));
            return 0;
        }
        boolean added = SlayerRuntime.engine().addCarry(
                player, type.get(), tier, count, System.currentTimeMillis());
        if (!added) {
            source.sendError(Component.literal(
                    "Could not add carry. Check player name or remove the existing entry first."));
            return 0;
        }
        source.sendFeedback(Component.literal(
                "Slayer carry added: " + player + " · " + type.get().displayName()
                        + (tier == 0 ? " · any tier" : " · tier " + tier)
                        + " · 0/" + count));
        return 1;
    }

    private static int slayerCarryRemove(
            FabricClientCommandSource source,
            String player) {
        if (!SlayerRuntime.engine().removeCarry(player)) {
            source.sendError(Component.literal("No active Slayer carry for " + player + "."));
            return 0;
        }
        source.sendFeedback(Component.literal("Removed Slayer carry for " + player + "."));
        return 1;
    }

    private static int slayerCarryComplete(
            FabricClientCommandSource source,
            String player) {
        if (!SlayerRuntime.completeCarry(player)) {
            source.sendError(Component.literal("No incomplete Slayer carry for " + player + "."));
            return 0;
        }
        source.sendFeedback(Component.literal("Advanced Slayer carry for " + player + "."));
        return 1;
    }

    private static int slayerCarryList(FabricClientCommandSource source) {
        List<SlayerSessionEngine.Carry> carries = SlayerRuntime.engine().carries();
        if (carries.isEmpty()) {
            source.sendFeedback(Component.literal("No active Slayer carries."));
            return 1;
        }
        source.sendFeedback(Component.literal("Active Slayer carries:"));
        for (SlayerSessionEngine.Carry carry : carries) {
            source.sendFeedback(Component.literal(
                    "- " + carry.player() + " · " + carry.type().displayName()
                            + (carry.tier() == 0 ? " · any tier" : " · tier " + carry.tier())
                            + " · " + carry.completed() + "/" + carry.total()));
        }
        return 1;
    }

    private static int superboomAdd(FabricClientCommandSource source) {
        String blockId = lookedBlockId();
        if (blockId.isBlank()) {
            source.sendError(Component.literal("Look at a block first. Usage: /rot superboom add"));
            return 0;
        }
        DungeonAthenSettings athen = qolConfigPublic().extras().athen();
        athen.superboomExtraBlocks = DungeonAthenPortPolicy.addExtraBlock(athen.superboomExtraBlocks, blockId);
        TrackerStore.save(CONFIG);
        source.sendFeedback(Component.literal("Superboom extra block added: " + DungeonLeftoverPolicy.path(blockId)));
        return 1;
    }

    private static int superboomRemove(FabricClientCommandSource source) {
        String blockId = lookedBlockId();
        if (blockId.isBlank()) {
            source.sendError(Component.literal("Look at a block first. Usage: /rot superboom remove"));
            return 0;
        }
        DungeonAthenSettings athen = qolConfigPublic().extras().athen();
        athen.superboomExtraBlocks = DungeonAthenPortPolicy.removeExtraBlock(athen.superboomExtraBlocks, blockId);
        TrackerStore.save(CONFIG);
        source.sendFeedback(Component.literal("Superboom extra block removed: " + DungeonLeftoverPolicy.path(blockId)));
        return 1;
    }

    private static int superboomList(FabricClientCommandSource source) {
        String csv = qolConfigPublic().extras().athen().superboomExtraBlocks;
        if (csv == null || csv.isBlank()) {
            source.sendFeedback(Component.literal(
                    "No extra Superboom blocks. Defaults still include cracked stone bricks and crypt walls."));
            return 1;
        }
        source.sendFeedback(Component.literal("Superboom extra blocks: " + csv));
        return 1;
    }

    private static String lookedBlockId() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || !(client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
                || hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return "";
        }
        return DungeonRuntime.lookedBlockId(client, hit.getBlockPos());
    }

    private static int dungeonCarryManager(FabricClientCommandSource source) {
        Minecraft client = Minecraft.getInstance();
        Screen parent = client.gui == null ? null : client.gui.screen();
        client.gui.setScreen(new DungeonCarryManagerScreen(parent));
        return 1;
    }

    private static int dungeonCarryAdd(
            FabricClientCommandSource source, String player, int count, String floor) {
        String start = DungeonCarryRuntime.add(player, floor, count);
        if (start.isBlank() && DungeonCarryPolicy.sanitizePlayer(player).isBlank()) {
            source.sendError(Component.literal("Could not add dungeon carry. Check the player name."));
            return 0;
        }
        source.sendFeedback(Component.literal(
                "Dungeon carry added: " + DungeonCarryPolicy.sanitizePlayer(player)
                        + " · " + DungeonCarryPolicy.normalizeFloor(floor)
                        + " · 0/" + Math.max(1, count)));
        return 1;
    }

    private static int dungeonCarryRemove(FabricClientCommandSource source, String player) {
        if (!DungeonCarryRuntime.remove(player)) {
            source.sendError(Component.literal("No active dungeon carry for " + player + "."));
            return 0;
        }
        source.sendFeedback(Component.literal("Removed dungeon carry for " + player + "."));
        return 1;
    }

    private static int dungeonCarryList(FabricClientCommandSource source) {
        List<String> lines = DungeonCarryRuntime.listLines();
        if (lines.isEmpty()) {
            source.sendFeedback(Component.literal("No active dungeon carries."));
            return 1;
        }
        source.sendFeedback(Component.literal("Active dungeon carries:"));
        for (String line : lines) {
            source.sendFeedback(Component.literal("- " + line));
        }
        return 1;
    }

    private static int dungeonCarryHistory(FabricClientCommandSource source) {
        List<String> lines = DungeonCarryRuntime.historyLines();
        if (lines.isEmpty()) {
            source.sendFeedback(Component.literal("No completed dungeon carries."));
            return 1;
        }
        source.sendFeedback(Component.literal("Completed dungeon carries:"));
        for (String line : lines) {
            source.sendFeedback(Component.literal("- " + line));
        }
        return 1;
    }

    public static void onBlocksBroken(TrackedMaterial material, int count) {
        onBlocksBroken(
                material,
                count,
                safeMultiply(count, material == null ? 0 : material.baseDrop()));
    }

    public static void onBlocksBroken(
            TrackedMaterial material, int count, long baseDrops) {
        long now = System.currentTimeMillis();
        if (count <= 0 || material == null) {
            return;
        }
        TrackingRuntimeTrace.miningEvidence(
                material.id(),
                "confirmed-break",
                count,
                "");
        if (!CONFIG.enabled || !tracksMaterial(material)) {
            if (!SESSION_ENGINE.isCollectionActive()) {
                TrackingRuntimeTrace.itemGainDecision(
                        "BLOCK_BREAK_EVIDENCE",
                        material.id(),
                        count,
                        "break",
                        "REJECTED",
                        TrackingRuntimeTrace.Reason.REJECTED_COLLECTION_INACTIVE.name());
            }
            SESSION_ENGINE.onConfirmedMaterialBreak(
                    material,
                    count,
                    now);
            return;
        }
        if (selectedTarget().isCombined()) {
            persistCombinedActiveTime(now);
            CONFIG.combinedLastBreakEpochMillis = now;
        }
        persistActiveTime(material, now);
        MaterialTrackerState state = CONFIG.state(material);
        state.totalBlocks = safeAdd(state.totalBlocks, count);
        state.sessionBlocks = safeAdd(state.sessionBlocks, count);
        long safeBaseDrops = Math.max(0, baseDrops);
        state.totalBaseDrops = safeAdd(
                state.totalBaseDrops, safeBaseDrops);
        state.sessionBaseDrops = safeAdd(
                state.sessionBaseDrops, safeBaseDrops);
        state.lastBreakEpochMillis = now;
        if (SESSION_ENGINE.isObservationEnabled()) {
            SESSION_ENGINE.onAcceptedTargetMaterialBlock(
                    material,
                    count,
                    state.sessionBlocks,
                    now,
                    "target-material-block:"
                            + material.id()
                            + ":" + now
                            + ":" + state.sessionBlocks,
                    "confirmed-break");
        }
    }

    public static void onInventoryRawGained(
            TrackedMaterial material, long rawItems) {
        if (!CONFIG.enabled || rawItems <= 0
                || !isExpecting(material, System.currentTimeMillis())) {
            return;
        }
        MaterialTrackerState state = CONFIG.state(material);
        state.sessionInventoryRaw += rawItems;
        state.totalInventoryRaw += rawItems;
        recomputeActualResource(
                material,
                material.rawItemName() + " packets",
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE);
    }

    /**
     * Routes an exact raw inventory increase for a non-target material
     * (e.g. Hard Stone, Cobblestone while another material or a gemstone is
     * selected) into the shared shadow ledger for OTHER_MINED correlation.
     * Independent of {@code CONFIG.enabled}: OTHER_MINED collection follows
     * Current Session activity, not the live target tracker toggle.
     */
    public static void onOtherMaterialInventoryGained(
            TrackedMaterial material, long rawDelta, long now) {
        if (material == null || rawDelta <= 0) {
            return;
        }
        SkyBlockItemIdentityResolver.Resolved resolved =
                SkyBlockItemIdentityResolver.fromMaterialHint(
                        material.id(),
                        material.rawItemName(),
                        "",
                        material.rawBazaarId());
        if (!SESSION_ENGINE.isCollectionActive()) {
            TrackingRuntimeTrace.observedItemGain(
                    new TrackingRuntimeTrace.ObservedGain(
                            "INVENTORY_ITEM_GAIN",
                            TrackingRuntimeTrace.ObservationPath.INVENTORY_DELTA,
                            material.rawItemName(),
                            material.id(),
                            "",
                            "",
                            rawDelta,
                            "MINING",
                            "REJECTED",
                            "REJECTED",
                            TrackingRuntimeTrace.Reason.REJECTED_COLLECTION_INACTIVE
                                    .name(),
                            "",
                            "inventory",
                            resolved));
            return;
        }
        MiningSessionShadowObserver.ObservationResult result =
                SESSION_ENGINE.onMaterialInventoryGain(material, rawDelta, now);
        if (result.appended()) {
            creditCanonicalOtherFromMaterial(material, rawDelta, now, resolved);
            cachedHudOtherSummary = null;
        }
        TrackingRuntimeTrace.observedItemGain(
                new TrackingRuntimeTrace.ObservedGain(
                        "INVENTORY_ITEM_GAIN",
                        TrackingRuntimeTrace.ObservationPath.INVENTORY_DELTA,
                        material.rawItemName(),
                        material.id(),
                        "",
                        "",
                        rawDelta,
                        "MINING",
                        result.appended() ? "OTHER" : "REJECTED",
                        result.appended() ? "ACCEPTED" : "REJECTED",
                        result.appended()
                                ? TrackingRuntimeTrace.Reason.ACCEPTED_OTHER_MINING
                                .name()
                                : reasonFromObservation(result),
                        duplicateOf(result),
                        "inventory",
                        resolved));
    }

    private static void observeMobLootOverlay(Component message) {
        if (message == null || !mobLootCollectionAllowed()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (observeMobLootChat(message.getString(), now)) {
            cachedHudOtherSummaryAtMillis = 0L;
        }
    }

    private static boolean observeMobLootChat(String plain, long now) {
        if (plain == null || plain.isBlank()) {
            return false;
        }
        Optional<Long> coins = MobLootCoinParser.parse(plain);
        if (coins.isPresent()
                && MOB_LOOT_DETECTOR.offerCoinGain(coins.get(), now)) {
            return true;
        }
        Optional<String> rareDrop = MobLootRareDropParser.parse(plain);
        MagicFindParser.parse(plain).ifPresent(magicFind -> {
            MAGIC_FIND_DETECTOR.inspectMessage(plain, now);
            CURRENT_SESSION.noteMagicFind(magicFind, now);
        });
        if (rareDrop.isPresent()
                && MOB_LOOT_DETECTOR.offerNamedItem(
                        SACK_RESOURCE_CATALOG,
                        selectedSelection(),
                        rareDrop.get(),
                        1L,
                        now,
                        "rare-drop")) {
            return true;
        }
        return MOB_LOOT_DETECTOR.offerActionBarGain(
                SACK_RESOURCE_CATALOG,
                selectedSelection(),
                plain,
                now);
    }

    /**
     * Action-bar / overlay quantity lines (e.g. {@code +128 Hard Stone}).
     * Hypixel often delivers sacked mining gains here without a temporary
     * inventory delta and without a {@code [Sacks]} chat envelope.
     */
    private static void observeActionBarMessage(Component message) {
        if (message == null) {
            return;
        }
        ActionBarGainParser.parse(message.getString()).ifPresent(gain -> {
            SkyBlockItemIdentityResolver.Resolved resolved =
                    gain.material() == null
                            ? SkyBlockItemIdentityResolver.fromTextToken(
                            gain.itemName())
                            : SkyBlockItemIdentityResolver.fromMaterialHint(
                            gain.material().id(),
                            gain.itemName(),
                            "",
                            gain.material().rawBazaarId());
            if (gain.material() == null) {
                TrackingRuntimeTrace.observedItemGain(
                        new TrackingRuntimeTrace.ObservedGain(
                                "ACTIONBAR_ITEM_GAIN",
                                TrackingRuntimeTrace.ObservationPath.ACTIONBAR,
                                gain.itemName(),
                                gain.itemName(),
                                "",
                                "",
                                gain.quantity(),
                                "MINING",
                                "REJECTED",
                                "REJECTED",
                                TrackingRuntimeTrace.Reason.REJECTED_NO_STABLE_ID
                                        .name(),
                                "",
                                "actionbar",
                                resolved));
                return;
            }
            if (tracksMaterial(gain.material())) {
                TrackingRuntimeTrace.observedItemGain(
                        new TrackingRuntimeTrace.ObservedGain(
                                "ACTIONBAR_ITEM_GAIN",
                                TrackingRuntimeTrace.ObservationPath.ACTIONBAR,
                                gain.itemName(),
                                gain.itemName(),
                                "",
                                "",
                                gain.quantity(),
                                "MINING",
                                "TARGET",
                                "IGNORED",
                                TrackingRuntimeTrace.Reason.REJECTED_TARGET_AUTHORITY
                                        .name(),
                                "",
                                "actionbar",
                                resolved));
                return;
            }
            long now = System.currentTimeMillis();
            if (!SESSION_ENGINE.isCollectionActive()) {
                TrackingRuntimeTrace.observedItemGain(
                        new TrackingRuntimeTrace.ObservedGain(
                                "ACTIONBAR_ITEM_GAIN",
                                TrackingRuntimeTrace.ObservationPath.ACTIONBAR,
                                gain.itemName(),
                                gain.itemName(),
                                "",
                                "",
                                gain.quantity(),
                                "MINING",
                                "REJECTED",
                                "REJECTED",
                                TrackingRuntimeTrace.Reason
                                        .REJECTED_COLLECTION_INACTIVE.name(),
                                "",
                                "actionbar",
                                resolved));
                return;
            }
            MiningSessionShadowObserver.ObservationResult result =
                    SESSION_ENGINE.onMaterialInventoryGain(
                            gain.material(),
                            gain.quantity(),
                            now);
            if (result.appended()) {
                creditCanonicalOtherFromMaterial(
                        gain.material(),
                        gain.quantity(),
                        now,
                        resolved);
                cachedHudOtherSummary = null;
            }
            TrackingRuntimeTrace.observedItemGain(
                    new TrackingRuntimeTrace.ObservedGain(
                            "ACTIONBAR_ITEM_GAIN",
                            TrackingRuntimeTrace.ObservationPath.ACTIONBAR,
                            gain.itemName(),
                            gain.itemName(),
                            "",
                            "",
                            gain.quantity(),
                            "MINING",
                            result.appended() ? "OTHER" : "REJECTED",
                            result.appended() ? "ACCEPTED" : "REJECTED",
                            result.appended()
                                    ? TrackingRuntimeTrace.Reason
                                    .ACCEPTED_OTHER_MINING.name()
                                    : reasonFromObservation(result),
                            duplicateOf(result),
                            "actionbar",
                            resolved));
        });
    }

    private static void creditCanonicalOtherFromMaterial(
            TrackedMaterial material,
            long quantity,
            long now,
            SkyBlockItemIdentityResolver.Resolved resolved) {
        String itemId = resolved != null && resolved.catalogMatch()
                ? resolved.stableId()
                : CurrentSessionCanonicalIds.canonicalItemId(
                material.id(), material.rawItemName());
        String display = resolved != null && !resolved.displayName().isBlank()
                ? resolved.displayName()
                : material.rawItemName();
        CURRENT_SESSION.creditItem(
                itemId,
                display,
                quantity,
                SessionSourceType.MINING,
                MiningClassification.OTHER,
                SkyBlockAreaDetector.detect(),
                RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                0.0,
                now);
        TrackingRuntimeTrace.currentSessionIngest(
                itemId,
                quantity,
                SessionSourceType.MINING.name(),
                MiningClassification.OTHER.name(),
                "ACCEPTED");
    }

    static boolean isCollectionActiveForTrace() {
        return SESSION_ENGINE.isCollectionActive();
    }

    private static String duplicateOf(
            MiningSessionShadowObserver.ObservationResult result) {
        if (result == null || result.reason() == null) {
            return "";
        }
        if (result.reason()
                != MiningSessionClassification.ReasonCode.DUPLICATE_DELIVERY) {
            return "";
        }
        String contextId = result.contextId();
        return contextId == null || contextId.isBlank() ? "prior-delivery" : contextId;
    }

    private static String reasonFromObservation(
            MiningSessionShadowObserver.ObservationResult result) {
        if (result == null || result.reason() == null) {
            return TrackingRuntimeTrace.Reason.REJECTED_UNKNOWN_SOURCE.name();
        }
        return switch (result.reason()) {
            case MISSING_MINING_CORRELATION ->
                    TrackingRuntimeTrace.Reason.REJECTED_NO_MINING_EVIDENCE.name();
            case DUPLICATE_DELIVERY ->
                    TrackingRuntimeTrace.Reason.REJECTED_DUPLICATE.name();
            case TARGET_EXCLUDED_FROM_OTHERS ->
                    TrackingRuntimeTrace.Reason.REJECTED_TARGET_AUTHORITY.name();
            case UNKNOWN_RESOURCE ->
                    TrackingRuntimeTrace.Reason.REJECTED_NO_STABLE_ID.name();
            default -> result.reason().name();
        };
    }

    public static void onCompactBonusObserved(
            TrackedMaterial material, long enchantedItems) {
        if (!CONFIG.enabled || enchantedItems <= 0
                || !isExpecting(material, System.currentTimeMillis())) {
            return;
        }
        MaterialTrackerState state = CONFIG.state(material);
        state.sessionCompactBonusEnchanted += enchantedItems;
        state.totalCompactBonusEnchanted += enchantedItems;
        recomputeActualResource(
                material,
                "Compact bonus",
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE);
    }

    public static void onSackObserved(
            TrackedMaterial material, long rawItems, long enchantedItems) {
        if (!CONFIG.enabled
                || !tracksMaterial(material)
                || (rawItems <= 0 && enchantedItems <= 0)) {
            return;
        }
        MaterialTrackerState state = CONFIG.state(material);
        state.sessionSackRaw += Math.max(0, rawItems);
        state.totalSackRaw += Math.max(0, rawItems);
        state.sessionSackEnchanted += Math.max(0, enchantedItems);
        state.totalSackEnchanted += Math.max(0, enchantedItems);
        DiagnosticRecorder.record("SACK_RESOURCE",
                "material=" + material.id()
                        + " raw=" + rawItems
                        + " enchanted=" + enchantedItems
                        + " sessionRaw=" + state.sessionSackRaw
                        + " sessionEnchanted=" + state.sessionSackEnchanted);
        recomputeActualResource(
                material,
                "inventory + Mining Sacks",
                MiningSessionObservation.EvidenceType.SACK_CHANGE);
    }

    public static void onSystemMessagePacket(Component message) {
        if (!onClientThread()) return;

        ClientBoundaryGuard.run("SYSTEM_CHAT", () -> {
            TrackingRuntimeTrace.callbackAlive("SystemChat");
            GEMSTONE_DIAGNOSTIC_OBSERVER.inspectMessage(message);

            AutoConversationRuntime.onChat(message);

            if (CONFIG.enabled && isMaterialSelection()) {
                for (TrackedMaterial material : selectedMaterials()) {
                    BREAK_DETECTORS.get(material).inspectMessage(message);
                    GAIN_DETECTORS.get(material).inspectMessage(message);
                }
            }

            observeShadowSystemMessage(message);
        });
    }

    private static void observeShadowSystemMessage(Component message) {
        if (message == null) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean collectionActive = SESSION_ENGINE.isCollectionActive();
        boolean observationEnabled = SESSION_ENGINE.isObservationEnabled();
        boolean shadowCollecting = collectionActive;

        if (observationEnabled || shadowCollecting) {
            Optional<PristineComponentIngress.Parsed> pristine =
                    PristineComponentIngress.parse(message);
            if (pristine.isPresent()) {
                PristineComponentIngress.Parsed parsed = pristine.get();
                PristineItemGainPipeline.Outcome outcome =
                        PristineItemGainPipeline.submit(
                                SESSION_ENGINE,
                                CURRENT_SESSION,
                                parsed.reward(),
                                SkyBlockAreaDetector.detect(),
                                now,
                                parsed.deliveryIdentity());
                if (outcome.creditedOthers()) {
                    cachedHudOtherSummary = null;
                }
                return;
            }
        }

        if (collectionActive && CONFIG.powderChestTrackerEnabled) {
            MiningSessionChestObserver.FinalizationResult chestResult =
                    SESSION_ENGINE.observePowderChestChatLine(
                    message.getString(),
                    now);
            if (chestResult.accepted()) {
                List<RotClientCurrentSession.PowderChestCredit> credits =
                        new ArrayList<>(chestResult.canonicalCredits().size());
                for (MiningSessionChestObserver.CanonicalCredit credit
                        : chestResult.canonicalCredits()) {
                    credits.add(new RotClientCurrentSession.PowderChestCredit(
                            credit.itemId(),
                            credit.displayName(),
                            credit.quantity(),
                            credit.sourceType()));
                }
                CURRENT_SESSION.creditPowderChestBatch(
                        credits,
                        SkyBlockAreaDetector.detect(),
                        now);
            }
        }

        if (mobLootCollectionAllowed()) {
            if (observeMobLootChat(message.getString(), now)) {
                cachedHudOtherSummaryAtMillis = 0L;
                return;
            }
        }

        if (observationEnabled || shadowCollecting) {
            Optional<SackComponentIngress.Batch> parsedBatch =
                    SackComponentIngress.parse(message);
            if (parsedBatch.isEmpty()) {
                return;
            }

            SackComponentIngress.Batch batch = parsedBatch.get();
            long coveredBatchMillis = batch.coveredBatchMillis();
            TrackingRuntimeTrace.event(
                    "SACK_MESSAGE",
                    Map.of(
                            "prefix", "[Sacks]",
                            "collection",
                            collectionActive ? "ACTIVE" : "OFF",
                            "target", selectedSelection().id(),
                            "covered_ms", Long.toString(coveredBatchMillis)));

            for (SackComponentIngress.ParsedHover parsedHover
                    : batch.parsedHovers()) {
                SackChangeParser.ParseResult parsed = parsedHover.parseResult();
                for (SackChangeParser.TokenTrace token : parsed.tokenTraces()) {
                    TrackingRuntimeTrace.event(
                            token.parsed() ? "SACK_TOKEN" : "SACK_UNPARSED_TOKEN",
                            Map.of(
                                    "token", token.safeLabel(),
                                    "qty", Long.toString(token.quantity()),
                                    "parsed", Boolean.toString(token.parsed())));
                }
                List<SackChangeParser.Change> changes = parsed.changes();
                TrackingRuntimeTrace.event(
                        "SACK_PARSED",
                        Map.of(
                                "path",
                                TrackingRuntimeTrace.ObservationPath.SACK_MESSAGE
                                        .name(),
                                "changes", Integer.toString(changes.size()),
                                "collection",
                                collectionActive ? "ACTIVE" : "OFF",
                                "area", SkyBlockAreaDetector.detect().id(),
                                "covered_ms", Long.toString(coveredBatchMillis)));
                // Chest research only — OTHER_MINED handoff is per-item below.
                SESSION_ENGINE.noteSackChangesForResearch(changes, now);
                for (SackChangeParser.Change change : changes) {
                    submitParsedSackItemGain(
                            change,
                            now,
                            collectionActive,
                            coveredBatchMillis,
                            batch.deliveryIdentity());
                }
            }
        }
    }

    /**
     * Canonical Sack handoff: parse observation → domain classification →
     * Current Session ingest. Trace events subscribe to the outcome; they are
     * not the terminal consumer.
     */
    private static void submitParsedSackItemGain(
            SackChangeParser.Change change,
            long now,
            boolean collectionActive,
            long coveredBatchMillis,
            String deliveryIdentity) {
        if (change.delta() <= 0L) {
            return;
        }
        if (mobLootCollectionAllowed()
                && MOB_LOOT_DETECTOR.offerSackGain(
                        SACK_RESOURCE_CATALOG,
                        selectedSelection(),
                        change,
                        now)) {
            cachedHudOtherSummaryAtMillis = 0L;
            return;
        }
        SackItemGainPipeline.Outcome outcome = SackItemGainPipeline.submit(
                new SackItemGainPipeline.Context(
                        SESSION_ENGINE,
                        CURRENT_SESSION,
                        SACK_RESOURCE_CATALOG,
                        RotClientClient::selectedSelection,
                        SkyBlockAreaDetector::detect,
                        () -> collectionActive,
                        () -> now),
                change,
                coveredBatchMillis,
                deliveryIdentity);
        if (outcome.creditedOthers()) {
            cachedHudOtherSummary = null;
        }
    }

    private static List<String> sackChangeHoverTexts(Component component) {
        return SackHoverExtractor.changeHoverTexts(component);
    }

    private static void collectSackChangeHoverTexts(
            Component component,
            List<String> texts) {
        for (String text : SackHoverExtractor.changeHoverTexts(component)) {
            if (!texts.contains(text)) {
                texts.add(text);
            }
        }
    }

    public static void onServerBlockUpdate(
            net.minecraft.core.BlockPos pos,
            net.minecraft.world.level.block.state.BlockState newState) {
        Minecraft client = Minecraft.getInstance();
        if (!ClientThreadGuard.shouldHandle(
                client.isSameThread())) {
            return;
        }

        ClientBoundaryGuard.run("BLOCK_UPDATE", () -> {
            GEMSTONE_GAIN_DETECTOR.onServerBlockUpdate(
                    client,
                    pos,
                    newState,
                    isGemstoneTrackingActive()
                            || SESSION_ENGINE.isCollectionActive());
            SlayerRuntime.onBlockUpdate(pos, newState);
            net.minecraft.world.level.block.state.BlockState oldState =
                    client.level == null ? null : client.level.getBlockState(pos);
            DungeonRuntime.onBlockUpdate(pos, oldState, newState);

            if (CONFIG.enabled && isMaterialSelection()) {
                for (TrackedMaterial material : selectedMaterials()) {
                    BREAK_DETECTORS.get(material)
                            .onServerBlockUpdate(client, pos, newState, true);
                }
            }
            // OTHER / non-target breaks follow Current Session collection,
            // not DiagnosticRecorder and not live-tracker auto-pause.
            if (SESSION_ENGINE.isCollectionActive()) {
                for (TrackedMaterial material : TrackedMaterial.values()) {
                    if (SESSION_ENGINE.supportsMaterialBreak(material)
                            && !tracksMaterial(material)) {
                        BREAK_DETECTORS.get(material)
                                .onServerBlockUpdate(
                                        client, pos, newState, false);
                    }
                }
            }
        });
    }

    public static void onBazaarTaxDetected(double percent) {
        if (Math.abs(CONFIG.bazaarTaxPercent - percent) < 0.0001) return;
        CONFIG.bazaarTaxPercent = percent;
        DiagnosticRecorder.record("BAZAAR_TAX", "percent=" + percent);
        TrackerStore.save(CONFIG);
    }

    public static void onBazaarSold(
            TrackedMaterial material,
            long amount,
            boolean enchanted,
            double grossCoins) {
        if (!CONFIG.enabled
                || !tracksMaterial(material)
                || amount <= 0
                || grossCoins <= 0) {
            return;
        }
        if (!SALE_GATES.get(material).shouldCredit(
                enchanted, amount, grossCoins, System.currentTimeMillis())) {
            return;
        }

        long available = enchanted
                ? currentSessionUnsoldEnchantedItems(material)
                : currentSessionUnsoldRawItems(material);
        SessionAccounting.SaleCredit credit =
                SessionAccounting.creditSale(available, amount, grossCoins);
        if (credit.amount() <= 0) return;

        MaterialTrackerState state = CONFIG.state(material);
        if (enchanted) {
            state.sessionSoldEnchanted += credit.amount();
        } else {
            state.sessionSoldRaw += credit.amount();
        }
        state.sessionRealizedGrossCoins += credit.grossCoins();
        DiagnosticRecorder.record("BAZAAR_RESOURCE_SOLD",
                "material=" + material.id()
                        + " item=" + (enchanted ? "enchanted" : "raw")
                        + " sold=" + amount
                        + " sessionAmount=" + credit.amount()
                        + " gross=" + grossCoins
                        + " sessionGross=" + state.sessionRealizedGrossCoins);
    }

    public static void onInventoryPacket(String packetType) {
        Minecraft client = Minecraft.getInstance();

        if (!ClientThreadGuard.shouldHandle(
                client.isSameThread())) {
            return;
        }

        ClientBoundaryGuard.run("INVENTORY_PACKET", () -> {
            GEMSTONE_DIAGNOSTIC_OBSERVER.onInventoryPacket(
                    client,
                    packetType);

            if (isMaterialSelection()) {
                for (MiningGainDetector detector : GAIN_DETECTORS.values()) {
                    detector.onInventoryPacket(client, packetType);
                }
            }

            GEMSTONE_GAIN_DETECTOR.onInventoryPacket(client, packetType);
            MOB_LOOT_DETECTOR.observe(
                    client,
                    mobLootCollectionAllowed(),
                    packetType);
        });
    }

    public static void onPlayerAttack(net.minecraft.world.entity.Entity target) {
        if (!mobLootCollectionAllowed() || target == null) return;
        ClientBoundaryGuard.run(
                "MOB_LOOT_ATTACK",
                () -> MOB_LOOT_DETECTOR.onPlayerAttack(
                        target,
                        System.currentTimeMillis()));
    }

    public static boolean isLocalPlayerCombatCause(
            int causeId,
            int directId,
            net.minecraft.world.entity.Entity causing,
            net.minecraft.world.entity.Entity direct) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        if (MobLootPlayerCause.matchesIds(
                client.player.getId(), causeId, directId)) {
            return true;
        }
        if (causing == client.player || direct == client.player) {
            return true;
        }
        if (isPlayerProjectile(causing) || isPlayerProjectile(direct)) {
            return true;
        }
        return recentlyFiredLocalProjectile() && isHoldingRangedWeapon();
    }

    public static void onSpawnedCombatProjectile(
            net.minecraft.world.entity.Entity entity,
            int ownerEntityId) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || entity == null) {
            return;
        }
        if (ownerEntityId == client.player.getId()
                || isPlayerProjectile(entity)) {
            lastLocalRangedCombatMillis = System.currentTimeMillis();
        }
    }

    private static void noteLocalRangedUse(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        if (!client.player.isUsingItem()) {
            return;
        }
        net.minecraft.world.item.ItemStack stack = client.player.getUseItem();
        if (isRangedWeapon(stack)) {
            lastLocalRangedCombatMillis = System.currentTimeMillis();
        }
    }

    private static boolean recentlyFiredLocalProjectile() {
        return lastLocalRangedCombatMillis > 0L
                && System.currentTimeMillis() - lastLocalRangedCombatMillis
                <= RANGED_COMBAT_CONTEXT_MILLIS;
    }

    private static boolean isHoldingRangedWeapon() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return false;
        }
        return isRangedWeapon(client.player.getMainHandItem())
                || isRangedWeapon(client.player.getOffhandItem())
                || isRangedWeapon(client.player.getUseItem());
    }

    private static boolean isRangedWeapon(net.minecraft.world.item.ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem()
                instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            return true;
        }
        net.minecraft.world.item.ItemUseAnimation animation =
                stack.getUseAnimation();
        return animation == net.minecraft.world.item.ItemUseAnimation.BOW
                || animation == net.minecraft.world.item.ItemUseAnimation.CROSSBOW;
    }

    private static boolean isPlayerProjectile(
            net.minecraft.world.entity.Entity entity) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null
                && entity instanceof net.minecraft.world.entity.projectile.Projectile projectile
                && projectile.getOwner() == client.player;
    }

    public static void onEntityEvent(
            net.minecraft.world.entity.Entity entity,
            byte eventId) {
        ClientBoundaryGuard.run(
                "SLAYER_ENTITY_EVENT",
                () -> SlayerRuntime.onEntityEvent(entity, eventId));
        if (!mobLootCollectionAllowed() || entity == null) return;
        ClientBoundaryGuard.run(
                "MOB_LOOT_DEATH",
                () -> MOB_LOOT_DETECTOR.onEntityDeath(
                        entity,
                        eventId,
                        System.currentTimeMillis()));
    }

    private static boolean mobLootCollectionAllowed() {
        return MobLootIngestionReadiness.LIVE_INGESTION_ENABLED
                && CURRENT_SESSION.isActive()
                && isConnectedToHypixel();
    }

    private static void creditMobLoot(
            String itemId,
            String displayName,
            long quantity,
            long nowMillis) {
        if (!mobLootCollectionAllowed() || quantity <= 0L) return;
        boolean coins = "COINS".equals(SkyBlockItemId.normalize(itemId));
        CURRENT_SESSION.creditItem(
                itemId,
                displayName,
                quantity,
                SessionSourceType.MOB,
                null,
                SkyBlockAreaDetector.detect(),
                coins
                        ? RotClientCurrentSessionConfig.PriceStatus
                        .RESOLVED_BAZAAR
                        : RotClientCurrentSessionConfig.PriceStatus.UNAVAILABLE,
                coins ? (double) quantity : 0.0,
                nowMillis);
        int magicFind = PLAYER_STATE.latest().magicFind();
        if (magicFind >= 0) {
            CURRENT_SESSION.noteMagicFind(magicFind, nowMillis);
        }
        cachedHudOtherSummaryAtMillis = 0L;
    }

    public static void onRawConsumedByCompactor(
            TrackedMaterial material, long rawItems) {
        if (!CONFIG.enabled || rawItems <= 0
                || !isExpecting(material, System.currentTimeMillis())) {
            return;
        }
        MaterialTrackerState state = CONFIG.state(material);
        state.compactorRawRemainder += rawItems;
        long crafted =
                state.compactorRawRemainder / material.rawPerEnchanted();
        state.compactorRawRemainder %= material.rawPerEnchanted();
        if (crafted <= 0) return;

        state.sessionCompactedEnchanted += crafted;
        state.totalCompactedEnchanted += crafted;
        DiagnosticRecorder.record("RESOURCE_COMPACTED",
                "material=" + material.id()
                        + " enchanted=" + crafted
                        + " sessionCompacted="
                        + state.sessionCompactedEnchanted);
        recomputeActualResource(
                material,
                "inventory + compactor",
                MiningSessionObservation.EvidenceType.INVENTORY_CHANGE);
    }

    private static int openHome(FabricClientCommandSource source) {
        return openMiningUi(source);
    }

    private static int openQolUi(FabricClientCommandSource source) {
        Minecraft.getInstance().schedule(() -> {
            WORKSPACE.flushIfDirty();
            WORKSPACE.setQolView("COMBAT", "");
            openClientUiNavigating(DashboardModule.QOL_SETTINGS, null);
        });
        return 1;
    }

    private static int openMiningUi(FabricClientCommandSource source) {
        Minecraft.getInstance().schedule(() -> {
            WORKSPACE.flushIfDirty();
            WORKSPACE.resetToDefaultOpenState();
            Minecraft.getInstance().gui.setScreen(
                    new MiningUiScreen(CONFIG, HUD, null, null, true));
        });
        return 1;
    }

    static void openClientUiNavigating(DashboardModule module, Screen parent) {
        WORKSPACE.flushIfDirty();
        WORKSPACE.resetToDefaultOpenState();
        if (module != null && module != DashboardModule.NONE) {
            WORKSPACE.navigateActive(
                    RotClientWorkspaceRoute.fromDashboardModule(module));
        }
        Minecraft.getInstance().gui.setScreen(
                new MiningUiScreen(CONFIG, HUD, parent, module, true));
    }

    /** Opens the unified Mining, Powder Chest, QoL and Client UI editor. */
    static void openHudEditor(Screen parent) {
        openHudEditor(parent, "");
    }

    static void openHudEditor(Screen parent, String focusId) {
        Minecraft.getInstance().gui.setScreen(new RotClientScreen(HUD, parent, focusId));
    }

    /** Opens the existing Rot Client home / mining UI (Click GUI target). */
    static void openClickGui() {
        WORKSPACE.flushIfDirty();
        WORKSPACE.resetToDefaultOpenState();
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return;
        }
        maybeSendDeveloperMessage(client);
        client.gui.setScreen(new MiningUiScreen(CONFIG, HUD, null, null, true));
    }

    static void closeClickGuiIfOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return;
        }
        Screen screen = client.gui.screen();
        if (screen instanceof MiningUiScreen || screen instanceof RotClientHomeScreen) {
            client.gui.setScreen(null);
        }
    }

    static boolean isRotClientUiOpen() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return false;
        }
        Screen screen = client.gui.screen();
        return screen instanceof MiningUiScreen
                || screen instanceof RotClientHomeScreen
                || screen instanceof RotClientScreen;
    }

    public static boolean wantsMonitorRefreshUi() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null) {
            return false;
        }
        Screen screen = client.gui.screen();
        return screen instanceof MiningUiScreen
                || screen instanceof RotClientHomeScreen
                || screen instanceof RotClientScreen
                || screen instanceof RotClientAppearanceScreen
                || screen instanceof RotClientColorPickerScreen;
    }

    public static boolean isNoCursorResetEnabled() {
        return qolConfig().noCursorResetEnabled;
    }

    private static void restoreStorageCursor(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            return;
        }
        NoCursorResetController controller = noCursorReset();
        if (!controller.consumeRestore(System.currentTimeMillis())) {
            return;
        }
        GLFW.glfwSetCursorPos(
                client.getWindow().handle(),
                controller.savedX(),
                controller.savedY());
        if (client.mouseHandler instanceof fi.rotclient.mixin.MouseHandlerCursorAccessor access) {
            access.rotclient$setXpos(controller.savedX());
            access.rotclient$setYpos(controller.savedY());
        }
    }

    public static int noCursorUnhookTimeoutMs() {
        return NoCursorResetPolicy.clampTimeoutMs(
                qolConfig().noCursorUnhookTimeoutMs);
    }

    public static PlayerDisplayHidePolicy.ActionHides actionBarHides() {
        return PlayerDisplayHidePolicy.from(qolConfig());
    }

    public static boolean shouldFilterActionBar() {
        return actionBarHides().any();
    }

    public static boolean hideActionHealth() {
        return actionBarHides().health();
    }

    public static boolean hideActionDefense() {
        return actionBarHides().defense();
    }

    public static boolean hideActionMana() {
        return actionBarHides().mana();
    }

    public static boolean hideActionOverflow() {
        return actionBarHides().overflow();
    }

    public static boolean hideActionSpeed() {
        return actionBarHides().speed();
    }

    public static boolean hideActionVitality() {
        return actionBarHides().vitality();
    }

    public static boolean hideActionLocation() {
        return actionBarHides().location();
    }

    static boolean clickGuiChatNotificationsEnabled() {
        return qolConfig().clickGuiChatNotifications;
    }

    static int clickGuiAccentColor() {
        return qolConfig().clickGuiColor;
    }

    static boolean clickGuiRoundedBottoms() {
        return qolConfig().clickGuiRoundedBottoms;
    }

    private static void registerVanillaHudHides() {
        wrapHudLayer(VanillaHudElements.HEALTH_BAR, HudLayerHidePolicy.Layer.HEALTH);
        wrapHudLayer(VanillaHudElements.FOOD_BAR, HudLayerHidePolicy.Layer.FOOD);
        wrapHudLayer(VanillaHudElements.ARMOR_BAR, HudLayerHidePolicy.Layer.ARMOR);
        wrapHudLayer(VanillaHudElements.EXPERIENCE_LEVEL, HudLayerHidePolicy.Layer.XP);
        wrapHudLayer(VanillaHudElements.HOTBAR, HudLayerHidePolicy.Layer.HOTBAR);
        wrapHudLayer(VanillaHudElements.SCOREBOARD, HudLayerHidePolicy.Layer.SCOREBOARD);
        wrapHudLayer(VanillaHudElements.OVERLAY_MESSAGE, HudLayerHidePolicy.Layer.ACTION);
        wrapHudLayer(VanillaHudElements.BOSS_BAR, HudLayerHidePolicy.Layer.BOSS);
        wrapHudLayer(VanillaHudElements.MOB_EFFECTS, HudLayerHidePolicy.Layer.EFFECTS);
        wrapHudLayer(VanillaHudElements.HELD_ITEM_TOOLTIP, HudLayerHidePolicy.Layer.ITEM_NAME);
        wrapHudLayer(VanillaHudElements.AIR_BAR, HudLayerHidePolicy.Layer.AIR);
        wrapHudLayer(VanillaHudElements.MOUNT_HEALTH, HudLayerHidePolicy.Layer.MOUNT);
        wrapHudLayer(VanillaHudElements.TITLE_AND_SUBTITLE, HudLayerHidePolicy.Layer.TITLES);
        wrapHudLayer(VanillaHudElements.PLAYER_LIST, HudLayerHidePolicy.Layer.TAB);
        wrapHudLayer(VanillaHudElements.INFO_BAR, HudLayerHidePolicy.Layer.XP);
    }

    private static void wrapHudLayer(
            Identifier elementId,
            HudLayerHidePolicy.Layer layer) {
        HudElementRegistry.replaceElement(elementId, original ->
                (graphics, delta) -> {
                    if (pauseMenuHidesHud()) {
                        return;
                    }
                    if (HudLayerHidePolicy.shouldHide(layer, HudLayerHidePolicy.flags(qolConfig()))) {
                        return;
                    }
                    original.extractRenderState(graphics, delta);
                });
    }

    /** Pause menu keeps the dimmed world but must not keep HUD overlays. */
    public static boolean pauseMenuHidesHud() {
        Minecraft client = Minecraft.getInstance();
        return client != null
                && client.gui != null
                && client.gui.screen() instanceof PauseScreen pause
                && pause.showsPauseMenu();
    }

    private static QolUtilityConfig qolConfig() {
        if (CONFIG.qolUtilities == null) {
            CONFIG.qolUtilities = new QolUtilityConfig();
        }
        return CONFIG.qolUtilities;
    }

    /** Package/mixin bridge for Batch 2 visual controllers. */
    public static QolUtilityConfig qolConfigPublic() {
        return qolConfig();
    }

    /** Action-bar fragments for Player Display. */
    public static void observePlayerDisplayText(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        QOL_HUD.statsTracker().observeActionBar(raw);
    }

    /** Tab list and scoreboard fragments for Player Display. */
    public static void observeHudSourceStats(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        QOL_HUD.statsTracker().observeHudSources(raw);
    }

    private static void tickClickGuiKey(Minecraft client) {
        if (client == null || client.getWindow() == null) {
            clickGuiKeyWasDown = false;
            return;
        }
        QolUtilityConfig qol = qolConfig();
        int glfwKey = QolKeybindNames.resolveGlfwKey(
                qol.clickGuiKeybind, "RIGHT_SHIFT");
        boolean down = QolKeybindNames.isKeyDown(
                client.getWindow().handle(), glfwKey);
        boolean consuming = isTextInputConsuming(client);
        boolean open = isRotClientUiOpen();
        if (ClickGuiKeyPolicy.shouldOpen(
                qol.clickGuiEnabled, down, clickGuiKeyWasDown, consuming, open)) {
            openClickGui();
        } else if (ClickGuiKeyPolicy.shouldClose(
                qol.clickGuiEnabled, down, clickGuiKeyWasDown, consuming, open)) {
            closeClickGuiIfOpen();
        }
        clickGuiKeyWasDown = down;
    }

    private static boolean isTextInputConsuming(Minecraft client) {
        Screen screen = client.gui == null ? null : client.gui.screen();
        if (screen == null) {
            return false;
        }
        // Chat / anvil / sign / any focused editable field: skip Click GUI.
        return screen instanceof net.minecraft.client.gui.screens.ChatScreen
                || screen.getClass().getName().contains("SignEdit")
                || screen.getClass().getName().contains("AbstractCommandBlock")
                || screen.getClass().getName().contains("BookEdit");
    }

    private static void maybeSendDeveloperMessage(Minecraft client) {
        QolUtilityConfig qol = qolConfig();
        if (!qol.clickGuiDeveloperMessage || developerMessageShown) {
            return;
        }
        developerMessageShown = true;
        if (client.player == null) {
            return;
        }
        client.player.sendSystemMessage(Component.literal(
                "[Rot Client] Developer message enabled (local only)."));
    }

    static void notifyQolModuleToggled(String moduleLabel, boolean enabled) {
        if (!clickGuiChatNotificationsEnabled()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gui == null || client.gui.hud == null) {
            return;
        }
        // Local HUD only. A system-message send re-enters ALLOW_GAME and
        // crashed the click when a later Slayer class failed to load from the JAR.
        client.gui.hud.getChat().addClientSystemMessage(Component.literal(
                "[Rot Client] "
                        + moduleLabel
                        + (enabled ? " enabled" : " disabled")));
    }

    private static int resetLayout(
            FabricClientCommandSource source,
            boolean resetUi,
            boolean resetHud) {
        if (resetUi) {
            WORKSPACE.resetClientUiPosition();
        }
        if (resetHud) {
            CONFIG.x = 12.0F;
            CONFIG.y = 12.0F;
            CONFIG.scale = 1.0F;
            CONFIG.powderChestHudX = 292.0F;
            CONFIG.powderChestHudY = 12.0F;
            CONFIG.powderChestHudScale = 1.0F;
            CONFIG.normalize();
            HUD.clampToScreen();
            POWDER_CHEST_HUD.clampToScreen();
            CONFIG.qolUtilities.extras().resetHudEditorChrome();
            save();
        }
        WORKSPACE.saveNow();
        if (resetUi && resetHud) {
            source.sendFeedback(Component.literal(
                    "Rot Client layout positions reset (Client UI + tracker HUDs)."));
        } else if (resetUi) {
            source.sendFeedback(Component.literal(
                    "Rot Client dashboard position reset."));
        } else {
            source.sendFeedback(Component.literal(
                    "Rot Client tracker HUD positions reset."));
        }
        return 1;
    }

    /** GUI action: reset positional layout only. */
    static void resetLayoutPositionsFromUi(boolean resetUi, boolean resetHud) {
        if (resetUi) {
            WORKSPACE.resetClientUiPosition();
        }
        if (resetHud) {
            CONFIG.x = 12.0F;
            CONFIG.y = 12.0F;
            CONFIG.scale = 1.0F;
            CONFIG.powderChestHudX = 292.0F;
            CONFIG.powderChestHudY = 12.0F;
            CONFIG.powderChestHudScale = 1.0F;
            CONFIG.normalize();
            HUD.clampToScreen();
            POWDER_CHEST_HUD.clampToScreen();
            CONFIG.qolUtilities.extras().resetHudEditorChrome();
            save();
        }
        WORKSPACE.saveNow();
    }

    private static boolean onClientThread() {
        return ClientThreadGuard.shouldHandle(
                Minecraft.getInstance().isSameThread());
    }

    /**
     * Minecraft 26.2 keeps {@code mouseScrolled} on the GUI listener interface,
     * not on {@link Screen}. Fabric's per-screen scroll gate only steals the
     * wheel from containers; hover-box panning is recorded from MouseHandler.
     */
    private static void registerTooltipAndStorageScroll(Screen screen) {
        ScreenMouseEvents.allowMouseScroll(screen).register(
                (current, mouseX, mouseY, horizontal, vertical) ->
                        ClientBoundaryGuard.call(
                                "CUSTOM_TOOLTIP_SCROLL",
                                () -> allowTooltipAndStorageScroll(
                                        current, horizontal, vertical),
                                true));
    }

    private static boolean allowTooltipAndStorageScroll(
            Screen current, double horizontal, double vertical) {
        ItemStack hovered = ItemStack.EMPTY;
        boolean overSlot = false;
        if (current instanceof AbstractContainerScreen<?> container) {
            Slot slot = ((AbstractContainerScreenAccessor) container)
                    .rotclient$hoveredSlot();
            overSlot = slot != null;
            hovered = slot == null ? ItemStack.EMPTY : slot.getItem();
        }
        boolean overItem = overSlot && hovered != null && !hovered.isEmpty();
        boolean container = current instanceof AbstractContainerScreen<?>;
        boolean overlay = container
                && StorageOverlayRuntime.shouldReplaceVanilla((AbstractContainerScreen<?>) current)
                && StorageOverlayRuntime.lastLayout() != null;
        Minecraft client = Minecraft.getInstance();
        boolean shift = client != null
                && client.getWindow() != null
                && (QolKeybindNames.isKeyDown(client.getWindow().handle(), GLFW.GLFW_KEY_LEFT_SHIFT)
                || QolKeybindNames.isKeyDown(client.getWindow().handle(), GLFW.GLFW_KEY_RIGHT_SHIFT));
        if (!container) {
            CustomTooltipRuntime.clear();
        }
        if (StorageOverlayPolicy.shouldStealOverlayWheel(
                container, overlay, shift, CustomTooltipRuntime.shouldStealWheel(overItem))) {
            return false;
        }
        if (overlay && CustomTooltipPolicy.storageOverlayTakesWheel(true, shift)) {
            return !StorageOverlayRuntime.scroll(vertical, overItem);
        }
        return !(container && StorageOverlayRuntime.scroll(vertical, overItem));
    }

    private static void addPauseMenuButton(
            Minecraft client, Screen screen, int width, int height) {
        Button dashboardButton = Button.builder(
                        Component.translatable("rotclient.menu.dashboard"),
                        button -> client.gui.setScreen(
                                new MiningUiScreen(CONFIG, HUD, screen)))
                .bounds(
                        0,
                        0,
                        PauseMenuButtonLayout.BUTTON_WIDTH,
                        PauseMenuButtonLayout.BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable(
                        "rotclient.menu.dashboard.tooltip")))
                .build();
        Screens.getWidgets(screen).add(dashboardButton);
        positionPauseMenuButton(screen, dashboardButton, width, height);
        ScreenEvents.beforeExtract(screen).register(
                (current, graphics, mouseX, mouseY, tickDelta) ->
                        positionPauseMenuButton(
                                current, dashboardButton, width, height));
    }

    private static void positionPauseMenuButton(
            Screen screen,
            AbstractWidget dashboardButton,
            int width,
            int height) {
        List<PauseMenuButtonLayout.Bounds> occupied = new ArrayList<>();
        for (AbstractWidget widget : Screens.getWidgets(screen)) {
            if (widget == dashboardButton || !widget.visible) continue;
            occupied.add(new PauseMenuButtonLayout.Bounds(
                    widget.getX(),
                    widget.getY(),
                    widget.getWidth(),
                    widget.getHeight()));
        }
        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(width, height, occupied);
        dashboardButton.setX(position.x());
        dashboardButton.setY(position.y());
    }

    private static int openEditor(FabricClientCommandSource source) {
        setTrackerEnabled(true);

        Minecraft.getInstance().schedule(() -> openHudEditor(null));

        source.sendFeedback(Component.literal(
                "Rot Client enabled. Editor opened."));

        return 1;
    }

private static int toggle(FabricClientCommandSource source) {
    if (!CONFIG.enabled && !selectedSelection().supportsLiveTracking()) {
        source.sendFeedback(Component.literal(
                "The selected tracker does not support live tracking. Tracking remains OFF."));
        return 1;
    }

    setTrackerEnabled(!CONFIG.enabled);

    source.sendFeedback(Component.literal(
            "Rot Client: " + (CONFIG.enabled ? "ON" : "OFF")));

    return 1;
}

    private static int resetSession(FabricClientCommandSource source) {
        TrackerSelection selection = selectedSelection();
        resetSessionData();
        source.sendFeedback(Component.literal(
                selection.displayName() + " session reset."));
        return 1;
    }

    private static int setTarget(
            FabricClientCommandSource source, TrackingTarget target) {
        setTrackingTarget(target);
        source.sendFeedback(Component.literal(
                "Rot Client target: " + target.displayName()));
        return 1;
    }
    static void resetSessionData() {
        TrackerSelection selection = selectedSelection();
        if (selection.isGemstone()) {
            CONFIG.gemstoneState(
                            selection.gemstone())
                    .persistActiveTime(
                            System.currentTimeMillis(),
                            PAUSE_AFTER_MILLIS);
            CONFIG.resetSelectedSessionState();
            GEMSTONE_GAIN_DETECTOR.reset();
            HUD.onMaterialChanged();
            TrackerStore.save(CONFIG);
            long shadowNow = System.currentTimeMillis();
            SESSION_ENGINE.onReset(
                    CONFIG.enabled,
                    selection,
                    shadowLiveBaseline(selection, shadowNow),
                    shadowNow);
            return;
        }
        long now =
                System.currentTimeMillis();
        if (selectedTarget().isCombined()) {
            persistCombinedActiveTime(now);
            CONFIG.combinedSessionActiveMillis = 0;
            CONFIG.combinedLastBreakEpochMillis = 0;
        }
        List<TrackedMaterial> materials =
                selectedMaterials();
        for (TrackedMaterial material : materials) {
            persistActiveTime(
                    material,
                    now);
        }
        CONFIG.resetSelectedSessionState();
        for (TrackedMaterial material : materials) {
            resetMaterialSessionRuntime(material);
        }
        HUD.onMaterialChanged();
        TrackerStore.save(CONFIG);
        long shadowNow = System.currentTimeMillis();
        SESSION_ENGINE.onReset(
                CONFIG.enabled,
                selection,
                shadowLiveBaseline(selection, shadowNow),
                shadowNow);
    }

    private static void resetMaterialSessionRuntime(
            TrackedMaterial material) {
        BREAK_DETECTORS.get(material).reset();
        GAIN_DETECTORS.get(material).resetSession(0);
        SALE_GATES.get(material).reset();
    }

    private static void prepareLaunchState() {
        CONFIG.combinedLastBreakEpochMillis = 0;

        for (TrackedMaterial material : TrackedMaterial.values()) {
            MaterialTrackerState state = CONFIG.state(material);
            state.lastBreakEpochMillis = 0;
            BREAK_DETECTORS.get(material).reset();
            GAIN_DETECTORS.get(material).resetSession(state.sessionBlocks);
            SALE_GATES.get(material).reset();
        }

        for (GemstoneType gemstone : GemstoneType.values()) {
            CONFIG.gemstoneState(
                    gemstone)
                    .lastBreakEpochMillis =
                    0L;
        }

        HUD.onMaterialChanged();
        TrackerStore.save(CONFIG);
    }

    static void setTrackerEnabled(boolean enabled) {
        if (CONFIG.enabled == enabled) return;

        TrackerSelection selection =
                selectedSelection();

        if (selection.isGemstone()) {
            long now =
                    System.currentTimeMillis();

            GemstoneTrackerState state =
                    CONFIG.gemstoneState(
                            selection.gemstone());

            if (!enabled) {
                state.persistActiveTime(
                        now,
                        PAUSE_AFTER_MILLIS);
            }

            state.lastBreakEpochMillis =
                    0L;

            GEMSTONE_GAIN_DETECTOR.reset();
            CONFIG.enabled = enabled;
            TrackerStore.save(CONFIG);
            if (enabled) {
                CURRENT_SESSION.ensureActiveForTracker(selection, now);
                ensureCurrentSessionCollection(now);
                SESSION_ENGINE.onTrackerEnabled(
                        selection,
                        shadowLiveBaseline(selection, now),
                        now);
            } else {
                SESSION_ENGINE.onTrackerDisabled(
                        selection,
                        shadowLiveBaseline(selection, now),
                        now);
            }
            return;
        }

        if (enabled
                && !selection.supportsLiveTracking()) {
            CONFIG.enabled = false;
            TrackerStore.save(CONFIG);
            long shadowNow = System.currentTimeMillis();
            SESSION_ENGINE.onTrackerDisabled(
                    selection,
                    shadowLiveBaseline(selection, shadowNow),
                    shadowNow);
            return;
        }

        long now =
                System.currentTimeMillis();

        if (selectedTarget().isCombined()) {
            persistCombinedActiveTime(now);
            CONFIG.combinedLastBreakEpochMillis = 0;
        }

        for (TrackedMaterial material :
                selectedMaterials()) {
            persistActiveTime(
                    material,
                    now);
        }

        CONFIG.enabled = enabled;

        for (TrackedMaterial material :
                selectedMaterials()) {
            CONFIG.state(material)
                    .lastBreakEpochMillis = 0;

            BREAK_DETECTORS.get(material)
                    .reset();
        }

        TrackerStore.save(CONFIG);
        if (enabled) {
            CURRENT_SESSION.ensureActiveForTracker(selection, now);
            ensureCurrentSessionCollection(now);
            SESSION_ENGINE.onTrackerEnabled(
                    selection,
                    shadowLiveBaseline(selection, now),
                    now);
        } else {
            SESSION_ENGINE.onTrackerDisabled(
                    selection,
                    shadowLiveBaseline(selection, now),
                    now);
        }
    }

    static void setTrackedMaterial(TrackedMaterial material) {
        if (material == null) return;
        setTrackingTarget(TrackingTarget.forMaterial(material));
    }

    static void setTrackingTarget(TrackingTarget target) {
        setTrackingSelection(TrackerSelection.forMaterial(target));
    }

    static TrackerSelection selectedSelection() {
        return CONFIG.selectedSelection();
    }

    static void setTrackingSelection(TrackerSelection selection) {
        TrackerSelection safeSelection =
                selection == null ? TrackerSelection.GOLD : selection;
        if (safeSelection == selectedSelection()) return;

        GEMSTONE_GAIN_DETECTOR.reset();

        TrackerSelection previousSelection = selectedSelection();
        long now = System.currentTimeMillis();

        if (previousSelection.isMaterial()) {
            TrackingTarget previousTarget = previousSelection.materialTarget();
            if (previousTarget.isCombined()) {
                persistCombinedActiveTime(now);
                CONFIG.combinedLastBreakEpochMillis = 0;
            }
            for (TrackedMaterial material : previousTarget.materials()) {
                persistActiveTime(material, now);
                CONFIG.state(material).lastBreakEpochMillis = 0;
                BREAK_DETECTORS.get(material).reset();
                GAIN_DETECTORS.get(material).resetSession(
                        CONFIG.state(material).sessionBlocks);
                SALE_GATES.get(material).reset();
            }
        }
        else if (previousSelection.isGemstone()) {
            GemstoneTrackerState state =
                    CONFIG.gemstoneState(
                            previousSelection.gemstone());

            state.persistActiveTime(
                    now,
                    PAUSE_AFTER_MILLIS);

            state.lastBreakEpochMillis =
                    0L;
        }

        CONFIG.setSelectedSelection(safeSelection);

        if (safeSelection.isMaterial()) {
            TrackingTarget target = safeSelection.materialTarget();
            applyMaterialPrices(
                    marketPrices,
                    now);
            for (TrackedMaterial material : target.materials()) {
                CONFIG.state(material).lastBreakEpochMillis = 0;
                BREAK_DETECTORS.get(material).reset();
                GAIN_DETECTORS.get(material)
                        .resetSession(CONFIG.state(material).sessionBlocks);
                SALE_GATES.get(material).reset();
            }
            CONFIG.enabled = false;
        } else {
            CONFIG.enabled = false;
        }

        HUD.onMaterialChanged();
        TrackerStore.save(CONFIG);
        CURRENT_SESSION.onTargetChanged(safeSelection.id(), now);
        SESSION_ENGINE.onSelectionChanged(
                safeSelection,
                CONFIG.enabled,
                shadowLiveBaseline(safeSelection, now),
                now);
        if (CURRENT_SESSION.isActive()) {
            ensureCurrentSessionCollection(now);
        }
    }

    private static void onShadowWorldChanged() {
        long now = System.currentTimeMillis();
        SharedMiningAimEvidence.reset();
        MOB_LOOT_DETECTOR.reset();
        SlayerRuntime.onWorldChanged();
        DungeonRuntime.onWorldChanged();
        TrackerSelection selection = selectedSelection();
        SESSION_ENGINE.onWorldChanged(
                selection,
                CONFIG.enabled,
                shadowLiveBaseline(selection, now),
                now);
        // A newly joined world has not rendered its own scoreboard yet;
        // never keep a stale area/sub-area from the previous world/session.
        SkyBlockAreaDetector.updateCurrentLocation(SkyBlockLocation.UNKNOWN);
        SkyBlockAreaDetector.clearSkyblockPresence();
        CustomScoreboardRuntime.onWorldChange();
        SkyBlockDungeonDetector.clear();
        // Also update a PAUSED Current Session. The controller stores the
        // pending UNKNOWN area without opening a segment, so reconnect cannot
        // briefly reopen the previous world's area before scoreboard data is
        // available.
        CURRENT_SESSION.onAreaChanged(
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA, now);
        lastAreaCheckMillis = 0L;
    }

    private static long lastAreaCheckMillis;
    private static final long AREA_CHECK_INTERVAL_MILLIS = 1_000L;
    private static final long AREA_STALE_AFTER_MILLIS = 5_000L;

    /**
     * Reads the live scoreboard sidebar (Hypixel renders the current
     * location as one of its lines) and reports a detected area to
     * {@link SkyBlockAreaDetector}. Best-effort and non-gating: a miss on
     * any given check simply leaves the previously known area in place.
     */
    private static void updateDetectedArea(Minecraft client, long now) {
        if (now - lastAreaCheckMillis < AREA_CHECK_INTERVAL_MILLIS) {
            return;
        }
        lastAreaCheckMillis = now;
        if (client.level == null) {
            return;
        }
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar == null) {
            expireDetectedAreaIfStale(now);
            return;
        }
        List<String> lines = new ArrayList<>();
        lines.add(sidebar.getDisplayName().getString());
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(sidebar)) {
            Component line = PlayerTeam.formatNameForTeam(
                    scoreboard.getPlayersTeam(entry.owner()), entry.ownerName());
            lines.add(line.getString());
        }
        DOMAIN_EVENTS.publish(new RotClientDomainEvents.ScoreboardObserved(
                RotClientDomainEvents.Source.SCOREBOARD,
                lines,
                now));
        SkyBlockProfileIdentity.detect(
                CommissionDisplayRuntime.tabLines(client)).ifPresent(profile -> {
            if (!profile.equals(currentProfileId)) {
                String previous = currentProfileId;
                currentProfileId = profile;
                DOMAIN_EVENTS.publish(
                        new RotClientDomainEvents.ProfileChanged(
                                RotClientDomainEvents.Source.SCOREBOARD,
                                previous,
                                profile,
                                now));
            }
        });
        SkyBlockLocation detected =
                SkyBlockAreaDetector.locationFromScoreboardLines(lines);
        SkyBlockAreaDetector.updateSkyblockPresence(lines);
        SkyBlockDungeonDetector.updateSticky(
                SkyBlockDungeonDetector.detectFromScoreboardLines(lines));
        if (!detected.isUnknown()) {
            SkyBlockLocation previous = SkyBlockAreaDetector.detectLocation();
            SkyBlockAreaDetector.updateCurrentLocation(
                    detected.withObservedAt(now));
            if (!detected.equals(previous)) {
                DOMAIN_EVENTS.publish(
                        new RotClientDomainEvents.LocationChanged(
                                RotClientDomainEvents.Source.SCOREBOARD,
                                detected.parentArea().id(),
                                detected.displaySubArea(),
                                now));
            }
            if (CURRENT_SESSION.isActive()) {
                CURRENT_SESSION.onAreaChanged(detected.parentArea(), now);
            }
        } else {
            expireDetectedAreaIfStale(now);
        }
    }

    private static void onOfficialLocation(
            OptionalHypixelModApiRuntime.LocationPacket packet) {
        long now = System.currentTimeMillis();
        SkyBlockLocation detected =
                SkyBlockAreaDetector.locationFromScoreboardLines(
                        OptionalHypixelModApiRuntime.locationHints(packet));
        if (detected.isUnknown()) {
            return;
        }
        SkyBlockLocation previous = SkyBlockAreaDetector.detectLocation();
        SkyBlockAreaDetector.updateCurrentLocation(detected.withObservedAt(now));
        if (!detected.equals(previous)) {
            DOMAIN_EVENTS.publish(new RotClientDomainEvents.LocationChanged(
                    RotClientDomainEvents.Source.HYPIXEL_MOD_API,
                    detected.parentArea().id(),
                    detected.displaySubArea(),
                    now));
        }
        if (CURRENT_SESSION.isActive()) {
            CURRENT_SESSION.onAreaChanged(detected.parentArea(), now);
        }
    }

    private static void expireDetectedAreaIfStale(long now) {
        if (SkyBlockAreaDetector.expireCurrentLocationIfStale(
                now, AREA_STALE_AFTER_MILLIS)
                && CURRENT_SESSION.isActive()) {
            CURRENT_SESSION.onAreaChanged(
                    SkyBlockArea.UNKNOWN_SKYBLOCK_AREA, now);
        }
    }

    private static MiningSessionParity.LiveBaseline shadowLiveBaseline(
            TrackerSelection selection,
            long capturedAtMillis) {
        if (selection == null) {
            throw new IllegalArgumentException(
                    "Shadow baseline selection cannot be null");
        }
        if (selection.isGemstone()) {
            GemstoneLedger ledger = CONFIG.gemstoneState(
                    selection.gemstone()).sessionLedger();
            Map<GemstoneTier, Long> tierQuantities =
                    new EnumMap<>(GemstoneTier.class);
            for (GemstoneTier tier : GemstoneTier.values()) {
                tierQuantities.put(tier, ledger.quantity(tier));
            }
            GemstoneTrackerState state = CONFIG.gemstoneState(
                    selection.gemstone());
            return MiningSessionParity.LiveBaseline.gemstone(
                    selection,
                    tierQuantities,
                    ledger.totalRoughEquivalent(),
                    state.sessionBlocks,
                    capturedAtMillis);
        }

        Map<TrackedMaterial, Long> normalizedQuantities =
                new EnumMap<>(TrackedMaterial.class);
        Map<TrackedMaterial, Long> blockQuantities =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : selection.materialTarget().materials()) {
            MaterialTrackerState state = CONFIG.state(material);
            normalizedQuantities.put(
                    material,
                    state.sessionActualRawEquivalent);
            blockQuantities.put(material, state.sessionBlocks);
        }
        return MiningSessionParity.LiveBaseline.material(
                selection,
                normalizedQuantities,
                blockQuantities,
                capturedAtMillis);
    }

    private static boolean isGemstoneTrackingActive() {
        return GemstoneLiveAccounting.isActive(
                CONFIG);
    }

    private static void onGemstoneBlockObserved(
            GemstoneType gemstone,
            long epochMillis) {
        if (!isGemstoneTrackingActive()
                || gemstone == null
                || CONFIG.selectedGemstone()
                != gemstone) {
            SESSION_ENGINE.onConfirmedGemstoneBreak(
                    gemstone,
                    epochMillis);
            return;
        }

        GemstoneTrackerState state =
                CONFIG.gemstoneState(
                        gemstone);

        state.persistActiveTime(
                epochMillis,
                PAUSE_AFTER_MILLIS);

        if (!GemstoneLiveAccounting.recordBlock(
                CONFIG,
                gemstone,
                epochMillis)) {
            SESSION_ENGINE.onConfirmedGemstoneBreak(
                    gemstone,
                    epochMillis);
            return;
        }

        DiagnosticRecorder.record(
                "GEMSTONE_LEDGER_BLOCK",
                "gemstone="
                        + gemstone.id()
                        + " sessionBlocks="
                        + state.sessionBlocks
                        + " totalBlocks="
                        + state.totalBlocks);
        if (SESSION_ENGINE.isObservationEnabled()) {
            SESSION_ENGINE.onAcceptedTargetGemstoneBlock(
                    gemstone,
                    state.sessionBlocks,
                    epochMillis,
                    "target-gemstone-block:"
                            + gemstone.id()
                            + ":" + epochMillis
                            + ":" + state.sessionBlocks,
                    "confirmed-break");
        }
    }

    private static void onGemstoneGainObserved(
            GemstoneType gemstone,
            GemstoneTier tier,
            long amount,
            String source) {
        if (!GemstoneLiveAccounting.recordGain(
                CONFIG,
                gemstone,
                tier,
                amount)) {
            return;
        }

        GemstoneTrackerState state =
                CONFIG.gemstoneState(
                        gemstone);

        DiagnosticRecorder.record(
                "GEMSTONE_LEDGER_GAIN",
                "gemstone="
                        + gemstone.id()
                        + " tier="
                        + tier.id()
                        + " amount="
                        + amount
                        + " source="
                        + source
                        + " sessionItems="
                        + state.sessionLedger()
                                .totalItemCount()
                        + " sessionRoughEquivalent="
                        + state.sessionLedger()
                                .totalRoughEquivalent());
        if (SESSION_ENGINE.isObservationEnabled()) {
            long observedAtMillis = System.currentTimeMillis();
            MiningSessionObservation.EvidenceType evidence =
                    "PRISTINE".equals(source)
                            ? MiningSessionObservation.EvidenceType
                            .PRISTINE_MESSAGE
                            : MiningSessionObservation.EvidenceType.SACK_CHANGE;
            SESSION_ENGINE.onAcceptedTargetGemstoneQuantity(
                    gemstone,
                    tier,
                    amount,
                    evidence,
                    state.sessionLedger().quantity(tier),
                    state.sessionLedger().totalRoughEquivalent(),
                    observedAtMillis,
                    "target-gemstone-quantity:"
                            + gemstone.id()
                            + ":" + tier.id()
                            + ":" + observedAtMillis
                            + ":" + state.sessionLedger().quantity(tier),
                    source);
            if (TargetItemGainPipeline.creditGemstone(
                    CURRENT_SESSION,
                    gemstone,
                    tier,
                    amount,
                    SkyBlockAreaDetector.detect(),
                    observedAtMillis)) {
                cachedHudOtherSummary = null;
            }
        }
    }

    public static boolean shouldKeepPauseBackgroundClear() {
        return CONFIG.enabled;
    }

    public static boolean isFullbrightEnabled() {
        return CONFIG.fullbrightEnabled;
    }

    public static boolean isAlwaysNightEnabled() {
        return CONFIG.alwaysNightEnabled;
    }

    static FullbrightNightPolicy.LightingState lightingState() {
        return new FullbrightNightPolicy.LightingState(
                CONFIG.fullbrightEnabled,
                CONFIG.alwaysNightEnabled,
                CONFIG.lightingForceBoth);
    }

    static void applyLightingState(FullbrightNightPolicy.LightingState state) {
        FullbrightNightPolicy.LightingState next =
                state == null ? FullbrightNightPolicy.LightingState.defaults() : state;
        if (CONFIG.fullbrightEnabled == next.fullbright()
                && CONFIG.alwaysNightEnabled == next.alwaysNight()
                && CONFIG.lightingForceBoth == next.forceBoth()) {
            return;
        }
        boolean nightWas = CONFIG.alwaysNightEnabled;
        boolean worldLoaded = Minecraft.getInstance() != null
                && Minecraft.getInstance().level != null;
        CONFIG.fullbrightEnabled = next.fullbright();
        CONFIG.alwaysNightEnabled = next.alwaysNight();
        CONFIG.lightingForceBoth = next.forceBoth();
        save();
        FullbrightNightRuntime.onAlwaysNightChanged(nightWas, next.alwaysNight(), worldLoaded);
    }

    static void setLightingCardEnabled(boolean enabled) {
        FullbrightNightPolicy.LightingState current = lightingState();
        applyLightingState(enabled
                ? FullbrightNightPolicy.enableCard(current)
                : FullbrightNightPolicy.disableCard(current));
    }

    static void resetLightingModule() {
        applyLightingState(FullbrightNightPolicy.LightingState.defaults());
    }

    static Boolean readLightingSetting(String settingId) {
        if (!FullbrightNightPolicy.isLightingSetting(settingId)) {
            return null;
        }
        return switch (settingId) {
            case FullbrightNightPolicy.USE_FULLBRIGHT -> CONFIG.fullbrightEnabled;
            case FullbrightNightPolicy.ALWAYS_NIGHT -> CONFIG.alwaysNightEnabled;
            case FullbrightNightPolicy.FORCE_BOTH -> CONFIG.lightingForceBoth;
            default -> null;
        };
    }

    static boolean writeLightingSetting(String settingId, boolean value) {
        if (!FullbrightNightPolicy.isLightingSetting(settingId)) {
            return false;
        }
        FullbrightNightPolicy.LightingState current = lightingState();
        FullbrightNightPolicy.LightingState next = switch (settingId) {
            case FullbrightNightPolicy.USE_FULLBRIGHT ->
                    FullbrightNightPolicy.setFullbright(current, value);
            case FullbrightNightPolicy.ALWAYS_NIGHT ->
                    FullbrightNightPolicy.setAlwaysNight(current, value);
            case FullbrightNightPolicy.FORCE_BOTH ->
                    FullbrightNightPolicy.setForceBoth(current, value);
            default -> current;
        };
        applyLightingState(next);
        return true;
    }

    static void setFullbrightEnabled(boolean enabled) {
        applyLightingState(FullbrightNightPolicy.setFullbright(lightingState(), enabled));
    }

    public static boolean isAutoSprintEnabled() {
        return CONFIG.autoSprintEnabled;
    }

    static void setAutoSprintEnabled(boolean enabled) {
        if (CONFIG.autoSprintEnabled == enabled) return;
        CONFIG.autoSprintEnabled = enabled;
        TrackerStore.save(CONFIG);
    }

    public static boolean isAutoClickerEnabled() {
        return qolConfig().autoClickerEnabled;
    }

    private static int autoClickerAdd(FabricClientCommandSource source, boolean left) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            source.sendFeedback(Component.literal("Auto Clicker: no local player."));
            return 0;
        }
        QolUtilityConfig qol = qolConfig();
        String identity = AutoClickerItemIdentity.identify(
                client.player.getMainHandItem());
        if (identity.isBlank()) {
            source.sendFeedback(Component.literal(
                    "Auto Clicker: hold an item to whitelist first."));
            return 0;
        }
        List<String> list = left
                ? AutoClickerWhitelist.ensureMutable(qol.autoClickerLeftWhitelist)
                : AutoClickerWhitelist.ensureMutable(qol.autoClickerRightWhitelist);
        if (left) {
            qol.autoClickerLeftWhitelist = list;
        } else {
            qol.autoClickerRightWhitelist = list;
        }
        if (!AutoClickerWhitelist.add(list, identity)) {
            source.sendFeedback(Component.literal(
                    "Auto Clicker: already whitelisted on "
                            + (left ? "left" : "right")
                            + ": "
                            + identity));
            return 1;
        }
        TrackerStore.save(CONFIG);
        source.sendFeedback(Component.literal(
                "Auto Clicker: added to "
                        + (left ? "left" : "right")
                        + " whitelist: "
                        + identity));
        return 1;
    }

    private static int autoClickerRemove(FabricClientCommandSource source, boolean left) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            source.sendFeedback(Component.literal("Auto Clicker: no local player."));
            return 0;
        }
        QolUtilityConfig qol = qolConfig();
        String identity = AutoClickerItemIdentity.identify(
                client.player.getMainHandItem());
        List<String> list = left
                ? qol.autoClickerLeftWhitelist
                : qol.autoClickerRightWhitelist;
        if (!AutoClickerWhitelist.remove(list, identity)) {
            source.sendFeedback(Component.literal(
                    "Auto Clicker: not on "
                            + (left ? "left" : "right")
                            + " whitelist: "
                            + identity));
            return 0;
        }
        TrackerStore.save(CONFIG);
        source.sendFeedback(Component.literal(
                "Auto Clicker: removed from "
                        + (left ? "left" : "right")
                        + " whitelist: "
                        + identity));
        return 1;
    }

    private static int autoClickerList(FabricClientCommandSource source) {
        QolUtilityConfig qol = qolConfig();
        source.sendFeedback(Component.literal(
                "Auto Clicker left: "
                        + formatAutoClickerWhitelist(qol.autoClickerLeftWhitelist)));
        source.sendFeedback(Component.literal(
                "Auto Clicker right: "
                        + formatAutoClickerWhitelist(qol.autoClickerRightWhitelist)));
        return 1;
    }

    private static String formatAutoClickerWhitelist(List<String> entries) {
        if (entries == null || entries.isEmpty()) {
            return "(empty)";
        }
        return String.join(", ", entries);
    }

    public static boolean isCameraEnabled() {
        return CONFIG.cameraEnabled;
    }

    static void setCameraEnabled(boolean enabled) {
        if (CONFIG.cameraEnabled == enabled) return;
        CONFIG.cameraEnabled = enabled;
        TrackerStore.save(CONFIG);
        enforceCameraPerspective();
    }

    /**
     * Address-based Hypixel detection helper. Not used to gate Auto Sprint;
     * Auto Sprint uses an Input.sprint override on all servers.
     */
    public static boolean isConnectedToHypixel() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) {
            return false;
        }
        var server = client.getCurrentServer();
        if (server == null || server.ip == null) {
            return false;
        }
        return HypixelServerPolicy.isHypixelAddress(server.ip);
    }

    /** Removes only front-facing third person (tick / enable / join). */
    static void enforceCameraPerspective() {
        if (!CONFIG.cameraEnabled) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        if (client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            client.options.setCameraType(CameraType.FIRST_PERSON);
        }
    }

    private static int setFortune(
            FabricClientCommandSource source,
            double mining,
            double materialFortune) {
        if (!isMaterialSelection()) {
            return rejectMaterialFortuneCommand(source);
        }
        TrackedMaterial material = selectedMaterial();
        CONFIG.fortuneAuto = false;
        CONFIG.miningFortune = mining;
        switch (material.fortuneType()) {
            case BLOCK -> CONFIG.blockFortune = materialFortune;
            case ORE -> CONFIG.oreFortune = materialFortune;
            case DWARVEN_METAL -> CONFIG.dwarvenMetalFortune = materialFortune;
            case GEMSTONE -> CONFIG.gemstoneFortune = materialFortune;
            case NONE, UNKNOWN -> {
                return rejectMaterialFortuneCommand(source);
            }
        }
        CONFIG.fortuneLastDetectedEpochMillis = System.currentTimeMillis();
        CONFIG.fortuneSource = "manual command";
        TrackerStore.save(CONFIG);
        source.sendFeedback(Component.literal(String.format(
                Locale.ROOT,
                "Rot Client fortune: %.0f Mining + %.0f %s = %.0f effective",
                mining,
                materialFortune,
                material.fortuneType().displayName(),
                mining + materialFortune)));
        return 1;
    }

    private static int autoFortune(FabricClientCommandSource source) {
        if (!isMaterialSelection()) {
            return rejectMaterialFortuneCommand(source);
        }
        CONFIG.fortuneAuto = true;
        CONFIG.fortuneLastDetectedEpochMillis = 0;
        CONFIG.fortuneSource = "waiting for auto detection";
        TrackerStore.save(CONFIG);
        source.sendFeedback(Component.literal(
                "Optional live Fortune detection enabled. "
                        + "Drop tracking calibrates itself from packets."));
        return 1;
    }

    private static int rejectMaterialFortuneCommand(
            FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(
                "Rot Client Fortune commands are material-only and "
                        + "cannot be used with a gemstone tracker."));
        return 0;
    }

    private static int status(FabricClientCommandSource source) {
        TrackerSelection selection =
                selectedSelection();

        if (selection.isGemstone()) {
            GemstoneType gemstone =
                    selection.gemstone();

            GemstoneTrackerState state =
                    CONFIG.gemstoneState(
                            gemstone);

            GemstoneLedger ledger =
                    state.sessionLedger();

            long activeMillis =
                    state.currentSessionActiveMillis(
                            System.currentTimeMillis(),
                            PAUSE_AFTER_MILLIS);

            source.sendFeedback(Component.literal(String.format(
                    Locale.ROOT,
                    "Rot Client status (%s Gemstone)\n"
                            + "Blocks: %d\n"
                            + "Rough: %d\n"
                            + "Flawed: %d\n"
                            + "Fine: %d\n"
                            + "Flawless: %d\n"
                            + "Perfect: %d\n"
                            + "Total items: %d\n"
                            + "Rough equivalent: %d\n"
                            + "Active time: %.1f seconds\n"
                            + "Tracker: %s",
                    gemstone.displayName(),
                    state.sessionBlocks,
                    ledger.quantity(
                            GemstoneTier.ROUGH),
                    ledger.quantity(
                            GemstoneTier.FLAWED),
                    ledger.quantity(
                            GemstoneTier.FINE),
                    ledger.quantity(
                            GemstoneTier.FLAWLESS),
                    ledger.quantity(
                            GemstoneTier.PERFECT),
                    ledger.totalItemCount(),
                    ledger.totalRoughEquivalent(),
                    activeMillis / 1_000.0,
                    CONFIG.enabled
                            ? "ON"
                            : "OFF")));

            return 1;
        }

        if (selectedTarget().isCombined()) {
            MaterialTrackerState mithril =
                    CONFIG.state(TrackedMaterial.MITHRIL);
            MaterialTrackerState titanium =
                    CONFIG.state(TrackedMaterial.TITANIUM);
            source.sendFeedback(Component.literal(String.format(
                    Locale.ROOT,
                    "Rot Client status (%s)\n"
                            + "Mithril: %d blocks, %d raw + %d enchanted, "
                            + "%.1f net coins\n"
                            + "Titanium: %d blocks, %d raw + %d enchanted, "
                            + "%.1f net coins\n"
                            + "Combined session net: %.1f coins\n"
                            + "Dwarven Metal Fortune: %.1f + "
                            + "Mining Fortune: %.1f",
                    selectedTarget().displayName(),
                    mithril.sessionBlocks,
                    currentSessionRawItems(TrackedMaterial.MITHRIL),
                    currentSessionEnchantedItems(TrackedMaterial.MITHRIL),
                    currentSessionProfit(TrackedMaterial.MITHRIL),
                    titanium.sessionBlocks,
                    currentSessionRawItems(TrackedMaterial.TITANIUM),
                    currentSessionEnchantedItems(TrackedMaterial.TITANIUM),
                    currentSessionProfit(TrackedMaterial.TITANIUM),
                    currentSessionProfit(),
                    CONFIG.dwarvenMetalFortune,
                    CONFIG.miningFortune)));
            return 1;
        }
        TrackedMaterial material = selectedMaterial();
        MaterialTrackerState state = CONFIG.state(material);
        long now = System.currentTimeMillis();
        String fortuneAge = CONFIG.fortuneLastDetectedEpochMillis <= 0
                ? "never"
                : ((now - CONFIG.fortuneLastDetectedEpochMillis) / 1_000)
                        + "s ago";
        double observedPerBlock = state.sessionBlocks <= 0
                ? 0
                : (double) state.sessionActualRawEquivalent
                        / state.sessionBlocks;
        double observedFortune = state.sessionActualRawEquivalent <= 0
                || state.sessionBaseDrops <= 0
                ? 0
                : Math.max(
                        0,
                        ((double) state.sessionActualRawEquivalent
                                / state.sessionBaseDrops - 1.0)
                                * 100.0);
        double categoryFortune = categoryFortune(material);
        source.sendFeedback(Component.literal(String.format(
                Locale.ROOT,
                "Rot Client status (%s)\n"
                        + "Blocks: %d\nTracked %s equivalent: %d (%s)\n"
                        + "Earned: %d %s + %d %s\n"
                        + "Unsold now: %d raw + %d enchanted\n"
                        + "Already sold: %d raw + %d enchanted "
                        + "(gross %.1f coins)\n"
                        + "Compactor observed: %d, Compact bonuses: %d\n"
                        + "Mining Sacks: %d raw + %d enchanted\n"
                        + "Estimated session net: %.1f, "
                        + "unsold estimate: %.1f (tax %.3f%%)\n"
                        + "Observed: %.2f %s/block, "
                        + "~%.1f effective Fortune\n"
                        + "Optional live stats: %.1f Mining + %.1f %s = %.1f\n"
                        + "Stat source: %s (%s)\n"
                        + "Diagnostic recording: %s",
                material.pureDisplayName(),
                state.sessionBlocks,
                material.displayName(),
                state.sessionActualRawEquivalent,
                state.actualRawEquivalentSource,
                currentSessionRawItems(material),
                material.rawItemName(),
                currentSessionEnchantedItems(material),
                material.enchantedItemName(),
                currentSessionUnsoldRawItems(material),
                currentSessionUnsoldEnchantedItems(material),
                state.sessionSoldRaw,
                state.sessionSoldEnchanted,
                state.sessionRealizedGrossCoins,
                state.sessionCompactedEnchanted,
                state.sessionCompactBonusEnchanted,
                state.sessionSackRaw,
                state.sessionSackEnchanted,
                currentSessionProfit(material),
                currentSessionUnsoldProfit(material),
                CONFIG.bazaarTaxPercent,
                observedPerBlock,
                material.displayName(),
                observedFortune,
                CONFIG.miningFortune,
                categoryFortune,
                material.fortuneType().displayName(),
                CONFIG.miningFortune + categoryFortune,
                CONFIG.fortuneSource,
                fortuneAge,
                DiagnosticRecorder.isRecording() ? "ON" : "OFF")));
        return 1;
    }

    private static int shadowStatus(FabricClientCommandSource source) {
        long now = System.currentTimeMillis();
        MiningSessionSnapshot snapshot = SESSION_ENGINE.snapshot(now)
                .orElse(null);
        if (snapshot == null) {
            source.sendFeedback(Component.literal(
                    "Rot Client shadow: no active or retained session."));
            return 1;
        }

        String lastAccepted = snapshot.lastAcceptedEventTimestamp().isEmpty()
                ? "none"
                : snapshot.lastAcceptedEventTimestamp().getAsLong()
                + " (ageMillis="
                + Math.max(
                0L,
                now - snapshot.lastAcceptedEventTimestamp().getAsLong())
                + ")";
        source.sendFeedback(Component.literal(
                "Rot Client shadow\n"
                        + "Diagnostic: "
                        + (snapshot.diagnosticsActive()
                        ? "active" : "inactive")
                        + ", tracker: "
                        + (snapshot.trackerEnabled() ? "ON" : "OFF")
                        + ", target: "
                        + snapshot.selectedTracker().id()
                        + "\nSession ID: " + snapshot.sessionId()
                        + ", session epoch: " + snapshot.sessionEpoch()
                        + ", selection epoch: " + snapshot.selectionEpoch()
                        + "\nLedger item entries: TARGET_MINED="
                        + snapshot.targetMinedEntryCount()
                        + ", OTHER_MINED="
                        + snapshot.otherMinedEntryCount()
                        + ", total=" + snapshot.entryCount()
                        + "\nTarget item quantities: "
                        + formatShadowQuantities(snapshot.quantities(
                        MiningSessionCategory.TARGET_MINED))
                        + "\nOther item quantities: "
                        + formatShadowQuantities(snapshot.quantities(
                        MiningSessionCategory.OTHER_MINED))
                        + "\nLedger entry price state UNRESOLVED: "
                        + snapshot.unresolvedPriceEntryCount()
                        + ", last accepted: " + lastAccepted
                        + "\nTarget parity aggregate: "
                        + snapshot.targetParityStatus().name()
                        + ", mismatches: "
                        + snapshot.parityMismatchCount()
                        + "\nCHEST_LOOT="
                        + snapshot.chestLootEntryCount()
                        + ", CURRENCY="
                        + snapshot.currencyEntryCount()
                        + "\nChest item quantities: "
                        + formatShadowQuantities(snapshot.quantities(
                        MiningSessionCategory.CHEST_LOOT))
                        + "\nCurrency quantities: "
                        + formatShadowQuantities(snapshot.quantities(
                        MiningSessionCategory.CURRENCY))
                        + "\n"
                        + formatShadowValuation(snapshot.valuation())));
        return 1;
    }

    private static int sessionStart(FabricClientCommandSource source) {
        MiningSessionAnalyticsController.StartResult result =
                startSessionAnalytics();
        if (result
                == MiningSessionAnalyticsController.StartResult
                .PERSISTENCE_BLOCKED) {
            source.sendError(Component.literal(
                    "Current Session cannot resume while its persistence "
                            + "file requires recovery."));
            return 0;
        }
        if (result == MiningSessionAnalyticsController.StartResult.ALREADY_ACTIVE) {
            source.sendFeedback(Component.literal(
                    "Current Session is already running."));
            return 1;
        }
        source.sendFeedback(Component.literal(
                "Current Session resumed. Live tracker totals are unchanged."));
        return 1;
    }

    private static int sessionStop(FabricClientCommandSource source) {
        MiningSessionAnalyticsController.StopResult result =
                stopSessionAnalytics();
        switch (result) {
            case STOPPED -> source.sendFeedback(Component.literal(
                    "Current Session paused. Final snapshot retained."));
            case ALREADY_STOPPED -> source.sendFeedback(Component.literal(
                    "Current Session is already paused."));
            case NOTHING_TO_STOP -> source.sendFeedback(Component.literal(
                    "No Current Session to pause."));
        }
        return 1;
    }

    private static int sessionReset(FabricClientCommandSource source) {
        MiningSessionAnalyticsController.ResetResult result =
                resetSessionAnalytics();
        if (result
                == MiningSessionAnalyticsController.ResetResult.NOTHING_TO_CLEAR) {
            source.sendFeedback(Component.literal(
                    "No Session Analytics session to reset."));
            return 1;
        }
        source.sendFeedback(Component.literal(
                "Current Session quantities cleared. Collection state stays aligned."));
        return 1;
    }

    private static int sessionStatus(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(SESSION_ANALYTICS.statusText(
                CURRENT_SESSION.snapshotConfig())));
        return 1;
    }

    private static int sessionCopy(FabricClientCommandSource source) {
        MiningSessionAnalyticsController.CopyResult result =
                SESSION_ANALYTICS.copySummary(
                        CURRENT_SESSION.snapshotConfig());
        switch (result) {
            case COPIED -> source.sendFeedback(Component.literal(
                    "Session Analytics summary copied to clipboard."));
            case NOTHING_TO_COPY -> source.sendFeedback(Component.literal(
                    "No Session Analytics summary to copy."));
            case FAILED -> source.sendFeedback(Component.literal(
                    "Could not copy Session Analytics summary."));
        }
        return 1;
    }

    private static int sessionSave(FabricClientCommandSource source) {
        MiningSessionHistoryController.SaveResult result =
                SESSION_HISTORY.saveCurrentStoppedSession();
        switch (result) {
            case SAVED -> source.sendFeedback(Component.literal(
                    "Paused Current Session freeze saved to local history."));
            case ALREADY_SAVED -> source.sendFeedback(Component.literal(
                    "That Current Session freeze is already saved in Session History."));
            case REJECTED_ACTIVE -> source.sendFeedback(Component.literal(
                    "Pause Current Session before saving it to Session History."));
            case REJECTED_NO_SESSION -> source.sendFeedback(Component.literal(
                    "No Current Session is available to save."));
            case UNAVAILABLE -> source.sendFeedback(Component.literal(
                    "Session History is unavailable."));
            case WRITE_FAILED -> source.sendFeedback(Component.literal(
                    "Could not write Session History."));
        }
        return 1;
    }

    private static int openTermSim(FabricClientCommandSource source) {
        return openTermSimPing(source, -1);
    }

    private static int openTermSimPing(FabricClientCommandSource source, int ping) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            source.sendError(Component.literal("Join the world before opening Terminal Simulator."));
            return 0;
        }
        TermSimRuntime.openFromCommand(ping);
        source.sendFeedback(Component.literal("Opened Terminal Simulator. Auto Terms still solves real F7 chests."));
        return 1;
    }

    private static int rotClientHelp(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(
                """
                Rot Client:
                Right Shift (or the Click GUI key) opens the dashboard.
                /rot              open dashboard
                /rot ui           same
                /rot qol          open the Modules catalog (Combat first)
                /rot edit         HUD layout editor
                /rot help         this list
                /rot layout reset
                /rot layout reset ui|hud

                Session:
                /rot session help
                /rot history help

                Mining:
                /rot target <material>
                /rot record start|stop
                /rot fortune auto|<mining> [material]
                /rot shadow status

                Other:
                /rot slayer status | carry ...
                /rot termsim [ping]
                /rot superboom add|remove|list
                /rot dcarry add|remove|list|history
                /rot toggle|reset|status

                Old aliases /rotclient, /miningtracker, /miningui still open
                the dashboard and print a deprecation notice."""
                        .trim()));
        return 1;
    }

    private static int sessionHelp(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(
                """
                Session Analytics commands:
                /rot session start
                /rot session stop
                /rot session reset
                /rot session status
                /rot session copy
                /rot session save
                Save stores only an explicitly retained PAUSED Current Session."""
                        .trim()));
        return 1;
    }

    private static int historyHelp(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(
                """
                Session History commands:
                /rot history list
                /rot history open <index-or-id>
                /rot history copy <index-or-id>
                /rot history delete <index-or-id>
                /rot history clear
                /rot history clear confirm <token>
                History is newest-first, local-only, and capped at 20 records."""
                        .trim()));
        return 1;
    }

    private static int historyUsage(
            FabricClientCommandSource source,
            String action) {
        source.sendFeedback(Component.literal(switch (action) {
            case "open" ->
                    "Usage: /rot history open <index-or-id>";
            case "copy" ->
                    "Usage: /rot history copy <index-or-id>";
            case "delete" ->
                    "Usage: /rot history delete <index-or-id>";
            case "clear confirm" ->
                    "Usage: /rot history clear confirm <token>";
            default -> "Usage: /rot history help";
        }));
        return 0;
    }

    private static int historyList(FabricClientCommandSource source) {
        source.sendFeedback(Component.literal(SESSION_HISTORY.listText()));
        return 1;
    }

    private static int historyOpen(
            FabricClientCommandSource source,
            String indexOrId) {
        MiningSessionHistoryController.OpenResult result =
                SESSION_HISTORY.open(indexOrId);
        switch (result) {
            case OPENED -> source.sendFeedback(Component.literal(
                    "Opened saved session in Session History. Live analytics are unchanged."));
            case NOT_FOUND -> source.sendFeedback(Component.literal(
                    "No Session History record matched that index or id."));
            case UNAVAILABLE -> source.sendFeedback(Component.literal(
                    "Session History is unavailable."));
        }
        return 1;
    }

    private static int historyCopy(
            FabricClientCommandSource source,
            String indexOrId) {
        MiningSessionHistoryController.CopyResult result =
                SESSION_HISTORY.copy(indexOrId);
        switch (result) {
            case COPIED -> source.sendFeedback(Component.literal(
                    "Saved session summary copied to clipboard."));
            case NOT_FOUND -> source.sendFeedback(Component.literal(
                    "No Session History record matched that index or id."));
            case UNAVAILABLE -> source.sendFeedback(Component.literal(
                    "Session History is unavailable."));
            case FAILED -> source.sendFeedback(Component.literal(
                    "Could not copy the saved session summary."));
        }
        return 1;
    }

    private static int historyDelete(
            FabricClientCommandSource source,
            String indexOrId) {
        MiningSessionHistoryController.DeleteResult result =
                SESSION_HISTORY.delete(indexOrId);
        switch (result) {
            case DELETED -> source.sendFeedback(Component.literal(
                    "Deleted one Session History record. Live analytics and tracker totals are unchanged."));
            case NOT_FOUND -> source.sendFeedback(Component.literal(
                    "No Session History record matched that index or id."));
            case UNAVAILABLE -> source.sendFeedback(Component.literal(
                    "Session History is unavailable."));
            case WRITE_FAILED -> source.sendFeedback(Component.literal(
                    "Could not update Session History."));
        }
        return 1;
    }

    private static int historyClear(FabricClientCommandSource source) {
        MiningSessionHistoryController.ClearResult result =
                SESSION_HISTORY.requestClear();
        switch (result) {
            case CONFIRMATION_REQUIRED -> source.sendFeedback(Component.literal(
                    MiningSessionHistoryController.clearConfirmationPrompt(
                            SESSION_HISTORY.pendingClearToken())));
            case NOTHING_TO_CLEAR -> source.sendFeedback(Component.literal(
                    "Session History is already empty."));
            case UNAVAILABLE -> source.sendFeedback(Component.literal(
                    "Session History is unavailable."));
            default -> source.sendFeedback(Component.literal(
                    "Could not prepare Session History clear."));
        }
        return 1;
    }

    private static int historyClearConfirm(
            FabricClientCommandSource source,
            String token) {
        MiningSessionHistoryController.ClearResult result =
                SESSION_HISTORY.confirmClear(token);
        switch (result) {
            case CLEARED -> source.sendFeedback(Component.literal(
                    "Session History cleared. Live analytics and TrackerStore are unchanged."));
            case CONFIRMATION_EXPIRED -> source.sendFeedback(Component.literal(
                    "Clear confirmation expired or token mismatched. Run history clear again."));
            case NOTHING_TO_CLEAR -> source.sendFeedback(Component.literal(
                    "Session History is already empty."));
            case UNAVAILABLE -> source.sendFeedback(Component.literal(
                    "Session History is unavailable."));
            case WRITE_FAILED -> source.sendFeedback(Component.literal(
                    "Could not clear Session History."));
            case CONFIRMATION_REQUIRED -> source.sendFeedback(Component.literal(
                    "Confirmation required."));
        }
        return 1;
    }

    static MiningSessionAnalyticsPresentation sessionAnalyticsPresentation() {
        RotClientCurrentSessionConfig session = CURRENT_SESSION.snapshotConfig();
        return MiningSessionAnalyticsPresentation.from(
                SESSION_ANALYTICS.viewModel(session),
                session);
    }

    /**
     * Cached compact HUD OTHERS projection from the persistent Current Session
     * (non-target mining + mob + chest/reward item units — not OTHER_MINED alone).
     */
    static MiningHudOtherSummary hudOtherMinedSummary() {
        long now = System.currentTimeMillis();
        if (cachedHudOtherSummary != null
                && now - cachedHudOtherSummaryAtMillis < 250L) {
            return cachedHudOtherSummary;
        }
        cachedHudOtherSummary = CURRENT_SESSION.otherHudSummary();
        cachedHudOtherSummaryAtMillis = now;
        return cachedHudOtherSummary;
    }

    static RotClientCurrentSessionConfig currentSessionSnapshot() {
        return CURRENT_SESSION.snapshotConfig();
    }

    /** Current Session age minus paused time, for HUD/UI duration display. */
    static long currentSessionActiveDurationMillis(long now) {
        return CURRENT_SESSION.activeDurationMillis(now);
    }

    static MiningSessionHistoryPresentation sessionHistoryPresentation() {
        return SESSION_HISTORY.presentation();
    }

    static MiningSessionAnalyticsController.StartResult
            startSessionAnalytics() {
        return resumeCurrentSession();
    }

    static MiningSessionAnalyticsController.StopResult
            stopSessionAnalytics() {
        return pauseCurrentSession();
    }

    static MiningSessionAnalyticsController.StartResult resumeCurrentSession() {
        long now = System.currentTimeMillis();
        if (CURRENT_SESSION.isActive()) {
            ensureCurrentSessionCollection(now);
            return MiningSessionAnalyticsController.StartResult.ALREADY_ACTIVE;
        }
        if (!CURRENT_SESSION.resume(now)) {
            if (SESSION_ENGINE.isCollectionActive()) {
                SESSION_ANALYTICS.stop();
            }
            return MiningSessionAnalyticsController.StartResult
                    .PERSISTENCE_BLOCKED;
        }
        ensureCurrentSessionCollection(now);
        return MiningSessionAnalyticsController.StartResult.STARTED;
    }

    static MiningSessionAnalyticsController.StopResult pauseCurrentSession() {
        long now = System.currentTimeMillis();
        if (CURRENT_SESSION.isPaused()) {
            if (SESSION_ENGINE.isCollectionActive()) {
                SESSION_ANALYTICS.stop();
            }
            CURRENT_SESSION.flushIfDirty(now, true);
            return MiningSessionAnalyticsController.StopResult.ALREADY_STOPPED;
        }
        if (SESSION_ENGINE.isCollectionActive()) {
            SESSION_ANALYTICS.stop();
        }
        CURRENT_SESSION.pause(now);
        CURRENT_SESSION.flushIfDirty(now, true);
        return MiningSessionAnalyticsController.StopResult.STOPPED;
    }

    static CurrentSessionStartNewCoordinator.Result startNewCurrentSession() {
        long now = System.currentTimeMillis();
        if (SESSION_ENGINE.isCollectionActive()) {
            SESSION_ANALYTICS.stop();
        }
        CurrentSessionStartNewCoordinator.Result result =
                CurrentSessionStartNewCoordinator.startDurably(
                        CURRENT_SESSION,
                        SESSION_HISTORY,
                        now,
                        CurrentSessionStartNewJournal.defaultPath());
        if (CURRENT_SESSION.isActive()) {
            ensureCurrentSessionCollection(now);
        }
        cachedHudOtherSummary = null;
        if (result != CurrentSessionStartNewCoordinator.Result.STARTED) {
            String message = switch (result) {
                case TRANSACTION_PREPARE_FAILED ->
                        "Start New failed: the recovery marker could not be "
                                + "written. Current Session was preserved.";
                case ARCHIVE_FAILED ->
                        "Start New failed: Session History was not written. "
                                + "Current Session was preserved.";
                case CURRENT_SAVE_FAILED_ROLLED_BACK ->
                        "Start New failed: the new Current Session could not "
                                + "be saved. The original session was preserved.";
                case ROLLBACK_FAILED ->
                        "Start New failed and History rollback also failed. "
                                + "Current Session was preserved; inspect local files.";
                case STARTED -> "";
            };
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.sendSystemMessage(
                        Component.literal("Rot Client: " + message));
            }
        }
        return result;
    }

    static MiningSessionAnalyticsController.ResetResult
            resetSessionAnalytics() {
        long now = System.currentTimeMillis();
        boolean wasActive = CURRENT_SESSION.isActive();
        MiningSessionAnalyticsController.ResetResult result =
                SESSION_ANALYTICS.reset();
        boolean clearedCanonical = CURRENT_SESSION.clearCreditedItems(now);
        cachedHudOtherSummary = null;
        if (wasActive) {
            // Reset must never leave Current Session RUNNING while generic
            // collection is silently OFF — restart collection together.
            ensureCurrentSessionCollection(now);
        }
        CURRENT_SESSION.flushIfDirty(now, true);
        return clearedCanonical
                ? MiningSessionAnalyticsController.ResetResult.CLEARED
                : result;
    }

    static MiningSessionAnalyticsController.CopyResult
            copySessionAnalyticsSummary() {
        return SESSION_ANALYTICS.copySummary(
                CURRENT_SESSION.snapshotConfig());
    }

    private static void ensureCurrentSessionCollection(long now) {
        TrackerSelection selection = selectedSelection();
        CURRENT_SESSION.ensureActiveForTracker(selection, now);
        if (!CURRENT_SESSION.isActive()) {
            return;
        }
        if (!SESSION_ENGINE.isCollectionActive()) {
            SESSION_ANALYTICS.start(CONFIG.enabled, selection);
        }
    }

    private static void syncCurrentSessionFromEngine(long now) {
        if (!CURRENT_SESSION.isActive()) {
            return;
        }
        if (!CURRENT_SESSION.shouldSync(now, 0L)) {
            return;
        }
        try {
            MiningSessionPriceBook priceBook =
                    SESSION_ENGINE.capturePriceBook(now);
            Optional<MiningSessionSnapshot> snap =
                    SESSION_ENGINE.snapshot(now);
            CURRENT_SESSION.refreshFromEngineMetadata(
                    snap.orElse(null),
                    selectedSelection(),
                    priceBook,
                    now);
        } catch (RuntimeException failure) {
            // Valuation refresh must never terminate Minecraft. Quantities
            // remain owned by Current Session; prices stay unresolved.
            DiagnosticRecorder.record(
                    "CURRENT_SESSION_SYNC_FAILED",
                    "reason=" + failure.getClass().getSimpleName());
            TrackingRuntimeTrace.event(
                    "CURRENT_SESSION_SYNC_FAILED",
                    Map.of(
                            "reason",
                            failure.getClass().getSimpleName(),
                            "action",
                            "SKIPPED"));
        }
    }

    static MiningSessionHistoryController.SaveResult
            saveSessionHistory() {
        return SESSION_HISTORY.saveCurrentStoppedSession();
    }

    private static void showPendingCurrentSessionPersistenceWarning(
            Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        String warning = CURRENT_SESSION.takePersistenceWarning();
        if (!warning.isBlank()) {
            client.player.sendSystemMessage(
                    Component.literal("Rot Client: " + warning));
        }
    }

    private static void showPendingStartNewRecoveryNotice(
            Minecraft client) {
        if (!startNewRecoveryNoticePending
                || client == null
                || client.player == null) {
            return;
        }
        startNewRecoveryNoticePending = false;
        String message = switch (START_NEW_RECOVERY_RESULT) {
            case COMPLETED_CURRENT_PUBLISH ->
                    "Recovered an interrupted Start New operation. The "
                            + "archived session and new Current Session are aligned.";
            case INVALID_JOURNAL ->
                    CurrentSessionStartNewJournal.INVALID_WARNING;
            case HISTORY_UNAVAILABLE ->
                    "Start New recovery is waiting because Session History "
                            + "is unavailable. Local files were left unchanged.";
            case CURRENT_PUBLISH_FAILED ->
                    "Start New recovery could not publish the next Current "
                            + "Session. The recovery marker was retained.";
            case CLEANUP_FAILED ->
                    "Start New completed, but its recovery marker could not "
                            + "be removed. Review the local config directory.";
            case NO_PENDING, CLEARED_BEFORE_ARCHIVE, ALREADY_COMMITTED -> "";
        };
        if (!message.isBlank()) {
            client.player.sendSystemMessage(
                    Component.literal("Rot Client: " + message));
        }
    }

    static MiningSessionHistoryController.OpenResult
            openSessionHistory(String indexOrId) {
        return SESSION_HISTORY.open(indexOrId);
    }

    static MiningSessionHistoryController.CopyResult
            copySessionHistory(String indexOrId) {
        return SESSION_HISTORY.copy(indexOrId);
    }

    static MiningSessionHistoryController.DeleteResult
            deleteSessionHistory(String indexOrId) {
        return SESSION_HISTORY.delete(indexOrId);
    }

    static MiningSessionHistoryController.ClearResult
            requestClearSessionHistory() {
        return SESSION_HISTORY.requestClear();
    }

    static MiningSessionHistoryController.ClearResult
            confirmClearSessionHistory() {
        String token = SESSION_HISTORY.pendingClearToken();
        return SESSION_HISTORY.confirmClear(token);
    }

    static void clearSessionHistorySelection() {
        SESSION_HISTORY.clearSelection();
    }

    static BazaarPriceService.MarketPrices currentMarketPrices() {
        return marketPrices;
    }

    static long marketPricesObservedAtMillis() {
        return marketPricesObservedAtMillis;
    }

    private static String formatShadowValuation(
            MiningSessionValuation valuation) {
        StringBuilder result = new StringBuilder();
        result.append("Price basis: ")
                .append(valuation.priceBasisLabel());

        if (valuation.hasResolvedValue()) {
            result.append("\nResolved item value: ")
                    .append(MiningSessionPriceBook.formatCoinAmount(
                            valuation.resolvedItemValue()))
                    .append(" coins");
        } else {
            result.append("\nResolved item value: unavailable");
        }

        result.append("\nResolved entries: ")
                .append(valuation.resolvedEntryCount())
                .append("\nUnresolved entries: ")
                .append(valuation.unresolvedEntryCount())
                .append("\nStale entries: ")
                .append(valuation.staleEntryCount())
                .append("\nUnavailable entries: ")
                .append(valuation.unavailableEntryCount())
                .append("\nUnsupported entries: ")
                .append(valuation.unsupportedEntryCount())
                .append("\nExcluded currency entries: ")
                .append(valuation.excludedCurrencyEntryCount());

        if (valuation.hasResolvedValue()) {
            result.append("\nTARGET_MINED value: ")
                    .append(MiningSessionPriceBook.formatCoinAmount(
                            valuation.valueByCategory(
                                    MiningSessionCategory.TARGET_MINED)))
                    .append("\nOTHER_MINED value: ")
                    .append(MiningSessionPriceBook.formatCoinAmount(
                            valuation.valueByCategory(
                                    MiningSessionCategory.OTHER_MINED)))
                    .append("\nCHEST_LOOT value: ")
                    .append(MiningSessionPriceBook.formatCoinAmount(
                            valuation.valueByCategory(
                                    MiningSessionCategory.CHEST_LOOT)));
        }

        if (valuation.priceBookObservedAtMillis().isPresent()) {
            long ageMillis = valuation.oldestQuoteAgeMillis().orElse(0L);
            result.append("\nPrice-book age: ")
                    .append(ageMillis / 1_000L)
                    .append('s');
        }

        return result.toString();
    }

    private static String formatShadowQuantities(
            Map<MiningSessionResource, Long> quantities) {
        if (quantities.isEmpty()) {
            return "none";
        }
        List<Map.Entry<MiningSessionResource, Long>> entries =
                new ArrayList<>(quantities.entrySet());
        entries.sort((left, right) -> left.getKey().resourceId()
                .compareTo(right.getKey().resourceId()));
        StringBuilder result = new StringBuilder();
        for (Map.Entry<MiningSessionResource, Long> entry : entries) {
            if (!result.isEmpty()) {
                result.append(", ");
            }
            result.append(entry.getKey().resourceId())
                    .append('=')
                    .append(entry.getValue());
        }
        return result.toString();
    }

    private static int trackingDebugOn(FabricClientCommandSource source) {
        try {
            Path path = TrackingRuntimeTrace.enable();
            source.sendFeedback(Component.literal(
                    "Tracking runtime trace ON"));
            source.sendFeedback(Component.literal(
                    "Runtime: " + path));
            source.sendFeedback(Component.literal(
                    "Summary will be written on /rot debug tracking off"));
            return 1;
        } catch (Exception ex) {
            source.sendFeedback(Component.literal(
                    "Could not enable tracking trace: " + ex.getMessage()));
            return 0;
        }
    }

    private static int trackingDebugOff(FabricClientCommandSource source) {
        TrackingRuntimeTrace.StopResult stop = TrackingRuntimeTrace.disable();
        if (stop.runtimeLog() == null && stop.summaryFile() == null) {
            source.sendFeedback(Component.literal(
                    "Tracking runtime trace was already OFF"));
            return 1;
        }
        source.sendFeedback(Component.literal("Tracking trace saved:"));
        if (stop.runtimeLog() != null) {
            source.sendFeedback(Component.literal(
                    String.valueOf(stop.runtimeLog())));
        }
        source.sendFeedback(Component.literal("Item summary saved:"));
        if (stop.summaryFile() != null) {
            source.sendFeedback(Component.literal(
                    String.valueOf(stop.summaryFile())));
        } else {
            source.sendFeedback(Component.literal(
                    "(summary write failed)"));
        }
        source.sendFeedback(Component.literal(
                "Unique items: " + stop.uniqueItems()
                        + " | bytes: " + stop.bytesWritten()
                        + " | truncated: " + stop.truncatedCount()));
        return 1;
    }

    private static int trackingDebugClear(FabricClientCommandSource source) {
        try {
            Path path = TrackingRuntimeTrace.clearAndRestart();
            source.sendFeedback(Component.literal(
                    "Tracking runtime trace cleared and ON"));
            source.sendFeedback(Component.literal("Runtime: " + path));
            return 1;
        } catch (Exception ex) {
            TrackingRuntimeTrace.clearStatusMemory();
            TrackingRuntimeTrace.disable();
            source.sendFeedback(Component.literal(
                    "Tracking status memory cleared; enable failed: "
                            + ex.getMessage()));
            return 0;
        }
    }

    private static int trackingDebugStatus(FabricClientCommandSource source) {
        for (String line : TrackingRuntimeTrace.statusReport().split("\n")) {
            source.sendFeedback(Component.literal(line));
        }
        return 1;
    }

    private static int startRecording(FabricClientCommandSource source) {
        try {
            if (DiagnosticRecorder.isRecording()) {
                source.sendFeedback(Component.literal(
                        "Diagnostic recording is already active."));
                return 0;
            }
            if (CURRENT_SESSION.isPaused()) {
                source.sendFeedback(Component.literal(
                        "Resume Current Session before starting diagnostic "
                                + "recording."));
                return 0;
            }

            TrackerSelection selection =
                    CONFIG.selectedSelection();

            ensureCurrentSessionCollection(System.currentTimeMillis());
            Path path =
                    DiagnosticRecorder.start();

            if (selection.isGemstone()) {
                GemstoneType gemstone =
                        selection.gemstone();

                GemstoneTrackerState state =
                        CONFIG.gemstoneState(gemstone);

                DiagnosticRecorder.record(
                        "SNAPSHOT",
                        "selection=gemstone"
                                + " gemstone="
                                + gemstone.id()
                                + " trackerEnabled="
                                + CONFIG.enabled
                                + " sessionItems="
                                + state.sessionLedger()
                                        .totalItemCount()
                                + " sessionRoughEquivalent="
                                + state.sessionLedger()
                                        .totalRoughEquivalent()
                                + " totalItems="
                                + state.totalLedger()
                                        .totalItemCount()
                                + " totalRoughEquivalent="
                                + state.totalLedger()
                                        .totalRoughEquivalent());
            } else {
                TrackedMaterial material =
                        selectedMaterial();

                MaterialTrackerState state =
                        CONFIG.state(material);

                DiagnosticRecorder.record(
                        "SNAPSHOT",
                        "selection=material"
                                + " material="
                                + material.id()
                                + " trackerEnabled="
                                + CONFIG.enabled
                                + " mf="
                                + CONFIG.miningFortune
                                + " of="
                                + CONFIG.oreFortune
                                + " dmf="
                                + CONFIG.dwarvenMetalFortune
                                + " blocks="
                                + state.sessionBlocks
                                + " actualEquivalent="
                                + state.sessionActualRawEquivalent
                                + " compacted="
                                + state.sessionCompactedEnchanted
                                + " sackRaw="
                                + state.sessionSackRaw
                                + " sackEnchanted="
                                + state.sessionSackEnchanted
                                + " tax="
                                + CONFIG.bazaarTaxPercent);
            }

            source.sendFeedback(
                    Component.literal(
                            "Rot Client diagnostic started: "
                                    + path));

            return 1;
        } catch (Exception exception) {
            source.sendFeedback(
                    Component.literal(
                            "Could not start diagnostic: "
                                    + exception.getMessage()));

            return 0;
        }
    }

    private static int stopRecording(FabricClientCommandSource source) {
        if (!DiagnosticRecorder.isRecording()) {
            source.sendFeedback(Component.literal(
                    "Rot Client diagnostic was not running."));
            return 0;
        }
        // Instrumentation only — do not tear down Current Session collection.
        Path path = DiagnosticRecorder.stop();
        long now = System.currentTimeMillis();
        ensureCurrentSessionCollection(now);
        if (path == null) {
            source.sendFeedback(Component.literal(
                    "Rot Client diagnostic was not running."));
            return 0;
        }
        source.sendFeedback(Component.literal(
                "Rot Client diagnostic saved: " + path));
        return 1;
    }

    static void save() {
        saveActiveSettingsProfile();
        TrackerStore.save(CONFIG);
    }

    private static void applyActiveSettingsProfileOnStartup() {
        RotClientProfile active =
                SETTINGS_PROFILES.activeProfile();

        if (active == null) {
            return;
        }

        RotClientProfileSettingsAdapter.apply(
                active.settings,
                CONFIG);

        /*
         * Keep rotclient.json aligned with the active profile immediately.
         * Do not call save() here, because startup must never capture the old
         * rotclient.json settings back into the profile before applying it.
         */
        TrackerStore.save(CONFIG);
    }

    private static void saveActiveSettingsProfile() {
        RotClientProfile active =
                SETTINGS_PROFILES.activeProfile();

        if (active == null) {
            return;
        }

        SETTINGS_PROFILES.captureCurrentSettings(
                active.id,
                CONFIG);
    }

    private static void applyMaterialPrices(
            BazaarPriceService.MarketPrices prices,
            long epochMillis) {
        if (prices == null
                || !isMaterialSelection()) {
            return;
        }
        for (TrackedMaterial material : TrackedMaterial.values()) {
            BazaarPriceService.MaterialPrices materialPrices =
                    prices.forMaterial(material);
            if (materialPrices == null) continue;
            MaterialTrackerState state = CONFIG.state(material);
            boolean updated = false;
            if (materialPrices.raw() != null) {
                state.lastRawPrice =
                        materialPrices.raw().instantSellPrice();
                updated = true;
            }
            if (materialPrices.enchanted() != null) {
                state.lastEnchantedPrice =
                        materialPrices.enchanted().instantSellPrice();
                updated = true;
            }
            if (updated) {
                state.lastPriceUpdateEpochMillis = epochMillis;
            }
        }
    }

    static boolean isMaterialSelection() {
        return CONFIG.hasMaterialSelection();
    }

    static TrackingTarget selectedTarget() {
        return CONFIG.requireMaterialTarget();
    }

    static List<TrackedMaterial> selectedMaterials() {
        return CONFIG.routedMaterials();
    }

    static TrackedMaterial selectedMaterial() {
        return CONFIG.requireSelectedMaterial();
    }

    static boolean tracksMaterial(TrackedMaterial material) {
        return CONFIG.routesMaterial(material);
    }

    static MaterialTrackerState selectedState() {
        return CONFIG.state(selectedMaterial());
    }

    static long currentSessionActiveMillis(long now) {
        if (selectedTarget().isCombined()) {
            if (CONFIG.combinedLastBreakEpochMillis <= 0) {
                return CONFIG.combinedSessionActiveMillis;
            }
            return CONFIG.combinedSessionActiveMillis
                    + Math.min(
                            PAUSE_AFTER_MILLIS,
                            Math.max(
                                    0,
                                    now - CONFIG.combinedLastBreakEpochMillis));
        }
        return currentSessionActiveMillis(selectedMaterial(), now);
    }

    static long currentSessionActiveMillis(
            TrackedMaterial material, long now) {
        MaterialTrackerState state = CONFIG.state(material);
        if (state.lastBreakEpochMillis <= 0) return state.sessionActiveMillis;
        return state.sessionActiveMillis
                + Math.min(
                        PAUSE_AFTER_MILLIS,
                        Math.max(0, now - state.lastBreakEpochMillis));
    }

    static long currentTotalActiveMillis(long now) {
        if (selectedTarget().isCombined()) {
            if (CONFIG.combinedLastBreakEpochMillis <= 0) {
                return CONFIG.combinedTotalActiveMillis;
            }
            return CONFIG.combinedTotalActiveMillis
                    + Math.min(
                            PAUSE_AFTER_MILLIS,
                            Math.max(
                                    0,
                                    now - CONFIG.combinedLastBreakEpochMillis));
        }
        MaterialTrackerState state = selectedState();
        if (state.lastBreakEpochMillis <= 0) return state.totalActiveMillis;
        return state.totalActiveMillis
                + Math.min(
                        PAUSE_AFTER_MILLIS,
                        Math.max(0, now - state.lastBreakEpochMillis));
    }

    static boolean isActive(long now) {
        if (selectedTarget().isCombined()) {
            return CONFIG.combinedLastBreakEpochMillis > 0
                    && now - CONFIG.combinedLastBreakEpochMillis
                    < PAUSE_AFTER_MILLIS;
        }
        return isActive(selectedMaterial(), now);
    }

    static long lastSelectedBreakEpochMillis() {
        return selectedTarget().isCombined()
                ? CONFIG.combinedLastBreakEpochMillis
                : selectedState().lastBreakEpochMillis;
    }

    static boolean isActive(TrackedMaterial material, long now) {
        MaterialTrackerState state = CONFIG.state(material);
        return state.lastBreakEpochMillis > 0
                && now - state.lastBreakEpochMillis < PAUSE_AFTER_MILLIS;
    }

    static boolean isExpecting(TrackedMaterial material, long now) {
        if (!CONFIG.enabled || !tracksMaterial(material)) return false;
        MaterialTrackerState state = CONFIG.state(material);
        return state.lastBreakEpochMillis > 0
                && now - state.lastBreakEpochMillis
                < RESOURCE_OBSERVATION_WINDOW_MILLIS;
    }

    static long sessionBlocks(TrackedMaterial material) {
        return CONFIG.state(material).sessionBlocks;
    }

    static long selectedSessionBlocks() {
        long total = 0;
        for (TrackedMaterial material : selectedMaterials()) {
            total = safeAdd(total, CONFIG.state(material).sessionBlocks);
        }
        return total;
    }

    static long selectedSessionBaseDrops() {
        long total = 0;
        for (TrackedMaterial material : selectedMaterials()) {
            total = safeAdd(total, CONFIG.state(material).sessionBaseDrops);
        }
        return total;
    }

    static long selectedSessionActualRawEquivalent() {
        long total = 0;
        for (TrackedMaterial material : selectedMaterials()) {
            total = safeAdd(
                    total,
                    CONFIG.state(material).sessionActualRawEquivalent);
        }
        return total;
    }

    static long currentSessionEnchantedItems() {
        return currentSessionEnchantedItems(selectedMaterial());
    }

    static long currentSessionEnchantedItems(TrackedMaterial material) {
        MaterialTrackerState state = CONFIG.state(material);
        return Math.max(
                state.sessionCompactedEnchanted
                        + state.sessionCompactBonusEnchanted,
                state.sessionSackEnchanted);
    }

    static long currentSessionRawItems() {
        return currentSessionRawItems(selectedMaterial());
    }

    static long currentSessionRawItems(TrackedMaterial material) {
        MaterialTrackerState state = CONFIG.state(material);
        return Math.max(
                0,
                state.sessionActualRawEquivalent
                        - currentSessionEnchantedItems(material)
                        * material.rawPerEnchanted());
    }

    static long currentSessionUnsoldEnchantedItems(
            TrackedMaterial material) {
        MaterialTrackerState state = CONFIG.state(material);
        return SessionAccounting.unsold(
                currentSessionEnchantedItems(material),
                state.sessionSoldEnchanted);
    }

    static long currentSessionUnsoldRawItems(TrackedMaterial material) {
        MaterialTrackerState state = CONFIG.state(material);
        return SessionAccounting.unsold(
                currentSessionRawItems(material),
                state.sessionSoldRaw);
    }

    static double currentSessionProfit() {
        double total = 0;
        for (TrackedMaterial material : selectedMaterials()) {
            total += currentSessionProfit(material);
        }
        return total;
    }

    static double currentSessionProfit(TrackedMaterial material) {
        MaterialTrackerState state = CONFIG.state(material);
        return applyBazaarTax(state.sessionRealizedGrossCoins)
                + currentSessionUnsoldProfit(material);
    }

    static double currentSessionUnsoldProfit() {
        double total = 0;
        for (TrackedMaterial material : selectedMaterials()) {
            total += currentSessionUnsoldProfit(material);
        }
        return total;
    }

    static double currentSessionUnsoldProfit(TrackedMaterial material) {
        return estimateNetValue(
                material,
                currentSessionUnsoldRawItems(material),
                currentSessionUnsoldEnchantedItems(material));
    }

    static double estimateNetValue(
            TrackedMaterial material,
            long rawItems,
            long enchantedItems) {
        return applyBazaarTax(quoteInstantSellGross(
                material, rawItems, enchantedItems));
    }

    private static double quoteInstantSellGross(
            TrackedMaterial material,
            long rawItems,
            long enchantedItems) {
        BazaarPriceService.MarketPrices prices = marketPrices;
        BazaarPriceService.MaterialPrices materialPrices =
                prices == null ? null : prices.forMaterial(material);
        if (materialPrices != null) {
            MaterialTrackerState state = CONFIG.state(material);
            double rawPrice = materialPrices.raw() == null
                    ? state.lastRawPrice
                    : materialPrices.raw().instantSellPrice();
            double enchantedPrice = materialPrices.enchanted() == null
                    ? state.lastEnchantedPrice
                    : materialPrices.enchanted().instantSellPrice();
            return SessionAccounting.marketGross(
                    rawItems,
                    enchantedItems,
                    rawPrice,
                    enchantedPrice);
        }
        MaterialTrackerState state = CONFIG.state(material);
        return SessionAccounting.marketGross(
                rawItems,
                enchantedItems,
                state.lastRawPrice,
                state.lastEnchantedPrice);
    }

    private static double applyBazaarTax(double gross) {
        return SessionAccounting.afterTax(gross, CONFIG.bazaarTaxPercent);
    }

    static double effectiveFortune(TrackedMaterial material) {
        return Math.max(
                0,
                CONFIG.miningFortune + categoryFortune(material));
    }

    private static double categoryFortune(TrackedMaterial material) {
        return switch (material.fortuneType()) {
            case BLOCK -> CONFIG.blockFortune;
            case ORE -> CONFIG.oreFortune;
            case DWARVEN_METAL -> CONFIG.dwarvenMetalFortune;
            case GEMSTONE -> CONFIG.gemstoneFortune;
            case NONE, UNKNOWN -> 0.0;
        };
    }

    private static void persistActiveTime(
            TrackedMaterial material, long now) {
        MaterialTrackerState state = CONFIG.state(material);
        if (state.lastBreakEpochMillis <= 0) return;
        long addition = Math.min(
                PAUSE_AFTER_MILLIS,
                Math.max(0, now - state.lastBreakEpochMillis));
        state.sessionActiveMillis += addition;
        state.totalActiveMillis += addition;
        state.lastBreakEpochMillis =
                isActive(material, now) ? now : 0;
    }

    private static void persistCombinedActiveTime(long now) {
        if (CONFIG.combinedLastBreakEpochMillis <= 0) return;
        long addition = Math.min(
                PAUSE_AFTER_MILLIS,
                Math.max(0, now - CONFIG.combinedLastBreakEpochMillis));
        CONFIG.combinedSessionActiveMillis = safeAdd(
                CONFIG.combinedSessionActiveMillis, addition);
        CONFIG.combinedTotalActiveMillis = safeAdd(
                CONFIG.combinedTotalActiveMillis, addition);
        CONFIG.combinedLastBreakEpochMillis =
                now - CONFIG.combinedLastBreakEpochMillis
                        < PAUSE_AFTER_MILLIS
                        ? now
                        : 0;
    }

    private static void recomputeActualResource(
            TrackedMaterial material,
            String source,
            MiningSessionObservation.EvidenceType evidence) {
        MaterialTrackerState state = CONFIG.state(material);
        SessionAccounting.ResourceReconciliation reconciliation =
                SessionAccounting.reconcileResource(
                        state.sessionInventoryRaw,
                        state.sessionCompactedEnchanted,
                        state.sessionCompactBonusEnchanted,
                        state.sessionSackRaw,
                        state.sessionSackEnchanted,
                        material.rawPerEnchanted());
        long updated = reconciliation.rawEquivalent();
        long delta = updated - state.sessionActualRawEquivalent;

        state.sessionActualRawEquivalent = updated;
        state.totalActualRawEquivalent =
                Math.max(0, state.totalActualRawEquivalent + delta);
        long observedAtMillis = System.currentTimeMillis();
        state.lastActualRawEquivalentEpochMillis = observedAtMillis;
        state.actualRawEquivalentSource = source;
        DiagnosticRecorder.record("RESOURCE_TOTAL",
                "material=" + material.id()
                        + " sessionActual=" + updated
                        + " inventoryRaw=" + state.sessionInventoryRaw
                        + " sackRaw=" + state.sessionSackRaw
                        + " reconciledRaw=" + reconciliation.rawItems()
                        + " sackEnchanted=" + state.sessionSackEnchanted
                        + " reconciledEnchanted="
                        + reconciliation.enchantedItems()
                        + " compacted="
                        + state.sessionCompactedEnchanted
                        + " compactBonus="
                        + state.sessionCompactBonusEnchanted
                        + " delta=" + delta);
        if (delta > 0L
                && SESSION_ENGINE.isObservationEnabled()) {
            SESSION_ENGINE.onAcceptedTargetMaterialQuantity(
                    material,
                    delta,
                    evidence,
                    updated,
                    observedAtMillis,
                    "target-material-quantity:"
                            + material.id()
                            + ":" + observedAtMillis
                            + ":" + updated,
                    source);
            if (TargetItemGainPipeline.creditMaterial(
                    CURRENT_SESSION,
                    material,
                    delta,
                    SkyBlockAreaDetector.detect(),
                    observedAtMillis)) {
                cachedHudOtherSummary = null;
            }
        }
    }

    private static long safeAdd(long left, long right) {
        if (right <= 0) return Math.max(0, left);
        if (left >= Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return Math.max(0, left) + right;
    }

    private static long safeMultiply(long left, long right) {
        if (left <= 0 || right <= 0) return 0;
        if (left > Long.MAX_VALUE / right) return Long.MAX_VALUE;
        return left * right;
    }

    private static Map<TrackedMaterial, MiningBreakDetector>
    createBreakDetectors() {
        Map<TrackedMaterial, MiningBreakDetector> detectors =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : TrackedMaterial.values()) {
            detectors.put(material, new MiningBreakDetector(material));
        }
        return detectors;
    }

    private static Map<TrackedMaterial, MiningGainDetector>
    createGainDetectors() {
        Map<TrackedMaterial, MiningGainDetector> detectors =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : TrackedMaterial.values()) {
            detectors.put(material, new MiningGainDetector(material));
        }
        return detectors;
    }

    private static Map<TrackedMaterial, SaleObservationGate>
    createSaleGates() {
        Map<TrackedMaterial, SaleObservationGate> gates =
                new EnumMap<>(TrackedMaterial.class);
        for (TrackedMaterial material : TrackedMaterial.values()) {
            gates.put(material, new SaleObservationGate());
        }
        return gates;
    }
}
