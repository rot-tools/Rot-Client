package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PickobulusLifecyclePolicyTest {
    @Test void unchangedCachedCountdownDoesNotPostponeReadyOrRepeatNotification() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.observeTabStatus("2s", 1_000));
        assertFalse(timer.observeTabStatus("2s", 2_000));
        assertEquals(1_000, timer.remaining(2_000));
        assertFalse(timer.observeTabStatus("2s", 3_000));
        assertTrue(timer.takeReadyNotification(3_000));
        assertTrue(timer.label(3_000).contains("READY (estimated)"));
        assertFalse(timer.observeTabStatus("2s", 10_000));
        assertFalse(timer.takeReadyNotification(10_000));
    }

    @Test void changedCountdownCorrectsDeadlineWithoutRearmingConsumedNotification() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.startUse(0, 1, "available"));
        assertTrue(timer.takeReadyNotification(1_000));
        assertTrue(timer.observeTabStatus("2s", 1_100));
        assertEquals(2_000, timer.remaining(1_100));
        assertFalse(timer.takeReadyNotification(3_100));
        assertTrue(timer.observeTabStatus("available", 3_200));
        assertFalse(timer.takeReadyNotification(3_200));
        assertTrue(timer.observeTabStatus("1s", 4_000));
        assertTrue(timer.takeReadyNotification(5_000));
        assertFalse(timer.takeReadyNotification(5_100));
    }

    @Test void startupReadyIsVisibleWithoutACompletionPopup() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.observeTabStatus("available", 1_000));
        assertEquals("Pickobulus: READY (server observed)", timer.label(1_000));
        assertFalse(timer.takeReadyNotification(1_000));
        assertTrue(timer.observeTabStatus("ready", 2_000));
        assertFalse(timer.takeReadyNotification(2_000));
    }

    @Test void readyCachedBeforeUseCannotCompleteTheNewUseWithoutCooldownEvidence() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.startUse(1_000, 60, "available"));
        assertFalse(timer.observeTabStatus("available", 1_010));
        assertFalse(timer.observeTabStatus("ready", 1_020));
        assertEquals(59_980, timer.remaining(1_020));
        assertFalse(timer.takeReadyNotification(1_020));
        assertTrue(timer.observeTabStatus("59s", 2_000));
        assertTrue(timer.observeTabStatus("available", 2_100));
        assertTrue(timer.takeReadyNotification(2_100));
        assertTrue(timer.authoritative());
    }

    @Test void namedReadyChatCanCompleteUseEvenWithoutATabCountdown() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.startUse(1_000, 60, "available"));
        timer.confirmReady(2_000);
        assertTrue(timer.takeReadyNotification(2_000));
        assertFalse(timer.takeReadyNotification(2_100));
        assertEquals("Pickobulus: READY (server observed)", timer.label(2_100));
    }

    @Test void unchangedServerCountdownLosesFreshnessAndElapsedCountdownIsAnEstimate() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.observeTabStatus("10s", 1_000));
        assertTrue(timer.label(4_000).contains("server observed"));
        assertFalse(timer.observeTabStatus("10s", 4_001));
        assertTrue(timer.label(4_001).contains("estimated"));
        assertTrue(timer.label(11_000).contains("READY (estimated)"));
        assertTrue(timer.observeTabStatus("available", 11_001));
        assertTrue(timer.label(11_001).contains("server observed"));
    }

    @Test void invalidOrOverflowingCooldownCannotPartiallyReplaceAnActiveTimer() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.start(1_000, 60, false));
        assertFalse(timer.start(2_000, Double.NaN, true));
        assertFalse(timer.start(2_000, Double.POSITIVE_INFINITY, true));
        assertFalse(timer.start(2_000, 0, true));
        assertFalse(timer.start(2_000, 601, true));
        assertFalse(timer.start(Long.MAX_VALUE - 100, 1, true));
        assertEquals(59_000, timer.remaining(2_000));
        assertTrue(timer.label(2_000).contains("estimated"));
    }

    @Test void backwardClockClearsTimerAndRequiresFreshObservation() {
        var timer = new PickobulusPolicy.Timer();
        assertTrue(timer.observeTabStatus("1s", 2_000));
        assertEquals(500, timer.remaining(2_500));
        assertEquals(-1, timer.remaining(2_499));
        assertFalse(timer.takeReadyNotification(3_000));
        assertTrue(timer.observeTabStatus("1s", 3_100));
        assertTrue(timer.takeReadyNotification(4_100));
    }

    @Test void strictLoreScopesCooldownToItsNamedAbilityIncludingIconHeaders() {
        var lore = List.of("Cooldown: 999s", "§6⦾ Ability: Pickobulus  RIGHT CLICK",
                "Throw your pickaxe.", "Cooldown: 60s",
                "§6⦾ Ability: Mining Speed Boost  RIGHT CLICK", "Cooldown: 120s");
        assertTrue(PickobulusPolicy.hasPickobulusAbility(lore));
        assertEquals(60, PickobulusPolicy.loreCooldown(lore).orElseThrow());
        assertTrue(PickobulusPolicy.loreCooldown(List.of("Ability: Pickobulus RIGHT CLICK",
                "⦾ Ability: Mining Speed Boost RIGHT CLICK", "Cooldown: 120s")).isEmpty());
        assertEquals(30, PickobulusPolicy.loreCooldown(List.of(
                "Pickaxe Ability: Pickobulus", "Cooldown: 0.5m")).orElseThrow());
    }

    @Test void descriptiveOrAmbiguousLoreCannotBorrowGenericCooldowns() {
        assertFalse(PickobulusPolicy.hasPickobulusAbility(List.of(
                "Use this with Pickobulus", "Cooldown: 60s")));
        assertTrue(PickobulusPolicy.loreCooldown(List.of(
                "Use this with Pickobulus", "Cooldown: 60s")).isEmpty());
        assertTrue(PickobulusPolicy.loreCooldown(List.of("Ability: Pickobulus RIGHT CLICK",
                "Cooldown: 60s", "Cooldown: 120s")).isEmpty());
        assertTrue(PickobulusPolicy.loreCooldown(List.of("Ability: Pickobulus RIGHT CLICK",
                "Cooldown: 60s", "Ability: Pickobulus RIGHT CLICK", "Cooldown: 60s")).isEmpty());
        assertTrue(PickobulusPolicy.seconds("Cooldown: " + "9".repeat(400) + "s").isEmpty());
    }

    @Test void tabParserRequiresExactNamedStatusAndRejectsContradictoryEvidence() {
        assertEquals("12s", PickobulusPolicy.tabStatus(List.of("Mining Speed Boost: 3s",
                "§ePickobulus: §a12s")).orElseThrow());
        assertEquals("available", PickobulusPolicy.tabStatus(List.of("Pickobulus: Available",
                "Pickobulus: Available")).orElseThrow());
        assertTrue(PickobulusPolicy.tabStatus(List.of("[Party] Name: Pickobulus: 12s")).isEmpty());
        assertTrue(PickobulusPolicy.tabStatus(List.of("Pickobulus: Available",
                "Pickobulus: 12s")).isEmpty());
        assertTrue(PickobulusPolicy.tabStatus(List.of("Pickobulus: 900s")).isEmpty());
    }

    @Test void duplicateUseCannotResetReadOnlyAcceptedCounts() {
        var shot = new PickobulusPolicy.ShotProjection();
        assertTrue(shot.start(1_000, "mithril"));
        shot.acceptedBreak(1_100, 4, true, "mithril");
        assertFalse(shot.start(1_250, "mithril"));
        assertEquals(4, shot.accepted());
        assertEquals(4, shot.selected());
        assertTrue(shot.start(1_251, "mithril"));
        assertEquals(0, shot.accepted());
    }

    @Test void acceptedProjectionUsesLongCountsAndRejectsEventsOutsideItsBoundedWindow() {
        var shot = new PickobulusPolicy.ShotProjection();
        assertTrue(shot.start(1_000, "mithril"));
        shot.acceptedBreak(999, 7, true, "mithril");
        shot.acceptedBreak(1_010, 0, true, "mithril");
        shot.acceptedBreak(1_020, -1, true, "mithril");
        shot.acceptedBreak(2_000, Integer.MAX_VALUE, true, "mithril");
        shot.acceptedBreak(3_000, Integer.MAX_VALUE, false, "mithril");
        shot.acceptedBreak(6_000, 1, true, "mithril");
        shot.acceptedBreak(6_001, 100, true, "mithril");
        assertEquals(4_294_967_295L, shot.accepted());
        assertEquals(2_147_483_648L, shot.selected());
        assertEquals(Long.MAX_VALUE, PickobulusPolicy.saturatedAdd(Long.MAX_VALUE - 1, 2));
    }

    @Test void trackerSwitchMakesOriginalTargetProjectionPartialEvenAfterSwitchingBack() {
        var shot = new PickobulusPolicy.ShotProjection();
        assertTrue(shot.start(1_000, "mithril"));
        shot.acceptedBreak(1_100, 4, true, "mithril");
        shot.observeSelection("titanium", 1_200);
        shot.acceptedBreak(1_300, 5, true, "titanium");
        shot.acceptedBreak(1_400, 6, true, "mithril");
        assertTrue(shot.selectionChanged());
        assertEquals(15, shot.accepted());
        assertEquals(4, shot.selected());
        assertTrue(shot.start(10_000, "titanium"));
        assertFalse(shot.selectionChanged());
        shot.observeSelection("gold", 15_001);
        assertFalse(shot.selectionChanged());
    }

    @Test void popupExpiresAndClockOrDeadlineFailureClearsIt() {
        var popup = new PickobulusPolicy.Popup();
        popup.show("Ability ready", 1_000);
        assertEquals("Ability ready", popup.text(3_499));
        assertEquals("", popup.text(3_500));
        popup.show("Ability ready", 4_000);
        assertEquals("", popup.text(3_999));
        popup.show("Ability ready", Long.MAX_VALUE - 1);
        assertEquals("", popup.text(Long.MAX_VALUE));
    }

    @Test void contextClearsAllTransientStateOnWorldConnectionOrSameWorldProfileChanges() {
        var context = new PickobulusPolicy.Context();
        Object world = new Object(), connection = new Object();
        assertEquals(new PickobulusPolicy.Context.Observation(true, true),
                context.observe(world, connection, "profile-a", true, true, true, 1_000));
        assertEquals(new PickobulusPolicy.Context.Observation(true, false),
                context.observe(world, connection, "profile-a", true, true, true, 1_001));
        assertTrue(context.observe(world, connection, "profile-b", true, true, true, 1_002).reset());
        assertTrue(context.observe(new Object(), connection, "profile-b", true, true, true, 1_003).reset());
        assertTrue(context.observe(world, new Object(), "profile-b", true, true, true, 1_004).reset());
    }

    @Test void disabledDisconnectedOrForeignSkyblockContextCannotAcceptAbilityMessages() {
        var context = new PickobulusPolicy.Context();
        Object world = new Object(), connection = new Object();
        assertTrue(context.observe(world, connection, "p", true, true, true, 1_000).active());
        assertEquals(new PickobulusPolicy.Context.Observation(false, true),
                context.observe(world, connection, "p", false, true, true, 1_001));
        assertTrue(context.observe(world, connection, "p", true, true, true, 1_002).reset());
        assertFalse(context.observe(world, connection, "p", true, true, false, 1_003).active());
        assertFalse(context.observe(world, connection, "p", true, false, true, 1_004).active());
        assertFalse(context.observe(world, null, "p", true, true, true, 1_005).active());
        assertFalse(context.observe(null, connection, "p", true, true, true, 1_006).active());
    }

    @Test void backwardContextClockInvalidatesTimerShotAndPopupTogether() {
        var context = new PickobulusPolicy.Context();
        Object world = new Object(), connection = new Object();
        assertTrue(context.observe(world, connection, "p", true, true, true, 2_000).reset());
        assertFalse(context.observe(world, connection, "p", true, true, true, 2_100).reset());
        assertTrue(context.observe(world, connection, "p", true, true, true, 2_099).reset());
        assertFalse(context.observe(world, connection, "p", true, true, true, 2_100).reset());
        assertFalse(context.observe(world, connection, "p", true, true, true, -1).active());
        assertTrue(context.observe(world, connection, "p", true, true, true, 3_000).reset());
    }
}
