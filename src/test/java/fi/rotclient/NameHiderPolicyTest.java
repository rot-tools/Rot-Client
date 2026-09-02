package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class NameHiderPolicyTest {
    @Test
    void scrambleIsDeterministicAndSameLength() {
        String first = NameHiderPolicy.scramble("Henri");
        String second = NameHiderPolicy.scramble("Henri");
        assertEquals(5, first.length());
        assertEquals(first, second);
        assertFalse(first.equalsIgnoreCase("Henri"));
        assertTrue(first.chars().noneMatch(Character::isLetter));
    }

    @Test
    void customModeUsesSanitizedAlias() {
        assertEquals(
                "Shadow",
                NameHiderPolicy.displayName(
                        NameHiderPolicy.Mode.CUSTOM, "Henri", "  Shadow  "));
        assertEquals(
                NameHiderPolicy.scramble("Henri"),
                NameHiderPolicy.displayName(
                        NameHiderPolicy.Mode.CUSTOM, "Henri", "   "));
    }

    @Test
    void customNameStripsFormattingAndCapsLength() {
        String sanitized = NameHiderPolicy.sanitizeCustomName("§cVeryLongCustomDisplayNameXXXX");
        assertEquals(NameHiderPolicy.MAX_CUSTOM_LENGTH, sanitized.length());
        assertFalse(sanitized.contains("§"));
    }

    @Test
    void replacesUsernameWithWordBoundaries() {
        assertEquals(
                "#@$%: hello",
                NameHiderPolicy.replaceUsername(
                        "Henri: hello", "Henri", "#@$%"));
        assertEquals(
                "hi #@$%",
                NameHiderPolicy.replaceUsername("hi Henri", "Henri", "#@$%"));
        assertEquals(
                "Alexander built a house",
                NameHiderPolicy.replaceUsername(
                        "Alexander built a house", "Alex", "HIDDEN"));
        assertTrue(NameHiderPolicy.containsUsername("[MVP+] Henri", "Henri"));
        assertFalse(NameHiderPolicy.containsUsername("Henrik", "Henri"));
    }

    @Test
    void matchIsCaseInsensitive() {
        assertEquals(
                "x",
                NameHiderPolicy.replaceUsername("henri", "Henri", "x"));
    }
}
