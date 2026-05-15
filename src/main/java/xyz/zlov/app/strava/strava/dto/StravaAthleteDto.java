package xyz.zlov.app.strava.strava.dto;

import lombok.Data;

@Data
public class StravaAthleteDto {
    private Long id;
    private String firstname;
    private String lastname;
    private String profile_medium;
}
