package fi.rotclient;

import fi.rotclient.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Minecraft bridge for {@link DungeonPolicy} and {@link DungeonAssistPolicy}:
 * HUD, ESP, party announces, Spirit Leap tints, F7 terminals, puzzle solvers,
 * salvage/party-finder/chest menus and Extra Stats requeue.
 */
public final class DungeonRuntime {
    static DungeonPolicy.Sidebar sidebar =
            DungeonPolicy.parseSidebar(List.of());
    static long bonzoUntil;
    static long spiritUntil;
    static long phoenixUntil;
    static long bonzoCdUntil;
    static long spiritCdUntil;
    static long phoenixCdUntil;
    static long terracottaUntil;
    static DungeonAssistPolicy.F7Timer f7Timer = DungeonAssistPolicy.F7Timer.NONE;
    static long f7TimerUntil;
    static int requeueTicks = -1;
    static int terminalCooldown;
    static int simonCooldown;
    static int puzzleScanTicks;
    static String lastAnnounce = "";
    static String lastF7Title = "";
    static String lastQuiz = "";
    static List<String> lastQuizAnswers = List.of();
    static int lastQuizOption = -1;
    static String lastRagnarock = "";
    static String lastMelody = "";
    static String lastWeirdoNpc = "";
    static int lastBreakerCharges = -1;
    static final Map<String, String> blessings = new LinkedHashMap<>();
    static final Map<String, String> tpLinks = new LinkedHashMap<>();
    static final List<Mark> puzzleMarks = new ArrayList<>();
    static DungeonPuzzlePolicy.MapPreview mapPreview =
            new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
    static DungeonPuzzlePolicy.WorldCell lastPad;
    static Vec3 lastPlayerPos;
    static int lastScore = -1;
    static int lastSecretsFound = -1;
    static String lastDuplicateTitle = "";
    static String lastPlayerCountTitle = "";
    static String locationHud = "";
    static int locationTicks;
    static long predevBossEnter;
    static boolean predevAtThird;
    static boolean predevTracking;
    static long predevDoneMs;
    static int runChests;
    static boolean chestRunCounted;
    static boolean chestWarned;
    static String lastLivid = "";
    static long lividUntil;
    static boolean bloodCampActive;
    static String lastLeapRegion = "";
    static final Set<Integer> leapedIds = new HashSet<>();
    static EmberDungeonPolicy.DebuffPhase debuffPhase = EmberDungeonPolicy.DebuffPhase.NONE;
    static long debuffUntil;
    static boolean debuffFired;
    static long puzzleStarted;
    static long warpCooldownUntil;
    static int quizTicks;
    static int quizStage;
    static int maxorStunTicks;
    static int stormCrushTicks;
    static int secretSpawnTicks;
    static int stormLbTicks;
    static boolean stormLbActive;
    static int termStartTicks;
    static String explosiveShotLine = "";
    static long explosiveShotUntil;
    static int unclaimedChests = -1;
    static DungeonExtraStatsPolicy.Snapshot extraStats =
            DungeonExtraStatsPolicy.Snapshot.idle();
    static int extraStatsQuietTicks;
    static String lastMelodyProgress = "";
    static String lastP3Objective = "";
    static int lastP3Completed;
    static int lastP3Total = 7;
    static final List<DungeonRoomDataPolicy.PlacedWaypoint> secretWaypoints = new ArrayList<>();
    static final Map<Long, List<DungeonRoomDataPolicy.PlacedWaypoint>> hashedRoomSecrets =
            new LinkedHashMap<>();
    static final Map<Long, DungeonRoomDataPolicy.Rotation> hashedRoomRotation =
            new LinkedHashMap<>();
    static final Set<Long> hashedRoomTried = new HashSet<>();
    static final Map<String, DungeonMapPolicy.RoomIdentity> hashedTileIdentity =
            new LinkedHashMap<>();
    static final Map<String, DungeonRoomDataPolicy.RoomMeta> hashedTileRoom =
            new LinkedHashMap<>();
    static final Map<String, Integer> secretsFoundByRoom = new LinkedHashMap<>();
    static final Set<String> collectedSecrets = new HashSet<>();
    static final Set<String> seenSecretEntities = new HashSet<>();
    static final List<ClickedSecret> clickedSecrets = new ArrayList<>();
    static final Set<Integer> seenKeyStands = new HashSet<>();
    static TempleDungeonPolicy.KeySkull droppedKey = TempleDungeonPolicy.KeySkull.NONE;
    static boolean mimicKilled;
    static boolean puzzleFailed;
    static boolean extraStatsSeen;
    static boolean dungeonRunStarted;
    static int dungeonWorldTicks;
    static String lastRelic = "";
    static long leapHideAtMs;
    static String melodyOtherName = "";
    static int melodyOtherPercent;
    static boolean melodyOtherOwn;
    static final Set<String> melodyOtherNames = new HashSet<>();
    static final List<EmberDungeonPolicy.ArrowClicks> arrowClicks = new ArrayList<>();
    static int i4Cooldown;
    static int i4Timer = -1;
    static boolean i4RodUsed;
    static boolean i4MaskUsed;
    static boolean i4LeapUsed;
    static int i4LeapWait;
    static String melodyLeapName = "";
    static long relicLookStart;
    static DungeonF7Policy.LookAim relicLookFrom;
    static DungeonF7Policy.LookAim relicLookTo;
    static long i4LookStart;
    static DungeonF7Policy.LookAim i4LookFrom;
    static BlockPos i4LookTarget;
    static final List<DungeonPolicy.TerminalClick> melodySkipQueue = new ArrayList<>();
    static DungeonMapPolicy.Board lastMapBoard =
            new DungeonMapPolicy.Board(DungeonMapPolicy.Calibration.none(), List.of(), List.of(), List.of(), "");
    static final java.util.Queue<DungeonLeftoverPolicy.QueuedClick> termQueue =
            DungeonLeftoverPolicy.newQueue();
    static DungeonLeftoverPolicy.SplitSnapshot splits =
            new DungeonLeftoverPolicy.SplitSnapshot(0L, 0L, 0L, 0L, false, false, false);
    static KuudraSplitPolicy.Snapshot kuudra = KuudraSplitPolicy.Snapshot.idle();
    static List<String> chestProfitHud = List.of();
    static int superboomCooldown;
    static int superboomOriginalSlot = -1;
    static int superboomSwapBackTicks = -1;
    static long triggerLastMs;
    static int ghostCooldown;
    static long crystalSpawnUntil;
    static long crystalPickupAt;
    static String lastCrystalHud = "";
    static long relicSpawnUntil;
    static long relicPickupAt;
    static DungeonF7Policy.SimonState simon = DungeonF7Policy.SimonState.idle();
    static String lastDragonHud = "";
    static boolean melodyWasOpen;
    static int lastTerminalSlot = 22;
    static String lastTermTitle = "";
    static long terminalOpenedAt;
    static final List<Integer> terminalPredictedSlots = new ArrayList<>();
    static long terminalPredictedAt;
    static boolean closeChestArmed;
    static int p3Terminals;
    static int p3Devices;
    static int p3Levers;
    static long dragonSpawnUntil;
    static final Set<String> dragonsDown = new HashSet<>();
    static boolean terminalHadClicks;
    static int goldorFrenzyTicks;
    static int purplePadTicks;
    static boolean autoClickedThisTick;
    static boolean superboomAttackHeld;
    static int closeChestWait = -1;
    static long termQueueUpdatedAt;
    static long terminalClickUntil;
    static String closeChestTitle = "";
    static DungeonGoldorPolicy.ShooterState sharpShooter =
            DungeonGoldorPolicy.ShooterState.idle();
    static DungeonGoldorPolicy.TermTimesState termTimes =
            DungeonGoldorPolicy.TermTimesState.idle();
    static final DungeonGoldorPolicy.PositionTracker positionCallouts =
            new DungeonGoldorPolicy.PositionTracker();
    static String lastTermTimeLine = "";
    static String lastTermTotalLine = "";

    private DungeonRuntime() {
    }

    static DungeonPolicy.Sidebar sidebar() {
        return sidebar;
    }

    static boolean autoClickedThisTick() {
        return autoClickedThisTick;
    }

    static List<DungeonPolicy.TerminalItem> terminalSnapshot(AbstractContainerScreen<?> screen) {
        return snapshot(screen);
    }

    static String lookedBlockId(Minecraft client, BlockPos pos) {
        return blockId(client, pos);
    }

