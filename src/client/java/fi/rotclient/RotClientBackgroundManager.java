package fi.rotclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;

/**
 * Discovers and draws optional custom UI backgrounds from
 * {@code config/rotclient/backgrounds/}.
 *
 * <p>Images are decoded once into a cached ARGB sample grid and reused while
 * rendering. Failed or unsafe paths fall back to the normal theme backdrop.
 *
 * <p>Limitation: Minecraft DynamicTexture upload is intentionally avoided for
 * API stability on 26.2; the sample grid is a coarse but stable approximation.
 */
final class RotClientBackgroundManager {
    static final String FOLDER_NAME = "backgrounds";
    private static final int SAMPLE_WIDTH = 160;
    private static final int SAMPLE_HEIGHT = 90;
    private static final long MAX_BYTES = 12L * 1024L * 1024L;

    private static volatile String loadedFile = "";
    private static volatile int[] sampleArgb;
    private static volatile int sampleWidth;
    private static volatile int sampleHeight;

    private RotClientBackgroundManager() {
    }

    static Path backgroundsDirectory() {
        return FabricLoader.getInstance()
                .getConfigDir()
                .resolve("rotclient")
                .resolve(FOLDER_NAME);
    }

    static Path ensureBackgroundsDirectory() {
        Path dir = backgroundsDirectory();
        try {
            Files.createDirectories(dir);
        } catch (IOException ignored) {
        }
        return dir;
    }

