package fi.rotclient;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class QolTextInputPolicyTest {
    @Test void registryListsAndMacrosCanExceedTheShortDisplayNameLimit() {
        assertTrue(QolTextInputPolicy.accepts("list", "minecraft:chest, minecraft:lever", 'a'));
        assertTrue(QolTextInputPolicy.accepts("qol.chat_commands.rules", "a".repeat(200), 'b'));
        assertFalse(QolTextInputPolicy.accepts("qol.name_hider.custom_name", "a".repeat(24), 'b'));
    }
    @Test void supplementaryCharactersCannotOverrunTheInputBound() {
        assertTrue(QolTextInputPolicy.accepts("list", "a".repeat(2046), 0x1F600));
        assertFalse(QolTextInputPolicy.accepts("list", "a".repeat(2047), 0x1F600));
        assertFalse(QolTextInputPolicy.accepts("list", "", 0xD800));
        assertFalse(QolTextInputPolicy.accepts("list", "", '\n'));
    }
    @Test void sliderReadoutsKeepTheConfiguredFractionalStep() {
        assertEquals(2, new QolNumberSettings.Spec(.15, 1.5, .05, true).displayDecimals());
        assertEquals(1, new QolNumberSettings.Spec(0, 100, .5, true).displayDecimals());
        assertEquals(0, new QolNumberSettings.Spec(0, 1000, 25, true).displayDecimals());
    }
}
