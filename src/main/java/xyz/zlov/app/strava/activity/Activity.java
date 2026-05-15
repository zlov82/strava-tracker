package xyz.zlov.app.strava.activity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "activities")
@Data
@NoArgsConstructor
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "strava_id", nullable = false, unique = true)
    private Long stravaId;

    @Column(name = "athlete_id", nullable = false)
    private Long athleteId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String type;

    @Column(name = "sport_type")
    private String sportType;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(nullable = false)
    private Double distance = 0.0;

    @Column(name = "moving_time", nullable = false)
    private Integer movingTime = 0;

    @Column(name = "elapsed_time", nullable = false)
    private Integer elapsedTime = 0;

    @Column(name = "total_elevation_gain", nullable = false)
    private Double totalElevationGain = 0.0;

    @Column(name = "average_speed")
    private Double averageSpeed;

    @Column(name = "max_speed")
    private Double maxSpeed;

    @Column(name = "average_heartrate")
    private Double averageHeartrate;

    @Column(name = "max_heartrate")
    private Double maxHeartrate;

    @Column(name = "average_cadence")
    private Double averageCadence;

    @Column(name = "average_watts")
    private Double averageWatts;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String rawData;

    @Column(nullable = false)
    private Boolean trainer = false;

    @Column(nullable = false)
    private Boolean commute = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
