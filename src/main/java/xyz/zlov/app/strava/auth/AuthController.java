package xyz.zlov.app.strava.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import xyz.zlov.app.strava.auth.dto.StravaTokenResponse;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    @Value("${strava.client-id}")
    private String clientId;

    @Value("${strava.client-secret}")
    private String clientSecret;

    @Value("${strava.redirect-uri}")
    private String redirectUri;

    private final TokenService tokenService;
    private final RestClient restClient;

    @GetMapping("/strava")
    public String redirectToStrava() {
        String url = UriComponentsBuilder
                .fromUriString("https://www.strava.com/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "activity:read_all")
                .toUriString();
        return "redirect:" + url;
    }

    @GetMapping("/callback")
    public String handleCallback(@RequestParam String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("code", code);
        params.add("grant_type", "authorization_code");

        StravaTokenResponse response = restClient.post()
                .uri("https://www.strava.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(StravaTokenResponse.class);

        tokenService.saveToken(response);
        return "redirect:/auth/success";
    }

    @GetMapping("/success")
    @ResponseBody
    public String success() {
        return "Authorization successful! Token saved to DB.";
    }
}
