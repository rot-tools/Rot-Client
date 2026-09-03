package fi.rotclient;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
final class SlayerRngCatalogTest {
 @Test void catalogCoversEveryBigDropId(){for(String id:SlayerDropScalePolicy.allDrops())assertTrue(SlayerRngCatalog.byId(id).isPresent(),id);}
 @Test void resolvesHypixelDisplayAndChance(){var e=SlayerRngCatalog.byDisplay("Judgement Core").orElseThrow();assertEquals("JUDGEMENT_CORE",e.skyBlockId());assertEquals(885562,e.requiredXp());}
 @Test void resolvesEnchantedBookAndLooseChatNames(){
  assertEquals("SMITE_6", SlayerRngCatalog.resolve("Enchanted Book (Smite VI)").orElseThrow().skyBlockId());
  assertEquals("JUDGEMENT_CORE", SlayerRngCatalog.resolve("Judgement Core!").orElseThrow().skyBlockId());
 }
}
