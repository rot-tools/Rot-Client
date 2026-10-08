package fi.rotclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/** Durable transaction intent and observations. Unknown outcomes never become profit. */
final class MarketTradeJournal {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    static final class Position {
        String id, market, itemId, name, tier, reforge, itemUuid, auctionUuid, sellerUuid;
        String listingUuid = "", status = "COUNTDOWN";
        String ownerUuid = "", profile = "", server = "", attributesKey = "";
        double buyUnit, sellUnit, listingFee, soldGross;
        long quantity, reserved, createdAt, soldAt;
        boolean buySent, bought, listed, sold, listingFeeKnown;
        boolean unresolved() { return buySent && !sold && (!listed || "BZ".equals(market)); }
        double cost() { return buyUnit * quantity; }
    }
    static final class Document {
        int schemaVersion = 1;
        MarketTradeSettings settings = new MarketTradeSettings();
        List<Position> positions = new ArrayList<>();
    }
    private final Path path;
    private Document document = new Document();
    private boolean healthy = true;
    MarketTradeJournal(Path path) {
        this.path = path;
        if (Files.exists(path)) try {
            if (Files.size(path) > 2_097_152) throw new IllegalStateException("Oversized trade journal");
            var loaded = JSON.fromJson(Files.readString(path), Document.class);
            if (loaded == null || loaded.schemaVersion != 1 || loaded.settings == null || loaded.positions == null
                    || loaded.positions.size() > 512 || loaded.positions.stream().anyMatch(p -> p == null || p.id == null
                    || !Double.isFinite(p.buyUnit) || !Double.isFinite(p.sellUnit) || p.buyUnit <= 0 || p.sellUnit <= 0 || p.quantity < 1 || p.quantity > 64
                    || !Double.isFinite(p.cost()) || p.reserved < p.cost()
                    || p.market == null || p.itemId == null || p.itemUuid == null || p.auctionUuid == null
                    || p.name == null || p.tier == null || p.reforge == null || p.status == null
                    || p.ownerUuid == null || p.profile == null || p.server == null || p.attributesKey == null
                    || p.sold && !p.listed || p.listed && !p.bought || p.bought && !p.buySent
                    || p.reserved < 0 || p.listingFee < 0 || !Double.isFinite(p.listingFee) || p.soldGross < 0 || !Double.isFinite(p.soldGross)))
                throw new IllegalStateException("Invalid trade journal");
            document = loaded;
        } catch (Exception unreadable) { healthy = false; }
        document.settings.normalize();
    }
    boolean healthy() { return healthy; }
    MarketTradeSettings settings() { return document.settings; }
    List<Position> positions() { return List.copyOf(document.positions); }
    boolean setSettings(MarketTradeSettings settings) {
        var previous = document.settings; document.settings = settings; settings.normalize();
        if (save()) return true;
        document.settings = previous; return false;
    }
    boolean add(Position p) {
        if (!healthy || document.positions.size() >= 512 || document.positions.stream().anyMatch(old -> old.id.equals(p.id))) return false;
        document.positions.add(p);
        if (save()) return true;
        document.positions.remove(p); return false;
    }
    long committedBudget() {
        long result = 0;
        for (Position p : document.positions) if (p.buySent) {
            try { result = Math.addExact(result, p.reserved); }
            catch (ArithmeticException overflow) { return Long.MAX_VALUE; }
        }
        return result;
    }
    long openPositions() { return document.positions.stream().filter(p -> p.buySent && !p.sold).count(); }
    double realizedGrossProfit() {
        return document.positions.stream().filter(p -> p.sold && p.listingFeeKnown)
                .mapToDouble(p -> p.soldGross - p.cost() - p.listingFee).sum();
    }
    boolean save() {
        if (!healthy) return false;
        Path temporary = path.resolveSibling(path.getFileName() + "." + java.util.UUID.randomUUID() + ".tmp");
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            Files.writeString(temporary, JSON.toJson(document), StandardCharsets.UTF_8);
            try { Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (java.nio.file.AtomicMoveNotSupportedException noAtomicMove) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception failure) {
            healthy = false;
            try { Files.deleteIfExists(temporary); } catch (Exception ignored) {}
            return false;
        }
    }
}
