package fi.rotclient;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SkyMallObservationPolicyTest {
    private static MiningLeftoverPolicy.SkyMall perk(String value) {
        return new MiningLeftoverPolicy.SkyMall(value);
    }

    @Test
    void rebuildingAnUnchangedTabCannotExtendOrResurrectExpiredEvidence() {
        var state = new SkyMallObservationPolicy.State();
        state.observeTab(perk("+50 Mining Fortune"), 1_000);
        for (long now = 1_250; now < 1_201_000; now += 250)
            state.observeTab(perk("+50 Mining Fortune"), now);
        assertNotNull(state.current(1_200_999));
        assertNull(state.current(1_201_000));
        state.observeTab(perk("+50 Mining Fortune"), 1_201_250);
        assertNull(state.current(1_201_250));
        state.observeTab(perk("+100 Mining Speed"), 1_201_500);
        assertEquals("+100 Mining Speed", state.current(1_201_500).perk());
    }

    @Test
    void sameGuiDoesNotRefreshAgeButAReopenedGuiIsANewObservation() {
        var state = new SkyMallObservationPolicy.State();
        Object menu = new Object();
        state.observeGui(menu, perk("+50 Mining Fortune"), 1_000);
        state.observeGui(menu, perk("+50 Mining Fortune"), 1_200_000);
        assertNull(state.current(1_201_000));
        state.observeGui(menu, perk("+50 Mining Fortune"), 1_202_000);
        assertNull(state.current(1_202_000));
        state.observeGui(null, null, 1_202_500);
        state.observeGui(new Object(), perk("+50 Mining Fortune"), 1_203_000);
        assertNotNull(state.current(1_203_000));
    }

    @Test
    void rolloverOrDisableCannotBeUndoneByTheSamePassiveSnapshots() {
        var state = new SkyMallObservationPolicy.State();
        Object menu = new Object();
        state.observeTab(perk("+50 Mining Fortune"), 1_000);
        state.observeGui(menu, perk("+50 Mining Fortune"), 1_000);
        state.invalidate();
        state.observeTab(perk("+50 Mining Fortune"), 2_000);
        state.observeGui(menu, perk("+50 Mining Fortune"), 2_000);
        assertNull(state.current(2_000));
        // A new server buff message remains authoritative even if today's random choice repeats.
        state.observeChat(perk("+50 Mining Fortune"), 2_100);
        assertNotNull(state.current(2_100));
    }

    @Test
    void worldProfileClearAndBackwardsClockInvalidateTheOldObservation() {
        var state = new SkyMallObservationPolicy.State();
        state.observeTab(perk("+50 Mining Fortune"), 1_000);
        assertNull(state.current(999));
        state.observeTab(perk("+50 Mining Fortune"), 1_001);
        assertNull(state.current(1_001));
        state.clear();
        assertNull(state.current(2_000));
        state.observeTab(perk("+50 Mining Fortune"), 2_000);
        assertNotNull(state.current(2_000));
    }

    @Test
    void currentEffectLoreReadsIconsAndWrappedGoblinTextWithoutThePossibleList() {
        assertEquals("Gain +100⸕ Mining Speed.", MiningLeftoverPolicy.parseSkyMallLore(
                List.of("Possible buffs:", "Gain +50☘ Mining Fortune.", "Your Current Effect",
                        "■ Gain +100⸕ Mining Speed.", "")).orElseThrow().perk());
        assertEquals("10x chance to find Golden and Diamond Goblins.", MiningLeftoverPolicy.parseSkyMallLore(
                List.of("Your Current Effect", "■ 10x chance to find Golden and", "Diamond Goblins.", "")).orElseThrow().perk());
        assertTrue(MiningLeftoverPolicy.parseSkyMallLore(List.of("Possible buffs:", "■ Gain +100⸕ Mining Speed.")).isEmpty());
        assertTrue(MiningLeftoverPolicy.parseSkyMallLore(List.of("Current buff:", "Skills:", "+100 Mining Speed")).isEmpty());
        assertTrue(MiningLeftoverPolicy.parseSkyMallLore(List.of("Current buff:", "Gain +50 ☘ Fig Fortune.")).isEmpty());
    }

    @Test
    void knownSkyMallFamiliesAndLegacyCooldownFormsRemainSupported() {
        for (String value : List.of("Gain +100⸕ Mining Speed.", "Gain +50☘ Mining Fortune.",
                "Gain +15% more Powder while mining.", "-20% Pickaxe Ability cooldowns.",
                "10x chance to find Golden and Diamond Goblins.", "Gain 5x Titanium drops.",
                "+15% Powder", "-20% Pickaxe Ability Cooldown", "20% Pickaxe Ability Cooldown Reduction")) {
            assertEquals(value, MiningLeftoverPolicy.parseSkyMallChat("New buff: " + value).orElseThrow().perk());
        }
        assertTrue(MiningLeftoverPolicy.parseSkyMallChat("New buff: Gain +50 ☘ Mangrove Fortune.").isEmpty());
        assertTrue(MiningLeftoverPolicy.parseSkyMallChat("Friend: New buff: Gain +50☘ Mining Fortune.").isEmpty());
    }

    @Test
    void foreignOrIncompleteTabWidgetsCannotBecomeASkyMallPerk() {
        assertTrue(MiningLeftoverPolicy.parseSkyMall(List.of("SkyMall:", "Skills:", "+100 Mining Speed")).isEmpty());
        assertTrue(MiningLeftoverPolicy.parseSkyMall(List.of("SkyMall:", "Skills: 100%", "+100 Mining Speed")).isEmpty());
        assertTrue(MiningLeftoverPolicy.parseSkyMall(List.of("SkyMall:", "")).isEmpty());
        assertEquals("+50 Mining Fortune", MiningLeftoverPolicy.parseSkyMall(List.of(
                "Sky Mall: +50 Mining Fortune", "Pet:", "Rabbit")).orElseThrow().perk());
    }

    @Test
    void exactRolloverResetAndDisabledRowsHaveNoQuotedOrSubstringAuthority() {
        assertTrue(MiningLeftoverPolicy.skyMallInvalidatedByChat("§bNew day! §eYour Sky Mall buff changed!"));
        assertTrue(MiningLeftoverPolicy.skyMallInvalidatedByChat("Reset your Heart of the Mountain! Your Perks and Abilities have been reset."));
        assertFalse(MiningLeftoverPolicy.skyMallInvalidatedByChat("Party > Friend: New day! Your Sky Mall buff changed!"));
        assertTrue(MiningLeftoverPolicy.skyMallLoreDisabled(List.of("§cDISABLED")));
        assertFalse(MiningLeftoverPolicy.skyMallLoreDisabled(List.of("Disabled messaging can be enabled.")));
    }
}
