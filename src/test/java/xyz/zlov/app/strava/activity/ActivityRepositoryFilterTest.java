package xyz.zlov.app.strava.activity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Needs PostgreSQL like the rest of the Spring context tests (Flyway runs the real migrations). */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActivityRepositoryFilterTest {

    @Autowired
    private ActivityRepository repo;

    private void save(long id, String type, String sportType, String start) {
        Activity a = new Activity();
        a.setStravaId(id);
        a.setAthleteId(1L);
        a.setName("a" + id);
        a.setType(type);
        a.setSportType(sportType);
        a.setStartDate(Instant.parse(start));
        repo.save(a);
    }

    @Test
    void filtersByDateRangeAndSportType() {
        save(9001, "Ride", "Ride", "2026-10-01T10:00:00Z");          // outdoor FIT
        save(9002, "Ride", "VirtualRide", "2026-10-02T10:00:00Z");   // trainer FIT
        save(9003, "VirtualRide", "VirtualRide", "2026-10-03T10:00:00Z"); // Strava sync
        save(9004, "Swim", null, "2026-10-04T10:00:00Z");            // no sport_type -> falls back to type
        save(9005, "Walk", "Walk", "2026-11-01T10:00:00Z");

        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-31T00:00:00Z");
        var page = PageRequest.of(0, 50);

        assertEquals(List.of(9004L, 9003L, 9002L, 9001L), ids(repo.findFiltered(from, to, "", page)));
        assertEquals(List.of(9003L, 9002L), ids(repo.findFiltered(from, to, "virtualride", page)), "case-insensitive");
        assertEquals(List.of(9001L), ids(repo.findFiltered(from, to, "Ride", page)), "Ride excludes trainer rides");
        assertEquals(List.of(9004L), ids(repo.findFiltered(from, to, "Swim", page)));
        assertEquals(List.of(9005L), ids(repo.findFiltered(Instant.parse("2026-11-01T00:00:00Z"), Instant.parse("3000-01-01T00:00:00Z"), "", page)));
        assertEquals(1, repo.findFiltered(from, to, "", PageRequest.of(0, 1)).size(), "limit applies");
    }

    private List<Long> ids(List<Activity> list) {
        return list.stream().map(Activity::getStravaId).toList();
    }
}
