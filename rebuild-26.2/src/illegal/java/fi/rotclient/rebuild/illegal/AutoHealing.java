package fi.rotclient.rebuild.illegal;

import fi.rotclient.rebuild.illegal.mixin.AutoHealingGameModeAccessor;
import java.util.List;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

final class AutoHealing {
    private static final long EMPTY_HOTBAR_RECHECK_NANOS = 250_000_000L;
    private final AutoHealingSettings settings;
    private final AutoHealingSequence sequence = new AutoHealingSequence();
    private Observation observation;
    private LocalPlayer owner;
    private ClientLevel level;
    private boolean selectingSlot;
    private AutoHealingSettings.Item healingItem;
    private long tick;
    private long nextUseAt = System.nanoTime();
    private long nextScanAt = nextUseAt;

    AutoHealing(AutoHealingSettings settings) { this.settings = settings; }

    void register() {
        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) observeActionBar(message);
            return true;
        });
        ClientReceiveMessageEvents.GAME_CANCELED.register((message, overlay) -> {
            if (overlay) observeActionBar(message);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((connection, client) -> {
            sequence.reset();
            owner = null;
            level = null;
            observation = null;
            nextUseAt = System.nanoTime();
            nextScanAt = nextUseAt;
        });
    }

    void observeActionBar(Component message) {
        if (!settings.enabled) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;
        AutoHealingPolicy.readHealth(message.getString()).ifPresent(health ->
                observation = new Observation(client.player, client.level, health, System.nanoTime()));
    }

    void updateEnabled(boolean enabled) {
        settings.enabled = enabled;
        stop(Minecraft.getInstance());
        sequence.reset();
        observation = null;
    }

    void selectedSlotChanged(Inventory inventory, int newSlot) {
        if (!selectingSlot && owner != null && inventory == owner.getInventory()
                && newSlot != inventory.getSelectedSlot()) sequence.manualChange();
    }

    boolean active() { return sequence.active(); }

    void afterInput(Minecraft client) {
        tick++;
        if (owner != client.player || level != client.level) {
            sequence.reset();
            owner = client.player;
            level = client.level;
        }
        var player = client.player;
        if (!settings.enabled || player == null || client.gameMode == null || !player.isAlive()
                || player.isSpectator() || player.isUsingItem() || !client.isWindowActive()
                || !client.mouseHandler.isMouseGrabbed() || client.gui.screen() != null || client.gui.overlay() != null) {
            stop(client);
            return;
        }
        long now = System.nanoTime();
        boolean fresh = observation != null && observation.player() == player && observation.level() == client.level
                && now - observation.atNanos() >= 0
                && now - observation.atNanos() <= AutoHealingPolicy.HEALTH_SAMPLE_MAX_AGE_NANOS;
        boolean low = fresh && AutoHealingPolicy.shouldHeal(observation.health(), settings.healthPercent, now - observation.atNanos());
        if (sequence.active()) {
            sequence.advance(tick, low, hotbar(client));
            return;
        }
        // Only a fresh recovered HP reading re-arms a manually cancelled sequence.
        if (!fresh || !sequence.canStart(low) || now - nextUseAt < 0 || now - nextScanAt < 0) return;
        var inventory = player.getInventory();
        int slot = AutoHealingPolicy.findReadySlot(inventory.getSelectedSlot(), Inventory.getSelectionSize(),
                index -> isReadyItem(player, inventory.getItem(index), settings.item));
        if (slot < 0) {
            nextScanAt = now + EMPTY_HOTBAR_RECHECK_NANOS;
            return;
        }
        healingItem = settings.item;
        sequence.start(tick, settings.selectionDelayTicks, settings.stepDelayTicks, settings.returnDelayTicks, slot, hotbar(client));
    }

    private void stop(Minecraft client) {
        if (sequence.active() && owner != null && owner == client.player && level == client.level && client.gameMode != null) {
            sequence.stop(hotbar(client));
        } else {
            sequence.reset();
        }
    }

    private AutoHealingSequence.Hotbar hotbar(Minecraft client) {
        var player = client.player;
        var inventory = player.getInventory();
        return new AutoHealingSequence.Hotbar() {
            @Override public int selectedSlot() { return inventory.getSelectedSlot(); }

            @Override public void selectSlot(int slot) {
                selectingSlot = true;
                try {
                    inventory.setSelectedSlot(slot);
                    ((AutoHealingGameModeAccessor) client.gameMode).rotRebuild$syncHeldItem();
                } finally {
                    selectingSlot = false;
                }
            }

            @Override public boolean ready(int slot) {
                return settings.item == healingItem && isReadyItem(player, inventory.getItem(slot), healingItem);
            }

            @Override public void use() {
                var lore = inventory.getItem(selectedSlot()).get(DataComponents.LORE);
                List<String> lines = lore == null ? List.of() : lore.lines().stream().map(Component::getString).toList();
                nextUseAt = System.nanoTime() + AutoHealingPolicy.cooldownNanos(settings.minimumSeconds, lines);
                client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            }
        };
    }

    private static boolean isReadyItem(LocalPlayer player, ItemStack stack, AutoHealingSettings.Item item) {
        return AutoHealingPolicy.matchesHealingItem(stack, item) && !player.getCooldowns().isOnCooldown(stack);
    }

    private record Observation(LocalPlayer player, ClientLevel level, AutoHealingPolicy.Health health, long atNanos) {}
}
