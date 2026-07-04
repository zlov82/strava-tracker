package xyz.zlov.app.strava.fit;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.zlov.app.strava.activity.Activity;
import xyz.zlov.app.strava.activity.ActivityRepository;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.activity.dto.TrainerRideDto;
import xyz.zlov.app.strava.auth.TokenService;

@Service
@RequiredArgsConstructor
public class FitImportService {

    private final FitParser fitParser;
    private final ActivityRepository activityRepository;
    private final ActivityService activityService;
    private final TokenService tokenService;

    @Value("${strava.athlete-id}")
    private Long configuredAthleteId;

    /** Parse a FIT file and upsert it as an Activity, returning the built DTO. */
    @Transactional
    public TrainerRideDto importFit(byte[] bytes, long stravaId, String name, String description) {
        Activity parsed = fitParser.parse(bytes, stravaId);

        Activity target = activityRepository.findByStravaId(stravaId).orElseGet(Activity::new);
        target.setStravaId(stravaId);
        target.setAthleteId(resolveAthleteId());
        target.setName(name != null && !name.isBlank() ? name.trim() : parsed.getName());
        target.setType(parsed.getType());
        target.setSportType(parsed.getSportType());
        target.setStartDate(parsed.getStartDate());
        target.setDistance(parsed.getDistance());
        target.setMovingTime(parsed.getMovingTime());
        target.setElapsedTime(parsed.getElapsedTime());
        target.setTotalElevationGain(parsed.getTotalElevationGain());
        target.setAverageSpeed(parsed.getAverageSpeed());
        target.setMaxSpeed(parsed.getMaxSpeed());
        target.setAverageHeartrate(parsed.getAverageHeartrate());
        target.setMaxHeartrate(parsed.getMaxHeartrate());
        target.setAverageCadence(parsed.getAverageCadence());
        target.setAverageWatts(parsed.getAverageWatts());
        target.setDescription(description != null ? description.trim() : "");
        target.setMapPolyline(parsed.getMapPolyline());
        target.setActivityRaw(parsed.getActivityRaw());
        target.setLapsRaw(parsed.getLapsRaw());
        target.setStreamsRaw(parsed.getStreamsRaw());
        target.setTrainer(parsed.getTrainer());
        target.setCommute(false);

        Activity saved = activityRepository.save(target);
        return activityService.buildTrainerRideDto(saved);
    }

    /** Athlete id is required (non-null). Prefer the id already in the DB (token, then any
     *  existing activity); otherwise fall back to the configured STRAVA_ATHLETE_ID. */
    private Long resolveAthleteId() {
        try {
            return tokenService.getAthleteId();
        } catch (RuntimeException e) {
            return activityRepository.findAllByOrderByStartDateDesc(PageRequest.of(0, 1))
                    .stream().findFirst()
                    .map(Activity::getAthleteId)
                    .orElse(configuredAthleteId);
        }
    }
}
