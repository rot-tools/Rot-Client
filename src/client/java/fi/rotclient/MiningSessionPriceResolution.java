package fi.rotclient;

import java.math.BigDecimal;
import java.util.Optional;

record MiningSessionPriceResolution(
        PriceStatus status,
        PriceSource source,
        BigDecimal unitPrice,
        Long priceObservedAtMillis,
        Long lastSuccessAtMillis) {
    MiningSessionPriceResolution {
        if (status == null) {
            throw new IllegalArgumentException(
                    "Price status cannot be null");
        }
        if (source == null) {
            throw new IllegalArgumentException(
                    "Price source cannot be null");
        }
        requireTimestamp(priceObservedAtMillis, "Price timestamp");
        requireTimestamp(lastSuccessAtMillis, "Last-success timestamp");
        if (unitPrice != null && unitPrice.signum() < 0) {
            throw new IllegalArgumentException(
                    "Unit price cannot be negative");
        }

        switch (status) {
            case RESOLVED -> {
                if (source == PriceSource.NONE
                        || unitPrice == null
                        || priceObservedAtMillis == null
                        || lastSuccessAtMillis == null) {
                    throw new IllegalArgumentException(
                            "Resolved price requires source, price and timestamps");
                }
            }
            case STALE -> {
                if (source == PriceSource.NONE
                        || unitPrice == null
                        || priceObservedAtMillis == null
                        || lastSuccessAtMillis == null) {
                    throw new IllegalArgumentException(
                            "Stale price requires its previous snapshot");
                }
            }
            case UNRESOLVED, NOT_APPLICABLE -> {
                if (source != PriceSource.NONE
                        || unitPrice != null
                        || priceObservedAtMillis != null
                        || lastSuccessAtMillis != null) {
                    throw new IllegalArgumentException(
                            "Price status cannot carry a price snapshot");
                }
            }
            case UNAVAILABLE -> {
                if (source != PriceSource.NONE
                        || unitPrice != null
                        || priceObservedAtMillis != null) {
                    throw new IllegalArgumentException(
                            "Unavailable price cannot expose a current price");
                }
            }
        }
    }

    static MiningSessionPriceResolution unresolved() {
        return new MiningSessionPriceResolution(
                PriceStatus.UNRESOLVED,
                PriceSource.NONE,
                null,
                null,
                null);
    }

    static MiningSessionPriceResolution resolved(
            PriceSource source,
            BigDecimal unitPrice,
            long priceObservedAtMillis) {
        return resolved(
                source,
                unitPrice,
                priceObservedAtMillis,
                priceObservedAtMillis);
    }

    static MiningSessionPriceResolution resolved(
            PriceSource source,
            BigDecimal unitPrice,
            long priceObservedAtMillis,
            long lastSuccessAtMillis) {
        return new MiningSessionPriceResolution(
                PriceStatus.RESOLVED,
                source,
                unitPrice,
                priceObservedAtMillis,
                lastSuccessAtMillis);
    }

    static MiningSessionPriceResolution unavailable() {
        return unavailable(null);
    }

    static MiningSessionPriceResolution unavailable(
            Long lastSuccessAtMillis) {
        return new MiningSessionPriceResolution(
                PriceStatus.UNAVAILABLE,
                PriceSource.NONE,
                null,
                null,
                lastSuccessAtMillis);
    }

    static MiningSessionPriceResolution stale(
            PriceSource source,
            BigDecimal unitPrice,
            long priceObservedAtMillis,
            long lastSuccessAtMillis) {
        return new MiningSessionPriceResolution(
                PriceStatus.STALE,
                source,
                unitPrice,
                priceObservedAtMillis,
                lastSuccessAtMillis);
    }

    static MiningSessionPriceResolution notApplicable() {
        return new MiningSessionPriceResolution(
                PriceStatus.NOT_APPLICABLE,
                PriceSource.NONE,
                null,
                null,
                null);
    }

    Optional<BigDecimal> estimatedTotal(long quantity) {
        if (quantity <= 0L || status != PriceStatus.RESOLVED) {
            return Optional.empty();
        }
        return Optional.of(
                unitPrice.multiply(
                        BigDecimal.valueOf(quantity)));
    }

    private static void requireTimestamp(
            Long timestamp,
            String label) {
        if (timestamp != null && timestamp < 0L) {
            throw new IllegalArgumentException(
                    label + " cannot be negative");
        }
    }

    enum PriceStatus {
        UNRESOLVED,
        RESOLVED,
        UNAVAILABLE,
        STALE,
        NOT_APPLICABLE
    }

    enum PriceSource {
        BAZAAR_INSTANT_SELL,
        NPC_SELL,
        NONE
    }
}
