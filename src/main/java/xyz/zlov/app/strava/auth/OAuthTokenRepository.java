package xyz.zlov.app.strava.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
    Optional<OAuthToken> findByAthleteId(Long athleteId);
    Optional<OAuthToken> findFirstByOrderByUpdatedAtDesc();
}
