package xyz.zlov.app.strava.fit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.zlov.app.strava.activity.Activity;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FitParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void parsesRealFitFile() throws Exception {
        byte[] bytes;
        try (InputStream in = getClass().getResourceAsStream("/fit/19166201899.fit")) {
            assertNotNull(in, "test FIT resource missing");
            bytes = in.readAllBytes();
        }

        Activity a = new FitParser().parse(bytes, 19166201899L);

        System.out.println("=== Parsed Activity ===");
        System.out.printf("stravaId=%d name='%s' type=%s sportType=%s trainer=%s%n",
                a.getStravaId(), a.getName(), a.getType(), a.getSportType(), a.getTrainer());
        System.out.printf("start=%s distance=%.1f m moving=%ds elapsed=%ds ascent=%.0f m%n",
                a.getStartDate(), a.getDistance(), a.getMovingTime(), a.getElapsedTime(), a.getTotalElevationGain());
        System.out.printf("avgSpeed=%s maxSpeed=%s avgHr=%s maxHr=%s avgCad=%s avgW=%s%n",
                a.getAverageSpeed(), a.getMaxSpeed(), a.getAverageHeartrate(),
                a.getMaxHeartrate(), a.getAverageCadence(), a.getAverageWatts());
        System.out.println("polyline length=" + (a.getMapPolyline() == null ? "null" : a.getMapPolyline().length()));

        JsonNode streams = MAPPER.readTree(a.getStreamsRaw());
        System.out.println("stream keys=" + streams.fieldNames().next() + " ... " + streams.size() + " streams");
        streams.fieldNames().forEachRemaining(k ->
                System.out.printf("  %-16s size=%d%n", k, streams.get(k).get("data").size()));

        JsonNode laps = MAPPER.readTree(a.getLapsRaw());
        System.out.println("laps=" + laps.size());
        if (laps.size() > 0) {
            System.out.println("  lap[0]=" + laps.get(0));
        }
        System.out.println("activity_raw=" + a.getActivityRaw());

        // Sanity assertions
        assertEquals(19166201899L, a.getStravaId());
        assertNotNull(a.getType());
        assertNotNull(a.getStartDate());
        assertTrue(a.getDistance() > 0, "distance should be > 0");
        assertTrue(a.getElapsedTime() > 0, "elapsed should be > 0");
        assertTrue(streams.has("distance"), "must have distance stream");
        assertTrue(streams.get("distance").get("data").size() > 0, "distance stream must have data");

        // All streams must be index-aligned (same length) for lap-slicing / charts
        int size = streams.get("time").get("data").size();
        streams.fieldNames().forEachRemaining(k ->
                assertEquals(size, streams.get(k).get("data").size(), "stream " + k + " length mismatch"));
    }
}
