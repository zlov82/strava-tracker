package xyz.zlov.app.strava.stats;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import xyz.zlov.app.strava.activity.ActivityRepository;
import xyz.zlov.app.strava.stats.dto.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final ActivityRepository activityRepository;

    public List<WeeklyVolumeDto> getWeeklyVolume(int weeks, String type) {
        return activityRepository.findWeeklyVolume(weeks, type == null ? "" : type)
                .stream()
                .map(r -> new WeeklyVolumeDto((String) r[0], toDouble(r[1])))
                .toList();
    }

    public List<MonthlyVolumeDto> getMonthlyVolume(int months) {
        return activityRepository.findMonthlyVolume(months)
                .stream()
                .map(r -> new MonthlyVolumeDto((String) r[0], toDouble(r[1])))
                .toList();
    }

    public SummaryDto getSummary() {
        List<Object[]> rows = activityRepository.findSummaryStats();
        if (rows.isEmpty() || rows.get(0)[0] == null) {
            return new SummaryDto(0, 0, 0, 0);
        }
        Object[] r = rows.get(0);
        return new SummaryDto(
                toDouble(r[0]),
                toDouble(r[1]),
                toDouble(r[2]),
                toLong(r[3])
        );
    }

    public List<PaceTrendDto> getPaceTrend(String type, int limit) {
        return activityRepository.findPaceTrend(type, PageRequest.of(0, limit))
                .stream()
                .map(r -> new PaceTrendDto(
                        (String) r[0],
                        (String) r[1],
                        // average_speed from Strava is in m/s → convert to km/h
                        Math.round(toDouble(r[2]) * 3.6 * 10.0) / 10.0
                ))
                .toList();
    }

    public MonthlyStatsByTypeDto getMonthlyStatsByType(int year, int month) {
        List<ActivityTypeStatsDto> stats = activityRepository.findMonthlyStatsByType(year, month)
                .stream()
                .map(r -> new ActivityTypeStatsDto(
                        (String) r[0],
                        toLong(r[1]),
                        toDouble(r[2]),
                        toLong(r[3]),
                        r[4] != null ? toDouble(r[4]) : null
                ))
                .toList();
        return new MonthlyStatsByTypeDto(year, month, stats);
    }

    public List<TypeBreakdownDto> getActivityTypeBreakdown() {
        List<Object[]> rows = activityRepository.findTypeBreakdown();
        long total = rows.stream().mapToLong(r -> toLong(r[1])).sum();
        return rows.stream()
                .map(r -> new TypeBreakdownDto(
                        (String) r[0],
                        toLong(r[1]),
                        total > 0 ? Math.round(toLong(r[1]) * 1000.0 / total) / 10.0 : 0
                ))
                .toList();
    }

    private double toDouble(Object val) {
        if (val == null) return 0.0;
        return ((Number) val).doubleValue();
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        return ((Number) val).longValue();
    }
}
