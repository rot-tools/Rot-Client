package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class MarketWatchSellerNameService {
    private static final String PROFILE_URL =
            "https://sessionserver.mojang.com/session/minecraft/profile/";

    private static final long FAILED_RETRY_MILLIS =
            10L * 60L * 1000L;

    private static final HttpClient HTTP =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(4))
                    .build();

    private static final ConcurrentHashMap<String, String> NAMES =
            new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<String, Long> FAILED_UNTIL =
            new ConcurrentHashMap<>();

    private static final Set<String> PENDING =
            ConcurrentHashMap.newKeySet();

    private MarketWatchSellerNameService() {
    }

    static String displayName(
            String rawUuid) {

        String uuid =
                normalizeUuid(
                        rawUuid);

        if (uuid.isBlank()) {
            return "";
        }

        String cached =
                NAMES.get(
                        uuid);

        if (cached != null
                && !cached.isBlank()) {

            return cached;
        }

        long now =
                System.currentTimeMillis();

        Long blockedUntil =
                FAILED_UNTIL.get(
                        uuid);

        if (blockedUntil != null
                && blockedUntil > now) {

            return "Unavailable";
        }

        if (PENDING.add(
                uuid)) {

            resolveAsync(
                    uuid);
        }

        return "Resolving...";
    }

    private static void resolveAsync(
            String uuid) {

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(
                                URI.create(
                                        PROFILE_URL
                                                + uuid))
                        .timeout(
                                Duration.ofSeconds(5))
                        .header(
                                "Accept",
                                "application/json")
                        .header(
                                "User-Agent",
                                "RotClient-MarketWatch")
                        .GET()
                        .build();

        HTTP.sendAsync(
                        request,
                        HttpResponse.BodyHandlers
                                .ofString(
                                        StandardCharsets.UTF_8))
                .whenComplete(
                        (response, error) -> {

                            try {
                                if (error != null
                                        || response == null
                                        || response.statusCode()
                                        != 200) {

                                    rememberFailure(
                                            uuid);

                                    return;
                                }

                                JsonObject root =
                                        JsonParser
                                                .parseString(
                                                        response.body())
                                                .getAsJsonObject();

                                String name =
                                        root.has("name")
                                                && !root
                                                .get("name")
                                                .isJsonNull()
                                                ? root
                                                .get("name")
                                                .getAsString()
                                                .trim()
                                                : "";

                                if (name.isBlank()) {
                                    rememberFailure(
                                            uuid);

                                    return;
                                }

                                NAMES.put(
                                        uuid,
                                        name);

                                FAILED_UNTIL.remove(
                                        uuid);

                            } catch (RuntimeException ignored) {
                                rememberFailure(
                                        uuid);

                            } finally {
                                PENDING.remove(
                                        uuid);
                            }
                        });
    }

    private static void rememberFailure(
            String uuid) {

        FAILED_UNTIL.put(
                uuid,
                System.currentTimeMillis()
                        + FAILED_RETRY_MILLIS);
    }

    private static String normalizeUuid(
            String rawUuid) {

        if (rawUuid == null) {
            return "";
        }

        String value =
                rawUuid
                        .trim()
                        .replace(
                                "-",
                                "")
                        .toLowerCase(
                                Locale.ROOT);

        if (!value.matches(
                "[0-9a-f]{32}")) {

            return "";
        }

        return value;
    }
}