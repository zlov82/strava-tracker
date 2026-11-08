package xyz.zlov.app.strava.activity.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;

/**
 * Parsed view over the {@code streams_raw} jsonb (Strava key_by_type format). Missing samples are NaN.
 * Points are not assumed to be 1 Hz: all durations are derived from the {@code time} stream.
 */
public final class StreamData {

    /** Speed (m/s) above which a point counts as "moving". */
    public static final double MOVING_SPEED = 0.5;
    /** Gaps longer than this between samples are timer pauses / data holes, not time spent at the sample value. */
    public static final int MAX_GAP_SEC = 10;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int[] time;
    private final Map<String, double[]> series;
    private final boolean[] moving;

    private StreamData(int[] time, Map<String, double[]> series) {
        this.time = time;
        this.series = series;
        this.moving = computeMoving();
    }

    public static StreamData parse(String streamsRaw) {
        if (streamsRaw == null || streamsRaw.isBlank()) {
            throw new IllegalArgumentException("Activity has no streams");
        }
        JsonNode root;
        try {
            root = MAPPER.readTree(streamsRaw);
        } catch (Exception e) {
            throw new IllegalArgumentException("Cannot parse streams: " + e.getMessage(), e);
        }
        JsonNode timeNode = root.path("time").path("data");
        if (!timeNode.isArray() || timeNode.isEmpty()) {
            throw new IllegalArgumentException("Activity has no time stream");
        }
        int n = timeNode.size();
        int[] time = new int[n];
        for (int i = 0; i < n; i++) time[i] = timeNode.get(i).asInt();

        Map<String, double[]> series = new HashMap<>();
        root.fields().forEachRemaining(e -> {
            if (e.getKey().equals("time")) return;
            JsonNode data = e.getValue().path("data");
            if (!data.isArray()) return;
            double[] arr = new double[n];
            for (int i = 0; i < n; i++) {
                JsonNode v = i < data.size() ? data.get(i) : null;
                arr[i] = (v == null || v.isNull() || !v.isNumber()) ? Double.NaN : v.asDouble();
            }
            series.put(e.getKey(), arr);
        });
        return new StreamData(time, series);
    }

    public int size() { return time.length; }

    public int time(int i) { return time[i]; }

    public boolean has(String key) { return series.containsKey(key); }

    /** Returns the series or null when the stream is absent. */
    public double[] values(String key) { return series.get(key); }

    public boolean moving(int i) { return moving[i]; }

    /** Seconds the value at point i is considered to hold (until the next sample, gaps capped). */
    public int durationSec(int i) {
        if (i >= time.length - 1) return 1;
        int dt = time[i + 1] - time[i];
        if (dt <= 0) return 0;
        return dt <= MAX_GAP_SEC ? dt : 1;
    }

    /** Index of the first point with time >= sec (size() if none). */
    public int firstIndexAtOrAfter(int sec) {
        int lo = 0, hi = time.length;
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (time[mid] < sec) lo = mid + 1; else hi = mid;
        }
        return lo;
    }

    /** Index of the last point with time <= sec (-1 if none). */
    public int lastIndexAtOrBefore(int sec) {
        return firstIndexAtOrAfter(sec + 1) - 1;
    }

    private boolean[] computeMoving() {
        int n = time.length;
        boolean[] m = new boolean[n];
        double[] vel = series.get("velocity_smooth");
        double[] dist = series.get("distance");
        for (int i = 0; i < n; i++) {
            if (vel != null && !Double.isNaN(vel[i])) {
                m[i] = vel[i] > MOVING_SPEED;
            } else if (dist != null && i > 0 && !Double.isNaN(dist[i]) && !Double.isNaN(dist[i - 1])) {
                m[i] = dist[i] > dist[i - 1];
            } else {
                m[i] = true;
            }
        }
        return m;
    }
}
