package fi.rotclient;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PauseMenuButtonLayoutTest {
    @Test
    void prefersFreeTopRightCorner() {
        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(480, 270, List.of());

        assertEquals(new PauseMenuButtonLayout.Position(378, 4), position);
    }

    @Test
    void movesToTopLeftWhenAnotherModUsesTopRight() {
        List<PauseMenuButtonLayout.Bounds> widgets = List.of(
                new PauseMenuButtonLayout.Bounds(378, 4, 98, 20)
        );

        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(480, 270, widgets);

        assertEquals(new PauseMenuButtonLayout.Position(4, 4), position);
    }

    @Test
    void avoidsAllFourOccupiedCorners() {
        List<PauseMenuButtonLayout.Bounds> widgets = List.of(
                new PauseMenuButtonLayout.Bounds(378, 4, 98, 20),
                new PauseMenuButtonLayout.Bounds(4, 4, 98, 20),
                new PauseMenuButtonLayout.Bounds(378, 246, 98, 20),
                new PauseMenuButtonLayout.Bounds(4, 246, 98, 20)
        );

        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(480, 270, widgets);

        assertEquals(new PauseMenuButtonLayout.Position(378, 28), position);
    }

    @Test
    void relaxesMarginsOnACompactButStillUsableScreen() {
        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(100, 24, List.of());

        assertEquals(new PauseMenuButtonLayout.Position(2, 0), position);
    }

    @Test
    void treatsAnExactFourPixelGapAsFree() {
        List<PauseMenuButtonLayout.Bounds> widgets = List.of(
                new PauseMenuButtonLayout.Bounds(276, 4, 98, 20)
        );

        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(480, 270, widgets);

        assertEquals(new PauseMenuButtonLayout.Position(378, 4), position);
    }

    @Test
    void rejectsAThreePixelGap() {
        List<PauseMenuButtonLayout.Bounds> widgets = List.of(
                new PauseMenuButtonLayout.Bounds(277, 4, 98, 20)
        );

        PauseMenuButtonLayout.Position position =
                PauseMenuButtonLayout.choose(480, 270, widgets);

        assertEquals(new PauseMenuButtonLayout.Position(4, 4), position);
    }
}
