package xyz.zlov.app.strava.activity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import xyz.zlov.app.strava.activity.dto.RideStreamsDto;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto;
import xyz.zlov.app.strava.activity.dto.TrainerRideDto;
import xyz.zlov.app.strava.fit.FitParser;
import xyz.zlov.app.strava.mcp.dto.RecentActivityDto;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end on real FIT files: trainer ride with power (20801346962) and outdoor ride without power (19166201899). */
class TrainerFitIntegrationTest {

    private static final long TRAINER_ID = 20801346962L;
    private static final long OUTDOOR_ID = 19166201899L;

    private static Activity trainer;
    private static Activity outdoor;
    private static ActivityService activityService;
    private static StreamAnalysisService analysis;

    @BeforeAll
    static void load() throws Exception {
        trainer = parse("/fit/20801346962.fit", TRAINER_ID);
        outdoor = parse("/fit/19166201899.fit", OUTDOOR_ID);
        activityService = new ActivityService(null, null) {
            @Override
            public Optional<Activity> fetchAndCacheDetails(Long id) {
                return Optional.ofNullable(id == TRAINER_ID ? trainer : id == OUTDOOR_ID ? outdoor : null);
            }

            @Override
            public Optional<Activity> fetchAndCacheStreams(Long id) {
                return fetchAndCacheDetails(id);
            }
        };
        analysis = new StreamAnalysisService(activityService);
    }

    private static Activity parse(String path, long id) throws Exception {
        try (InputStream in = TrainerFitIntegrationTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing fixture " + path);
            return new FitParser().parse(in.readAllBytes(), id);
        }
    }

    @Test
    void trainerRideHasLapBoundariesAndBestPower() {
        assertTrue(trainer.getTrainer(), "VIRTUAL_ACTIVITY must be flagged as trainer");
        TrainerRideDto dto = activityService.buildTrainerRideDto(trainer);

        assertEquals(15, dto.laps().size());
        TrainerRideDto.LapDto l1 = dto.laps().get(0);
        TrainerRideDto.LapDto l2 = dto.laps().get(1);
        assertEquals(0, l1.startSec());
        assertEquals(600, l1.elapsedTimeSec());
        assertEquals(600, l2.startSec(), "lap 2 starts where lap 1 ends");
        assertEquals(300, l2.elapsedTimeSec());
        assertEquals(l1.endSec(), l2.startSec());
        assertNotNull(l2.startIndex());
        assertTrue(l2.endIndex() > l2.startIndex());

        assertTrue(dto.hasDeviceWatts());
        assertNotNull(dto.watts());
        assertNotNull(dto.laps().get(1).watts());
        assertNotNull(dto.bestPower());
        double p5s = dto.bestPower().get("5s");
        double p1min = dto.bestPower().get("1min");
        double p5min = dto.bestPower().get("5min");
        assertTrue(p5s <= 311.0 && p5s >= p1min && p1min >= p5min, "curve must be non-increasing and bounded by max power");
        assertTrue(p5min >= 170, "5-min work blocks are ~178 W avg, best 5 min was " + p5min);
        assertTrue(dto.bestPower().containsKey("60min"), "ride is 77 min long");
    }

    @Test
    void outdoorRideWithoutPowerHasNoBestPowerButHasLapBoundaries() {
        TrainerRideDto dto = activityService.buildTrainerRideDto(outdoor);

        assertNull(dto.bestPower());
        assertFalse(dto.laps().isEmpty());
        assertNotNull(dto.laps().getFirst().startSec());
    }

    @Test
    void streamsToolReturnsAlignedArraysAndCapsPoints() {
        RideStreamsDto full = analysis.getStreams(TRAINER_ID, List.of("time", "heartrate", "watts", "velocity", "grade", "moving", "temp"),
                null, null, null);
        assertEquals(2000, full.count(), "default cap");
        assertTrue(full.downsampled());
        assertEquals(List.of("temp"), full.unavailable());
        full.streams().values().forEach(a -> assertEquals(2000, a.size()));

        RideStreamsDto lap = analysis.getStreams(TRAINER_ID, List.of("time", "watts"), null, 600, 899);
        assertEquals(300, lap.count());
        assertFalse(lap.downsampled(), "one interval comes back at full resolution");
        assertEquals(600L, lap.streams().get("time").getFirst().longValue());
    }

