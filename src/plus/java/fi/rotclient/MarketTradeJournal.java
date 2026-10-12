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
        String id = "", market = "", itemId = "", name = "", tier = "", reforge = "", itemUuid = "", auctionUuid = "", sellerUuid = "";
        String listingUuid = "", status = "COUNTDOWN";
        String ownerUuid = "", profile = "", server = "", attributesKey = "";
        double buyUnit, sellUnit, listingFee, soldGross;
        long quantity, reserved, createdAt, soldAt;
        boolean buySent, bought, listed, sold, listingFeeKnown;
        boolean purchaseOnly, receiptObserved, fillObserved, claimSent, baselineClear;
        boolean manualReview, manuallyReviewed;
        long baselineQuantity, confirmedAt, sourceUpdatedAt;
        boolean completedPurchase() {
            return purchaseOnly && buySent && bought && receiptObserved && confirmedAt > 0
                    && ("AH".equals(market) ? baselineClear && !itemUuid.isBlank() : "BZ".equals(market) && fillObserved && claimSent);
        }
        boolean unresolved() { return buySent && !manuallyReviewed && (manualReview || !sold && !completedPurchase()); }
        double cost() { return buyUnit * quantity; }
    }
    static final class Document {
        int schemaVersion = 2;
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
            if (loaded == null || loaded.schemaVersion < 1 || loaded.schemaVersion > 2 || loaded.settings == null
                    || loaded.settings.schemaVersion < 1 || loaded.settings.schemaVersion > 2 || loaded.positions == null
                    || loaded.positions.size() > 512 || loaded.positions.stream().anyMatch(p -> p == null || p.id == null
                    || !Double.isFinite(p.buyUnit) || !Double.isFinite(p.sellUnit) || p.buyUnit <= 0 || p.sellUnit <= 0 || p.quantity < 1 || p.quantity > 64
                    || !Double.isFinite(p.cost()) || p.reserved < p.cost()
                    || p.market == null || p.itemId == null || p.itemUuid == null || p.auctionUuid == null
                    || p.name == null || p.tier == null || p.reforge == null || p.status == null
                    || p.ownerUuid == null || p.profile == null || p.server == null || p.attributesKey == null || p.sellerUuid == null || p.listingUuid == null
                    || !("AH".equals(p.market) || "BZ".equals(p.market)) || p.baselineQuantity < 0 || p.confirmedAt < 0 || p.sourceUpdatedAt < 0
                    || p.purchaseOnly && (p.listed || p.sold || p.bought && !p.completedPurchase())
                    || p.sold && !p.listed || p.listed && !p.bought || p.bought && !p.buySent
                    || p.reserved < 0 || p.listingFee < 0 || !Double.isFinite(p.listingFee) || p.soldGross < 0 || !Double.isFinite(p.soldGross)))
                throw new IllegalStateException("Invalid trade journal");
            document = loaded;
            for (var p : document.positions) if (!p.purchaseOnly && p.buySent && !p.sold && !p.manuallyReviewed) {
                p.manualReview = true;
                p.status = "REVIEW: legacy purchase/listing; check manually before continuing";
            }
            document.schemaVersion = 2;
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
    long openPositions() { return document.positions.stream().filter(p -> p.buySent && !p.sold && !p.manuallyReviewed).count(); }
    boolean auctionAttempted(String auctionUuid) {
        String normalized = MarketTradeCandidatePolicy.uuid(auctionUuid);
        return document.positions.stream().anyMatch(p -> p.buySent && "AH".equals(p.market)
                && normalized.equals(MarketTradeCandidatePolicy.uuid(p.auctionUuid)));
    }
    boolean bazaarSnapshotAttempted(String itemId, long sourceUpdatedAt) {
        return document.positions.stream().anyMatch(p -> p.buySent && "BZ".equals(p.market)
                && p.itemId.equals(itemId) && (p.unresolved() || sourceUpdatedAt <= p.sourceUpdatedAt));
    }
    boolean markReviewed(String id) {
        var p = document.positions.stream().filter(position -> position.id.equals(id)).findFirst().orElse(null);
        if (p == null || !p.buySent || p.manuallyReviewed) return false;
        boolean review = p.manualReview;
        String previousStatus = p.status;
        p.manualReview = false; p.manuallyReviewed = true;
        p.status = p.completedPurchase() ? "BUY_DONE — reviewed manually; holding slot released"
                : "REVIEWED MANUALLY — outcome not verified";
        if (save()) return true;
        p.manualReview = review; p.manuallyReviewed = false; p.status = previousStatus; return false;
    }
    double realizedGrossProfit() {
        return document.positions.stream().filter(p -> p.sold && p.listingFeeKnown)
                .mapToDouble(p -> p.soldGross - p.cost() - p.listingFee).sum();
    }
    boolean save() {
        if (!healthy) return false;
        Path temporary = path.resolveSibling(path.getFileName() + "." + java.util.UUID.randomUUID() + ".tmp");
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            byte[] bytes = JSON.toJson(document).getBytes(StandardCharsets.UTF_8);
            try (var channel = java.nio.channels.FileChannel.open(temporary,
                    java.nio.file.StandardOpenOption.CREATE_NEW, java.nio.file.StandardOpenOption.WRITE)) {
                var buffer = java.nio.ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
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
