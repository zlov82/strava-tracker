package xyz.zlov.app.strava.stats.dto;

public record ActivityTypeStatsDto(
        String type,
        long count,
        double distanceMeters,
        long movingTimeMinutes,
        Double totalElevationGain
) {}
