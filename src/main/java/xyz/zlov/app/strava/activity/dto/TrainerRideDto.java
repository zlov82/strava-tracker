package xyz.zlov.app.strava.activity.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record TrainerRideDto(
        Long stravaId,
        Instant date,
        String name,
        String type,
        String sportType,
        double distance,
        int elapsedTimeSec,
        int movingTimeSec,
        Stats heartrate,
        Stats watts,
        Stats cadence,
        double elevation,
        Double avgSpeed,
        Double maxSpeed,
        List<LapDto> laps,
        String description,
        boolean commute,
        boolean trainer,
        boolean hasDeviceWatts,
        Map<String, Double> bestPower,
        List<Stop> stops
) {
    /** A stop of at least 20 s: seconds from start, length, and distance covered when it began (m). */
    public record Stop(int startSec, int durationSec, Double atDistanceM) {}

    public record Stats(Double avg, Double max, Double min) {}

    public record AvgMin(Double avg, Double min) {}

    public record LapDto(
            int lapNum,
            double distance,
            Integer movingTimeSec,
            Double elevationGain,
            Stats heartrate,
            Stats watts,
            AvgMin cadence,
            Integer startSec,
            Integer endSec,
            Integer elapsedTimeSec,
            Integer startIndex,
            Integer endIndex
    ) {}
}
