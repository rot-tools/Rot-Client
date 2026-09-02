package fi.rotclient;

/** Low-volume diagnostic instrumentation for the shared fallback scan. */
final class MiningBreakScanProfiler {
    private static final long REPORT_EVERY_FRAMES = 100L;
    private static long frames;
    private static long captureNanos;
    private static long detectorPasses;

    private MiningBreakScanProfiler() {
    }

    static void observe(MiningBreakScanFrame frame, int detectorCount) {
        if (frame == null || detectorCount <= 0) return;
        frames++;
        captureNanos += frame.captureNanos();
        detectorPasses += detectorCount;
        if (frames % REPORT_EVERY_FRAMES != 0L
                || !DiagnosticRecorder.isRecording()) {
            return;
        }
        long averageMicros = captureNanos / frames / 1_000L;
        DiagnosticRecorder.record(
                "BREAK_SCAN_PROFILE",
                "frames=" + frames
                        + " positionsPerFrame="
                        + MiningBreakScanPlan.POSITIONS_PER_FRAME
                        + " detectorPasses=" + detectorPasses
                        + " averageCaptureMicros=" + averageMicros
                        + " worldReadsPerFrame="
                        + MiningBreakScanPlan.POSITIONS_PER_FRAME);
    }
}
