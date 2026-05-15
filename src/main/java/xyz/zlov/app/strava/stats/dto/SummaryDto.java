package xyz.zlov.app.strava.stats.dto;

public record SummaryDto(
        double totalDistanceKm,
        double totalTimeHours,
        double totalElevationM,
        long totalActivities
) {}
