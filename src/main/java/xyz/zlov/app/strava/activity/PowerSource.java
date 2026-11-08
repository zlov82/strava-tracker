package xyz.zlov.app.strava.activity;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tells real power (power meter or smart trainer) from power Strava estimates for outdoor rides.
 * Real = done on a trainer, or flagged {@code device_watts} in activity_raw (set by Strava, and by FIT import
 * whenever the file carries power). Unknown (no activity_raw) counts as not real.
 */
public final class PowerSource {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PowerSource() {}

    public static boolean isReal(Activity activity) {
        if (Boolean.TRUE.equals(activity.getTrainer())) return true;
        if (activity.getActivityRaw() == null) return false;
        try {
            return MAPPER.readTree(activity.getActivityRaw()).path("device_watts").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }
}
