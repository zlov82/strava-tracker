package xyz.zlov.app.strava.mcp;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import xyz.zlov.app.strava.activity.ActivityRepository;
import xyz.zlov.app.strava.activity.ActivityService;
import xyz.zlov.app.strava.activity.StreamAnalysisService;
import xyz.zlov.app.strava.activity.dto.RideStreamsDto;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto;
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
    private final StreamAnalysisService streamAnalysisService;

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
            "including heart rate/power/cadence stats and a per-lap breakdown. Laps carry startSec/endSec/elapsedTimeSec " +
            "(seconds from activity start) and startIndex/endIndex into the streams. bestPower holds the best average " +
            "power for 5s..60min windows, only for real (sensor/trainer) power.")
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

    @Tool(description = "Get per-second streams of an activity as equal-length arrays, one per requested stream. " +
            "Supported: time, heartrate, watts, cadence, distance, altitude, velocity (m/s), grade (%, derived), " +
            "moving (0/1, derived from speed), temp (usually unavailable; listed in 'unavailable'). " +
            "Without resolution the response is capped at 2000 points (downsampled by averaging). " +
            "Use fromSec/toSec to read one interval at full resolution.")
    public RideStreamsDto getRideStreams(
            @ToolParam(description = "Strava activity id") Long stravaId,
            @ToolParam(description = "Streams to return, e.g. [\"time\",\"heartrate\",\"watts\"]") List<String> streams,
            @ToolParam(description = "Max number of points after downsampling (e.g. 500). Default cap 2000.", required = false)
            Integer resolution,
            @ToolParam(description = "Start of the range, seconds from activity start (inclusive)", required = false)
            Integer fromSec,
            @ToolParam(description = "End of the range, seconds from activity start (inclusive)", required = false)
            Integer toSec) {
        return streamAnalysisService.getStreams(stravaId, streams, resolution, fromSec, toSec);
    }

    @Tool(description = "Compute time in heart-rate / power zones on the server using the caller's own zone bounds " +
            "(Strava zone settings are NOT used). Bounds are upper limits of zones 1..N-1; the last zone is open-ended; " +
            "each zone is [lower, upper), e.g. hrZones [123,138,152,163] gives Z1 <123, Z2 123-137, ..., Z5 >=163. " +
            "Returns seconds and percent per zone, optionally per lap, and the share of time at/above extra HR thresholds. " +
            "Power zones are only computed for real (sensor/trainer) power.")
    public TimeInZonesDto getTimeInZones(
            @ToolParam(description = "Strava activity id") Long stravaId,
            @ToolParam(description = "Upper bounds of heart-rate zones, ascending, e.g. [123,138,152,163]", required = false)
            List<Integer> hrZones,
            @ToolParam(description = "Upper bounds of power zones in watts, ascending, e.g. [105,144,173,202,230]", required = false)
            List<Integer> powerZones,
            @ToolParam(description = "Count only time in motion. Default true.", required = false)
            Boolean movingOnly,
            @ToolParam(description = "Also break the result down per lap. Default false.", required = false)
            Boolean perLap,
            @ToolParam(description = "Arbitrary HR thresholds; returns share of time at or above each, e.g. [143,145,150]", required = false)
            List<Integer> extraThresholds) {
        return streamAnalysisService.getTimeInZones(stravaId, hrZones, powerZones, movingOnly, perLap, extraThresholds);
    }
}
