// SPDX-License-Identifier: GPL-3.0-only
package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class AutismMovementInventoryTest {
    @Test void doubleJumpConsumesOneAirborneJumpAndOnlyLandingRearmsIt() {
        var p=new AutismAirJumpPolicy();
        p.update("DoubleJump",true,true,1000);p.jumped(true);
        assertTrue(p.mayJump());
        p.update("DoubleJump",false,true,1050);p.jumped(false);
        assertFalse(p.mayJump());
        p.update("DoubleJump",false,false,1100);assertFalse(p.mayJump());
        p.update("DoubleJump",true,false,1150);assertTrue(p.mayJump());
    }
    @Test void freeJumpsRepeatButGhostModeNeverAddsJumpImpulse() {
        var p=new AutismAirJumpPolicy();p.update("JumpFreely",false,true,1000);
        for(int i=0;i<4;i++){assertTrue(p.mayJump());p.jumped(false);}
        p.update("GhostBlock",false,true,1100);assertFalse(p.mayJump());
    }
    @Test void noGhostSupportBeforeFirstPressAndGraceIsBoundedBySpeed() {
        var p=new AutismAirJumpPolicy();p.update("GhostBlock",false,false,1000);
        assertFalse(p.ghostActive(1000,0));
        p.update("GhostBlock",false,true,1050);
        assertTrue(p.ghostActive(1350,0));assertFalse(p.ghostActive(1351,0));
        assertTrue(p.ghostActive(1950,.8));assertFalse(p.ghostActive(1951,99));
        assertEquals(300,AutismAirJumpPolicy.graceMillis(Double.NaN));
        assertEquals(900,AutismAirJumpPolicy.graceMillis(8));
    }
    @Test void worldDisableModeAndClockResetCannotRetainGhostSupport() {
        var p=new AutismAirJumpPolicy();p.update("GhostBlock",false,true,1000);
        assertFalse(p.ghostActive(999,1));
        p.update("GhostBlock",false,false,999);assertFalse(p.ghostActive(1000,1));
        p.update("GhostBlock",false,true,1100);p.reset();assertFalse(p.ghostActive(1101,1));
        p.update("GhostBlock",false,true,1200);p.update("DoubleJump",false,false,1250);
        assertFalse(p.ghostActive(1250,1));
    }
    @Test void ghostSupportOnlyClipsCrossingDownwardMotionIncludingNegativeHeights() {
        assertEquals(-.25,AutismAirJumpPolicy.clipDown(64.25,-.8));
        assertEquals(-.1,AutismAirJumpPolicy.clipDown(64.25,-.1));
        assertEquals(.42,AutismAirJumpPolicy.clipDown(64,.42));
        assertEquals(0,AutismAirJumpPolicy.clipDown(64,-.08));
        assertEquals(-.75,AutismAirJumpPolicy.clipDown(-12.25,-2));
        assertEquals(-.08,AutismAirJumpPolicy.clipDown(Double.NaN,-.08));
    }
    @Test void inventoryToolsAreOptInAndTiesKeepHotbarTools() {
        var tools=List.of(new AutismPortPolicy.Tool(2,8,false,true,20),new AutismPortPolicy.Tool(10,12,false,true,20),
                new AutismPortPolicy.Tool(35,99,false,true,2),new AutismPortPolicy.Tool(36,200,false,false,0));
        assertEquals(2,AutismPortPolicy.bestTool(tools,0,false,false,false));
        assertEquals(10,AutismPortPolicy.bestTool(tools,0,false,false,true));
        assertEquals(35,AutismPortPolicy.bestTool(tools,0,true,false,true));
        assertEquals(2,AutismPortPolicy.bestTool(List.of(tools.getFirst(),new AutismPortPolicy.Tool(10,8,false,false,0)),0,true,false,true));
    }
    @Test void inventorySilkPreferenceFallsBackToDurableFastestTool() {
        var tools=List.of(new AutismPortPolicy.Tool(2,12,false,false,0),new AutismPortPolicy.Tool(10,4,true,true,1));
        assertEquals(2,AutismPortPolicy.bestTool(tools,0,false,true,true));
        assertEquals(10,AutismPortPolicy.bestTool(tools,0,true,true,true));
    }
    @Test void swapBudgetNeverRetriesCorrectionOrASecondInventoryToolUntilMiningEnds() {
        var b=new AutismPortPolicy.ToolSwapBudget();
        assertTrue(b.take());assertFalse(b.take());b.cancel();assertFalse(b.take());
        b.reset();assertTrue(b.take());
        b.reset();b.cancel();assertFalse(b.take());assertTrue(b.cancelled());
    }
    @Test void anotherInventoryControllerCancelsSelectionAndRestoreOwnership() {
        var o=new AutismPortPolicy.ToolOwnership();assertTrue(o.begin(2));o.assigned(2);o.cancel();
        assertFalse(o.begin(2));assertEquals(-1,o.restoreSlot(2));o.reset();assertTrue(o.begin(3));
    }
    @Test void variedDelaysAreReproducibleBoundedAndPreserveFixedExistingDefaults() {
        var random=new Random(5);
        for(int base:new int[]{0,1,12,100,500,1000})for(int i=0;i<100;i++){
            int d=AutismPortPolicy.toolDelay(base,true,random);assertTrue(d>=base&&d<=1000);
            assertEquals(base,AutismPortPolicy.toolDelay(base,false,random));
        }
        assertEquals(0,AutismPortPolicy.toolDelay(-1,true,random));
        assertEquals(1000,AutismPortPolicy.toolDelay(9999,true,random));
    }
    @Test void newControlsPersistWithoutOptingExistingUsersIntoInventoryChanges() {
        var c=new QolUtilityConfig();assertFalse(c.readBoolean(AutismModuleSettings.TOOL+".inventory"));
        assertFalse(c.readBoolean(AutismModuleSettings.TOOL+".random_delay"));
        assertEquals("JumpFreely",AutismModuleSettings.text(c,AutismModuleSettings.JUMP+".mode"));
        assertTrue(AutismModuleSettings.write(c,AutismModuleSettings.JUMP+".mode","GhostBlock"));
        c.writeBoolean(AutismModuleSettings.TOOL+".inventory",true);
        var gson=new com.google.gson.Gson();var copy=gson.fromJson(gson.toJson(c),QolUtilityConfig.class);
        assertEquals("GhostBlock",AutismModuleSettings.text(copy,AutismModuleSettings.JUMP+".mode"));
        assertTrue(copy.readBoolean(AutismModuleSettings.TOOL+".inventory"));
        AutismModuleSettings.reset(copy,AutismModuleSettings.JUMP);
        assertTrue(copy.readBoolean(AutismModuleSettings.TOOL+".inventory"));
    }
}
