package xyz.zlov.app.strava.activity.analysis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Cuts a time range out of the streams, derives extra streams and downsamples. */
public final class StreamSampler {

    public static final Set<String> SUPPORTED = Set.of(
            "time", "heartrate", "watts", "cadence", "distance", "altitude", "velocity", "grade", "moving", "temp");

    private static final int GRADE_WINDOW = 5;
    /** Streams whose bucket value is the last sample (cumulative/positional); the rest are averaged. */
    private static final Set<String> LAST_VALUE = Set.of("time", "distance", "altitude");

    private StreamSampler() {}

    public record Result(int from, int to, int sourceCount, Map<String, List<Number>> streams, List<String> unavailable) {}

    /**
     * @param resolution max number of points after downsampling (equal-size buckets by index), null = no downsampling
     * @param fromSec    inclusive lower time bound, null = from start
     * @param toSec      inclusive upper time bound, null = to end
     */
    public static Result sample(StreamData d, List<String> requested, Integer resolution, Integer fromSec, Integer toSec) {
        int lo = fromSec == null ? 0 : d.firstIndexAtOrAfter(fromSec);
        int hi = toSec == null ? d.size() - 1 : d.lastIndexAtOrBefore(toSec);
        if (lo > hi || lo >= d.size()) {
            throw new IllegalArgumentException("Empty time range: fromSec=" + fromSec + ", toSec=" + toSec);
        }
        int count = hi - lo + 1;
        int buckets = (resolution == null || resolution <= 0 || resolution >= count) ? count : resolution;

        Map<String, List<Number>> out = new LinkedHashMap<>();
        List<String> unavailable = new ArrayList<>();
        for (String name : requested) {
            double[] src = source(d, name);
            if (src == null) {
                if (!unavailable.contains(name)) unavailable.add(name);
                continue;
            }
            out.put(name, bucketize(src, lo, count, buckets, name));
        }
        return new Result(d.time(lo), d.time(hi), count, out, unavailable);
    }

    private static double[] source(StreamData d, String name) {
        return switch (name) {
            case "time" -> {
                double[] t = new double[d.size()];
                for (int i = 0; i < t.length; i++) t[i] = d.time(i);
                yield t;
            }
            case "heartrate", "watts", "cadence", "distance", "altitude" -> d.values(name);
            case "velocity" -> d.values("velocity_smooth");
            case "temp" -> d.values("temp");
            case "moving" -> {
                double[] m = new double[d.size()];
                for (int i = 0; i < m.length; i++) m[i] = d.moving(i) ? 1 : 0;
                yield m;
            }
            case "grade" -> grade(d);
            default -> null;
        };
    }

    static double[] grade(StreamData d) {
        double[] alt = d.values("altitude");
        double[] dist = d.values("distance");
        if (alt == null || dist == null) return null;
        int n = d.size();
        double[] g = new double[n];
        for (int i = 0; i < n; i++) {
            int a = Math.max(0, i - GRADE_WINDOW), b = Math.min(n - 1, i + GRADE_WINDOW);
            double dd = dist[b] - dist[a];
            g[i] = (Double.isNaN(dd) || dd < 5 || Double.isNaN(alt[a]) || Double.isNaN(alt[b]))
                    ? Double.NaN : (alt[b] - alt[a]) / dd * 100.0;
        }
        return g;
    }

    private static List<Number> bucketize(double[] src, int lo, int count, int buckets, String name) {
        List<Number> out = new ArrayList<>(buckets);
        boolean last = LAST_VALUE.contains(name);
        boolean max = name.equals("moving");
        boolean integral = name.equals("time") || name.equals("moving");
        int idx = 0;
        for (int b = 0; b < buckets; b++) {
            int end = (int) ((long) (b + 1) * count / buckets); // exclusive, relative to lo
            double sum = 0, mx = Double.NaN, lastV = Double.NaN;
            int cnt = 0;
            for (; idx < end; idx++) {
                double v = src[lo + idx];
                if (Double.isNaN(v)) continue;
                sum += v; cnt++; lastV = v;
                mx = Double.isNaN(mx) ? v : Math.max(mx, v);
            }
            double value = cnt == 0 ? Double.NaN : last ? lastV : max ? mx : sum / cnt;
            if (Double.isNaN(value)) out.add(null);
            else if (integral) out.add((long) Math.round(value));
            else out.add(Math.round(value * 100.0) / 100.0);
        }
        return out;
    }
}
