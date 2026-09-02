package fi.rotclient;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.WallSkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

/**
 * Maps {@link SecretHitboxesPolicy} shape ids onto Minecraft voxel shapes.
 */
public final class SecretHitboxesRuntime {
    private static final VoxelShape LEVER_FLOOR = Block.box(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);
    private static final VoxelShape LEVER_NORTH = Block.box(5.0D, 3.0D, 10.0D, 11.0D, 13.0D, 16.0D);
    private static final VoxelShape LEVER_SOUTH = Block.box(5.0D, 3.0D, 0.0D, 11.0D, 13.0D, 6.0D);
    private static final VoxelShape LEVER_EAST = Block.box(0.0D, 3.0D, 5.0D, 6.0D, 13.0D, 11.0D);
    private static final VoxelShape LEVER_WEST = Block.box(10.0D, 3.0D, 5.0D, 16.0D, 13.0D, 11.0D);

    private SecretHitboxesRuntime() {
    }

    public static VoxelShape shapeFor(BlockState state) {
        if (state == null) {
            return null;
        }
        QolUtilityConfig qol = RotClientClient.qolConfigPublic();
        SecretHitboxesPolicy.ShapeId id = SecretHitboxesPolicy.resolve(
                qol.secretHitboxesEnabled,
                qol.secretHitboxesOnlyDungeons,
                SkyBlockDungeonDetector.confidentlyInDungeon(),
                kind(state),
                qol.secretHitboxesLever,
                qol.secretHitboxesOldLever,
                qol.secretHitboxesButton,
                qol.secretHitboxesFlatButton,
                qol.secretHitboxesSkull,
                qol.secretHitboxesChests,
                qol.secretHitboxesOnlyTrappedChests,
                attachFace(state),
                cardinal(state),
                powered(state));
        return voxel(id, powered(state));
    }

    private static VoxelShape voxel(SecretHitboxesPolicy.ShapeId id, boolean powered) {
        if (id == null || id == SecretHitboxesPolicy.ShapeId.NONE) {
            return null;
        }
        double f = (powered ? 1.0D : 2.0D);
        return switch (id) {
            case FULL_CUBE -> Shapes.block();
            case LEVER_FLOOR -> LEVER_FLOOR;
            case LEVER_NORTH -> LEVER_NORTH;
            case LEVER_SOUTH -> LEVER_SOUTH;
            case LEVER_EAST -> LEVER_EAST;
            case LEVER_WEST -> LEVER_WEST;
            case BUTTON_UP -> Block.box(0.0D, 16.0D - f, 0.0D, 16.0D, 16.0D, 16.0D);
            case BUTTON_DOWN -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, f, 16.0D);
            case BUTTON_EAST -> Block.box(0.0D, 0.0D, 0.0D, f, 16.0D, 16.0D);
            case BUTTON_WEST -> Block.box(16.0D - f, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
            case BUTTON_SOUTH -> Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, f);
            case BUTTON_NORTH -> Block.box(0.0D, 0.0D, 16.0D - f, 16.0D, 16.0D, 16.0D);
            case NONE -> null;
        };
    }

    private static SecretHitboxesPolicy.BlockKind kind(BlockState state) {
        if (state.getBlock() instanceof LeverBlock) {
            return SecretHitboxesPolicy.BlockKind.LEVER;
        }
        if (state.getBlock() instanceof ButtonBlock) {
            return SecretHitboxesPolicy.BlockKind.BUTTON;
        }
        if (state.getBlock() instanceof SkullBlock
                || state.getBlock() instanceof WallSkullBlock) {
            return SecretHitboxesPolicy.BlockKind.SKULL;
        }
        if (state.getBlock() instanceof TrappedChestBlock) {
            return SecretHitboxesPolicy.BlockKind.TRAPPED_CHEST;
        }
        if (state.getBlock() instanceof ChestBlock) {
            return SecretHitboxesPolicy.BlockKind.CHEST;
        }
        return SecretHitboxesPolicy.BlockKind.NONE;
    }

    private static SecretHitboxesPolicy.AttachFace attachFace(BlockState state) {
        if (!state.hasProperty(LeverBlock.FACE) && !state.hasProperty(ButtonBlock.FACE)) {
            return SecretHitboxesPolicy.AttachFace.WALL;
        }
        AttachFace face = state.hasProperty(LeverBlock.FACE)
                ? state.getValue(LeverBlock.FACE)
                : state.getValue(ButtonBlock.FACE);
        if (face == AttachFace.CEILING) {
            return SecretHitboxesPolicy.AttachFace.CEILING;
        }
        if (face == AttachFace.FLOOR) {
            return SecretHitboxesPolicy.AttachFace.FLOOR;
        }
        return SecretHitboxesPolicy.AttachFace.WALL;
    }

    private static SecretHitboxesPolicy.Cardinal cardinal(BlockState state) {
        Direction facing = Direction.NORTH;
        if (state.hasProperty(LeverBlock.FACING)) {
            facing = state.getValue(LeverBlock.FACING);
        } else if (state.hasProperty(ButtonBlock.FACING)) {
            facing = state.getValue(ButtonBlock.FACING);
        }
        return switch (facing) {
            case EAST -> SecretHitboxesPolicy.Cardinal.EAST;
            case WEST -> SecretHitboxesPolicy.Cardinal.WEST;
            case SOUTH -> SecretHitboxesPolicy.Cardinal.SOUTH;
            default -> SecretHitboxesPolicy.Cardinal.NORTH;
        };
    }

    private static boolean powered(BlockState state) {
        if (state.hasProperty(LeverBlock.POWERED)) {
            return state.getValue(LeverBlock.POWERED);
        }
        if (state.hasProperty(ButtonBlock.POWERED)) {
            return state.getValue(ButtonBlock.POWERED);
        }
        return false;
    }
}
