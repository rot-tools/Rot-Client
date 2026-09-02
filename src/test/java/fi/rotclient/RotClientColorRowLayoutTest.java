package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class RotClientColorRowLayoutTest {
    @Test
    void rightAlignedColumnsStayInsideContentAndDoNotOverlap() {
        int left = 200;
        int right = 800;
        RotClientColorRowLayout.Columns columns =
                RotClientColorRowLayout.compute(left, right, 40);
        assertTrue(RotClientColorRowLayout.pickerInsideBounds(columns, right));
        assertTrue(RotClientColorRowLayout.swatchInsideBounds(columns, right));
        assertTrue(RotClientColorRowLayout.hexBeforeSwatch(columns));
        assertTrue(RotClientColorRowLayout.swatchBeforePicker(columns));
        assertTrue(columns.labelMaxWidth() > 0);
        assertEquals(
                right - RotClientColorRowLayout.PADDING - RotClientColorRowLayout.PICKER_SIZE,
                columns.pickerX());
    }

    @Test
    void narrowViewportKeepsControlsAlignedFromTheRight() {
        int left = 10;
        int right = 220;
        RotClientColorRowLayout.Columns a =
                RotClientColorRowLayout.compute(left, right, 0);
        RotClientColorRowLayout.Columns b =
                RotClientColorRowLayout.compute(left, right, 40);
        assertEquals(a.pickerX(), b.pickerX());
        assertEquals(a.swatchX(), b.swatchX());
        assertEquals(a.hexX(), b.hexX());
        assertTrue(RotClientColorRowLayout.swatchBeforePicker(a));
        assertTrue(a.labelLeft() + a.labelMaxWidth() <= a.hexX());
    }
}
