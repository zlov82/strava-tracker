package xyz.zlov.app.strava.activity.dto;

import java.util.List;

public record TimeInZonesDto(
        Long stravaId,
        boolean movingOnly,
        Breakdown overall,
        List<LapBreakdown> laps,
        String note
) {
    /** heartrate/power/hrAbove are null when not requested or not available. */
    public record Breakdown(Distribution heartrate, Distribution power, List<ThresholdShare> hrAbove) {}

    /** totalSec is time with data counted in zones; noDataSec is time without a sample value. */
    public record Distribution(List<ZoneBucket> zones, int totalSec, int noDataSec) {}

    /** lower inclusive, upper exclusive; null = unbounded. */
    public record ZoneBucket(String zone, Integer lower, Integer upper, int seconds, double percent) {}

    public record ThresholdShare(int threshold, int seconds, double percent) {}

    public record LapBreakdown(int lapNum, Integer startSec, Integer endSec, Breakdown zones) {}
}
