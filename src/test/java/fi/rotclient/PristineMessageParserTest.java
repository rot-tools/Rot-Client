package fi.rotclient;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PristineMessageParserTest {
    @Test
    void parsesEverySupportedGemstone() {
        for (GemstoneType gemstone :
                GemstoneType.values()) {
            String message =
                    "PRISTINE! You found "
                            + "\uE010 "
                            + "Flawed "
                            + gemstone.displayName()
                            + " Gemstone x20!";

            PristineMessageParser.Reward reward =
                    PristineMessageParser.parse(
                            message);

            assertNotNull(
                    reward,
                    gemstone.id());

            assertEquals(
                    gemstone,
                    reward.gemstone());

            assertEquals(
                    20L,
                    reward.flawedAmount());
        }
    }

    @Test
    void parsesCommaSeparatedAmount() {
        PristineMessageParser.Reward reward =
                PristineMessageParser.parse(
                        "PRISTINE! You found "
                                + "Flawed Ruby Gemstone "
                                + "x1,234!");

        assertEquals(
                GemstoneType.RUBY,
                reward.gemstone());

        assertEquals(
                1_234L,
                reward.flawedAmount());
    }

    @Test
    void acceptsSymbolBeforeGemstoneName() {
        PristineMessageParser.Reward reward =
                PristineMessageParser.parse(
                        "PRISTINE! You found "
                                + "Flawed \u2726 Jade "
                                + "Gemstone x16!");

        assertEquals(
                GemstoneType.JADE,
                reward.gemstone());

        assertEquals(
                16L,
                reward.flawedAmount());
    }

    @Test
    void removesLegacyFormattingCodes() {
        PristineMessageParser.Reward reward =
                PristineMessageParser.parse(
                        "\u00A7rPRISTINE! "
                                + "\u00A7aYou found "
                                + "\u00A7fFlawed "
                                + "\u00A7cRuby "
                                + "\u00A7fGemstone "
                                + "\u00A7ex25!");

        assertEquals(
                GemstoneType.RUBY,
                reward.gemstone());

        assertEquals(
                25L,
                reward.flawedAmount());
    }

    @Test
    void handlesExtraWhitespace() {
        PristineMessageParser.Reward reward =
                PristineMessageParser.parse(
                        "  PRISTINE!   You   found   "
                                + "Flawed   Aquamarine "
                                + "Gemstone   x40!  ");

        assertEquals(
                GemstoneType.AQUAMARINE,
                reward.gemstone());

        assertEquals(
                40L,
                reward.flawedAmount());
    }

    @Test
    void rejectsUnknownGemstone() {
        assertNull(
                PristineMessageParser.parse(
                        "PRISTINE! You found "
                                + "Flawed Fake "
                                + "Gemstone x20!"));
    }

    @Test
    void rejectsZeroAmount() {
        assertNull(
                PristineMessageParser.parse(
                        "PRISTINE! You found "
                                + "Flawed Ruby "
                                + "Gemstone x0!"));
    }

    @Test
    void rejectsMalformedAndUnrelatedMessages() {
        assertNull(
                PristineMessageParser.parse(
                        null));

        assertNull(
                PristineMessageParser.parse(
                        ""));

        assertNull(
                PristineMessageParser.parse(
                        "[Sacks] Added items"));

        assertNull(
                PristineMessageParser.parse(
                        "PRISTINE! You found "
                                + "Flawed Ruby Gemstone!"));

        assertNull(
                PristineMessageParser.parse(
                        "You found Flawed Ruby "
                                + "Gemstone x20!"));
    }

    @Test
    void rewardRejectsInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PristineMessageParser.Reward(
                        null,
                        20L));

        assertThrows(
                IllegalArgumentException.class,
                () -> new PristineMessageParser.Reward(
                        GemstoneType.RUBY,
                        0L));
    }
}
