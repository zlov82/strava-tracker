package xyz.zlov.app.strava.mcp.dto;

import xyz.zlov.app.strava.activity.Activity;

import java.time.Instant;

public record RecentActivityDto(
        Long stravaId,
        String name,
        String type,
        String sportType,
        Instant startDate,
        double distanceKm,
        int movingTimeSec,
        double elevationM,
        Double averageHeartrate,
        Double averageWatts,
        Double averageSpeedKmh
) {

    public static RecentActivityDto from(Activity a) {
        return new RecentActivityDto(
                a.getStravaId(),
                a.getName(),
                a.getType(),
                a.getSportType(),
                a.getStartDate(),
                Math.round(a.getDistance() / 100.0) / 10.0,
                a.getMovingTime(),
                a.getTotalElevationGain(),
                a.getAverageHeartrate(),
                a.getAverageWatts(),
                a.getAverageSpeed() != null ? Math.round(a.getAverageSpeed() * 36.0) / 10.0 : null
        );
    }
}
