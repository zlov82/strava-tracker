package xyz.zlov.app.strava.sync;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "sync_state")
@Data
@NoArgsConstructor
public class SyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "athlete_id", nullable = false, unique = true)
    private Long athleteId;

    @Column(name = "last_sync_at", nullable = false)
    private Long lastSyncAt = 0L;

    @Column(nullable = false)
    private String status = "IDLE";

    private String error;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
