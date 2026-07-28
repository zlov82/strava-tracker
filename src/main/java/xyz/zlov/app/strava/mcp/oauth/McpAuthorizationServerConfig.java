package xyz.zlov.app.strava.mcp.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.time.Duration;
import java.util.UUID;

/**
 * OAuth 2.1 authorization server for the Claude Connectors "OAuth Client ID/Secret" flow —
 * additive to the static bearer-token path in {@link xyz.zlov.app.strava.config.McpAuthFilter}.
 * <p>
 * Only activates once a client secret and login password have actually been configured
 * ({@code MCP_OAUTH_CLIENT_SECRET} / {@code MCP_OAUTH_LOGIN_PASSWORD}). Without them, this whole
 * class is skipped so the app still starts cleanly and {@code /mcp} keeps working via the static
 * token — docker-compose always passes these env vars through (blank if unset in the shell),
 * which would otherwise make {@link RegisteredClient.Builder#build()} fail fast on a blank
 * clientId/secret at context startup.
 */
@Configuration
@ConditionalOnExpression("!'${app.mcp.oauth.client-secret:}'.isEmpty() and !'${app.mcp.oauth.login-password:}'.isEmpty()")
public class McpAuthorizationServerConfig {

    @Value("${app.mcp.oauth.issuer}")
    private String issuer;

    @Value("${app.mcp.oauth.client-id}")
    private String clientId;

    @Value("${app.mcp.oauth.client-secret}")
    private String clientSecret;

    @Value("${app.mcp.oauth.redirect-uri}")
    private String redirectUri;

    @Value("${app.mcp.oauth.login-username}")
    private String loginUsername;

    @Value("${app.mcp.oauth.login-password}")
    private String loginPassword;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .oauth2AuthorizationServer(authorizationServer ->
                http.securityMatcher(authorizationServer.getEndpointsMatcher()))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient claudeClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scope("mcp")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(true)
                        .build())
                .build();

        return new InMemoryRegisteredClientRepository(claudeClient);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        UserDetails owner = User.withUsername(loginUsername)
                .password(passwordEncoder.encode(loginPassword))
                .roles("OWNER")
                .build();
        return new InMemoryUserDetailsManager(owner);
    }
}
