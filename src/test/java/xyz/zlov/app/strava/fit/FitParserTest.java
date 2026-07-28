package xyz.zlov.app.strava.fit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import xyz.zlov.app.strava.activity.Activity;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void parsesPoolSwimWithDerivedPaceAndStableDistanceStream() throws Exception {
        byte[] bytes;
        try (InputStream in = getClass().getResourceAsStream("/fit/19331936841.fit")) {
            assertNotNull(in, "test swim FIT resource missing");
            bytes = in.readAllBytes();
        }

        Activity a = new FitParser().parse(bytes, 19331936841L);

        assertEquals("Swim", a.getType());
        assertEquals("Swim", a.getSportType());
        assertEquals(1150.0, a.getDistance(), 0.1);
        assertEquals(2203, a.getMovingTime());
        assertNotNull(a.getAverageSpeed(), "pool swim should derive average speed from distance/time");
        assertTrue(a.getAverageSpeed() > 0, "derived swim speed should be > 0");
        assertEquals(106.98, a.getAverageHeartrate(), 0.01);
        assertEquals(120.0, a.getMaxHeartrate(), 0.1);

        JsonNode streams = MAPPER.readTree(a.getStreamsRaw());
        JsonNode distance = streams.get("distance").get("data");
        assertEquals(333, distance.size());
        assertFalse(distance.get(distance.size() - 1).isNull(), "distance stream should carry last known distance");
        assertEquals(1150.0, distance.get(distance.size() - 1).asDouble(), 0.1);

        JsonNode laps = MAPPER.readTree(a.getLapsRaw());
        assertEquals(40, laps.size());
        assertTrue(laps.get(1).get("average_speed").asDouble() > 0, "moving swim lap should derive average speed");
        assertEquals(104.2, laps.get(9).get("average_heartrate").asDouble(), 0.1);
        assertEquals(109.0, laps.get(9).get("max_heartrate").asDouble(), 0.1);
        assertTrue(laps.get(0).get("average_speed").isNull(), "rest lap with zero distance should not derive speed");

        JsonNode activityRaw = MAPPER.readTree(a.getActivityRaw());
        assertEquals(106.98, activityRaw.get("average_heartrate").asDouble(), 0.01);
        assertEquals(120.0, activityRaw.get("max_heartrate").asDouble(), 0.1);
    }
}
