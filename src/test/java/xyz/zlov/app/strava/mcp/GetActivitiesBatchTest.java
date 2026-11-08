package xyz.zlov.app.strava.mcp;

import org.junit.jupiter.api.Test;
import xyz.zlov.app.strava.activity.Activity;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.activity.StravaDetailsUnavailableException;
import xyz.zlov.app.strava.activity.dto.ActivitiesBatchDto;
import xyz.zlov.app.strava.fit.FitParser;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GetActivitiesBatchTest {

    private static final long OK = 20801346962L;
    private static final long NO_DETAILS = 20752488934L;
    private static final long UNKNOWN = 111L;
    private static final long BROKEN = 222L;

    private final ActivityMcpTools tools = new ActivityMcpTools(null, new StubService(), null);

    /** OK -> loads from a real FIT; NO_DETAILS -> Strava 403; UNKNOWN -> not in DB; BROKEN -> unusable data. */
    private static class StubService extends ActivityService {
        private final Activity ok;

        StubService() {
            super(null, null);
            try (InputStream in = getClass().getResourceAsStream("/fit/20801346962.fit")) {
                ok = new FitParser().parse(in.readAllBytes(), OK);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        @Override
        public Optional<Activity> fetchAndCacheDetails(Long id) {
            if (id == NO_DETAILS) throw new StravaDetailsUnavailableException(id, null);
            if (id == BROKEN) throw new IllegalArgumentException("Activity has no streams");
            return id == OK ? Optional.of(ok) : Optional.empty();
        }

        @Override
        public Optional<Activity> fetchAndCacheStreams(Long id) {
            return fetchAndCacheDetails(id);
        }
    }

    @Test
    void oneBadIdDoesNotFailTheBatch() {
        ActivitiesBatchDto r = tools.getActivities(List.of(NO_DETAILS, OK, UNKNOWN, BROKEN));

        assertEquals(1, r.activities().size());
        assertEquals(OK, r.activities().getFirst().stravaId());
        assertEquals(List.of(NO_DETAILS, UNKNOWN, BROKEN), r.failed().stream().map(ActivitiesBatchDto.Failure::stravaId).toList());
        assertTrue(r.failed().get(0).reason().contains("20752488934.fit"), "actionable FIT hint");
        assertEquals("Activity not found", r.failed().get(1).reason());
        assertEquals("Activity has no streams", r.failed().get(2).reason());
    }

    @Test
    void resultsKeepRequestOrderAndAllFailedIsNotAnError() {
        ActivitiesBatchDto allBad = tools.getActivities(List.of(UNKNOWN, NO_DETAILS));
        assertTrue(allBad.activities().isEmpty());
        assertEquals(2, allBad.failed().size());

        ActivitiesBatchDto empty = tools.getActivities(List.of());
        assertTrue(empty.activities().isEmpty() && empty.failed().isEmpty());
        assertTrue(tools.getActivities(null).activities().isEmpty());
    }

    @Test
    void unexpectedErrorsStillFailTheWholeCall() {
        ActivityMcpTools boom = new ActivityMcpTools(null, new StubService() {
            @Override
            public Optional<Activity> fetchAndCacheDetails(Long id) {
                throw new IllegalStateException("database is down");
            }
        }, null);

        assertThrows(IllegalStateException.class, () -> boom.getActivities(List.of(OK)));
    }
}
