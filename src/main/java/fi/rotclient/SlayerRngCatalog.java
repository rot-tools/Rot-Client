package fi.rotclient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Slayer drop XP/chance table, represented with stable SkyBlock ids. */
public final class SlayerRngCatalog {
    public record Entry(SlayerPolicy.SlayerType type, String skyBlockId, String display,
                        long requiredXp, double baseChancePercent) {}
    private static final List<Entry> ENTRIES = build();
    private static final Map<String, Entry> BY_DISPLAY = index();

    private SlayerRngCatalog() {}
    public static List<Entry> entries(){return ENTRIES;}
    public static List<Entry> entries(SlayerPolicy.SlayerType type){return ENTRIES.stream().filter(e->e.type()==type).toList();}
    public static Optional<Entry> byDisplay(String display){return Optional.ofNullable(BY_DISPLAY.get(normalize(display)));}
    public static Optional<Entry> byId(String id){String key=id==null?"":id.trim().toUpperCase(Locale.ROOT);return ENTRIES.stream().filter(e->e.skyBlockId().equals(key)).findFirst();}

    /**
     * Chat and {@code /rng} item names drift from the catalog: enchanted-book
     * wrappers, extra punctuation, and a few Hypixel aliases.
     */
    public static Optional<Entry> resolve(String raw) {
        return resolve(raw, null);
    }

    public static Optional<Entry> resolve(String raw, SlayerPolicy.SlayerType family) {
        String name = SlayerRngMeterPolicy.displayName(raw);
        Optional<Entry> exact = byDisplay(name);
        if (exact.isPresent() && familyMatches(exact.get(), family)) {
            return exact;
        }
        String key = normalize(name);
        if (key.isBlank()) {
            return Optional.empty();
        }
        key = aliasKey(key);
        Optional<Entry> id = byId(key.replace(' ', '_'));
        if (id.isPresent() && familyMatches(id.get(), family)) {
            return id;
        }
        Entry best = null;
        int bestLen = 0;
        for (Entry entry : ENTRIES) {
            if (!familyMatches(entry, family)) {
                continue;
            }
            String display = normalize(entry.display());
            if (display.length() < 4) {
                continue;
            }
            if (key.equals(display) || key.contains(display) || display.contains(key)) {
                if (display.length() > bestLen) {
                    best = entry;
                    bestLen = display.length();
                }
            }
        }
        return Optional.ofNullable(best);
    }

    private static boolean familyMatches(Entry entry, SlayerPolicy.SlayerType family) {
        return family == null || entry == null || entry.type() == family;
    }

    private static String aliasKey(String key) {
        return switch (key) {
            case "ender artifact upgrader", "ender artefact upgrade", "ender artefact upgrader"
                    -> "ender artifact upgrade";
            case "enchant rune", "enchanting rune", "enchanting rune i" -> "enchant rune i";
            case "endersnake rune", "ender snake rune i" -> "endersnake rune i";
            case "end rune" -> "end rune i";
            default -> key;
        };
    }

