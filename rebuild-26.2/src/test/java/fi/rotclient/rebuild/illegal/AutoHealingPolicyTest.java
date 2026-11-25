package fi.rotclient.rebuild.illegal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AutoHealingPolicyTest {
    @Test
    void readsOldAndCurrentHealthIconsWithoutMistakingManaOrSkillProgress() {
        assertEquals(new AutoHealingPolicy.Health(1104, 2508), AutoHealingPolicy.readHealth(
                "§c1,104/2,508❤   §a70❈ Defense   §b155/310✎ Mana").orElseThrow());
        assertEquals(new AutoHealingPolicy.Health(18.5, 100), AutoHealingPolicy.readHealth(
                "\uE010 18.5/100   85/100♨   +100 Combat (421/600)").orElseThrow());
        assertEquals(new AutoHealingPolicy.Health(20, 100), AutoHealingPolicy.readHealth("Health: 20/100").orElseThrow());
        assertTrue(AutoHealingPolicy.readHealth("155/310✎ Mana   +100 Combat (421/600)").isEmpty());
        assertTrue(AutoHealingPolicy.readHealth("0/0❤").isEmpty());
    }

    @Test
    void triggersAtOrBelowTheThresholdOnFreshPositiveHealth() {
        assertTrue(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(199, 1000), 20, 1));
        assertTrue(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(200, 1000), 20, 0));
        assertFalse(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(0, 1000), 20, 0));
        assertFalse(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(200, 1000), 50, 2_000_000_001L));
        assertFalse(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(100, 100), 100, 0));
    }

    @Test
    void percentageUsesTheObservedMaximumForSmallAndLargeHealthPools() {
        for (double maximum : new double[] {20, 100, 2508, 1000000}) {
            assertTrue(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(maximum / 4, maximum), 25, 0));
            assertFalse(AutoHealingPolicy.shouldHeal(new AutoHealingPolicy.Health(maximum / 4 + 1, maximum), 25, 0));
        }
    }

    @Test
    void appliesTheLongerIntervalAndDoesNotMistakeRechargeForCooldown() {
        assertEquals(6_000_000_000L, AutoHealingPolicy.cooldownNanos(6, List.of("§8Cooldown: §a1s")));
        assertEquals(7_500_000_000L, AutoHealingPolicy.cooldownNanos(2, List.of("Cooldown: 7.5 seconds")));
        assertEquals(4_000_000_000L, AutoHealingPolicy.cooldownNanos(4, List.of("Charges: 5 / 15s")));
    }

    @Test
    void identifiesTheConfiguredWandBySkyBlockIdInEveryHotbarSlot() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        var item = new AutoHealingSettings().item;
        assertEquals(AutoHealingSettings.Item.WAND_OF_ATONEMENT, item);

        var stick = new Holder.Direct<>(Items.BLAZE_ROD, DataComponentMap.EMPTY);
        var direct = new CompoundTag();
        direct.putString("id", "WAND_OF_ATONEMENT");
        var modern = new ItemStack(stick);
        modern.set(DataComponents.CUSTOM_DATA, CustomData.of(direct));
        var nested = new CompoundTag();
        nested.put("ExtraAttributes", direct);
        var legacy = new ItemStack(stick);
        legacy.set(DataComponents.CUSTOM_DATA, CustomData.of(nested));
        var renamed = new ItemStack(stick);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Wand of Atonement"));
        assertTrue(AutoHealingPolicy.matchesHealingItem(legacy, item));
        assertFalse(AutoHealingPolicy.matchesHealingItem(renamed, item));
        assertFalse(AutoHealingPolicy.matchesHealingItem(modern, AutoHealingSettings.Item.WAND_OF_HEALING));

        int size = Inventory.getSelectionSize();
        for (int wandSlot = 0; wandSlot < size; wandSlot++) {
            final int index = wandSlot;
            int selected = (wandSlot + 1) % size;
            assertEquals(wandSlot, AutoHealingPolicy.findReadySlot(selected, size, slot ->
                    AutoHealingPolicy.matchesHealingItem(slot == index ? modern : renamed, item)));
        }
        assertEquals(8, AutoHealingPolicy.findReadySlot(8, size, slot -> slot == 0 || slot == 8));
        assertEquals(-1, AutoHealingPolicy.findReadySlot(0, size, slot -> false));
    }

    @Test
    void selectsTheHealingSlotForOneUseAndRestoresThePreviousSlot() {
        var slot = new AtomicInteger(1);
        var sequence = new ArrayList<String>();
        AutoHealingPolicy.useFromSlot(7, slot::get, next -> {
            slot.set(next);
            sequence.add("select " + next);
        }, () -> sequence.add("use " + slot.get()));
        assertEquals(List.of("select 7", "use 7", "select 1"), sequence);

        assertThrows(IllegalStateException.class, () -> AutoHealingPolicy.useFromSlot(0, slot::get, slot::set,
                () -> { throw new IllegalStateException(); }));
        assertEquals(1, slot.get());

        AutoHealingPolicy.useFromSlot(8, slot::get, slot::set, () -> slot.set(2));
        assertEquals(2, slot.get());
    }
}
