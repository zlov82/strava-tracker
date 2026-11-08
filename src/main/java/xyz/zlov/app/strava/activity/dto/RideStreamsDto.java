package xyz.zlov.app.strava.activity.dto;

import java.util.List;
import java.util.Map;

public record RideStreamsDto(
        Long stravaId,
        int fromSec,
        int toSec,
        int count,
        int sourceCount,
        boolean downsampled,
        Map<String, List<Number>> streams,
        List<String> unavailable
) {}
