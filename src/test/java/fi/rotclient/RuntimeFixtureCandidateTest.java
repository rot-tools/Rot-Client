package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RuntimeFixtureCandidateTest {
    @Test
    void parsesSanitizedSummaryIntoReviewableCandidate() {
        String summary = """
                Rot Client tracking unique-item summary
                schema=rotclient-tracking-runtime-v2
                target=GOLD
                area=DWARVEN_MINES
                unique_items=2

                ITEM #1
                DISPLAY NAME=Cobblestone
                RESOLVED ID=COBBLESTONE
                TOTAL OBSERVED QUANTITY=115
                OTHERS OBSERVATIONS=1
                CATALOG MATCH=YES
                NOTES=NONE

                ITEM #2
                DISPLAY NAME=Weird Ore
                RESOLVED ID=WEIRD_ORE
                TOTAL OBSERVED QUANTITY=3
                OTHERS OBSERVATIONS=0
                CATALOG MATCH=NO
                NOTES=unknown_or_unverified_catalog_identity
                """;
        RuntimeFixtureCandidate.Candidate candidate =
                RuntimeFixtureCandidate.fromSummaryBody(summary);
        assertEquals("GOLD", candidate.targetId());
        assertEquals("DWARVEN_MINES", candidate.areaId());
        assertEquals(2, candidate.items().size());
        assertEquals("COBBLESTONE", candidate.items().get(0).resolvedId());
        assertEquals("OTHER_MINED", candidate.items().get(0).classification());
        assertTrue(candidate.unknownIds().contains("WEIRD_ORE"));
        String review = RuntimeFixtureCandidate.renderReviewText(candidate);
        assertTrue(review.contains("human_research_verification_required"));
        assertFalse(review.contains("auto_promote_complete"));
        assertTrue(review.contains("do_not_auto_promote_to_catalog"));
    }
}
