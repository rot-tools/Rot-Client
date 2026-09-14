package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/** Plus-only Slayer actions. Shared SlayerRuntime owns observation and HUD state only. */
final class SlayerAutomationRuntime {
    private static final SlayerAutomationPolicy.DaggerSwapState DAGGER_SWAP =
            new SlayerAutomationPolicy.DaggerSwapState();
    private static final SlayerAutomationPolicy.SoulcryState SOULCRY =
            new SlayerAutomationPolicy.SoulcryState();
    private static final SlayerAutomationPolicy.SoulcryAbilityGate SOULCRY_ABILITY =
            new SlayerAutomationPolicy.SoulcryAbilityGate();

    private static int daggerUseCooldown;
    private static int lastDaggerTargetEntityId = Integer.MIN_VALUE;
    private static int autoStartTicks = -1;
    private static String autoStartCommand = "";

    private SlayerAutomationRuntime() {
    }

    static void tick(Minecraft client) {
        QolSkyblockExtras settings = RotClientClient.qolConfigPublic().extras();
        if (settings.slayerDaggerSwapEnabled) {
            tickDaggerSwap(client);
        } else {
            resetDaggerSwap();
        }
        tickSoulcry(client, settings);
        tickAutoStart(client, settings);
    }

    static void onChat(String line) {
        SlayerAutomationPolicy.abilityCooldownTicks(line).ifPresent(SOULCRY_ABILITY::observeRemaining);
    }

    static void onAttack(Minecraft client, Entity entity) {
        QolSkyblockExtras settings = RotClientClient.qolConfigPublic().extras();
        if (settings.slayerAutoSoulcryEnabled && settings.slayerAutoSoulcryAttackBased) {
            tryAttackSoulcry(client, entity, settings);
        }
        if (!settings.slayerDaggerSwapEnabled || entity == null
                || client == null || client.level == null) {
            return;
        }
        String attunementLine = SlayerRuntime.automationAttunementLine(entity);
        if (attunementLine == null) {
            return;
        }
        if (lastDaggerTargetEntityId != entity.getId()) {
            DAGGER_SWAP.reset();
            lastDaggerTargetEntityId = entity.getId();
        }
        int variance = SlayerAutomationPolicy.clampVariance(settings.slayerDaggerSwapVariance);
        int sampled = variance <= 0 ? 0 : ThreadLocalRandom.current().nextInt(variance + 1);
        DAGGER_SWAP.observe(attunementLine, settings.slayerDaggerSwapDelay, sampled);
    }

    static void scheduleAutoStart() {
        QolSkyblockExtras settings = RotClientClient.qolConfigPublic().extras();
        if (!settings.slayerAutoStartEnabled) {
            return;
        }
        Optional<SlayerFightPolicy.QuestRef> quest = SlayerFightPolicy.questFromSidebar(
                List.of(SkyBlockSidebar.text().split("\n")));
        if (quest.isEmpty()) {
            return;
        }
        autoStartCommand = SlayerFightPolicy.autoStartCommand(quest.get().type(), quest.get().tier());
        autoStartTicks = SlayerFightPolicy.clampAutoStartDelayTicks(settings.slayerAutoStartDelay);
    }

    static void reset() {
        resetDaggerSwap();
        SOULCRY.reset();
        SOULCRY_ABILITY.reset();
        autoStartTicks = -1;
        autoStartCommand = "";
    }

    private static void tickDaggerSwap(Minecraft client) {
        if (daggerUseCooldown > 0) {
            daggerUseCooldown--;
        }
        DAGGER_SWAP.tick();
        DAGGER_SWAP.ready().ifPresent(attunement -> applyDaggerSwap(client, attunement));
    }

    private static void applyDaggerSwap(
            Minecraft client,
            SlayerMechanicsPolicy.DaggerAttunement attunement) {
        LocalPlayer player = client == null ? null : client.player;
        if (player == null || client.gameMode == null || player.connection == null) {
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (SlayerAutomationPolicy.supportsDagger(
                SkyBlockItemIdentity.skyBlockId(held), attunement)) {
            if (attunementMode(held) == attunement.mode()) {
                DAGGER_SWAP.complete();
                return;
            }
            if (daggerUseCooldown <= 0) {
                ClickPulseHelper.pulseUse(client);
                daggerUseCooldown = 2;
            }
            return;
        }
        for (int slot = 0; slot < 9; slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (!SlayerAutomationPolicy.supportsDagger(
                    SkyBlockItemIdentity.skyBlockId(candidate), attunement)) {
                continue;
            }
            player.getInventory().setSelectedSlot(slot);
            player.connection.send(new ServerboundSetCarriedItemPacket(slot));
            daggerUseCooldown = 1;
            return;
        }
        DAGGER_SWAP.complete();
    }

    private static int attunementMode(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return -1;
        }
        CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag tag = custom.copyTag();
        int direct = tag.getInt("td_attune_mode").orElse(-1);
        if (direct >= 0) {
            return direct;
        }
        return tag.getCompound("ExtraAttributes")
                .map(extra -> extra.getInt("td_attune_mode").orElse(-1))
                .orElse(-1);
    }

