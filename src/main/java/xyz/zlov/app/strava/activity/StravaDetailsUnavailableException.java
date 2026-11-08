package xyz.zlov.app.strava.activity;

/** Activity details are not cached and Strava API refuses to serve them (app is inactive). */
public class StravaDetailsUnavailableException extends RuntimeException {

    public StravaDetailsUnavailableException(Long stravaId, Throwable cause) {
        super("Details of activity " + stravaId + " are not stored and cannot be fetched: Strava API access is "
                + "disabled for this app (403 Inactive). Import the activity's FIT file on the /import page "
                + "(file name = Strava activity id, e.g. " + stravaId + ".fit) and retry.", cause);
    }
}
