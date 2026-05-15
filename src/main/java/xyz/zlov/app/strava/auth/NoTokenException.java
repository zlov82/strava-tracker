package xyz.zlov.app.strava.auth;

public class NoTokenException extends RuntimeException {
    public NoTokenException() {
        super("No OAuth token. Authorize at /auth/strava");
    }
}
