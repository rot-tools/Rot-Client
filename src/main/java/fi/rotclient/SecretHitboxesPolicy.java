package fi.rotclient;

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

    private SecretHitboxesPolicy() {
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
        if (face == AttachFace.FLOOR || face == AttachFace.CEILING) {
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
