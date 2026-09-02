package fi.rotclient;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Observes exact gemstone inventory and Mining Sacks events.
 *
 * This class never changes tracker state. It only writes diagnostic events
 * while DiagnosticRecorder is active.
 */
final class GemstoneDiagnosticObserver {
    private static final long SACK_DUPLICATE_WINDOW_MILLIS =
            2_500L;

    private static final Pattern SACK_ITEM =
            Pattern.compile(
                    "\\+([\\d,]+)\\s+([^\\n(]+?)\\s*\\(",
                    Pattern.CASE_INSENSITIVE);

    private Map<GemstoneType, EnumMap<GemstoneTier, Long>>
            previousInventory = emptySnapshot();

    private boolean inventoryInitialized;
    private String lastSackFingerprint = "";
    private long lastSackEpochMillis;

    void tick(Minecraft client) {
        observeInventory(client, "tick");
    }

    void onInventoryPacket(
            Minecraft client,
            String packetType) {
        String source =
                packetType == null || packetType.isBlank()
                        ? "inventory-packet"
                        : packetType;

        observeInventory(client, source);
    }

    void inspectMessage(Component message) {
        if (!DiagnosticRecorder.isRecording()
                || message == null) {
            return;
        }

        String plainMessage =
                message.getString().trim();

        if (!plainMessage.regionMatches(
                true,
                0,
                "[Sacks]",
                0,
                7)) {
            return;
        }

        String addedText =
                firstAddedHoverText(message);

        if (addedText == null) {
            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_MESSAGE",
                    "result=no-added-items-hover");
            return;
        }

        long now =
                System.currentTimeMillis();

        String fingerprint =
                plainMessage + "\n" + addedText;

        if (fingerprint.equals(lastSackFingerprint)
                && now - lastSackEpochMillis
                <= SACK_DUPLICATE_WINDOW_MILLIS) {
            return;
        }

        lastSackFingerprint = fingerprint;
        lastSackEpochMillis = now;

        List<String> hoverLines =
                diagnosticHoverLines(addedText);

        DiagnosticRecorder.record(
                "GEMSTONE_SACK_HOVER",
                "lineCount=" + hoverLines.size());

