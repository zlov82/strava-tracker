package xyz.zlov.app.strava.fit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.garmin.fit.FitDecoder;
import com.garmin.fit.FitMessages;
import com.garmin.fit.LapMesg;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.Sport;
import com.garmin.fit.SubSport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import xyz.zlov.app.strava.activity.Activity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Decodes a Garmin FIT file into an {@link Activity}, reproducing the exact JSON shapes
 * (streams_raw / laps_raw / activity_raw) and encoded map polyline that the app previously
 * received from the Strava API, so all downstream code keeps working unchanged.
 */
@Slf4j
@Component
public class FitParser {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double SEMICIRCLE_TO_DEG = 180.0 / Math.pow(2, 31);
    private static final String[] MONTHS_GENITIVE = {
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };

    /** Parse a FIT (or gzipped FIT) file. name/description/athleteId are filled by the caller. */
    public Activity parse(byte[] bytes, long stravaId) {
        FitMessages messages = decode(bytes);

        List<SessionMesg> sessions = messages.getSessionMesgs();
        if (sessions.isEmpty()) {
            throw new FitParseException("FIT file has no session message");
        }
        SessionMesg session = sessions.getFirst();
        List<RecordMesg> records = messages.getRecordMesgs();

        Activity a = new Activity();
        a.setStravaId(stravaId);

        Instant start = session.getStartTime() != null
                ? session.getStartTime().getDate().toInstant()
                : Instant.now();
        a.setStartDate(start);

        a.setDistance(orZero(session.getTotalDistance()));
        a.setMovingTime(seconds(firstNonNull(session.getTotalTimerTime(), session.getTotalMovingTime(), session.getTotalElapsedTime())));
        a.setElapsedTime(seconds(session.getTotalElapsedTime()));
        a.setTotalElevationGain(session.getTotalAscent() != null ? session.getTotalAscent().doubleValue() : 0.0);
        a.setAverageSpeed(toDouble(firstNonNull(session.getEnhancedAvgSpeed(), session.getAvgSpeed())));
        a.setMaxSpeed(toDouble(firstNonNull(session.getEnhancedMaxSpeed(), session.getMaxSpeed())));
        a.setAverageHeartrate(toDouble(session.getAvgHeartRate()));
        a.setMaxHeartrate(toDouble(session.getMaxHeartRate()));
        a.setAverageCadence(toDouble(session.getAvgCadence()));
        a.setAverageWatts(toDouble(session.getAvgPower()));

        applySport(a, session.getSport(), session.getSubSport());
        a.setCommute(false);
        a.setName(autoName(a.getType(), start));
        a.setDescription("");

        long startTs = session.getStartTime() != null ? session.getStartTime().getTimestamp() : 0L;
        a.setStreamsRaw(buildStreams(records, startTs));
        a.setLapsRaw(buildLaps(messages.getLapMesgs(), records));
        a.setActivityRaw(buildActivityRaw(session));
        a.setMapPolyline(buildPolyline(records));

        return a;
    }

    private FitMessages decode(byte[] bytes) {
        try (InputStream in = isGzip(bytes)
                ? new GZIPInputStream(new ByteArrayInputStream(bytes))
                : new ByteArrayInputStream(bytes)) {
            return new FitDecoder().decode(in);
        } catch (IOException | RuntimeException e) {
            throw new FitParseException("Failed to decode FIT file: " + e.getMessage(), e);
        }
    }

    private static boolean isGzip(byte[] b) {
        return b.length >= 2 && (b[0] & 0xff) == 0x1f && (b[1] & 0xff) == 0x8b;
    }

    // ── Type mapping ─────────────────────────────────────────────────────────────

