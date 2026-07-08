package xyz.zlov.app.strava.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class McpAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${app.mcp.auth-token:}")
    private String expectedToken;

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

        if (expectedToken.isBlank() || presentedToken == null || !constantTimeEquals(expectedToken, presentedToken)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
