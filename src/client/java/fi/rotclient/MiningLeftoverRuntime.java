package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Combined mining leftover suite: Scatha, events, Glacite, helpers, HOTM HUD.
 */
public final class MiningLeftoverRuntime {
    private static int wormCooldown;
    private static MiningLeftoverPolicy.WormKind lastWorm = MiningLeftoverPolicy.WormKind.NONE;
    private static MiningLeftoverPolicy.Pity pity;
    private static MiningLeftoverPolicy.PickaxeAbility ability;
    private static MiningLeftoverPolicy.DrillFuel fuel;
    private static MiningLeftoverPolicy.TreasureDistance treasure;
    private static MiningLeftoverPolicy.MiningEvent event = MiningLeftoverPolicy.MiningEvent.NONE;
    private static MiningLeftoverPolicy.SkyMall skyMall;
    private static MiningLeftoverPolicy.Cold cold;
    private static Map<MiningLeftoverPolicy.CorpseType, Boolean> corpses = Map.of();
    private static final Set<Integer> seenWorms = new HashSet<>();
    private static String titleText = "";
    private static int titleTicks;
    private static boolean hotmOpen;

    private MiningLeftoverRuntime() {
    }

    static void clear() {
        wormCooldown = 0;
        lastWorm = MiningLeftoverPolicy.WormKind.NONE;
        pity = null;
        ability = null;
        fuel = null;
        treasure = null;
        event = MiningLeftoverPolicy.MiningEvent.NONE;
        skyMall = null;
        cold = null;
        corpses = Map.of();
        seenWorms.clear();
        titleText = "";
        titleTicks = 0;
        hotmOpen = false;
        MiningAssistRuntime.clear();
    }

    static boolean hudVisible(QolUtilityConfig qol) {
        if (qol == null) {
            return false;
        }
        QolSkyblockExtras extras = qol.extras();
        return extras.miningScathaEnabled && extras.miningScathaHud
                || extras.miningEventsEnabled && extras.miningEventsHud
                || extras.miningGlaciteEnabled && (extras.miningGlacitePityHud || extras.miningGlaciteCorpseHud)
                || extras.miningHelpersEnabled && (extras.miningHelpersAbilityHud
                || extras.miningHelpersDrillFuel
                || extras.miningHelpersMetalDistance)
                || extras.miningHotmEnabled && extras.miningHotmSkyMall;
    }

    static List<String> hudLines(QolUtilityConfig qol) {
        if (!hudVisible(qol)) {
            return List.of();
        }
        QolSkyblockExtras extras = qol.extras();
        return MiningLeftoverPolicy.hudLines(
                extras.miningGlaciteEnabled && extras.miningGlacitePityHud ? pity : null,
                extras.miningHelpersEnabled && extras.miningHelpersAbilityHud ? ability : null,
                extras.miningHelpersEnabled && extras.miningHelpersDrillFuel ? fuel : null,
                extras.miningHelpersEnabled && extras.miningHelpersMetalDistance ? treasure : null,
                extras.miningEventsEnabled && extras.miningEventsHud ? event : MiningLeftoverPolicy.MiningEvent.NONE,
                extras.miningScathaEnabled && extras.miningScathaHud ? wormCooldown : 0,
                extras.miningScathaEnabled ? lastWorm : MiningLeftoverPolicy.WormKind.NONE,
                extras.miningHotmEnabled && extras.miningHotmSkyMall ? skyMall : null,
                extras.miningGlaciteEnabled && extras.miningGlaciteCorpseHud ? corpses : Map.of());
    }

    static String overlayTitle() {
        return titleTicks > 0 ? titleText : "";
    }

