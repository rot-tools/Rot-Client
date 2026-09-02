package fi.rotclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class AutoClickerCpsMeterTest {
    @Test
    void reportsOnlySyntheticClicksFromTheRollingSecond() {
        AutoClickerCpsMeter meter = new AutoClickerCpsMeter();
        meter.recordLeft(1_000L);
        meter.recordLeft(1_300L);
        meter.recordRight(1_700L);

        AutoClickerCpsMeter.Snapshot live = meter.snapshot(1_999L);
        assertEquals(2, live.leftCps());
        assertEquals(1, live.rightCps());
        assertEquals(3, live.totalCps());

        AutoClickerCpsMeter.Snapshot expired = meter.snapshot(2_000L);
        assertEquals(1, expired.leftCps());
        assertEquals(1, expired.rightCps());
    }
}