        for (int index = 0;
             index < hoverLines.size();
             index++) {
            String line = hoverLines.get(index);

            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_HOVER_LINE",
                    "index=" + index
                            + " text=" + line
                            + " codepoints="
                            + diagnosticCodePoints(line));
        }

        List<GemstoneSackEntry> entries =
                parseAddedItems(addedText);

        if (entries.isEmpty()) {
            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_MESSAGE",
                    "result=no-gemstone-entry");
            return;
        }

        for (GemstoneSackEntry entry : entries) {
            DiagnosticRecorder.record(
                    "GEMSTONE_SACK_OBSERVED",
                    "gemstone="
                            + entry.gemstone().id()
                            + " tier="
                            + entry.tier().id()
                            + " amount="
                            + entry.amount());
        }
    }

    private void observeInventory(
            Minecraft client,
            String source) {
        if (client == null
                || client.player == null) {
            previousInventory = emptySnapshot();
            inventoryInitialized = false;
            return;
        }

        Map<GemstoneType, EnumMap<GemstoneTier, Long>>
                current = inventorySnapshot(client);

        if (inventoryInitialized
                && DiagnosticRecorder.isRecording()) {
            for (GemstoneInventoryDelta delta :
                    diffSnapshots(
                            previousInventory,
                            current)) {
                DiagnosticRecorder.record(
                        "GEMSTONE_INVENTORY_DELTA",
                        "gemstone="
                                + delta.gemstone().id()
                                + " tier="
                                + delta.tier().id()
                                + " before="
                                + delta.before()
                                + " after="
                                + delta.after()
                                + " delta="
                                + delta.delta()
                                + " source="
                                + source);
            }
        }

        previousInventory = current;
        inventoryInitialized = true;
    }

    private static Map<GemstoneType, EnumMap<GemstoneTier, Long>>
    inventorySnapshot(Minecraft client) {
        Map<GemstoneType, EnumMap<GemstoneTier, Long>>
                snapshot = emptySnapshot();

        for (ItemStack stack :
                client.player
                        .getInventory()
                        .getNonEquipmentItems()) {
            if (stack.isEmpty()) {
                continue;
            }

            GemstoneItem item =
                    matchItemName(
                            stack.getHoverName()
                                    .getString()
                                    .trim());

            if (item == null) {
                continue;
            }

            EnumMap<GemstoneTier, Long> quantities =
                    snapshot.get(item.gemstone());

            long current =
                    quantities.get(item.tier());

            quantities.put(
                    item.tier(),
                    safeAdd(
                            current,
                            stack.getCount()));
        }

        return snapshot;
    }

    static List<String> diagnosticHoverLines(
            String text) {
        List<String> lines =
                new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return lines;
        }

        String[] rawLines =
                text.split("\\R", -1);

        for (String rawLine : rawLines) {
            String line =
                    rawLine.replace("\r", "");

            if (!line.isBlank()) {
                lines.add(line);
            }
        }

        return lines;
    }

    static String diagnosticCodePoints(
            String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        int offset = 0;
        int count = 0;
        int totalCodePoints =
                value.codePointCount(
                        0,
                        value.length());

        while (offset < value.length()
                && count < 160) {
            int codePoint =
                    value.codePointAt(offset);

            if (result.length() > 0) {
                result.append(',');
            }

            result.append("U+");

            String hex =
                    Integer.toHexString(codePoint)
                            .toUpperCase(
                                    java.util.Locale.ROOT);

            for (int padding = hex.length();
                 padding < 4;
                 padding++) {
                result.append('0');
            }

            result.append(hex);

            offset +=
                    Character.charCount(codePoint);

            count++;
        }

        if (count < totalCodePoints) {
            result.append(",...");
        }

        return result.toString();
    }
    static GemstoneItem matchItemName(
            String itemName) {
        GemstoneType gemstone =
                GemstoneType.fromItemName(itemName);

        GemstoneTier tier =
                GemstoneTier.fromItemName(itemName);

        if (gemstone == null || tier == null) {
            return null;
        }

        return new GemstoneItem(
                gemstone,
                tier);
    }

    static String stripSackIcons(
            String value) {
        if (value == null || value.isEmpty()) {
            return value == null ? "" : value;
        }

        StringBuilder result =
                new StringBuilder(value.length());

        int offset = 0;

        while (offset < value.length()) {
            int codePoint =
                    value.codePointAt(offset);

            int type =
                    Character.getType(codePoint);

            boolean removable =
                    type == Character.PRIVATE_USE
                            || type == Character.FORMAT;

            if (!removable) {
                result.appendCodePoint(codePoint);
            }

            offset +=
                    Character.charCount(codePoint);
        }

        return result.toString();
    }
    static List<GemstoneSackEntry> parseAddedItems(
            String addedText) {
        List<GemstoneSackEntry> entries =
                new ArrayList<>();

        if (addedText == null
                || addedText.isBlank()) {
            return entries;
        }

        String normalizedText =
                stripSackIcons(addedText);

        Matcher matcher =
                SACK_ITEM.matcher(normalizedText);

        while (matcher.find()) {
            long amount =
                    parseLong(matcher.group(1));

            GemstoneItem item =
                    matchItemName(
                            matcher.group(2).trim());

            if (amount <= 0L || item == null) {
                continue;
            }

            entries.add(
                    new GemstoneSackEntry(
                            item.gemstone(),
                            item.tier(),
                            amount));
        }

        return entries;
    }

    static List<GemstoneInventoryDelta> diffSnapshots(
            Map<GemstoneType, EnumMap<GemstoneTier, Long>> before,
            Map<GemstoneType, EnumMap<GemstoneTier, Long>> after) {
        List<GemstoneInventoryDelta> deltas =
                new ArrayList<>();

        for (GemstoneType gemstone :
                GemstoneType.values()) {
            for (GemstoneTier tier :
                    GemstoneTier.values()) {
                long previous =
                        quantity(
                                before,
                                gemstone,
                                tier);

                long current =
                        quantity(
                                after,
                                gemstone,
                                tier);

                if (previous == current) {
                    continue;
                }

                deltas.add(
                        new GemstoneInventoryDelta(
                                gemstone,
                                tier,
                                previous,
                                current,
                                current - previous));
            }
        }

        return deltas;
    }

    static Map<GemstoneType, EnumMap<GemstoneTier, Long>>
    emptySnapshot() {
        Map<GemstoneType, EnumMap<GemstoneTier, Long>>
                snapshot =
                new EnumMap<>(GemstoneType.class);

        for (GemstoneType gemstone :
                GemstoneType.values()) {
            EnumMap<GemstoneTier, Long> quantities =
                    new EnumMap<>(GemstoneTier.class);

            for (GemstoneTier tier :
                    GemstoneTier.values()) {
                quantities.put(tier, 0L);
            }

            snapshot.put(gemstone, quantities);
        }

        return snapshot;
    }

    private static long quantity(
            Map<GemstoneType, EnumMap<GemstoneTier, Long>> snapshot,
            GemstoneType gemstone,
            GemstoneTier tier) {
        if (snapshot == null) {
            return 0L;
        }

        EnumMap<GemstoneTier, Long> quantities =
                snapshot.get(gemstone);

        if (quantities == null) {
            return 0L;
        }

        Long value =
                quantities.get(tier);

        return value == null
                ? 0L
                : Math.max(0L, value);
    }

    private static String firstAddedHoverText(
            Component component) {
        HoverEvent hover =
                component.getStyle().getHoverEvent();

        if (hover instanceof HoverEvent.ShowText showText) {
            String text =
                    showText.value().getString();

            if (text.regionMatches(
                    true,
                    0,
                    "Added items:",
                    0,
                    12)) {
                return text;
            }
        }

        for (Component sibling :
                component.getSiblings()) {
            String found =
                    firstAddedHoverText(sibling);

            if (found != null) {
                return found;
            }
        }

        return null;
    }

    private static long parseLong(
            String value) {
        try {
            return Long.parseLong(
                    value.replace(",", ""));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private static long safeAdd(
            long left,
            long right) {
        long safeLeft =
                Math.max(0L, left);

        long safeRight =
                Math.max(0L, right);

        if (safeLeft
                > Long.MAX_VALUE - safeRight) {
            return Long.MAX_VALUE;
        }

        return safeLeft + safeRight;
    }

    record GemstoneItem(
            GemstoneType gemstone,
            GemstoneTier tier) {
    }

    record GemstoneSackEntry(
            GemstoneType gemstone,
            GemstoneTier tier,
            long amount) {
    }

    record GemstoneInventoryDelta(
            GemstoneType gemstone,
            GemstoneTier tier,
            long before,
            long after,
            long delta) {
    }
}
