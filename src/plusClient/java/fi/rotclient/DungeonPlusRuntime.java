package fi.rotclient;

import static fi.rotclient.DungeonRuntime.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Plus-only dungeon automations. Shared {@link DungeonRuntime} keeps HUD/ESP.
 */
final class DungeonPlusRuntime {
    private DungeonPlusRuntime() {
    }

    static void tick(Minecraft client) {
        tickPlus(client);
        DungeonHoverTermsRuntime.tick(client);
        DungeonSoulsandRuntime.tick(client);
    }

    static void onScreenOpened(Screen screen) {
        onPlusScreenOpened(screen);
    }

    static void onChat(Component message) {
        if (message != null) {
            onPlusChat(message.getString());
        }
    }

    static void tickPlus(Minecraft client) {
        if (client == null || client.player == null || client.level == null) {
            return;
        }
        QolSkyblockExtras extras = extras();
        long now = System.currentTimeMillis();
        maybeCloseChest(client, extras);
        armRequeueFromScreen(client, extras);
        if (extras.dungeonF7Enabled && extras.dungeonF7DebuffAuto && !debuffFired
                && debuffPhase != EmberDungeonPolicy.DebuffPhase.NONE) {
            if (useDebuffItem(client, extras)) {
                debuffFired = true;
            }
        }
        scanSecretWaypoints(client, extras);
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
    }

    static void onPlusChat(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        QolSkyblockExtras extras = extras();
        Minecraft client = Minecraft.getInstance();
        if (extras.dungeonAnnounceEnabled && extras.dungeonAnnounceAutoUlt
                && EmberDungeonPolicy.shouldFireUltimate(raw, sidebar.floor(), sidebar.dungeonClass())) {
            dropClassUltimate(client);
            showTitle(client, true, "§dUsed Ultimate!");
        }
    }

    static void onPlusScreenOpened(Screen screen) {
    }

    static void autoClickTerminal(Minecraft client) {
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
        List<DungeonPolicy.TerminalClick> live =
                DungeonPolicy.solveTerminalClicks(terminal, title, items);
        if (live.isEmpty()) {
            maybePlayTerminalComplete(client, extras);
            clearPredictedClicks();
            termQueue.clear();
            melodySkipQueue.clear();
            return;
        }
        terminalHadClicks = true;
        DungeonAthenSettings athen = extras.athen();
        if (DungeonAthenPortPolicy.firstClickPending(
                terminalOpenedAt, System.currentTimeMillis(), athen.termFirstClickDelay)) {
            return;
        }
        if (terminalClickUntil > System.currentTimeMillis()) {
            return;
        }
        List<DungeonPolicy.TerminalClick> clicks = pinglessRemaining(terminal, live);
        if (clicks.isEmpty()) {
            return;
        }
        clicks = DungeonAthenPortPolicy.orderClicks(
                clicks, lastTerminalSlot, athen.termOrder, extras.dungeonTerminalsHumanOrder);
        DungeonPolicy.TerminalClick click = clicks.getFirst();
        if (terminal == DungeonPolicy.Terminal.RUBIX && athen.termRubixLeftOnly) {
            click = new DungeonPolicy.TerminalClick(click.slot(), 0);
        }
        if (terminal == DungeonPolicy.Terminal.MELODY) {
            DungeonPolicy.MelodyState melody = DungeonPolicy.parseMelody(items);
            melodySkipQueue.addAll(DungeonPolicy.melodySkipClicks(
                    melody,
                    extras.dungeonTerminalsMelodySkip,
                    extras.dungeonTerminalsMelodySkipFirstRow,
                    extras.dungeonTerminalsMelodySkipMode,
                    DungeonPolicy.melodyPlayRows(items)));
        }
        sendTerminalClick(client, screen, extras, click);
    }

    static void autoSimon(Minecraft client) {
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
        simon = DungeonF7Policy.SimonState.idle();
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        simonCooldown = 3;
    }

