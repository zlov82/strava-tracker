package xyz.zlov.app.strava.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

import java.util.List;
import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long> {

    Optional<Activity> findByStravaId(Long stravaId);

    @Query("SELECT MAX(a.startDate) FROM Activity a WHERE a.athleteId = :athleteId")
    Optional<Instant> findMaxStartDateByAthleteId(@Param("athleteId") Long athleteId);

    Page<Activity> findAllByOrderByStartDateDesc(Pageable pageable);

    @Query(value = """
            SELECT to_char(date_trunc('week', start_date), 'IYYY-"W"IW') AS week,
                   round(cast(sum(distance) / 1000.0 AS numeric), 1)     AS km
            FROM activities
            WHERE start_date >= now() - make_interval(weeks => :weeks)
              AND (:type = '' OR type = :type)
            GROUP BY date_trunc('week', start_date)
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> findWeeklyVolume(@Param("weeks") int weeks, @Param("type") String type);

    @Query(value = """
            SELECT to_char(date_trunc('month', start_date), 'YYYY-MM') AS month,
                   round(cast(sum(distance) / 1000.0 AS numeric), 1)   AS km
            FROM activities
            WHERE start_date >= now() - make_interval(months => :months)
            GROUP BY date_trunc('month', start_date)
            ORDER BY 1
            """, nativeQuery = true)
    List<Object[]> findMonthlyVolume(@Param("months") int months);

    @Query("SELECT SUM(a.distance)/1000.0, SUM(a.movingTime)/3600.0, SUM(a.totalElevationGain), COUNT(a) FROM Activity a")
    List<Object[]> findSummaryStats();

    @Query(value = """
            SELECT to_char(start_date, 'YYYY-MM-DD') AS date,
                   name,
                   average_speed
            FROM activities
            WHERE type = :type AND distance > 100 AND average_speed IS NOT NULL
            ORDER BY start_date DESC
            """, nativeQuery = true)
    List<Object[]> findPaceTrend(@Param("type") String type, Pageable pageable);

    @Query("SELECT a.type, COUNT(a) FROM Activity a GROUP BY a.type ORDER BY COUNT(a) DESC")
    List<Object[]> findTypeBreakdown();
}
