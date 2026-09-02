package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Bounded development-time cache for public Hypixel SkyBlock metadata.
 *
 * <p>Four requests are made sequentially with no retries. Valid cached data is
 * retained when a network refresh fails. No player, chat, inventory, session,
 * diagnostic, or authentication data is sent.</p>
 */
final class SkyBlockPublicDataCache {
    private static final long MAX_RESPONSE_BYTES = 16L * 1024L * 1024L;

    enum Dataset {
        ITEMS(
                "hypixel-items.json",
                "https://api.hypixel.net/v2/resources/skyblock/items"),
        COLLECTIONS(
                "hypixel-collections.json",
                "https://api.hypixel.net/v2/resources/skyblock/collections"),
        SKILLS(
                "hypixel-skills.json",
                "https://api.hypixel.net/v2/resources/skyblock/skills"),
        BAZAAR(
                "hypixel-bazaar.json",
                "https://api.hypixel.net/v2/skyblock/bazaar");

        private final String fileName;
        private final URI uri;

        Dataset(String fileName, String uri) {
            this.fileName = fileName;
            this.uri = URI.create(uri);
        }

        String fileName() {
            return fileName;
        }

        URI uri() {
            return uri;
        }
    }

    record Result(
            Map<Dataset, Path> files,
            boolean usedNetwork,
            List<String> warnings) {
        Result {
            files = Map.copyOf(files);
            warnings = List.copyOf(warnings);
        }

        Path file(Dataset dataset) {
            Path path = files.get(dataset);
            if (path == null) {
                throw new IllegalArgumentException("No cache file for " + dataset);
            }
            return path;
        }
    }

    private final HttpClient client;

    SkyBlockPublicDataCache() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    SkyBlockPublicDataCache(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    Result prepare(Path cacheDir, boolean allowNetwork)
            throws IOException, InterruptedException {
        Objects.requireNonNull(cacheDir, "cacheDir");
        Files.createDirectories(cacheDir);
        Map<Dataset, Path> files = new EnumMap<>(Dataset.class);
        List<String> warnings = new ArrayList<>();
        boolean usedNetwork = false;

        for (Dataset dataset : Dataset.values()) {
            Path target = cacheDir.resolve(dataset.fileName());
            if (allowNetwork) {
                try {
                    byte[] response = fetch(dataset);
                    validate(dataset, response);
                    writeAtomically(target, response);
                    usedNetwork = true;
                } catch (IOException ex) {
                    if (!isValidCache(dataset, target)) {
                        throw ex;
                    }
                    warnings.add(dataset.name()
                            + " network refresh failed; retained valid cache: "
                            + bounded(ex.getMessage()));
                }
            }
            if (!isValidCache(dataset, target)) {
                throw new IOException(
                        "Missing or invalid " + dataset.name()
                                + " cache at " + target);
            }
            files.put(dataset, target);
        }
        return new Result(files, usedNetwork, warnings);
    }

    private byte[] fetch(Dataset dataset)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(dataset.uri())
                .timeout(Duration.ofSeconds(120))
                .header("Accept", "application/json")
                .header("User-Agent", "RotClient-SkyBlockDataAudit/2.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(
                request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() / 100 != 2) {
            throw new IOException(
                    "HTTP " + response.statusCode() + " for " + dataset.uri());
        }
        byte[] body = response.body();
        if (body == null || body.length == 0) {
            throw new IOException("Empty response for " + dataset.uri());
        }
        if (body.length > MAX_RESPONSE_BYTES) {
            throw new IOException(
                    "Response exceeded " + MAX_RESPONSE_BYTES
                            + " bytes for " + dataset.uri());
        }
        return body;
    }

    private static boolean isValidCache(Dataset dataset, Path path) {
        if (!Files.isRegularFile(path)) return false;
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (bytes.length > MAX_RESPONSE_BYTES) return false;
            validate(dataset, bytes);
            return true;
        } catch (IOException | RuntimeException ex) {
            return false;
        }
    }

    private static void validate(Dataset dataset, byte[] bytes)
            throws IOException {
        try {
            JsonObject root = JsonParser.parseString(
                    new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!root.has("success") || !root.get("success").getAsBoolean()) {
                throw new IOException(dataset.name() + " response was unsuccessful");
            }
            String required = switch (dataset) {
                case ITEMS -> "items";
                case COLLECTIONS -> "collections";
                case SKILLS -> "skills";
                case BAZAAR -> "products";
            };
            if (!root.has(required)) {
                throw new IOException(
                        dataset.name() + " response missing " + required);
            }
        } catch (IllegalStateException | com.google.gson.JsonParseException ex) {
            throw new IOException(dataset.name() + " response was not valid JSON", ex);
        }
    }

    private static void writeAtomically(Path target, byte[] bytes)
            throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IOException("Cache target has no parent: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(
                parent, target.getFileName().toString() + ".", ".tmp");
        boolean moved = false;
        try {
            Files.write(temporary, bytes);
            try {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporary,
                        target,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) {
                Files.deleteIfExists(temporary);
            }
        }
    }

    private static String bounded(String value) {
        if (value == null) return "unknown";
        String oneLine = value.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= 200 ? oneLine : oneLine.substring(0, 200);
    }
}
