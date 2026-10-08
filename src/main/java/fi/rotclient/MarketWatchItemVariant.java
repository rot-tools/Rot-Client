package fi.rotclient;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.Locale;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;

/** Identity from API item_bytes, independent of item rendering and client registries. */
record MarketWatchItemVariant(String itemId, String reforge, String itemUuid, int quantity, boolean known, String attributesKey) {
    MarketWatchItemVariant(String id, String reforge, String uuid, int count, boolean known) {
        this(id, reforge, uuid, count, known, "");
    }
    static final MarketWatchItemVariant UNKNOWN = new MarketWatchItemVariant("", "", "", 1, false);
    MarketWatchItemVariant {
        itemId = clean(itemId).toUpperCase(Locale.ROOT);
        reforge = clean(reforge).toLowerCase(Locale.ROOT);
        itemUuid = clean(itemUuid);
        attributesKey = clean(attributesKey);
        quantity = Math.max(1, Math.min(64, quantity));
        known = known && !itemId.isBlank();
    }
    static MarketWatchItemVariant decode(String encoded) {
        if (encoded == null || encoded.isBlank() || encoded.length() > 350_000) return UNKNOWN;
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length > 262_144) return UNKNOWN;
            var root = NbtIo.readCompressed(new ByteArrayInputStream(bytes), NbtAccounter.create(2_097_152));
            var list = root.getList("i").orElse(null);
            CompoundTag item = list == null ? root : list.size() == 1 ? list.getCompound(0).orElse(null) : null;
            if (item == null) return UNKNOWN;
            CompoundTag tag = item.getCompound("tag").orElse(item);
            var components = item.getCompound("components").orElse(null);
            if (components != null) tag = components.getCompound("minecraft:custom_data").orElse(tag);
            var extra = tag.getCompound("ExtraAttributes").orElse(null);
            if (extra == null) return UNKNOWN;
            String id = extra.getString("id").orElse("");
            // Pet type/level and book enchantments need their own valuation grouping; never merge all PETs/books.
            if (id.equals("PET") || id.equals("ENCHANTED_BOOK")) return UNKNOWN;
            int count = item.getInt("count").orElse(item.getInt("Count").orElse(1));
            if (count < 1 || count > 64) return UNKNOWN;
            return new MarketWatchItemVariant(id, extra.getString("modifier").orElse(""),
                    extra.getString("uuid").orElse(""), count, !id.isBlank(), attributesKey(extra));
        } catch (Exception malformed) { return UNKNOWN; }
    }
    /** Keep value-bearing attributes; omit only per-instance provenance. Sorted recursively for API/client parity. */
    static String attributesKey(CompoundTag extra) {
        if (extra == null || !extra.contains("id")) return "";
        var value = extra.copy();
        for (String field : java.util.List.of("uuid", "timestamp", "originTag")) value.remove(field);
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(canonical(value).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException unavailable) { return ""; }
    }
    private static String canonical(net.minecraft.nbt.Tag tag) {
        if (tag instanceof CompoundTag compound) return compound.keySet().stream().sorted()
                .map(key -> key.length() + ":" + key + "=" + canonical(compound.get(key)))
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
        if (tag instanceof net.minecraft.nbt.ListTag list) return list.stream().map(MarketWatchItemVariant::canonical)
                .collect(java.util.stream.Collectors.joining(",", "[", "]"));
        return tag == null ? "null" : tag.toString();
    }
    private static String clean(String text) { return text == null ? "" : text.trim(); }
}
