package xyz.zlov.app.strava.activity.analysis;

import java.util.LinkedHashMap;
import java.util.Map;

/** Best average power (mean-maximal power) for standard durations. */
public final class PowerCurve {

    private static final String[] LABELS = {"5s", "15s", "30s", "1min", "2min", "3min", "5min", "8min", "10min", "20min", "30min", "60min"};
    private static final int[] WINDOWS = {5, 15, 30, 60, 120, 180, 300, 480, 600, 1200, 1800, 3600};

    private PowerCurve() {}

    /**
     * Resamples power to a 1 Hz grid (missing samples and timer pauses count as 0 W, like Strava does),
     * then finds the max moving average per window. Windows longer than the activity are omitted.
     */
    public static Map<String, Double> best(StreamData d) {
        double[] watts = d.values("watts");
        Map<String, Double> result = new LinkedHashMap<>();
        if (watts == null) return result;

        double[] grid = toGrid(d, watts);
        double[] prefix = new double[grid.length + 1];
        for (int i = 0; i < grid.length; i++) prefix[i + 1] = prefix[i] + grid[i];

        for (int k = 0; k < WINDOWS.length; k++) {
            int w = WINDOWS[k];
            if (w > grid.length) break;
            double best = 0;
            for (int t = 0; t + w <= grid.length; t++) {
                best = Math.max(best, (prefix[t + w] - prefix[t]) / w);
            }
            result.put(LABELS[k], Math.round(best * 10.0) / 10.0);
        }
        return result;
    }

    static double[] toGrid(StreamData d, double[] watts) {
        int n = d.size();
        int t0 = d.time(0);
        int len = d.time(n - 1) - t0 + 1;
        double[] grid = new double[Math.max(len, 0)];
        for (int i = 0; i < n; i++) {
            double w = Double.isNaN(watts[i]) ? 0 : watts[i];
            int from = d.time(i) - t0;
            int fill = d.durationSec(i);
            for (int s = from; s < from + fill && s < grid.length; s++) grid[s] = w;
        }
        return grid;
    }
}