    static int coldAlpha() {
        Minecraft client = Minecraft.getInstance();
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.miningGlaciteEnabled || !extras.miningGlaciteColdOverlay || cold == null) {
            return 0;
        }
        if (client.player == null) {
            return 0;
        }
        return Math.min(140, cold.value() * 4);
    }

    static void tick(Minecraft client) {
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        QolSkyblockExtras extras = qol.extras();
        if (titleTicks > 0) {
            titleTicks--;
        }
        if (client == null || client.player == null) {
            return;
        }
        MiningAssistRuntime.tick(client);
        SkyblockFlavorRuntime.tick(client);
        boolean any = extras.miningScathaEnabled
                || extras.miningEventsEnabled
                || extras.miningGlaciteEnabled
                || extras.miningHelpersEnabled
                || extras.miningHotmEnabled;
        if (!any) {
            return;
        }
        List<String> tab = CommissionDisplayRuntime.tabLines(client);
        if (extras.miningGlaciteEnabled) {
            pity = MiningLeftoverPolicy.parsePity(tab).orElse(pity);
            corpses = MiningLeftoverPolicy.parseCorpseTab(tab);
        }
        if (extras.miningHelpersEnabled && extras.miningHelpersAbilityHud) {
            ability = MiningLeftoverPolicy.parseAbility(tab).orElse(null);
        }
        if (extras.miningHotmEnabled && extras.miningHotmSkyMall) {
            skyMall = MiningLeftoverPolicy.parseSkyMall(tab).orElse(null);
        }
        cold = MiningLeftoverPolicy.parseCold(scoreboardLines()).orElse(null);
        if (extras.miningHelpersEnabled && extras.miningHelpersDrillFuel) {
            fuel = MiningLeftoverPolicy.parseDrillFuel(
                    InventoryChromeRuntime.loreLines(client.player.getMainHandItem()))
                    .orElse(null);
        }
        Screen screen = client.gui == null ? null : client.gui.screen();
        String title = screen == null || screen.getTitle() == null ? "" : screen.getTitle().getString();
        boolean hotmNow = extras.miningHotmEnabled
                && extras.miningHotmScreenHint
                && MiningLeftoverPolicy.isHotmScreen(title);
        if (hotmNow && !hotmOpen) {
            showTitle("Heart of the Mountain", true, false);
        }
        hotmOpen = hotmNow;
        int previous = wormCooldown;
        if (wormCooldown > 0) {
            wormCooldown = MiningLeftoverPolicy.tickCooldown(wormCooldown);
            if (extras.miningScathaEnabled
                    && extras.miningScathaCooldown
                    && MiningLeftoverPolicy.cooldownJustEnded(previous, wormCooldown)) {
                showTitle("Cooldown ended", true, extras.miningScathaSounds);
            }
        }
        if (extras.miningScathaEnabled || extras.miningHelpersEnabled && extras.miningHelpersCommissionMobs
                || extras.miningEventsEnabled && extras.miningEventsGoblinEsp) {
            scanWorms(client, extras);
        }
    }

    static void onChat(Component message, boolean overlay) {
        if (message == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        String text = message.getString();
        if (!overlay) {
            SkyblockFlavorRuntime.onChat(message);
        }
        MiningAssistRuntime.observeChat(text);
        if (overlay) {
            if (extras.miningHelpersEnabled && extras.miningHelpersMetalDistance) {
                treasure = MiningLeftoverPolicy.parseTreasure(text).orElse(treasure);
            }
            MiningAssistRuntime.observeOverlay(text);
            MiningLeftoverPolicy.MiningEvent bossbar = MiningLeftoverPolicy.parseEvent(text);
            if (extras.miningEventsEnabled && bossbar != MiningLeftoverPolicy.MiningEvent.NONE) {
                event = bossbar;
            }
            return;
        }
        if (extras.miningHelpersEnabled && extras.miningHelpersFetchur) {
            MiningLeftoverPolicy.solveFetchur(text).ifPresent(answer ->
                    localChat("Fetchur: " + answer));
        }
        if (extras.miningHelpersEnabled && extras.miningHelpersFossilMuncher) {
            MiningLeftoverPolicy.solveFossilMuncher(text).ifPresent(answer ->
                    localChat("Fossil Muncher: " + answer));
        }
        MiningLeftoverPolicy.NotifyKind kind = MiningLeftoverPolicy.classifyChat(text);
        if (kind == MiningLeftoverPolicy.NotifyKind.WORM_APPROACHING && extras.miningScathaEnabled) {
            wormCooldown = MiningLeftoverPolicy.WORM_COOLDOWN_TICKS;
            showTitle(MiningLeftoverPolicy.titleFor(kind), extras.miningScathaTitles, extras.miningScathaSounds);
        }
        if (kind == MiningLeftoverPolicy.NotifyKind.SCATHA_PET && extras.miningScathaEnabled && extras.miningScathaPetDrop) {
            showTitle(MiningLeftoverPolicy.titleFor(kind), true, extras.miningScathaSounds);
        }
        if (kind == MiningLeftoverPolicy.NotifyKind.MINESHAFT_PORTAL) {
            if (extras.miningHelpersEnabled && extras.miningHelpersNotifyPortal) {
                showTitle(MiningLeftoverPolicy.titleFor(kind), true, true);
            }
            if (extras.miningGlaciteEnabled && extras.miningGlaciteShaftParty) {
                sendParty("Mineshaft portal");
            }
        }
        if (kind == MiningLeftoverPolicy.NotifyKind.SUSPICIOUS_SCRAP
                && extras.miningHelpersEnabled
                && extras.miningHelpersNotifyScrap) {
            showTitle(MiningLeftoverPolicy.titleFor(kind), true, true);
        }
        if ((kind == MiningLeftoverPolicy.NotifyKind.GOLDEN_GOBLIN
                || kind == MiningLeftoverPolicy.NotifyKind.DIAMOND_GOBLIN)
                && extras.miningHelpersEnabled
                && extras.miningHelpersNotifyGoblin) {
            showTitle(MiningLeftoverPolicy.titleFor(kind), true, true);
        }
        if (kind == MiningLeftoverPolicy.NotifyKind.COMMISSION_COMPLETE
                && extras.miningHelpersEnabled
                && extras.miningHelpersCallKing) {
            sendCommand(MiningLeftoverPolicy.kingCallCommand());
        }
        MiningLeftoverPolicy.parseCorpseLoot(text).ifPresent(type -> {
            if (extras.miningGlaciteEnabled && extras.miningGlaciteCorpseHud) {
                showTitle(type.name() + " corpse", true, false);
            }
        });
        MiningLeftoverPolicy.parseCorpseCoords(text).ifPresent(coords -> {
            if (extras.miningGlaciteEnabled && extras.miningGlacitePartyShare) {
                sendParty(coords.partyLine());
            }
        });
        if (extras.miningEventsEnabled) {
            MiningLeftoverPolicy.MiningEvent parsed = MiningLeftoverPolicy.parseEvent(text);
            if (parsed != MiningLeftoverPolicy.MiningEvent.NONE) {
                MiningLeftoverPolicy.MiningEvent previous = event;
                event = parsed;
                if (extras.miningEventsTitles && previous != parsed) {
                    showTitle(MiningLeftoverPolicy.eventLabel(parsed), true, false);
                }
            }
        }
    }

    static boolean allowGameMessage(Component message) {
        if (message == null) {
            return true;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        String text = message.getString();
        if (extras.miningHelpersEnabled && extras.miningHelpersFetchur
                && MiningLeftoverPolicy.solveFetchur(text).isPresent()) {
            return false;
        }
        return !extras.miningHelpersEnabled
                || !extras.miningHelpersFossilMuncher
                || MiningLeftoverPolicy.solveFossilMuncher(text).isEmpty();
    }

    public static void renderGizmos() {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.level == null || client.player == null) {
            return;
        }
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        MiningAssistRuntime.renderGizmos();
        SkyblockFlavorRuntime.renderGizmos();
        boolean mobs = extras.miningHelpersEnabled && extras.miningHelpersCommissionMobs;
        boolean goblin = extras.miningEventsEnabled
                && extras.miningEventsGoblinEsp
                && event == MiningLeftoverPolicy.MiningEvent.GOBLIN_RAID;
        if (!mobs && !goblin) {
            return;
        }
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            String name = stand.getName().getString();
            boolean commission = mobs && MiningLeftoverPolicy.isCommissionMob(name);
            boolean raid = goblin && name.toLowerCase().contains("goblin");
            if (!commission && !raid) {
                continue;
            }
            AABB box = stand.getBoundingBox().inflate(0.2D, 0.6D, 0.2D);
            Gizmos.cuboid(box, GizmoStyle.stroke(0xFF55FF55, 2.0F)).setAlwaysOnTop();
        }
    }

    private static void scanWorms(Minecraft client, QolSkyblockExtras extras) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || !extras.miningScathaEnabled) {
            return;
        }
        int px = player.blockPosition().getX();
        int py = player.blockPosition().getY();
        int pz = player.blockPosition().getZ();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand)) {
                continue;
            }
            MiningLeftoverPolicy.WormKind kind = MiningLeftoverPolicy.wormKind(stand.getName().getString());
            if (kind == MiningLeftoverPolicy.WormKind.NONE || !seenWorms.add(stand.getId())) {
                continue;
            }
            if (!MiningLeftoverPolicy.withinWormAlertRadius(
                    stand.blockPosition().getX() - px,
                    stand.blockPosition().getY() - py,
                    stand.blockPosition().getZ() - pz)) {
                continue;
            }
            lastWorm = kind;
            showTitle(
                    kind == MiningLeftoverPolicy.WormKind.SCATHA ? "Scatha" : "Worm",
                    extras.miningScathaTitles,
                    extras.miningScathaSounds);
            if (extras.miningScathaParty) {
                sendParty(MiningLeftoverPolicy.scathaPartyLine(kind));
            }
        }
    }

    static MiningLeftoverPolicy.Pity pity() {
        return pity;
    }

    static Map<MiningLeftoverPolicy.CorpseType, Boolean> corpses() {
        return corpses;
    }

    static String scoreboardText() {
        return String.join("\n", scoreboardLines());
    }

    static void showTitle(String text, boolean titles, boolean sound) {
        if (!titles || text == null || text.isBlank()) {
            return;
        }
        titleText = text;
        titleTicks = 40;
        Minecraft client = Minecraft.getInstance();
        if (sound && client != null && client.player != null) {
            client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        }
    }

    private static void localChat(String text) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.player != null) {
            client.player.sendSystemMessage(Component.literal("§e" + text));
        }
    }

    static void sendPartyChat(String text) {
        sendParty(text);
    }

    private static void sendParty(String text) {
        sendCommand("pc " + text);
    }

    private static void sendCommand(String command) {
        Minecraft client = Minecraft.getInstance();
        if (client == null || client.player == null || command == null || command.isBlank()) {
            return;
        }
        String payload = command.startsWith("/") ? command.substring(1) : command;
        client.player.connection.sendCommand(payload);
    }

    private static List<String> scoreboardLines() {
        List<String> lines = new ArrayList<>();
        String text = SkyBlockSidebar.text();
        if (text.isBlank()) {
            return lines;
        }
        for (String line : text.split("\\R")) {
            if (!line.isBlank()) {
                lines.add(line);
            }
        }
        return lines;
    }
}
