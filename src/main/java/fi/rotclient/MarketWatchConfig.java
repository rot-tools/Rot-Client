package fi.rotclient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class MarketWatchConfig {
    static final int SCHEMA_VERSION = 1;

    int schemaVersion = SCHEMA_VERSION;
    boolean enabled;

    List<MarketWatchAuctionWatch> auctionWatches =
            new ArrayList<>();

    List<MarketWatchBazaarWatch> bazaarWatches =
            new ArrayList<>();

    static MarketWatchConfig defaults() {
        return new MarketWatchConfig();
    }

    void normalize() {
        if (schemaVersion <= SCHEMA_VERSION) {
            schemaVersion = SCHEMA_VERSION;
        }

        if (auctionWatches == null) {
            auctionWatches = new ArrayList<>();
        }

        if (bazaarWatches == null) {
            bazaarWatches = new ArrayList<>();
        }

        Set<String> usedIds = new HashSet<>();

        List<MarketWatchAuctionWatch> normalizedAuctions =
                new ArrayList<>();

        for (MarketWatchAuctionWatch watch : auctionWatches) {
            if (watch == null) {
                continue;
            }

            watch.normalize();

            while (!usedIds.add(watch.id)) {
                watch.id = UUID.randomUUID().toString();
            }

            normalizedAuctions.add(watch);
        }

        List<MarketWatchBazaarWatch> normalizedBazaar =
                new ArrayList<>();

        for (MarketWatchBazaarWatch watch : bazaarWatches) {
            if (watch == null) {
                continue;
            }

            watch.normalize();

            while (!usedIds.add(watch.id)) {
                watch.id = UUID.randomUUID().toString();
            }

            normalizedBazaar.add(watch);
        }

        auctionWatches = normalizedAuctions;
        bazaarWatches = normalizedBazaar;
    }
}