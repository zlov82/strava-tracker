package xyz.zlov.app.strava.auth.dto;

import lombok.Data;

@Data
public class StravaTokenResponse {
    private String access_token;
    private String refresh_token;
    private Long expires_at;
    private AthleteInfo athlete;

    @Data
    public static class AthleteInfo {
        private Long id;
    }
}
