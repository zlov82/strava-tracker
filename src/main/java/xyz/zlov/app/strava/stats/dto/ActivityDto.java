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
        /** Exact distance in meters (distanceKm is rounded to 0.1 km and loses precision, e.g. for swims). */
        double distanceM,
        int movingTimeSec,
        int elapsedTimeSec,
        double elevationM,
        Double averageSpeedKmh,
        Double maxSpeedKmh,
        /** Exact speeds in m/s (the km/h fields are rounded to 0.1 and distort swim pace). */
        Double averageSpeedMs,
        Double maxSpeedMs,
        Double averageHeartrate,
        Double maxHeartrate,
        Double averageCadence,
        Double averageWatts,
        boolean trainer,
        boolean commute,
        String description,
        String mapPolyline,
        @JsonRawValue String activityRaw,
        @JsonRawValue String lapsRaw,
        @JsonRawValue String streamsRaw
) {}
