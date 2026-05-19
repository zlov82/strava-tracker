package xyz.zlov.app.strava.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.zlov.app.strava.strava.StravaClient;
import xyz.zlov.app.strava.strava.dto.StravaActivityDto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ActivityRepository activityRepository;
    private final StravaClient stravaClient;

    @Transactional
    public Optional<Activity> fetchAndCacheDetails(Long stravaId) {
        Activity activity = activityRepository.findByStravaId(stravaId).orElse(null);
        if (activity == null) return Optional.empty();
        if (activity.getActivityRaw() != null) return Optional.of(activity);

        String activityJson = stravaClient.getActivityRaw(stravaId);
        String lapsJson     = stravaClient.getActivityLapsRaw(stravaId);

        activity.setActivityRaw(activityJson);
        activity.setLapsRaw(lapsJson);

        try {
            var node = MAPPER.readTree(activityJson);
            if (node.has("description") && !node.get("description").isNull()) {
                activity.setDescription(node.get("description").asText());
            } else {
                activity.setDescription("");
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse activity JSON for stravaId={}", stravaId);
        }

        activityRepository.save(activity);
        return Optional.of(activity);
    }

    @Transactional
    public Optional<Activity> fetchAndCacheStreams(Long stravaId) {
        Activity activity = activityRepository.findByStravaId(stravaId).orElse(null);
        if (activity == null) return Optional.empty();
        if (activity.getStreamsRaw() != null) return Optional.of(activity);

        activity.setStreamsRaw(stravaClient.getActivityStreamsRaw(stravaId));
        activityRepository.save(activity);
        return Optional.of(activity);
    }

    @Transactional
    public void upsertActivities(List<StravaActivityDto> dtos) {
        for (StravaActivityDto dto : dtos) {
            Activity activity = activityRepository.findByStravaId(dto.getId())
                    .orElseGet(Activity::new);
            mapDto(activity, dto);
            activityRepository.save(activity);
        }
    }

    private void mapDto(Activity activity, StravaActivityDto dto) {
        activity.setStravaId(dto.getId());
        activity.setAthleteId(dto.getAthlete() != null ? dto.getAthlete().getId() : null);
        activity.setName(dto.getName());
        activity.setType(dto.getType() != null ? dto.getType() : "Unknown");
        activity.setSportType(dto.getSport_type());
        activity.setStartDate(dto.getStart_date() != null ? Instant.parse(dto.getStart_date()) : Instant.now());
        activity.setDistance(orZero(dto.getDistance()));
        activity.setMovingTime(orZeroInt(dto.getMoving_time()));
        activity.setElapsedTime(orZeroInt(dto.getElapsed_time()));
        activity.setTotalElevationGain(orZero(dto.getTotal_elevation_gain()));
        activity.setAverageSpeed(dto.getAverage_speed());
        activity.setMaxSpeed(dto.getMax_speed());
        activity.setAverageHeartrate(dto.getAverage_heartrate());
        activity.setMaxHeartrate(dto.getMax_heartrate());
        activity.setAverageCadence(dto.getAverage_cadence());
        activity.setAverageWatts(dto.getAverage_watts());
        activity.setDescription(dto.getDescription());
        activity.setTrainer(dto.getTrainer() != null && dto.getTrainer());
        activity.setCommute(dto.getCommute() != null && dto.getCommute());
        activity.setMapPolyline(dto.getMap() != null ? dto.getMap().getSummary_polyline() : null);
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }

    private int orZeroInt(Integer value) {
        return value != null ? value : 0;
    }
}
