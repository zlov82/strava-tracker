package xyz.zlov.app.strava.activity.dto;

import java.util.List;

/** Result of a batch lookup: what could be loaded, plus per-id reasons for what could not. */
public record ActivitiesBatchDto(List<TrainerRideDto> activities, List<Failure> failed) {

    public record Failure(Long stravaId, String reason) {}
}
