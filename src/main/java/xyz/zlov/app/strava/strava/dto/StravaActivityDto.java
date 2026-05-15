package xyz.zlov.app.strava.strava.dto;

import lombok.Data;

@Data
public class StravaActivityDto {
    private Long id;
    private String name;
    private String type;
    private String sport_type;
    private String start_date;
    private Double distance;
    private Integer moving_time;
    private Integer elapsed_time;
    private Double total_elevation_gain;
    private Double average_speed;
    private Double max_speed;
    private Double average_heartrate;
    private Double max_heartrate;
    private Double average_cadence;
    private Double average_watts;
    private Boolean trainer;
    private Boolean commute;
    private String description;
    private AthleteRef athlete;

    @Data
    public static class AthleteRef {
        private Long id;
    }
}
