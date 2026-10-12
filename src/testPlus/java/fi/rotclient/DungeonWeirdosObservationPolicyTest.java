package fi.rotclient;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static fi.rotclient.DungeonRoomDataPolicy.*;

final class DungeonWeirdosObservationPolicyTest {
    @Test void allSixWinningClaimsResolveToSpeakingNpc() {
        for (String text : List.of("The reward is not in my chest!",
                "At least one of them is lying, and the reward is not in Bob's chest.",
                "My chest doesn't have the reward. We are all telling the truth.",
                "My chest has the reward and I'm telling the truth!",
                "The reward isn't in any of our chests.",
                "Both of them are telling the truth. Also, Bob has the reward in their chest.")) {
            assertEquals(new DungeonWeirdosObservationPolicy.Speech("Alice",true),
                    DungeonWeirdosObservationPolicy.speech("§e[NPC] Alice: §f"+text).orElseThrow());
        }
    }

    @Test void contradictoryClaimsAndPartyQuotesDoNotBecomeWinningNpc() {
        assertFalse(DungeonWeirdosObservationPolicy.speech("[NPC] Alice: The reward is not in my chest. They are both lying.")
                .orElseThrow().solution());
        assertFalse(DungeonWeirdosObservationPolicy.speech("[NPC] Alice: My chest has the reward.").orElseThrow().solution());
        assertTrue(DungeonWeirdosObservationPolicy.speech("Party > Bob: [NPC] Alice: The reward is not in my chest!").isEmpty());
        assertTrue(DungeonWeirdosObservationPolicy.speech("[NPC] Alice: Both of them are telling the truth. Also, bananas").isEmpty());
    }

    @Test void npcChestOffsetFollowsAllFourRoomRotationsInsteadOfNearestChest() {
        int[][] offsets={{1,0},{0,1},{-1,0},{0,-1}};
        for (int i=0;i<4;i++) assertEquals(new IntVec(-180+offsets[i][0],69,-180+offsets[i][1]),
                DungeonWeirdosObservationPolicy.chest(-180,-180,new Rotation(i*90,-200,-200)));
    }
}
