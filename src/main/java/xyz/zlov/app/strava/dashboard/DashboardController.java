package xyz.zlov.app.strava.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.zlov.app.strava.activity.Activity;
import xyz.zlov.app.strava.activity.ActivityRepository;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.stats.StatsService;
import xyz.zlov.app.strava.stats.dto.*;
import xyz.zlov.app.strava.strava.StravaClient;
import xyz.zlov.app.strava.strava.dto.StravaAthleteDto;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {

    private final StatsService statsService;
    private final ActivityRepository activityRepository;
    private final StravaClient stravaClient;
    private final ActivityService activityService;

    @GetMapping("/athlete")
    public Map<String, String> getAthlete() {
        StravaAthleteDto a = stravaClient.getAthlete();
        return Map.of(
                "name", a.getFirstname() + " " + a.getLastname(),
                "avatar", a.getProfile_medium() != null ? a.getProfile_medium() : ""
        );
    }

    @GetMapping("/stats/weekly")
    public List<WeeklyVolumeDto> weekly(
            @RequestParam(defaultValue = "12") int weeks,
            @RequestParam(defaultValue = "") String type) {
        return statsService.getWeeklyVolume(weeks, type);
    }

    @GetMapping("/stats/monthly")
    public List<MonthlyVolumeDto> monthly(
            @RequestParam(defaultValue = "6") int months) {
        return statsService.getMonthlyVolume(months);
    }

    @GetMapping("/stats/summary")
    public SummaryDto summary() {
        return statsService.getSummary();
    }

    @GetMapping("/stats/pace")
    public List<PaceTrendDto> pace(
            @RequestParam(defaultValue = "Ride") String type,
            @RequestParam(defaultValue = "30") int limit) {
        return statsService.getPaceTrend(type, limit);
    }

    @GetMapping("/stats/breakdown")
    public List<TypeBreakdownDto> breakdown() {
        return statsService.getActivityTypeBreakdown();
    }

    @GetMapping("/activities/{stravaId}")
    public ResponseEntity<ActivityDto> activity(@PathVariable Long stravaId) {
        return activityService.fetchAndCacheDescription(stravaId)
                .map(a -> ResponseEntity.ok(toDto(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/activities")
    public Page<ActivityDto> activities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return activityRepository.findAllByOrderByStartDateDesc(PageRequest.of(page, size))
                .map(this::toDto);
    }

    private ActivityDto toDto(Activity a) {
        return new ActivityDto(
                a.getId(),
                a.getStravaId(),
                a.getName(),
                a.getType(),
                a.getSportType(),
                a.getStartDate(),
                Math.round(a.getDistance() / 100.0) / 10.0,
                a.getMovingTime(),
                a.getElapsedTime(),
                a.getTotalElevationGain(),
                a.getAverageSpeed() != null ? Math.round(a.getAverageSpeed() * 36.0) / 10.0 : null,
                a.getMaxSpeed() != null ? Math.round(a.getMaxSpeed() * 36.0) / 10.0 : null,
                a.getAverageHeartrate(),
                a.getMaxHeartrate(),
                a.getAverageCadence(),
                a.getAverageWatts(),
                Boolean.TRUE.equals(a.getTrainer()),
                Boolean.TRUE.equals(a.getCommute()),
                a.getDescription(),
                a.getRawData()
        );
    }
}
