package fi.rotclient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Development-time public metadata refresh. Fetches only public Hypixel
 * resources. Never uploads user/session/inventory/chat data.
 */
final class SkyBlockDataRefresh {
    record Result(
            SkyBlockCanonicalDataset dataset,
            Path cacheDir,
            Path outputDir,
            boolean usedNetwork) {
    }

    private final HttpClient client;

    SkyBlockDataRefresh() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build());
    }

    SkyBlockDataRefresh(HttpClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    Result refresh(
            Path cacheDir,
            Path outputDir,
            boolean allowNetwork,
            List<SkyBlockDataReconciler.EnrichmentHint> enrichment)
            throws IOException, InterruptedException {
        Files.createDirectories(cacheDir);
        Files.createDirectories(outputDir);

        SkyBlockPublicDataCache.Result cache =
                new SkyBlockPublicDataCache(client).prepare(
                        cacheDir, allowNetwork);
        Path itemsCache = cache.file(SkyBlockPublicDataCache.Dataset.ITEMS);
        Path bazaarCache = cache.file(SkyBlockPublicDataCache.Dataset.BAZAAR);

        SkyBlockOfficialSnapshot items =
                SkyBlockOfficialSnapshot.parseItemsJson(
                        Files.readString(itemsCache, StandardCharsets.UTF_8));
        var productIds = SkyBlockOfficialSnapshot.parseBazaarProductIdsJson(
                Files.readString(bazaarCache, StandardCharsets.UTF_8));
        SkyBlockOfficialSnapshot official = items.withBazaarProductIds(productIds);

        SkyBlockDomainRules domain = SkyBlockDomainRules.fromBuiltinRegistry();
        SkyBlockCanonicalDataset dataset = SkyBlockDataReconciler.reconcile(
                domain,
                official,
                enrichment == null ? List.of() : enrichment,
                Instant.now().toString());
        SkyBlockCanonicalDatasetLoader.writeDatasetBundle(outputDir, dataset);
        return new Result(dataset, cacheDir, outputDir, cache.usedNetwork());
    }
}
