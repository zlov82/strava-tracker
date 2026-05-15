package xyz.zlov.app.strava.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import xyz.zlov.app.strava.auth.dto.StravaTokenResponse;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final OAuthTokenRepository tokenRepository;
    private final RestClient restClient;

    @Value("${strava.client-id}")
    private String clientId;

    @Value("${strava.client-secret}")
    private String clientSecret;

    public Long getAthleteId() {
        return tokenRepository.findFirstByOrderByUpdatedAtDesc()
                .map(OAuthToken::getAthleteId)
                .orElseThrow(NoTokenException::new);
    }

    public String getValidAccessToken() {
        OAuthToken token = tokenRepository.findFirstByOrderByUpdatedAtDesc()
                .orElseThrow(NoTokenException::new);
        if (isExpiredSoon(token)) {
            token = refresh(token);
        }
        return token.getAccessToken();
    }

    public OAuthToken saveToken(StravaTokenResponse response) {
        OAuthToken token = tokenRepository.findByAthleteId(response.getAthlete().getId())
                .orElseGet(OAuthToken::new);
        token.setAthleteId(response.getAthlete().getId());
        token.setAccessToken(response.getAccess_token());
        token.setRefreshToken(response.getRefresh_token());
        token.setExpiresAt(response.getExpires_at());
        return tokenRepository.save(token);
    }

    private boolean isExpiredSoon(OAuthToken token) {
        return Instant.now().getEpochSecond() >= token.getExpiresAt() - 300;
    }

    private OAuthToken refresh(OAuthToken token) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", token.getRefreshToken());
        params.add("grant_type", "refresh_token");

        StravaTokenResponse response = restClient.post()
                .uri("https://www.strava.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(StravaTokenResponse.class);

        token.setAccessToken(response.getAccess_token());
        token.setRefreshToken(response.getRefresh_token());
        token.setExpiresAt(response.getExpires_at());
        return tokenRepository.save(token);
    }
}
