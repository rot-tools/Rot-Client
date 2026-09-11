package fi.rotclient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

final class MarketWatchOpportunityPreferences {

    static final double DEFAULT_BUDGET =
            25_000_000.0D;

    static final double MAX_BUDGET =
            100_000_000_000.0D;

    private static final Path FILE =
            Path.of(
                    "config",
                    "rotclient-market-watch-opportunities.properties");

    private static final String BUDGET_KEY =
            "tradingBudgetCoins";

    private static final String FILTER_RARITIES_KEY =
            "filter.rarities";

    private static final String FILTER_TYPES_KEY =
            "filter.types";

    private static final String FILTER_LIQUIDITY_KEY =
            "filter.liquidity";

    private static final String FILTER_CONFIDENCE_KEY =
            "filter.confidence";

    private static final String FILTER_ROI_KEY =
            "filter.roi";

    private static final String FILTER_PROFIT_KEY =
            "filter.profit";

    private static final String FILTER_EDGE_KEY =
            "filter.edge";

    private static final String FILTER_VOLUME_KEY =
            "filter.volume";

    private MarketWatchOpportunityPreferences() {
    }

    static synchronized double loadBudgetCoins() {

        Properties properties =
                readProperties();

        String raw =
                properties.getProperty(
                        BUDGET_KEY);

        if (raw == null
                || raw.isBlank()) {

            return DEFAULT_BUDGET;
        }

        try {
            return normalizeBudget(
                    Double.parseDouble(
                            raw.trim()));

        } catch (NumberFormatException ignored) {
            return DEFAULT_BUDGET;
        }
    }

    static synchronized void saveBudgetCoins(
            double budgetCoins) {

        Properties properties =
                readProperties();

        properties.setProperty(
                BUDGET_KEY,
                Double.toString(
                        normalizeBudget(
                                budgetCoins)));

        writeProperties(
                properties);
    }

    static synchronized FilterPreferences
    loadFilterPreferences() {

        Properties properties =
                readProperties();

        return new FilterPreferences(
                property(
                        properties,
                        FILTER_RARITIES_KEY,
                        ""),
                property(
                        properties,
                        FILTER_TYPES_KEY,
                        ""),
                property(
                        properties,
                        FILTER_LIQUIDITY_KEY,
                        "ANY"),
                property(
                        properties,
                        FILTER_CONFIDENCE_KEY,
                        "ANY"),
                property(
                        properties,
                        FILTER_ROI_KEY,
                        "ANY"),
                property(
                        properties,
                        FILTER_PROFIT_KEY,
                        "ANY"),
                property(
                        properties,
                        FILTER_EDGE_KEY,
                        "ANY"),
                property(
                        properties,
                        FILTER_VOLUME_KEY,
                        "ANY"));
    }

    static synchronized void saveFilterPreferences(
            FilterPreferences preferences) {

        if (preferences == null) {
            return;
        }

        Properties properties =
                readProperties();

        properties.setProperty(
                FILTER_RARITIES_KEY,
                safe(
                        preferences.rarities()));

        properties.setProperty(
                FILTER_TYPES_KEY,
                safe(
                        preferences.types()));

        properties.setProperty(
                FILTER_LIQUIDITY_KEY,
                safeOr(
                        preferences.liquidity(),
                        "ANY"));

        properties.setProperty(
                FILTER_CONFIDENCE_KEY,
                safeOr(
                        preferences.confidence(),
                        "ANY"));

        properties.setProperty(
                FILTER_ROI_KEY,
                safeOr(
                        preferences.roi(),
                        "ANY"));

        properties.setProperty(
                FILTER_PROFIT_KEY,
                safeOr(
                        preferences.profit(),
                        "ANY"));

        properties.setProperty(
                FILTER_EDGE_KEY,
                safeOr(
                        preferences.edge(),
                        "ANY"));

        properties.setProperty(
                FILTER_VOLUME_KEY,
                safeOr(
                        preferences.volume(),
                        "ANY"));

        writeProperties(
                properties);
    }

    static double normalizeBudget(
            double value) {

        if (!Double.isFinite(value)
                || value <= 0.0D) {

            return DEFAULT_BUDGET;
        }

        return Math.min(
                MAX_BUDGET,
                Math.max(
                        1.0D,
                        value));
    }

    private static Properties readProperties() {

        Properties properties =
                new Properties();

        if (!Files.isRegularFile(FILE)) {
            return properties;
        }

        try (Reader reader =
                     Files.newBufferedReader(
                             FILE,
                             StandardCharsets.UTF_8)) {

            properties.load(
                    reader);

        } catch (IOException ignored) {
            // Corrupt or unavailable settings must not affect the client.
        }

        return properties;
    }

    private static void writeProperties(
            Properties properties) {

        if (properties == null) {
            return;
        }

        try {
            Path parent =
                    FILE.getParent();

            if (parent != null) {
                Files.createDirectories(
                        parent);
            }

            try (Writer writer =
                         Files.newBufferedWriter(
                                 FILE,
                                 StandardCharsets.UTF_8)) {

                properties.store(
                        writer,
                        "Rot Client Market Watch Opportunities");
            }

        } catch (IOException ignored) {
            // A settings write failure must never affect the client.
        }
    }

    private static String property(
            Properties properties,
            String key,
            String fallback) {

        if (properties == null
                || key == null) {

            return fallback;
        }

        String value =
                properties.getProperty(
                        key);

        return value == null
                ? fallback
                : value.trim();
    }

    private static String safe(
            String value) {

        return value == null
                ? ""
                : value.trim();
    }

    private static String safeOr(
            String value,
            String fallback) {

        String safe =
                safe(
                        value);

        return safe.isBlank()
                ? fallback
                : safe;
    }

    record FilterPreferences(
            String rarities,
            String types,
            String liquidity,
            String confidence,
            String roi,
            String profit,
            String edge,
            String volume) {
    }
}