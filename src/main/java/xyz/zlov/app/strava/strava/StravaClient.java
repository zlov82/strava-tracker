package xyz.zlov.app.strava.strava;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import xyz.zlov.app.strava.auth.TokenService;
import xyz.zlov.app.strava.strava.dto.StravaActivityDto;
import xyz.zlov.app.strava.strava.dto.StravaAthleteDto;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StravaClient {

    private static final String BASE_URL = "https://www.strava.com/api/v3";

    private final RestClient restClient;
    private final TokenService tokenService;

    public StravaAthleteDto getAthlete() {
        return restClient.get()
                .uri(BASE_URL + "/athlete")
                .header("Authorization", "Bearer " + tokenService.getValidAccessToken())
                .retrieve()
                .body(StravaAthleteDto.class);
    }

    public StravaActivityDto getActivity(long id) {
        return restClient.get()
                .uri(BASE_URL + "/activities/{id}", id)
                .header("Authorization", "Bearer " + tokenService.getValidAccessToken())
                .retrieve()
                .body(StravaActivityDto.class);
    }

    public List<StravaActivityDto> getActivities(long after, int page) {
        try {
            return restClient.get()
                    .uri(BASE_URL + "/athlete/activities?after={after}&page={page}&per_page=50",
                            after, page)
                    .header("Authorization", "Bearer " + tokenService.getValidAccessToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Strava rate limit hit, waiting 60s...");
                try {
                    Thread.sleep(60_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                return getActivities(after, page);
            }
            throw e;
        }
    }
}
