package fi.rotclient.rebuild.illegal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LegacySkyBlockTexturesTest {
    private static final Identifier SERVER_MODEL = Identifier.parse("hypixel_skyblock:hybrid_sword");

    @Test
    void mapsSkyBlockIdsToVanillaModelKeysUsedByLocalPacks() {
        var data = new CompoundTag();
        data.putString("id", "hyperion");
        assertEquals(Identifier.parse("minecraft:iron_sword"), LegacySkyBlockTextures.renderModel(SERVER_MODEL, data));

        var legacyData = new CompoundTag();
        var attributes = new CompoundTag();
        attributes.putString("id", " DIVAN-DRILL ");
        legacyData.put("ExtraAttributes", attributes);
        assertEquals(Identifier.parse("minecraft:prismarine_shard"),
                LegacySkyBlockTextures.renderModel(SERVER_MODEL, legacyData));
    }

    @Test
    void preservesCustomNamespacesAndUnmappedServerItems() {
        var data = new CompoundTag();
        data.putString("id", "HYPERION");
        var customModel = Identifier.parse("my_pack:katana");
        var vanillaModel = Identifier.parse("minecraft:diamond_sword");
        assertEquals(customModel, LegacySkyBlockTextures.renderModel(customModel, data));
        assertEquals(vanillaModel, LegacySkyBlockTextures.renderModel(vanillaModel, data));

        data.putString("id", "TARANTULA_CATALYST");
        assertEquals(SERVER_MODEL, LegacySkyBlockTextures.renderModel(SERVER_MODEL, data));
        assertEquals(SERVER_MODEL, LegacySkyBlockTextures.renderModel(SERVER_MODEL, null));
    }
}
