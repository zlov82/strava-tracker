package xyz.zlov.app.strava.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import xyz.zlov.app.strava.activity.Activity;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.fit.FitParser;
import xyz.zlov.app.strava.stats.dto.ActivityDto;

import java.io.InputStream;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/** The site needs exact distance and speed: the rounded km / km/h fields distort swims. */
class DashboardPrecisionTest {

    @Test
    void activityDtoCarriesExactDistanceAndSpeed() throws Exception {
        Activity swim;
        try (InputStream in = getClass().getResourceAsStream("/fit/19331936841.fit")) {
            swim = new FitParser().parse(in.readAllBytes(), 19331936841L);
        }
        Activity finalSwim = swim;
        ActivityService service = new ActivityService(null, null) {
            @Override
            public Optional<Activity> fetchAndCacheDetails(Long id) {
                return Optional.of(finalSwim);
            }
        };
        DashboardController controller = new DashboardController(null, null, null, service);

        ResponseEntity<ActivityDto> response = controller.activity(19331936841L);
        ActivityDto dto = response.getBody();

        assertNotNull(dto);
        assertEquals(1150.0, dto.distanceM(), 0.001);
        assertEquals(1.2, dto.distanceKm(), 0.001, "legacy field stays rounded to 0.1 km");
        assertNotNull(dto.averageSpeedMs());
        assertEquals(swim.getAverageSpeed(), dto.averageSpeedMs(), 1e-9, "exact m/s, not rounded km/h");
        // the rounded km/h field would give 1.9 km/h = 0.5278 m/s and a pace ~5 s/100 m off
        double exactPace = 100 / dto.averageSpeedMs();
        double roundedPace = 100 / (dto.averageSpeedKmh() / 3.6);
        assertTrue(Math.abs(exactPace - roundedPace) > 1.0, "rounding visibly distorts pace: " + exactPace + " vs " + roundedPace);
    }
}
