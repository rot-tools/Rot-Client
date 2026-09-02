package fi.rotclient;

import com.mojang.blaze3d.platform.InputConstants;
import fi.rotclient.mixin.KeyMappingAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Auto Dojo for Control / Mastery / Discipline.
 */
public final class AutoDojoRuntime {
    private static DungeonLeftoverPolicy.DojoType type = DungeonLeftoverPolicy.DojoType.NONE;
    private static Vec3 lastSkeleton = Vec3.ZERO;
    private static Vec3 skeletonVel = Vec3.ZERO;
    private static long lookCooldown;
    private static BlockPos masteryTarget;
    private static long masteryExpiry;
    private static boolean drawing;

    private AutoDojoRuntime() {
    }

    public static void onChat(String raw) {
        if (DungeonLeftoverPolicy.dojoChatClears(raw)) {
            reset();
            return;
        }
        DungeonLeftoverPolicy.DojoType next = DungeonLeftoverPolicy.dojoTypeFromChat(raw);
        if (next != DungeonLeftoverPolicy.DojoType.NONE) {
            type = next;
        }
    }

    public static void tick(Minecraft client) {
        QolSkyblockExtras extras = extras();
        if (!extras.autoDojoEnabled || client == null || client.player == null || client.level == null) {
            releaseUse(client);
            return;
        }
        if (client.gui != null && client.gui.screen() != null) {
            return;
        }
        switch (type) {
            case CONTROL -> {
                if (extras.autoDojoControl) {
                    handleControl(client, extras);
                }
            }
            case MASTERY -> {
                if (extras.autoDojoMastery) {
                    handleMastery(client, extras);
                }
            }
            case DISCIPLINE -> {
                if (extras.autoDojoDiscipline) {
                    handleDiscipline(client, extras);
                }
            }
            default -> releaseUse(client);
        }
    }

    public static void onWorldChanged() {
        reset();
    }

    private static void handleControl(Minecraft client, QolSkyblockExtras extras) {
        LocalPlayer player = client.player;
        Entity closest = null;
        double min = 25.0D;
        AABB search = player.getBoundingBox().inflate(24.0D);
        for (WitherSkeleton skeleton : client.level.getEntitiesOfClass(WitherSkeleton.class, search)) {
            if (skeleton.getItemBySlot(EquipmentSlot.HEAD).is(Items.REDSTONE_BLOCK)) {
                continue;
            }
            double dist = player.distanceTo(skeleton);
            if (dist < min) {
                min = dist;
                closest = skeleton;
            }
        }
        if (closest == null) {
            return;
        }
        Vec3 now = closest.position();
        if (lastSkeleton != Vec3.ZERO) {
            skeletonVel = now.subtract(lastSkeleton);
        }
        lastSkeleton = now;
        long t = System.currentTimeMillis();
        if (t - lookCooldown < 40L) {
            return;
        }
        lookCooldown = t;
        double pred = extras.autoDojoControlPredict;
        lookAt(player,
                now.x + skeletonVel.x * pred,
                now.y + skeletonVel.y * 2.0D + 2.5D,
                now.z + skeletonVel.z * pred);
    }