    static void clickSolved(AbstractContainerScreen<?> screen, DungeonPolicy.TerminalClick click) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || screen == null || click == null) {
            return;
        }
        sendTerminalClick(client, screen, extras(), click);
    }

    public static boolean shouldHideTerminalHeader(AbstractContainerScreen<?> screen) {
        if (screen == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        DungeonAthenSettings athen = extras.athen();
        if (!extras.dungeonTerminalsEnabled || (!athen.termHideHeader && !athen.termHideTitle)) {
            return false;
        }
        return DungeonPolicy.detectTerminal(titleOf(screen)) != DungeonPolicy.Terminal.NONE;
    }

    static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        QolSkyblockExtras extras = extras();
        autoClickedThisTick = false;
        dungeonWorldTicks++;
        List<String> lines = sidebarLines();
        sidebar = DungeonPolicy.parseSidebar(lines);
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceScoreTitle
                && DungeonPolicy.crossedScoreMilestone(
                        lastScore, sidebar.score(), extras.dungeonAnnounceScoreThreshold)) {
            showTitle(client, true, "§e" + DungeonAssistPolicy.scoreTitleText(
                    sidebar.score(), extras.dungeonAnnounceScoreThreshold));
        }
        lastScore = sidebar.score();
        noteDungeonRunProgress(client);
        maybeFinishExtraStats(client);
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceSecretChime
                && SkyBlockDungeonDetector.confidentlyInDungeon()
                && DungeonPolicy.secretCountIncreased(lastSecretsFound, sidebar.secretsFound())
                && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.4F);
        }
        if (SkyBlockDungeonDetector.confidentlyInDungeon()) {
            if (DungeonPolicy.secretCountIncreased(lastSecretsFound, sidebar.secretsFound())) {
                int previous = Math.max(0, lastSecretsFound);
                creditRoomSecrets(client, sidebar.secretsFound() - previous);
            }
            lastSecretsFound = sidebar.secretsFound();
        } else {
            lastSecretsFound = -1;
            lastDuplicateTitle = "";
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceDuplicateClass
                && SkyBlockDungeonDetector.confidentlyInDungeon()) {
            List<String> rosterLines = new ArrayList<>(lines);
            rosterLines.addAll(CommissionDisplayRuntime.tabLines(client));
            String title = EmberDungeonPolicy.duplicateClassTitle(
                    EmberDungeonPolicy.duplicateClasses(
                            EmberDungeonPolicy.teammateClasses(rosterLines)));
            if (!title.isEmpty() && !title.equals(lastDuplicateTitle)) {
                lastDuplicateTitle = title;
                showTitle(client, true, "§c" + title);
            } else if (title.isEmpty()) {
                lastDuplicateTitle = "";
            }
        }
        if (!SkyBlockDungeonDetector.confidentlyInDungeon()) {
            lastScore = -1;
            lastPlayerCountTitle = "";
        }
        long now = System.currentTimeMillis();
        if (terracottaUntil > 0L && now >= terracottaUntil) {
            terracottaUntil = 0L;
        }
        if (f7TimerUntil > 0L && now >= f7TimerUntil) {
            f7TimerUntil = 0L;
            f7Timer = DungeonAssistPolicy.F7Timer.NONE;
        }
        if (requeueTicks > 0) {
            requeueTicks--;
        } else if (requeueTicks == 0) {
            requeueTicks = -1;
            if (extras.dungeonRequeueEnabled && client.player.connection != null) {
                client.player.connection.sendCommand("instancerequeue");
            }
        }
        if (terminalCooldown > 0) {
            terminalCooldown--;
        }
        if (simonCooldown > 0) {
            simonCooldown--;
        }
        if (i4Cooldown > 0) {
            i4Cooldown--;
        }
        if (superboomCooldown > 0) {
            superboomCooldown--;
        }
        if (ghostCooldown > 0) {
            ghostCooldown--;
        }
        if (superboomSwapBackTicks > 0) {
            superboomSwapBackTicks--;
        } else if (superboomSwapBackTicks == 0) {
            superboomSwapBackTicks = -1;
            if (superboomOriginalSlot >= 0 && extras.dungeonF7SuperboomSwapBack) {
                client.player.getInventory().setSelectedSlot(superboomOriginalSlot);
            }
            superboomOriginalSlot = -1;
        }
        if (lividUntil > 0L && now >= lividUntil) {
            lividUntil = 0L;
        }
        if (debuffUntil > 0L && now >= debuffUntil) {
            debuffUntil = 0L;
            debuffPhase = EmberDungeonPolicy.DebuffPhase.NONE;
            debuffFired = false;
        }
        lastBreakerCharges = DungeonPolicy.breakerCharges(
                InventoryChromeRuntime.loreLines(client.player.getMainHandItem())).orElse(-1);
        observeTerminalOpen(client, extras);
        if (goldorFrenzyTicks > 0) {
            goldorFrenzyTicks--;
        }
        if (purplePadTicks > 0) {
            purplePadTicks--;
        }
        if (quizTicks > 0) {
            quizTicks--;
        }
        if (maxorStunTicks > 0) {
            maxorStunTicks--;
        }
        if (stormCrushTicks > 0) {
            stormCrushTicks--;
        }
        if (secretSpawnTicks > 0) {
            secretSpawnTicks--;
        }
        if (locationTicks > 0) {
            locationTicks--;
        } else {
            locationHud = "";
        }
        if (termStartTicks > 0) {
            termStartTicks--;
        }
        if (stormLbActive) {
            stormLbTicks++;
        }
        observeTabHud(client, extras);
        pruneClickedSecrets();
        scanKeyDrops(client, extras);
        refreshChestProfitHud(client, extras);
        HateDoorsRuntime.scan(client, extras);
        scanDungeonMap(client, extras);
        if (extras.dungeonF7Enabled && extras.dungeonF7Simon) {
            scanSimon(client);
        }
        if (puzzleScanTicks++ % 8 == 0) {
            scanPuzzles(client, extras);
            if (extras.dungeonF7Enabled && extras.dungeonF7ArrowAlign) {
                scanArrowAlign(client);
            }
        }
        DungeonWatcherRuntime.tick(client);
        DungeonPartyJoinRuntime.tick(client);
        DungeonCarryRuntime.tick(client);
        updateDragonHud(client, extras);
        observeTeleport(client.player);
        observeLeaps(client, extras);
        updateCrystalHud(client, extras, now);
        tickGoldorHelpers(client, extras, now);
    }




    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        String raw = message.getString();
        QolSkyblockExtras extras = extras();
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        DungeonCarryRuntime.onChat(raw);
        DungeonWatcherRuntime.onChat(raw, client);
        DungeonPartyJoinRuntime.onChat(raw, client);
        if (DungeonBladePolicy.dungeonEnterChat(raw)) {
            warpCooldownUntil = now + DungeonBladePolicy.WARP_COOLDOWN_MS;
        }
        DungeonBladePolicy.quizTimerTicks(raw).ifPresent(ticks -> {
            quizTicks = ticks;
            int stage = DungeonBladePolicy.quizStage(raw);
            if (stage > 0) {
                quizStage = stage;
            }
        });
        if (DungeonBladePolicy.maxorStunStart(raw)) {
            maxorStunTicks = DungeonBladePolicy.MAXOR_STUN_TICKS;
        }
        if (DungeonBladePolicy.maxorEnraged(raw)) {
            maxorStunTicks = 0;
        }
        if (DungeonBladePolicy.stormCrushChat(raw)) {
            stormCrushTicks = DungeonBladePolicy.STORM_CRUSH_TICKS;
            if (extras.dungeonF7Enabled && extras.dungeonF7StormCrush) {
                showTitle(client, true, "§6Storm crushed!");
            }
        }
        DungeonBladePolicy.explosiveShotPerEnemy(raw).ifPresent(perEnemy -> {
            explosiveShotLine = DungeonBladePolicy.explosiveShotHudLine(perEnemy);
            explosiveShotUntil = now + 8_000L;
            if (extras.dungeonHudEnabled && extras.dungeonHudExplosiveShot
                    && client != null && client.player != null && !explosiveShotLine.isBlank()) {
                client.player.sendSystemMessage(Component.literal("§a" + explosiveShotLine));
            }
        });
        if (extras.dungeonHudEnabled && extras.dungeonHudExtraStats) {
            extraStats = DungeonExtraStatsPolicy.apply(extraStats, raw);
            if (DungeonExtraStatsPolicy.extraStatsLine(raw)) {
                extraStatsQuietTicks = 0;
            }
            maybePrintExtraStats(client);
        }
        DungeonAssistPolicy.F7Timer phaseTimer = DungeonAssistPolicy.f7TimerFromChat(raw);
        if (phaseTimer == DungeonAssistPolicy.F7Timer.MAXOR_START
                && DungeonBladePolicy.shouldTrackPredev(
                        extras.dungeonF7Enabled && extras.dungeonF7Predev,
                        extras.dungeonF7PredevAll,
                        sidebar.dungeonClass() == DungeonPolicy.DungeonClass.HEALER)) {
            predevBossEnter = now;
            predevAtThird = false;
            predevTracking = true;
            predevDoneMs = 0L;
        } else if (phaseTimer == DungeonAssistPolicy.F7Timer.GOLDOR) {
            predevTracking = false;
        }
        if (predevTracking
                && predevAtThird
                && DungeonBladePolicy.ownLeapChat(raw)
                && DungeonBladePolicy.shouldTrackPredev(
                        extras.dungeonF7Enabled && extras.dungeonF7Predev,
                        extras.dungeonF7PredevAll,
                        sidebar.dungeonClass() == DungeonPolicy.DungeonClass.HEALER)) {
            predevDoneMs = Math.max(0L, now - predevBossEnter);
            predevTracking = false;
            predevAtThird = false;
            if (predevDoneMs > 0L
                    && (extras.dungeonF7PredevPbMs <= 0L || predevDoneMs < extras.dungeonF7PredevPbMs)) {
                extras.dungeonF7PredevPbMs = predevDoneMs;
                TrackerStore.save(RotClientClient.trackerConfig());
            }
            if (client != null && client.player != null) {
                client.player.sendSystemMessage(Component.literal("§aPredev completed in "
                        + String.format(Locale.ROOT, "%.2fs", predevDoneMs / 1000.0D)));
            }
        }
        if (!stormLbActive
                && (phaseTimer == DungeonAssistPolicy.F7Timer.STORM_START
                || phaseTimer == DungeonAssistPolicy.F7Timer.STORM_PAD)) {
            stormLbActive = true;
            stormLbTicks = 0;
        }
        if (DungeonF7Policy.stormDeath(raw)) {
            stormLbActive = false;
            termStartTicks = DungeonBladePolicy.TERM_START_TICKS;
        }
        DungeonBladePolicy.deathPlayer(raw).ifPresent(name -> {
            if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceDeath) {
                String text = DungeonBladePolicy.deathPartyMessage(extras.dungeonAnnounceDeathMessage, name);
                sendParty(client, text);
            }
        });
        DungeonPolicy.Invincibility inv = DungeonPolicy.invincibilityFromChat(raw);
        if (inv != DungeonPolicy.Invincibility.NONE) {
            long until = now + DungeonPolicy.invincibilityMillis(inv);
            long cooldownUntil = now + DungeonPolicy.cooldownMillis(inv);
            switch (inv) {
                case BONZO -> {
                    bonzoUntil = until;
                    bonzoCdUntil = cooldownUntil;
                }
                case SPIRIT -> {
                    spiritUntil = until;
                    spiritCdUntil = cooldownUntil;
                }
                case PHOENIX -> {
                    phoenixUntil = until;
                    phoenixCdUntil = cooldownUntil;
                }
                default -> {
                }
            }
        }
        if (DungeonPolicy.isTerracottaStart(raw)) {
            terracottaUntil = now + DungeonPolicy.TERRACOTTA_MILLIS;
        }
        if (DungeonPolicy.isTerracottaEnd(raw)) {
            terracottaUntil = 0L;
        }
        if (extras.dungeonLeapEnabled && extras.dungeonLeapAnnounce) {
            DungeonAssistPolicy.leapTarget(raw).ifPresent(name -> {
                String text = DungeonAssistPolicy.leapAnnounce(extras.dungeonLeapMessage, name);
                if (client != null && client.player != null && !text.isBlank()) {
                    client.player.sendSystemMessage(Component.literal("§d" + text));
                }
            });
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspDoors) {
            TempleDungeonPolicy.doorKeyTitle(TempleDungeonPolicy.doorKeyEvent(raw))
                    .ifPresent(title -> showTitle(client, true, "§e" + title));
        }
        noteDungeonRunStart(raw);
        if (DungeonPolicy.shouldArmRequeue(
                extras.dungeonRequeueEnabled,
                dungeonRunStarted,
                extraStatsSeen,
                dungeonWorldTicks,
                raw)) {
            extraStatsSeen = true;
            requeueTicks = Math.max(0, extras.dungeonRequeueDelay);
        }
        if (!chestRunCounted && DungeonPolicy.isDungeonEnd(raw)) {
            chestRunCounted = true;
            runChests++;
            if (extras.dungeonHudEnabled && extras.dungeonHudChestWarning
                    && !chestWarned
                    && DungeonBladePolicy.chestWarningReached(
                            unclaimedChests, runChests, extras.dungeonHudChestWarningCount)) {
                chestWarned = true;
                showTitle(client, true, "§c" + DungeonBladePolicy.CHEST_WARNING_TITLE);
                if (client != null && client.player != null) {
                    client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 1.0F);
                }
            }
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnouncePlayerCount
                && DungeonBladePolicy.startingCountdown(raw)
                && DungeonBladePolicy.notEnoughPlayers(
                        DungeonBladePolicy.scoreboardClassCount(sidebarLines()))) {
            String title = DungeonBladePolicy.PLAYER_COUNT_TITLE;
            if (!title.equals(lastPlayerCountTitle)) {
                lastPlayerCountTitle = title;
                showTitle(client, true, "§c" + title);
                if (client != null && client.player != null) {
                    client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 1.0F);
                }
            }
        }
        if (DungeonBladePolicy.ownLeapChat(raw)) {
            leapHideAtMs = now;
        }
        noteMelodyParty(raw, extras, client);
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceLocation) {
            DungeonBladePolicy.partyLocation(raw).ifPresent(notice -> {
                if (notice.username().equalsIgnoreCase(localName(client))) {
                    return;
                }
                if (!notice.hud().isBlank() && notice.hud().equals(lastAnnounce)) {
                    return;
                }
                locationHud = notice.hud();
                locationTicks = DungeonBladePolicy.LOCATION_TICKS;
                showTitle(client, true, "§e" + notice.hud());
                if (client != null && client.player != null) {
                    client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
                }
            });
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceBlood
                && DungeonPolicy.isBloodCampReady(raw)
                && client != null && client.gui != null) {
            bloodCampActive = true;
            client.gui.hud.setTitle(Component.literal("§cBlood Camp Ready"));
        }
        if (EmberDungeonPolicy.isLividStart(raw)) {
            lastLivid = "Livid invuln";
            lividUntil = now + EmberDungeonPolicy.LIVID_INVULN_MILLIS;
        }
        EmberDungeonPolicy.relicPickup(raw).ifPresent(color -> {
            lastRelic = color + " Relic";
            relicPickupAt = now;
            if (extras.dungeonF7Enabled && extras.dungeonF7RelicLook && client != null && client.player != null) {
                EmberDungeonPolicy.relicByName(color).ifPresent(relic -> {
                    relicLookFrom = new DungeonF7Policy.LookAim(client.player.getYRot(), client.player.getXRot());
                    relicLookTo = DungeonF7Policy.aimAt(
                            client.player.getX(),
                            client.player.getY() + client.player.getEyeHeight(),
                            client.player.getZ(),
                            relic.cauldron().x() + 0.5D,
                            relic.cauldron().y() + 0.8D,
                            relic.cauldron().z() + 0.5D);
                    relicLookStart = now;
                });
            }
        });
        if (extras.dungeonF7Enabled && extras.dungeonF7RelicSpawn
                && DungeonAssistPolicy.f7TimerFromChat(raw) == DungeonAssistPolicy.F7Timer.NECRON) {
            relicSpawnUntil = now + DungeonF7Policy.relicSpawnMillis(extras.dungeonF7RelicSpawnTicks);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Crystals) {
            if (DungeonF7Policy.crystalSpawnChat(raw)) {
                crystalSpawnUntil = now + DungeonF7Policy.CRYSTAL_RESPAWN_MILLIS;
            }
            if (DungeonF7Policy.crystalPickup(raw)) {
                crystalPickupAt = now;
            }
        }
        DungeonF7Policy.melodyPlayer(raw).ifPresent(name -> melodyLeapName = name);
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceMelody
                && DungeonF7Policy.melodyAlertChat(raw)) {
            showTitle(client, true, "§dMelody");
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons) {
            if (extras.dungeonF7DragonSpray && DungeonF7Policy.dragonSprayChat(raw)) {
                lastDragonHud = "Ice sprayed";
                showTitle(client, true, "§bIce Sprayed");
            }
            if (extras.dungeonF7DragonArrows && DungeonF7Policy.dragonArrowChat(raw)) {
                lastDragonHud = "Arrows hit";
                showTitle(client, true, "§eArrows Hit");
            }
        }
        DungeonF7Policy.p3Progress(raw).ifPresent(progress -> {
            switch (progress.kind()) {
                case "terminal" -> p3Terminals = progress.current();
                case "device" -> p3Devices = progress.current();
                case "lever" -> p3Levers = progress.current();
                default -> {
                }
            }
            lastP3Objective = progress.kind().substring(0, 1).toUpperCase(Locale.ROOT)
                    + progress.kind().substring(1);
            lastP3Completed = progress.current();
            lastP3Total = progress.total();
            if (extras.dungeonF7Enabled
                    && extras.dungeonF7TermPbs
                    && client != null
                    && client.player != null
                    && DungeonGoldorPolicy.namesMatch(progress.player(), localName(client))
                    && "terminal".equals(progress.kind())) {
                persistTerminalTypePb(extras, now);
            }
        });
        observeGoldorChat(client, extras, raw, now);
        if (DungeonF7Policy.p3GateDestroyed(raw)) {
            p3Terminals = 0;
            p3Devices = 0;
            p3Levers = 0;
            lastP3Objective = "Gate Destroyed";
            lastP3Completed = lastP3Total;
        }
        if (DungeonPolicy.normalize(raw).equals("The gate will open in 5 seconds!")) {
            lastP3Objective = "Break Gate";
        }
        if (DungeonF7Policy.dragonSpawnChat(raw)) {
            dragonsDown.clear();
            if (extras.dungeonF7Enabled && extras.dungeonF7DragonTimer) {
                dragonSpawnUntil = now + DungeonF7Policy.DRAGON_SPAWN_MILLIS;
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons
                && DungeonF7Policy.dragonKillChat(raw)) {
            DungeonF7Policy.dragonPadName(raw).ifPresent(name ->
                    dragonsDown.add(name.toLowerCase(Locale.ROOT)));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7PurplePad
                && DungeonAssistPolicy.f7TimerFromChat(raw) == DungeonAssistPolicy.F7Timer.STORM_PY) {
            purplePadTicks = DungeonF7Policy.PURPLE_PAD_TICKS;
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7AutoI4 && DungeonF7Policy.stormDeath(raw)) {
            i4Timer = 0;
            i4RodUsed = false;
            i4MaskUsed = false;
            i4LeapUsed = false;
            if (extras.dungeonF7GoldorFrenzy) {
                goldorFrenzyTicks = DungeonF7Policy.GOLDOR_FRENZY_TICKS;
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7AutoI4 && extras.dungeonF7AutoI4Leap
                && EmberDungeonPolicy.isDeviceDoneChat(raw)
                && client != null && client.player != null
                && EmberDungeonPolicy.isOnI4Device(
                        client.player.getX(), client.player.getY(), client.player.getZ())) {
            i4LeapUsed = true;
            i4Timer = -1;
        }
        EmberDungeonPolicy.DebuffPhase nextDebuff = EmberDungeonPolicy.debuffPhase(raw);
        if (nextDebuff != EmberDungeonPolicy.DebuffPhase.NONE) {
            debuffPhase = nextDebuff;
            debuffUntil = now + EmberDungeonPolicy.DEFAULT_DEBUFF_TICKS * 50L;
            debuffFired = false;
        }
        if (extras.dungeonHudPuzzleTimer) {
            if (EmberDungeonPolicy.isPuzzleTimerStart(raw)) {
                puzzleStarted = now;
            } else if (EmberDungeonPolicy.isPuzzleTimerEnd(raw)) {
                puzzleStarted = 0L;
            }
        }
        if (extras.dungeonHudEnabled && extras.dungeonHudRunTimers) {
            DungeonLeftoverPolicy.SplitEvent event = DungeonLeftoverPolicy.splitEvent(raw);
            if (event != DungeonLeftoverPolicy.SplitEvent.NONE) {
                DungeonLeftoverPolicy.SplitSnapshot previous = splits;
                splits = DungeonLeftoverPolicy.applySplit(splits, event, now);
                persistSplitPbs(previous, splits, extras);
            }
        }
        if (extras.dungeonHudEnabled && extras.dungeonHudKuudraSplits) {
            KuudraSplitPolicy.Phase phase = KuudraSplitPolicy.eventFromChat(raw);
            if (phase != KuudraSplitPolicy.Phase.NONE) {
                KuudraSplitPolicy.Snapshot previous = kuudra;
                kuudra = KuudraSplitPolicy.apply(kuudra, phase, now);
                persistKuudraPbs(previous, kuudra, extras);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Timers) {
            DungeonAssistPolicy.F7Timer timer = DungeonAssistPolicy.f7TimerFromChat(raw);
            if (timer != DungeonAssistPolicy.F7Timer.NONE
                    && DungeonF7Policy.timerAllowed(
                    timer,
                    extras.dungeonF7TimerPad,
                    extras.dungeonF7TimerLightning,
                    extras.dungeonF7TimerLightning,
                    extras.dungeonF7TimerGoldor,
                    extras.dungeonF7TimerNecron,
                    extras.dungeonF7TimerMaxor,
                    extras.dungeonF7TimerStorm)) {
                f7Timer = timer;
                f7TimerUntil = now + DungeonAssistPolicy.f7TimerMillis(timer);
            }
        }
        if (DungeonAssistPolicy.f7Title(raw) != DungeonAssistPolicy.F7Title.NONE) {
            DungeonAssistPolicy.F7Title kind = DungeonAssistPolicy.f7Title(raw);
            if (!shouldSuppressProgressTitle(client, extras, raw, kind)) {
                DungeonAssistPolicy.f7TitleText(
                        raw,
                        extras.dungeonF7TitleCrystalText,
                        extras.dungeonF7TitleWitherText,
                        extras.dungeonF7TitleTerminalText,
                        extras.dungeonF7TitleGateText).ifPresent(title -> {
                    lastF7Title = title;
                    showTitle(client, extras.dungeonF7Enabled && extras.dungeonF7Titles
                            && extras.dungeonAnnounceEnabled && extras.dungeonAnnounceF7
                            && DungeonF7Policy.titleAllowed(
                            kind,
                            extras.dungeonF7TitleCrystal,
                            extras.dungeonF7TitleWither,
                            extras.dungeonF7TitleTerminal,
                            extras.dungeonF7TitleGate), "§b" + title);
                });
            }
        } else {
            DungeonPolicy.f7Title(raw).ifPresent(title -> {
                lastF7Title = title;
                showTitle(client, extras.dungeonF7Enabled && extras.dungeonF7Titles
                        && extras.dungeonAnnounceEnabled && extras.dungeonAnnounceF7, "§b" + title);
            });
        }
        DungeonAssistPolicy.blessingLine(raw).ifPresent(line -> {
            String key = line.contains(" ") ? line.substring(0, line.indexOf(' ')) : line;
            blessings.put(key, line);
        });
        if (DungeonAssistPolicy.isRagnarockCancelled(raw) || DungeonPolicy.isRagnarockCancelled(raw)) {
            lastRagnarock = "Ragnarock cancelled";
            showTitle(client, extras.dungeonAnnounceEnabled && extras.dungeonAnnounceRagnarock,
                    "§cRagnarock Cancelled");
        }
        DungeonAssistPolicy.ragnarockStrength(raw).ifPresent(strength ->
                lastRagnarock = "Ragnarock +" + strength);
        if (extras.dungeonPuzzlesEnabled && (extras.dungeonPuzzlesQuiz || extras.dungeonPuzzlesQuizBoxes)) {
            DungeonAssistPolicy.quizCorrectOption(raw, lastQuizAnswers).ifPresent(index ->
                    lastQuizOption = index);
            List<String> answers = DungeonAssistPolicy.quizAnswers(raw);
            if (!answers.isEmpty()) {
                lastQuizAnswers = answers;
                lastQuiz = answers.getFirst();
                lastQuizOption = -1;
                if (extras.dungeonPuzzlesQuiz) {
                    showTitle(client, true, "§a" + lastQuiz);
                }
            }
        }
        if (extras.dungeonPuzzlesEnabled && extras.dungeonPuzzlesWeirdos) {
            DungeonAssistPolicy.WeirdoKind kind = DungeonAssistPolicy.weirdoKind(raw);
            if (kind == DungeonAssistPolicy.WeirdoKind.CORRECT) {
                lastWeirdoNpc = DungeonAssistPolicy.weirdoNpcName(raw).orElse("truth");
                lastQuiz = "Weirdos: " + lastWeirdoNpc;
                showTitle(client, extras.dungeonAnnounceEnabled && extras.dungeonAnnounceRooms,
                        "§aTruth: " + lastWeirdoNpc);
            } else if (kind == DungeonAssistPolicy.WeirdoKind.WRONG
                    && lastWeirdoNpc.isEmpty()) {
                lastQuiz = "Weirdos lie: "
                        + DungeonAssistPolicy.weirdoNpcName(raw).orElse("NPC");
            }
        }
        if (DungeonAssistPolicy.isRoomAlert(raw)) {
            puzzleFailed = true;
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceRooms
                && DungeonAssistPolicy.isRoomAlert(raw)) {
            showTitle(client, true, "§cPuzzle fail");
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceRooms
                && (DungeonPolicy.isRoomClearedChat(raw) || DungeonPolicy.isRoomSecretsChat(raw))
                && client != null && client.gui != null) {
            client.gui.hud.setTitle(Component.literal("§a"
                    + (DungeonPolicy.isRoomClearedChat(raw) ? "Room cleared" : "Secrets done")));
        }
        if (DungeonPolicy.isMimicChat(raw)) {
            mimicKilled = true;
        }
        if (extras.dungeonAnnounceEnabled) {
            DungeonPolicy.partyAnnounce(raw).ifPresent(text -> {
                if (DungeonPolicy.normalize(raw).equalsIgnoreCase(text)
                        || text.equals(lastAnnounce)) {
                    return;
                }
                boolean allow = switch (text) {
                    case DungeonPolicy.MIMIC_PARTY -> extras.dungeonAnnounceMimic;
                    case DungeonPolicy.PRINCE_PARTY -> extras.dungeonAnnouncePrince;
                    case DungeonPolicy.BAT_PARTY -> extras.dungeonAnnounceBat;
                    default -> false;
                };
                if (!allow) {
                    return;
                }
                if (client != null && client.player != null && client.player.connection != null) {
                    lastAnnounce = text;
                    client.player.connection.sendCommand("pc " + text);
                }
            });
        }
    }

    static void onWorldChanged() {
        sidebar = DungeonPolicy.parseSidebar(List.of());
        bonzoUntil = 0L;
        spiritUntil = 0L;
        phoenixUntil = 0L;
        bonzoCdUntil = 0L;
        spiritCdUntil = 0L;
        phoenixCdUntil = 0L;
        terracottaUntil = 0L;
        f7Timer = DungeonAssistPolicy.F7Timer.NONE;
        f7TimerUntil = 0L;
        requeueTicks = -1;
        terminalCooldown = 0;
        simonCooldown = 0;
        puzzleScanTicks = 0;
        lastAnnounce = "";
        lastF7Title = "";
        lastQuiz = "";
        lastQuizAnswers = List.of();
        lastQuizOption = -1;
        lastRagnarock = "";
        lastMelody = "";
        lastWeirdoNpc = "";
        lastBreakerCharges = -1;
        lastScore = -1;
        lastSecretsFound = -1;
        lastDuplicateTitle = "";
        lastPlayerCountTitle = "";
        locationHud = "";
        locationTicks = 0;
        predevBossEnter = 0L;
        predevAtThird = false;
        predevTracking = false;
        predevDoneMs = 0L;
        runChests = 0;
        chestRunCounted = false;
        chestWarned = false;
        lastLivid = "";
        lividUntil = 0L;
        bloodCampActive = false;
        lastLeapRegion = "";
        leapedIds.clear();
        debuffPhase = EmberDungeonPolicy.DebuffPhase.NONE;
        debuffUntil = 0L;
        debuffFired = false;
        puzzleStarted = 0L;
        warpCooldownUntil = 0L;
        quizTicks = 0;
        quizStage = 0;
        maxorStunTicks = 0;
        stormCrushTicks = 0;
        secretSpawnTicks = 0;
        stormLbTicks = 0;
        stormLbActive = false;
        termStartTicks = 0;
        explosiveShotLine = "";
        explosiveShotUntil = 0L;
        extraStats = DungeonExtraStatsPolicy.Snapshot.idle();
        extraStatsQuietTicks = 0;
        lastMelodyProgress = "";
        lastP3Objective = "";
        lastP3Completed = 0;
        lastP3Total = 7;
        secretWaypoints.clear();
        clickedSecrets.clear();
        seenKeyStands.clear();
        droppedKey = TempleDungeonPolicy.KeySkull.NONE;
        hashedRoomSecrets.clear();
        hashedRoomRotation.clear();
        hashedRoomTried.clear();
        hashedTileIdentity.clear();
        hashedTileRoom.clear();
        secretsFoundByRoom.clear();
        collectedSecrets.clear();
        seenSecretEntities.clear();
        mimicKilled = false;
        puzzleFailed = false;
        extraStatsSeen = false;
        dungeonRunStarted = false;
        dungeonWorldTicks = 0;
        HateDoorsRuntime.clear();
        lastRelic = "";
        leapHideAtMs = 0L;
        resetMelodyOther();
        crystalSpawnUntil = 0L;
        crystalPickupAt = 0L;
        lastCrystalHud = "";
        relicSpawnUntil = 0L;
        relicPickupAt = 0L;
        relicLookStart = 0L;
        relicLookFrom = null;
        relicLookTo = null;
        i4Timer = -1;
        i4RodUsed = false;
        i4MaskUsed = false;
        i4LeapUsed = false;
        melodyLeapName = "";
        i4LookStart = 0L;
        i4LookFrom = null;
        i4LookTarget = null;
        simon = DungeonF7Policy.SimonState.idle();
        lastDragonHud = "";
        melodyWasOpen = false;
        lastTerminalSlot = 22;
        lastTermTitle = "";
        terminalOpenedAt = 0L;
        clearPredictedClicks();
        closeChestArmed = false;
        closeChestWait = -1;
        closeChestTitle = "";
        superboomAttackHeld = false;
        autoClickedThisTick = false;
        termQueueUpdatedAt = 0L;
        terminalClickUntil = 0L;
        termQueue.clear();
        p3Terminals = 0;
        p3Devices = 0;
        p3Levers = 0;
        dragonSpawnUntil = 0L;
        dragonsDown.clear();
        terminalHadClicks = false;
        goldorFrenzyTicks = 0;
        purplePadTicks = 0;
        arrowClicks.clear();
        i4Cooldown = 0;
        i4Timer = -1;
        i4RodUsed = false;
        i4MaskUsed = false;
        i4LeapUsed = false;
        i4LeapWait = 0;
        melodyLeapName = "";
        relicLookStart = 0L;
        melodySkipQueue.clear();
        superboomCooldown = 0;
        ghostCooldown = 0;
        superboomSwapBackTicks = -1;
        superboomOriginalSlot = -1;
        termQueue.clear();
        splits = new DungeonLeftoverPolicy.SplitSnapshot(0L, 0L, 0L, 0L, false, false, false);
        kuudra = KuudraSplitPolicy.Snapshot.idle();
        sharpShooter = DungeonGoldorPolicy.resetShooter();
        termTimes = DungeonGoldorPolicy.TermTimesState.idle();
        positionCallouts.reset();
        lastTermTimeLine = "";
        lastTermTotalLine = "";
        chestProfitHud = List.of();
        lastMapBoard = emptyMapBoard();
        blessings.clear();
        tpLinks.clear();
        puzzleMarks.clear();
        mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
        lastPad = null;
        lastPlayerPos = null;
    }

    static List<String> displayLines(boolean editorOpen) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled) {
            if (extras.dungeonMenusEnabled && extras.dungeonMenusChestProfit && !chestProfitHud.isEmpty()) {
                return chestProfitHud;
            }
            return List.of();
        }
        if (!editorOpen
                && !SkyBlockDungeonDetector.confidentlyInDungeon()
                && chestProfitHud.isEmpty()
                && !(extras.dungeonHudKuudraSplits && kuudra.startMs() > 0L)) {
            String warp = extras.dungeonHudWarpCooldown
                    ? DungeonBladePolicy.warpHudLine(warpCooldownUntil - System.currentTimeMillis())
                    : "";
            List<String> hub = new ArrayList<>();
            if (!warp.isBlank()) {
                hub.add(warp);
            }
            if (extras.dungeonHudUnclaimedChests && unclaimedChests >= 0) {
                hub.add("Chests " + unclaimedChests);
            }
            if (extras.dungeonHudExtraStats && extraStats.ready()) {
                hub.addAll(extraStats.compactLines());
            }
            if (hub.isEmpty()) {
                return List.of();
            }
            List<String> lines = new ArrayList<>();
            lines.add("Dungeon");
            lines.addAll(hub);
            return lines;
        }
        List<String> lines = new ArrayList<>();
        lines.add("Dungeon");
        if (extras.dungeonHudFloor && !sidebar.floor().isEmpty()) {
            lines.add("Floor " + DungeonPolicy.hudFloorLabel(sidebar.floor()));
        }
        if (extras.dungeonHudClass && sidebar.dungeonClass() != DungeonPolicy.DungeonClass.UNKNOWN) {
            lines.add(sidebar.dungeonClass().name().charAt(0)
                    + sidebar.dungeonClass().name().substring(1).toLowerCase(Locale.ROOT));
        }
        if (extras.dungeonHudSecrets && !extras.dungeonHudMapExtra && !sidebar.secrets().isEmpty()) {
            lines.add("Secrets " + sidebar.secrets());
        }
        if (extras.dungeonHudScore && !extras.dungeonHudMapExtra && sidebar.score() >= 0) {
            lines.add("Score " + sidebar.score());
        }
        if (extras.dungeonHudCleared && sidebar.clearedPercent() >= 0) {
            lines.add("Cleared " + sidebar.clearedPercent() + "%");
        }
        long now = System.currentTimeMillis();
        if (extras.dungeonHudInvincibility) {
            addTimer(lines, "Bonzo", bonzoUntil, now);
            addTimer(lines, "Spirit", spiritUntil, now);
            addTimer(lines, "Phoenix", phoenixUntil, now);
            if (now >= bonzoUntil) {
                addTimer(lines, "Bonzo CD", bonzoCdUntil, now);
            }
            if (now >= spiritUntil) {
                addTimer(lines, "Spirit CD", spiritCdUntil, now);
            }
            if (now >= phoenixUntil) {
                addTimer(lines, "Phoenix CD", phoenixCdUntil, now);
            }
        }
        if (extras.dungeonHudTerracotta) {
            addTimer(lines, "Terracotta", terracottaUntil, now);
        }
        if (extras.dungeonHudBlessings) {
            if (blessings.isEmpty()) {
                lines.addAll(DungeonPolicy.blessingLines(sidebarLines()));
            } else {
                for (String line : blessings.values()) {
                    lines.add("Blessing " + line);
                }
            }
        }
        if (extras.dungeonHudF7Timers && extras.dungeonF7Enabled) {
            addTimer(lines, DungeonAssistPolicy.f7TimerLabel(f7Timer), f7TimerUntil, now);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7MaxorStun && maxorStunTicks > 0) {
            addTimer(lines, "Maxor stun", now + maxorStunTicks * 50L, now);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7StormCrush && stormCrushTicks > 0) {
            addTimer(lines, "Storm crush", now + stormCrushTicks * 50L, now);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7StormLb
                && sidebar.dungeonClass() == DungeonPolicy.DungeonClass.ARCHER) {
            String lb = DungeonBladePolicy.stormLbHudLine(DungeonBladePolicy.stormLbRemaining(stormLbTicks));
            if (!lb.isBlank()) {
                lines.add(lb);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7TermStart && termStartTicks > 0) {
            String terms = DungeonBladePolicy.termStartHudLine(termStartTicks);
            if (!terms.isBlank()) {
                lines.add(terms);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Crystals) {
            if (extras.dungeonF7CrystalSpawn) {
                addTimer(lines, "Crystal spawn", crystalSpawnUntil, now);
            }
            if (crystalPickupAt > 0L && extras.dungeonF7CrystalPlace) {
                lines.add("Crystal place " + String.format(Locale.ROOT, "%.1fs",
                        Math.max(0L, now - crystalPickupAt) / 1000.0D));
            }
            if (!lastCrystalHud.isEmpty()) {
                lines.add(lastCrystalHud);
            }
        }
        if (extras.dungeonHudRagnarock && !lastRagnarock.isEmpty()) {
            lines.add(lastRagnarock);
        }
        if ((extras.dungeonHudMelody || (extras.dungeonF7Enabled && extras.dungeonF7MelodyDisplay))
                && !lastMelody.isEmpty()) {
            lines.add(lastMelody);
        }
        if (extras.dungeonHudMelodyOther && !melodyOtherOwn && !melodyOtherName.isBlank()) {
            lines.add(DungeonBladePolicy.melodyTeammateHud(melodyOtherHudName(), melodyOtherPercent));
        }
        if (extras.dungeonHudQuiz && extras.dungeonPuzzlesEnabled && !lastQuiz.isEmpty()) {
            lines.add(lastQuiz);
        }
        if (extras.dungeonPuzzlesEnabled && extras.dungeonPuzzlesQuizTimer && quizTicks > 0) {
            String quizLine = DungeonBladePolicy.quizHudLine(quizStage, quizTicks);
            if (!quizLine.isBlank()) {
                lines.add(quizLine);
            }
        }
        if (extras.dungeonHudWarpCooldown) {
            String warp = DungeonBladePolicy.warpHudLine(warpCooldownUntil - now);
            if (!warp.isBlank()) {
                lines.add(warp);
            }
        }
        if (extras.dungeonHudSecretSpawn && secretSpawnTicks > 0 && !sidebar.boss()) {
            String spawn = DungeonBladePolicy.secretSpawnHudLine(secretSpawnTicks);
            if (!spawn.isBlank()) {
                lines.add(spawn);
            }
        }
        if (extras.dungeonHudExplosiveShot && now < explosiveShotUntil && !explosiveShotLine.isBlank()) {
            lines.add(explosiveShotLine);
        }
        if (extras.dungeonHudUnclaimedChests && unclaimedChests >= 0) {
            lines.add("Chests " + unclaimedChests);
        }
        if (extras.dungeonHudExtraStats && extraStats.ready()) {
            lines.addAll(extraStats.compactLines());
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceLocation
                && locationTicks > 0 && !locationHud.isBlank()) {
            lines.add(locationHud);
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceKeyDrop
                && droppedKey != TempleDungeonPolicy.KeySkull.NONE
                && TempleDungeonPolicy.keyDropClass(
                        sidebar.dungeonClass(), extras.dungeonAnnounceKeyDropAll)) {
            String title = TempleDungeonPolicy.keyDropTitle(droppedKey);
            if (!title.isBlank()) {
                lines.add(title);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Predev
                && DungeonBladePolicy.shouldTrackPredev(
                        true,
                        extras.dungeonF7PredevAll,
                        sidebar.dungeonClass() == DungeonPolicy.DungeonClass.HEALER)) {
            if (predevTracking && predevBossEnter > 0L) {
                lines.add(DungeonBladePolicy.predevHudLine(
                        now - predevBossEnter,
                        extras.dungeonF7PredevPbMs,
                        true));
            } else if (predevDoneMs > 0L) {
                lines.add(DungeonBladePolicy.predevHudLine(
                        predevDoneMs,
                        extras.dungeonF7PredevPbMs,
                        true));
            }
        }
        if (extras.dungeonHudLedge && extras.dungeonF7Enabled) {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null
                    && DungeonGoldorPolicy.showLedge(
                            true,
                            extras.dungeonHudLedgeYellow,
                            DungeonGoldorPolicy.inYellowPad(
                                    client.player.getX(), client.player.getY(), client.player.getZ()),
                            termTimes.inP2(),
                            termTimes.stormDead(),
                            extras.dungeonHudLedgeAll,
                            sidebar.floor() != null
                                    && sidebar.floor().toUpperCase(Locale.ROOT).startsWith("M"),
                            sidebar.dungeonClass())) {
                lines.add(DungeonGoldorPolicy.ledgeHudLine(
                        DungeonGoldorPolicy.ledgeDistance(client.player.getX())));
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7TermTimes) {
            Map<String, Long> pbs = extras.dungeonF7TermPbs
                    ? DungeonLeftoverPolicy.parseSplitTimes(extras.dungeonF7TermPbTimes)
                    : Map.of();
            lines.addAll(DungeonGoldorPolicy.termHudLines(termTimes, pbs, now));
        }
        if (extras.dungeonHudPuzzleTimer && puzzleStarted > 0L) {
            lines.add("Puzzle " + Math.max(0, (now - puzzleStarted) / 1000L) + "s");
        }
        if (extras.dungeonHudRunTimers && splits.startMs() > 0L) {
            Map<String, Long> pbs = extras.dungeonHudShowSplitPbs
                    ? DungeonLeftoverPolicy.parseSplitTimes(extras.dungeonSplitPbs)
                    : Map.of();
            lines.addAll(splits.hudLines(now, pbs));
        }
        if (extras.dungeonHudKuudraSplits && kuudra.startMs() > 0L) {
            Map<String, Long> pbs = extras.dungeonHudShowSplitPbs
                    ? KuudraSplitPolicy.parseTimes(extras.kuudraSplitPbs)
                    : Map.of();
            lines.addAll(KuudraSplitPolicy.hudLines(kuudra, now, pbs));
        }
        if (extras.dungeonMenusEnabled && extras.dungeonMenusChestProfit && !chestProfitHud.isEmpty()) {
            lines.addAll(chestProfitHud);
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspLivid && !lastLivid.isEmpty()) {
            addTimer(lines, lastLivid, lividUntil, now);
        }
        if (extras.dungeonLeapEnabled && extras.dungeonLeapCounter && !lastLeapRegion.isEmpty()) {
            EmberDungeonPolicy.leapRegions().stream()
                    .filter(region -> region.id().equals(lastLeapRegion))
                    .findFirst()
                    .ifPresent(region -> lines.add(EmberDungeonPolicy.leapCounterText(
                            leapedIds.size(), region.maxCount())));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Debuff
                && debuffPhase != EmberDungeonPolicy.DebuffPhase.NONE) {
            addTimer(lines, EmberDungeonPolicy.debuffLabel(debuffPhase), debuffUntil, now);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Relics && !lastRelic.isEmpty()) {
            lines.add(lastRelic);
            if (extras.dungeonF7RelicPlace && relicPickupAt > 0L) {
                lines.add("Relic place " + String.format(Locale.ROOT, "%.1fs",
                        Math.max(0L, now - relicPickupAt) / 1000.0D));
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7RelicSpawn) {
            addTimer(lines, "Relic spawn", relicSpawnUntil, now);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SimonProgress
                && !simon.remaining().isEmpty()) {
            lines.add("Simon " + simon.remaining().size());
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons && extras.dungeonF7DragonPriority
                && (dragonSpawnUntil > now || !dragonsDown.isEmpty() || !lastDragonHud.isEmpty())) {
            lines.add(DungeonF7Policy.dragonPriorityLine(
                    extras.dungeonF7DragonPaul,
                    dragonsDown,
                    extras.dungeonF7DragonSoloClass));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons && !lastDragonHud.isEmpty()) {
            lines.add(lastDragonHud);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7DragonTimer && dragonSpawnUntil > now) {
            lines.add(DungeonF7Policy.formatCountdown(
                    "Dragon spawn",
                    dragonSpawnUntil - now,
                    extras.dungeonF7TimerTicks,
                    extras.dungeonF7TimerSymbol,
                    extras.dungeonF7TimerPrefix));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7P3Display
                && (p3Terminals > 0 || p3Devices > 0 || p3Levers > 0 || !lastP3Objective.isBlank())) {
            lines.add(DungeonF7Policy.p3HudLine(p3Terminals, p3Devices, p3Levers));
            if (!lastP3Objective.isBlank()) {
                lines.add(DungeonBladePolicy.sectionObjectiveHud(
                        lastP3Objective, lastP3Completed, lastP3Total));
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7GoldorFrenzy && goldorFrenzyTicks > 0) {
            lines.add(DungeonF7Policy.goldorFrenzyLine(goldorFrenzyTicks));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7PurplePad && purplePadTicks > 0) {
            lines.add(DungeonF7Policy.purplePadLine(purplePadTicks));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7ArrowAlign && !arrowClicks.isEmpty()) {
            int left = arrowClicks.stream().mapToInt(EmberDungeonPolicy.ArrowClicks::clicks).sum();
            lines.add("Arrows " + left);
        }
        if (extras.dungeonHudMap && !mapPreview.summary().isEmpty() && !extras.dungeonHudMapExtra) {
            lines.add(mapPreview.summary());
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7BreakerCharges && lastBreakerCharges >= 0) {
            lines.add("Breaker " + lastBreakerCharges);
        }
        if (lines.size() == 1 && editorOpen) {
            lines.add("Secrets 0/0");
            lines.add("Score 0");
        }
        return lines.size() <= 1 && !editorOpen ? List.of() : lines;
    }

    static DungeonMapPolicy.Board mapBoard() {
        return lastMapBoard;
    }

    static DungeonPuzzlePolicy.MapPreview mapPreview() {
        return mapPreview;
    }

    static DungeonMapPolicy.Schematic mapSchematic() {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled || !extras.dungeonHudMap) {
            return DungeonMapPolicy.Schematic.empty();
        }
        if (extras.dungeonHudMapHideBoss && sidebar.boss()) {
            return DungeonMapPolicy.Schematic.empty();
        }
        return DungeonMapPolicy.schematic(
                lastMapBoard,
                hashedTileIdentity,
                secretsFoundByRoom,
                extras.dungeonHudMapDoors,
                extras.dungeonHudMapPlayers,
                extras.dungeonMapRevealHidden() && extras.dungeonHudCheaterDarken,
                extras.dungeonHudCheaterDarkenFactor,
                DungeonMapPolicy.clampHudCell(extras.dungeonHudMapScale),
                DungeonMapPolicy.HUD_GAP,
                DungeonMapPolicy.HUD_PAD);
    }

    static List<DungeonAssistPolicy.MapChip> mapStatusChips() {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled || !extras.dungeonHudMap || !extras.dungeonHudMapExtra) {
            return List.of();
        }
        boolean puzzlesDone = !puzzleFailed
                && DungeonMapPolicy.puzzlesComplete(DungeonMapPolicy.uniqueRooms(lastMapBoard));
        return DungeonAssistPolicy.mapStatusBar(
                sidebar,
                extras.dungeonHudSecrets,
                extras.dungeonHudCrypts,
                extras.dungeonHudScore,
                extras.dungeonHudDeaths,
                mimicKilled,
                puzzlesDone,
                extras.dungeonHudMapMimic,
                extras.dungeonHudMapPuzzles);
    }

    public static boolean enqueueTerminalClick(int slot, int button) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonTerminalsEnabled || !extras.dungeonTerminalsQueue) {
            return false;
        }
        DungeonLeftoverPolicy.enqueue(termQueue, slot, button);
        rememberPredictedClick(DungeonPolicy.detectTerminal(lastTermTitle), slot);
        return true;
    }

    static List<String> mapFooterLines() {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled || !extras.dungeonHudMap || !extras.dungeonHudMapExtra) {
            return List.of();
        }
        return DungeonAssistPolicy.mapExtraInfo(
                sidebar,
                extras.dungeonHudSecrets,
                extras.dungeonHudCrypts,
                extras.dungeonHudScore,
                extras.dungeonHudDeaths);
    }

    static int overlayScore() {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled || !extras.dungeonHudScoreOverlay || sidebar.score() < 0) {
            return -1;
        }
        return sidebar.score();
    }

    static boolean shouldHideDioriteNow() {
        QolSkyblockExtras extras = extras();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (!extras.dungeonF7Enabled || !extras.dungeonF7HideDiorite || player == null) {
            return false;
        }
        BlockPos origin = player.blockPosition();
        return DungeonPolicy.inF7PillarBox(origin.getX(), origin.getY(), origin.getZ());
    }

    public static void renderGizmos() {
        QolSkyblockExtras extras = extras();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.level == null || !SkyBlockDungeonDetector.confidentlyInDungeon()) {
            return;
        }
        AABB search = player.getBoundingBox().inflate(DungeonPolicy.ESP_SCAN_RANGE);
        Vec3 eye = player.getEyePosition();
        if (extras.dungeonEspEnabled) {
            boolean needNamed = needsNamedDungeonEsp(extras);
            if (extras.dungeonEspBats || extras.dungeonEspTeammates || needNamed) {
                for (LivingEntity living : client.level.getEntitiesOfClass(LivingEntity.class, search)) {
                    if (living == player) {
                        continue;
                    }
                    if (living instanceof Player) {
                        if (extras.dungeonEspTeammates) {
                            box(living.getBoundingBox(), extras.dungeonEspTeammateColor, extras, eye);
                        }
                        continue;
                    }
                    if (extras.dungeonEspBats && living.getType() == EntityTypes.BAT) {
                        box(living.getBoundingBox(), extras.dungeonEspBatColor, extras, eye);
                    }
                    if (needNamed) {
                        considerNamedEsp(living, extras, eye);
                    }
                }
            }
        }
        highlightBlazeOrder(client, player, extras, eye, search);
        if ((extras.dungeonEspEnabled && extras.dungeonEspSimon)
                || (extras.dungeonF7Enabled && extras.dungeonF7Simon)) {
            highlightSimon(client, player, extras, eye);
        }
        if (extras.dungeonF7Enabled && !extras.dungeonF7HideDiorite) {
            markDiorite(client, player, extras, eye);
        }
        highlightEmberWorld(client, player, extras, eye, search);
        highlightDungeonDrops(client, player, extras, eye, search);
        renderClickedSecrets(extras, eye);
        for (Mark mark : puzzleMarks) {
            box(mark.box(), mark.color(), extras, eye);
        }
    }

    public static int highlightColor(AbstractContainerScreen<?> screen, int slotIndex) {
        if (screen == null || slotIndex < 0) {
            return 0;
        }
        QolSkyblockExtras extras = extras();
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (extras.dungeonLeapEnabled
                && extras.dungeonLeapHighlight
                && DungeonPolicy.isLeapMenu(title)) {
            if (slotIndex >= screen.getMenu().slots.size()) {
                return 0;
            }
            Slot slot = screen.getMenu().slots.get(slotIndex);
            if (slot == null || slot.getItem().isEmpty()) {
                return 0;
            }
            List<String> lore = InventoryChromeRuntime.loreLines(slot.getItem());
            if (DungeonPolicy.leapHeadUnavailable(lore)) {
                return 0;
            }
            DungeonPolicy.DungeonClass dungeonClass = DungeonPolicy.classFromLore(lore);
            return leapColor(dungeonClass);
        }
        if (extras.dungeonMenusEnabled && extras.dungeonMenusSalvage
                && DungeonAssistPolicy.isSalvageMenu(title)) {
            return salvageColor(screen, slotIndex, extras);
        }
        if (extras.dungeonMenusEnabled && extras.dungeonMenusPartyFinder
                && DungeonAssistPolicy.isPartyFinderMenu(title)) {
            return partyFinderColor(screen, slotIndex, extras);
        }
        if (extras.dungeonMenusEnabled && extras.dungeonMenusChestProfit
                && DungeonAssistPolicy.isDungeonChestMenu(title)) {
            return chestColor(screen, slotIndex, extras);
        }
        if (!extras.dungeonTerminalsEnabled || !extras.dungeonTerminalsOverlay) {
            return 0;
        }
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(title);
        if (terminal == DungeonPolicy.Terminal.NONE) {
            return 0;
        }
        if (!DungeonF7Policy.overlayTypeEnabled(
                terminal,
                extras.dungeonTerminalsMelody,
                extras.dungeonTerminalsNumbers,
                extras.dungeonTerminalsColors,
                extras.dungeonTerminalsRubix,
                extras.dungeonTerminalsPanes,
                extras.dungeonTerminalsStarts)) {
            return 0;
        }
        List<DungeonPolicy.TerminalItem> items = snapshot(screen);
        if (terminal == DungeonPolicy.Terminal.MELODY) {
            DungeonPolicy.MelodyState melody = DungeonPolicy.parseMelody(items);
            lastMelody = melody.readyToClick()
                    ? "Melody " + melody.current() + "=" + melody.correct()
                    : "Melody";
            int tint = DungeonF7Policy.melodyOverlayColor(
                    melody,
                    slotIndex,
                    extras.dungeonTerminalsMelodyColumnColor,
                    extras.dungeonTerminalsMelodyIndicatorColor,
                    extras.dungeonTerminalsMelodyWrongColor,
                    extras.dungeonTerminalsColor);
            if (tint != 0) {
                return tint;
            }
            if (melodyOverlay(melody, slotIndex)) {
                return extras.athen().termMelodyFillColor;
            }
            return extras.athen().termMelodyOtherColor;
        }
        List<DungeonPolicy.TerminalClick> clicks =
                remainingTerminalClicks(terminal, title, items);
        int order = -1;
        int button = 0;
        for (int i = 0; i < clicks.size(); i++) {
            if (clicks.get(i).slot() == slotIndex) {
                order = i;
                button = clicks.get(i).button();
                break;
            }
        }
        if (order < 0) {
            return 0;
        }
        DungeonAthenSettings athen = extras.athen();
        if (terminal == DungeonPolicy.Terminal.NUMBERS && extras.dungeonTerminalsNumbersShow
                && athen.termNumbersShowText) {
            return DungeonF7Policy.numbersOverlayColor(
                    order,
                    extras.dungeonTerminalsNumbers1Color,
                    extras.dungeonTerminalsNumbers2Color,
                    extras.dungeonTerminalsNumbers3Color,
                    extras.dungeonTerminalsColor);
        }
        if (terminal == DungeonPolicy.Terminal.RUBIX) {
            return DungeonF7Policy.rubixOverlayColor(
                    button,
                    extras.dungeonTerminalsRubixPosColor,
                    extras.dungeonTerminalsRubixNegColor,
                    extras.dungeonTerminalsColor);
        }
        if (terminal == DungeonPolicy.Terminal.STARTS_WITH) {
            return athen.termNamesSolutionColor;
        }
        if (terminal == DungeonPolicy.Terminal.PANES) {
            return athen.termPanesSolutionColor;
        }
        if (terminal == DungeonPolicy.Terminal.SELECT_ALL || terminal == DungeonPolicy.Terminal.COLORS) {
            return athen.termColorsSolutionColor;
        }
        return extras.dungeonTerminalsColor;
    }

    static String overlayLabel(AbstractContainerScreen<?> screen, int slotIndex) {
        if (screen == null || slotIndex < 0) {
            return "";
        }
        QolSkyblockExtras extras = extras();
        DungeonAthenSettings athen = extras.athen();
        if (!extras.dungeonTerminalsEnabled || !extras.dungeonTerminalsOverlay || !athen.termNumbersShowText) {
            return "";
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (DungeonPolicy.detectTerminal(title) != DungeonPolicy.Terminal.NUMBERS) {
            return "";
        }
        List<DungeonPolicy.TerminalClick> clicks =
                remainingTerminalClicks(DungeonPolicy.Terminal.NUMBERS, title, snapshot(screen));
        for (int i = 0; i < clicks.size(); i++) {
            if (clicks.get(i).slot() == slotIndex) {
                return String.valueOf(i + 1);
            }
        }
        return "";
    }

    public static void renderMenuExtras(
            AbstractContainerScreen<?> screen,
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            int leftPos,
            int topPos) {
        // Slot extras are drawn from SkyBlockMenuHighlightRuntime.
    }

    static void renderPartyFinderSlot(
            AbstractContainerScreen<?> screen,
            net.minecraft.client.gui.GuiGraphicsExtractor graphics,
            Slot slot,
            int slotIndex,
            int leftPos,
            int topPos) {
        if (screen == null || slot == null || slot.getItem().isEmpty()) {
            return;
        }
        QolSkyblockExtras extras = extras();
        DungeonAthenSettings athen = extras.athen();
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (!extras.dungeonMenusEnabled || !DungeonAssistPolicy.isPartyFinderMenu(title)) {
            return;
        }
        if (athen.pfStack) {
            int count = slot.getItem().getCount();
            if (count > 1) {
                Minecraft client = Minecraft.getInstance();
                if (client != null && client.font != null) {
                    RotClientUiDraw.text(
                            graphics,
                            client.font,
                            String.valueOf(count),
                            leftPos + slot.x + 10,
                            topPos + slot.y + 8,
                            0xFFFFFFFF,
                            true);
                }
            }
        }
        if (!athen.pfShowStats) {
            return;
        }
        String player = DungeonPartyFinderPolicy.partyLeader(
                slot.getItem().getHoverName().getString(),
                InventoryChromeRuntime.loreLines(slot.getItem())).orElse("");
        if (player.isBlank()) {
            return;
        }
        String floor = DungeonCarryPolicy.normalizeFloor(sidebar.floor());
        DungeonPartyJoinRuntime.prefetch(player, floor);
        var stats = DungeonPartyJoinRuntime.cached(player, floor);
        if (stats.isEmpty()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.font == null) {
            return;
        }
        RotClientUiDraw.text(
                graphics,
                client.font,
                DungeonPartyFinderPolicy.formatPb(stats.get().pbSeconds()),
                leftPos + slot.x,
                topPos + slot.y - 8,
                0xFFA3E635,
                true);
    }


    static void sendTerminalClick(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            QolSkyblockExtras extras,
            DungeonPolicy.TerminalClick click) {
        if (TermSimRuntime.isOpen()) {
            TermSimRuntime.click(click.slot(), click.button());
            armTerminalCooldown(extras);
            lastTerminalSlot = click.slot();
            autoClickedThisTick = true;
            rememberPredictedClick(DungeonPolicy.detectTerminal(
                    screen.getTitle() == null ? "" : screen.getTitle().getString()), click.slot());
            return;
        }
        if (extras.dungeonTerminalsQueue) {
            DungeonLeftoverPolicy.enqueue(termQueue, click.slot(), click.button());
            playTerminalClickSound(client, extras);
            lastTerminalSlot = click.slot();
            termQueueUpdatedAt = System.currentTimeMillis();
            autoClickedThisTick = true;
            rememberPredictedClick(DungeonPolicy.detectTerminal(
                    screen.getTitle() == null ? "" : screen.getTitle().getString()), click.slot());
            return;
        }
        int packetButton;
        ContainerInput input;
        int button = click.button();
        if (extras.athen().termRubixLeftOnly
                && DungeonPolicy.detectTerminal(screen.getTitle() == null ? "" : screen.getTitle().getString())
                == DungeonPolicy.Terminal.RUBIX) {
            button = 0;
        }
        if (button == 1) {
            packetButton = 1;
            input = ContainerInput.PICKUP;
        } else if (extras.dungeonTerminalsClone) {
            packetButton = 2;
            input = ContainerInput.CLONE;
        } else {
            packetButton = 0;
            input = ContainerInput.PICKUP;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                click.slot(),
                packetButton,
                input,
                client.player);
        playTerminalClickSound(client, extras);
        armTerminalCooldown(extras);
        lastTerminalSlot = click.slot();
        autoClickedThisTick = true;
        rememberPredictedClick(DungeonPolicy.detectTerminal(
                screen.getTitle() == null ? "" : screen.getTitle().getString()), click.slot());
    }

    static void armTerminalCooldown(QolSkyblockExtras extras) {
        DungeonAthenSettings athen = extras.athen();
        int minMs = DungeonAthenPortPolicy.clampTermDelayMs(athen.termMinDelayMs);
        int maxMs = DungeonAthenPortPolicy.clampTermDelayMs(athen.termMaxDelayMs);
        if (minMs > 0 || maxMs > 0) {
            terminalClickUntil = System.currentTimeMillis()
                    + DungeonAthenPortPolicy.randomBetween(minMs, maxMs);
            terminalCooldown = 0;
        } else {
            terminalCooldown = Math.max(1, extras.dungeonTerminalsDelay);
            terminalClickUntil = 0L;
        }
    }

    static void playTerminalClickSound(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonTerminalsSounds) {
            return;
        }
        playConfiguredClickSound(client, extras);
    }

    static void playConfiguredClickSound(Minecraft client, QolSkyblockExtras extras) {
        if (client == null || client.player == null) {
            return;
        }
        DungeonAthenSettings athen = extras.athen();
        String configured = athen.termClickSound == null ? "" : athen.termClickSound.trim();
        Identifier soundId = Identifier.tryParse(configured.contains(":")
                ? configured
                : "minecraft:" + configured);
        SoundEvent sound = soundId == null ? null : BuiltInRegistries.SOUND_EVENT.getValue(soundId);
        if (sound == null) {
            sound = SoundEvents.NOTE_BLOCK_PLING.value();
        }
        client.player.level().playLocalSound(
                client.player.getX(), client.player.getY(), client.player.getZ(),
                sound,
                SoundSource.PLAYERS,
                (float) athen.termClickVolume,
                (float) athen.termClickPitch,
                false);
    }





    static void updateDragonHud(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonF7Enabled || !extras.dungeonF7Dragons || !extras.dungeonF7DragonHealth
                || client.player == null || client.level == null) {
            return;
        }
        AABB search = client.player.getBoundingBox().inflate(80.0D);
        String best = "";
        float bestHealth = 0.0F;
        for (EnderDragon dragon : client.level.getEntitiesOfClass(EnderDragon.class, search)) {
            float health = dragon.getHealth();
            if (health > bestHealth) {
                bestHealth = health;
                String name = dragon.getName() == null ? "Dragon" : dragon.getName().getString();
                best = DungeonF7Policy.dragonHealthLine(name, health);
            }
        }
        if (!best.isBlank()) {
            lastDragonHud = best;
        }
    }



    static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }

    static void scanArrowAlign(Minecraft client) {
        arrowClicks.clear();
        if (client.player == null || client.level == null) {
            return;
        }
        EmberDungeonPolicy.IntVec corner = EmberDungeonPolicy.ARROW_GRID_CORNER;
        if (client.player.distanceToSqr(corner.x(), corner.y(), corner.z()) > 200.0D) {
            return;
        }
        AABB grid = new AABB(corner.x(), corner.y(), corner.z(),
                corner.x() + 1, corner.y() + 5, corner.z() + 5);
        int[] rotations = new int[25];
        java.util.Arrays.fill(rotations, -1);
        for (ItemFrame frame : client.level.getEntitiesOfClass(ItemFrame.class, grid)) {
            if (!frame.getItem().is(Items.ARROW)) {
                continue;
            }
            BlockPos pos = frame.blockPosition();
            int index = EmberDungeonPolicy.arrowIndex(pos.getX(), pos.getY(), pos.getZ());
            if (index >= 0) {
                rotations[index] = frame.getRotation();
            }
        }
        EmberDungeonPolicy.matchArrowSolution(rotations).ifPresent(solution ->
                arrowClicks.addAll(EmberDungeonPolicy.remainingArrowClicks(rotations, solution)));
    }

    static void observeLeaps(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonLeapEnabled || !extras.dungeonLeapCounter || client.player == null) {
            return;
        }
        var here = EmberDungeonPolicy.leapRegionAt(
                client.player.getX(), client.player.getY(), client.player.getZ());
        if (here.isEmpty()) {
            lastLeapRegion = "";
            leapedIds.clear();
            return;
        }
        EmberDungeonPolicy.LeapRegion region = here.get();
        if (!region.id().equals(lastLeapRegion)) {
            lastLeapRegion = region.id();
            leapedIds.clear();
        }
        AABB search = client.player.getBoundingBox().inflate(16.0D);
        for (Player other : client.level.getEntitiesOfClass(Player.class, search)) {
            if (other == client.player) {
                continue;
            }
            if (region.box().contains(other.getX(), other.getY(), other.getZ())
                    && other.getDeltaMovement().horizontalDistanceSqr() > 0.4D) {
                leapedIds.add(other.getId());
            }
        }
    }

    public static void onLeapEntityPacket(int entityId, double x, double y, double z) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonLeapEnabled || !extras.dungeonLeapCounter) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        if (entityId == client.player.getId()) {
            return;
        }
        var here = EmberDungeonPolicy.leapRegionAt(
                client.player.getX(), client.player.getY(), client.player.getZ());
        if (here.isEmpty()) {
            return;
        }
        EmberDungeonPolicy.LeapRegion region = here.get();
        lastLeapRegion = region.id();
        Entity entity = client.level.getEntity(entityId);
        if (!(entity instanceof Player) || entity == client.player) {
            return;
        }
        if (EmberDungeonPolicy.isLeapTeleportIntoRegion(region, x, y, z)) {
            leapedIds.add(entityId);
        }
    }


    static boolean hashAndCacheRoom(Minecraft client, int cx, int cz, long key) {
        List<String> column = new ArrayList<>();
        for (int y = DungeonRoomDataPolicy.HASH_Y_TOP; y >= DungeonRoomDataPolicy.HASH_Y_BOTTOM; y--) {
            column.add(fullBlockId(client, new BlockPos(cx, y, cz)));
        }
        int core = DungeonRoomDataPolicy.hashColumn(column);
        var room = DungeonRoomDataPolicy.roomForCore(core);
        int air = 0;
        for (String id : column) {
            if (id == null || id.isBlank() || id.endsWith(":air") || "minecraft:air".equals(id)) {
                air++;
            }
        }
        if (room.isEmpty()) {
            if (air < 80) {
                hashedRoomTried.add(key);
            }
            return false;
        }
        if (secretsPlacedFor(room.get().name())) {
            rememberHashedIdentity(cx, cz, room.get());
            hashedRoomTried.add(key);
            return true;
        }
        int tileX = DungeonMapPolicy.tileFromWorld(DungeonRoomDataPolicy.roomOrigin(cx));
        int tileZ = DungeonMapPolicy.tileFromWorld(DungeonRoomDataPolicy.roomOrigin(cz));
        List<DungeonRoomDataPolicy.MapTile> tiles = new ArrayList<>(
                DungeonMapPolicy.tilesNamed(hashedTileIdentity, room.get().name()));
        DungeonMapPolicy.UniqueRoom unique = DungeonMapPolicy.roomAt(
                DungeonMapPolicy.uniqueRooms(lastMapBoard, hashedTileIdentity), tileX, tileZ);
        if (unique != null) {
            for (DungeonMapPolicy.RoomTile tile : unique.tiles()) {
                DungeonRoomDataPolicy.MapTile mapTile =
                        new DungeonRoomDataPolicy.MapTile(tile.tileX(), tile.tileZ());
                if (!tiles.contains(mapTile)) {
                    tiles.add(mapTile);
                }
            }
        }
        int highest = DungeonRoomDataPolicy.highestBlock(column);
        Optional<DungeonRoomDataPolicy.Rotation> rotation = DungeonRoomDataPolicy.resolveRotation(
                room.get(),
                cx,
                cz,
                highest,
                tiles,
                (x, y, z) -> fullBlockId(client, new BlockPos(x, y, z)));
        if (rotation.isEmpty()) {
            rememberHashedIdentity(cx, cz, room.get());
            return false;
        }
        hashedRoomTried.add(key);
        hashedRoomSecrets.put(key, DungeonRoomDataPolicy.placeSecrets(room.get(), rotation.get()));
        hashedRoomRotation.put(key, rotation.get());
        rememberHashedIdentity(cx, cz, room.get());
        return true;
    }

    static boolean secretsPlacedFor(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        for (List<DungeonRoomDataPolicy.PlacedWaypoint> placed : hashedRoomSecrets.values()) {
            if (placed == null || placed.isEmpty()) {
                continue;
            }
            if (name.equalsIgnoreCase(placed.getFirst().roomName())) {
                return true;
            }
        }
        return false;
    }

    static void placePendingSecrets(Minecraft client) {
        if (client == null || client.level == null || hashedTileRoom.isEmpty()) {
            return;
        }
        Map<String, List<DungeonRoomDataPolicy.MapTile>> byName = new LinkedHashMap<>();
        for (Map.Entry<String, DungeonMapPolicy.RoomIdentity> entry : hashedTileIdentity.entrySet()) {
            DungeonMapPolicy.RoomIdentity identity = entry.getValue();
            if (identity == null || identity.name().isBlank()) {
                continue;
            }
            int[] tile = DungeonMapPolicy.parseTileKey(entry.getKey());
            if (tile == null) {
                continue;
            }
            byName.computeIfAbsent(identity.name(), ignored -> new ArrayList<>())
                    .add(new DungeonRoomDataPolicy.MapTile(tile[0], tile[1]));
        }
        for (Map.Entry<String, List<DungeonRoomDataPolicy.MapTile>> entry : byName.entrySet()) {
            if (secretsPlacedFor(entry.getKey())) {
                continue;
            }
            List<DungeonRoomDataPolicy.MapTile> tiles = entry.getValue();
            if (tiles.isEmpty()) {
                continue;
            }
            DungeonRoomDataPolicy.RoomMeta room = null;
            for (DungeonRoomDataPolicy.MapTile tile : tiles) {
                room = hashedTileRoom.get(DungeonMapPolicy.tileKey(tile.tileX(), tile.tileZ()));
                if (room != null) {
                    break;
                }
            }
            if (room == null) {
                continue;
            }
            boolean placed = false;
            for (DungeonRoomDataPolicy.MapTile tile : tiles) {
                int cx = DungeonMapPolicy.roomWorldCenter(tile.tileX());
                int cz = DungeonMapPolicy.roomWorldCenter(tile.tileZ());
                long key = pack(
                        DungeonRoomDataPolicy.roomOrigin(cx),
                        DungeonRoomDataPolicy.roomOrigin(cz));
                if (hashedRoomSecrets.containsKey(key) || !client.level.hasChunkAt(new BlockPos(cx, 69, cz))) {
                    continue;
                }
                List<String> column = new ArrayList<>();
                for (int y = DungeonRoomDataPolicy.HASH_Y_TOP; y >= DungeonRoomDataPolicy.HASH_Y_BOTTOM; y--) {
                    column.add(fullBlockId(client, new BlockPos(cx, y, cz)));
                }
                int highest = DungeonRoomDataPolicy.highestBlock(column);
                Optional<DungeonRoomDataPolicy.Rotation> rotation = DungeonRoomDataPolicy.resolveRotation(
                        room,
                        cx,
                        cz,
                        highest,
                        tiles,
                        (x, y, z) -> fullBlockId(client, new BlockPos(x, y, z)));
                if (rotation.isEmpty()) {
                    continue;
                }
                hashedRoomTried.add(key);
                hashedRoomSecrets.put(key, DungeonRoomDataPolicy.placeSecrets(room, rotation.get()));
                hashedRoomRotation.put(key, rotation.get());
                break;
            }
        }
    }

    static void rememberHashedIdentity(
            int cx, int cz, DungeonRoomDataPolicy.RoomMeta room) {
        int tileX = DungeonMapPolicy.tileFromWorld(DungeonRoomDataPolicy.roomOrigin(cx));
        int tileZ = DungeonMapPolicy.tileFromWorld(DungeonRoomDataPolicy.roomOrigin(cz));
        DungeonMapPolicy.RoomIdentity identity =
                new DungeonMapPolicy.RoomIdentity(
                        room.name(),
                        room.secrets(),
                        DungeonMapPolicy.fromRoomData(room.type()));
        hashedTileIdentity.put(DungeonMapPolicy.tileKey(tileX, tileZ), identity);
        hashedTileRoom.put(DungeonMapPolicy.tileKey(tileX, tileZ), room);
        DungeonMapPolicy.UniqueRoom unique = DungeonMapPolicy.roomAt(
                DungeonMapPolicy.uniqueRooms(lastMapBoard, hashedTileIdentity), tileX, tileZ);
        if (unique == null) {
            return;
        }
        for (DungeonMapPolicy.RoomTile tile : unique.tiles()) {
            hashedTileIdentity.put(DungeonMapPolicy.tileKey(tile.tileX(), tile.tileZ()), identity);
            hashedTileRoom.put(DungeonMapPolicy.tileKey(tile.tileX(), tile.tileZ()), room);
        }
    }

    static void creditRoomSecrets(Minecraft client, int delta) {
        if (delta <= 0 || client == null || client.player == null) {
            return;
        }
        int tileX = DungeonMapPolicy.tileFromWorld((int) Math.floor(client.player.getX()));
        int tileZ = DungeonMapPolicy.tileFromWorld((int) Math.floor(client.player.getZ()));
        DungeonMapPolicy.UniqueRoom unique = DungeonMapPolicy.roomAt(
                DungeonMapPolicy.uniqueRooms(lastMapBoard, hashedTileIdentity), tileX, tileZ);
        if (unique == null) {
            return;
        }
        DungeonMapPolicy.RoomIdentity identity = hashedTileIdentity.get(
                DungeonMapPolicy.tileKey(tileX, tileZ));
        if (identity == null || identity.name().isBlank()) {
            for (DungeonMapPolicy.RoomTile tile : unique.tiles()) {
                identity = hashedTileIdentity.get(DungeonMapPolicy.tileKey(tile.tileX(), tile.tileZ()));
                if (identity != null && !identity.name().isBlank()) {
                    break;
                }
            }
        }
        if (identity == null || identity.name().isBlank()) {
            return;
        }
        int next = secretsFoundByRoom.getOrDefault(identity.name(), 0) + delta;
        if (identity.secrets() > 0) {
            next = Math.min(next, identity.secrets());
        }
        secretsFoundByRoom.put(identity.name(), next);
    }



    static void highlightEmberWorld(
            Minecraft client,
            LocalPlayer player,
            QolSkyblockExtras extras,
            Vec3 eye,
            AABB search) {
        if (extras.dungeonEspEnabled && extras.dungeonEspHateDoors) {
            BlockPos origin = player.blockPosition();
            for (int dx = -6; dx <= 6; dx++) {
                for (int dy = -3; dy <= 6; dy++) {
                    for (int dz = -6; dz <= 6; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        String id = blockId(client, pos);
                        if (!EmberDungeonPolicy.isHateDoorBlock(
                                id,
                                extras.dungeonEspHateWither,
                                extras.dungeonEspHateBlood,
                                extras.dungeonEspHateEntrance)) {
                            continue;
                        }
                        int color;
                        if (extras.dungeonEspHateBlood && id.contains("red")) {
                            color = EmberDungeonPolicy.glassArgb(
                                    EmberDungeonPolicy.glassTint(extras.dungeonEspHateBloodGlass));
                        } else if (extras.dungeonEspHateEntrance && (id.contains("lime")
                                || id.contains("green") || id.contains("oak"))) {
                            color = EmberDungeonPolicy.glassArgb(
                                    EmberDungeonPolicy.glassTint(extras.dungeonEspHateEntranceGlass));
                        } else {
                            color = EmberDungeonPolicy.glassArgb(
                                    EmberDungeonPolicy.glassTint(extras.dungeonEspHateWitherGlass));
                        }
                        box(blockBox(pos), color, extras, eye);
                    }
                }
            }
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspSecretWaypoints) {
            observeCollectedSecrets(client, extras);
            List<DungeonRoomDataPolicy.MapTile> currentTiles = DungeonMapPolicy.currentRoomTiles(
                    (int) Math.floor(player.getX()),
                    (int) Math.floor(player.getZ()),
                    hashedTileIdentity);
            String currentName = currentHashedRoomName(client);
            for (DungeonRoomDataPolicy.PlacedWaypoint waypoint : secretWaypoints) {
                if (shouldHideCollectedSecret(client, extras, waypoint)) {
                    continue;
                }
                boolean currentRoom = DungeonMapPolicy.inRoomTiles(
                        waypoint.x(), waypoint.z(), currentTiles)
                        || (!currentName.isBlank() && currentName.equalsIgnoreCase(waypoint.roomName()));
                if (!currentRoom) {
                    continue;
                }
                BlockPos pos = new BlockPos(waypoint.x(), waypoint.y(), waypoint.z());
                int color = DungeonRoomDataPolicy.secretColor(waypoint.kind());
                box(blockBox(pos), color, extras, eye, true);
                String label = DungeonRoomDataPolicy.secretLabel(waypoint.kind());
                if (!label.isBlank()) {
                    var text = Gizmos.billboardTextOverBlock(label, pos, 0, color, 0.035F);
                    text.setAlwaysOnTop();
                }
            }
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspDoors && lastMapBoard.calibration().ok()) {
            for (DungeonMapPolicy.WorldDoor door : DungeonMapPolicy.worldDoors(lastMapBoard)) {
                box(new AABB(door.minX(), door.minY(), door.minZ(), door.maxX(), door.maxY(), door.maxZ()),
                        DungeonMapPolicy.doorArgb(door.type()), extras, eye);
            }
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspIcedMobs) {
            boolean holdingSpray = false;
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = player.getInventory().getItem(slot);
                if (stack == null || stack.isEmpty()) {
                    continue;
                }
                if (EmberDungeonPolicy.isIceSprayItem(
                        stack.getHoverName().getString(), InventoryChromeRuntime.loreLines(stack))) {
                    holdingSpray = true;
                    break;
                }
            }
            if (holdingSpray) {
                for (LivingEntity living : client.level.getEntitiesOfClass(LivingEntity.class, search)) {
                    if (EmberDungeonPolicy.isIcedMobName(entityName(living))) {
                        box(living.getBoundingBox(), extras.dungeonEspSecretColor, extras, eye);
                    }
                }
            }
        }
        if (extras.dungeonTerminalsEnabled && extras.dungeonTerminalsHitboxes) {
            DungeonAthenSettings athen = extras.athen();
            for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
                if (!EmberDungeonPolicy.isInactiveTerminal(entityName(stand))) {
                    continue;
                }
                if (EmberDungeonPolicy.terminalSection(stand.getX(), stand.getY(), stand.getZ()) > 0) {
                    box(stand.getBoundingBox(), extras.dungeonTerminalsHitboxColor, extras, eye);
                }
            }
            renderTerminalWaypoints(client, extras, athen, eye);
        }
        DungeonCarryRuntime.renderGizmos(client);
        if (extras.dungeonF7Enabled && extras.dungeonF7I4
                && !termTimes.goldorPhase()
                && player.blockPosition().closerThan(new BlockPos(66, 128, 50), 40.0D)) {
            boolean predicted = false;
            for (EmberDungeonPolicy.IntVec vec : EmberDungeonPolicy.i4Blocks()) {
                BlockPos pos = new BlockPos(vec.x(), vec.y(), vec.z());
                if (!EmberDungeonPolicy.isI4Lit(blockId(client, pos))) {
                    continue;
                }
                int color = extras.dungeonF7I4Color;
                if (extras.dungeonF7I4Predict && !predicted) {
                    color = extras.dungeonF7I4PredictColor;
                    predicted = true;
                }
                box(blockBox(pos), color, extras, eye);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SharpShooter && termTimes.goldorPhase()) {
            renderSharpShooter(extras, eye);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Gate) {
            int section = EmberDungeonPolicy.p3Section(player.getX(), player.getY(), player.getZ());
            EmberDungeonPolicy.gateForSection(section).ifPresent(gate -> {
                BlockPos coal = new BlockPos(gate.coal().x(), gate.coal().y(), gate.coal().z());
                if (!blockId(client, coal).contains("coal")) {
                    return;
                }
                EmberDungeonPolicy.Aabb box = gate.box();
                box(new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()),
                        extras.dungeonF7GateColor, extras, eye);
            });
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Relics && player.getY() < 20.0D) {
            for (EmberDungeonPolicy.Relic relic : EmberDungeonPolicy.relics()) {
                BlockPos spawn = new BlockPos(relic.spawn().x(), relic.spawn().y(), relic.spawn().z());
                BlockPos cauldron = new BlockPos(
                        relic.cauldron().x(), relic.cauldron().y(), relic.cauldron().z());
                if (player.blockPosition().closerThan(spawn, 48.0D)) {
                    box(blockBox(spawn), relic.color(), extras, eye);
                }
                if (player.blockPosition().closerThan(cauldron, 48.0D)) {
                    box(blockBox(cauldron), relic.color(), extras, eye);
                    if (extras.dungeonF7RelicBeacon) {
                        Vec3 from = new Vec3(cauldron.getX() + 0.5D, cauldron.getY() + 0.5D, cauldron.getZ() + 0.5D);
                        tracer(from, from.add(0.0D, 80.0D, 0.0D), relic.color(), extras);
                    }
                }
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7RelicHighlight && !lastRelic.isBlank()
                && player.getY() < 20.0D) {
            EmberDungeonPolicy.relicByName(lastRelic).ifPresent(relic -> {
                BlockPos cauldron = new BlockPos(
                        relic.cauldron().x(), relic.cauldron().y(), relic.cauldron().z());
                if (player.blockPosition().closerThan(cauldron, 48.0D)) {
                    box(blockBox(cauldron), relic.color(), extras, eye);
                }
            });
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons && extras.dungeonF7DragonBoxes
                && player.getY() < 30.0D) {
            for (DungeonF7Policy.DragonPad pad : DungeonF7Policy.dragonPads()) {
                EmberDungeonPolicy.Aabb box = pad.box();
                box(new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()),
                        pad.color(), extras, eye);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons && extras.dungeonF7DragonTracers
                && player.getY() < 40.0D) {
            for (DungeonF7Policy.DragonPad pad : DungeonF7Policy.dragonPads()) {
                EmberDungeonPolicy.Aabb box = pad.box();
                Vec3 center = new Vec3(
                        (box.minX() + box.maxX()) / 2.0D,
                        (box.minY() + box.maxY()) / 2.0D,
                        (box.minZ() + box.maxZ()) / 2.0D);
                tracer(eye, center, pad.color(), extras);
            }
            AABB dragons = player.getBoundingBox().inflate(80.0D);
            for (EnderDragon dragon : client.level.getEntitiesOfClass(EnderDragon.class, dragons)) {
                tracer(eye, dragon.getBoundingBox().getCenter(), 0xAAFFFFFF, extras);
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7ArrowAlign) {
            for (EmberDungeonPolicy.ArrowClicks click : arrowClicks) {
                EmberDungeonPolicy.IntVec vec = EmberDungeonPolicy.arrowPos(click.index());
                box(blockBox(new BlockPos(vec.x(), vec.y(), vec.z())),
                        EmberDungeonPolicy.arrowColor(click.clicks()), extras, eye);
            }
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspLivid) {
            BlockPos wool = new BlockPos(
                    EmberDungeonPolicy.LIVID_WOOL.x(),
                    EmberDungeonPolicy.LIVID_WOOL.y(),
                    EmberDungeonPolicy.LIVID_WOOL.z());
            EmberDungeonPolicy.lividFromWool(blockId(client, wool)).ifPresent(name -> {
                lastLivid = name;
                for (Player other : client.level.getEntitiesOfClass(Player.class, search)) {
                    if (other == player) {
                        continue;
                    }
                    if (EmberDungeonPolicy.hologramMatchesLivid(entityName(other), name)
                            || EmberDungeonPolicy.hologramMatchesLivid(other.getName().getString(), name)) {
                        box(other.getBoundingBox(), extras.dungeonEspLividColor, extras, eye);
                    }
                }
                for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
                    if (EmberDungeonPolicy.hologramMatchesLivid(entityName(stand), name)) {
                        box(stand.getBoundingBox(), extras.dungeonEspLividColor, extras, eye);
                    }
                }
            });
        }
    }

    static void renderTerminalWaypoints(
            Minecraft client,
            QolSkyblockExtras extras,
            DungeonAthenSettings athen,
            Vec3 eye) {
        DungeonPolicy.DungeonClass playerClass = sidebar.dungeonClass();
        for (EmberDungeonPolicy.WaypointNode node : EmberDungeonPolicy.waypointNodes()) {
            DungeonPolicy.DungeonClass assigned =
                    DungeonAthenPortPolicy.parseClass(athen.waypointClass(node.id()));
            if (!DungeonAthenPortPolicy.waypointVisible(athen.termCheckClass, playerClass, assigned)) {
                continue;
            }
            BlockPos pos = new BlockPos(node.x(), node.y(), node.z());
            int color = node.lever() ? athen.termLeverColor : athen.termWaypointColor;
            boolean fill = DungeonAthenPortPolicy.shouldFill(athen.termHighlightStyle);
            boolean outline = DungeonAthenPortPolicy.shouldOutline(athen.termHighlightStyle);
            int fillArgb = fill ? withAlpha(color, 0x66) : 0;
            int stroke = outline ? color : fillArgb;
            var props = Gizmos.cuboid(
                    blockBox(pos),
                    GizmoStyle.strokeAndFill(stroke, 2.0F, fillArgb));
            if (!athen.termDepthTest) {
                props.setAlwaysOnTop();
            }
            if (athen.termRenderText) {
                var text = Gizmos.billboardTextOverBlock(
                        node.label(), pos, 0, color, 0.035F);
                if (!athen.termDepthTest) {
                    text.setAlwaysOnTop();
                }
            }
        }
    }

    static List<DungeonMapPolicy.PlayerIcon> mapPlayers() {
        return lastMapBoard.players();
    }

    static boolean melodyOverlay(DungeonPolicy.MelodyState melody, int slot) {
        if (melody.readyToClick() && slot == melody.clickSlot()) {
            return true;
        }
        if (melody.correct() != null) {
            int col = melody.correct() + 1;
            int row = slot / 9;
            int slotCol = slot % 9;
            return slotCol == col && row >= 1 && row <= 4;
        }
        return false;
    }

    static int salvageColor(AbstractContainerScreen<?> screen, int slotIndex, QolSkyblockExtras extras) {
        if (slotIndex >= screen.getMenu().slots.size()) {
            return 0;
        }
        Slot slot = screen.getMenu().slots.get(slotIndex);
        if (slot == null || slot.getItem().isEmpty()) {
            return 0;
        }
        ItemStack stack = slot.getItem();
        int boost = SkyBlockItemData.infoSnapshot(stack).qualityBoost();
        String name = stack.getHoverName().getString();
        boolean starred = DungeonPolicy.isStarredName(name) || name.contains("✪");
        if (!DungeonAssistPolicy.salvageable(name, boost, starred)) {
            return 0;
        }
        return DungeonAssistPolicy.salvageColor(
                boost, extras.dungeonMenusSalvage50Color, extras.dungeonMenusSalvageLowColor);
    }

    static int partyFinderColor(
            AbstractContainerScreen<?> screen, int slotIndex, QolSkyblockExtras extras) {
        if (slotIndex >= screen.getMenu().slots.size()) {
            return 0;
        }
        Slot slot = screen.getMenu().slots.get(slotIndex);
        if (slot == null || slot.getItem().isEmpty()) {
            return 0;
        }
        List<String> lore = InventoryChromeRuntime.loreLines(slot.getItem());
        DungeonAthenSettings athen = extras.athen();
        if (athen.pfHighlight) {
            int status = DungeonPartyFinderPolicy.highlightColor(
                    DungeonPartyFinderPolicy.loreStatus(lore),
                    athen.pfJoinableColor,
                    athen.pfDupeColor,
                    athen.pfBlockedColor,
                    athen.pfVcColor,
                    athen.pfPermColor,
                    athen.pfCarryColor);
            if (status != 0) {
                return status;
            }
        }
        return DungeonAssistPolicy.partyFinderMatches(lore, extras.dungeonMenusPartyCata)
                ? extras.dungeonMenusProfitColor
                : 0;
    }

    static int chestColor(AbstractContainerScreen<?> screen, int slotIndex, QolSkyblockExtras extras) {
        if (slotIndex >= screen.getMenu().slots.size()) {
            return 0;
        }
        Slot slot = screen.getMenu().slots.get(slotIndex);
        if (slot == null || slot.getItem().isEmpty()) {
            return 0;
        }
        return DungeonAssistPolicy.chestProfit(
                slot.getItem().getHoverName().getString(),
                InventoryChromeRuntime.loreLines(slot.getItem()),
                extras.dungeonMenusIncludeEssence)
                .map(line -> DungeonAssistPolicy.profitTint(
                        line.coins() - (extras.dungeonMenusIncludeCost
                                ? DungeonAssistPolicy.chestCost(InventoryChromeRuntime.loreLines(slot.getItem()))
                                : 0L),
                        extras.dungeonMenusProfitColor,
                        0x80EF4444))
                .orElse(0);
    }

    static List<DungeonPolicy.TerminalItem> snapshot(AbstractContainerScreen<?> screen) {
        List<DungeonPolicy.TerminalItem> items = new ArrayList<>();
        List<Slot> slots = screen.getMenu().slots;
        int limit = Math.min(54, slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = slots.get(i);
            ItemStack stack = slot == null ? ItemStack.EMPTY : slot.getItem();
            String name = stack.isEmpty() ? "" : stack.getHoverName().getString();
            String id = "";
            if (!stack.isEmpty()) {
                var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
                id = key == null ? "" : key.getPath();
            }
            boolean enchanted = !stack.isEmpty() && stack.hasFoil();
            int count = stack.isEmpty() ? 0 : stack.getCount();
            items.add(new DungeonPolicy.TerminalItem(i, name, id, enchanted, count));
        }
        return items;
    }

    static void scanPuzzles(Minecraft client, QolSkyblockExtras extras) {
        puzzleMarks.clear();
        if (!extras.dungeonPuzzlesEnabled || !SkyBlockDungeonDetector.confidentlyInDungeon()) {
            return;
        }
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        if (extras.dungeonPuzzlesIce) {
            scanIceFill(client, origin);
        }
        if (extras.dungeonPuzzlesIcePath) {
            scanIcePath(client);
        }
        if (extras.dungeonPuzzlesWater) {
            scanWaterBoard(client, origin);
        }
        if (extras.dungeonPuzzlesBoulder) {
            scanBoulder(client, origin);
        }
        if (extras.dungeonPuzzlesTpMaze) {
            scanTpMaze(client, origin);
        }
        if (extras.dungeonPuzzlesCreeperBeams) {
            scanCreeperBeams(client);
        }
        if (extras.dungeonPuzzlesTicTacToe) {
            scanTicTacToe(client);
        }
        if (extras.dungeonPuzzlesQuizBoxes) {
            scanQuizOptionBoxes(client);
        }
        if (extras.dungeonPuzzlesWeirdos && !lastWeirdoNpc.isEmpty()) {
            scanWeirdoChest(client, player, origin);
        }
    }

    static void scanIceFill(Minecraft client, BlockPos origin) {
        if (scanIceFillBoard(client) || skipPuzzleFallback(client, "Ice Fill")) {
            return;
        }
        int minX = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        Map<Long, Boolean> ice = new HashMap<>();
        int y = origin.getY();
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                BlockPos pos = origin.offset(dx, 0, dz);
                String id = blockId(client, pos);
                if (DungeonPuzzlePolicy.isIceWalkable(id)) {
                    ice.put(pack(pos.getX(), pos.getZ()), true);
                    minX = Math.min(minX, pos.getX());
                    minZ = Math.min(minZ, pos.getZ());
                    maxX = Math.max(maxX, pos.getX());
                    maxZ = Math.max(maxZ, pos.getZ());
                }
            }
        }
        if (ice.isEmpty() || maxX - minX > 16 || maxZ - minZ > 16) {
            return;
        }
        boolean[][] walkable = new boolean[maxZ - minZ + 1][maxX - minX + 1];
        for (Map.Entry<Long, Boolean> entry : ice.entrySet()) {
            int x = unpackX(entry.getKey()) - minX;
            int z = unpackZ(entry.getKey()) - minZ;
            walkable[z][x] = true;
        }
        int startX = origin.getX() - minX;
        int startZ = origin.getZ() - minZ;
        if (startX < 0 || startZ < 0 || startZ >= walkable.length || startX >= walkable[0].length
                || !walkable[startZ][startX]) {
            boolean found = false;
            for (int z = 0; z < walkable.length && !found; z++) {
                for (int x = 0; x < walkable[0].length; x++) {
                    if (walkable[z][x]) {
                        startX = x;
                        startZ = z;
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                return;
            }
        }
        List<DungeonPuzzlePolicy.Cell> path = DungeonPuzzlePolicy.iceFillPath(walkable, startX, startZ);
        int shown = 0;
        for (DungeonPuzzlePolicy.Cell cell : path) {
            if (shown++ > 12) {
                break;
            }
            puzzleMarks.add(new Mark(blockBox(minX + cell.x(), y, minZ + cell.z()), 0xFF38BDF8));
        }
    }

    static void scanWaterBoard(Minecraft client, BlockPos origin) {
        if (scanWaterBoardLayout(client) || skipPuzzleFallback(client, "Water Board")) {
            return;
        }
        List<BlockPos> levers = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        List<String> palette = DungeonPuzzlePolicy.defaultWaterColors();
        int minX = origin.getX();
        int maxX = origin.getX();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();
        int y = origin.getY();
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    String id = blockId(client, pos);
                    if (id.equals("lever")) {
                        levers.add(pos);
                        String near = nearbyColor(client, pos, palette);
                        colors.add(near.isBlank() ? "lever" + levers.size() : near);
                    }
                    if (id.contains("water") || DungeonPuzzlePolicy.isChestBlock(id)
                            || id.contains("wool") || id.contains("terracotta")) {
                        minX = Math.min(minX, pos.getX());
                        maxX = Math.max(maxX, pos.getX());
                        minZ = Math.min(minZ, pos.getZ());
                        maxZ = Math.max(maxZ, pos.getZ());
                        y = pos.getY();
                    }
                }
            }
        }
        if (levers.size() < 2 || levers.size() > 8 || maxX - minX > 24 || maxZ - minZ > 24) {
            return;
        }
        int width = maxX - minX + 1;
        int height = maxZ - minZ + 1;
        char[][] grid = new char[height][width];
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                String id = blockId(client, new BlockPos(minX + x, y, minZ + z));
                if (id.contains("water")) {
                    grid[z][x] = 'S';
                } else if (DungeonPuzzlePolicy.isChestBlock(id)) {
                    grid[z][x] = 'C';
                } else {
                    int gate = DungeonPuzzlePolicy.leverColorIndex(id, colors);
                    grid[z][x] = gate >= 0 ? (char) ('0' + gate) : (id.isBlank() || id.equals("air") ? '.' : '#');
                }
            }
        }
        boolean[] open = new boolean[levers.size()];
        List<Integer> clicks = DungeonPuzzlePolicy.waterLeversToToggle(grid, open);
        if (clicks.isEmpty()) {
            for (BlockPos lever : levers) {
                puzzleMarks.add(new Mark(blockBox(lever), 0xFFFACC15));
            }
            return;
        }
        for (int index : clicks) {
            if (index >= 0 && index < levers.size()) {
                puzzleMarks.add(new Mark(blockBox(levers.get(index)), 0xFF22C55E));
            }
        }
    }

    static void scanBoulder(Minecraft client, BlockPos origin) {
        if (scanBoulderBoard(client) || skipPuzzleFallback(client, "Boulder")) {
            return;
        }
        int size = 7;
        char[][] grid = new char[size][size];
        int originX = origin.getX() - size / 2;
        int originZ = origin.getZ() - size / 2;
        int y = origin.getY();
        boolean sawBoulder = false;
        boolean sawChest = false;
        for (int z = 0; z < size; z++) {
            for (int x = 0; x < size; x++) {
                BlockPos pos = new BlockPos(originX + x, y, originZ + z);
                String id = blockId(client, pos);
                if (x == size / 2 && z == size / 2) {
                    grid[z][x] = 'P';
                } else if (DungeonPuzzlePolicy.isChestBlock(id)) {
                    grid[z][x] = 'C';
                    sawChest = true;
                } else if (DungeonPuzzlePolicy.isBoulderBlock(id)) {
                    grid[z][x] = 'O';
                    sawBoulder = true;
                } else if (DungeonPuzzlePolicy.isBoulderWall(id)) {
                    grid[z][x] = '#';
                } else {
                    grid[z][x] = '.';
                }
            }
        }
        if (!sawBoulder || !sawChest) {
            return;
        }
        List<DungeonPuzzlePolicy.BoulderStep> steps = DungeonPuzzlePolicy.boulderPath(grid);
        if (steps.isEmpty()) {
            return;
        }
        DungeonPuzzlePolicy.BoulderStep next = steps.getFirst();
        puzzleMarks.add(new Mark(
                blockBox(originX + next.x(), y, originZ + next.z()),
                next.push() ? 0xFFF97316 : 0xFF22C55E));
    }

    static void scanTpMaze(Minecraft client, BlockPos origin) {
        List<DungeonPuzzlePolicy.WorldCell> pads = new ArrayList<>();
        DungeonPuzzlePolicy.WorldCell chest = null;
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (hashedRoomIs(client, "Teleport Maze") && rotation != null) {
            for (DungeonPuzzleBoardPolicy.RelPos rel : DungeonPuzzleBoardPolicy.teleportMazePads()) {
                DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(rel, rotation);
                pads.add(new DungeonPuzzlePolicy.WorldCell(world.x(), world.y(), world.z()));
            }
        } else {
            for (int dx = -20; dx <= 20; dx++) {
                for (int dy = -3; dy <= 3; dy++) {
                    for (int dz = -20; dz <= 20; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        String id = blockId(client, pos);
                        if (DungeonPuzzlePolicy.isPressurePad(id)) {
                            pads.add(new DungeonPuzzlePolicy.WorldCell(pos.getX(), pos.getY(), pos.getZ()));
                        }
                        if (DungeonPuzzlePolicy.isChestBlock(id)) {
                            chest = new DungeonPuzzlePolicy.WorldCell(pos.getX(), pos.getY(), pos.getZ());
                        }
                    }
                }
            }
        }
        if (pads.size() < 2) {
            return;
        }
        DungeonPuzzlePolicy.WorldCell start = new DungeonPuzzlePolicy.WorldCell(
                origin.getX(), origin.getY(), origin.getZ());
        List<DungeonPuzzlePolicy.WorldCell> path = DungeonPuzzlePolicy.teleportPath(pads, tpLinks, start, chest);
        if (path.isEmpty()) {
            int shown = 0;
            for (DungeonPuzzlePolicy.WorldCell pad : pads) {
                if (shown++ > 12) {
                    break;
                }
                puzzleMarks.add(new Mark(blockBox(pad.x(), pad.y(), pad.z()), 0x80A855F7));
            }
            return;
        }
        int shown = 0;
        for (DungeonPuzzlePolicy.WorldCell pad : path) {
            if (shown++ > 8) {
                break;
            }
            puzzleMarks.add(new Mark(blockBox(pad.x(), pad.y(), pad.z()), 0xFFA855F7));
        }
    }

    static void scanWeirdoChest(Minecraft client, LocalPlayer player, BlockPos origin) {
        AABB search = player.getBoundingBox().inflate(16.0D);
        List<DungeonPuzzlePolicy.NamedPos> npcs = new ArrayList<>();
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
            String name = entityName(stand);
            if (!name.isBlank()) {
                npcs.add(new DungeonPuzzlePolicy.NamedPos(
                        name, stand.blockPosition().getX(), stand.blockPosition().getY(),
                        stand.blockPosition().getZ()));
            }
        }
        List<DungeonPuzzlePolicy.WorldCell> chests = new ArrayList<>();
        for (int dx = -12; dx <= 12; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -12; dz <= 12; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (DungeonPuzzlePolicy.isChestBlock(blockId(client, pos))) {
                        chests.add(new DungeonPuzzlePolicy.WorldCell(pos.getX(), pos.getY(), pos.getZ()));
                    }
                }
            }
        }
        DungeonPuzzlePolicy.weirdoChest(npcs, chests, lastWeirdoNpc).ifPresent(chest ->
                puzzleMarks.add(new Mark(blockBox(chest.x(), chest.y(), chest.z()), 0xFF22C55E)));
    }

    static void scanDungeonMap(Minecraft client, QolSkyblockExtras extras) {
        boolean hideBoss = extras.dungeonHudMapHideBoss && sidebar.boss();
        boolean needPreview = extras.dungeonHudEnabled && extras.dungeonHudMap && !hideBoss;
        boolean needBoard = extras.dungeonEspEnabled;
        if (!needPreview) {
            mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
        }
        if (!needPreview && !needBoard) {
            return;
        }
        if (client.player == null) {
            if (!needPreview) {
                mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
            }
            return;
        }
        MapItemSavedData data = findDungeonMapData(client);
        if (data == null) {
            lastMapBoard = emptyMapBoard();
            if (needPreview) {
                mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
            }
            return;
        }
        String selfName = client.player.getScoreboardName();
        List<DungeonMapPolicy.PlayerIcon> icons = mapIcons(data, extras, selfName);
        String classKey = "";
        if (extras.dungeonHudClassIcons || extras.dungeonHudHeadMarkers) {
            DungeonPolicy.DungeonClass found = sidebar.dungeonClass();
            classKey = found == DungeonPolicy.DungeonClass.UNKNOWN
                    ? (extras.dungeonHudHeadMarkers ? "head" : "")
                    : found.name().toLowerCase(Locale.ROOT);
        }
        DungeonMapPolicy.Board board = DungeonMapPolicy.scan(
                data.colors,
                128,
                icons,
                extras.dungeonHudMapPlayers ? client.player.getX() : null,
                extras.dungeonHudMapPlayers ? client.player.getZ() : null,
                extras.dungeonHudMapPlayers ? client.player.getYRot() : 0.0F,
                classKey,
                selfName == null ? "" : selfName,
                DungeonMapPolicy.floorNumber(sidebar.floor()));
        if (board.calibration().ok()) {
            if (extras.dungeonMapRevealHidden()) {
                board = DungeonLeftoverPolicy.revealHiddenRooms(board);
            }
            lastMapBoard = DungeonMapPolicy.applyHashedRooms(
                    board, hashedTileIdentity, extras.dungeonMapRevealHidden());
            mapPreview = new DungeonPuzzlePolicy.MapPreview(
                    0, 0, new int[0], -1, -1, lastMapBoard.summary());
        } else if (needPreview) {
            mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
        }
    }

    static MapItemSavedData findDungeonMapData(Minecraft client) {
        if (client.player == null || client.level == null) {
            return null;
        }
        MapItemSavedData held = mapDataIfDungeon(client, client.player.getMainHandItem());
        if (held != null) {
            return held;
        }
        held = mapDataIfDungeon(client, client.player.getOffhandItem());
        if (held != null) {
            return held;
        }
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            MapItemSavedData data = mapDataIfDungeon(client, stack);
            if (data != null) {
                return data;
            }
        }
        return null;
    }

    static MapItemSavedData mapDataIfDungeon(Minecraft client, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.is(Items.FILLED_MAP) || client.level == null) {
            return null;
        }
        MapId mapId = stack.get(DataComponents.MAP_ID);
        if (mapId == null) {
            return null;
        }
        MapItemSavedData data = client.level.getMapData(mapId);
        if (data == null || data.colors == null) {
            return null;
        }
        if (!DungeonMapPolicy.calibrate(data.colors, 128).ok()) {
            return null;
        }
        return data;
    }

    static void observeTeleport(LocalPlayer player) {
        Vec3 now = player.position();
        if (lastPlayerPos != null && now.distanceTo(lastPlayerPos) > 8.0D) {
            DungeonPuzzlePolicy.WorldCell from = lastPad;
            DungeonPuzzlePolicy.WorldCell to = new DungeonPuzzlePolicy.WorldCell(
                    player.blockPosition().getX(), player.blockPosition().getY(),
                    player.blockPosition().getZ());
            if (from != null) {
                tpLinks.put(DungeonPuzzlePolicy.cellKey(from), DungeonPuzzlePolicy.cellKey(to));
            }
            lastPad = to;
        } else {
            lastPad = new DungeonPuzzlePolicy.WorldCell(
                    player.blockPosition().getX(), player.blockPosition().getY(),
                    player.blockPosition().getZ());
        }
        lastPlayerPos = now;
    }

    static void highlightSimon(
            Minecraft client, LocalPlayer player, QolSkyblockExtras extras, Vec3 eye) {
        BlockPos start = new BlockPos(110, 121, 91);
        if (!player.blockPosition().closerThan(start, 48.0D)) {
            return;
        }
        box(blockBox(start), extras.dungeonEspSimonColor, extras, eye);
        List<EmberDungeonPolicy.IntVec> remaining =
                simon.remaining().isEmpty() ? simon.order() : simon.remaining();
        for (int i = 0; i < remaining.size(); i++) {
            EmberDungeonPolicy.IntVec vec = remaining.get(i);
            box(blockBox(new BlockPos(vec.x(), vec.y(), vec.z())),
                    DungeonF7Policy.simonColor(
                            i,
                            extras.dungeonF7SimonFirstColor,
                            extras.dungeonF7SimonSecondColor,
                            extras.dungeonF7SimonOtherColor),
                    extras,
                    eye);
        }
    }

    static void markDiorite(
            Minecraft client, LocalPlayer player, QolSkyblockExtras extras, Vec3 eye) {
        BlockPos origin = player.blockPosition();
        if (!DungeonPolicy.inF7PillarBox(origin.getX(), origin.getY(), origin.getZ())) {
            return;
        }
        for (int dx = -6; dx <= 6; dx++) {
            for (int dy = -4; dy <= 8; dy++) {
                for (int dz = -6; dz <= 6; dz++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!DungeonPolicy.isF7Diorite(blockId(client, pos))) {
                        continue;
                    }
                    box(new AABB(pos.getX(), pos.getY(), pos.getZ(),
                                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1),
                            0x66AAAAAA, extras, eye);
                }
            }
        }
    }

    static void box(AABB box, int color, QolSkyblockExtras extras, Vec3 eye) {
        box(box, color, extras, eye, extras != null && !extras.dungeonEspDepth);
    }

    static void box(
            AABB box, int color, QolSkyblockExtras extras, Vec3 eye, boolean throughWalls) {
        int fill = extras.dungeonEspFill
                ? withAlpha(color, (int) Math.round(DungeonAssistPolicy.clampOpacity(extras.dungeonEspOpacity) * 2.55D))
                : 0;
        var props = Gizmos.cuboid(box, GizmoStyle.strokeAndFill(color, 2.0F, fill));
        if (throughWalls) {
            props.setAlwaysOnTop();
        }
        if (extras.dungeonEspTracers) {
            tracer(eye, box.getCenter(), color, extras, throughWalls);
        }
    }

    static void tracer(Vec3 from, Vec3 to, int color, QolSkyblockExtras extras) {
        tracer(from, to, color, extras, extras != null && !extras.dungeonEspDepth);
    }

    static void tracer(
            Vec3 from, Vec3 to, int color, QolSkyblockExtras extras, boolean throughWalls) {
        var line = Gizmos.line(from, to, color);
        if (throughWalls) {
            line.setAlwaysOnTop();
        }
    }

    static boolean needsNamedDungeonEsp(QolSkyblockExtras extras) {
        return extras.dungeonEspSecrets
                || extras.dungeonEspStarred
                || extras.dungeonEspFels
                || extras.dungeonEspShadow
                || extras.dungeonEspKeys
                || extras.dungeonEspMimic
                || extras.dungeonEspCrystals
                || extras.dungeonEspWither
                || extras.dungeonEspThorn
                || extras.dungeonEspSpiritBear
                || extras.dungeonEspLivid
                || extras.dungeonEspBloodBox
                || (extras.dungeonF7Enabled && extras.dungeonF7WitherEsp);
    }

    static void considerNamedEsp(
            Entity entity,
            QolSkyblockExtras extras,
            Vec3 eye) {
        String name = entityName(entity);
        String lower = name.toLowerCase(Locale.ROOT);
        if (extras.dungeonEspSecrets && lower.contains("secret")) {
            int secretColor = lower.contains("chest")
                    ? extras.dungeonEspChestColor
                    : extras.dungeonEspSecretColor;
            box(entity.getBoundingBox(), secretColor, extras, eye);
        }
        DungeonPolicy.EspKind kind = DungeonPolicy.classifyHologram(name);
        if (kind == DungeonPolicy.EspKind.BLAZE) {
            return;
        }
        int color = colorFor(kind, extras, name);
        if (color == 0) {
            return;
        }
        box(entity.getBoundingBox(), color, extras, eye);
        if (extras.dungeonEspBloodBox && bloodCampActive
                && (kind == DungeonPolicy.EspKind.WATCHER
                || EmberDungeonPolicy.isBloodMobName(name))) {
            box(entity.getBoundingBox(), extras.dungeonEspBloodBoxColor, extras, eye);
            var line = Gizmos.line(eye, entity.getBoundingBox().getCenter(),
                    extras.dungeonEspBloodLineColor);
            if (!extras.dungeonEspDepth) {
                line.setAlwaysOnTop();
            }
        }
    }

    static int colorFor(DungeonPolicy.EspKind kind, QolSkyblockExtras extras, String name) {
        return switch (kind) {
            case STARRED -> extras.dungeonEspStarred ? extras.dungeonEspStarredColor : 0;
            case BAT -> extras.dungeonEspBats ? extras.dungeonEspBatColor : 0;
            case FEL -> extras.dungeonEspFels ? extras.dungeonEspFelColor : 0;
            case SHADOW_ASSASSIN -> extras.dungeonEspShadow ? extras.dungeonEspShadowColor : 0;
            case WITHER_KEY -> extras.dungeonEspKeys ? extras.dungeonEspKeyColor : 0;
            case BLOOD_KEY -> extras.dungeonEspKeys ? extras.dungeonEspBloodKeyColor : 0;
            case MIMIC, PRINCE -> extras.dungeonEspMimic ? extras.dungeonEspMimicColor : 0;
            case CRYSTAL, RELIC -> extras.dungeonEspCrystals ? extras.dungeonEspCrystalColor : 0;
            case WITHER -> {
                if (!extras.dungeonEspWither && !(extras.dungeonF7Enabled && extras.dungeonF7WitherEsp)) {
                    yield 0;
                }
                yield DungeonF7Policy.witherColor(
                        DungeonF7Policy.witherBoss(name),
                        extras.dungeonF7MaxorColor,
                        extras.dungeonF7StormColor,
                        extras.dungeonF7GoldorColor,
                        extras.dungeonF7NecronColor,
                        extras.dungeonEspWitherColor);
            }
            case THORN -> extras.dungeonEspThorn ? extras.dungeonEspThornColor : 0;
            case SPIRIT_BEAR -> extras.dungeonEspSpiritBear ? extras.dungeonEspSpiritBearColor : 0;
            case LIVID -> extras.dungeonEspLivid ? extras.dungeonEspLividColor : 0;
            case WATCHER -> extras.dungeonEspBloodBox ? extras.dungeonEspBloodBoxColor : 0;
            case BLAZE, NONE -> 0;
        };
    }

    static int leapColor(DungeonPolicy.DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case ARCHER -> 0x80FFAA00;
            case MAGE -> 0x8055FFFF;
            case BERSERK -> 0x80FF5555;
            case TANK -> 0x8055FF55;
            case HEALER -> 0x80FF55FF;
            case UNKNOWN -> 0;
        };
    }

    static void scanSimon(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        List<EmberDungeonPolicy.IntVec> lit = new ArrayList<>();
        for (EmberDungeonPolicy.IntVec lantern : DungeonF7Policy.simonLanterns()) {
            if (DungeonF7Policy.isSimonSequenceLit(blockId(
                    client, new BlockPos(lantern.x(), lantern.y(), lantern.z())))) {
                EmberDungeonPolicy.IntVec button = DungeonF7Policy.simonButtonForLantern(
                        lantern.x(), lantern.y(), lantern.z());
                if (button != null) {
                    lit.add(button);
                }
            }
        }
        simon = DungeonF7Policy.observeSimon(simon, lit);
    }


    static void updateCrystalHud(Minecraft client, QolSkyblockExtras extras, long now) {
        lastCrystalHud = "";
        if (!extras.dungeonF7Enabled || !extras.dungeonF7Crystals || client.player == null) {
            return;
        }
        if (extras.dungeonF7CrystalAlert) {
            for (int slot = 0; slot < 9; slot++) {
                ItemStack stack = client.player.getInventory().getItem(slot);
                if (stack != null && !stack.isEmpty()
                        && DungeonF7Policy.holdingEnergyCrystal(stack.getHoverName().getString())) {
                    lastCrystalHud = "Place crystal";
                    break;
                }
            }
        }
        boolean melodyOpen = false;
        if (client.gui.screen() instanceof AbstractContainerScreen<?> screen) {
            String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
            if (DungeonF7Policy.melodyTerminalTitle(title)) {
                melodyOpen = true;
                DungeonPolicy.MelodyState melody = DungeonPolicy.parseMelody(snapshot(screen));
                lastMelody = melody.readyToClick()
                        ? "Melody " + melody.current() + "=" + melody.correct()
                        : "Melody";
                if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceMelody && !melodyWasOpen) {
                    showTitle(client, true, "§dMelody");
                }
                if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceMelodyParty && !melodyWasOpen) {
                    sendParty(client, DungeonBladePolicy.melodyPartyMessage(extras.dungeonAnnounceMelodyMessage));
                }
                if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceMelodyProgress) {
                    DungeonBladePolicy.melodyClayRow(snapshot(screen)).ifPresent(row ->
                            DungeonBladePolicy.melodyProgressParty(row).ifPresent(text -> {
                                if (!text.equals(lastMelodyProgress)) {
                                    lastMelodyProgress = text;
                                    sendParty(client, text);
                                }
                            }));
                }
            }
        }
        if (!melodyOpen) {
            lastMelodyProgress = "";
        }
        melodyWasOpen = melodyOpen;
    }

    public static boolean shouldCancelBlockUse(BlockPos pos, boolean sneaking) {
        if (pos == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonF7Enabled) {
            return false;
        }
        if (DungeonF7Policy.blockWrongArrow(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7ArrowAlign && extras.dungeonF7ArrowBlockWrong,
                sneaking,
                false,
                arrowClicks)) {
            return true;
        }
        EmberDungeonPolicy.IntVec next = simon.nextButton();
        if (DungeonF7Policy.blockWrongSimon(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7Simon && extras.dungeonF7SimonBlockWrong,
                sneaking,
                next)) {
            return true;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client == null ? null : client.player;
        boolean holdingRelicOrMenu = player != null && (
                DungeonF7Policy.holdingRelicOrMenu(player.getMainHandItem().getHoverName().getString())
                        || DungeonF7Policy.holdingRelicOrMenu(player.getOffhandItem().getHoverName().getString()));
        if (EmberDungeonPolicy.blockRelicClick(
                extras.dungeonF7RelicBlockWrong,
                lastRelic,
                holdingRelicOrMenu,
                pos.getX(),
                pos.getY(),
                pos.getZ())) {
            return true;
        }
        if (!lastRelic.isBlank()
                && EmberDungeonPolicy.correctRelicCauldron(lastRelic, pos.getX(), pos.getY(), pos.getZ())) {
            lastRelic = "";
            relicPickupAt = 0L;
        }
        return false;
    }

    public static boolean shouldCancelEntityUse(Entity entity, boolean sneaking) {
        if (!(entity instanceof ItemFrame frame)) {
            return false;
        }
        return shouldCancelBlockUse(frame.blockPosition(), sneaking);
    }

    public static void onBlockUsed(BlockPos pos) {
        if (pos == null) {
            return;
        }
        if (DungeonF7Policy.isSimonButton(pos.getX(), pos.getY(), pos.getZ())
                || DungeonPolicy.isSimonStart(pos.getX(), pos.getY(), pos.getZ())) {
            if (DungeonPolicy.isSimonStart(pos.getX(), pos.getY(), pos.getZ())) {
                simon = DungeonF7Policy.SimonState.idle();
            }
            playSimonSound(Minecraft.getInstance());
        }
        DungeonRoomDataPolicy.matchingSecret(
                secretWaypoints, collectedSecrets, pos.getX(), pos.getY(), pos.getZ(), 0)
                .ifPresent(waypoint -> collectedSecrets.add(
                        DungeonRoomDataPolicy.secretKey(waypoint.x(), waypoint.y(), waypoint.z())));
        noteClickedSecret(pos);
    }

    public static boolean shouldHideTerminalTooltip(AbstractContainerScreen<?> screen) {
        return terminalOverlayActive(screen) && extras().dungeonTerminalsStopTooltips;
    }

    public static boolean shouldHideTerminalSlot(AbstractContainerScreen<?> screen, Slot slot) {
        if (slot == null || !terminalOverlayActive(screen) || !extras().dungeonTerminalsHideClicked) {
            return false;
        }
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(titleOf(screen));
        if (terminal == DungeonPolicy.Terminal.NONE || terminal == DungeonPolicy.Terminal.MELODY) {
            return false;
        }
        List<DungeonPolicy.TerminalClick> live = DungeonPolicy.solveTerminalClicks(
                terminal, titleOf(screen), snapshot(screen));
        if (live.isEmpty()) {
            return false;
        }
        List<DungeonPolicy.TerminalClick> clicks = pinglessRemaining(terminal, live);
        return DungeonF7Policy.shouldHideClickedSlot(
                true,
                DungeonF7Policy.chestTerminalSlot(slot.index),
                slot.getItem() == null || slot.getItem().isEmpty(),
                DungeonF7Policy.slotInSolution(clicks, slot.index));
    }

    public static boolean shouldCancelTerminalSlot(AbstractContainerScreen<?> screen, int slot) {
        if (screen == null || !extras().dungeonTerminalsEnabled) {
            return false;
        }
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(titleOf(screen));
        if (terminal == DungeonPolicy.Terminal.NONE) {
            return false;
        }
        if (DungeonF7Policy.chestTerminalSlot(slot) && terminalFirstClickPending()) {
            return true;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonTerminalsBlockWrongSlots) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        boolean sneaking = client != null && client.player != null && client.player.isShiftKeyDown();
        List<DungeonPolicy.TerminalClick> live = DungeonPolicy.solveTerminalClicks(
                terminal, titleOf(screen), snapshot(screen));
        if (live.isEmpty()) {
            return false;
        }
        List<DungeonPolicy.TerminalClick> clicks = pinglessRemaining(terminal, live);
        return DungeonF7Policy.shouldBlockWrongTerminalSlot(
                true,
                sneaking,
                DungeonF7Policy.chestTerminalSlot(slot),
                DungeonF7Policy.slotInSolution(clicks, slot));
    }

    public static void noteTerminalSlotClick(AbstractContainerScreen<?> screen, int slot) {
        if (screen == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        QolSkyblockExtras extras = extras();
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(titleOf(screen));
        if (terminal == DungeonPolicy.Terminal.NONE) {
            return;
        }
        List<DungeonPolicy.TerminalClick> live = DungeonPolicy.solveTerminalClicks(
                terminal, titleOf(screen), snapshot(screen));
        boolean solved = DungeonF7Policy.slotInSolution(live, slot);
        if (solved) {
            rememberPredictedClick(terminal, slot);
            playTerminalClickSound(client, extras);
            List<DungeonPolicy.TerminalClick> remaining = pinglessRemaining(terminal, live);
            if (remaining.isEmpty()) {
                terminalHadClicks = true;
                maybePlayTerminalComplete(client, extras);
            } else {
                terminalHadClicks = true;
            }
        }
    }

    static void addTimer(List<String> lines, String label, long until, long now) {
        if (label == null || label.isBlank() || until <= now) {
            return;
        }
        QolSkyblockExtras extras = extras();
        lines.add(DungeonF7Policy.formatCountdown(
                label,
                until - now,
                extras.dungeonF7TimerTicks,
                extras.dungeonF7TimerSymbol,
                extras.dungeonF7TimerPrefix));
    }

    static void observeTabHud(Minecraft client, QolSkyblockExtras extras) {
        if (client == null) {
            return;
        }
        boolean wantSpawn = extras.dungeonHudEnabled && extras.dungeonHudSecretSpawn
                && SkyBlockDungeonDetector.confidentlyInDungeon()
                && !sidebar.boss();
        boolean wantChests = extras.dungeonHudEnabled
                && (extras.dungeonHudUnclaimedChests || extras.dungeonHudChestWarning);
        if (!wantSpawn && !wantChests) {
            return;
        }
        for (String line : CommissionDisplayRuntime.tabLines(client)) {
            if (wantSpawn && DungeonBladePolicy.timeElapsedTab(line)) {
                secretSpawnTicks = DungeonBladePolicy.SECRET_SPAWN_TICKS;
            }
            if (wantChests) {
                DungeonBladePolicy.unclaimedChests(line).ifPresent(count -> {
                    if (unclaimedChests != count) {
                        runChests = 0;
                        chestWarned = false;
                        chestRunCounted = false;
                    }
                    unclaimedChests = count;
                });
            }
        }
    }

    static void sendParty(Minecraft client, String text) {
        if (text == null || text.isBlank() || client == null || client.player == null
                || client.player.connection == null) {
            return;
        }
        if (text.equals(lastAnnounce)) {
            return;
        }
        lastAnnounce = text;
        client.player.connection.sendCommand("pc " + text);
    }

    static void showTitle(Minecraft client, boolean enabled, String text) {
        if (!enabled || client == null || client.gui == null || text == null || text.isBlank()) {
            return;
        }
        client.gui.hud.setTitle(Component.literal(text));
    }

    static List<String> sidebarLines() {
        String text = SkyBlockSidebar.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("\\R"));
    }

    static String entityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        Component custom = entity.getCustomName();
        if (custom != null) {
            return custom.getString();
        }
        return entity.getName().getString();
    }

    static String blockId(Minecraft client, BlockPos pos) {
        return blockId(client.level.getBlockState(pos));
    }

    static String blockId(BlockState state) {
        if (state == null) {
            return "";
        }
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.getPath();
    }

    static String fullBlockId(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "minecraft:air" : key.toString();
    }

    static String nearbyColor(Minecraft client, BlockPos pos, List<String> palette) {
        for (BlockPos around : List.of(pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west())) {
            int idx = DungeonPuzzlePolicy.leverColorIndex(blockId(client, around), palette);
            if (idx >= 0) {
                return palette.get(idx);
            }
        }
        return "";
    }

    static AABB blockBox(BlockPos pos) {
        return blockBox(pos.getX(), pos.getY(), pos.getZ());
    }

    static AABB blockBox(int x, int y, int z) {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    static int unpackZ(long packed) {
        return (int) packed;
    }

    static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    static List<DungeonMapPolicy.PlayerIcon> mapIcons(
            MapItemSavedData data, QolSkyblockExtras extras, String selfName) {
        if (!extras.dungeonHudMapPlayers || data == null) {
            return List.of();
        }
        Minecraft client = Minecraft.getInstance();
        List<String> rosterLines = new ArrayList<>(sidebarLines());
        if (client != null) {
            rosterLines.addAll(CommissionDisplayRuntime.tabLines(client));
        }
        Map<String, DungeonPolicy.DungeonClass> classes =
                EmberDungeonPolicy.teammateClasses(rosterLines);
        List<DungeonMapPolicy.MapDecorationHint> hints = new ArrayList<>();
        for (var decoration : data.getDecorations()) {
            int x = decoration.x();
            int z = decoration.y();
            if (x < 0) {
                x += 128;
            }
            if (z < 0) {
                z += 128;
            }
            float yaw = decoration.rot() * 360.0F / 16.0F;
            String name = decoration.name().map(Component::getString).orElse("");
            boolean selfMarker = decoration.type() != null
                    && decoration.type().value() == MapDecorationTypes.FRAME.value();
            hints.add(new DungeonMapPolicy.MapDecorationHint(x, z, yaw, selfMarker, name));
        }
        return DungeonMapPolicy.assignTeammateIcons(
                hints,
                new ArrayList<>(classes.keySet()),
                selfName,
                classes,
                extras.dungeonHudClassIcons,
                extras.dungeonHudHeadMarkers);
    }

    public static float cameraHelperZoom(float vanilla) {
        QolSkyblockExtras extras = extras();
        return TempleDungeonPolicy.cameraZoom(
                RotClientClient.isCameraEnabled(),
                extras.cameraClip,
                extras.cameraCustomDistance,
                extras.cameraDistance,
                vanilla);
    }

    static boolean isDropKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_Q;
    }

    static int melodyDigitFromKey(int glfwKey, DungeonAthenSettings athen) {
        if (QolKeybindNames.resolveGlfwKey(athen.termMelodyKey1, "1") == glfwKey) {
            return 1;
        }
        if (QolKeybindNames.resolveGlfwKey(athen.termMelodyKey2, "2") == glfwKey) {
            return 2;
        }
        if (QolKeybindNames.resolveGlfwKey(athen.termMelodyKey3, "3") == glfwKey) {
            return 3;
        }
        if (QolKeybindNames.resolveGlfwKey(athen.termMelodyKey4, "4") == glfwKey) {
            return 4;
        }
        return digitFromKey(glfwKey);
    }

    static boolean clickHoveredSolver(AbstractContainerScreen<?> screen, int button) {
        if (!(screen instanceof AbstractContainerScreenAccessor accessor)) {
            return false;
        }
        Slot hovered = accessor.rotclient$hoveredSlot();
        if (hovered == null) {
            return false;
        }
        if (terminalFirstClickPending()) {
            return false;
        }
        String title = titleOf(screen);
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(title);
        var click = DungeonAthenPortPolicy.hoverClick(
                remainingTerminalClicks(terminal, title, snapshot(screen)),
                hovered.index,
                terminal);
        if (click.isEmpty()) {
            return false;
        }
        clickSolved(screen, new DungeonPolicy.TerminalClick(click.get().slot(), button));
        return true;
    }

    public static boolean tryBreakerInstamine(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null || pos == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        var stack = client.player.getMainHandItem();
        String name = stack.getHoverName().getString();
        String id = AutoClickerItemIdentity.skyBlockId(stack);
        boolean holding = TempleDungeonPolicy.isDungeonBreakerItem(name, id);
        int charges = DungeonPolicy.breakerCharges(InventoryChromeRuntime.loreLines(stack)).orElse(0);
        boolean fatigue = client.player.hasEffect(MobEffects.MINING_FATIGUE);
        if (!DungeonAthenPortPolicy.shouldInstamineBreaker(
                extras.dungeonF7Enabled,
                extras.athen().breakerInstamine,
                holding,
                fatigue,
                charges,
                fullBlockId(client, pos))) {
            return false;
        }
        client.level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return true;
    }

    public static boolean shouldSkipBreakerSecretMine(BlockPos pos) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || client.level == null || pos == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonF7Enabled || !extras.dungeonF7BreakerPreventSecrets) {
            return false;
        }
        var stack = client.player.getMainHandItem();
        String name = stack.getHoverName().getString();
        String id = AutoClickerItemIdentity.skyBlockId(stack);
        if (!TempleDungeonPolicy.isDungeonBreakerItem(name, id)) {
            return false;
        }
        return TempleDungeonPolicy.shouldBlockBreakerOnSecret(true, true, fullBlockId(client, pos));
    }

    static void resetSplitPersonalBests() {
        QolSkyblockExtras extras = extras();
        extras.dungeonSplitPbs = "";
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    static void resetKuudraPersonalBests() {
        QolSkyblockExtras extras = extras();
        extras.kuudraSplitPbs = "";
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    static void resetTerminalPersonalBests() {
        QolSkyblockExtras extras = extras();
        extras.dungeonF7TermPbTimes = "";
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    static void resetPredevPersonalBest() {
        QolSkyblockExtras extras = extras();
        extras.dungeonF7PredevPbMs = 0L;
        TrackerStore.save(RotClientClient.trackerConfig());
    }

    public static void paintMaskOverlay(GuiGraphicsExtractor graphics, int x, int y, ItemStack stack) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled
                || !extras.dungeonHudMaskOverlay
                || graphics == null
                || stack == null
                || stack.isEmpty()) {
            return;
        }
        DungeonPolicy.Invincibility kind =
                DungeonPolicy.maskFromSkyBlockId(AutoClickerItemIdentity.skyBlockId(stack));
        long now = System.currentTimeMillis();
        long remaining;
        long max;
        switch (kind) {
            case BONZO -> {
                remaining = bonzoCdUntil - now;
                max = DungeonPolicy.BONZO_COOLDOWN_MILLIS;
            }
            case SPIRIT -> {
                remaining = spiritCdUntil - now;
                max = DungeonPolicy.SPIRIT_COOLDOWN_MILLIS;
            }
            default -> {
                return;
            }
        }
        int height = DungeonPolicy.maskOverlayHeight(remaining, max);
        if (height <= 0) {
            return;
        }
        graphics.fill(x, y + 16 - height, x + 16, y + 16, extras.dungeonHudMaskOverlayColor);
    }

    static void onBlockUpdate(BlockPos pos, BlockState oldState, BlockState newState) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonF7Enabled || !extras.dungeonF7SharpShooter || pos == null) {
            return;
        }
        if (!termTimes.goldorPhase()
                || !DungeonGoldorPolicy.isDeviceBlock(pos.getX(), pos.getY(), pos.getZ())) {
            return;
        }
        sharpShooter = DungeonGoldorPolicy.observeBlock(
                sharpShooter,
                pos.getX(),
                pos.getY(),
                pos.getZ(),
                blockId(oldState),
                blockId(newState));
    }

    public static void onEntityMetadata(int entityId) {
        Minecraft client = Minecraft.getInstance();
        QolSkyblockExtras extras = extras();
        if (client == null || client.level == null || client.player == null
                || !extras.dungeonF7Enabled || !extras.dungeonF7SharpShooter
                || !termTimes.goldorPhase() || sharpShooter.complete()) {
            return;
        }
        Entity entity = client.level.getEntity(entityId);
        if (entity == null
                || !DungeonGoldorPolicy.inSharpRoom(
                        client.player.getX(), client.player.getY(), client.player.getZ())) {
            return;
        }
        if (DungeonGoldorPolicy.isActiveArmorStand(entityName(entity))) {
            completeSharpShooter(client, extras, "Entity");
        }
    }

    static void tickGoldorHelpers(Minecraft client, QolSkyblockExtras extras, long now) {
        if (client.player == null || client.level == null) {
            return;
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SharpShooter && termTimes.goldorPhase()
                && !sharpShooter.complete()) {
            sharpShooter = DungeonGoldorPolicy.observeWorld(sharpShooter, vec ->
                    blockId(client, new BlockPos(vec.x(), vec.y(), vec.z())));
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnouncePosition
                && SkyBlockDungeonDetector.confidentlyInDungeon()) {
            positionCallouts.tick(
                    client.player.getX(),
                    client.player.getY(),
                    client.player.getZ(),
                    termTimes.goldorSection(),
                    termTimes.inP2(),
                    termTimes.inP3()).ifPresent(text -> sendParty(client, text));
        }
        if (predevTracking
                && !predevAtThird
                && DungeonBladePolicy.shouldTrackPredev(
                        extras.dungeonF7Enabled && extras.dungeonF7Predev,
                        extras.dungeonF7PredevAll,
                        sidebar.dungeonClass() == DungeonPolicy.DungeonClass.HEALER)
                && DungeonBladePolicy.atThirdDevice(client.player.getX(), client.player.getZ())) {
            predevAtThird = true;
        }
    }

    static void observeGoldorChat(
            Minecraft client,
            QolSkyblockExtras extras,
            String raw,
            long now) {
        DungeonGoldorPolicy.TermTimesState previous = termTimes;
        termTimes = DungeonGoldorPolicy.applyChat(termTimes, raw, now);
        if (previous.goldorSection() != termTimes.goldorSection()
                || (previous.goldorPhase() && !termTimes.goldorPhase())) {
            resetMelodyOther();
        }
        DungeonAssistPolicy.f7TitleFields(raw).ifPresent(fields -> {
            if (fields.kind() == DungeonAssistPolicy.F7Title.TERMINAL
                    && melodyOtherNames.stream()
                    .anyMatch(name -> DungeonGoldorPolicy.namesMatch(fields.player(), name))) {
                resetMelodyOther();
            }
        });
        if (!previous.stormPhase() && termTimes.stormPhase()) {
            positionCallouts.enterP2();
        }
        if (!previous.goldorPhase() && termTimes.goldorPhase()) {
            positionCallouts.enterTerminals();
            sharpShooter = DungeonGoldorPolicy.resetShooter();
        }
        if (previous.goldorSection() != 5 && termTimes.goldorSection() == 5) {
            positionCallouts.enterTunnel();
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7TermTimes) {
            if (!termTimes.lastLine().isBlank() && !termTimes.lastLine().equals(lastTermTimeLine)
                    && client != null && client.player != null) {
                lastTermTimeLine = termTimes.lastLine();
                client.player.sendSystemMessage(Component.literal("§6" + termTimes.lastLine()));
            }
            if (!termTimes.totalLine().isBlank() && !termTimes.totalLine().equals(lastTermTotalLine)
                    && client != null && client.player != null) {
                lastTermTotalLine = termTimes.totalLine();
                client.player.sendSystemMessage(Component.literal("§b" + termTimes.totalLine()));
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7TermPbs) {
            persistSectionPbs(previous, termTimes, extras);
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnouncePosition) {
            positionCallouts.noteChat(raw);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SharpShooter && termTimes.goldorPhase()
                && !sharpShooter.complete()
                && client != null && client.player != null
                && DungeonGoldorPolicy.inSharpRoom(
                        client.player.getX(), client.player.getY(), client.player.getZ())
                && DungeonGoldorPolicy.localCompletedDevice(raw, localName(client))) {
            completeSharpShooter(client, extras, "Chat");
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SsComplete
                && client != null && client.player != null
                && DungeonBladePolicy.atSimonSays(
                        client.player.getX(), client.player.getY(), client.player.getZ())
                && DungeonGoldorPolicy.localCompletedDevice(raw, localName(client))) {
            showTitle(client, true, "§a" + DungeonBladePolicy.DEVICE_COMPLETE_TITLE);
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Pre4Complete
                && client != null && client.player != null
                && DungeonBladePolicy.atFourthDevice(
                        client.player.getX(), client.player.getY(), client.player.getZ())
                && DungeonGoldorPolicy.localCompletedDevice(raw, localName(client))) {
            showTitle(client, true, "§a" + DungeonBladePolicy.DEVICE_COMPLETE_TITLE);
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        }
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceKeyDrop
                && TempleDungeonPolicy.keyPickupClearsDrop(raw)) {
            droppedKey = TempleDungeonPolicy.KeySkull.NONE;
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspSecretClicked
                && TempleDungeonPolicy.lockedChestChat(raw)
                && !clickedSecrets.isEmpty()) {
            ClickedSecret last = clickedSecrets.removeLast();
            clickedSecrets.add(new ClickedSecret(last.x(), last.y(), last.z(), last.untilMs(), true));
        }
    }

    static void completeSharpShooter(Minecraft client, QolSkyblockExtras extras, String method) {
        if (sharpShooter.complete()) {
            return;
        }
        sharpShooter = DungeonGoldorPolicy.completeShooter(sharpShooter);
        if (extras.dungeonF7SharpComplete && client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal(
                    "§aSharp shooter device complete §7(" + method + ")"));
            showTitle(client, true, "§aDevice Complete");
        }
    }

    static void renderSharpShooter(QolSkyblockExtras extras, Vec3 eye) {
        for (EmberDungeonPolicy.IntVec pos : sharpShooter.marked()) {
            box(blockBox(pos.x(), pos.y(), pos.z()), extras.dungeonF7SharpMarkedColor, extras, eye);
        }
        if (sharpShooter.target() != null) {
            EmberDungeonPolicy.IntVec target = sharpShooter.target();
            box(blockBox(target.x(), target.y(), target.z()), extras.dungeonF7SharpTargetColor, extras, eye);
        }
        if (!extras.dungeonF7SharpAim) {
            return;
        }
        List<DungeonGoldorPolicy.Aim> aims = sharpShooter.aims();
        for (int i = 0; i < Math.min(3, aims.size()); i++) {
            EmberDungeonPolicy.Aabb aabb = DungeonGoldorPolicy.aimBox(aims.get(i));
            int color = switch (i) {
                case 0 -> extras.dungeonF7SharpAim1Color;
                case 1 -> extras.dungeonF7SharpAim2Color;
                default -> extras.dungeonF7SharpAim3Color;
            };
            box(new AABB(aabb.minX(), aabb.minY(), aabb.minZ(), aabb.maxX(), aabb.maxY(), aabb.maxZ()),
                    color, extras, eye);
        }
    }

    static void persistSectionPbs(
            DungeonGoldorPolicy.TermTimesState previous,
            DungeonGoldorPolicy.TermTimesState next,
            QolSkyblockExtras extras) {
        Map<String, Long> stored = DungeonLeftoverPolicy.parseSplitTimes(extras.dungeonF7TermPbTimes);
        Map<String, Long> updated = DungeonGoldorPolicy.recordSectionPbs(previous, next, stored);
        if (!updated.equals(stored)) {
            extras.dungeonF7TermPbTimes = DungeonLeftoverPolicy.writeSplitTimes(updated);
            TrackerStore.save(RotClientClient.trackerConfig());
        }
    }

    static void persistTerminalTypePb(QolSkyblockExtras extras, long now) {
        Map<String, Long> pbs = DungeonLeftoverPolicy.parseSplitTimes(extras.dungeonF7TermPbTimes);
        DungeonPolicy.Terminal type = DungeonPolicy.detectTerminal(lastTermTitle);
        if (DungeonGoldorPolicy.recordTerminalTypePb(pbs, type, terminalOpenedAt, now)) {
            extras.dungeonF7TermPbTimes = DungeonLeftoverPolicy.writeSplitTimes(pbs);
            TrackerStore.save(RotClientClient.trackerConfig());
        }
    }

    static String localName(Minecraft client) {
        if (client == null || client.player == null) {
            return "";
        }
        return client.player.getGameProfile().name();
    }

    public static boolean shouldHideHudTitle(Component title) {
        if (title == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonF7Enabled || !SkyBlockDungeonDetector.confidentlyInDungeon()) {
            return false;
        }
        return shouldSuppressProgressTitle(
                Minecraft.getInstance(), extras, title.getString(), DungeonAssistPolicy.f7Title(title.getString()));
    }

    public static boolean shouldHideTeammate(Player player) {
        if (player == null) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer local = client == null ? null : client.player;
        if (local == null || player.getId() == local.getId()) {
            return false;
        }
        if (!SkyBlockDungeonDetector.confidentlyInDungeon()) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonF7Enabled) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (HidePlayersPolicy.hideAfterLeap(
                extras.dungeonF7HideAfterLeap,
                extras.dungeonF7HideAfterLeapBoss,
                sidebar.boss(),
                leapHideAtMs,
                now)) {
            return true;
        }
        return HidePlayersPolicy.hideAtSimonSays(
                extras.dungeonF7HideAtSs,
                sidebar.boss(),
                extras.dungeonF7HideAtSsPreTerms,
                termTimes.inP3(),
                DungeonBladePolicy.atSimonSays(player.getX(), player.getY(), player.getZ()));
    }

    static boolean shouldSuppressProgressTitle(
            Minecraft client,
            QolSkyblockExtras extras,
            String raw,
            DungeonAssistPolicy.F7Title kind) {
        if (!extras.dungeonF7Enabled || raw == null || raw.isBlank()) {
            return false;
        }
        boolean atSs = client != null && client.player != null
                && DungeonBladePolicy.atSimonSays(
                        client.player.getX(), client.player.getY(), client.player.getZ());
        boolean atPre4 = client != null && client.player != null
                && DungeonBladePolicy.atFourthDevice(
                        client.player.getX(), client.player.getY(), client.player.getZ());
        if (DungeonAssistPolicy.hideProgressTitleAtDevice(
                extras.dungeonF7HideTitlesAtSs,
                extras.dungeonF7HideTitlesAtPre4,
                atSs,
                atPre4,
                termTimes.inP3(),
                raw)) {
            return true;
        }
        if (!extras.dungeonF7HideOtherTitles || !termTimes.inP3()) {
            return false;
        }
        if (kind != DungeonAssistPolicy.F7Title.TERMINAL && kind != DungeonAssistPolicy.F7Title.GATE
                && !DungeonAssistPolicy.isProgressTitle(raw)) {
            return false;
        }
        return DungeonAssistPolicy.otherProgressTitle(raw, localName(client));
    }

    static void noteMelodyParty(String raw, QolSkyblockExtras extras, Minecraft client) {
        if (!extras.dungeonHudMelodyOther || !termTimes.inP3()) {
            return;
        }
        DungeonBladePolicy.melodyPartyPercent(raw).ifPresent(party -> {
            if (party.percent() <= melodyOtherPercent) {
                return;
            }
            melodyOtherPercent = party.percent();
            melodyOtherName = party.username();
            melodyOtherOwn = party.username().equalsIgnoreCase(localName(client));
            melodyOtherNames.add(party.username());
        });
    }

    static void resetMelodyOther() {
        melodyOtherNames.clear();
        melodyOtherName = "";
        melodyOtherPercent = 0;
        melodyOtherOwn = false;
    }

    static String melodyOtherHudName() {
        Map<String, DungeonPolicy.DungeonClass> classes =
                EmberDungeonPolicy.teammateClasses(sidebarLines());
        DungeonPolicy.DungeonClass found = classes.getOrDefault(
                melodyOtherName, DungeonPolicy.dungeonClass(melodyOtherName));
        if (found == DungeonPolicy.DungeonClass.UNKNOWN) {
            return melodyOtherName.isBlank() ? "Someone" : melodyOtherName;
        }
        String name = found.name();
        return name.charAt(0) + name.substring(1).toLowerCase(Locale.ROOT);
    }

    static void persistSplitPbs(
            DungeonLeftoverPolicy.SplitSnapshot previous,
            DungeonLeftoverPolicy.SplitSnapshot next,
            QolSkyblockExtras extras) {
        if (previous == null || next == null) {
            return;
        }
        Map<String, Long> pbs = DungeonLeftoverPolicy.parseSplitTimes(extras.dungeonSplitPbs);
        boolean changed = false;
        String floor = sidebar.floor();
        if (next.bloodRushDone() && !previous.bloodRushDone()) {
            changed |= DungeonLeftoverPolicy.recordSplitPersonalBest(
                    pbs, DungeonLeftoverPolicy.splitPbKey(floor, "BLOOD_RUSH"), next.bloodRushMs());
            changed |= DungeonLeftoverPolicy.recordSplitPersonalBest(pbs, "BLOOD_RUSH", next.bloodRushMs());
        }
        if (next.bloodOpenDone() && !previous.bloodOpenDone()) {
            changed |= DungeonLeftoverPolicy.recordSplitPersonalBest(
                    pbs, DungeonLeftoverPolicy.splitPbKey(floor, "BLOOD_OPEN"), next.bloodOpenMs());
            changed |= DungeonLeftoverPolicy.recordSplitPersonalBest(pbs, "BLOOD_OPEN", next.bloodOpenMs());
        }
        if (next.bossDone() && !previous.bossDone()) {
            changed |= DungeonLeftoverPolicy.recordSplitPersonalBest(
                    pbs, DungeonLeftoverPolicy.splitPbKey(floor, "BOSS_ENTER"), next.bossEnterMs());
            changed |= DungeonLeftoverPolicy.recordSplitPersonalBest(pbs, "BOSS_ENTER", next.bossEnterMs());
        }
        if (changed) {
            extras.dungeonSplitPbs = DungeonLeftoverPolicy.writeSplitTimes(pbs);
            TrackerStore.save(RotClientClient.trackerConfig());
        }
    }

    static void persistKuudraPbs(
            KuudraSplitPolicy.Snapshot previous,
            KuudraSplitPolicy.Snapshot next,
            QolSkyblockExtras extras) {
        if (previous == null || next == null) {
            return;
        }
        Map<String, Long> pbs = KuudraSplitPolicy.parseTimes(extras.kuudraSplitPbs);
        boolean changed = false;
        for (KuudraSplitPolicy.Phase phase : next.times().keySet()) {
            Long ms = next.times().get(phase);
            if (ms == null || ms <= 0L || previous.times().containsKey(phase)) {
                continue;
            }
            changed |= KuudraSplitPolicy.recordPersonalBest(pbs, phase, ms);
        }
        if (changed) {
            extras.kuudraSplitPbs = KuudraSplitPolicy.writeTimes(pbs);
            TrackerStore.save(RotClientClient.trackerConfig());
        }
    }

    static void refreshChestProfitHud(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonMenusEnabled
                || !extras.dungeonMenusChestProfit
                || client.gui == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            chestProfitHud = List.of();
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (!DungeonAssistPolicy.isDungeonChestMenu(title)) {
            chestProfitHud = List.of();
            return;
        }
        List<DungeonAssistPolicy.ChestCoinLine> loot = new ArrayList<>();
        long cost = 0L;
        int limit = Math.min(54, screen.getMenu().slots.size());
        for (int i = 0; i < limit; i++) {
            Slot slot = screen.getMenu().slots.get(i);
            if (slot == null || slot.getItem().isEmpty()) {
                continue;
            }
            List<String> lore = InventoryChromeRuntime.loreLines(slot.getItem());
            cost = Math.max(cost, DungeonAssistPolicy.chestCost(lore));
            DungeonAssistPolicy.chestProfit(
                    slot.getItem().getHoverName().getString(), lore, extras.dungeonMenusIncludeEssence)
                    .ifPresent(loot::add);
        }
        DungeonAssistPolicy.ChestProfitSummary summary = DungeonAssistPolicy.summarizeChest(
                title,
                loot,
                extras.dungeonMenusIncludeCost ? cost : 0L,
                extras.dungeonMenusIncludeEssence);
        chestProfitHud = summary.hudLines(extras.dungeonMenusCompactProfit);
    }

    public static boolean handleContainerKey(AbstractContainerScreen<?> screen, int glfwKey) {
        if (screen == null) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (closeChestArmed && extras.dungeonMenusEnabled && extras.dungeonMenusCloseChest) {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.player != null) {
                closeChestArmed = false;
                client.player.closeContainer();
                return true;
            }
        }
        if (extras.dungeonTerminalsProtect && isCloseKey(glfwKey) && protectingTerminal()) {
            return true;
        }
        if (extras.dungeonTerminalsEnabled
                && DungeonPolicy.detectTerminal(titleOf(screen)) != DungeonPolicy.Terminal.NONE) {
            DungeonAthenSettings athen = extras.athen();
            if (athen.termDropKey && isDropKey(glfwKey)) {
                return clickHoveredSolver(screen, 0);
            }
            if (QolKeybindNames.resolveGlfwKey(athen.termKeybindLeft, "") == glfwKey) {
                return clickHoveredSolver(screen, 0);
            }
            if (QolKeybindNames.resolveGlfwKey(athen.termKeybindRight, "") == glfwKey) {
                return clickHoveredSolver(screen, 1);
            }
            int melodyDigit = melodyDigitFromKey(glfwKey, athen);
            if (extras.dungeonTerminalsMelodyKeys
                    && DungeonPolicy.detectTerminal(titleOf(screen)) == DungeonPolicy.Terminal.MELODY
                    && melodyDigit >= 1) {
                int slot = DungeonF7Policy.melodySlotForDigit(
                        melodyDigit, DungeonPolicy.melodyPlayRows(snapshot(screen)));
                if (slot >= 0) {
                    if (terminalFirstClickPending()) {
                        return true;
                    }
                    clickContainerSlot(screen, slot);
                    return true;
                }
            }
        }
        int digit = digitFromKey(glfwKey);
        if (digit < 1) {
            return false;
        }
        if (extras.dungeonTerminalsEnabled && extras.dungeonTerminalsMelodyKeys
                && DungeonPolicy.detectTerminal(titleOf(screen)) == DungeonPolicy.Terminal.MELODY) {
            int slot = DungeonF7Policy.melodySlotForDigit(
                    digit, DungeonPolicy.melodyPlayRows(snapshot(screen)));
            if (slot >= 0) {
                if (terminalFirstClickPending()) {
                    return true;
                }
                clickContainerSlot(screen, slot);
                return true;
            }
        }
        if (extras.dungeonLeapEnabled && extras.dungeonLeapKeys && DungeonPolicy.isLeapMenu(titleOf(screen))) {
            return DungeonLeapOverlayRuntime.clickDigit(screen, digit);
        }
        return false;
    }

    public static boolean handleContainerMouse(AbstractContainerScreen<?> screen, int button) {
        if (screen == null || !closeChestArmed) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonMenusEnabled || !extras.dungeonMenusCloseChest) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null) {
            return false;
        }
        closeChestArmed = false;
        client.player.closeContainer();
        return true;
    }

    static boolean protectingTerminal() {
        QolSkyblockExtras extras = extras();
        return extras.dungeonTerminalsEnabled
                && extras.dungeonTerminalsProtect
                && DungeonF7Policy.protectTerminal(
                        terminalOpenedAt,
                        System.currentTimeMillis(),
                        extras.dungeonTerminalsProtectMs);
    }

    static void observeTerminalOpen(Minecraft client, QolSkyblockExtras extras) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            // Pingless terminals keep the same chest open. A 1-frame empty screen
            // must not re-arm first-click delay between Auto Terms clicks.
            return;
        }
        String title = titleOf(screen);
        if (DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.NONE) {
            lastTermTitle = "";
            terminalOpenedAt = 0L;
            termQueue.clear();
            melodySkipQueue.clear();
            termQueueUpdatedAt = 0L;
            clearPredictedClicks();
            return;
        }
        if (!title.equals(lastTermTitle)) {
            lastTermTitle = title;
            terminalOpenedAt = System.currentTimeMillis();
            terminalHadClicks = false;
            termQueue.clear();
            melodySkipQueue.clear();
            termQueueUpdatedAt = 0L;
            clearPredictedClicks();
        }
    }

    static boolean isCloseKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_ESCAPE || glfwKey == GLFW.GLFW_KEY_E;
    }

    static int digitFromKey(int glfwKey) {
        if (glfwKey >= GLFW.GLFW_KEY_1 && glfwKey <= GLFW.GLFW_KEY_4) {
            return glfwKey - GLFW.GLFW_KEY_0;
        }
        if (glfwKey >= GLFW.GLFW_KEY_KP_1 && glfwKey <= GLFW.GLFW_KEY_KP_4) {
            return glfwKey - GLFW.GLFW_KEY_KP_0;
        }
        return -1;
    }

    static void noteDungeonRunStart(String raw) {
        if (dungeonRunStarted || raw == null) {
            return;
        }
        DungeonLeftoverPolicy.SplitEvent event = DungeonLeftoverPolicy.splitEvent(raw);
        if (event == DungeonLeftoverPolicy.SplitEvent.START
                || event == DungeonLeftoverPolicy.SplitEvent.BLOOD_DOOR
                || event == DungeonLeftoverPolicy.SplitEvent.BLOOD_CLEAR
                || event == DungeonLeftoverPolicy.SplitEvent.BOSS_ENTER
                || DungeonBladePolicy.dungeonEnterChat(raw)) {
            dungeonRunStarted = true;
        }
    }

    static void noteDungeonRunProgress(Minecraft client) {
        if (dungeonRunStarted || client == null) {
            return;
        }
        if (sidebar.secretsFound() > 0) {
            dungeonRunStarted = true;
            return;
        }
        for (String line : CommissionDisplayRuntime.tabLines(client)) {
            if (DungeonBladePolicy.timeElapsedPositive(line)) {
                dungeonRunStarted = true;
                return;
            }
        }
    }

    static void maybeFinishExtraStats(Minecraft client) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonHudEnabled || !extras.dungeonHudExtraStats) {
            return;
        }
        if (extraStats.collecting() && extraStats.ready()) {
            extraStatsQuietTicks++;
            if (extraStatsQuietTicks >= DungeonExtraStatsPolicy.DUMP_QUIET_TICKS) {
                extraStats = DungeonExtraStatsPolicy.closeDump(extraStats);
            }
        }
        maybePrintExtraStats(client);
    }

    static void maybePrintExtraStats(Minecraft client) {
        if (!DungeonExtraStatsPolicy.shouldPrint(extraStats)) {
            return;
        }
        List<String> summary = extraStats.compactLines();
        extraStats = DungeonExtraStatsPolicy.markPrinted(extraStats);
        if (client == null || client.player == null || summary.isEmpty()) {
            return;
        }
        for (String line : summary) {
            client.player.sendSystemMessage(Component.literal("§7" + line));
        }
    }

    static DungeonMapPolicy.Board emptyMapBoard() {
        return new DungeonMapPolicy.Board(
                DungeonMapPolicy.Calibration.none(), List.of(), List.of(), List.of(), "");
    }


    static String titleOf(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }

    static void clickContainerSlot(AbstractContainerScreen<?> screen, int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.gameMode == null || client.player == null) {
            return;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                slot,
                0,
                ContainerInput.PICKUP,
                client.player);
        lastTerminalSlot = slot;
    }







    static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }

    static boolean terminalOverlayActive(AbstractContainerScreen<?> screen) {
        if (screen == null || !extras().dungeonTerminalsEnabled || !extras().dungeonTerminalsOverlay) {
            return false;
        }
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(titleOf(screen));
        if (terminal == DungeonPolicy.Terminal.NONE) {
            return false;
        }
        QolSkyblockExtras extras = extras();
        return DungeonF7Policy.overlayTypeEnabled(
                terminal,
                extras.dungeonTerminalsMelody,
                extras.dungeonTerminalsNumbers,
                extras.dungeonTerminalsColors,
                extras.dungeonTerminalsRubix,
                extras.dungeonTerminalsPanes,
                extras.dungeonTerminalsStarts);
    }

    static boolean terminalFirstClickPending() {
        QolSkyblockExtras extras = extras();
        return DungeonAthenPortPolicy.firstClickPending(
                terminalOpenedAt,
                System.currentTimeMillis(),
                extras.athen().termFirstClickDelay);
    }

    static void maybePlayTerminalComplete(Minecraft client, QolSkyblockExtras extras) {
        if (!terminalHadClicks || !extras.dungeonTerminalsCompleteSounds) {
            terminalHadClicks = false;
            return;
        }
        terminalHadClicks = false;
        if (client == null || client.player == null) {
            return;
        }
        DungeonAthenSettings athen = extras.athen();
        client.player.playSound(
                SoundEvents.EXPERIENCE_ORB_PICKUP,
                (float) athen.termClickVolume,
                (float) Math.max(0.5D, athen.termClickPitch));
    }

    static void playSimonSound(Minecraft client) {
        QolSkyblockExtras extras = extras();
        if (client == null || client.player == null
                || !extras.dungeonF7Enabled
                || !extras.dungeonF7SimonSounds) {
            return;
        }
        playConfiguredClickSound(client, extras);
    }

    static List<DungeonPolicy.TerminalClick> remainingTerminalClicks(
            DungeonPolicy.Terminal terminal,
            String title,
            List<DungeonPolicy.TerminalItem> items) {
        return pinglessRemaining(terminal, DungeonPolicy.solveTerminalClicks(terminal, title, items));
    }

    static List<DungeonPolicy.TerminalClick> pinglessRemaining(
            DungeonPolicy.Terminal terminal,
            List<DungeonPolicy.TerminalClick> live) {
        if (!DungeonAthenPortPolicy.usesPinglessPredict(terminal)) {
            return live == null ? List.of() : live;
        }
        long now = System.currentTimeMillis();
        if (DungeonAthenPortPolicy.queueNeedsResync(
                terminalPredictedAt, now, extras().athen().termResyncTimeout)) {
            terminalPredictedSlots.clear();
            terminalPredictedAt = 0L;
        } else if (!terminalPredictedSlots.isEmpty()) {
            List<Integer> kept = DungeonAthenPortPolicy.keepPredictedSlots(live, terminalPredictedSlots);
            terminalPredictedSlots.clear();
            terminalPredictedSlots.addAll(kept);
        }
        return DungeonAthenPortPolicy.withoutPredictedSlots(live, terminalPredictedSlots);
    }

    static void rememberPredictedClick(DungeonPolicy.Terminal terminal, int slot) {
        if (!DungeonAthenPortPolicy.usesPinglessPredict(terminal) || slot < 0) {
            return;
        }
        List<Integer> next = DungeonAthenPortPolicy.withPredictedSlot(terminalPredictedSlots, slot);
        terminalPredictedSlots.clear();
        terminalPredictedSlots.addAll(next);
        terminalPredictedAt = System.currentTimeMillis();
    }

    static void clearPredictedClicks() {
        terminalPredictedSlots.clear();
        terminalPredictedAt = 0L;
    }

    private record BlazeMark(AABB box, int health, double y) {
    }

    static void highlightBlazeOrder(
            Minecraft client,
            LocalPlayer player,
            QolSkyblockExtras extras,
            Vec3 eye,
            AABB search) {
        if (!extras.dungeonPuzzlesEnabled || !extras.dungeonPuzzlesBlaze || client.level == null) {
            return;
        }
        List<BlazeMark> blazes = new ArrayList<>();
        for (LivingEntity living : client.level.getEntitiesOfClass(LivingEntity.class, search)) {
            if (living == player || living instanceof Player) {
                continue;
            }
            String name = entityName(living);
            if (DungeonPolicy.classifyHologram(name) != DungeonPolicy.EspKind.BLAZE) {
                continue;
            }
            DungeonPolicy.blazeHealth(name).ifPresent(hp -> blazes.add(new BlazeMark(
                    living.getBoundingBox().inflate(0.5D, 1.0D, 0.5D).move(0.0D, -1.0D, 0.0D),
                    hp,
                    living.getY())));
        }
        if (blazes.isEmpty()) {
            return;
        }
        double lowestY = Double.POSITIVE_INFINITY;
        double highestY = Double.NEGATIVE_INFINITY;
        for (BlazeMark mark : blazes) {
            lowestY = Math.min(lowestY, mark.y());
            highestY = Math.max(highestY, mark.y());
        }
        boolean lowestFirst = DungeonAssistPolicy.blazeLowestFirst(
                currentHashedRoomName(client),
                blazeMaterialNear(client, player.blockPosition(), true),
                blazeMaterialNear(client, player.blockPosition(), false),
                player.getY(),
                lowestY,
                highestY);
        blazes.sort(lowestFirst
                ? Comparator.comparingInt(BlazeMark::health)
                : Comparator.comparingInt(BlazeMark::health).reversed());
        box(blazes.getFirst().box(), 0xFF22C55E, extras, eye);
        if (blazes.size() > 1) {
            box(blazes.get(1).box(), 0xFFFACC15, extras, eye);
            tracer(blazes.getFirst().box().getCenter(), blazes.get(1).box().getCenter(), 0xFFFACC15, extras);
        }
    }

    static void observeCollectedSecrets(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonEspHideCollected || client == null || client.level == null) {
            return;
        }
        for (DungeonRoomDataPolicy.PlacedWaypoint waypoint : secretWaypoints) {
            String key = DungeonRoomDataPolicy.secretKey(waypoint.x(), waypoint.y(), waypoint.z());
            if (collectedSecrets.contains(key)) {
                continue;
            }
            BlockPos pos = new BlockPos(waypoint.x(), waypoint.y(), waypoint.z());
            if (!client.level.hasChunkAt(pos)) {
                continue;
            }
            if (!DungeonRoomDataPolicy.secretBlockStillPresent(waypoint.kind(), fullBlockId(client, pos))) {
                collectedSecrets.add(key);
                continue;
            }
            if (waypoint.kind() != DungeonRoomDataPolicy.SecretKind.ITEM
                    && waypoint.kind() != DungeonRoomDataPolicy.SecretKind.BAT) {
                continue;
            }
            AABB box = new AABB(pos).inflate(
                    waypoint.kind() == DungeonRoomDataPolicy.SecretKind.BAT ? 5.0D : 2.5D);
            boolean present = waypoint.kind() == DungeonRoomDataPolicy.SecretKind.BAT
                    ? client.level.getEntitiesOfClass(LivingEntity.class, box).stream()
                    .anyMatch(entity -> entity.getType() == EntityTypes.BAT)
                    : !client.level.getEntitiesOfClass(ItemEntity.class, box).isEmpty();
            if (present) {
                seenSecretEntities.add(key);
            } else if (seenSecretEntities.contains(key)) {
                collectedSecrets.add(key);
            }
        }
    }

    static boolean shouldHideCollectedSecret(
            Minecraft client,
            QolSkyblockExtras extras,
            DungeonRoomDataPolicy.PlacedWaypoint waypoint) {
        if (!extras.dungeonEspHideCollected || waypoint == null) {
            return false;
        }
        String key = DungeonRoomDataPolicy.secretKey(waypoint.x(), waypoint.y(), waypoint.z());
        if (collectedSecrets.contains(key)) {
            return true;
        }
        if (client == null || client.level == null) {
            return false;
        }
        BlockPos pos = new BlockPos(waypoint.x(), waypoint.y(), waypoint.z());
        if (!client.level.hasChunkAt(pos)) {
            return false;
        }
        return !DungeonRoomDataPolicy.secretBlockStillPresent(waypoint.kind(), fullBlockId(client, pos));
    }

    static String currentHashedRoomName(Minecraft client) {
        if (client == null || client.player == null) {
            return "";
        }
        int tileX = DungeonMapPolicy.tileFromWorld((int) Math.floor(client.player.getX()));
        int tileZ = DungeonMapPolicy.tileFromWorld((int) Math.floor(client.player.getZ()));
        DungeonMapPolicy.RoomIdentity identity =
                hashedTileIdentity.get(DungeonMapPolicy.tileKey(tileX, tileZ));
        return identity == null ? "" : identity.name();
    }

    static boolean hashedRoomIs(Minecraft client, String name) {
        return name != null && name.equalsIgnoreCase(currentHashedRoomName(client));
    }

    static boolean skipPuzzleFallback(Minecraft client, String roomName) {
        String hashed = currentHashedRoomName(client);
        return !hashed.isBlank() && !roomName.equalsIgnoreCase(hashed);
    }

    static Optional<DungeonRoomDataPolicy.Rotation> currentHashedRotation(Minecraft client) {
        if (client == null || client.player == null) {
            return Optional.empty();
        }
        long key = pack(
                DungeonRoomDataPolicy.roomOrigin((int) Math.floor(client.player.getX())),
                DungeonRoomDataPolicy.roomOrigin((int) Math.floor(client.player.getZ())));
        return Optional.ofNullable(hashedRoomRotation.get(key));
    }

    static DungeonPuzzleBoardPolicy.BlockProbe relativeBlockProbe(
            Minecraft client,
            DungeonRoomDataPolicy.Rotation rotation) {
        return (x, y, z) -> {
            DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(
                    new DungeonPuzzleBoardPolicy.RelPos(x, y, z), rotation);
            return fullBlockId(client, new BlockPos(world.x(), world.y(), world.z()));
        };
    }

    static boolean scanIceFillBoard(Minecraft client) {
        if (!hashedRoomIs(client, "Ice Fill")) {
            return false;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return false;
        }
        List<DungeonPuzzleBoardPolicy.IceFillFloor> floors =
                DungeonPuzzleBoardPolicy.iceFillFloors(
                        relativeBlockProbe(client, rotation),
                        extras().dungeonPuzzlesIceOptimize);
        if (floors.isEmpty()) {
            return false;
        }
        for (DungeonPuzzleBoardPolicy.IceFillFloor floor : floors) {
            for (DungeonPuzzleBoardPolicy.RelPos rel : floor.path()) {
                DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(rel, rotation);
                puzzleMarks.add(new Mark(blockBox(world.x(), world.y(), world.z()), 0xFF38BDF8));
            }
        }
        return true;
    }

    static boolean scanWaterBoardLayout(Minecraft client) {
        if (!hashedRoomIs(client, "Water Board")) {
            return false;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return false;
        }
        DungeonPuzzleBoardPolicy.BlockProbe probe = relativeBlockProbe(client, rotation);
        Optional<Integer> pattern = DungeonPuzzleBoardPolicy.waterPatternId(probe);
        if (pattern.isEmpty()) {
            return false;
        }
        List<DungeonPuzzleBoardPolicy.WaterLever> levers = DungeonPuzzleBoardPolicy.waterLevers(
                pattern.get(),
                DungeonPuzzleBoardPolicy.waterExtendedSlots(probe),
                extras().dungeonPuzzlesWaterOptimized);
        if (levers.isEmpty()) {
            return false;
        }
        levers = new ArrayList<>(levers);
        levers.sort(Comparator.comparingDouble(lever ->
                lever.times().isEmpty() ? Double.MAX_VALUE : lever.times().getFirst()));
        for (int i = 0; i < levers.size(); i++) {
            DungeonPuzzleBoardPolicy.RelPos rel = levers.get(i).pos();
            DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(rel, rotation);
            puzzleMarks.add(new Mark(
                    blockBox(world.x(), world.y(), world.z()),
                    i == 0 ? 0xFF22C55E : 0xFFFACC15));
        }
        return true;
    }

    static boolean scanBoulderBoard(Minecraft client) {
        if (!hashedRoomIs(client, "Boulder")) {
            return false;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return false;
        }
        String signature = DungeonPuzzleBoardPolicy.boulderSignature(rel ->
                DungeonPuzzleBoardPolicy.isAir(relativeBlockProbe(client, rotation)
                        .idAt(rel.x(), rel.y(), rel.z())));
        List<DungeonPuzzleBoardPolicy.BoulderClick> clicks =
                DungeonPuzzleBoardPolicy.boulderClicks(signature);
        if (clicks.isEmpty()) {
            return false;
        }
        for (int i = 0; i < clicks.size(); i++) {
            DungeonPuzzleBoardPolicy.RelPos rel = clicks.get(i).render();
            DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(rel, rotation);
            puzzleMarks.add(new Mark(
                    blockBox(world.x(), world.y(), world.z()),
                    i == 0 ? 0xFFF97316 : 0xFFFACC15));
        }
        return true;
    }

    static void scanCreeperBeams(Minecraft client) {
        if (!hashedRoomIs(client, "Creeper Beams")) {
            return;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return;
        }
        int[] colors = {
                0xFF22C55E, 0xFF38BDF8, 0xFFFACC15, 0xFFA855F7,
                0xFFEF4444, 0xFFFB923C, 0xFF14B8A6, 0xFFEC4899
        };
        List<DungeonPuzzleBoardPolicy.CreeperPair> pairs = DungeonPuzzleBoardPolicy.creeperBeamPairs();
        DungeonPuzzleBoardPolicy.BlockProbe probe = relativeBlockProbe(client, rotation);
        int colorIndex = 0;
        for (DungeonPuzzleBoardPolicy.CreeperPair pair : pairs) {
            if (!DungeonPuzzleBoardPolicy.isSeaLantern(probe.idAt(pair.a().x(), pair.a().y(), pair.a().z()))
                    || !DungeonPuzzleBoardPolicy.isSeaLantern(probe.idAt(
                    pair.b().x(), pair.b().y(), pair.b().z()))) {
                continue;
            }
            int color = colors[colorIndex++ % colors.length];
            DungeonRoomDataPolicy.IntVec a = DungeonPuzzleBoardPolicy.world(pair.a(), rotation);
            DungeonRoomDataPolicy.IntVec b = DungeonPuzzleBoardPolicy.world(pair.b(), rotation);
            puzzleMarks.add(new Mark(blockBox(a.x(), a.y(), a.z()), color));
            puzzleMarks.add(new Mark(blockBox(b.x(), b.y(), b.z()), color));
        }
    }

    static void scanQuizOptionBoxes(Minecraft client) {
        if (!hashedRoomIs(client, "Quiz") || lastQuizOption < 0 || lastQuizOption > 2) {
            return;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return;
        }
        DungeonPuzzleBoardPolicy.RelPos rel = DungeonPuzzleBoardPolicy.quizOptionRels().get(lastQuizOption);
        DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(
                new DungeonPuzzleBoardPolicy.RelPos(rel.x(), rel.y() - 1, rel.z()), rotation);
        puzzleMarks.add(new Mark(blockBox(world.x(), world.y(), world.z()), 0xFF22C55E));
    }

    static void scanTicTacToe(Minecraft client) {
        if (!hashedRoomIs(client, "Tic Tac Toe") || client.level == null || client.player == null) {
            return;
        }
        int cx = DungeonRoomDataPolicy.roomCenter((int) Math.floor(client.player.getX()));
        int cz = DungeonRoomDataPolicy.roomCenter((int) Math.floor(client.player.getZ()));
        AABB box = new AABB(cx - 9, 65, cz - 9, cx + 9, 73, cz + 9);
        List<ItemFrame> frames = new ArrayList<>();
        for (ItemFrame frame : client.level.getEntitiesOfClass(ItemFrame.class, box)) {
            if (frame.getItem().is(Items.FILLED_MAP) && frame.getItem().has(DataComponents.MAP_ID)) {
                frames.add(frame);
            }
        }
        if (frames.size() == 8 || frames.size() % 2 == 0) {
            return;
        }
        char[] board = new char[9];
        DungeonTicTacToePolicy.BoardPos leftmost = null;
        char facing = 'X';
        int sign = 1;
        for (ItemFrame frame : frames) {
            MapId mapId = frame.getItem().get(DataComponents.MAP_ID);
            if (mapId == null) {
                continue;
            }
            MapItemSavedData mapData = client.level.getMapData(mapId);
            if (mapData == null || mapData.colors == null || mapData.colors.length <= 8256) {
                continue;
            }
            Direction direction = frame.getDirection();
            sign = direction == Direction.SOUTH || direction == Direction.WEST ? -1 : 1;
            BlockPos framePos = frame.blockPosition();
            boolean alongX = Math.abs(frame.getX() % 0.5D) < 1.0E-6D;
            facing = alongX ? 'X' : 'Z';
            int row = 0;
            for (int i = 2; i >= 0; i--) {
                int realI = i * sign;
                BlockPos candidate = alongX
                        ? framePos.offset(realI, 0, 0)
                        : framePos.offset(0, 0, realI);
                String id = blockId(client, candidate);
                if (id.contains("stone_button") || DungeonPuzzleBoardPolicy.isAir(fullBlockId(client, candidate))) {
                    leftmost = new DungeonTicTacToePolicy.BoardPos(
                            candidate.getX(), candidate.getY(), candidate.getZ());
                    row = i;
                    break;
                }
            }
            int column = 72 - (int) frame.getY();
            if (column < 0 || column > 2) {
                continue;
            }
            char mark = DungeonTicTacToePolicy.markFromMapColor(mapData.colors[8256]);
            if (mark != DungeonTicTacToePolicy.EMPTY) {
                board[DungeonTicTacToePolicy.boardIndex(column, row)] = mark;
            }
        }
        if (leftmost == null) {
            return;
        }
        for (int index : DungeonTicTacToePolicy.findBestMoves(
                board, DungeonTicTacToePolicy.PLAYER, DungeonTicTacToePolicy.OPPONENT)) {
            DungeonTicTacToePolicy.BoardPos pos =
                    DungeonTicTacToePolicy.indexToPos(index, leftmost, facing, sign);
            puzzleMarks.add(new Mark(blockBox(pos.x(), pos.y(), pos.z()), 0xFF22C55E));
        }
    }

    static void scanIcePath(Minecraft client) {
        if (!hashedRoomIs(client, "Ice Path") || client.level == null || client.player == null) {
            return;
        }
        DungeonRoomDataPolicy.Rotation rotation = currentHashedRotation(client).orElse(null);
        if (rotation == null) {
            return;
        }
        DungeonPuzzleBoardPolicy.BlockProbe probe = relativeBlockProbe(client, rotation);
        boolean[][] blocked = new boolean[DungeonIcePathPolicy.GRID][DungeonIcePathPolicy.GRID];
        for (int z = 0; z < DungeonIcePathPolicy.GRID; z++) {
            for (int x = 0; x < DungeonIcePathPolicy.GRID; x++) {
                DungeonPuzzleBoardPolicy.RelPos rel = DungeonIcePathPolicy.relPos(x, z);
                blocked[z][x] = !DungeonPuzzleBoardPolicy.isAir(probe.idAt(rel.x(), rel.y(), rel.z()));
            }
        }
        DungeonRoomDataPolicy.IntVec center = DungeonRoomDataPolicy.fromComp(15, 67, 15, rotation);
        AABB box = new AABB(
                center.x() - 10, 67, center.z() - 10,
                center.x() + 11, 68, center.z() + 11);
        Silverfish fish = null;
        for (Silverfish candidate : client.level.getEntitiesOfClass(Silverfish.class, box)) {
            fish = candidate;
            break;
        }
        if (fish == null) {
            return;
        }
        BlockPos fishPos = fish.blockPosition();
        DungeonRoomDataPolicy.IntVec rel = DungeonRoomDataPolicy.toComp(
                fishPos.getX(), fishPos.getY(), fishPos.getZ(), rotation);
        Optional<DungeonIcePathPolicy.GridPos> start =
                DungeonIcePathPolicy.gridFromRel(rel.x(), rel.z());
        if (start.isEmpty()) {
            return;
        }
        List<DungeonIcePathPolicy.GridPos> path = DungeonIcePathPolicy.expandPath(
                DungeonIcePathPolicy.solve(blocked, start.get().x(), start.get().z()));
        boolean first = true;
        for (DungeonIcePathPolicy.GridPos cell : path) {
            if (cell.equals(start.get())) {
                continue;
            }
            DungeonPuzzleBoardPolicy.RelPos worldRel = DungeonIcePathPolicy.relPos(cell.x(), cell.z());
            DungeonRoomDataPolicy.IntVec world = DungeonPuzzleBoardPolicy.world(worldRel, rotation);
            puzzleMarks.add(new Mark(
                    blockBox(world.x(), world.y(), world.z()),
                    first ? 0xFF22C55E : 0xFF38BDF8));
            first = false;
        }
    }

    static boolean blazeMaterialNear(Minecraft client, BlockPos origin, boolean ice) {
        if (client.level == null || origin == null) {
            return false;
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    String id = blockId(client, origin.offset(dx, dy, dz));
                    if (ice && DungeonAssistPolicy.isBlazeIceBlock(id)) {
                        return true;
                    }
                    if (!ice && DungeonAssistPolicy.isBlazeMagmaBlock(id)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static void pruneClickedSecrets() {
        long now = System.currentTimeMillis();
        clickedSecrets.removeIf(secret -> now >= secret.untilMs());
    }

    static void noteClickedSecret(BlockPos pos) {
        QolSkyblockExtras extras = extras();
        Minecraft client = Minecraft.getInstance();
        if (pos == null || client == null || client.level == null) {
            return;
        }
        if (!TempleDungeonPolicy.shouldBoxSecretClick(
                extras.dungeonEspEnabled && extras.dungeonEspSecretClicked,
                SkyBlockDungeonDetector.confidentlyInDungeon(),
                sidebar.boss(),
                extras.dungeonEspSecretClickedBoss,
                TempleDungeonPolicy.isSecretBlockId(fullBlockId(client, pos)))) {
            return;
        }
        for (ClickedSecret secret : clickedSecrets) {
            if (secret.x() == pos.getX() && secret.y() == pos.getY() && secret.z() == pos.getZ()) {
                return;
            }
        }
        long until = System.currentTimeMillis()
                + TempleDungeonPolicy.clampSecretStaySeconds(extras.dungeonEspSecretClickedSeconds) * 1000L;
        clickedSecrets.add(new ClickedSecret(pos.getX(), pos.getY(), pos.getZ(), until, false));
    }

    static void renderClickedSecrets(QolSkyblockExtras extras, Vec3 eye) {
        if (!extras.dungeonEspEnabled || !extras.dungeonEspSecretClicked) {
            return;
        }
        pruneClickedSecrets();
        for (ClickedSecret secret : clickedSecrets) {
            int color = secret.locked()
                    ? extras.dungeonEspSecretClickedLockedColor
                    : extras.dungeonEspSecretClickedColor;
            box(blockBox(secret.x(), secret.y(), secret.z()), color, extras, eye, true);
        }
    }

    static void highlightDungeonDrops(
            Minecraft client,
            LocalPlayer player,
            QolSkyblockExtras extras,
            Vec3 eye,
            AABB search) {
        if (!extras.dungeonEspEnabled || !extras.dungeonEspItems || client.level == null) {
            return;
        }
        for (ItemEntity item : client.level.getEntitiesOfClass(ItemEntity.class, search)) {
            ItemStack stack = item.getItem();
            String name = stack == null || stack.isEmpty() ? "" : stack.getHoverName().getString();
            if (!DungeonBladePolicy.highlightDungeonDrop(
                    true,
                    true,
                    sidebar.boss(),
                    name)) {
                continue;
            }
            int color = DungeonBladePolicy.dungeonDropColor(
                    item.distanceTo(player), item.tickCount);
            if (color == 0) {
                continue;
            }
            box(item.getBoundingBox().inflate(0.1D).move(0.0D, 0.05D, 0.0D), color, extras, eye);
        }
    }

    static void scanKeyDrops(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonAnnounceEnabled
                || !extras.dungeonAnnounceKeyDrop
                || !SkyBlockDungeonDetector.confidentlyInDungeon()
                || sidebar.boss()
                || client.level == null
                || client.player == null) {
            if (!SkyBlockDungeonDetector.confidentlyInDungeon() || sidebar.boss()) {
                droppedKey = TempleDungeonPolicy.KeySkull.NONE;
                seenKeyStands.clear();
            }
            return;
        }
        if (droppedKey != TempleDungeonPolicy.KeySkull.NONE) {
            return;
        }
        AABB search = client.player.getBoundingBox().inflate(DungeonPolicy.ESP_SCAN_RANGE);
        for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
            if (seenKeyStands.contains(stand.getId())) {
                continue;
            }
            TempleDungeonPolicy.KeySkull skull = TempleDungeonPolicy.keySkull(helmetUuid(stand));
            if (skull == TempleDungeonPolicy.KeySkull.NONE) {
                continue;
            }
            seenKeyStands.add(stand.getId());
            droppedKey = skull;
            if (TempleDungeonPolicy.keyDropClass(
                    sidebar.dungeonClass(), extras.dungeonAnnounceKeyDropAll)) {
                String title = TempleDungeonPolicy.keyDropTitle(skull);
                showTitle(client, true, "§e" + title);
                client.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 2.0F, 0.5F);
            }
            return;
        }
    }

    static String helmetUuid(ArmorStand stand) {
        ItemStack head = stand.getItemBySlot(EquipmentSlot.HEAD);
        if (head == null || head.isEmpty()) {
            return "";
        }
        ResolvableProfile profile = head.get(DataComponents.PROFILE);
        if (profile == null || profile.partialProfile() == null || profile.partialProfile().id() == null) {
            return "";
        }
        return profile.partialProfile().id().toString();
    }

    private record ClickedSecret(int x, int y, int z, long untilMs, boolean locked) {
    }

    private record Mark(AABB box, int color) {
    }
}
