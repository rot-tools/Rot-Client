package fi.rotclient;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class SlayerSpawnPolicyTest {
    @Test
    void parsesExactFormattedServerSpawnForKnownFamilies() {
        var announced = SlayerSpawnPolicy.announcement(
                " §cSLAYER BOSS! The Voidgloom Seraph IV spawned! ").orElseThrow();
        assertEquals(SlayerPolicy.SlayerType.VOIDGLOOM, announced.type());
        assertEquals(4, announced.tier());
        assertEquals(5, SlayerSpawnPolicy.announcement(
                "SLAYER BOSS! The Conjoined Brood V spawned!").orElseThrow().tier());
        assertEquals(SlayerPolicy.SlayerType.VAMPIRE, SlayerSpawnPolicy.announcement(
                "SLAYER BOSS! The Bloodfiend III spawned!").orElseThrow().type());
    }

    @Test
    void rejectsPlayerChatQuotedMessagesUnknownNamesAndImpossibleTiers() {
        for (String line : List.of(
                "Party > [MVP+] Friend: SLAYER BOSS! The Voidgloom Seraph IV spawned!",
                "Friend: SLAYER BOSS! The Voidgloom Seraph IV spawned!",
                "I saw SLAYER BOSS! The Voidgloom Seraph IV spawned!",
                "SLAYER BOSS! The Voidgloom Seraph V spawned!",
                "SLAYER BOSS! The Giant VI spawned!",
                "SLAYER BOSS! The Voidgloom Seraph IV spawned! extra")) {
            assertTrue(SlayerSpawnPolicy.announcement(line).isEmpty(), line);
        }
    }

    @Test
    void bindsOnlyOneFreshCorrectlyIdentifiedBossToLocalAnnouncement() {
        var state = armed();
        var resolution = state.resolve(List.of(candidate(40, 9_900L,
                boss("Voidgloom Seraph IV 210M❤", ""))), "LocalPlayer", 10_100L).orElseThrow();
        assertEquals(40, resolution.entityId());
        assertEquals("LocalPlayer", resolution.descriptor().owner());
        assertEquals(4, resolution.descriptor().tier());
        assertTrue(state.resolve(List.of(candidate(40, 9_900L,
                boss("Voidgloom Seraph IV 210M❤", ""))), "LocalPlayer", 10_150L).isEmpty());
    }

    @Test
    void doesNotChooseBetweenTwoFreshBossesEvenWhenOneWouldBeCloser() {
        assertTrue(armed().resolve(List.of(
                candidate(40, 9_900L, boss("Voidgloom Seraph IV 210M❤", "")),
                candidate(80, 10_000L, boss("Voidgloom Seraph IV 180M❤", ""))),
                "LocalPlayer", 10_100L).isEmpty());
    }

    @Test
    void unclassifiedFreshBodyBlocksOwnershipUntilItIsIdentifiedAsAnotherMob() {
        var state = armed();
        var expected = candidate(40, 9_900L, boss("Voidgloom Seraph IV 210M❤", ""));
        assertTrue(state.resolve(List.of(expected, candidate(80, 10_000L, null)),
                "LocalPlayer", 10_100L).isEmpty());
        var mini = SlayerPolicy.classifyTag("Voidling Devotee 750k❤", "").orElseThrow();
        assertEquals(SlayerPolicy.EntityRole.MINIBOSS, mini.role());
        assertEquals(40, state.resolve(List.of(expected, candidate(80, 10_000L, mini)),
                "LocalPlayer", 10_200L).orElseThrow().entityId());
    }

    @Test
    void explicitForeignOwnerCannotBeOverridden() {
        assertTrue(armed().resolve(List.of(candidate(40, 10_000L,
                boss("Voidgloom Seraph IV 210M❤", "Owner: Friend"))),
                "LocalPlayer", 10_100L).isEmpty());
    }

    @Test
    void knownTierAndSpeciesMustMatchAnnouncement() {
        assertTrue(armed().resolve(List.of(candidate(40, 10_000L,
                boss("Voidgloom Seraph III 50M❤", ""))),
                "LocalPlayer", 10_100L).isEmpty());
        assertTrue(armed().resolve(List.of(new SlayerSpawnPolicy.Candidate(40,
                SlayerPolicy.SlayerType.REVENANT, 10_000L,
                boss("Voidgloom Seraph IV 210M❤", ""), true)),
                "LocalPlayer", 10_100L).isEmpty());
    }

    @Test
    void acceptsLateMetadataWithinWindowButRejectsOldReusedOrFutureBodies() {
        var boss = boss("Voidgloom Seraph IV 210M❤", "");
        assertTrue(armed().resolve(List.of(candidate(40, 9_499L, boss)),
                "LocalPlayer", 10_100L).isEmpty());
        assertTrue(armed().resolve(List.of(candidate(40, 10_501L, boss)),
                "LocalPlayer", 10_600L).isEmpty());
        assertTrue(armed().resolve(List.of(candidate(40, 10_200L, boss)),
                "LocalPlayer", 10_100L).isEmpty());
        assertTrue(armed().resolve(List.of(candidate(40, 10_500L, boss)),
                "LocalPlayer", 11_500L).isPresent());
    }

    @Test
    void expiredOrResetWorldStateCannotAssignOwners() {
        var candidate = candidate(40, 10_000L, boss("Voidgloom Seraph IV 210M❤", ""));
        assertTrue(armed().resolve(List.of(candidate), "LocalPlayer", 11_501L).isEmpty());
        var state = armed();
        state.reset();
        assertTrue(state.resolve(List.of(candidate), "LocalPlayer", 10_100L).isEmpty());
        assertTrue(state.observe("SLAYER BOSS! The Voidgloom Seraph IV spawned!", 10_200L));
    }

    @Test
    void duplicateDeliveryDoesNotExtendWindowOrReopenConsumedAnnouncement() {
        var state = armed();
        assertFalse(state.observe("SLAYER BOSS! The Voidgloom Seraph IV spawned!", 11_000L));
        assertTrue(state.resolve(List.of(candidate(40, 10_000L,
                boss("Voidgloom Seraph IV 210M❤", ""))), "LocalPlayer", 11_501L).isEmpty());

        state = armed();
        assertTrue(state.resolve(List.of(candidate(40, 10_000L,
                boss("Voidgloom Seraph IV 210M❤", ""))), "LocalPlayer", 10_100L).isPresent());
        assertFalse(state.observe("SLAYER BOSS! The Voidgloom Seraph IV spawned!", 10_200L));
        assertTrue(state.resolve(List.of(candidate(80, 10_200L,
                boss("Voidgloom Seraph IV 210M❤", ""))), "LocalPlayer", 10_300L).isEmpty());
    }

    @Test
    void deadEntitiesInvalidPlayerNamesAndBackwardsClockCannotAssignOwners() {
        var descriptor = boss("Voidgloom Seraph IV 210M❤", "");
        assertTrue(armed().resolve(List.of(new SlayerSpawnPolicy.Candidate(40,
                SlayerPolicy.SlayerType.VOIDGLOOM, 10_000L, descriptor, false)),
                "LocalPlayer", 10_100L).isEmpty());
        assertTrue(armed().resolve(List.of(candidate(40, 10_000L, descriptor)),
                "Local Player", 10_100L).isEmpty());
        assertTrue(armed().resolve(List.of(candidate(40, 10_000L, descriptor)),
                "LocalPlayer", 9_999L).isEmpty());
    }

    private static SlayerSpawnPolicy.State armed() {
        var state = new SlayerSpawnPolicy.State();
        assertTrue(state.observe("SLAYER BOSS! The Voidgloom Seraph IV spawned!", 10_000L));
        return state;
    }

    private static SlayerSpawnPolicy.Candidate candidate(
            int id, long added, SlayerPolicy.EntityDescriptor descriptor) {
        return new SlayerSpawnPolicy.Candidate(id, SlayerPolicy.SlayerType.VOIDGLOOM, added, descriptor, true);
    }

    private static SlayerPolicy.EntityDescriptor boss(String name, String owner) {
        return SlayerPolicy.classifyTag(name, owner).orElseThrow();
    }
}
