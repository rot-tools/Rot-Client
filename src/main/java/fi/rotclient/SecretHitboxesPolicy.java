package fi.rotclient;

import java.util.Set;

/**
 * Secret hitbox decisions. Returns which replacement shape to use
 * for dungeon secret blocks (lever / button / skull / chest).
 */
public final class SecretHitboxesPolicy {
    public enum BlockKind {
        NONE,
        LEVER,
        BUTTON,
        SKULL,
        CHEST,
        TRAPPED_CHEST
    }

    public enum AttachFace {
        FLOOR,
        WALL,
        CEILING
    }

    public enum Cardinal {
        NORTH,
        SOUTH,
        EAST,
        WEST
    }

    public enum ShapeId {
        NONE,
        FULL_CUBE,
        LEVER_FLOOR,
        LEVER_CEILING,
        LEVER_NORTH,
        LEVER_SOUTH,
        LEVER_EAST,
        LEVER_WEST,
        BUTTON_UP,
        BUTTON_DOWN,
        BUTTON_NORTH,
        BUTTON_SOUTH,
        BUTTON_EAST,
        BUTTON_WEST
    }

    private static final Set<String> F7_BOSS_LEVERS = Set.of(
            "61,136,142", "60,136,142", "59,136,142",
            "62,135,142", "61,135,142", "59,135,142", "58,135,142",
            "62,134,142", "61,134,142", "59,134,142", "58,134,142",
            "61,133,142", "60,133,142", "59,133,142");

    public static boolean expandLeverAt(int x, int y, int z, int floorNumber) {
        if (floorNumber != 7) {
            return true;
        }
        return !F7_BOSS_LEVERS.contains(x + "," + y + "," + z);
    }

    public static boolean shouldApply(
            boolean moduleEnabled,
            boolean onlyInDungeons,
            boolean confidentlyInDungeon) {
        if (!moduleEnabled) {
            return false;
        }
        if (onlyInDungeons && !confidentlyInDungeon) {
            return false;
        }
        return true;
    }

    public static ShapeId resolve(
            boolean moduleEnabled,
            boolean onlyInDungeons,
            boolean confidentlyInDungeon,
            BlockKind kind,
            boolean leverEnabled,
            boolean oldLeverStyle,
            boolean buttonEnabled,
            boolean flatButtonStyle,
            boolean skullEnabled,
            boolean chestsEnabled,
            boolean onlyTrappedChests,
            AttachFace face,
            Cardinal direction,
            boolean powered) {
        if (!shouldApply(moduleEnabled, onlyInDungeons, confidentlyInDungeon)
                || kind == null
                || kind == BlockKind.NONE) {
            return ShapeId.NONE;
        }
        return switch (kind) {
            case SKULL -> skullEnabled ? ShapeId.FULL_CUBE : ShapeId.NONE;
            case CHEST -> {
                if (!chestsEnabled || onlyTrappedChests) {
                    yield ShapeId.NONE;
                }
                yield ShapeId.FULL_CUBE;
            }
            case TRAPPED_CHEST -> chestsEnabled ? ShapeId.FULL_CUBE : ShapeId.NONE;
            case LEVER -> {
                if (!leverEnabled) {
                    yield ShapeId.NONE;
                }
                if (!oldLeverStyle) {
                    yield ShapeId.FULL_CUBE;
                }
                yield oldLeverShape(face, direction);
            }
            case BUTTON -> {
                if (!buttonEnabled) {
                    yield ShapeId.NONE;
                }
                if (!flatButtonStyle) {
                    yield ShapeId.FULL_CUBE;
                }
                yield flatButtonShape(face, direction);
            }
            default -> ShapeId.NONE;
        };
    }

    static ShapeId oldLeverShape(AttachFace face, Cardinal direction) {
        if (face == AttachFace.CEILING) {
            return ShapeId.LEVER_CEILING;
        }
        if (face == AttachFace.FLOOR) {
            return ShapeId.LEVER_FLOOR;
        }
        if (direction == null) {
            return ShapeId.LEVER_FLOOR;
        }
        return switch (direction) {
            case EAST -> ShapeId.LEVER_EAST;
            case WEST -> ShapeId.LEVER_WEST;
            case SOUTH -> ShapeId.LEVER_SOUTH;
            case NORTH -> ShapeId.LEVER_NORTH;
        };
    }

    static ShapeId flatButtonShape(AttachFace face, Cardinal direction) {
        if (face == AttachFace.CEILING) {
            return ShapeId.BUTTON_UP;
        }
        if (face == AttachFace.FLOOR) {
            return ShapeId.BUTTON_DOWN;
        }
        if (direction == null) {
            return ShapeId.BUTTON_DOWN;
        }
        return switch (direction) {
            case EAST -> ShapeId.BUTTON_EAST;
            case WEST -> ShapeId.BUTTON_WEST;
            case SOUTH -> ShapeId.BUTTON_SOUTH;
            case NORTH -> ShapeId.BUTTON_NORTH;
        };
    }
}
