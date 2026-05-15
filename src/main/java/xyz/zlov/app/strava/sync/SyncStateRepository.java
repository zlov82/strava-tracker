package xyz.zlov.app.strava.sync;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SyncStateRepository extends JpaRepository<SyncState, Long> {
    Optional<SyncState> findByAthleteId(Long athleteId);
}