    static void autoI4(Minecraft client) {
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

    static void tickAutoI4(Minecraft client, QolSkyblockExtras extras) {
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

    static void relicLook(Minecraft client, QolSkyblockExtras extras, long now) {
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

    static void clickLeapMenu(Minecraft client, QolSkyblockExtras extras) {
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
            List<String> lore = InventoryChromeRuntime.loreLines(slot.getItem());
            if (DungeonPolicy.leapHeadUnavailable(lore)) {
                continue;
            }
            DungeonPolicy.DungeonClass dungeonClass = DungeonPolicy.classFromLore(lore);
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

    static void scanSecretWaypoints(Minecraft client, QolSkyblockExtras extras) {
        secretWaypoints.clear();
        if (client.player == null || client.level == null || extras == null) {
            return;
        }
        boolean mapHud = extras.dungeonHudEnabled && extras.dungeonHudMap;
        boolean secretEsp = extras.dungeonEspEnabled && extras.dungeonEspSecretWaypoints;
        if (!mapHud && !secretEsp) {
            return;
        }
        boolean reveal = extras.dungeonMapRevealHidden();
        boolean fullGrid = mapHud || reveal;
        List<int[]> centers = new ArrayList<>();
        int playerCx = DungeonRoomDataPolicy.roomCenter(client.player.blockPosition().getX());
        int playerCz = DungeonRoomDataPolicy.roomCenter(client.player.blockPosition().getZ());
        centers.add(new int[]{playerCx, playerCz});
        if (fullGrid) {
            for (int tileZ = 0; tileZ < DungeonMapPolicy.GRID; tileZ++) {
                for (int tileX = 0; tileX < DungeonMapPolicy.GRID; tileX++) {
                    centers.add(new int[]{
                            DungeonMapPolicy.roomWorldCenter(tileX),
                            DungeonMapPolicy.roomWorldCenter(tileZ)
                    });
                }
            }
        } else if (lastMapBoard.calibration().ok()) {
            for (DungeonMapPolicy.RoomTile tile : lastMapBoard.rooms()) {
                if (tile.type() == DungeonMapPolicy.RoomType.EMPTY
                        || tile.type() == DungeonMapPolicy.RoomType.UNDISCOVERED) {
                    continue;
                }
                centers.add(new int[]{
                        DungeonMapPolicy.roomWorldCenter(tile.tileX()),
                        DungeonMapPolicy.roomWorldCenter(tile.tileZ())
                });
            }
        }
        int budget = reveal ? DungeonMapPolicy.GRID * DungeonMapPolicy.GRID : 8;
        int newHashes = 0;
        for (int[] center : centers) {
            int cx = center[0];
            int cz = center[1];
            long key = pack(
                    DungeonRoomDataPolicy.roomOrigin(cx),
                    DungeonRoomDataPolicy.roomOrigin(cz));
            if (hashedRoomSecrets.containsKey(key) || hashedRoomTried.contains(key)) {
                continue;
            }
            boolean playerRoom = cx == playerCx && cz == playerCz;
            if (!playerRoom && newHashes >= budget) {
                continue;
            }
            BlockPos sample = new BlockPos(cx, 69, cz);
            if (!client.level.hasChunkAt(sample)) {
                continue;
            }
            if (hashAndCacheRoom(client, cx, cz, key)) {
                newHashes++;
            }
        }
        placePendingSecrets(client);
        for (List<DungeonRoomDataPolicy.PlacedWaypoint> placed : hashedRoomSecrets.values()) {
            secretWaypoints.addAll(placed);
        }
    }

    static void dropClassUltimate(Minecraft client) {
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

    static boolean useDebuffItem(Minecraft client, QolSkyblockExtras extras) {
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

    static boolean useNamedItem(Minecraft client, String... needles) {
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

    static void autoSimonNext(Minecraft client) {
        EmberDungeonPolicy.IntVec next = simon.nextButton();
        if (simonCooldown > 0
                || client.gameMode == null
                || client.player == null
                || next == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        if (pos.getX() != next.x() || pos.getY() != next.y() || pos.getZ() != next.z()) {
            return;
        }
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
        simon = DungeonF7Policy.consumeNext(simon);
        simonCooldown = 3;
    }

    static void armRequeueFromScreen(Minecraft client, QolSkyblockExtras extras) {
        if (client == null || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return;
        }
        if (!DungeonPolicy.shouldArmRequeue(
                extras.dungeonRequeueEnabled,
                dungeonRunStarted,
                extraStatsSeen,
                dungeonWorldTicks,
                titleOf(screen))) {
            return;
        }
        extraStatsSeen = true;
        requeueTicks = Math.max(0, extras.dungeonRequeueDelay);
    }

    static void maybeCloseChest(Minecraft client, QolSkyblockExtras extras) {
        if (!extras.dungeonMenusEnabled
                || !extras.dungeonMenusCloseChest
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            closeChestArmed = false;
            return;
        }
        if (!TempleDungeonPolicy.shouldAutoCloseChest(true, screen.getTitle().getString())) {
            closeChestArmed = false;
            closeChestWait = -1;
            closeChestTitle = "";
            return;
        }
        if ("Any Key".equals(DungeonF7Policy.normalizeCloseChestMode(extras.dungeonMenusCloseChestMode))) {
            closeChestArmed = true;
            closeChestWait = -1;
            return;
        }
        closeChestArmed = false;
        String title = screen.getTitle().getString();
        if (!title.equals(closeChestTitle) || closeChestWait < 0) {
            closeChestTitle = title;
            closeChestWait = DungeonAthenPortPolicy.chestCloseDelayTicks(
                    extras.athen().closeChestMinDelay, extras.athen().closeChestMaxDelay);
        }
        if (closeChestWait > 0) {
            closeChestWait--;
            return;
        }
        closeChestWait = -1;
        closeChestTitle = "";
        client.player.closeContainer();
    }

    static void flushTermQueue(Minecraft client, QolSkyblockExtras extras) {
        if (terminalCooldown > 0 || client.gameMode == null || client.player == null
                || !(client.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            if (!(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
                termQueue.clear();
                melodySkipQueue.clear();
                termQueueUpdatedAt = 0L;
                clearPredictedClicks();
            }
            return;
        }
        String title = screen.getTitle() == null ? "" : screen.getTitle().getString();
        if (DungeonPolicy.detectTerminal(title) == DungeonPolicy.Terminal.NONE) {
            termQueue.clear();
            melodySkipQueue.clear();
            termQueueUpdatedAt = 0L;
            clearPredictedClicks();
            return;
        }
        if (DungeonAthenPortPolicy.queueNeedsResync(
                termQueueUpdatedAt, System.currentTimeMillis(), extras.athen().termResyncTimeout)) {
            termQueue.clear();
            melodySkipQueue.clear();
            termQueueUpdatedAt = 0L;
            clearPredictedClicks();
            return;
        }
        if (terminalFirstClickPending()) {
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
        armTerminalCooldown(extras);
        lastTerminalSlot = click.slot();
    }

    static void autoSuperboom(Minecraft client, QolSkyblockExtras extras) {
        boolean attackDown = client.options != null && client.options.keyAttack.isDown();
        if (!attackDown) {
            superboomAttackHeld = false;
            return;
        }
        if (superboomAttackHeld || superboomCooldown > 0 || client.gameMode == null || client.player == null
                || !(client.hitResult instanceof BlockHitResult hit)
                || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        superboomAttackHeld = true;
        String id = blockId(client, hit.getBlockPos());
        if (!DungeonAthenPortPolicy.isSuperboomWall(id, extras.athen().superboomExtraBlocks)) {
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
        int delay = DungeonAthenPortPolicy.randomBetween(
                extras.athen().superboomMinDelay, extras.athen().superboomMaxDelay);
        superboomCooldown = Math.max(1, delay);
        if (extras.dungeonF7SuperboomSwapBack) {
            superboomSwapBackTicks = DungeonAthenPortPolicy.randomBetween(
                    extras.athen().superboomSwapBackMin, extras.athen().superboomSwapBackMax);
            int target = DungeonAthenPortPolicy.superboomTargetSlot(
                    extras.athen().superboomSwapTo, selected, extras.athen().superboomCustomSlot);
            superboomOriginalSlot = target;
        } else {
            player.getInventory().setSelectedSlot(selected);
        }
    }

    static Integer findSuperboom(LocalPlayer player) {
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

    static void ghostBlocks(Minecraft client, QolSkyblockExtras extras) {
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

    static void triggerBot(Minecraft client, QolSkyblockExtras extras) {
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
}
