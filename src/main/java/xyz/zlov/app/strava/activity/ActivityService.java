package xyz.zlov.app.strava.activity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.zlov.app.strava.activity.dto.TrainerRideDto;
import xyz.zlov.app.strava.strava.StravaClient;
import xyz.zlov.app.strava.strava.dto.StravaActivityDto;

import java.time.Instant;
import java.util.ArrayList;
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

    public TrainerRideDto buildTrainerRideDto(Activity activity) {
        Double avgHr = null;
        Double maxHr = null;
        Double minHr = null;
        Double maxWatts = null;
        Double maxCadence = null;

        if (activity.getActivityRaw() != null) {
            try {
                JsonNode node = MAPPER.readTree(activity.getActivityRaw());
                maxWatts = doubleOrNull(node, "max_watts");
                maxHr = doubleOrNull(node, "max_heartrate");
                avgHr = doubleOrNull(node, "average_heartrate");
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse activity_raw for stravaId={}", activity.getStravaId());
            }
        }

        Double minWatts = null;
        Double minCadence = null;
        JsonNode streams = null;

        if (activity.getStreamsRaw() != null) {
            try {
                streams = MAPPER.readTree(activity.getStreamsRaw());
                minHr = minFromStream(streams, "heartrate", false);
                minWatts = minFromStream(streams, "watts", true);
                minCadence = minFromStream(streams, "cadence", true);
                maxCadence = maxFromStream(streams, "cadence", true);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse streams_raw for stravaId={}", activity.getStravaId());
            }
        }

        List<TrainerRideDto.LapDto> laps = new ArrayList<>();

        if (activity.getLapsRaw() != null) {
            try {
                JsonNode lapsNode = MAPPER.readTree(activity.getLapsRaw());
                for (JsonNode lap : lapsNode) {
                    int startIdx = lap.has("start_index") ? lap.get("start_index").asInt() : -1;
                    int endIdx   = lap.has("end_index")   ? lap.get("end_index").asInt()   : -1;

                    Double lapMinHr      = minFromStreamSlice(streams, "heartrate", startIdx, endIdx, false);
                    Double lapMinCadence = minFromStreamSlice(streams, "cadence",   startIdx, endIdx, true);
                    Double lapMinWatts   = minFromStreamSlice(streams, "watts",     startIdx, endIdx, true);
                    Double lapMaxWatts   = maxFromStreamSlice(streams, "watts",     startIdx, endIdx, true);

                    TrainerRideDto.Stats lapHr = statsOrNull(
                            doubleOrNull(lap, "average_heartrate"),
                            doubleOrNull(lap, "max_heartrate"),
                            lapMinHr);
                    TrainerRideDto.Stats lapWatts = statsOrNull(
                            doubleOrNull(lap, "average_watts"),
                            lapMaxWatts,
                            lapMinWatts);
                    Double lapAvgCadence = doubleOrNull(lap, "average_cadence");
                    TrainerRideDto.AvgMin lapCadence = lapAvgCadence != null
                            ? new TrainerRideDto.AvgMin(lapAvgCadence, lapMinCadence)
                            : null;

                    Integer lapMovingTime = lap.has("moving_time") && !lap.get("moving_time").isNull()
                            ? lap.get("moving_time").asInt() : null;
                    Double lapElevation = doubleOrNull(lap, "total_elevation_gain");

                    laps.add(new TrainerRideDto.LapDto(
                            lap.has("lap_index") ? lap.get("lap_index").asInt() : 0,
                            lap.has("distance") ? lap.get("distance").asDouble() : 0.0,
                            lapMovingTime,
                            lapElevation,
                            lapHr, lapWatts, lapCadence));
                }
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse laps_raw for stravaId={}", activity.getStravaId());
            }
        }

        TrainerRideDto.Stats heartrate = statsOrNull(avgHr, maxHr, minHr);
        TrainerRideDto.Stats watts = statsOrNull(
                activity.getAverageWatts(), maxWatts, minWatts);
        TrainerRideDto.Stats cadence = statsOrNull(
                activity.getAverageCadence(), maxCadence, minCadence);

        return new TrainerRideDto(
                activity.getStravaId(),
                activity.getStartDate(),
                activity.getName(),
                activity.getType(),
                activity.getSportType(),
                activity.getDistance(),
                activity.getElapsedTime(),
                activity.getMovingTime(),
                heartrate, watts, cadence,
                activity.getTotalElevationGain(),
                activity.getAverageSpeed() != null ? Math.round(activity.getAverageSpeed() * 36.0) / 10.0 : null,
                activity.getMaxSpeed() != null ? Math.round(activity.getMaxSpeed() * 36.0) / 10.0 : null,
                laps,
                activity.getDescription(),
                Boolean.TRUE.equals(activity.getCommute()),
                Boolean.TRUE.equals(activity.getTrainer())
        );
    }

    private TrainerRideDto.Stats statsOrNull(Double avg, Double max, Double min) {
        if (avg == null && max == null && min == null) return null;
        return new TrainerRideDto.Stats(avg, max, min);
    }

    private Double minFromStreamSlice(JsonNode streams, String key, int from, int to, boolean skipZeros) {
        if (streams == null || from < 0 || to < 0) return null;
        JsonNode stream = streams.get(key);
        if (stream == null || !stream.has("data")) return null;
        JsonNode data = stream.get("data");
        Double min = null;
        for (int i = from; i <= to && i < data.size(); i++) {
            JsonNode val = data.get(i);
            if (val != null && !val.isNull()) {
                double v = val.asDouble();
                if (skipZeros && v == 0) continue;
                if (min == null || v < min) min = v;
            }
        }
        return min;
    }

    private Double maxFromStreamSlice(JsonNode streams, String key, int from, int to, boolean skipZeros) {
        if (streams == null || from < 0 || to < 0) return null;
        JsonNode stream = streams.get(key);
        if (stream == null || !stream.has("data")) return null;
        JsonNode data = stream.get("data");
        Double max = null;
        for (int i = from; i <= to && i < data.size(); i++) {
            JsonNode val = data.get(i);
            if (val != null && !val.isNull()) {
                double v = val.asDouble();
                if (skipZeros && v == 0) continue;
                if (max == null || v > max) max = v;
            }
        }
        return max;
    }

    private Double maxFromStream(JsonNode streams, String key, boolean skipZeros) {
        JsonNode stream = streams.get(key);
        if (stream == null || !stream.has("data")) return null;
        Double max = null;
        for (JsonNode val : stream.get("data")) {
            if (!val.isNull()) {
                double v = val.asDouble();
                if (skipZeros && v == 0) continue;
                if (max == null || v > max) max = v;
            }
        }
        return max;
    }

    private Double minFromStream(JsonNode streams, String key, boolean skipZeros) {
        JsonNode stream = streams.get(key);
        if (stream == null || !stream.has("data")) return null;
        Double min = null;
        for (JsonNode val : stream.get("data")) {
            if (!val.isNull()) {
                double v = val.asDouble();
                if (skipZeros && v == 0) continue;
                if (min == null || v < min) min = v;
            }
        }
        return min;
    }

    private Double doubleOrNull(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asDouble() : null;
    }

    private double orZero(Double value) {
        return value != null ? value : 0.0;
    }

    private int orZeroInt(Integer value) {
        return value != null ? value : 0;
    }
}
