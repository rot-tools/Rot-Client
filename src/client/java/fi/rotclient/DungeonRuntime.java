package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
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
    private static DungeonPolicy.Sidebar sidebar =
            DungeonPolicy.parseSidebar(List.of());
    private static long bonzoUntil;
    private static long spiritUntil;
    private static long phoenixUntil;
    private static long terracottaUntil;
    private static DungeonAssistPolicy.F7Timer f7Timer = DungeonAssistPolicy.F7Timer.NONE;
    private static long f7TimerUntil;
    private static int requeueTicks = -1;
    private static int terminalCooldown;
    private static int simonCooldown;
    private static int puzzleScanTicks;
    private static String lastAnnounce = "";
    private static String lastF7Title = "";
    private static String lastQuiz = "";
    private static String lastRagnarock = "";
    private static String lastMelody = "";
    private static String lastWeirdoNpc = "";
    private static int lastBreakerCharges = -1;
    private static final Map<String, String> blessings = new LinkedHashMap<>();
    private static final Map<String, String> tpLinks = new LinkedHashMap<>();
    private static final List<Mark> puzzleMarks = new ArrayList<>();
    private static DungeonPuzzlePolicy.MapPreview mapPreview =
            new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
    private static DungeonPuzzlePolicy.WorldCell lastPad;
    private static Vec3 lastPlayerPos;
    private static int lastScore = -1;
    private static int lastSecretsFound = -1;
    private static String lastDuplicateTitle = "";
    private static String lastLivid = "";
    private static long lividUntil;
    private static boolean bloodCampActive;
    private static String lastLeapRegion = "";
    private static final Set<Integer> leapedIds = new HashSet<>();
    private static EmberDungeonPolicy.DebuffPhase debuffPhase = EmberDungeonPolicy.DebuffPhase.NONE;
    private static long debuffUntil;
    private static boolean debuffFired;
    private static long puzzleStarted;
    private static final List<DungeonRoomDataPolicy.PlacedWaypoint> secretWaypoints = new ArrayList<>();
    private static String lastRelic = "";
    private static final List<EmberDungeonPolicy.ArrowClicks> arrowClicks = new ArrayList<>();
    private static int i4Cooldown;
    private static int i4Timer = -1;
    private static boolean i4RodUsed;
    private static boolean i4MaskUsed;
    private static boolean i4LeapUsed;
    private static int i4LeapWait;
    private static String melodyLeapName = "";
    private static long relicLookStart;
    private static DungeonF7Policy.LookAim relicLookFrom;
    private static DungeonF7Policy.LookAim relicLookTo;
    private static long i4LookStart;
    private static DungeonF7Policy.LookAim i4LookFrom;
    private static BlockPos i4LookTarget;
    private static final List<DungeonPolicy.TerminalClick> melodySkipQueue = new ArrayList<>();
    private static DungeonMapPolicy.Board lastMapBoard =
            new DungeonMapPolicy.Board(DungeonMapPolicy.Calibration.none(), List.of(), List.of(), List.of(), "");
    private static final java.util.Queue<DungeonLeftoverPolicy.QueuedClick> termQueue =
            DungeonLeftoverPolicy.newQueue();
    private static DungeonLeftoverPolicy.SplitSnapshot splits =
            new DungeonLeftoverPolicy.SplitSnapshot(0L, 0L, 0L, 0L, false, false, false);
    private static KuudraSplitPolicy.Snapshot kuudra = KuudraSplitPolicy.Snapshot.idle();
    private static List<String> chestProfitHud = List.of();
    private static int superboomCooldown;
    private static int superboomOriginalSlot = -1;
    private static int superboomSwapBackTicks = -1;
    private static long triggerLastMs;
    private static int ghostCooldown;
    private static long crystalSpawnUntil;
    private static long crystalPickupAt;
    private static String lastCrystalHud = "";
    private static long relicSpawnUntil;
    private static long relicPickupAt;
    private static final List<EmberDungeonPolicy.IntVec> simonOrder = new ArrayList<>();
    private static final List<EmberDungeonPolicy.IntVec> simonRemaining = new ArrayList<>();
    private static String lastDragonHud = "";
    private static boolean melodyWasOpen;
    private static int lastTerminalSlot = 22;
    private static String lastTermTitle = "";
    private static long terminalOpenedAt;
    private static boolean closeChestArmed;
    private static int p3Terminals;
    private static int p3Devices;
    private static int p3Levers;
    private static long dragonSpawnUntil;
    private static int goldorFrenzyTicks;
    private static int purplePadTicks;

    private DungeonRuntime() {
    }

    static void tick(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        QolSkyblockExtras extras = extras();
        List<String> lines = sidebarLines();
        sidebar = DungeonPolicy.parseSidebar(lines);
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceScoreTitle
                && DungeonPolicy.crossedScoreMilestone(
                        lastScore, sidebar.score(), extras.dungeonAnnounceScoreThreshold)) {
            showTitle(client, true, "§e" + DungeonAssistPolicy.scoreTitleText(
                    sidebar.score(), extras.dungeonAnnounceScoreThreshold));
        }
        lastScore = sidebar.score();
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceSecretChime
                && SkyBlockDungeonDetector.confidentlyInDungeon()
                && DungeonPolicy.secretCountIncreased(lastSecretsFound, sidebar.secretsFound())
                && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.8F, 1.4F);
        }
        if (SkyBlockDungeonDetector.confidentlyInDungeon()) {
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
        maybeCloseChest(client, extras);
        observeTerminalOpen(client, extras);
        if (goldorFrenzyTicks > 0) {
            goldorFrenzyTicks--;
        }
        if (purplePadTicks > 0) {
            purplePadTicks--;
        }
        refreshChestProfitHud(client, extras);
        HateDoorsRuntime.scan(client, extras);
        if (extras.dungeonF7Enabled && extras.dungeonF7DebuffAuto && !debuffFired
                && debuffPhase != EmberDungeonPolicy.DebuffPhase.NONE) {
            if (useDebuffItem(client, extras)) {
                debuffFired = true;
            }
        }
        if (puzzleScanTicks++ % 8 == 0) {
            scanPuzzles(client, extras);
            scanDungeonMap(client, extras);
            if (extras.dungeonF7Enabled && extras.dungeonF7ArrowAlign) {
                scanArrowAlign(client);
            }
            if (extras.dungeonF7Enabled && extras.dungeonF7Simon) {
                scanSimon(client);
            }
            if (extras.dungeonEspEnabled && extras.dungeonEspSecretWaypoints) {
                scanSecretWaypoints(client);
            }
        }
        if (extras.dungeonTerminalsEnabled && extras.dungeonTerminalsAuto) {
            autoClickTerminal(client);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SimonAuto) {
            autoSimon(client);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7SimonTrigger) {
            autoSimonNext(client);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7AutoI4) {
            tickAutoI4(client, extras);
            autoI4(client);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7RelicLook) {
            relicLook(client, extras, now);
        }
        updateDragonHud(client, extras);
        if (extras.dungeonF7Enabled && extras.dungeonF7AutoSuperboom) {
            autoSuperboom(client, extras);
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspGhostBlock) {
            ghostBlocks(client, extras);
        }
        if (extras.dungeonEspEnabled && extras.dungeonEspTriggerBot) {
            triggerBot(client, extras);
        }
        if (extras.dungeonTerminalsEnabled && extras.dungeonTerminalsQueue) {
            flushTermQueue(client, extras);
        } else if (!termQueue.isEmpty()) {
            termQueue.clear();
            melodySkipQueue.clear();
        }
        observeTeleport(client.player);
        observeLeaps(client, extras);
        updateCrystalHud(client, extras, now);
    }

    static void onChat(Component message) {
        if (message == null) {
            return;
        }
        String raw = message.getString();
        QolSkyblockExtras extras = extras();
        long now = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        DungeonPolicy.Invincibility inv = DungeonPolicy.invincibilityFromChat(raw);
        if (inv != DungeonPolicy.Invincibility.NONE) {
            long until = now + DungeonPolicy.invincibilityMillis(inv);
            switch (inv) {
                case BONZO -> bonzoUntil = until;
                case SPIRIT -> spiritUntil = until;
                case PHOENIX -> phoenixUntil = until;
                default -> {
                }
            }
        }
        if (DungeonPolicy.isTerracottaChat(raw)) {
            terracottaUntil = now + DungeonPolicy.TERRACOTTA_MILLIS;
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
        if (extras.dungeonRequeueEnabled && DungeonPolicy.isDungeonEnd(raw)) {
            requeueTicks = Math.max(0, extras.dungeonRequeueDelay);
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
            relicSpawnUntil = now + DungeonF7Policy.RELIC_SPAWN_MILLIS;
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Crystals) {
            if (DungeonF7Policy.crystalSpawnChat(raw)) {
                crystalSpawnUntil = now + DungeonF7Policy.CRYSTAL_RESPAWN_MILLIS;
            }
            if (DungeonF7Policy.crystalPickup(raw)) {
                crystalPickupAt = now;
            }
        }
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
        });
        if (DungeonF7Policy.p3GateDestroyed(raw)) {
            p3Terminals = 0;
            p3Devices = 0;
            p3Levers = 0;
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7DragonTimer
                && DungeonF7Policy.dragonSpawnChat(raw)) {
            dragonSpawnUntil = now + DungeonF7Policy.DRAGON_SPAWN_MILLIS;
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
        AutoDojoRuntime.onChat(raw);
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceAutoUlt
                && EmberDungeonPolicy.shouldFireUltimate(raw, sidebar.floor(), sidebar.dungeonClass())) {
            dropClassUltimate(client);
            showTitle(client, true, "§dUsed Ultimate!");
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
            DungeonAssistPolicy.f7TitleText(raw).ifPresent(title -> {
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
        if (extras.dungeonPuzzlesEnabled && extras.dungeonPuzzlesQuiz) {
            DungeonAssistPolicy.quizAnswer(raw).ifPresent(answer -> {
                lastQuiz = answer;
                showTitle(client, true, "§a" + answer);
            });
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
        lastRagnarock = "";
        lastMelody = "";
        lastWeirdoNpc = "";
        lastBreakerCharges = -1;
        lastScore = -1;
        lastSecretsFound = -1;
        lastDuplicateTitle = "";
        lastLivid = "";
        lividUntil = 0L;
        bloodCampActive = false;
        lastLeapRegion = "";
        leapedIds.clear();
        debuffPhase = EmberDungeonPolicy.DebuffPhase.NONE;
        debuffUntil = 0L;
        debuffFired = false;
        puzzleStarted = 0L;
        secretWaypoints.clear();
        HateDoorsRuntime.clear();
        lastRelic = "";
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
        simonOrder.clear();
        simonRemaining.clear();
        lastDragonHud = "";
        melodyWasOpen = false;
        lastTerminalSlot = 22;
        lastTermTitle = "";
        terminalOpenedAt = 0L;
        closeChestArmed = false;
        p3Terminals = 0;
        p3Devices = 0;
        p3Levers = 0;
        dragonSpawnUntil = 0L;
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
        chestProfitHud = List.of();
        lastMapBoard = new DungeonMapPolicy.Board(
                DungeonMapPolicy.Calibration.none(), List.of(), List.of(), List.of(), "");
        AutoDojoRuntime.onWorldChanged();
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
            return List.of();
        }
        List<String> lines = new ArrayList<>();
        lines.add("Dungeon");
        if (extras.dungeonHudFloor && !sidebar.floor().isEmpty()) {
            lines.add("Floor " + sidebar.floor());
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
        if (extras.dungeonF7Enabled && extras.dungeonF7Crystals) {
            addTimer(lines, "Crystal spawn", crystalSpawnUntil, now);
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
        if (extras.dungeonHudQuiz && extras.dungeonPuzzlesEnabled && !lastQuiz.isEmpty()) {
            lines.add(lastQuiz);
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
        if (extras.dungeonF7Enabled && extras.dungeonF7SimonProgress && !simonRemaining.isEmpty()) {
            lines.add("Simon " + simonRemaining.size());
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons && !lastDragonHud.isEmpty()) {
            lines.add(lastDragonHud);
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7DragonTimer && dragonSpawnUntil > now) {
            lines.add(DungeonF7Policy.dragonSpawnLine(dragonSpawnUntil, now));
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7P3Display
                && (p3Terminals > 0 || p3Devices > 0 || p3Levers > 0)) {
            lines.add(DungeonF7Policy.p3HudLine(p3Terminals, p3Devices, p3Levers));
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

    public static boolean enqueueTerminalClick(int slot, int button) {
        QolSkyblockExtras extras = extras();
        if (!extras.dungeonTerminalsEnabled || !extras.dungeonTerminalsQueue) {
            return false;
        }
        DungeonLeftoverPolicy.enqueue(termQueue, slot, button);
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
        AABB search = player.getBoundingBox().inflate(48.0D);
        Vec3 eye = player.getEyePosition();
        if (extras.dungeonEspEnabled) {
            boolean needNamed = needsNamedDungeonEsp(extras);
            if (extras.dungeonEspBats || extras.dungeonEspTeammates || needNamed) {
                List<BlazeMark> blazes = new ArrayList<>();
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
                        considerNamedEsp(living, extras, eye, blazes);
                    }
                }
                if (!blazes.isEmpty()) {
                    blazes.sort(Comparator.comparingInt(BlazeMark::health));
                    box(blazes.getFirst().box(), 0xFF22C55E, extras, eye);
                    if (blazes.size() > 1) {
                        box(blazes.get(1).box(), 0xFFFACC15, extras, eye);
                    }
                }
            }
        }
        if ((extras.dungeonEspEnabled && extras.dungeonEspSimon)
                || (extras.dungeonF7Enabled && extras.dungeonF7Simon)) {
            highlightSimon(client, player, extras, eye);
        }
        if (extras.dungeonF7Enabled && !extras.dungeonF7HideDiorite) {
            markDiorite(client, player, extras, eye);
        }
        highlightEmberWorld(client, player, extras, eye, search);
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
            DungeonPolicy.DungeonClass dungeonClass =
                    DungeonPolicy.classFromLore(InventoryChromeRuntime.loreLines(slot.getItem()));
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
            return tint != 0 ? tint : (melodyOverlay(melody, slotIndex) ? extras.dungeonTerminalsColor : 0);
        }
        List<DungeonPolicy.TerminalClick> clicks =
                DungeonPolicy.solveTerminalClicks(terminal, title, items);
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
        if (terminal == DungeonPolicy.Terminal.NUMBERS && extras.dungeonTerminalsNumbersShow) {
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
        return extras.dungeonTerminalsColor;
    }

    private static void autoClickTerminal(Minecraft client) {
        if (terminalCooldown > 0
                || client.gameMode == null
                || client.player == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            lastMelody = "";
            melodySkipQueue.clear();
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        DungeonPolicy.Terminal terminal = DungeonPolicy.detectTerminal(title);
        QolSkyblockExtras extras = extras();
        if (terminal == DungeonPolicy.Terminal.NONE
                || !DungeonPolicy.shouldAutoSolve(
                terminal,
                extras.dungeonTerminalsAutoMelody,
                extras.dungeonTerminalsAutoNumbers,
                extras.dungeonTerminalsAutoColors,
                extras.dungeonTerminalsAutoRubix,
                extras.dungeonTerminalsAutoPanes,
                extras.dungeonTerminalsAutoStarts)) {
            return;
        }
        List<DungeonPolicy.TerminalItem> items = snapshot(screen);
        if (!melodySkipQueue.isEmpty()) {
            sendTerminalClick(client, screen, extras, melodySkipQueue.removeFirst());
            return;
        }
        List<DungeonPolicy.TerminalClick> clicks =
                DungeonPolicy.solveTerminalClicks(terminal, title, items);
        if (clicks.isEmpty()) {
            return;
        }
        if (extras.dungeonTerminalsHumanOrder) {
            clicks = DungeonPolicy.preferHumanClick(clicks, lastTerminalSlot);
        }
        DungeonPolicy.TerminalClick click = clicks.getFirst();
        if (terminal == DungeonPolicy.Terminal.MELODY) {
            DungeonPolicy.MelodyState melody = DungeonPolicy.parseMelody(items);
            melodySkipQueue.addAll(DungeonPolicy.melodySkipClicks(
                    melody,
                    extras.dungeonTerminalsMelodySkip,
                    extras.dungeonTerminalsMelodySkipFirstRow,
                    extras.dungeonTerminalsMelodySkipMode));
        }
        sendTerminalClick(client, screen, extras, click);
    }

    private static void sendTerminalClick(
            Minecraft client,
            AbstractContainerScreen<?> screen,
            QolSkyblockExtras extras,
            DungeonPolicy.TerminalClick click) {
        if (TermSimRuntime.isOpen()) {
            TermSimRuntime.click(click.slot(), click.button());
            terminalCooldown = Math.max(1, extras.dungeonTerminalsDelay);
            lastTerminalSlot = click.slot();
            return;
        }
        if (extras.dungeonTerminalsQueue) {
            DungeonLeftoverPolicy.enqueue(termQueue, click.slot(), click.button());
            lastTerminalSlot = click.slot();
            return;
        }
        int packetButton;
        ContainerInput input;
        if (click.button() == 1) {
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
        if (extras.dungeonTerminalsSounds && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.35F, 1.2F);
        }
        terminalCooldown = Math.max(1, extras.dungeonTerminalsDelay);
        lastTerminalSlot = click.slot();
    }

    private static void autoSimon(Minecraft client) {
        if (simonCooldown > 0
                || client.gameMode == null
                || client.player == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (!DungeonPolicy.isSimonStart(pos.getX(), pos.getY(), pos.getZ())) {
            return;
        }
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        simonCooldown = 3;
    }

    private static void autoI4(Minecraft client) {
        if (i4Cooldown > 0 || client.gameMode == null || client.player == null || client.level == null) {
            return;
        }
        if (!EmberDungeonPolicy.isOnI4Device(
                client.player.getX(), client.player.getY(), client.player.getZ())) {
            return;
        }
        List<EmberDungeonPolicy.IntVec> remaining = new ArrayList<>();
        for (EmberDungeonPolicy.IntVec vec : EmberDungeonPolicy.i4Blocks()) {
            BlockPos pos = new BlockPos(vec.x(), vec.y(), vec.z());
            if (EmberDungeonPolicy.isI4Lit(blockId(client, pos))) {
                remaining.add(vec);
            }
        }
        Optional<EmberDungeonPolicy.IntVec> next = DungeonF7Policy.nextLitI4(remaining);
        if (next.isEmpty()) {
            i4LookStart = 0L;
            i4LookTarget = null;
            return;
        }
        BlockPos pos = new BlockPos(next.get().x(), next.get().y(), next.get().z());
        long now = System.currentTimeMillis();
        QolSkyblockExtras extras = extras();
        if (i4LookTarget == null || !i4LookTarget.equals(pos)) {
            i4LookTarget = pos;
            i4LookFrom = new DungeonF7Policy.LookAim(client.player.getYRot(), client.player.getXRot());
            i4LookStart = now;
        }
        DungeonF7Policy.LookAim to = DungeonF7Policy.aimAt(
                client.player.getX(),
                client.player.getY() + client.player.getEyeHeight(),
                client.player.getZ(),
                pos.getX() + 0.5D,
                pos.getY() + 0.5D,
                pos.getZ() + 0.5D);
        int duration = DungeonF7Policy.clampI4RotationMs(extras.dungeonF7AutoI4Rotation);
        double progress = duration <= 0 ? 1.0D : (now - i4LookStart) / (double) duration;
        DungeonF7Policy.LookAim from = i4LookFrom == null
                ? new DungeonF7Policy.LookAim(client.player.getYRot(), client.player.getXRot())
                : i4LookFrom;
        DungeonF7Policy.LookAim aim = DungeonF7Policy.lerpLook(from, to, progress);
        client.player.setYRot(aim.yaw());
        client.player.setXRot(aim.pitch());
        client.player.yRotO = aim.yaw();
        client.player.xRotO = aim.pitch();
        if (progress < 1.0D) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        BlockHitResult hit = new BlockHitResult(center, Direction.WEST, pos, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        i4Cooldown = 4;
        i4LookStart = 0L;
        i4LookTarget = null;
    }

    private static void tickAutoI4(Minecraft client, QolSkyblockExtras extras) {
        if (client.player == null || client.gameMode == null || i4Timer < 0) {
            return;
        }
        if (!EmberDungeonPolicy.isOnI4Device(
                client.player.getX(), client.player.getY(), client.player.getZ())) {
            return;
        }
        int tick = ++i4Timer;
        if (!i4RodUsed && extras.dungeonF7AutoI4Rod && DungeonF7Policy.i4Action(tick, DungeonF7Policy.I4_ROD_TICK)) {
            i4RodUsed = useNamedItem(client, "fishing_rod", "fishing rod");
        }
        if (!i4MaskUsed && extras.dungeonF7AutoI4Mask && DungeonF7Policy.i4Action(tick, DungeonF7Policy.I4_MASK_TICK)) {
            i4MaskUsed = useNamedItem(client, "bonzo", "spirit mask");
        }
        if (!i4LeapUsed && extras.dungeonF7AutoI4Leap && DungeonF7Policy.i4Action(tick, DungeonF7Policy.I4_LEAP_TICK)) {
            i4LeapUsed = useNamedItem(client, "spirit leap", "leap");
            i4LeapWait = 8;
        }
        if (i4LeapWait > 0) {
            i4LeapWait--;
            clickLeapMenu(client, extras);
        }
        if (tick > DungeonF7Policy.I4_LEAP_TICK + 40) {
            i4Timer = -1;
        }
    }

    private static void relicLook(Minecraft client, QolSkyblockExtras extras, long now) {
        if (client.player == null || relicLookFrom == null || relicLookTo == null || relicLookStart <= 0L) {
            return;
        }
        ItemStack held = client.player.getMainHandItem();
        String name = held == null || held.isEmpty() ? "" : held.getHoverName().getString();
        if (!DungeonF7Policy.holdingRelic(name, lastRelic.replace(" Relic", ""))) {
            relicLookStart = 0L;
            return;
        }
        int duration = DungeonF7Policy.clampRelicLookMs(extras.dungeonF7RelicLookTime);
        double progress = (now - relicLookStart) / (double) duration;
        DungeonF7Policy.LookAim aim = DungeonF7Policy.lerpLook(relicLookFrom, relicLookTo, progress);
        client.player.setYRot(aim.yaw());
        client.player.setXRot(aim.pitch());
        client.player.yRotO = aim.yaw();
        client.player.xRotO = aim.pitch();
        if (progress >= 1.0D) {
            relicLookStart = 0L;
        }
    }

    private static void updateDragonHud(Minecraft client, QolSkyblockExtras extras) {
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

    private static boolean useNamedItem(Minecraft client, String... needles) {
        if (client.player == null || client.gameMode == null || needles == null) {
            return false;
        }
        LocalPlayer player = client.player;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String blob = (itemId(stack) + " " + stack.getHoverName().getString()).toLowerCase(Locale.ROOT);
            boolean match = false;
            for (String needle : needles) {
                if (needle != null && blob.contains(needle.toLowerCase(Locale.ROOT))) {
                    match = true;
                    break;
                }
            }
            if (!match) {
                continue;
            }
            int selected = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.getInventory().setSelectedSlot(selected);
            return true;
        }
        return false;
    }

    private static void clickLeapMenu(Minecraft client, QolSkyblockExtras extras) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen) || client.gameMode == null) {
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (!DungeonPolicy.isLeapMenu(title)) {
            return;
        }
        DungeonPolicy.DungeonClass preferred = DungeonF7Policy.leapClass(extras.dungeonF7AutoI4LeapClass);
        int melodySlot = -1;
        int classSlot = -1;
        int anySlot = -1;
        for (Slot slot : screen.getMenu().slots) {
            if (slot == null || slot.getItem().isEmpty() || slot.index >= 54) {
                continue;
            }
            String name = slot.getItem().getHoverName().getString();
            DungeonPolicy.DungeonClass dungeonClass =
                    DungeonPolicy.classFromLore(InventoryChromeRuntime.loreLines(slot.getItem()));
            if (anySlot < 0) {
                anySlot = slot.index;
            }
            if (extras.dungeonF7AutoI4LeapMelody && !melodyLeapName.isBlank()
                    && name.equalsIgnoreCase(melodyLeapName)) {
                melodySlot = slot.index;
            }
            if (classSlot < 0 && dungeonClass == preferred) {
                classSlot = slot.index;
            }
        }
        int chosen = melodySlot >= 0 ? melodySlot : (classSlot >= 0 ? classSlot : anySlot);
        if (chosen < 0) {
            return;
        }
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId,
                chosen,
                0,
                ContainerInput.PICKUP,
                client.player);
        i4LeapWait = 0;
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }

    private static void scanArrowAlign(Minecraft client) {
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

    private static void observeLeaps(Minecraft client, QolSkyblockExtras extras) {
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

    private static void scanSecretWaypoints(Minecraft client) {
        secretWaypoints.clear();
        if (client.player == null || client.level == null) {
            return;
        }
        int cx = DungeonRoomDataPolicy.roomCenter(client.player.blockPosition().getX());
        int cz = DungeonRoomDataPolicy.roomCenter(client.player.blockPosition().getZ());
        List<String> column = new ArrayList<>();
        for (int y = DungeonRoomDataPolicy.HASH_Y_TOP; y >= DungeonRoomDataPolicy.HASH_Y_BOTTOM; y--) {
            column.add(fullBlockId(client, new BlockPos(cx, y, cz)));
        }
        int core = DungeonRoomDataPolicy.hashColumn(column);
        DungeonRoomDataPolicy.roomForCore(core).ifPresent(room -> {
            DungeonRoomDataPolicy.Rotation rotation = null;
            for (DungeonRoomDataPolicy.Rotation candidate :
                    DungeonRoomDataPolicy.candidateRotations(cx, cz)) {
                if (DungeonRoomDataPolicy.isBlueTerracotta(fullBlockId(client, new BlockPos(
                        candidate.cornerX(),
                        client.player.blockPosition().getY(),
                        candidate.cornerZ())))
                        || DungeonRoomDataPolicy.isBlueTerracotta(fullBlockId(client, new BlockPos(
                        candidate.cornerX(), 69, candidate.cornerZ())))) {
                    rotation = candidate;
                    break;
                }
            }
            if (rotation == null) {
                rotation = DungeonRoomDataPolicy.candidateRotations(cx, cz).getFirst();
            }
            secretWaypoints.addAll(DungeonRoomDataPolicy.placeSecrets(room, rotation));
        });
    }

    private static void dropClassUltimate(Minecraft client) {
        if (client == null || client.player == null) {
            return;
        }
        LocalPlayer player = client.player;
        if (player.connection != null) {
            player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(
                    net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.DROP_ITEM,
                    BlockPos.ZERO,
                    net.minecraft.core.Direction.DOWN));
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (EmberDungeonPolicy.isClassUltimateItem(
                    stack.getHoverName().getString(),
                    InventoryChromeRuntime.loreLines(stack))) {
                int selected = player.getInventory().getSelectedSlot();
                player.getInventory().setSelectedSlot(slot);
                player.drop(false);
                player.getInventory().setSelectedSlot(selected);
                return;
            }
        }
    }

    private static boolean useDebuffItem(Minecraft client, QolSkyblockExtras extras) {
        if (client == null || client.player == null || client.gameMode == null) {
            return false;
        }
        LocalPlayer player = client.player;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            String name = stack.getHoverName().getString();
            List<String> lore = InventoryChromeRuntime.loreLines(stack);
            boolean ice = extras.dungeonF7DebuffIce && EmberDungeonPolicy.isIceSprayItem(name, lore);
            boolean gravity = extras.dungeonF7DebuffGravity && EmberDungeonPolicy.isGravityWandItem(name, lore);
            if (!ice && !gravity) {
                continue;
            }
            int selected = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(slot);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            player.getInventory().setSelectedSlot(selected);
            return true;
        }
        return false;
    }

    private static void highlightEmberWorld(
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
            for (DungeonRoomDataPolicy.PlacedWaypoint waypoint : secretWaypoints) {
                BlockPos pos = new BlockPos(waypoint.x(), waypoint.y(), waypoint.z());
                if (player.blockPosition().closerThan(pos, 48.0D)) {
                    box(blockBox(pos), DungeonRoomDataPolicy.secretColor(waypoint.kind()), extras, eye);
                }
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
            for (ArmorStand stand : client.level.getEntitiesOfClass(ArmorStand.class, search)) {
                if (!EmberDungeonPolicy.isInactiveTerminal(entityName(stand))) {
                    continue;
                }
                if (EmberDungeonPolicy.terminalSection(stand.getX(), stand.getY(), stand.getZ()) > 0) {
                    box(stand.getBoundingBox(), extras.dungeonTerminalsHitboxColor, extras, eye);
                }
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7I4
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
                }
            }
        }
        if (extras.dungeonF7Enabled && extras.dungeonF7Dragons && extras.dungeonF7DragonBoxes
                && player.getY() < 30.0D) {
            for (DungeonF7Policy.DragonPad pad : DungeonF7Policy.dragonPads()) {
                EmberDungeonPolicy.Aabb box = pad.box();
                box(new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()),
                        pad.color(), extras, eye);
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

    static List<DungeonMapPolicy.PlayerIcon> mapPlayers() {
        return lastMapBoard.players();
    }

    private static boolean melodyOverlay(DungeonPolicy.MelodyState melody, int slot) {
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

    private static int salvageColor(AbstractContainerScreen<?> screen, int slotIndex, QolSkyblockExtras extras) {
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

    private static int partyFinderColor(
            AbstractContainerScreen<?> screen, int slotIndex, QolSkyblockExtras extras) {
        if (slotIndex >= screen.getMenu().slots.size()) {
            return 0;
        }
        Slot slot = screen.getMenu().slots.get(slotIndex);
        if (slot == null || slot.getItem().isEmpty()) {
            return 0;
        }
        return DungeonAssistPolicy.partyFinderMatches(
                InventoryChromeRuntime.loreLines(slot.getItem()), extras.dungeonMenusPartyCata)
                ? extras.dungeonMenusProfitColor
                : 0;
    }

    private static int chestColor(AbstractContainerScreen<?> screen, int slotIndex, QolSkyblockExtras extras) {
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

    private static List<DungeonPolicy.TerminalItem> snapshot(AbstractContainerScreen<?> screen) {
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

    private static void scanPuzzles(Minecraft client, QolSkyblockExtras extras) {
        puzzleMarks.clear();
        if (!extras.dungeonPuzzlesEnabled || !SkyBlockDungeonDetector.confidentlyInDungeon()) {
            return;
        }
        LocalPlayer player = client.player;
        BlockPos origin = player.blockPosition();
        if (extras.dungeonPuzzlesIce) {
            scanIceFill(client, origin);
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
        if (extras.dungeonPuzzlesWeirdos && !lastWeirdoNpc.isEmpty()) {
            scanWeirdoChest(client, player, origin);
        }
    }

    private static void scanIceFill(Minecraft client, BlockPos origin) {
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

    private static void scanWaterBoard(Minecraft client, BlockPos origin) {
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

    private static void scanBoulder(Minecraft client, BlockPos origin) {
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

    private static void scanTpMaze(Minecraft client, BlockPos origin) {
        List<DungeonPuzzlePolicy.WorldCell> pads = new ArrayList<>();
        DungeonPuzzlePolicy.WorldCell chest = null;
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
        if (pads.size() < 2) {
            return;
        }
        DungeonPuzzlePolicy.WorldCell start = new DungeonPuzzlePolicy.WorldCell(
                origin.getX(), origin.getY(), origin.getZ());
        List<DungeonPuzzlePolicy.WorldCell> path = DungeonPuzzlePolicy.teleportPath(pads, tpLinks, start, chest);
        int shown = 0;
        for (DungeonPuzzlePolicy.WorldCell pad : path) {
            if (shown++ > 8) {
                break;
            }
            puzzleMarks.add(new Mark(blockBox(pad.x(), pad.y(), pad.z()), 0xFFA855F7));
        }
    }

    private static void scanWeirdoChest(Minecraft client, LocalPlayer player, BlockPos origin) {
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

    private static void scanDungeonMap(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonHudEnabled || !extras.dungeonHudMap || client.player == null
                || (extras.dungeonHudMapHideBoss && sidebar.boss())) {
            mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
            return;
        }
        for (ItemStack stack : client.player.getInventory().getNonEquipmentItems()) {
            if (stack == null || stack.isEmpty() || !stack.is(Items.FILLED_MAP)) {
                continue;
            }
            MapId mapId = stack.get(DataComponents.MAP_ID);
            if (mapId == null) {
                continue;
            }
            MapItemSavedData data = client.level.getMapData(mapId);
            if (data == null || data.colors == null) {
                continue;
            }
            List<DungeonMapPolicy.PlayerIcon> icons = mapIcons(data, extras);
            DungeonMapPolicy.Board board = DungeonMapPolicy.scan(
                    data.colors,
                    128,
                    icons,
                    extras.dungeonHudMapPlayers ? client.player.getX() : null,
                    extras.dungeonHudMapPlayers ? client.player.getZ() : null);
            if (board.calibration().ok()) {
                if (extras.dungeonHudCheaterMap) {
                    board = DungeonLeftoverPolicy.revealHiddenRooms(board);
                }
                lastMapBoard = board;
                mapPreview = DungeonMapPolicy.render(
                        data.colors,
                        128,
                        board,
                        extras.dungeonHudMapDoors,
                        extras.dungeonHudMapPlayers,
                        extras.dungeonHudCheaterMap && extras.dungeonHudCheaterDarken,
                        extras.dungeonHudCheaterDarkenFactor);
            } else {
                mapPreview = DungeonPuzzlePolicy.previewDungeonMap(data.colors, 128, 21);
            }
            return;
        }
        mapPreview = new DungeonPuzzlePolicy.MapPreview(0, 0, new int[0], -1, -1, "");
    }

    private static void observeTeleport(LocalPlayer player) {
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

    private static void highlightSimon(
            Minecraft client, LocalPlayer player, QolSkyblockExtras extras, Vec3 eye) {
        BlockPos start = new BlockPos(110, 121, 91);
        if (!player.blockPosition().closerThan(start, 48.0D)) {
            return;
        }
        box(blockBox(start), extras.dungeonEspSimonColor, extras, eye);
        List<EmberDungeonPolicy.IntVec> remaining =
                simonRemaining.isEmpty() ? simonOrder : simonRemaining;
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

    private static void markDiorite(
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

    private static void box(AABB box, int color, QolSkyblockExtras extras, Vec3 eye) {
        int fill = extras.dungeonEspFill
                ? withAlpha(color, (int) Math.round(DungeonAssistPolicy.clampOpacity(extras.dungeonEspOpacity) * 2.55D))
                : 0;
        var props = Gizmos.cuboid(box, GizmoStyle.strokeAndFill(color, 2.0F, fill));
        if (!extras.dungeonEspDepth) {
            props.setAlwaysOnTop();
        }
        if (extras.dungeonEspTracers) {
            var line = Gizmos.line(eye, box.getCenter(), color);
            if (!extras.dungeonEspDepth) {
                line.setAlwaysOnTop();
            }
        }
    }

    private static boolean needsNamedDungeonEsp(QolSkyblockExtras extras) {
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
                || (extras.dungeonPuzzlesEnabled && extras.dungeonPuzzlesBlaze)
                || (extras.dungeonF7Enabled && extras.dungeonF7WitherEsp);
    }

    private static void considerNamedEsp(
            Entity entity,
            QolSkyblockExtras extras,
            Vec3 eye,
            List<BlazeMark> blazes) {
        String name = entityName(entity);
        String lower = name.toLowerCase(Locale.ROOT);
        if (extras.dungeonEspSecrets && lower.contains("secret")) {
            int secretColor = lower.contains("chest")
                    ? extras.dungeonEspChestColor
                    : extras.dungeonEspSecretColor;
            box(entity.getBoundingBox(), secretColor, extras, eye);
        }
        DungeonPolicy.EspKind kind = DungeonPolicy.classifyHologram(name);
        if (kind == DungeonPolicy.EspKind.BLAZE
                && extras.dungeonPuzzlesEnabled
                && extras.dungeonPuzzlesBlaze) {
            DungeonPolicy.blazeHealth(name).ifPresent(hp ->
                    blazes.add(new BlazeMark(entity.getBoundingBox(), hp)));
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

    private static int colorFor(DungeonPolicy.EspKind kind, QolSkyblockExtras extras, String name) {
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

    private static int leapColor(DungeonPolicy.DungeonClass dungeonClass) {
        return switch (dungeonClass) {
            case ARCHER -> 0x80FFAA00;
            case MAGE -> 0x8055FFFF;
            case BERSERK -> 0x80FF5555;
            case TANK -> 0x8055FF55;
            case HEALER -> 0x80FF55FF;
            case UNKNOWN -> 0;
        };
    }

    private static void scanSimon(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        List<EmberDungeonPolicy.IntVec> lit = new ArrayList<>();
        for (EmberDungeonPolicy.IntVec vec : DungeonF7Policy.simonButtons()) {
            if (blockId(client, new BlockPos(vec.x(), vec.y(), vec.z())).contains("sea_lantern")
                    || blockId(client, new BlockPos(vec.x(), vec.y(), vec.z())).contains("lamp")) {
                lit.add(vec);
                if (simonOrder.stream().noneMatch(existing ->
                        existing.x() == vec.x() && existing.y() == vec.y() && existing.z() == vec.z())) {
                    simonOrder.add(vec);
                }
            }
        }
        if (!lit.isEmpty()) {
            simonRemaining.clear();
            simonRemaining.addAll(simonOrder);
        }
        if (lit.isEmpty() && simonOrder.size() > 8) {
            simonOrder.clear();
        }
    }

    private static void autoSimonNext(Minecraft client) {
        if (simonCooldown > 0
                || client.gameMode == null
                || client.player == null
                || simonRemaining.isEmpty()
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        EmberDungeonPolicy.IntVec next = simonRemaining.getFirst();
        BlockPos pos = hit.getBlockPos();
        if (pos.getX() != next.x() || pos.getY() != next.y() || pos.getZ() != next.z()) {
            return;
        }
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        simonRemaining.removeFirst();
        simonCooldown = 3;
    }

    private static void updateCrystalHud(Minecraft client, QolSkyblockExtras extras, long now) {
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
            }
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
        EmberDungeonPolicy.IntVec next = simonRemaining.isEmpty() ? null : simonRemaining.getFirst();
        return DungeonF7Policy.blockWrongSimon(
                pos.getX(), pos.getY(), pos.getZ(),
                extras.dungeonF7Simon && extras.dungeonF7SimonBlockWrong,
                sneaking,
                next);
    }

    private static void addTimer(List<String> lines, String label, long until, long now) {
        if (label == null || label.isBlank() || until <= now) {
            return;
        }
        lines.add(label + " " + String.format(Locale.ROOT, "%.1fs", (until - now) / 1000.0D));
    }

    private static void showTitle(Minecraft client, boolean enabled, String text) {
        if (!enabled || client == null || client.gui == null || text == null || text.isBlank()) {
            return;
        }
        client.gui.hud.setTitle(Component.literal(text));
    }

    private static List<String> sidebarLines() {
        String text = SkyBlockSidebar.text();
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.split("\\R"));
    }

    private static String entityName(Entity entity) {
        if (entity == null) {
            return "";
        }
        Component custom = entity.getCustomName();
        if (custom != null) {
            return custom.getString();
        }
        return entity.getName().getString();
    }

    private static String blockId(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "" : key.getPath();
    }

    private static String fullBlockId(Minecraft client, BlockPos pos) {
        BlockState state = client.level.getBlockState(pos);
        var key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key == null ? "minecraft:air" : key.toString();
    }

    private static String nearbyColor(Minecraft client, BlockPos pos, List<String> palette) {
        for (BlockPos around : List.of(pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west())) {
            int idx = DungeonPuzzlePolicy.leverColorIndex(blockId(client, around), palette);
            if (idx >= 0) {
                return palette.get(idx);
            }
        }
        return "";
    }

    private static AABB blockBox(BlockPos pos) {
        return blockBox(pos.getX(), pos.getY(), pos.getZ());
    }

    private static AABB blockBox(int x, int y, int z) {
        return new AABB(x, y, z, x + 1, y + 1, z + 1);
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    private static List<DungeonMapPolicy.PlayerIcon> mapIcons(
            MapItemSavedData data, QolSkyblockExtras extras) {
        if (!extras.dungeonHudMapPlayers || data == null) {
            return List.of();
        }
        Map<String, DungeonPolicy.DungeonClass> classes =
                EmberDungeonPolicy.teammateClasses(sidebarLines());
        List<DungeonMapPolicy.PlayerIcon> icons = new ArrayList<>();
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
            String classKey = "";
            if (extras.dungeonHudClassIcons || extras.dungeonHudHeadMarkers) {
                DungeonPolicy.DungeonClass found = classes.getOrDefault(name,
                        DungeonPolicy.dungeonClass(name));
                if (found == DungeonPolicy.DungeonClass.UNKNOWN && extras.dungeonHudClassIcons) {
                    found = sidebar.dungeonClass();
                }
                classKey = found == DungeonPolicy.DungeonClass.UNKNOWN
                        ? (extras.dungeonHudHeadMarkers ? "head" : "")
                        : found.name().toLowerCase(Locale.ROOT);
            }
            icons.add(new DungeonMapPolicy.PlayerIcon(x, z, yaw, false, classKey, name));
        }
        return icons;
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

    private static void persistSplitPbs(
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

    private static void persistKuudraPbs(
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

    private static void refreshChestProfitHud(Minecraft client, QolSkyblockExtras extras) {
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
        int digit = digitFromKey(glfwKey);
        if (digit < 1) {
            return false;
        }
        if (extras.dungeonTerminalsEnabled && extras.dungeonTerminalsMelodyKeys
                && DungeonPolicy.detectTerminal(titleOf(screen)) == DungeonPolicy.Terminal.MELODY) {
            int slot = DungeonF7Policy.melodySlotForDigit(digit);
            if (slot >= 0) {
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

    private static boolean protectingTerminal() {
        QolSkyblockExtras extras = extras();
        return extras.dungeonTerminalsEnabled
                && extras.dungeonTerminalsProtect
                && DungeonF7Policy.protectTerminal(
                        terminalOpenedAt,
                        System.currentTimeMillis(),
                        extras.dungeonTerminalsProtectMs);
    }

    private static void observeTerminalOpen(Minecraft client, QolSkyblockExtras extras) {
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            lastTermTitle = "";
            terminalOpenedAt = 0L;
            return;
        }
        String title = titleOf(screen);
        if (DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.NONE) {
            lastTermTitle = "";
            return;
        }
        if (!title.equals(lastTermTitle)) {
            lastTermTitle = title;
            terminalOpenedAt = System.currentTimeMillis();
        }
    }

    private static boolean isCloseKey(int glfwKey) {
        return glfwKey == GLFW.GLFW_KEY_ESCAPE || glfwKey == GLFW.GLFW_KEY_E;
    }

    private static int digitFromKey(int glfwKey) {
        if (glfwKey >= GLFW.GLFW_KEY_1 && glfwKey <= GLFW.GLFW_KEY_4) {
            return glfwKey - GLFW.GLFW_KEY_0;
        }
        if (glfwKey >= GLFW.GLFW_KEY_KP_1 && glfwKey <= GLFW.GLFW_KEY_KP_4) {
            return glfwKey - GLFW.GLFW_KEY_KP_0;
        }
        return -1;
    }

    private static String titleOf(AbstractContainerScreen<?> screen) {
        return screen.getTitle() == null ? "" : screen.getTitle().getString();
    }

    private static void clickContainerSlot(AbstractContainerScreen<?> screen, int slot) {
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

    private static void maybeCloseChest(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonMenusEnabled
                || !extras.dungeonMenusCloseChest
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            closeChestArmed = false;
            return;
        }
        if (!TempleDungeonPolicy.shouldAutoCloseChest(true, screen.getTitle().getString())) {
            closeChestArmed = false;
            return;
        }
        if ("Any Key".equals(DungeonF7Policy.normalizeCloseChestMode(extras.dungeonMenusCloseChestMode))) {
            closeChestArmed = true;
            return;
        }
        closeChestArmed = false;
        client.player.closeContainer();
    }

    private static void flushTermQueue(Minecraft client, QolSkyblockExtras extras) {
        if (terminalCooldown > 0 || client.gameMode == null || client.player == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
                termQueue.clear();
                melodySkipQueue.clear();
            }
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.NONE) {
            termQueue.clear();
            melodySkipQueue.clear();
            return;
        }
        var next = DungeonLeftoverPolicy.dequeueIfReady(termQueue, 0);
        if (next.isEmpty()) {
            return;
        }
        DungeonLeftoverPolicy.QueuedClick click = next.get();
        int packetButton = extras.dungeonTerminalsClone ? 2 : click.button();
        ContainerInput input = extras.dungeonTerminalsClone ? ContainerInput.CLONE : ContainerInput.PICKUP;
        client.gameMode.handleContainerInput(
                screen.getMenu().containerId, click.slot(), packetButton, input, client.player);
        terminalCooldown = Math.max(1, extras.dungeonTerminalsDelay);
    }

    private static void autoSuperboom(Minecraft client, QolSkyblockExtras extras) {
        if (superboomCooldown > 0 || client.gameMode == null || client.player == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        String id = blockId(client, hit.getBlockPos());
        if (!DungeonLeftoverPolicy.isSuperboomWall(id)) {
            return;
        }
        LocalPlayer player = client.player;
        Integer slot = findSuperboom(player);
        if (slot == null) {
            return;
        }
        if (!DungeonLeftoverPolicy.shouldAutoSuperboom(true, true, true, true)) {
            return;
        }
        int selected = player.getInventory().getSelectedSlot();
        if (extras.dungeonF7SuperboomSwapBack && superboomOriginalSlot < 0) {
            superboomOriginalSlot = selected;
        }
        player.getInventory().setSelectedSlot(slot);
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
        player.swing(InteractionHand.MAIN_HAND);
        superboomCooldown = Math.max(1, extras.dungeonF7SuperboomDelay);
        if (extras.dungeonF7SuperboomSwapBack) {
            superboomSwapBackTicks = Math.max(1, extras.dungeonF7SuperboomDelay);
        } else {
            player.getInventory().setSelectedSlot(selected);
        }
    }

    private static Integer findSuperboom(LocalPlayer player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (TempleDungeonPolicy.isSuperboomItem(
                    stack.getHoverName().getString(),
                    AutoClickerItemIdentity.skyBlockId(stack))) {
                return i;
            }
        }
        return null;
    }

    private static void ghostBlocks(Minecraft client, QolSkyblockExtras extras) {
        if (ghostCooldown > 0 || client.player == null || client.level == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        long window = client.getWindow() == null ? 0L : client.getWindow().handle();
        boolean key = QolKeybindNames.isBoundDown(window, extras.dungeonEspGhostKeybind);
        boolean stonk = extras.dungeonEspGhostStonk
                && client.options != null
                && client.options.keyUse.isDown()
                && DungeonLeftoverPolicy.isPickaxe(
                        BuiltInRegistries.ITEM.getKey(client.player.getMainHandItem().getItem()).getPath(),
                        client.player.getMainHandItem().getHoverName().getString());
        if (!key && !stonk) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        String id = fullBlockId(client, pos);
        if (!DungeonLeftoverPolicy.canGhostBlock(true, true, extras.dungeonEspGhostUayor, id)) {
            return;
        }
        client.level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        ghostCooldown = 1;
    }

    private static void triggerBot(Minecraft client, QolSkyblockExtras extras) {
        long now = System.currentTimeMillis();
        if (now - triggerLastMs < extras.dungeonEspTriggerDelay
                || client.gameMode == null || client.player == null) {
            return;
        }
        String blockId = "";
        BlockHitResult blockHit = null;
        if (client.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
            blockHit = hit;
            blockId = blockId(client, hit.getBlockPos());
        }
        String hologram = "";
        Entity entity = null;
        if (client.hitResult instanceof net.minecraft.world.phys.EntityHitResult entityHit) {
            entity = entityHit.getEntity();
            hologram = entityName(entity);
        }
        boolean lookingCrystal = DungeonLeftoverPolicy.isEnergyCrystalName(hologram)
                || (entity != null && entity.getType() == EntityTypes.END_CRYSTAL);
        DungeonLeftoverPolicy.TriggerKind kind = DungeonLeftoverPolicy.triggerKind(
                extras.dungeonEspTriggerCrystal,
                extras.dungeonEspTriggerSecret,
                lookingCrystal,
                blockId,
                hologram);
        if (kind == DungeonLeftoverPolicy.TriggerKind.NONE) {
            return;
        }
        if (kind == DungeonLeftoverPolicy.TriggerKind.SECRET && blockHit != null) {
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHit);
            client.player.swing(InteractionHand.MAIN_HAND);
            triggerLastMs = now;
            return;
        }
        if (kind == DungeonLeftoverPolicy.TriggerKind.CRYSTAL) {
            boolean holdingCrystal = DungeonLeftoverPolicy.isEnergyCrystalName(
                    client.player.getMainHandItem().getHoverName().getString());
            if (lookingCrystal && extras.dungeonEspTriggerTake && entity != null) {
                client.gameMode.interact(
                        client.player,
                        entity,
                        (net.minecraft.world.phys.EntityHitResult) client.hitResult,
                        InteractionHand.MAIN_HAND);
                triggerLastMs = now;
            } else if (holdingCrystal && extras.dungeonEspTriggerPlace && blockHit != null) {
                client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, blockHit);
                triggerLastMs = now;
            }
        }
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }

    private record BlazeMark(AABB box, int health) {
    }

    private record Mark(AABB box, int color) {
    }
}
