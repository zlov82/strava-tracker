package xyz.zlov.app.strava.activity;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tells real power (power meter or smart trainer) from power Strava estimates for outdoor rides.
 *
 * <p>{@link #hasDeviceWatts} is three-valued:
 * <ul>
 *   <li>{@code false}: the activity has no power at all, or it is known to be estimated;</li>
 *   <li>{@code true}: there is power and it comes from a device: a trainer ride, a VirtualRide
 *       (indoor platforms such as MyWhoosh/Zwift always feed device power), or {@code device_watts=true}
 *       in activity_raw (set by Strava and by FIT import when the file carries power);</li>
 *   <li>{@code null}: there is power but it is unknown where it comes from (outdoor ride whose
 *       details were never stored).</li>
 * </ul>
 */
public final class PowerSource {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private PowerSource() {}

    public static Boolean hasDeviceWatts(Activity activity) {
        if (!hasPower(activity)) return false;
        if (Boolean.TRUE.equals(activity.getTrainer()) || isVirtualRide(activity)) return true;
        if (activity.getActivityRaw() == null) return null;
        try {
            return MAPPER.readTree(activity.getActivityRaw()).path("device_watts").asBoolean(false);
        } catch (Exception e) {
            return false;
        }
    }

    /** True only when power is known to be real; unknown counts as not real. */
    public static boolean isReal(Activity activity) {
        return Boolean.TRUE.equals(hasDeviceWatts(activity));
    }

    private static boolean hasPower(Activity a) {
        if (a.getAverageWatts() != null) return true;
        return a.getStreamsRaw() != null && a.getStreamsRaw().contains("\"watts\"");
    }

    private static boolean isVirtualRide(Activity a) {
        String sport = a.getSportType() != null ? a.getSportType() : a.getType();
        return "VirtualRide".equalsIgnoreCase(sport);
    }
}