    private static void handleMastery(Minecraft client, QolSkyblockExtras extras) {
        LocalPlayer player = client.player;
        long now = System.currentTimeMillis();
        if (masteryTarget != null
                && (now > masteryExpiry
                || !client.level.getBlockState(masteryTarget).is(Blocks.WOOL.yellow()))) {
            masteryTarget = null;
        }
        if (masteryTarget == null) {
            BlockPos origin = player.blockPosition();
            BlockPos found = null;
            double best = 26.0D;
            for (int dx = -12; dx <= 12; dx++) {
                for (int dy = -6; dy <= 8; dy++) {
                    for (int dz = -12; dz <= 12; dz++) {
                        BlockPos pos = origin.offset(dx, dy, dz);
                        if (!client.level.getBlockState(pos).is(Blocks.WOOL.yellow())) {
                            continue;
                        }
                        double dist = Math.sqrt(dx * dx + dz * dz);
                        if (dist < best) {
                            best = dist;
                            found = pos;
                        }
                    }
                }
            }
            if (found != null) {
                masteryTarget = found;
                masteryExpiry = now + 3500L;
            }
        }
        if (masteryTarget == null) {
            releaseUse(client);
            return;
        }
        lookAt(player, masteryTarget.getX() + 0.5D, masteryTarget.getY() + 1.1D, masteryTarget.getZ() + 0.5D);
        Integer bow = findItem(player, "bow");
        if (bow != null) {
            player.getInventory().setSelectedSlot(bow);
        }
        long remaining = masteryExpiry - now;
        if (!drawing) {
            setUse(client, true);
            drawing = true;
        } else if (remaining <= extras.autoDojoMasteryDelay) {
            setUse(client, false);
            drawing = false;
            masteryTarget = null;
        }
    }

    private static void handleDiscipline(Minecraft client, QolSkyblockExtras extras) {
        LocalPlayer player = client.player;
        Zombie best = null;
        double min = 7.0D;
        AABB search = player.getBoundingBox().inflate(6.0D);
        for (Zombie zombie : client.level.getEntitiesOfClass(Zombie.class, search)) {
            if (zombie.getType() != EntityTypes.ZOMBIE) {
                continue;
            }
            double dist = player.distanceTo(zombie);
            if (dist >= min) {
                continue;
            }
            min = dist;
            best = zombie;
        }
        if (best == null) {
            return;
        }
        ItemStack helmet = best.getItemBySlot(EquipmentSlot.HEAD);
        String helmetId = itemId(helmet);
        String sword = DungeonLeftoverPolicy.disciplineSwordForHelmet(helmetId);
        Integer slot = findItem(player, sword);
        if (slot != null) {
            player.getInventory().setSelectedSlot(slot);
        }
        lookAt(player, best.getX(), best.getY() + 1.2D, best.getZ());
        if (extras.autoDojoDisciplineAttack && client.options != null) {
            KeyMapping mapping = client.options.keyAttack;
            InputConstants.Key bound = ((KeyMappingAccessor) (Object) mapping).rotclient$boundKey();
            if (bound != null) {
                KeyMapping.set(bound, true);
                KeyMapping.click(bound);
                KeyMapping.set(bound, false);
            }
        }
    }

    private static void lookAt(LocalPlayer player, double x, double y, double z) {
        double dx = x - player.getX();
        double dy = y - (player.getY() + player.getEyeHeight());
        double dz = z - player.getZ();
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) Math.toDegrees(Math.atan2(-dy, horiz));
        player.setYRot(yaw);
        player.setXRot(pitch);
        player.yRotO = yaw;
        player.xRotO = pitch;
    }

    private static Integer findItem(LocalPlayer player, String needle) {
        if (needle == null || needle.isBlank()) {
            return null;
        }
        for (int i = 0; i < 9; i++) {
            if (itemId(player.getInventory().getItem(i)).contains(needle)) {
                return i;
            }
        }
        return null;
    }

    private static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null ? "" : key.getPath();
    }

    private static void setUse(Minecraft client, boolean down) {
        if (client == null || client.options == null || client.options.keyUse == null) {
            return;
        }
        InputConstants.Key bound = ((KeyMappingAccessor) (Object) client.options.keyUse).rotclient$boundKey();
        if (bound != null) {
            KeyMapping.set(bound, down);
        }
    }

    private static void releaseUse(Minecraft client) {
        if (drawing) {
            setUse(client, false);
            drawing = false;
        }
    }

    private static void reset() {
        type = DungeonLeftoverPolicy.DojoType.NONE;
        lastSkeleton = Vec3.ZERO;
        skeletonVel = Vec3.ZERO;
        masteryTarget = null;
        drawing = false;
    }

    private static QolSkyblockExtras extras() {
        return RotClientClient.qolConfigPublic().extras();
    }
}
