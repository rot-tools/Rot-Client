package fi.rotclient;

final class RateGraphMath {
    private RateGraphMath() {
    }

    static double[] smooth(double[] source) {
        if (source == null || source.length == 0) {
            return new double[0];
        }
        double[] current = source.clone();
        for (int i = 0; i < current.length; i++) {
            current[i] = sanitizeSample(current[i]);
        }
        if (current.length < 3) {
            return current;
        }

        for (int pass = 0; pass < 2; pass++) {
            double[] next = new double[current.length];
            for (int i = 0; i < current.length; i++) {
                double previous = current[Math.max(0, i - 1)];
                double value = current[i];
                double following = current[Math.min(current.length - 1, i + 1)];
                next[i] = sanitizeSample((previous + value * 2.0 + following) / 4.0);
            }
            current = next;
        }
        return current;
    }

    static double sampleCatmullRom(double[] values, double position) {
        if (values == null || values.length == 0) {
            return 0;
        }
        if (!Double.isFinite(position)) {
            position = 0;
        }
        int first = Math.min(values.length - 1, Math.max(0, (int) Math.floor(position)));
        int second = Math.min(values.length - 1, first + 1);
        int before = Math.max(0, first - 1);
        int after = Math.min(values.length - 1, second + 1);
        double t = Math.max(0, Math.min(1, position - first));
        double t2 = t * t;
        double t3 = t2 * t;
        double value = 0.5 * (
                2.0 * values[first]
                        + (-values[before] + values[second]) * t
                        + (2.0 * values[before] - 5.0 * values[first]
                        + 4.0 * values[second] - values[after]) * t2
                        + (-values[before] + 3.0 * values[first]
                        - 3.0 * values[second] + values[after]) * t3);
        value = sanitizeSample(value);
        // Clamp to the segment range so curves never imply false overshoot.
        double lo = Math.min(values[first], values[second]);
        double hi = Math.max(values[first], values[second]);
        if (value < lo) {
            return lo;
        }
        if (value > hi) {
            return hi;
        }
        return value;
    }

    static double sanitizeSample(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return 0.0;
        }
        if (value > 1.0e12) {
            return 1.0e12;
        }
        return value;
    }
}