    @Test
    void timeInZonesMatchesTotalsAndLapBreakdown() {
        TimeInZonesDto r = analysis.getTimeInZones(TRAINER_ID, List.of(123, 138, 152, 163), List.of(105, 144, 173, 202, 230),
                false, true, List.of(143, 150));

        assertEquals(4628, r.overall().heartrate().totalSec());
        assertEquals(4628, r.overall().power().totalSec());
        assertEquals(5, r.overall().power().zones().size() - 1, "5 bounds -> 6 zones, last index 5");
        assertEquals(15, r.laps().size());
        assertEquals(2, r.overall().hrAbove().size());
        assertTrue(r.overall().hrAbove().get(0).seconds() >= r.overall().hrAbove().get(1).seconds());
        int lapSum = r.laps().stream().mapToInt(l -> l.zones().heartrate().totalSec()).sum();
        assertTrue(lapSum >= 4628 && lapSum <= 4628 + r.laps().size(), "laps share one boundary sample each, got " + lapSum);
        assertNull(r.note());
    }

    @Test
    void outdoorRideSkipsPowerZonesWithNote() {
        TimeInZonesDto r = analysis.getTimeInZones(OUTDOOR_ID, List.of(123, 138, 152, 163), List.of(105, 144),
                true, false, null);

        assertNotNull(r.overall().heartrate());
        assertNull(r.overall().power());
        assertNotNull(r.note());
        assertNull(r.laps());
    }

    @Test
    void invalidArgumentsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> analysis.getTimeInZones(TRAINER_ID, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> analysis.getTimeInZones(TRAINER_ID, List.of(150, 120), null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> analysis.getStreams(999L, List.of("time"), null, null, null));
        assertThrows(IllegalArgumentException.class, () -> analysis.getStreams(TRAINER_ID, List.of(), null, null, null));
    }

    @Test
    void estimatedPowerIsHiddenAndRealPowerOutdoorIsShown() throws Exception {
        // Outdoor ride, Strava-estimated watts: trainer=false, device_watts=false
        Activity estimated = parse("/fit/19166201899.fit", OUTDOOR_ID);
        estimated.setAverageWatts(150.0);
        estimated.setActivityRaw("{\"max_watts\":300.0,\"device_watts\":false}");
        estimated.setLapsRaw("[{\"lap_index\":1,\"distance\":1000.0,\"average_watts\":150.0,\"start_index\":0,\"end_index\":100}]");

        TrainerRideDto hidden = activityService.buildTrainerRideDto(estimated);
        assertFalse(hidden.hasDeviceWatts());
        assertNull(hidden.watts(), "estimated power must be hidden");
        assertNull(hidden.laps().getFirst().watts());
        assertNull(hidden.bestPower());

        RecentActivityDto listed = RecentActivityDto.from(estimated);
        assertFalse(listed.hasDeviceWatts());
        assertNull(listed.averageWatts());

        // Same ride with a real power meter (device_watts=true)
        estimated.setActivityRaw("{\"max_watts\":300.0,\"device_watts\":true}");
        TrainerRideDto shown = activityService.buildTrainerRideDto(estimated);
        assertTrue(shown.hasDeviceWatts());
        assertEquals(150.0, shown.watts().avg());
        assertEquals(150.0, shown.laps().getFirst().watts().avg());
        assertEquals(150.0, RecentActivityDto.from(estimated).averageWatts());

        // Summary-only outdoor row (no activity_raw yet): there is power but its origin is unknown -> null, hidden
        estimated.setActivityRaw(null);
        RecentActivityDto unknown = RecentActivityDto.from(estimated);
        assertNull(unknown.hasDeviceWatts());
        assertNull(unknown.averageWatts());
    }

    private static Activity summary(String type, String sportType, Boolean trainer, Double avgWatts, Double avgHr) {
        Activity a = new Activity();
        a.setStravaId(1L);
        a.setName("x");
        a.setType(type);
        a.setSportType(sportType);
        a.setTrainer(trainer);
        a.setAverageWatts(avgWatts);
        a.setAverageHeartrate(avgHr);
        a.setStartDate(java.time.Instant.parse("2026-10-27T15:10:51Z"));
        return a;
    }

