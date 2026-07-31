package xyz.zlov.app.strava.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import xyz.zlov.app.strava.activity.ActivityRepository;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.activity.dto.TrainerRideDto;
import xyz.zlov.app.strava.mcp.dto.RecentActivityDto;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ActivityMcpTools {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ActivityRepository activityRepository;
    private final ActivityService activityService;

    @Tool(description = "List the athlete's most recent training activities across all sport types " +
            "(run, ride, swim, etc.), newest first. Use this to see recent training load before asking " +
            "for detailed ride data.")
    public List<RecentActivityDto> listRecentActivities(
            @ToolParam(description = "Max number of activities to return, most recent first. Defaults to 20, capped at 100.", required = false)
            Integer limit) {
        int size = (limit == null || limit <= 0) ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        return activityRepository.findAllByOrderByStartDateDesc(PageRequest.of(0, size))
                .map(RecentActivityDto::from)
                .getContent();
    }

    @Tool(description = "Get full trainer-ready detail for a single cycling ride by its Strava activity id, " +
            "including heart rate/power/cadence stats and a per-lap breakdown.")
    public TrainerRideDto getRide(@ToolParam(description = "Strava activity id") Long stravaId) {
        return activityService.fetchAndCacheDetails(stravaId)
                .flatMap(a -> activityService.fetchAndCacheStreams(stravaId))
                .map(activityService::buildTrainerRideDto)
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + stravaId));
    }

    @Tool(description = "Get full trainer-ready detail for multiple cycling rides at once, by Strava activity ids. " +
            "Unknown ids are silently skipped.")
    public List<TrainerRideDto> getRides(@ToolParam(description = "List of Strava activity ids") List<Long> stravaIds) {
        return stravaIds.stream()
                .map(id -> activityService.fetchAndCacheDetails(id)
                        .flatMap(a -> activityService.fetchAndCacheStreams(id))
                        .map(activityService::buildTrainerRideDto)
                        .orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }
}
