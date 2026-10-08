package fi.rotclient;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import net.minecraft.nbt.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class MarketWatchVariantPolicyTest {
    private static CompoundTag extra(String uuid, int enchant) {
        var t = new CompoundTag(); t.putString("id", "TEST_SWORD"); t.putString("modifier", "spicy");
        t.putString("uuid", uuid); t.putLong("timestamp", 100);
        var ench = new CompoundTag(); ench.putInt("sharpness", enchant); t.put("enchantments", ench); return t;
    }
    private static String encoded(CompoundTag extra, boolean modern, int quantity) throws Exception {
        var item = new CompoundTag(); item.putByte("Count", (byte) quantity);
        var custom = new CompoundTag(); custom.put("ExtraAttributes", extra);
        if (modern) { var c = new CompoundTag(); c.put("minecraft:custom_data", custom); item.put("components", c); }
        else item.put("tag", custom);
        var list = new ListTag(); list.add(item); var root = new CompoundTag(); root.put("i", list);
        var out = new ByteArrayOutputStream(); NbtIo.writeCompressed(root, out); return Base64.getEncoder().encodeToString(out.toByteArray());
    }
    private static MarketWatchAuction auction(String tier, MarketWatchItemVariant v) {
        return new MarketWatchAuction("auction", "Spicy Test Sword", "weapon", tier, 10, 100, 50, 0, true, "", "seller", v);
    }
    @Test void legacyAndComponentsDecodeTheSameIdentity() throws Exception {
        var old = MarketWatchItemVariant.decode(encoded(extra("one", 5), false, 1));
        var modern = MarketWatchItemVariant.decode(encoded(extra("two", 5), true, 1));
        assertTrue(old.known()); assertEquals("TEST_SWORD", old.itemId()); assertEquals("spicy", old.reforge());
        assertEquals("one", old.itemUuid()); assertEquals(old.attributesKey(), modern.attributesKey());
    }
    @Test void valueBearingAttributesMustNotShareAReference() throws Exception {
        var first = MarketWatchItemVariant.decode(encoded(extra("one", 5), false, 1));
        var second = MarketWatchItemVariant.decode(encoded(extra("two", 7), false, 1));
        assertNotEquals(MarketWatchVariantPolicy.comparableKey(auction("EPIC", first)), MarketWatchVariantPolicy.comparableKey(auction("EPIC", second)));
        assertNotEquals(MarketWatchVariantPolicy.comparableKey(auction("EPIC", first)), MarketWatchVariantPolicy.comparableKey(auction("LEGENDARY", first)));
    }
    @Test void malformedAndAmbiguousIdentityCannotProveAFilter() throws Exception {
        assertFalse(MarketWatchItemVariant.decode("not-base64").known());
        assertFalse(MarketWatchItemVariant.decode("A".repeat(350001)).known());
        assertFalse(MarketWatchItemVariant.decode(encoded(extra("one", 5), false, 0)).known());
        var pet = extra("one", 5); pet.putString("id", "PET");
        assertFalse(MarketWatchItemVariant.decode(encoded(pet, false, 1)).known());
        var w = new MarketWatchAuctionWatch(); w.itemName = "Spicy Test Sword"; w.reforge = "spicy";
        assertFalse(MarketWatchVariantPolicy.matches(w, auction("EPIC", MarketWatchItemVariant.UNKNOWN)));
    }
    @Test void explicitRarityAndModifierAlsoMatchCanonicalBaseNames() {
        var w = new MarketWatchAuctionWatch(); w.itemId = "TEST_SWORD"; w.itemName = "Test Sword"; w.tier = "EPIC"; w.reforge = "spicy";
        var v = new MarketWatchItemVariant("TEST_SWORD", "spicy", "id", 1, true);
        assertTrue(MarketWatchVariantPolicy.matches(w, auction("EPIC", v)));
        assertFalse(MarketWatchVariantPolicy.matches(w, auction("LEGENDARY", v)));
        w.reforge = "none"; assertFalse(MarketWatchVariantPolicy.matches(w, auction("EPIC", v)));
        w.reforge = " Ancient "; assertEquals("ancient", w.copy().reforge);
    }
    @Test void compoundInsertionOrderDoesNotAffectFingerprint() {
        var a = new CompoundTag(); a.putString("id", "X"); a.putInt("upgrade", 3);
        var b = new CompoundTag(); b.putInt("upgrade", 3); b.putString("id", "X");
        assertEquals(MarketWatchItemVariant.attributesKey(a), MarketWatchItemVariant.attributesKey(b));
        b.putInt("upgrade", 4); assertNotEquals(MarketWatchItemVariant.attributesKey(a), MarketWatchItemVariant.attributesKey(b));
    }
}