    private void applySport(Activity a, Sport sport, SubSport subSport) {
        boolean indoor = subSport == SubSport.INDOOR_CYCLING || subSport == SubSport.VIRTUAL_ACTIVITY;
        if (sport == Sport.CYCLING) {
            a.setType("Ride");
            a.setSportType(indoor ? "VirtualRide" : "Ride");
            a.setTrainer(indoor);
        } else if (sport == Sport.RUNNING) {
            a.setType("Run");
            a.setSportType("Run");
            a.setTrainer(false);
        } else if (sport == Sport.WALKING || sport == Sport.HIKING) {
            a.setType("Walk");
            a.setSportType("Walk");
            a.setTrainer(false);
        } else if (sport == Sport.SWIMMING) {
            a.setType("Swim");
            a.setSportType("Swim");
            a.setTrainer(false);
        } else {
            String name = sport != null ? capitalize(sport.name()) : "Workout";
            a.setType(name);
            a.setSportType(name);
            a.setTrainer(false);
        }
    }

    private String autoName(String type, Instant start) {
        String noun = switch (type) {
            case "Ride" -> "Заезд";
            case "Run" -> "Пробежка";
            case "Swim" -> "Заплыв";
            case "Walk" -> "Прогулка";
            default -> "Тренировка";
        };
        ZonedDateTime local = start.atZone(ZoneId.systemDefault());
        return noun + " " + local.getDayOfMonth() + " " + MONTHS_GENITIVE[local.getMonthValue() - 1];
    }

    // ── Streams (Strava key_by_type format) ──────────────────────────────────────

    private String buildStreams(List<RecordMesg> records, long startTs) {
        ArrayNode time = MAPPER.createArrayNode();
        ArrayNode distance = MAPPER.createArrayNode();
        ArrayNode altitude = MAPPER.createArrayNode();
        ArrayNode velocity = MAPPER.createArrayNode();
        ArrayNode heartrate = MAPPER.createArrayNode();
        ArrayNode cadence = MAPPER.createArrayNode();
        ArrayNode watts = MAPPER.createArrayNode();

        boolean hasAlt = false, hasVel = false, hasHr = false, hasCad = false, hasWatts = false;

        for (RecordMesg r : records) {
            long ts = r.getTimestamp() != null ? r.getTimestamp().getTimestamp() : startTs;
            time.add(ts - startTs);
            addFloat(distance, r.getDistance());

            Float alt = firstNonNull(r.getEnhancedAltitude(), r.getAltitude());
            hasAlt |= addFloat(altitude, alt);
            Float vel = firstNonNull(r.getEnhancedSpeed(), r.getSpeed());
            hasVel |= addFloat(velocity, vel);
            hasHr |= addShort(heartrate, r.getHeartRate());
            hasCad |= addShort(cadence, r.getCadence());
            hasWatts |= addInt(watts, r.getPower());
        }

        ObjectNode root = MAPPER.createObjectNode();
        putStream(root, "time", time);
        putStream(root, "distance", distance);
        if (hasAlt) putStream(root, "altitude", altitude);
        if (hasVel) putStream(root, "velocity_smooth", velocity);
        if (hasHr) putStream(root, "heartrate", heartrate);
        if (hasCad) putStream(root, "cadence", cadence);
        if (hasWatts) putStream(root, "watts", watts);
        return root.toString();
    }

    private void putStream(ObjectNode root, String key, ArrayNode data) {
        ObjectNode stream = MAPPER.createObjectNode();
        stream.set("data", data);
        stream.put("series_type", "distance");
        stream.put("original_size", data.size());
        stream.put("resolution", "high");
        root.set(key, stream);
    }

    // ── Laps ─────────────────────────────────────────────────────────────────────

    private String buildLaps(List<LapMesg> laps, List<RecordMesg> records) {
        long[] recTimes = new long[records.size()];
        for (int i = 0; i < records.size(); i++) {
            recTimes[i] = records.get(i).getTimestamp() != null ? records.get(i).getTimestamp().getTimestamp() : 0L;
        }

        ArrayNode arr = MAPPER.createArrayNode();
        int lapIndex = 1;
        for (LapMesg lap : laps) {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("lap_index", lapIndex++);
            node.put("distance", orZero(lap.getTotalDistance()));
            putIntOrNull(node, "moving_time", seconds(firstNonNull(lap.getTotalMovingTime(), lap.getTotalTimerTime(), lap.getTotalElapsedTime())));
            putDoubleOrNull(node, "total_elevation_gain", lap.getTotalAscent() != null ? lap.getTotalAscent().doubleValue() : null);
            putDoubleOrNull(node, "average_speed", toDouble(firstNonNull(lap.getEnhancedAvgSpeed(), lap.getAvgSpeed())));
            putDoubleOrNull(node, "max_speed", toDouble(lap.getMaxSpeed()));
            putDoubleOrNull(node, "average_heartrate", toDouble(lap.getAvgHeartRate()));
            putDoubleOrNull(node, "max_heartrate", toDouble(lap.getMaxHeartRate()));
            putDoubleOrNull(node, "average_cadence", toDouble(lap.getAvgCadence()));
            putDoubleOrNull(node, "average_watts", toDouble(lap.getAvgPower()));

            int[] range = lapRange(lap, recTimes);
            node.put("start_index", range[0]);
            node.put("end_index", range[1]);
            arr.add(node);
        }
        return arr.toString();
    }

