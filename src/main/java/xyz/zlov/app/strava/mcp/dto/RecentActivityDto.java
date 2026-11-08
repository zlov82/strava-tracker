package xyz.zlov.app.strava.mcp.dto;

import xyz.zlov.app.strava.activity.Activity;
import xyz.zlov.app.strava.activity.PowerSource;
import xyz.zlov.app.strava.activity.dto.Rounding;

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
        Boolean hasDeviceWatts,
        Double averageSpeedKmh
) {

    public static RecentActivityDto from(Activity a) {
        Boolean hasDeviceWatts = PowerSource.hasDeviceWatts(a);
        boolean realPower = Boolean.TRUE.equals(hasDeviceWatts);
        return new RecentActivityDto(
                a.getStravaId(),
                a.getName(),
                a.getType(),
                a.getSportType(),
                a.getStartDate(),
                Math.round(a.getDistance() / 100.0) / 10.0,
                a.getMovingTime(),
                a.getTotalElevationGain(),
                Rounding.tenths(a.getAverageHeartrate()),
                realPower ? Rounding.tenths(a.getAverageWatts()) : null, // estimated/unknown power is hidden
                hasDeviceWatts,
                a.getAverageSpeed() != null ? Math.round(a.getAverageSpeed() * 36.0) / 10.0 : null
        );
    }
}
