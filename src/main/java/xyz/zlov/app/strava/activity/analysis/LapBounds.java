package xyz.zlov.app.strava.activity.analysis;

import com.fasterxml.jackson.databind.JsonNode;

/** Lap position inside the activity: stream indexes and seconds from start. Any value may be null. */
public record LapBounds(int lapNum, Integer startIndex, Integer endIndex,
                        Integer startSec, Integer endSec, Integer elapsedTimeSec) {

    public static LapBounds of(JsonNode lap, StreamData data, int fallbackNum) {
        int num = lap.hasNonNull("lap_index") ? lap.get("lap_index").asInt() : fallbackNum;
        int s = lap.hasNonNull("start_index") ? lap.get("start_index").asInt() : -1;
        int e = lap.hasNonNull("end_index") ? lap.get("end_index").asInt() : -1;
        Integer elapsedRaw = lap.hasNonNull("elapsed_time") ? lap.get("elapsed_time").asInt() : null;

        boolean indexed = s >= 0 && e >= s;
        Integer startSec = null, endSec = null, elapsed = elapsedRaw;
        if (indexed && data != null && e < data.size()) {
            startSec = data.time(s);
            endSec = data.time(e);
            if (elapsed == null) elapsed = endSec - startSec;
        }
        return new LapBounds(num, indexed ? s : null, indexed ? e : null, startSec, endSec, elapsed);
    }
}
