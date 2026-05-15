package xyz.zlov.app.strava.strava;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import xyz.zlov.app.strava.activity.ActivityRepository;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.auth.TokenService;
import xyz.zlov.app.strava.strava.dto.StravaActivityDto;
import xyz.zlov.app.strava.sync.SyncState;
import xyz.zlov.app.strava.sync.SyncStateRepository;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StravaPoller {

    private final StravaClient stravaClient;
    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final SyncStateRepository syncStateRepository;
    private final TokenService tokenService;

    @Scheduled(fixedDelay = 3_600_000)
    public void sync() { sync(false); }

    public void sync(boolean fullSync) {
        Long athleteId;
        try {
            athleteId = tokenService.getAthleteId();
        } catch (IllegalStateException e) {
            log.info("No OAuth token yet, skipping sync");
            return;
        }

        SyncState state = syncStateRepository.findByAthleteId(athleteId)
                .orElseGet(() -> {
                    SyncState s = new SyncState();
                    s.setAthleteId(athleteId);
                    return s;
                });

        state.setStatus("RUNNING");
        state.setError(null);
        syncStateRepository.save(state);

        try {
            long after = fullSync ? 0L : activityRepository.findMaxStartDateByAthleteId(athleteId)
                    .map(Instant::getEpochSecond)
                    .orElse(0L);
            log.info("Starting Strava sync: after={}, mode={}", after, after == 0 ? "FULL" : "INCREMENTAL");

            int page = 1;
            int total = 0;
            List<StravaActivityDto> batch;
            do {
                batch = stravaClient.getActivities(after, page++);
                if (!batch.isEmpty()) {
                    activityService.upsertActivities(batch);
                    total += batch.size();
                    log.info("Synced {} activities (total so far: {})", batch.size(), total);
                }
            } while (batch.size() == 50);

            state.setLastSyncAt(Instant.now().getEpochSecond());
            state.setStatus("IDLE");
            log.info("Sync complete. Total activities synced: {}", total);
        } catch (Exception e) {
            log.error("Sync failed: {}", e.getMessage(), e);
            state.setStatus("ERROR");
            state.setError(e.getMessage());
        }

        syncStateRepository.save(state);
    }
}
