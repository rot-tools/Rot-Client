package fi.rotclient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * First-person viewmodel transforms (bounded offset, scale, and rotation).
 */
public final class ViewmodelRuntime {
    private ViewmodelRuntime() {
    }

    public static boolean active() {
        return RotClientClient.qolConfigPublic().extras().viewmodelEnabled;
    }

    public static boolean skipEquipAnimation() {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return extras.viewmodelEnabled && extras.viewmodelNoEquip;
    }

    public static void applyArmOffset(PoseStack matrices, InteractionHand hand, ItemStack item) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.viewmodelEnabled || matrices == null) {
            return;
        }
        if (!ViewmodelPolicy.applyToStack(extras.viewmodelApplyToHand, item == null || item.isEmpty())) {
            return;
        }
        double x = extras.viewmodelOffsetX;
        if (hand == InteractionHand.OFF_HAND) {
            x = -x;
        }
        matrices.translate(x, extras.viewmodelOffsetY, extras.viewmodelOffsetZ);
    }

    public static void applyItemPose(PoseStack matrices) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.viewmodelEnabled || matrices == null) {
            return;
        }
        matrices.mulPose(Axis.XP.rotationDegrees((float) extras.viewmodelRotX));
        matrices.mulPose(Axis.YP.rotationDegrees((float) extras.viewmodelRotY));
        matrices.mulPose(Axis.ZP.rotationDegrees((float) extras.viewmodelRotZ));
        matrices.scale(
                (float) extras.viewmodelScaleX,
                (float) extras.viewmodelScaleY,
                (float) extras.viewmodelScaleZ);
    }

    /**
     * Rotation and scale for the bare first-person arm, only applied when
     * "apply to hand" is on.
     */
    public static void applyHandPose(PoseStack matrices) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.viewmodelEnabled || !extras.viewmodelApplyToHand) {
            return;
        }
        applyItemPose(matrices);
    }

    public static void applyHandOffset(PoseStack matrices, HumanoidArm arm) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.viewmodelEnabled || !extras.viewmodelApplyToHand || matrices == null) {
            return;
        }
        double x = extras.viewmodelOffsetX;
        if (arm == HumanoidArm.LEFT) {
            x = -x;
        }
        matrices.translate(x, extras.viewmodelOffsetY, extras.viewmodelOffsetZ);
    }

    public static void applySwingTranslate(PoseStack matrices, float x, float y, float z) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (extras.viewmodelEnabled && matrices != null) {
            matrices.translate(
                    x * (float) extras.viewmodelSwingX,
                    y * (float) extras.viewmodelSwingY,
                    z * (float) extras.viewmodelSwingZ);
            return;
        }
        matrices.translate(x, y, z);
    }

    /**
     * True when the local player's swing duration must ignore Haste and Mining Fatigue.
     */
    public static boolean ignoreSwingEffects(LivingEntity entity) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        return extras.viewmodelEnabled && extras.viewmodelNoHaste && isLocalPlayer(entity);
    }

    public static int swingDuration(LivingEntity entity, int vanilla) {
        QolSkyblockExtras extras = RotClientClient.qolConfigPublic().extras();
        if (!extras.viewmodelEnabled || !isLocalPlayer(entity)) {
            return vanilla;
        }
        return ViewmodelPolicy.swingDuration(
                vanilla,
                extras.viewmodelNoBowSwing,
                entity.isHolding(Items.BOW),
                extras.viewmodelSwingSpeed);
    }

    private static boolean isLocalPlayer(LivingEntity entity) {
        Minecraft client = Minecraft.getInstance();
        return entity != null
                && client != null
                && client.player != null
                && entity.getId() == client.player.getId();
    }
}