    /** Maps a lap to [start_index, end_index] into the record array by timestamp. */
    private int[] lapRange(LapMesg lap, long[] recTimes) {
        if (recTimes.length == 0 || lap.getStartTime() == null) return new int[]{-1, -1};
        long lapStart = lap.getStartTime().getTimestamp();
        long lapEnd = lapStart + (lap.getTotalElapsedTime() != null ? Math.round(lap.getTotalElapsedTime()) : 0);
        int start = -1, end = -1;
        for (int i = 0; i < recTimes.length; i++) {
            if (start == -1 && recTimes[i] >= lapStart) start = i;
            if (recTimes[i] <= lapEnd) end = i;
        }
        if (start == -1) start = 0;
        if (end < start) end = start;
        return new int[]{start, end};
    }

    // ── activity_raw (fields read by ActivityService.buildTrainerRideDto) ─────────

    private String buildActivityRaw(SessionMesg session) {
        ObjectNode node = MAPPER.createObjectNode();
        putDoubleOrNull(node, "max_watts", toDouble(session.getMaxPower()));
        putDoubleOrNull(node, "max_heartrate", toDouble(session.getMaxHeartRate()));
        putDoubleOrNull(node, "average_heartrate", toDouble(session.getAvgHeartRate()));
        node.put("description", "");
        return node.toString();
    }

    // ── Map polyline ─────────────────────────────────────────────────────────────

    private String buildPolyline(List<RecordMesg> records) {
        List<double[]> points = new ArrayList<>();
        for (RecordMesg r : records) {
            Integer lat = r.getPositionLat();
            Integer lng = r.getPositionLong();
            if (lat != null && lng != null) {
                points.add(new double[]{lat * SEMICIRCLE_TO_DEG, lng * SEMICIRCLE_TO_DEG});
            }
        }
        return points.isEmpty() ? null : PolylineEncoder.encode(points);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    private boolean addFloat(ArrayNode arr, Float v) {
        if (v == null) { arr.addNull(); return false; }
        arr.add(Math.round(v * 100.0) / 100.0);
        return true;
    }

    private boolean addShort(ArrayNode arr, Short v) {
        if (v == null) { arr.addNull(); return false; }
        arr.add(v.intValue());
        return true;
    }

    private boolean addInt(ArrayNode arr, Integer v) {
        if (v == null) { arr.addNull(); return false; }
        arr.add(v.intValue());
        return true;
    }

    private void putIntOrNull(ObjectNode node, String field, Integer v) {
        if (v == null) node.putNull(field); else node.put(field, v);
    }

    private void putDoubleOrNull(ObjectNode node, String field, Double v) {
        if (v == null) node.putNull(field); else node.put(field, v);
    }

    @SafeVarargs
    private static <T> T firstNonNull(T... values) {
        for (T v : values) if (v != null) return v;
        return null;
    }

    private static Double toDouble(Number n) {
        return n != null ? n.doubleValue() : null;
    }

    private static double orZero(Float f) {
        return f != null ? f : 0.0;
    }

    private static int seconds(Float f) {
        return f != null ? Math.round(f) : 0;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String lower = s.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public static class FitParseException extends RuntimeException {
        public FitParseException(String message) { super(message); }
        public FitParseException(String message, Throwable cause) { super(message, cause); }
    }
}
