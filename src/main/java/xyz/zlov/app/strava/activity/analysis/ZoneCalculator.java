package xyz.zlov.app.strava.activity.analysis;

import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.Distribution;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.ThresholdShare;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.ZoneBucket;

import java.util.ArrayList;
import java.util.List;

/** Time-in-zone math over an index range [from, to] (inclusive) of a stream. */
public final class ZoneCalculator {

    private ZoneCalculator() {}

    /** Upper bounds must be strictly ascending. Zone i is [bounds[i-1], bounds[i]); the last zone is open-ended. */
    public static void validateBounds(String name, List<Integer> bounds) {
        for (int i = 0; i < bounds.size(); i++) {
            if (bounds.get(i) == null) throw new IllegalArgumentException(name + " must not contain null");
            if (i > 0 && bounds.get(i) <= bounds.get(i - 1)) {
                throw new IllegalArgumentException(name + " must be strictly ascending: " + bounds);
            }
        }
    }

    public static Distribution distribution(StreamData d, String key, List<Integer> bounds,
                                            int from, int to, boolean movingOnly) {
        double[] v = d.values(key);
        int zones = bounds.size() + 1;
        int[] sec = new int[zones];
        int noData = 0;
        for (int i = Math.max(from, 0); i <= to && i < d.size(); i++) {
            if (movingOnly && !d.moving(i)) continue;
            int dt = d.durationSec(i);
            if (Double.isNaN(v[i])) { noData += dt; continue; }
            int z = 0;
            while (z < bounds.size() && v[i] >= bounds.get(z)) z++;
            sec[z] += dt;
        }
        int total = 0;
        for (int s : sec) total += s;

        List<ZoneBucket> out = new ArrayList<>();
        for (int z = 0; z < zones; z++) {
            Integer lower = z == 0 ? null : bounds.get(z - 1);
            Integer upper = z == bounds.size() ? null : bounds.get(z);
            out.add(new ZoneBucket("Z" + (z + 1), lower, upper, sec[z], percent(sec[z], total)));
        }
        return new Distribution(out, total, noData);
    }

    /** Share of time with value >= threshold. */
    public static List<ThresholdShare> thresholds(StreamData d, String key, List<Integer> thresholds,
                                                  int from, int to, boolean movingOnly) {
        double[] v = d.values(key);
        List<ThresholdShare> out = new ArrayList<>();
        int total = 0;
        int[] above = new int[thresholds.size()];
        for (int i = Math.max(from, 0); i <= to && i < d.size(); i++) {
            if (movingOnly && !d.moving(i)) continue;
            if (Double.isNaN(v[i])) continue;
            int dt = d.durationSec(i);
            total += dt;
            for (int k = 0; k < above.length; k++) if (v[i] >= thresholds.get(k)) above[k] += dt;
        }
        for (int k = 0; k < above.length; k++) {
            out.add(new ThresholdShare(thresholds.get(k), above[k], percent(above[k], total)));
        }
        return out;
    }

    private static double percent(int part, int total) {
        return total == 0 ? 0.0 : Math.round(part * 1000.0 / total) / 10.0;
    }
}
