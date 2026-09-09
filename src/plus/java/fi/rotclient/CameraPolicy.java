package fi.rotclient;

/**
 * Camera QoL: when enabled, perspective toggles between first person and rear
 * third person. Front-facing third person is skipped. This is not freecam.
 */
public final class CameraPolicy {
    private CameraPolicy() {
    }

    /**
     * Resolves the effective perspective after Camera module rules.
     *
     * @param moduleEnabled Camera module toggle
     * @param requested     perspective the game / user just selected
     * @return requested perspective, except front-facing third person resolves
     *         to first person while the module is enabled
     */
    public static PerspectiveMode resolve(
            boolean moduleEnabled,
            PerspectiveMode requested) {
        PerspectiveMode safe =
                requested == null ? PerspectiveMode.FIRST_PERSON : requested;
        if (!moduleEnabled) {
            return safe;
        }
        return safe == PerspectiveMode.THIRD_PERSON_FRONT
                ? PerspectiveMode.FIRST_PERSON
                : safe;
    }

    /** True when the requested perspective is allowed under Camera rules. */
    public static boolean allows(
            boolean moduleEnabled,
            PerspectiveMode requested) {
        if (!moduleEnabled) {
            return true;
        }
        return requested == null
                || requested != PerspectiveMode.THIRD_PERSON_FRONT;
    }

    /**
     * Vanilla three-view cycle when Camera is off. When Camera is on, the
     * front-facing view is omitted: first -> rear -> first.
     */
    public static PerspectiveMode cycle(
            boolean moduleEnabled,
            PerspectiveMode current) {
        PerspectiveMode safe =
                current == null ? PerspectiveMode.FIRST_PERSON : current;
        if (moduleEnabled) {
            return switch (safe) {
                case FIRST_PERSON -> PerspectiveMode.THIRD_PERSON_BACK;
                case THIRD_PERSON_BACK, THIRD_PERSON_FRONT ->
                        PerspectiveMode.FIRST_PERSON;
            };
        }
        return switch (safe) {
            case FIRST_PERSON -> PerspectiveMode.THIRD_PERSON_BACK;
            case THIRD_PERSON_BACK -> PerspectiveMode.THIRD_PERSON_FRONT;
            case THIRD_PERSON_FRONT -> PerspectiveMode.FIRST_PERSON;
        };
    }
}