    private static List<Entry> build(){
        List<Entry> e=new ArrayList<>();
        var r=SlayerPolicy.SlayerType.REVENANT;
        add(e,r,"REVENANT_FLESH","Revenant flesh",0,100); add(e,r,"FOUL_FLESH","Foul flesh",3093,16.1616);
        add(e,r,"PESTILENCE_RUNE","Pestilence I",7977,6.2679); add(e,r,"UNDEAD_CATALYST","Undead catalyst",24750,2.0202);
        add(e,r,"SMITE_6","Smite VI",124370,.402); add(e,r,"BEHEADED_HORROR","Beheaded horror",310925,.1608);
        add(e,r,"REVENANT_CATALYST","Revenant catalyst",49500,1.0101); add(e,r,"SNAKE_RUNE","Snake rune I",332250,.1505);
        add(e,r,"FESTERING_MAGGOT","Festering maggot",367424,.1361); add(e,r,"REVENANT_VISCERA","Revenant viscera",3674,13.6082);
        add(e,r,"SCYTHE_BLADE","Scythe blade",489900,.1021); add(e,r,"SEVERED_HAND","Severed hand",1049785,.0476);
        add(e,r,"SHREDDED_SINEW","Shredded sinew",918562,.1021); add(e,r,"WARDEN_HEART","Warden heart",3674250,.0186);
        add(e,r,"MATCH_STICKS","Match sticks",0,0); add(e,r,"MATCHA_DYE","Matcha dye",75000000,.0008);
        var t=SlayerPolicy.SlayerType.TARANTULA;
        add(e,t,"TARANTULA_WEB","Tarantula web",0,100); add(e,t,"TOXIC_ARROW_POISON","Toxic arrow poison",3277,15.2542);
        add(e,t,"BITE_RUNE","Bite rune I",7657,6.5292); add(e,t,"DARK_QUEENS_SOUL_DROP","Darkness within rune I",74930,.6673);
        add(e,t,"SPIDER_CATALYST","Spider catalyst",24250,2.0619); add(e,t,"TARANTULA_SILK","Tarantula silk",3513,14.2318);
        add(e,t,"BANE_OF_ARTHROPODS_6","Bane of Arthropods VI",120269,.4157); add(e,t,"TARANTULA_CATALYST","Tarantula catalyst",117108,.427);
        add(e,t,"FLY_SWATTER","Fly swatter",234216,.2135); add(e,t,"VIAL_OF_VENOM","Vial of venom",351325,.1423);
        add(e,t,"TARANTULA_TALISMAN","Tarantula talisman",234216,.2135); add(e,t,"DIGESTED_MOSQUITO","Digested mosquito",702650,.0712);
        add(e,t,"SHRIVELED_WASP","Shriveled wasp",351325,.1423); add(e,t,"ENSNARED_SNAIL","Ensnared snail",1171083,.0427);
        add(e,t,"PRIMORDIAL_EYE","Primordial eye",3513250,.0142); add(e,t,"BRICK_RED_DYE","Brick red dye",75000000,.0004);
        var s=SlayerPolicy.SlayerType.SVEN;
        add(e,s,"WOLF_TOOTH","Wolf tooth",0,100); add(e,s,"HAMSTER_WHEEL","Hamster wheel",3000,16.6667);
        add(e,s,"SPIRIT_RUNE","Spirit rune I",7917,6.3154); add(e,s,"CRITICAL_6","Critical VI",61634,.8112);
        add(e,s,"FURBALL","Furball",30637,1.632); add(e,s,"RED_CLAW_EGG","Red claw egg",410900,.1217);
        add(e,s,"COUTURE_RUNE","Couture rune I",219833,.2274); add(e,s,"GRIZZLY_BAIT","Grizzly salmon",880500,.0568);
        add(e,s,"OVERFLUX_CAPACITOR","Overflux capacitor",1232700,.0406); add(e,s,"CELESTE_DYE","Celeste dye",75000000,.0002);
        var v=SlayerPolicy.SlayerType.VOIDGLOOM;
        add(e,v,"NULL_SPHERE","Null sphere",0,100); add(e,v,"TWILIGHT_ARROW_POISON","Twilight arrow poison",3300,15.1515);
        add(e,v,"ENDERSNAKE_RUNE","Endersnake rune I",9438,5.2977); add(e,v,"SUMMONING_EYE","Summoning eye",74250,.6734);
        add(e,v,"MANA_STEAL_1","Mana steal I",11183,4.4709); add(e,v,"TRANSMISSION_TUNER","Transmission tuner",22366,2.2355);
        add(e,v,"NULL_ATOM","Null atom",10120,4.9404); add(e,v,"HAZMAT_ENDERMAN","Hazmat enderman",32202,1.5527);
        add(e,v,"POCKET_ESPRESSO_MACHINE","Pocket espresso machine",128809,.3882); add(e,v,"SMARTY_PANTS_1","Smarty pants I",28338,1.7644);
        add(e,v,"END_RUNE","End rune I",75505,.6622); add(e,v,"HANDY_BLOOD_CHALICE","Handy blood chalice",283380,.1764);
        add(e,v,"SINFUL_DICE","Sinful dice",108992,.4587); add(e,v,"ENDER_ARTIFACT_UPGRADER","Ender artifact upgrade",1771125,.0282);
        add(e,v,"VOID_CONQUEROR_ENDERMAN_SKIN","Void conqueror enderman skin",302020,.1656); add(e,v,"ETHERWARP_MERGER","Etherwarp merger",118075,.4235);
        add(e,v,"JUDGEMENT_CORE","Judgement core",885562,.0565); add(e,v,"ENCHANT_RUNE","Enchant rune I",1078642,.0464);
        add(e,v,"ENDSTONE_IDOL","Endstone idol",3542250,.0141); add(e,v,"BYZANTIUM_DYE","Byzantium dye",75000000,.0002);
        var i=SlayerPolicy.SlayerType.INFERNO;
        add(e,i,"DERELICT_ASHE","Derelict ashe",0,100); add(e,i,"ENCHANTED_BLAZE_POWDER","Enchanted blaze powder",2351,21.2598);
        add(e,i,"LAVA_TEARS_RUNE","Lavatears rune I",21660,2.3084); add(e,i,"WISP_ICE_WATER","Wisp's ice water",14270,3.5039);
        add(e,i,"BUNDLE_OF_MAGMA_ARROWS","Bundle of magma arrows",4756,10.5116); add(e,i,"MANA_DISINTEGRATOR","Mana disintegrator",10192,4.9054);
        add(e,i,"SCORCHED_BOOKS","Scorched books",17837,2.8031); add(e,i,"KELVIN_INVERTER","Kelvin inverter",14270,3.5039);
        add(e,i,"BLAZE_ROD_DISTILLATE","Blaze rod distillate",0,4.6952); add(e,i,"GLOWSTONE_DUST_DISTILLATE","Glowstone dust distillate",0,4.6952);
        add(e,i,"MAGMA_CREAM_DISTILLATE","Magma cream distillate",0,4.6952); add(e,i,"NETHER_STALK_DISTILLATE","Nether stalk distillate",0,4.6952);
        add(e,i,"GABAGOOL_DISTILLATE","Gabagool distillate",10649,4.6952); add(e,i,"SCORCHED_POWER_CRYSTAL","Scorched power crystal",12558,3.9814);
        add(e,i,"ARCHFIEND_DICE","Archfiend dice",37675,1.3271); add(e,i,"FIRE_ASPECT_3","Fire aspect III",32508,1.5381);
        add(e,i,"FIERY_BURST_RUNE","Fiery burst rune I",243675,.2052); add(e,i,"FLAWED_OPAL_GEM","Flawed opal gemstone",14776,3.3838);
        add(e,i,"DUPLEX_1","Duplex I",23220,2.1533); add(e,i,"HIGH_CLASS_ARCHFIEND_DICE","High class archfiend dice",194939,.2565);
        add(e,i,"WILSON_ENGINEERING_PLANS","Wilson's engineering plans",478058,.1046); add(e,i,"SUBZERO_INVERTER","Subzero inverter",478058,.1046);
        add(e,i,"FLAME_DYE","Flame dye",75000000,.0002);
        var b=SlayerPolicy.SlayerType.VAMPIRE;
        add(e,b,"COVEN_SEAL","Coven seal",0,100); add(e,b,"BUNDLE_OF_QUANTUM","Bundle of quantum book",1687,13.3333);
        add(e,b,"SOULTWIST_RUNE","Soultwist rune",1912,11.7647); add(e,b,"BUBBA_BLISTER","Bubba blister",2250,10);
        add(e,b,"CHOCOLATE_CHIP","Chocolate chip",2250,10); add(e,b,"GUARDIAN_LUCKY_BLOCK","Guardian lucky block",3600,6.25);
        add(e,b,"MCGRUBBER_BURGER","McGrubber burger",18450,1.2195); add(e,b,"UNFANGED_VAMPIRE_PART","Unfanged vampire part",18450,1.2195);
        add(e,b,"BUNDLE_OF_THE_ONE","Bundle of The One book",12525,1.7964); add(e,b,"SANGRIA_DYE","Sangria dye",1687,.01);
        return List.copyOf(e);
    }
    private static void add(List<Entry> out,SlayerPolicy.SlayerType type,String id,String display,long xp,double chance){out.add(new Entry(type,id,display,xp,chance));}
    private static Map<String,Entry> index(){Map<String,Entry> map=new LinkedHashMap<>();for(Entry e:ENTRIES)map.put(normalize(e.display()),e);return Map.copyOf(map);}
    private static String normalize(String value){return value==null?"":value.replaceAll("§.","").trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");}
}
