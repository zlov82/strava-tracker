package xyz.zlov.app.strava.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Accepts either the static shared-secret bearer token (CLI/mcp-remote use) or a valid
 * JWT access token issued by {@link xyz.zlov.app.strava.mcp.oauth.McpAuthorizationServerConfig}
 * (Claude Connectors OAuth flow).
 */
@Component
public class McpAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.mcp.auth-token:}")
    private String expectedToken;

    private final JwtDecoder jwtDecoder;

    public McpAuthFilter(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/mcp")) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader("Authorization");
        String presentedToken = header != null && header.startsWith(BEARER_PREFIX)
                ? header.substring(BEARER_PREFIX.length())
                : null;

        if (presentedToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (!expectedToken.isBlank() && constantTimeEquals(expectedToken, presentedToken)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            var jwt = jwtDecoder.decode(presentedToken);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(
                    jwt.getSubject(), null, List.of(new SimpleGrantedAuthority("SCOPE_mcp")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } catch (JwtException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
