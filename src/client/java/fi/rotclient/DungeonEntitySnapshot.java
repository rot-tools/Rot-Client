package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dungeon ESP used to ask the world for every living entity within 128 blocks several times per
 * rendered frame, and to read every entity's name and classify it each time. This keeps one
 * snapshot of the nearby entities, refreshed at most every {@link #REFRESH_TICKS} ticks, with
 * names and classifications computed lazily and only once per snapshot. The entities themselves
 * are live, so boxes still follow them every frame; only which entities exist and what they are
 * called lags, by at most a tenth of a second.
 */
final class DungeonEntitySnapshot {
    static final int REFRESH_TICKS = 2;

    /** One entity with its name and hologram classification, computed on first use. */
    static final class Seen<T extends Entity> {
        private final T entity;
        private String name;
        private String lower;
        private DungeonPolicy.EspKind kind;

        private Seen(T entity) {
            this.entity = entity;
        }

        T entity() {
            return entity;
        }

        String name() {
            if (name == null) {
                name = entity instanceof ItemEntity item ? itemName(item) : DungeonRuntime.entityName(entity);
            }
            return name;
        }

        String lower() {
            if (lower == null) {
                lower = name().toLowerCase(Locale.ROOT);
            }
            return lower;
        }

        DungeonPolicy.EspKind kind() {
            if (kind == null) {
                kind = entity instanceof Player ? DungeonPolicy.EspKind.NONE : DungeonPolicy.classifyHologram(name());
            }
            return kind;
        }

        boolean gone() {
            return entity.isRemoved();
        }
    }

    private static List<Seen<LivingEntity>> living = List.of();
    private static Object livingLevel;
    private static long livingAt = -1L;
    private static List<Seen<ItemEntity>> items = List.of();
    private static Object itemsLevel;
    private static long itemsAt = -1L;

    private DungeonEntitySnapshot() {
    }

    static List<Seen<LivingEntity>> living(Minecraft client, LocalPlayer player) {
        long tick = DungeonRuntime.dungeonWorldTicks;
        if (livingLevel != client.level || DungeonPolicy.scanDue(tick, livingAt, REFRESH_TICKS)) {
            AABB search = player.getBoundingBox().inflate(DungeonPolicy.ESP_SCAN_RANGE);
            List<Seen<LivingEntity>> next = new ArrayList<>();
            for (LivingEntity entity : client.level.getEntitiesOfClass(LivingEntity.class, search)) {
                next.add(new Seen<>(entity));
            }
            living = next;
            livingLevel = client.level;
            livingAt = tick;
        }
        return living;
    }

    static List<Seen<ItemEntity>> items(Minecraft client, LocalPlayer player) {
        long tick = DungeonRuntime.dungeonWorldTicks;
        if (itemsLevel != client.level || DungeonPolicy.scanDue(tick, itemsAt, REFRESH_TICKS)) {
            AABB search = player.getBoundingBox().inflate(DungeonPolicy.ESP_SCAN_RANGE);
            List<Seen<ItemEntity>> next = new ArrayList<>();
            for (ItemEntity entity : client.level.getEntitiesOfClass(ItemEntity.class, search)) {
                next.add(new Seen<>(entity));
            }
            items = next;
            itemsLevel = client.level;
            itemsAt = tick;
        }
        return items;
    }

    static void clear() {
        living = List.of();
        livingLevel = null;
        livingAt = -1L;
        items = List.of();
        itemsLevel = null;
        itemsAt = -1L;
    }

    private static String itemName(ItemEntity item) {
        ItemStack stack = item.getItem();
        return stack == null || stack.isEmpty() ? "" : stack.getHoverName().getString();
    }
}
