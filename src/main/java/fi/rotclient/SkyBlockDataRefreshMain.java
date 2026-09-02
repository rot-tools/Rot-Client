package fi.rotclient;

import java.nio.file.Path;

/**
 * CLI entrypoint for {@code refreshSkyBlockData}. Public metadata only.
 */
public final class SkyBlockDataRefreshMain {
    private SkyBlockDataRefreshMain() {
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path cacheDir = root.resolve("build").resolve("skyblock-data-cache");
        Path outputDir = root.resolve("build")
                .resolve("skyblock-data-review");
        boolean allowNetwork = true;
        boolean adopt = false;
        for (String arg : args) {
            if ("--offline".equals(arg)) {
                allowNetwork = false;
            } else if ("--adopt".equals(arg)) {
                adopt = true;
            } else if (arg.startsWith("--cacheDir=")) {
                cacheDir = Path.of(arg.substring("--cacheDir=".length()));
            } else if (arg.startsWith("--outputDir=")) {
                outputDir = Path.of(arg.substring("--outputDir=".length()));
            }
        }
        if (adopt) {
            outputDir = root.resolve("src")
                    .resolve("main")
                    .resolve("resources")
                    .resolve("assets")
                    .resolve("rotclient")
                    .resolve("data");
        }

        SkyBlockDataRefresh.Result result = new SkyBlockDataRefresh()
                .refresh(cacheDir, outputDir, allowNetwork, java.util.List.of());
        System.out.println("SkyBlock data refresh complete");
        System.out.println("usedNetwork=" + result.usedNetwork());
        System.out.println("items=" + result.dataset().items().size());
        System.out.println("conflicts=" + result.dataset().conflicts().size());
        System.out.println("bazaarProducts="
                + result.dataset().bazaarProductIds().size());
        System.out.println("output=" + result.outputDir());
        System.out.println("adopted=" + adopt);
    }
}
