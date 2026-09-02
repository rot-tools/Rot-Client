package fi.rotclient;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** CLI for a review-only official-vs-Rot mining reconciliation report. */
public final class SkyBlockMiningAuditMain {
    private SkyBlockMiningAuditMain() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path cacheDir = root.resolve("build").resolve("skyblock-data-cache");
        Path reportPath = root.resolve("build")
                .resolve("reports")
                .resolve("skyblock-data")
                .resolve("mining-reconciliation.md");
        boolean allowNetwork = true;
        for (String arg : args) {
            if ("--offline".equals(arg)) {
                allowNetwork = false;
            } else if (arg.startsWith("--cacheDir=")) {
                cacheDir = Path.of(arg.substring("--cacheDir=".length()));
            } else if (arg.startsWith("--report=")) {
                reportPath = Path.of(arg.substring("--report=".length()));
            }
        }

        SkyBlockPublicDataCache.Result cache =
                new SkyBlockPublicDataCache().prepare(cacheDir, allowNetwork);
        String itemsJson = read(cache.file(SkyBlockPublicDataCache.Dataset.ITEMS));
        String collectionsJson = read(cache.file(
                SkyBlockPublicDataCache.Dataset.COLLECTIONS));
        String skillsJson = read(cache.file(SkyBlockPublicDataCache.Dataset.SKILLS));
        String bazaarJson = read(cache.file(SkyBlockPublicDataCache.Dataset.BAZAAR));
        SkyBlockOfficialSnapshot official = SkyBlockOfficialSnapshot
                .parseItemsJson(itemsJson)
                .withBazaarProductIds(
                        SkyBlockOfficialSnapshot.parseBazaarProductIdsJson(
                                bazaarJson));
        SkyBlockOfficialCollectionsSnapshot collections =
                SkyBlockOfficialCollectionsSnapshot.parse(collectionsJson);
        SkyBlockMiningResourceRegistry mechanics =
                SkyBlockMiningResourceRegistry.bundled();
        SkyBlockCanonicalDataset canonical = SkyBlockCanonicalDatasetLoader
                .loadBundled()
                .orElseThrow(() -> new IllegalStateException(
                        "Bundled canonical item dataset is missing"));

        SkyBlockMiningReconciliation.Report report =
                SkyBlockMiningReconciliation.reconcile(
                        mechanics, canonical, official, collections);
        String markdown = SkyBlockMiningReconciliationReport.render(
                report,
                mechanics,
                official,
                collections,
                lastUpdated(skillsJson),
                lastUpdated(bazaarJson),
                cache.usedNetwork(),
                cache.warnings());
        writeAtomically(reportPath, markdown);

        System.out.println("SkyBlock mining reconciliation complete");
        System.out.println("usedNetwork=" + cache.usedNetwork());
        System.out.println("officialCollections="
                + report.officialCollectionCount());
        System.out.println("rotCollections=" + report.rotCollectionCount());
        System.out.println("conflicts=" + report.statusCounts().getOrDefault(
                SkyBlockMiningReconciliation.Status.CONFLICT, 0));
        System.out.println("unresolved=" + report.statusCounts().getOrDefault(
                SkyBlockMiningReconciliation.Status.UNRESOLVED, 0));
        System.out.println("report=" + reportPath.toAbsolutePath());
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static long lastUpdated(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.has("lastUpdated")
                ? Math.max(0L, root.get("lastUpdated").getAsLong())
                : 0L;
    }

    private static void writeAtomically(Path target, String value)
            throws IOException {
        Path absolute = target.toAbsolutePath();
        Path parent = absolute.getParent();
        if (parent == null) {
            throw new IOException("Report target has no parent: " + target);
        }
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(
                parent, absolute.getFileName().toString() + ".", ".tmp");
        boolean moved = false;
        try {
            Files.writeString(temporary, value, StandardCharsets.UTF_8);
            try {
                Files.move(
                        temporary,
                        absolute,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(
                        temporary,
                        absolute,
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moved = true;
        } finally {
            if (!moved) Files.deleteIfExists(temporary);
        }
    }
}
