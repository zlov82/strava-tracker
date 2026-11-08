package xyz.zlov.app.strava.activity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.zlov.app.strava.activity.analysis.LapBounds;
import xyz.zlov.app.strava.activity.analysis.StreamData;
import xyz.zlov.app.strava.activity.analysis.StreamSampler;
import xyz.zlov.app.strava.activity.analysis.ZoneCalculator;
import xyz.zlov.app.strava.activity.dto.RideStreamsDto;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.Breakdown;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.Distribution;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.LapBreakdown;
import xyz.zlov.app.strava.activity.dto.TimeInZonesDto.ThresholdShare;

import java.util.ArrayList;
import java.util.List;

/** Per-second stream queries used by the MCP trainer tools. */
@Service
@RequiredArgsConstructor
public class StreamAnalysisService {

    /** Cap applied when the caller does not pass {@code resolution}, to keep responses a sane size. */
    static final int DEFAULT_MAX_POINTS = 2000;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ActivityService activityService;

    public RideStreamsDto getStreams(Long stravaId, List<String> streams, Integer resolution,
                                     Integer fromSec, Integer toSec) {
        if (streams == null || streams.isEmpty()) {
            throw new IllegalArgumentException("streams must not be empty; supported: " + StreamSampler.SUPPORTED);
        }
        Activity activity = load(stravaId);
        StreamData data = StreamData.parse(activity.getStreamsRaw());

        List<String> requested = streams.stream().map(s -> s.trim().toLowerCase()).distinct().toList();
        Integer effective = resolution != null && resolution > 0 ? resolution : DEFAULT_MAX_POINTS;
        StreamSampler.Result r = StreamSampler.sample(data, requested, effective, fromSec, toSec);
        List<String> unavailable = new ArrayList<>(r.unavailable());

        return new RideStreamsDto(stravaId, r.from(), r.to(), pointCount(r), r.sourceCount(),
                pointCount(r) < r.sourceCount(), r.streams(), unavailable);
    }

    public TimeInZonesDto getTimeInZones(Long stravaId, List<Integer> hrZones, List<Integer> powerZones,
                                         Boolean movingOnly, Boolean perLap, List<Integer> extraThresholds) {
        boolean moving = movingOnly == null || movingOnly;
        boolean laps = Boolean.TRUE.equals(perLap);
        boolean wantHr = hrZones != null && !hrZones.isEmpty();
        boolean wantPower = powerZones != null && !powerZones.isEmpty();
        boolean wantThresholds = extraThresholds != null && !extraThresholds.isEmpty();
        if (!wantHr && !wantPower && !wantThresholds) {
            throw new IllegalArgumentException("Pass at least one of hrZones, powerZones, extraThresholds");
        }
        if (wantHr) ZoneCalculator.validateBounds("hrZones", hrZones);
        if (wantPower) ZoneCalculator.validateBounds("powerZones", powerZones);

        Activity activity = load(stravaId);
        StreamData data = StreamData.parse(activity.getStreamsRaw());

        List<String> notes = new ArrayList<>();
        boolean hrAvailable = data.has("heartrate");
        if ((wantHr || wantThresholds) && !hrAvailable) notes.add("No heartrate stream: HR zones/thresholds skipped.");
        boolean powerAvailable = data.has("watts") && activityService.hasRealPower(activity);
        if (wantPower && !powerAvailable) {
            notes.add(data.has("watts")
                    ? "Power is not from a sensor/trainer (estimated): power zones skipped."
                    : "No watts stream: power zones skipped.");
        }

        Breakdown overall = breakdown(data, 0, data.size() - 1, moving,
                wantHr && hrAvailable ? hrZones : null,
                wantPower && powerAvailable ? powerZones : null,
                wantThresholds && hrAvailable ? extraThresholds : null);

        List<LapBreakdown> lapResults = null;
        if (laps) {
            lapResults = new ArrayList<>();
            if (activity.getLapsRaw() == null) {
                notes.add("No laps stored for this activity: perLap skipped.");
            } else {
                try {
                    int num = 0;
                    for (JsonNode lap : MAPPER.readTree(activity.getLapsRaw())) {
                        LapBounds b = LapBounds.of(lap, data, ++num);
                        if (b.startIndex() == null) continue;
                        lapResults.add(new LapBreakdown(b.lapNum(), b.startSec(), b.endSec(),
                                breakdown(data, b.startIndex(), b.endIndex(), moving,
                                        wantHr && hrAvailable ? hrZones : null,
                                        wantPower && powerAvailable ? powerZones : null,
                                        wantThresholds && hrAvailable ? extraThresholds : null)));
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Cannot parse laps: " + e.getMessage(), e);
                }
            }
        }
        return new TimeInZonesDto(stravaId, moving, overall, lapResults,
                notes.isEmpty() ? null : String.join(" ", notes));
    }

    private Breakdown breakdown(StreamData d, int from, int to, boolean moving,
                                List<Integer> hrZones, List<Integer> powerZones, List<Integer> thresholds) {
        Distribution hr = hrZones == null ? null : ZoneCalculator.distribution(d, "heartrate", hrZones, from, to, moving);
        Distribution pw = powerZones == null ? null : ZoneCalculator.distribution(d, "watts", powerZones, from, to, moving);
        List<ThresholdShare> above = thresholds == null ? null : ZoneCalculator.thresholds(d, "heartrate", thresholds, from, to, moving);
        return new Breakdown(hr, pw, above);
    }

    private int pointCount(StreamSampler.Result r) {
        return r.streams().values().stream().findFirst().map(List::size).orElse(0);
    }

    private Activity load(Long stravaId) {
        return activityService.fetchAndCacheDetails(stravaId)
                .flatMap(a -> activityService.fetchAndCacheStreams(stravaId))
                .orElseThrow(() -> new IllegalArgumentException("Activity not found: " + stravaId));
    }
}
