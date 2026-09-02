package fi.rotclient;

/**
 * Authoritative player-state snapshot foundation for future drop mechanics
 * (Phase C). Observation extraction is intentionally deferred — this models
 * the domain shape, revision, and confidence/staleness without inventing
 * live tab/stat parsers yet.
 *
 * <p>Prefer final displayed stats (tab Magic Find, etc). Do not double-add
 * gear-derived bonuses on top of authoritative totals.
 */
final class PlayerStateService {
    enum Confidence {
        UNKNOWN,
        STALE,
        OBSERVED,
        AUTHORITATIVE
    }

    /**
     * Immutable snapshot. Null/NaN numeric inputs normalize to unset (-1) so
     * callers never treat missing research as an exact game rule.
     */
    record Snapshot(
            int magicFind,
            int petLuck,
            int miningFortune,
            int miningSpeed,
            int pristine,
            int breakingPower,
            SkyBlockArea area,
            String heldItemId,
            long revision,
            long observedAtMillis,
            Confidence confidence) {
        Snapshot {
            magicFind = sanitizeStat(magicFind);
            petLuck = sanitizeStat(petLuck);
            miningFortune = sanitizeStat(miningFortune);
            miningSpeed = sanitizeStat(miningSpeed);
            pristine = sanitizeStat(pristine);
            breakingPower = sanitizeStat(breakingPower);
            area = area == null ? SkyBlockArea.UNKNOWN_SKYBLOCK_AREA : area;
            heldItemId = heldItemId == null ? "" : SkyBlockItemId.normalize(heldItemId);
            revision = Math.max(0L, revision);
            observedAtMillis = Math.max(0L, observedAtMillis);
            confidence = confidence == null ? Confidence.UNKNOWN : confidence;
        }

        boolean hasAuthoritativeMagicFind() {
            return confidence == Confidence.AUTHORITATIVE && magicFind >= 0;
        }

        boolean isStale(long nowMillis, long maxAgeMillis) {
            if (confidence == Confidence.UNKNOWN || observedAtMillis <= 0L) {
                return true;
            }
            if (confidence == Confidence.STALE) {
                return true;
            }
            return nowMillis - observedAtMillis > Math.max(0L, maxAgeMillis);
        }
    }

    private Snapshot latest = empty(0L);
    private long revision;

    Snapshot latest() {
        return latest;
    }

    long revision() {
        return revision;
    }

    /** Records a new snapshot; never invents missing stats from gear math. */
    Snapshot publish(Snapshot next) {
        if (next == null) {
            return latest;
        }
        revision++;
        latest = new Snapshot(
                next.magicFind(),
                next.petLuck(),
                next.miningFortune(),
                next.miningSpeed(),
                next.pristine(),
                next.breakingPower(),
                next.area(),
                next.heldItemId(),
                revision,
                next.observedAtMillis(),
                next.confidence());
        return latest;
    }

    void reset() {
        revision = 0L;
        latest = empty(0L);
    }

    static Snapshot empty(long nowMillis) {
        return new Snapshot(
                -1, -1, -1, -1, -1, -1,
                SkyBlockArea.UNKNOWN_SKYBLOCK_AREA,
                "",
                0L,
                nowMillis,
                Confidence.UNKNOWN);
    }

    private static int sanitizeStat(int value) {
        if (value < 0) {
            return -1;
        }
        return value;
    }
}
