// SPDX-License-Identifier: GPL-3.0-only
// AUTISM Client 5.0 settings adapted to the Rot Client+ catalog/config by Rot Tools.
package fi.rotclient;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import java.util.*;
import static fi.rotclient.QolUtilityCatalog.*;

final class AutismModuleSettings {
    static final String SAFE = "qol.autism_safe_walk", PARKOUR = "qol.autism_parkour",
            SNEAK = "qol.autism_sneak", INTERACT = "qol.autism_no_interact", TOOL = "qol.autism_auto_tool",
            SIGN = "qol.autism_auto_sign", LOOK = "qol.autism_free_look", PLACE = "qol.autism_air_place";
    static final List<String> MODULES = List.of(SAFE, PARKOUR, SNEAK, INTERACT, TOOL, SIGN, LOOK, PLACE);
    private record Field(String id, String label, String description, Object fallback,
                         QolNumberSettings.Spec range, List<String> options) {}
    private static final List<Field> FIELDS = fields();
    private static final Map<String, Field> FIELD_BY_ID = FIELDS.stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Field::id, f -> f));
    private static List<Field> fields() {
        List<Field> f = new ArrayList<>();
        number(f, SAFE, "look_down", "Look Down", 25, 0, 90, 1, "Minimum downward look in degrees.");
        number(f, SAFE, "edge_distance", "Edge Distance", .30, .15, 1.5, .05, "Probe distance in blocks.");
        number(f, SAFE, "hold_min", "Minimum Hold", 100, 0, 500, 5, "Minimum sneak hold in milliseconds.");
        number(f, SAFE, "hold_max", "Maximum Hold", 200, 0, 500, 5, "Maximum sneak hold in milliseconds.");
        choice(f, SNEAK, "mode", "Mode", "Vanilla", List.of("Vanilla", "Packet"), "Vanilla changes local input; Packet changes outgoing sneak input only.");
        for (String kind : List.of("block_mine", "block_use", "entity_use", "entity_hit")) {
            text(f, INTERACT, kind, kind.replace('_', ' ') + " IDs", "Comma-separated registry IDs, e.g. chest, minecraft:lever. Empty lists block nothing.");
            choice(f, INTERACT, kind + "_mode", "List Mode", kind.equals("entity_hit") ? "WhiteList" : "BlackList", List.of("BlackList", "WhiteList"), "Blacklist blocks listed targets; whitelist permits only listed targets.");
            if (kind.endsWith("use")) choice(f, INTERACT, kind + "_hand", "Hand", "Both", List.of("Both", "Mainhand", "Offhand", "None"), "Which hand this protection applies to.");
        }
        bool(f, INTERACT, "named", "Include Named Entities", false, "Apply entity filters to custom-named entities too.");
        bool(f, INTERACT, "babies", "Include Babies", false, "Apply entity filters to baby mobs too.");
        number(f, TOOL, "delay", "Switch Delay", 12, 0, 1000, 1, "Delay before a hotbar tool change, in milliseconds.");
        bool(f, TOOL, "ignore_durability", "Ignore Durability", true, "Otherwise skip tools with two or fewer uses remaining.");
        bool(f, TOOL, "silk", "Prefer Silk Touch", false, "Prefer a suitable Silk Touch tool, then fall back to the fastest tool.");
        bool(f, TOOL, "sneak", "Require Sneaking", false, "Switch tools only while you sneak.");
        bool(f, TOOL, "restore", "Switch Back", false, "Restore only if you have not manually changed the selected slot.");
        number(f, TOOL, "restore_delay", "Restore Delay", 100, 0, 1000, 25, "Milliseconds after mining ends. This slice scans the hotbar only.");
        for (int line = 1; line <= 4; line++) text(f, SIGN, "line_" + line, "Line " + line, "Text trimmed to the sign's pixel width.");
        bool(f, SIGN, "edit_existing", "Also When Editing", false, "Replace non-empty signs too. Market automation sign entry always takes precedence.");
        bool(f, SIGN, "auto_done", "Auto Done", true, "Submit the filled sign on the next tick using the normal editor.");
        choice(f, LOOK, "perspective", "Perspective", "Back", List.of("Back", "Front"), "Camera side while looking around independently of your player.");
        bool(f, LOOK, "no_pitch_limit", "No Pitch Limit", true, "Allow the detached view to rotate beyond straight up/down.");
        bool(f, LOOK, "hold", "Hold Key to Look", true, "When a key is bound, enable the module and hold that key. Without a bind, use the module toggle.");
        number(f, PLACE, "range", "Range", 5, 1, 6, .05, "Distance of the air-placement target in blocks. The server still decides whether placement is allowed.");
        bool(f, PLACE, "guide", "Guide", true, "Outline the predicted placement block.");
        bool(f, PLACE, "fill", "Fill", true, "Add a translucent fill to the placement guide.");
        for (String id : MODULES) text(f, id, "keybind", "Keybind", "Toggle this module. Leave unbound to use the dashboard only.");
        return List.copyOf(f);
    }
    private static void number(List<Field> f,String m,String k,String l,double d,double lo,double hi,double step,String help) { f.add(new Field(m+"."+k,l,help,d,new QolNumberSettings.Spec(lo,hi,step,true),List.of())); }
    private static void bool(List<Field> f,String m,String k,String l,boolean d,String help) { f.add(new Field(m+"."+k,l,help,d,null,List.of())); }
    private static void text(List<Field> f,String m,String k,String l,String help) { f.add(new Field(m+"."+k,l,help,"",null,List.of())); }
    private static void choice(List<Field> f,String m,String k,String l,String d,List<String> opts,String help) { f.add(new Field(m+"."+k,l,help,d,null,opts)); }
    static boolean owns(String id) { return id != null && MODULES.contains(id); }
    private static Field field(String id) { return id == null ? null : FIELD_BY_ID.get(id); }
    private static String key(String id) { return "autismPort:"+id; }
    static Boolean module(QolUtilityConfig c,String id) { return owns(id) ? PlusOpaqueSettings.bool(c,key(id),false) : null; }
    static boolean module(QolUtilityConfig c,String id,boolean value) { if (!owns(id)) return false; PlusOpaqueSettings.write(c,key(id),value); return true; }
    static Object read(QolUtilityConfig c,String id) {
        Field f=field(id); if(f==null)return null;
        JsonElement v=PlusOpaqueSettings.fields(c).get(key(id));
        if(v==null || !v.isJsonPrimitive())return f.fallback;
        try {
            JsonPrimitive p=v.getAsJsonPrimitive();
            if(f.fallback instanceof Boolean) return p.isBoolean()?p.getAsBoolean():f.fallback;
            if(f.fallback instanceof Double) { double n=p.isNumber()?p.getAsDouble():Double.NaN; return Double.isFinite(n)?f.range.clamp(n):f.fallback; }
            String s=p.isString()?p.getAsString():(String)f.fallback;
            return f.options.isEmpty()?s:f.options.contains(s)?s:f.fallback;
        } catch(RuntimeException bad) {return f.fallback;}
    }
    static boolean write(QolUtilityConfig c,String id,Object value) {
        Field f=field(id); if(f==null || value==null)return false;
        JsonPrimitive v;
        if(f.fallback instanceof Double && value instanceof Number n) {
            if(!Double.isFinite(n.doubleValue()))return false;
            v=new JsonPrimitive(f.range.snap(n.doubleValue()));
        } else if(f.fallback instanceof Boolean && value instanceof Boolean b) v=new JsonPrimitive(b);
        else if(f.fallback instanceof String && value instanceof String s) {
            if(!f.options.isEmpty()&&!f.options.contains(s))return false;
            v=new JsonPrimitive(s.substring(0,Math.min(s.length(),2048)));
        } else return false;
        PlusOpaqueSettings.fields(c).add(key(id),v);return true;
    }
    static QolNumberSettings.Spec spec(String id) {Field f=field(id);return f==null?null:f.range;}
    static boolean enabled(QolUtilityConfig c,String id){return Boolean.TRUE.equals(module(c,id));}
    static boolean bool(QolUtilityConfig c,String id){return Boolean.TRUE.equals(read(c,id));}
    static double number(QolUtilityConfig c,String id){Object v=read(c,id);return v instanceof Double n?n:0;}
    static String text(QolUtilityConfig c,String id){Object v=read(c,id);return v instanceof String s?s:"";}
    static boolean reset(QolUtilityConfig c,String id){if(!owns(id))return false;PlusOpaqueSettings.fields(c).remove(key(id));for(Field f:FIELDS)if(f.id.startsWith(id+"."))PlusOpaqueSettings.fields(c).remove(key(f.id));if(PLACE.equals(id))PlusOpaqueSettings.fields(c).remove("autismAirPlaceColor");return true;}
    static List<ModuleDef> catalog() {
        List<ModuleDef> out=new ArrayList<>();
        for(String id:MODULES){
            String name = switch (id) {
                case SAFE -> "SafeWalk";
                case PARKOUR -> "Parkour";
                case SNEAK -> "Sneak";
                case INTERACT -> "NoInteract";
                case TOOL -> "AutoTool (Hotbar)";
                case SIGN -> "AutoSign";
                case LOOK -> "FreeLook";
                case PLACE -> "AirPlace";
                default -> throw new IllegalStateException(id);
            };
            String help = switch (id) {
                case SAFE -> "Sneak at nearby ledges while looking down.";
                case PARKOUR -> "Jump at block edges while moving on foot.";
                case SNEAK -> "Hold sneak locally or in outgoing input packets.";
                case INTERACT -> "Filter mining, block use and entity use/attacks by registry ID.";
                case TOOL -> "Choose the best hotbar mining tool; inventory swaps remain deferred.";
                case SIGN -> "Fill the sign editor with four configured lines.";
                case LOOK -> "Look around in third person without rotating the player.";
                case PLACE -> "Hold Use with a block to place at the air target.";
                default -> throw new IllegalStateException(id);
            };
            List<SettingDef> settings=new ArrayList<>();
            String lastSection="";
            for(Field f:FIELDS)if(f.id.startsWith(id+".")){
                if(id.equals(INTERACT)){
                    String suffix=f.id.substring(id.length()+1),section=suffix.startsWith("block_mine")?"Mining":suffix.startsWith("block_use")?"Block Use":suffix.startsWith("entity_use")?"Entity Use":suffix.startsWith("entity_hit")?"Attacks":"Entity Filters";
                    if(!lastSection.equals(section)){settings.add(section(id+".section_"+section.toLowerCase(Locale.ROOT).replace(' ','_'),section));lastSection=section;}
                }
                SettingType type=f.id.endsWith(".keybind")?SettingType.KEYBIND:f.fallback instanceof Boolean?SettingType.TOGGLE:f.range!=null?SettingType.NUMBER:f.options.isEmpty()?SettingType.TEXT:SettingType.ENUM;
                settings.add(setting(f.id,f.label,f.description,type,f.options.toArray(String[]::new)));
            }
            if (id.equals(PLACE)) settings.add(setting(id + ".color", "Guide Color", "Placement guide color.", SettingType.COLOR));
            out.add(QolUtilityCatalog.module(id,name,help+" AUTISM Client GPL-3.0 port.",Group.UTILITIES,"Movement & Interaction",false,true,true,List.of("autism",name,"movement","interaction"),settings.toArray(SettingDef[]::new)));
        }
        return List.copyOf(out);
    }
}
