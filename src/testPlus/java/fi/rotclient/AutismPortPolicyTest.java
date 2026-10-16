// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutismPortPolicyTest {
    @Test void emptyWhitelistCannotDisableAllInteractions(){
        assertFalse(AutismPortPolicy.blocks("minecraft:chest",List.of(),"WhiteList"));
        assertFalse(AutismPortPolicy.blocks("minecraft:chest",List.of(),"BlackList"));
    }
    @Test void registryModesRespectNamespacedAndUnqualifiedIds(){
        var ids=AutismPortPolicy.registryEntries(" Chest, minecraft:lever ; chest \n anvil");
        assertEquals(List.of("chest","minecraft:lever","anvil"),ids);
        assertTrue(AutismPortPolicy.blocks("minecraft:chest",ids,"BlackList"));
        assertFalse(AutismPortPolicy.blocks("minecraft:chest",ids,"WhiteList"));
        assertTrue(AutismPortPolicy.blocks("minecraft:stone",ids,"WhiteList"));
        assertFalse(AutismPortPolicy.blocks("minecraft:stone",ids,"BlackList"));
        assertFalse(AutismPortPolicy.blocks("minecraft:trapped_chest",List.of("chest"),"BlackList"));
    }
    @Test void handsAreFilteredIndependently(){
        assertTrue(AutismPortPolicy.handMatches("Offhand",true));
        assertFalse(AutismPortPolicy.handMatches("Offhand",false));
        assertFalse(AutismPortPolicy.handMatches("None",true));
        assertTrue(AutismPortPolicy.handMatches("Both",false));
    }
    @Test void sneakHoldsThroughTwoClearTicksAndReleasesAfterThird(){
        var s=new AutismPortPolicy.SneakHold();
        assertTrue(s.update(true,true,1000,100));
        assertTrue(s.update(true,false,1050,0));
        assertTrue(s.update(true,false,1101,0));
        assertFalse(s.update(true,false,1102,0));
    }
    @Test void disablingOrWorldResetCannotLeaveSyntheticSneakLatched(){
        var s=new AutismPortPolicy.SneakHold();
        assertTrue(s.update(true,true,1000,500));
        assertFalse(s.update(false,false,1001,0));
        assertFalse(s.update(true,false,1002,0));
        assertTrue(s.update(true,true,1003,500));s.reset();
        assertFalse(s.update(true,false,1004,0));
    }
    @Test void backwardClockDropsPendingHold(){
        var s=new AutismPortPolicy.SneakHold();s.update(true,true,1000,500);
        assertFalse(s.update(true,true,999,500));
    }
    @Test void toolChoiceProtectsDurabilityAndUsesNearestTie(){
        var tools=List.of(new AutismPortPolicy.Tool(0,10,false,true,2),
                new AutismPortPolicy.Tool(2,8,false,true,100),new AutismPortPolicy.Tool(7,8,false,true,100));
        assertEquals(7,AutismPortPolicy.bestTool(tools,6,false,false));
        assertEquals(0,AutismPortPolicy.bestTool(tools,6,true,false));
    }
    @Test void silkPreferenceFallsBackAndNeverSelectsInventoryOrInvalidSpeed(){
        var tools=List.of(new AutismPortPolicy.Tool(1,12,false,false,0),new AutismPortPolicy.Tool(4,4,true,false,0),
                new AutismPortPolicy.Tool(10,100,true,false,0),new AutismPortPolicy.Tool(0,Float.NaN,true,false,0));
        assertEquals(4,AutismPortPolicy.bestTool(tools,0,true,true));
        assertEquals(1,AutismPortPolicy.bestTool(tools,0,true,false));
        assertEquals(1,AutismPortPolicy.bestTool(tools.subList(0,1),0,true,true));
        assertEquals(-1,AutismPortPolicy.bestTool(List.of(),0,true,true));
    }
    @Test void everyNewModuleDefaultsOffAndSettingsPersistInOpaquePlusSlot(){
        var c=new QolUtilityConfig();
        for(String id:AutismModuleSettings.MODULES){assertFalse(c.isModuleEnabled(id),id);c.setModuleEnabled(id,true);assertTrue(c.isModuleEnabled(id),id);}
        assertTrue(c.writeNumber(AutismModuleSettings.SAFE+".edge_distance",.75));
        c.writeBoolean(AutismModuleSettings.TOOL+".restore",true);
        assertTrue(c.writeText(AutismModuleSettings.INTERACT+".block_use","chest, lever"));
        var gson=new com.google.gson.Gson();var restored=gson.fromJson(gson.toJson(c),QolUtilityConfig.class);
        assertEquals(.75,restored.readNumber(AutismModuleSettings.SAFE+".edge_distance"),1e-9);
        assertEquals(true,restored.readBoolean(AutismModuleSettings.TOOL+".restore"));
        assertEquals("chest, lever",restored.readText(AutismModuleSettings.INTERACT+".block_use"));
        assertTrue(AutismModuleSettings.reset(restored,AutismModuleSettings.SAFE));
        assertFalse(restored.isModuleEnabled(AutismModuleSettings.SAFE));
        assertTrue(restored.isModuleEnabled(AutismModuleSettings.TOOL));
        assertFalse(restored.writeNumber(AutismModuleSettings.SAFE+".edge_distance",Double.NaN));
    }
    @Test void manualSelectionCancelsToolSwitchAndRestoreUntilMiningRelease() {
        var ownership = new AutismPortPolicy.ToolOwnership();
        assertTrue(ownership.begin(0));
        ownership.assigned(3);
        assertEquals(0, ownership.restoreSlot(3));
        ownership.observe(4);
        assertFalse(ownership.begin(4));
        assertFalse(ownership.begin(3)); // Returning to the old tool does not reclaim ownership.
        assertEquals(-1, ownership.restoreSlot(3));
        ownership.reset();
        assertTrue(ownership.begin(4));
        ownership.assigned(6);
        assertEquals(4, ownership.restoreSlot(6));
    }
    @Test void manualSelectionDuringSwitchDelayAlsoCancelsThePendingToolChange() {
        var ownership = new AutismPortPolicy.ToolOwnership();
        assertTrue(ownership.begin(2));
        assertFalse(ownership.begin(5));
        assertEquals(-1, ownership.restoreSlot(5));
    }
    @Test void importedModuleHotkeysPersistAndResetIndependently() {
        var config = new QolUtilityConfig();
        for (String id : AutismModuleSettings.MODULES) {
            assertEquals("", config.readKeybind(id + ".keybind"));
            assertTrue(config.writeKeybind(id + ".keybind", "F8"));
            assertEquals("F8", config.readKeybind(id + ".keybind"));
        }
        AutismModuleSettings.reset(config, AutismModuleSettings.SAFE);
        assertEquals("", config.readKeybind(AutismModuleSettings.SAFE + ".keybind"));
        assertEquals("F8", config.readKeybind(AutismModuleSettings.TOOL + ".keybind"));
    }
    @Test void autoSignPreservesExistingTextUnlessExplicitlyEnabled() {
        assertTrue(AutismPortPolicy.mayFillSign(new String[]{"", "", "", ""}, false));
        assertFalse(AutismPortPolicy.mayFillSign(new String[]{"", "shop", "", ""}, false));
        assertTrue(AutismPortPolicy.mayFillSign(new String[]{"", "shop", "", ""}, true));
        assertFalse(AutismPortPolicy.mayFillSign(null, true));
    }
    @Test void freeLookRespectsPitchLimitsAndIgnoresNonFiniteMouseInput() {
        var limited = AutismPortPolicy.look(0, 80, 0, 200, true);
        assertEquals(90, limited.pitch());
        var free = AutismPortPolicy.look(170, 80, 200, 200, false);
        assertEquals(-160, free.yaw());
        assertEquals(110, free.pitch());
        assertEquals(new AutismPortPolicy.Look(1, 2), AutismPortPolicy.look(1, 2, Double.NaN, 0, true));
    }
    @Test void airPlaceColorAndRangePersistAndResetWithoutChangingOtherModules() {
        var config = new QolUtilityConfig();
        assertTrue(config.writeColor(AutismModuleSettings.PLACE + ".color", 0xAA123456));
        assertEquals(0xAA123456, config.readColor(AutismModuleSettings.PLACE + ".color"));
        assertTrue(config.writeNumber(AutismModuleSettings.PLACE + ".range", 999));
        assertEquals(6, config.readNumber(AutismModuleSettings.PLACE + ".range"));
        config.writeText(AutismModuleSettings.SIGN + ".line_1", "This is a long sign line to be trimmed by its pixel width.");
        AutismModuleSettings.reset(config, AutismModuleSettings.PLACE);
        assertEquals(0xFFFF3B3B, config.readColor(AutismModuleSettings.PLACE + ".color"));
        assertEquals(5, config.readNumber(AutismModuleSettings.PLACE + ".range"));
        assertTrue(config.readText(AutismModuleSettings.SIGN + ".line_1").startsWith("This is"));
    }
}