    static List<String> listBackgroundFiles() {
        Path dir = ensureBackgroundsDirectory();
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(dir)) {
            return names;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                if (!Files.isRegularFile(path)) {
                    continue;
                }
                String name = path.getFileName().toString();
                if (isSupportedImageName(name)
                        && isSafeFileName(name)
                        && resolveSafeBackground(name) != null) {
                    names.add(name);
                }
            }
        } catch (IOException ignored) {
        }
        names.sort(Comparator.naturalOrder());
        return names;
    }

    static boolean isSupportedImageName(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".gif");
    }

    static boolean isSafeFileName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.contains("..")
                || name.contains("/")
                || name.contains("\\")
                || name.contains(":")
                || name.length() > 120) {
            return false;
        }
        return true;
    }

    /**
     * Resolves a background file under the backgrounds directory only.
     * Returns null on path traversal or missing files.
     */
    static Path resolveSafeBackground(String fileName) {
        if (!isSafeFileName(fileName) || !isSupportedImageName(fileName)) {
            return null;
        }
        Path dir = backgroundsDirectory().toAbsolutePath().normalize();
        Path resolved = dir.resolve(fileName).toAbsolutePath().normalize();
        if (!resolved.startsWith(dir)) {
            return null;
        }
        return resolved;
    }

    static boolean openBackgroundsFolder() {
        Path dir = ensureBackgroundsDirectory();
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(dir.toFile());
                return true;
            }
        } catch (Exception ignored) {
        }
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("explorer", dir.toAbsolutePath().toString()).start();
                return true;
            }
            if (os.contains("mac")) {
                new ProcessBuilder("open", dir.toAbsolutePath().toString()).start();
                return true;
            }
            new ProcessBuilder("xdg-open", dir.toAbsolutePath().toString()).start();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static String backgroundsPathHint() {
        return ensureBackgroundsDirectory().toAbsolutePath().toString();
    }

    static void invalidateCache() {
        loadedFile = "";
        sampleArgb = null;
        sampleWidth = 0;
        sampleHeight = 0;
    }

    static void drawIfEnabled(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            RotClientAppearanceConfig config) {
        if (config == null || !config.customBackgroundEnabled) {
            return;
        }
        if (!ensureSampleLoaded(config.customBackgroundFile)) {
            return;
        }
        int[] sample = sampleArgb;
        if (sample == null || sampleWidth <= 0 || sampleHeight <= 0) {
            return;
        }

        int opacity = RotClientAppearanceConfig.clamp(
                config.customBackgroundOpacity, 0, 255);
        String fit = config.customBackgroundFit == null
                ? "fill"
                : config.customBackgroundFit;
        for (int sy = 0; sy < sampleHeight; sy++) {
            for (int sx = 0; sx < sampleWidth; sx++) {
                int argb = sample[sy * sampleWidth + sx];
                int mapped = RotClientAppearanceConfig.withAlpha(argb, opacity);
                int dx;
                int dy;
                int dw;
                int dh;
                if ("stretch".equals(fit)) {
                    dx = x + sx * width / sampleWidth;
                    dy = y + sy * height / sampleHeight;
                    dw = Math.max(1, (x + (sx + 1) * width / sampleWidth) - dx);
                    dh = Math.max(1, (y + (sy + 1) * height / sampleHeight) - dy);
                } else if ("fit".equals(fit)) {
                    float scale = Math.min(
                            width / (float) sampleWidth,
                            height / (float) sampleHeight);
                    int drawW = Math.max(1, Math.round(sampleWidth * scale));
                    int drawH = Math.max(1, Math.round(sampleHeight * scale));
                    int ox = x + (width - drawW) / 2;
                    int oy = y + (height - drawH) / 2;
                    dx = ox + sx * drawW / sampleWidth;
                    dy = oy + sy * drawH / sampleHeight;
                    dw = Math.max(1, (ox + (sx + 1) * drawW / sampleWidth) - dx);
                    dh = Math.max(1, (oy + (sy + 1) * drawH / sampleHeight) - dy);
                } else {
                    float scale = Math.max(
                            width / (float) sampleWidth,
                            height / (float) sampleHeight);
                    int drawW = Math.max(1, Math.round(sampleWidth * scale));
                    int drawH = Math.max(1, Math.round(sampleHeight * scale));
                    int ox = x + (width - drawW) / 2;
                    int oy = y + (height - drawH) / 2;
                    dx = ox + sx * drawW / sampleWidth;
                    dy = oy + sy * drawH / sampleHeight;
                    dw = Math.max(1, (ox + (sx + 1) * drawW / sampleWidth) - dx);
                    dh = Math.max(1, (oy + (sy + 1) * drawH / sampleHeight) - dy);
                }
                if (dx + dw < x || dy + dh < y
                        || dx > x + width || dy > y + height) {
                    continue;
                }
                graphics.fill(dx, dy, dx + dw, dy + dh, mapped);
            }
        }

        int dim = RotClientAppearanceConfig.clamp(config.customBackgroundDim, 0, 255);
        if (dim > 0) {
            graphics.fill(
                    x,
                    y,
                    x + width,
                    y + height,
                    (dim << 24));
        }
    }

    private static boolean ensureSampleLoaded(String fileName) {
        Path path = resolveSafeBackground(fileName);
        if (path == null) {
            invalidateCache();
            return false;
        }
        if (fileName.equals(loadedFile) && sampleArgb != null) {
            return true;
        }
        if (!Files.isRegularFile(path)) {
            invalidateCache();
            return false;
        }
        try {
            long size = Files.size(path);
            if (size <= 0 || size > MAX_BYTES) {
                invalidateCache();
                return false;
            }
            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null
                    || image.getWidth() <= 0
                    || image.getHeight() <= 0
                    || image.getWidth() > 8192
                    || image.getHeight() > 8192) {
                invalidateCache();
                return false;
            }
            BufferedImage rgb = new BufferedImage(
                    SAMPLE_WIDTH,
                    SAMPLE_HEIGHT,
                    BufferedImage.TYPE_INT_ARGB);
            var g = rgb.createGraphics();
            g.drawImage(image, 0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT, null);
            g.dispose();
            int[] pixels = new int[SAMPLE_WIDTH * SAMPLE_HEIGHT];
            rgb.getRGB(0, 0, SAMPLE_WIDTH, SAMPLE_HEIGHT, pixels, 0, SAMPLE_WIDTH);
            sampleArgb = pixels;
            sampleWidth = SAMPLE_WIDTH;
            sampleHeight = SAMPLE_HEIGHT;
            loadedFile = fileName;
            return true;
        } catch (Exception ignored) {
            invalidateCache();
            return false;
        }
    }
}
