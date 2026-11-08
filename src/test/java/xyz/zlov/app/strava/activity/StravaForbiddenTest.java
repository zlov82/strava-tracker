package xyz.zlov.app.strava.activity;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import xyz.zlov.app.strava.strava.StravaClient;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StravaForbiddenTest {

    private final ActivityRepository repo = mock(ActivityRepository.class);
    private final StravaClient client = mock(StravaClient.class);
    private final ActivityService service = new ActivityService(repo, client);

    private Activity summaryOnly(long id) {
        Activity a = new Activity();
        a.setStravaId(id);
        when(repo.findByStravaId(id)).thenReturn(Optional.of(a));
        return a;
    }

    @Test
    void forbiddenFromStravaBecomesActionableMessage() {
        summaryOnly(20752488934L);
        when(client.getActivityRaw(anyLong())).thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8));

        StravaDetailsUnavailableException e = assertThrows(StravaDetailsUnavailableException.class,
                () -> service.fetchAndCacheDetails(20752488934L));
        assertTrue(e.getMessage().contains("20752488934.fit"), e.getMessage());
        assertTrue(e.getMessage().contains("/import"), e.getMessage());
    }

    @Test
    void forbiddenOnStreamsIsAlsoTranslated() {
        Activity a = summaryOnly(1L);
        a.setActivityRaw("{}");
        when(client.getActivityStreamsRaw(anyLong())).thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8));

        assertThrows(StravaDetailsUnavailableException.class, () -> service.fetchAndCacheStreams(1L));
    }

    @Test
    void otherStravaErrorsAreNotSwallowed() {
        summaryOnly(2L);
        when(client.getActivityRaw(anyLong())).thenThrow(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "boom", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8));

        assertThrows(HttpServerErrorException.class, () -> service.fetchAndCacheDetails(2L));
    }

    @Test
    void cachedDetailsNeverCallStrava() {
        Activity a = summaryOnly(3L);
        a.setActivityRaw("{}");
        assertTrue(service.fetchAndCacheDetails(3L).isPresent());
    }
}
