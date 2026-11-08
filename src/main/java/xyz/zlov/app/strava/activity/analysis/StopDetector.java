package xyz.zlov.app.strava.activity.analysis;

import xyz.zlov.app.strava.activity.dto.TrainerRideDto.Stop;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds stops: consecutive time without movement. A point is stationary when it is not "moving"
 * (see {@link StreamData#moving(int)}); a recording gap longer than {@link StreamData#MAX_GAP_SEC}
 * (timer auto-pause) counts as stationary as well.
 */
public final class StopDetector {

    /** Stops shorter than this are not reported. */
    public static final int MIN_STOP_SEC = 20;

    private StopDetector() {}

    public static List<Stop> detect(StreamData d, int minSec) {
        double[] dist = d.values("distance");
        List<Stop> stops = new ArrayList<>();
        int n = d.size();
        int runStart = -1;
        int runEndSec = 0;
        for (int i = 0; i < n; i++) {
            int rawDt = i < n - 1 ? d.time(i + 1) - d.time(i) : 1;
            boolean stationary = !d.moving(i) || rawDt > StreamData.MAX_GAP_SEC;
            if (stationary) {
                if (runStart < 0) runStart = i;
                runEndSec = d.time(i) + Math.max(rawDt, 0);
            } else if (runStart >= 0) {
                addIfLongEnough(stops, d, dist, runStart, runEndSec, minSec);
                runStart = -1;
            }
        }
        if (runStart >= 0) addIfLongEnough(stops, d, dist, runStart, runEndSec, minSec);
        return stops;
    }

    private static void addIfLongEnough(List<Stop> out, StreamData d, double[] dist, int start, int endSec, int minSec) {
        int duration = endSec - d.time(start);
        if (duration < minSec) return;
        Double at = (dist == null || Double.isNaN(dist[start])) ? null : Math.round(dist[start] * 10.0) / 10.0;
        out.add(new Stop(d.time(start), duration, at));
    }
}