    private static void resetDaggerSwap() {
        DAGGER_SWAP.reset();
        daggerUseCooldown = 0;
        lastDaggerTargetEntityId = Integer.MIN_VALUE;
    }

    private static void tickSoulcry(Minecraft client, QolSkyblockExtras settings) {
        SOULCRY_ABILITY.tick();
        if (!settings.slayerAutoSoulcryEnabled || !settings.slayerAutoSoulcryTickBased
                || client == null || client.player == null || client.level == null
                || (client.gui != null && client.gui.screen() != null)) {
            SOULCRY.reset();
            return;
        }
        ItemStack held = client.player.getMainHandItem();
        if (!SlayerAutomationPolicy.isSoulcryKatana(SkyBlockItemIdentity.skyBlockId(held))) {
            SOULCRY.reset();
            return;
        }
        SlayerSessionEngine.ActiveBoss boss = SlayerRuntime.snapshot().activeBosses().stream()
                .filter(SlayerSessionEngine.ActiveBoss::owned)
                .filter(candidate -> candidate.descriptor().role() == SlayerPolicy.EntityRole.BOSS)
                .filter(candidate -> candidate.descriptor().type() == SlayerPolicy.SlayerType.VOIDGLOOM)
                .findFirst().orElse(null);
        if (boss == null || (settings.slayerAutoSoulcryCheckHitbox
                && (!(client.hitResult instanceof EntityHitResult hit)
                || hit.getEntity().getId() != boss.entityId()))) {
            SOULCRY.reset();
            return;
        }
        if (settings.slayerAutoSoulcryCheckMana && !hasSoulcryMana(held)) {
            SOULCRY.reset();
            return;
        }
        if (!SOULCRY_ABILITY.ready(client.player.getCooldowns().isOnCooldown(held))) {
            SOULCRY.reset();
            return;
        }
        int min = SlayerAutomationPolicy.clampSoulcryDelay(settings.slayerAutoSoulcryMinDelay);
        int max = Math.max(min, SlayerAutomationPolicy.clampSoulcryDelay(settings.slayerAutoSoulcryMaxDelay));
        if (SOULCRY.arm(min, max,
                min == max ? min : ThreadLocalRandom.current().nextInt(min, max + 1))) {
            return;
        }
        SOULCRY.tick();
        if (SOULCRY.ready()) {
            ClickPulseHelper.pulseUse(client);
            SOULCRY.complete();
            SOULCRY_ABILITY.markUsed();
        }
    }

    private static void tryAttackSoulcry(
            Minecraft client,
            Entity entity,
            QolSkyblockExtras settings) {
        if (client == null || client.player == null || client.level == null || entity == null) {
            return;
        }
        SlayerPolicy.EntityDescriptor descriptor = SlayerRuntime.automationDescriptor(entity);
        if (descriptor == null || descriptor.role() != SlayerPolicy.EntityRole.BOSS
                || descriptor.type() != SlayerPolicy.SlayerType.VOIDGLOOM) {
            return;
        }
        boolean owned = descriptor.owner().equalsIgnoreCase(client.player.getGameProfile().name());
        if (!owned && !settings.slayerAutoSoulcryOtherBosses) {
            return;
        }
        ItemStack held = client.player.getMainHandItem();
        if (!SlayerAutomationPolicy.isSoulcryKatana(SkyBlockItemIdentity.skyBlockId(held))
                || (settings.slayerAutoSoulcryCheckMana && !hasSoulcryMana(held))
                || !SOULCRY_ABILITY.ready(client.player.getCooldowns().isOnCooldown(held))) {
            return;
        }
        ClickPulseHelper.pulseUse(client);
        SOULCRY_ABILITY.markUsed();
    }

    private static boolean hasSoulcryMana(ItemStack held) {
        SkyBlockStatBarParser.Stats stats = RotClientClient.qolHud().statsTracker().stats();
        double mana = stats.mana().orElse(-1.0D);
        double overflow = stats.overflowMana().orElse(0.0D);
        if (mana < 0.0D) {
            return false;
        }
        return SlayerAutomationPolicy.hasSoulcryMana(mana, overflow, hasUltimateWise(held));
    }

    private static boolean hasUltimateWise(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag direct = tag.getCompound("enchantments").orElse(null);
        if (direct != null && direct.contains("ultimate_wise")) {
            return true;
        }
        return tag.getCompound("ExtraAttributes")
                .flatMap(extra -> extra.getCompound("enchantments"))
                .map(enchantments -> enchantments.contains("ultimate_wise"))
                .orElse(false);
    }

    private static void tickAutoStart(Minecraft client, QolSkyblockExtras settings) {
        if (!settings.slayerAutoStartEnabled || autoStartTicks < 0) {
            autoStartTicks = -1;
            return;
        }
        if (autoStartTicks-- > 0) {
            return;
        }
        String command = autoStartCommand;
        autoStartTicks = -1;
        autoStartCommand = "";
        if (command != null && !command.isBlank() && client != null && client.player != null) {
            client.player.connection.sendCommand(command);
        }
    }
}
