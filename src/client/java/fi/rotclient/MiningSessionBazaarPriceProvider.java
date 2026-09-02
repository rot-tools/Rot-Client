package fi.rotclient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Captures the live Bazaar cache without performing network I/O.
 * One invalid product never invalidates unrelated quotes or crashes the
 * client tick — rejected quotes are skipped and optionally traced.
 */
final class MiningSessionBazaarPriceProvider
        implements MiningSessionPriceProvider {
    static final MiningSessionBazaarPriceProvider INSTANCE =
            new MiningSessionBazaarPriceProvider();

    private MiningSessionBazaarPriceProvider() {
    }

    @Override
    public MiningSessionPriceBook capture(long nowMillis) {
        try {
            return captureChecked(nowMillis);
        } catch (RuntimeException failure) {
            // External-data / conversion failures must degrade to unavailable,
            // never terminate Minecraft from a periodic tick.
            DiagnosticRecorder.record(
                    "PRICE_BOOK_CAPTURE_FAILED",
                    "reason=" + safeExceptionLabel(failure));
            TrackingRuntimeTrace.event(
                    "PRICE_BOOK_CAPTURE_FAILED",
                    Map.of(
                            "reason", safeExceptionLabel(failure),
                            "action", "UNAVAILABLE"));
            return MiningSessionPriceBook.unavailable();
        }
    }

    private static MiningSessionPriceBook captureChecked(long nowMillis) {
        if (nowMillis < 0L) {
            return MiningSessionPriceBook.unavailableInvalidTimestamp();
        }

        MiningSessionBazaarPriceCache.CacheSnapshot cacheSnapshot =
                MiningSessionBazaarPriceCache.capture();
        if (!cacheSnapshot.isAvailable()) {
            return MiningSessionPriceBook.unavailable();
        }

        long observedAtMillis = cacheSnapshot.observedAtMillis();
        if (!MiningSessionPriceBook.hasValidTimestampOrdering(
                nowMillis,
                observedAtMillis)) {
            return MiningSessionPriceBook.unavailableInvalidTimestamp();
        }

        BazaarPriceService.MarketPrices marketPrices =
                cacheSnapshot.marketPrices();
        Map<String, BigDecimal> unitPrices = new HashMap<>();
        for (TrackedMaterial material : TrackedMaterial.values()) {
            BazaarPriceService.MaterialPrices materialPrices =
                    marketPrices.forMaterial(material);
            if (materialPrices == null) {
                continue;
            }
            addProduct(
                    unitPrices,
                    material.rawBazaarId(),
                    materialPrices.raw());
            addProduct(
                    unitPrices,
                    material.enchantedBazaarId(),
                    materialPrices.enchanted());
        }
        for (Map.Entry<String, BazaarPriceService.ProductPrice> entry
                : marketPrices.byGemstoneProductId().entrySet()) {
            addProduct(
                    unitPrices,
                    entry.getKey(),
                    entry.getValue());
        }

        if (unitPrices.isEmpty()) {
            return MiningSessionPriceBook.unavailable();
        }

        return MiningSessionPriceBook.available(
                observedAtMillis,
                unitPrices);
    }

    private static void addProduct(
            Map<String, BigDecimal> unitPrices,
            String productId,
            BazaarPriceService.ProductPrice productPrice) {
        if (productId == null
                || productId.isBlank()
                || productPrice == null) {
            return;
        }

        double instantSell = productPrice.instantSellPrice();
        MiningSessionPriceBook.NormalizeResult result =
                MiningSessionPriceBook.normalizeUnitPriceFromDouble(instantSell);
        if (!result.accepted()) {
            emitRejected(
                    productId,
                    instantSell,
                    result.rejectReason().orElse(
                            MiningSessionPriceBook.RejectReason.NON_POSITIVE),
                    "BAZAAR_INSTANT_SELL");
            return;
        }
        unitPrices.put(productId, result.normalized().orElseThrow());
    }

    private static void emitRejected(
            String productId,
            double rawValue,
            MiningSessionPriceBook.RejectReason reason,
            String source) {
        String details = "PRODUCT_ID=" + sanitizeId(productId)
                + " RAW_VALUE="
                + MiningSessionPriceBook.sanitizeNumericForDiag(rawValue)
                + " REASON="
                + MiningSessionPriceBook.rejectReasonLabel(reason)
                + " SOURCE=" + source
                + " ACTION=SKIPPED";
        DiagnosticRecorder.record("PRICE_QUOTE_REJECTED", details);
        TrackingRuntimeTrace.event(
                "PRICE_QUOTE_REJECTED",
                Map.of(
                        "product_id", sanitizeId(productId),
                        "raw_value",
                        MiningSessionPriceBook.sanitizeNumericForDiag(rawValue),
                        "reason",
                        MiningSessionPriceBook.rejectReasonLabel(reason),
                        "source", source,
                        "action", "SKIPPED"));
    }

    private static String sanitizeId(String productId) {
        if (productId == null || productId.isBlank()) {
            return "UNKNOWN";
        }
        String trimmed = productId.trim();
        return trimmed.length() > 64 ? trimmed.substring(0, 64) : trimmed;
    }

    private static String safeExceptionLabel(Throwable failure) {
        if (failure == null) {
            return "unknown";
        }
        String name = failure.getClass().getSimpleName();
        return name == null || name.isBlank() ? "RuntimeException" : name;
    }
}
