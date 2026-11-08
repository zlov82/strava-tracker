package xyz.zlov.app.strava.activity.dto;

/** Output rounding: averages come from different sources with different precision (125.0, 125.7, 85.8233...). */
public final class Rounding {

    private Rounding() {}

    /** Rounds to one decimal place; null stays null. */
    public static Double tenths(Double v) {
        return v == null ? null : Math.round(v * 10.0) / 10.0;
    }
}
