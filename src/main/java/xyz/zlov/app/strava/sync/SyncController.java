package xyz.zlov.app.strava.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import xyz.zlov.app.strava.auth.TokenService;
import xyz.zlov.app.strava.strava.StravaPoller;

import java.time.Instant;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncStateRepository syncStateRepository;
    private final TokenService tokenService;
    private final StravaPoller stravaPoller;

    @PostMapping("/full")
    public SyncStatusDto triggerFullSync() {
        stravaPoller.sync(true);
        return getStatus();
    }

    @GetMapping("/status")
    public SyncStatusDto getStatus() {
        Long athleteId = tokenService.getAthleteId();
        return syncStateRepository.findByAthleteId(athleteId)
                .map(s -> new SyncStatusDto(
                        s.getStatus(),
                        s.getLastSyncAt() > 0 ? Instant.ofEpochSecond(s.getLastSyncAt()).toString() : null,
                        s.getError()
                ))
                .orElse(new SyncStatusDto("NEVER", null, null));
    }

    public record SyncStatusDto(String status, String lastSyncAt, String error) {}
}
