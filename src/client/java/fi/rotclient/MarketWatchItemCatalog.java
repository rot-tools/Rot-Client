package fi.rotclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class MarketWatchItemCatalog {
    record AuctionSuggestion(
            String itemName,
            String tier,
            String category,
            long lowestBin,
            long medianBin,
            int binCount) {
    }

    record BazaarSuggestion(
            String productId,
            String displayName,
            double instantBuy,
            double instantSell,
            double spreadCoins,
            double spreadPercent,
            long weeklyVolume) {
    }

    record AuctionStats(
            String itemName,
            String tier,
            String category,
            long lowestBin,
            long medianBin,
            int binCount,
            long observedAtMillis) {

        boolean available() {
            return binCount > 0;
        }
    }

    record BazaarStats(
            String productId,
            String displayName,
            double instantBuy,
            double instantSell,
            double spreadCoins,
            double spreadPercent,
            long weeklyVolume,
            long observedAtMillis) {

        boolean available() {
            return productId != null
                    && !productId.isBlank();
        }
    }

    private MarketWatchItemCatalog() {
    }

    static List<AuctionSuggestion> auctionSuggestions(
            String query,
            int limit) {

        MarketWatchDataService.AuctionState state =
                MarketWatchDataService.currentAuctions();

        if (state == null
                || !state.available()) {
            return List.of();
        }

        String needle =
                normalizeSearch(query);

        Map<String, MutableAuctionGroup> groups =
                new LinkedHashMap<>();

        for (MarketWatchAuction auction
                : state.snapshot().auctions()) {

            if (auction == null
                    || auction.itemName().isBlank()
                    || !auction.bin()) {
                continue;
            }

            String searchable =
                    normalizeSearch(
                            auction.itemName()
                                    + " "
                                    + auction.tier()
                                    + " "
                                    + auction.category());

            if (!matchesTokens(
                    searchable,
                    needle)) {
                continue;
            }

            String key =
                    auction.itemName()
                            .toLowerCase(Locale.ROOT)
                            + '\u0000'
                            + auction.tier()
                            .toUpperCase(Locale.ROOT);

            MutableAuctionGroup group =
                    groups.computeIfAbsent(
                            key,
                            ignored ->
                                    new MutableAuctionGroup(
                                            auction.itemName(),
                                            auction.tier(),
                                            auction.category()));

            group.prices.add(
                    Math.max(
                            0L,
                            auction.startingBid()));
        }

        List<AuctionSuggestion> result =
                new ArrayList<>();

        for (MutableAuctionGroup group
                : groups.values()) {

            group.prices.removeIf(
                    price -> price <= 0L);

            if (group.prices.isEmpty()) {
                continue;
            }

            group.prices.sort(
                    Long::compareTo);

            result.add(
                    new AuctionSuggestion(
                            group.itemName,
                            group.tier,
                            group.category,
                            group.prices.getFirst(),
                            median(group.prices),
                            group.prices.size()));
        }

        result.sort(
                Comparator
                        .comparingLong(
                                AuctionSuggestion::lowestBin)
                        .thenComparing(
                                AuctionSuggestion::itemName,
                                String.CASE_INSENSITIVE_ORDER));

        int safeLimit =
                Math.max(
                        0,
                        limit);

        if (result.size() > safeLimit) {
            return List.copyOf(
                    result.subList(
                            0,
                            safeLimit));
        }

        return List.copyOf(result);
    }

    static List<BazaarSuggestion> bazaarSuggestions(
            String query,
            int limit) {

        MarketWatchDataService.BazaarState state =
                MarketWatchDataService.currentBazaar();

        if (state == null
                || !state.available()) {
            return List.of();
        }

        String needle =
                normalizeSearch(query);

        List<BazaarSuggestion> result =
                new ArrayList<>();

        for (MarketWatchBazaarProduct product
                : state.snapshot()
                .products()
                .values()) {

            if (product == null
                    || product.productId().isBlank()) {
                continue;
            }

            String name =
                    displayName(
                            product.productId());

            String searchable =
                    normalizeSearch(
                            name
                                    + " "
                                    + product.productId());

            if (!matchesTokens(
                    searchable,
                    needle)) {
                continue;
            }

            result.add(
                    bazaarSuggestion(
                            product,
                            name));
        }

        result.sort(
                Comparator
                        .comparing(
                                BazaarSuggestion::displayName,
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(
                                BazaarSuggestion::productId));

        int safeLimit =
                Math.max(
                        0,
                        limit);

        if (result.size() > safeLimit) {
            return List.copyOf(
                    result.subList(
                            0,
                            safeLimit));
        }

        return List.copyOf(result);
    }

    static AuctionStats auctionStats(
            String itemName,
            String tier) {

        if (itemName == null
                || itemName.isBlank()) {

            return new AuctionStats(
                    "",
                    "",
                    "",
                    0L,
                    0L,
                    0,
                    -1L);
        }

        MarketWatchDataService.AuctionState state =
                MarketWatchDataService.currentAuctions();

        if (state == null
                || !state.available()) {

            return new AuctionStats(
                    itemName.trim(),
                    cleanTier(tier),
                    "",
                    0L,
                    0L,
                    0,
                    -1L);
        }

        String wantedName =
                itemName.trim();

        String wantedTier =
                cleanTier(tier);

        List<Long> prices =
                new ArrayList<>();

        String category = "";

        for (MarketWatchAuction auction
                : state.snapshot().auctions()) {

            if (auction == null
                    || !auction.bin()
                    || !wantedName.equalsIgnoreCase(
                    auction.itemName())) {

                continue;
            }

            if (!wantedTier.isBlank()
                    && !wantedTier.equalsIgnoreCase(
                    auction.tier())) {

                continue;
            }

            long price =
                    auction.startingBid();

            if (price <= 0L) {
                continue;
            }

            prices.add(price);

            if (category.isBlank()) {
                category =
                        auction.category();
            }
        }

        prices.sort(
                Long::compareTo);

        return new AuctionStats(
                wantedName,
                wantedTier,
                category,
                prices.isEmpty()
                        ? 0L
                        : prices.getFirst(),
                median(prices),
                prices.size(),
                state.observedAtMillis());
    }

    static BazaarStats bazaarStats(
            String productId) {

        String id =
                normalizeId(productId);

        if (id.isBlank()) {
            return new BazaarStats(
                    "",
                    "",
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0L,
                    -1L);
        }

        MarketWatchDataService.BazaarState state =
                MarketWatchDataService.currentBazaar();

        if (state == null
                || !state.available()) {

            return new BazaarStats(
                    id,
                    displayName(id),
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0L,
                    -1L);
        }

        MarketWatchBazaarProduct product =
                state.snapshot().product(id);

        if (product == null) {
            return new BazaarStats(
                    id,
                    displayName(id),
                    0.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    0L,
                    state.observedAtMillis());
        }

        BazaarSuggestion suggestion =
                bazaarSuggestion(
                        product,
                        displayName(id));

        return new BazaarStats(
                suggestion.productId(),
                suggestion.displayName(),
                suggestion.instantBuy(),
                suggestion.instantSell(),
                suggestion.spreadCoins(),
                suggestion.spreadPercent(),
                suggestion.weeklyVolume(),
                state.observedAtMillis());
    }

    static String displayName(
            String productId) {

        String id =
                normalizeId(productId);

        if (id.isBlank()) {
            return "";
        }

        RotItemIndex.ItemDef indexed =
                RotItemIndex.find(id);

        if (indexed != null
                && indexed.name() != null
                && !indexed.name().isBlank()) {

            return indexed.name().trim();
        }

        SkyBlockContentRegistry.KnownItem known =
                SkyBlockContentRegistry
                        .builtin()
                        .lookup(id)
                        .orElse(null);

        if (known != null
                && known.displayName() != null
                && !known.displayName().isBlank()) {

            return known.displayName();
        }

        return humanize(id);
    }

    private static BazaarSuggestion bazaarSuggestion(
            MarketWatchBazaarProduct product,
            String displayName) {

        double buy =
                nonNegative(
                        product.quickBuyPrice());

        double sell =
                nonNegative(
                        product.quickSellPrice());

        double spread =
                Math.max(
                        0.0D,
                        buy - sell);

        double spreadPercent =
                sell > 0.0D
                        ? spread / sell * 100.0D
                        : 0.0D;

        long weeklyVolume =
                Math.max(
                        0L,
                        product.buyMovingWeek())
                        + Math.max(
                        0L,
                        product.sellMovingWeek());

        return new BazaarSuggestion(
                product.productId(),
                displayName,
                buy,
                sell,
                spread,
                spreadPercent,
                weeklyVolume);
    }

    private static boolean matchesTokens(
            String searchable,
            String normalizedQuery) {

        if (normalizedQuery.isBlank()) {
            return true;
        }

        for (String token
                : normalizedQuery.split(" ")) {

            if (!token.isBlank()
                    && !searchable.contains(token)) {

                return false;
            }
        }

        return true;
    }

    private static String normalizeSearch(
            String value) {

        return value == null
                ? ""
                : value
                .toLowerCase(Locale.ROOT)
                .replace('_', ' ')
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String normalizeId(
            String value) {

        return value == null
                ? ""
                : value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static String cleanTier(
            String value) {

        return value == null
                ? ""
                : value
                .trim()
                .toUpperCase(Locale.ROOT);
    }

    private static double nonNegative(
            double value) {

        return Double.isFinite(value)
                && value > 0.0D
                ? value
                : 0.0D;
    }

    private static long median(
            List<Long> sorted) {

        if (sorted == null
                || sorted.isEmpty()) {
            return 0L;
        }

        int size =
                sorted.size();

        int middle =
                size / 2;

        if ((size & 1) == 1) {
            return sorted.get(middle);
        }

        long left =
                sorted.get(
                        middle - 1);

        long right =
                sorted.get(middle);

        return left
                + (right - left) / 2L;
    }

    private static String humanize(
            String id) {

        String[] pieces =
                id.toLowerCase(Locale.ROOT)
                        .split("_");

        StringBuilder result =
                new StringBuilder();

        for (String piece : pieces) {
            if (piece.isBlank()) {
                continue;
            }

            if (!result.isEmpty()) {
                result.append(' ');
            }

            result.append(
                    Character.toUpperCase(
                            piece.charAt(0)));

            if (piece.length() > 1) {
                result.append(
                        piece.substring(1));
            }
        }

        return result.toString();
    }

    private static final class MutableAuctionGroup {
        private final String itemName;
        private final String tier;
        private final String category;

        private final List<Long> prices =
                new ArrayList<>();

        private MutableAuctionGroup(
                String itemName,
                String tier,
                String category) {

            this.itemName =
                    itemName;

            this.tier =
                    tier;

            this.category =
                    category;
        }
    }
}