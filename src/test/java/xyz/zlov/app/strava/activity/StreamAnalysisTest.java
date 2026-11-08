package xyz.zlov.app.strava.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import xyz.zlov.app.strava.activity.analysis.LapBounds;
import xyz.zlov.app.strava.activity.analysis.PowerCurve;
import xyz.zlov.app.strava.activity.analysis.StreamData;
import xyz.zlov.app.strava.activity.analysis.StopDetector;
import xyz.zlov.app.strava.activity.analysis.StreamSampler;
import xyz.zlov.app.strava.activity.dto.TrainerRideDto.Stop;
import xyz.zlov.app.strava.activity.analysis.ZoneCalculator;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.Distribution;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.ThresholdShare;

import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

import static org.junit.jupiter.api.Assertions.*;

/** Pure-math tests on synthetic 1 Hz streams: expected values are computable by hand. */
class StreamAnalysisTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static StreamData streams(int seconds, IntToDoubleFunction watts, IntToDoubleFunction hr, IntToDoubleFunction speed) {
        ObjectNode root = MAPPER.createObjectNode();
        addStream(root, "time", seconds, i -> i);
        addStream(root, "watts", seconds, watts);
        addStream(root, "heartrate", seconds, hr);
        addStream(root, "velocity_smooth", seconds, speed);
        addStream(root, "distance", seconds, i -> i * 5.0);
        addStream(root, "altitude", seconds, i -> i * 0.5); // 10 % grade (0.5 m per 5 m)
        return StreamData.parse(root.toString());
    }

    private static void addStream(ObjectNode root, String key, int n, IntToDoubleFunction f) {
        ArrayNode data = MAPPER.createArrayNode();
        for (int i = 0; i < n; i++) data.add(f.applyAsDouble(i));
        ObjectNode s = MAPPER.createObjectNode();
        s.set("data", data);
        root.set(key, s);
    }

    @Test
    void bestPowerFindsPeakWindows() {
        // 600 s at 200 W with a 60 s block at 400 W (t=300..359)
        StreamData d = streams(600, i -> (i >= 300 && i < 360) ? 400 : 200, i -> 120, i -> 5);
        Map<String, Double> best = PowerCurve.best(d);

        assertEquals(400.0, best.get("5s"), 0.001);
        assertEquals(400.0, best.get("1min"), 0.001);
        assertEquals((400 * 60 + 200 * 240) / 300.0, best.get("5min"), 0.001);
        assertEquals((400 * 60 + 200 * 540) / 600.0, best.get("10min"), 0.001);
        assertFalse(best.containsKey("20min"), "windows longer than the activity are omitted");
    }

    @Test
    void bestPowerTreatsTimerPauseAsZeroNotAsHeldValue() {
        // 300 W for 60 s, then a 5-minute hole in the timestamps, then 300 W for 60 s
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode time = MAPPER.createArrayNode();
        ArrayNode watts = MAPPER.createArrayNode();
        for (int i = 0; i < 60; i++) { time.add(i); watts.add(300); }
        for (int i = 0; i < 60; i++) { time.add(360 + i); watts.add(300); }
        root.set("time", MAPPER.createObjectNode().set("data", time));
        root.set("watts", MAPPER.createObjectNode().set("data", watts));
        Map<String, Double> best = PowerCurve.best(StreamData.parse(root.toString()));

        assertEquals(300.0, best.get("1min"), 0.001);
        // 3-min window cannot be 300 W: the pause contributes zeros
        assertTrue(best.get("3min") < 150.0, "pause must not be filled with the held value, was " + best.get("3min"));
    }

    @Test
    void zonesUseInclusiveLowerBoundAndSumToTotal() {
        // hr = 100 for 10 s, 123 for 10 s (boundary -> Z2), 140 for 10 s, 170 for 10 s
        StreamData d = streams(40, i -> 0, i -> i < 10 ? 100 : i < 20 ? 123 : i < 30 ? 140 : 170, i -> 5);
        Distribution z = ZoneCalculator.distribution(d, "heartrate", List.of(123, 138, 152, 163), 0, 39, true);

        assertEquals(5, z.zones().size());
        assertEquals(10, z.zones().get(0).seconds());
        assertEquals(10, z.zones().get(1).seconds());
        assertEquals(10, z.zones().get(2).seconds());
        assertEquals(0, z.zones().get(3).seconds());
        assertEquals(10, z.zones().get(4).seconds());
        assertEquals(40, z.totalSec());
        assertEquals(25.0, z.zones().get(0).percent(), 0.001);
        assertNull(z.zones().get(0).lower());
        assertNull(z.zones().get(4).upper());
    }

    @Test
    void movingOnlyExcludesStoppedTime() {
        // 30 s moving, 30 s stopped (speed 0), all at HR 130
        StreamData d = streams(60, i -> 0, i -> 130, i -> i < 30 ? 5 : 0);
        Distribution moving = ZoneCalculator.distribution(d, "heartrate", List.of(123, 138), 0, 59, true);
        Distribution all = ZoneCalculator.distribution(d, "heartrate", List.of(123, 138), 0, 59, false);

        assertEquals(30, moving.totalSec());
        assertEquals(60, all.totalSec());
    }

    @Test
    void thresholdsReportShareAtOrAbove() {
        StreamData d = streams(100, i -> 0, i -> i < 50 ? 140 : 150, i -> 5);
        List<ThresholdShare> t = ZoneCalculator.thresholds(d, "heartrate", List.of(143, 150, 151), 0, 99, true);

        assertEquals(50, t.get(0).seconds());
        assertEquals(50.0, t.get(0).percent(), 0.001);
        assertEquals(50, t.get(1).seconds(), "threshold is inclusive");
        assertEquals(0, t.get(2).seconds());
    }

    @Test
    void zoneBoundsMustBeStrictlyAscending() {
        assertThrows(IllegalArgumentException.class, () -> ZoneCalculator.validateBounds("hrZones", List.of(120, 120)));
        assertThrows(IllegalArgumentException.class, () -> ZoneCalculator.validateBounds("hrZones", List.of(150, 120)));
        assertDoesNotThrow(() -> ZoneCalculator.validateBounds("hrZones", List.of(120, 130)));
    }

    @Test
    void samplerCutsRangeAndReportsUnavailableStreams() {
        StreamData d = streams(100, i -> i, i -> 120, i -> 5);
        StreamSampler.Result r = StreamSampler.sample(d, List.of("time", "watts", "temp", "bogus"), null, 10, 19);

        assertEquals(10, r.from());
        assertEquals(19, r.to());
        assertEquals(10, r.streams().get("time").size());
        assertEquals(10L, r.streams().get("time").getFirst().longValue());
        assertEquals(19.0, r.streams().get("watts").getLast().doubleValue(), 0.001);
        assertEquals(List.of("temp", "bogus"), r.unavailable());
    }

    @Test
    void samplerDownsamplesByAveragingAndKeepsArraysAligned() {
        StreamData d = streams(100, i -> i, i -> 120, i -> 5);
        StreamSampler.Result r = StreamSampler.sample(d, List.of("time", "watts", "distance"), 10, null, null);

        assertEquals(10, r.streams().get("watts").size());
        assertEquals(10, r.streams().get("time").size());
        assertEquals(4.5, r.streams().get("watts").getFirst().doubleValue(), 0.001); // mean(0..9)
        assertEquals(99L, r.streams().get("time").getLast().longValue());           // last of bucket
        assertEquals(100, r.sourceCount());
    }

    @Test
    void derivedGradeAndMoving() {
        StreamData d = streams(60, i -> 0, i -> 120, i -> i < 30 ? 5 : 0);
        StreamSampler.Result r = StreamSampler.sample(d, List.of("grade", "moving"), null, null, null);

        assertEquals(10.0, r.streams().get("grade").get(30).doubleValue(), 0.01);
        assertEquals(1L, r.streams().get("moving").getFirst().longValue());
        assertEquals(0L, r.streams().get("moving").getLast().longValue());
    }

    @Test
    void lapBoundsComputeSecondsFromStreamTime() throws Exception {
        StreamData d = streams(100, i -> 0, i -> 120, i -> 5);
        var lap = MAPPER.readTree("{\"lap_index\":2,\"start_index\":40,\"end_index\":79}");
        LapBounds b = LapBounds.of(lap, d, 2);

        assertEquals(40, b.startSec());
        assertEquals(79, b.endSec());
        assertEquals(39, b.elapsedTimeSec());

        var withElapsed = MAPPER.readTree("{\"lap_index\":2,\"start_index\":40,\"end_index\":79,\"elapsed_time\":40}");
        assertEquals(40, LapBounds.of(withElapsed, d, 2).elapsedTimeSec(), "stored elapsed_time wins");

        var noIndex = MAPPER.readTree("{\"lap_index\":3}");
        assertNull(LapBounds.of(noIndex, d, 3).startIndex());
    }

    @Test
    void stopsAreReportedOnlyFromTwentySeconds() {
        // moving 0..59, stopped 60..89 (30 s), moving 90..149, stopped 150..159 (10 s, too short), moving 160..199
        StreamData d = streams(200, i -> 0, i -> 120, i -> (i >= 60 && i < 90) || (i >= 150 && i < 160) ? 0 : 5);
        List<Stop> stops = StopDetector.detect(d, StopDetector.MIN_STOP_SEC);

        assertEquals(1, stops.size());
        assertEquals(60, stops.getFirst().startSec());
        assertEquals(30, stops.getFirst().durationSec());
        assertEquals(300.0, stops.getFirst().atDistanceM(), 0.001); // distance = 5 m/s * 60 s
    }

    @Test
    void recordingGapCountsAsStop() {
        // moving 0..59, then the timer pauses for 2 minutes, then moving again
        ObjectNode root = MAPPER.createObjectNode();
        ArrayNode time = MAPPER.createArrayNode();
        ArrayNode speed = MAPPER.createArrayNode();
        for (int i = 0; i < 60; i++) { time.add(i); speed.add(5); }
        for (int i = 0; i < 30; i++) { time.add(180 + i); speed.add(5); }
        root.set("time", MAPPER.createObjectNode().set("data", time));
        root.set("velocity_smooth", MAPPER.createObjectNode().set("data", speed));
        List<Stop> stops = StopDetector.detect(StreamData.parse(root.toString()), StopDetector.MIN_STOP_SEC);

        assertEquals(1, stops.size());
        assertEquals(59, stops.getFirst().startSec());
        assertEquals(121, stops.getFirst().durationSec());
        assertNull(stops.getFirst().atDistanceM(), "no distance stream -> no distance");
    }

    @Test
    void elevationGainSumsOnlyClimbs() {
        // altitude rises 0.5 m/s: 20 s -> 10 m gain in any 21-point slice
        StreamData d = streams(100, i -> 0, i -> 120, i -> 5);
        assertEquals(10.0, d.elevationGain(10, 30), 0.001);
        assertEquals(0.0, d.elevationGain(5, 5), 0.001);
    }
}
