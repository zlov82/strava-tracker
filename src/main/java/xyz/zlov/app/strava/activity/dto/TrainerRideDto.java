package xyz.zlov.app.strava.activity.dto;

import java.time.Instant;
import java.util.List;

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
        boolean trainer
) {
    public record Stats(Double avg, Double max, Double min) {}

    public record AvgMin(Double avg, Double min) {}

    public record LapDto(
            int lapNum,
            double distance,
            Integer movingTimeSec,
            Double elevationGain,
            Stats heartrate,
            Stats watts,
            AvgMin cadence
    ) {}
}
