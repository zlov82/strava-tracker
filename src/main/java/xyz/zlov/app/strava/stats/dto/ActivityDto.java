package xyz.zlov.app.strava.stats.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.Instant;

public record ActivityDto(
        Long id,
        Long stravaId,
        String name,
        String type,
        String sportType,
        Instant startDate,
        double distanceKm,
        int movingTimeSec,
        int elapsedTimeSec,
        double elevationM,
        Double averageSpeedKmh,
        Double maxSpeedKmh,
        Double averageHeartrate,
        Double maxHeartrate,
        Double averageCadence,
        Double averageWatts,
        boolean trainer,
        boolean commute,
        String description,
        @JsonRawValue String rawData
) {}