    @Test
    void virtualRideWithoutDetailsHasRealPower() {
        // old Strava-synced MyWhoosh ride: trainer flag empty, no activity_raw, but power is there
        RecentActivityDto d = RecentActivityDto.from(summary("VirtualRide", "VirtualRide", false, 131.04, 125.7));

        assertEquals(Boolean.TRUE, d.hasDeviceWatts());
        assertEquals(131.0, d.averageWatts());
        assertEquals(125.7, d.averageHeartrate());
        // falls back to type when sport_type is empty
        assertEquals(Boolean.TRUE, RecentActivityDto.from(summary("VirtualRide", null, false, 100.0, null)).hasDeviceWatts());
    }

    @Test
    void activitiesWithoutPowerNeverClaimDeviceWatts() {
        // Strava sometimes flags swims/walks (trainer or device_watts) although there is no power
        Activity swim = summary("Swim", "Swim", true, null, 110.0);
        swim.setActivityRaw("{\"device_watts\":true}");
        assertEquals(Boolean.FALSE, RecentActivityDto.from(swim).hasDeviceWatts());
        assertEquals(Boolean.FALSE, RecentActivityDto.from(summary("Walk", "Walk", false, null, 90.0)).hasDeviceWatts());
        // a VirtualRide with no power at all
        assertEquals(Boolean.FALSE, RecentActivityDto.from(summary("VirtualRide", "VirtualRide", false, null, 100.0)).hasDeviceWatts());
    }

    @Test
    void averagesAreRoundedToTenthsEverywhere() throws Exception {
        assertEquals(85.8, RecentActivityDto.from(summary("Walk", "Walk", false, null, 85.82336956521739)).averageHeartrate());
        assertEquals(125.0, RecentActivityDto.from(summary("Ride", "Ride", false, null, 125.0)).averageHeartrate());

        Activity a = parse("/fit/19166201899.fit", OUTDOOR_ID);
        a.setAverageHeartrate(133.33333);
        a.setActivityRaw("{\"average_heartrate\":133.33333,\"max_heartrate\":159.0}");
        a.setLapsRaw("[{\"lap_index\":1,\"distance\":1000.0,\"average_heartrate\":120.66666,\"max_heartrate\":150.0,\"average_cadence\":76.44444,\"start_index\":0,\"end_index\":100}]");
        TrainerRideDto dto = activityService.buildTrainerRideDto(a);

        assertEquals(133.3, dto.heartrate().avg());
        assertEquals(120.7, dto.laps().getFirst().heartrate().avg());
        assertEquals(76.4, dto.laps().getFirst().cadence().avg());
    }

    @Test
    void trainerRideListsPowerAsReal() {
        RecentActivityDto listed = RecentActivityDto.from(trainer);
        assertTrue(listed.hasDeviceWatts());
        assertNotNull(listed.averageWatts());
    }

    @Test
    void trainerLapsGetElevationFromAltitudeStream() {
        TrainerRideDto dto = activityService.buildTrainerRideDto(trainer);

        double sum = 0;
        for (TrainerRideDto.LapDto lap : dto.laps()) {
            assertNotNull(lap.elevationGain(), "lap " + lap.lapNum() + " has no elevation");
            assertTrue(lap.elevationGain() >= 0);
            sum += lap.elevationGain();
        }
        // session ascent is 386 m; lap sums must be in the same ballpark
        assertEquals(trainer.getTotalElevationGain(), sum, trainer.getTotalElevationGain() * 0.15,
                "sum of lap gains " + sum + " vs session " + trainer.getTotalElevationGain());
    }

    @Test
    void stopsAreExposed() {
        assertNotNull(activityService.buildTrainerRideDto(trainer).stops());
        TrainerRideDto outdoorDto = activityService.buildTrainerRideDto(outdoor);
        assertNotNull(outdoorDto.stops());
        // outdoor ride is 8867 s elapsed vs 8036 s moving: there must be stops
        assertFalse(outdoorDto.stops().isEmpty(), "outdoor ride should have stops");
        outdoorDto.stops().forEach(st -> assertTrue(st.durationSec() >= 20));
    }
}
