package com.fractalhive.keycloak.filter;

import com.fractalhive.keycloak.config.JwtCookieAuthenticationFilter;
import com.fractalhive.keycloak.exception.InvalidTokenException;
import com.fractalhive.keycloak.exception.KeycloakExceptionHandler;
import com.fractalhive.keycloak.exception.MissingTokenException;
import com.fractalhive.keycloak.exception.TokenExpiredException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Keycloak authentication filter for workflow engine integration.
 * Validates JWT tokens and sets request attributes (roles, schemaName) for downstream filters.
 * <p>
 * This filter runs with Order(-1) to execute before workflow engine filters:
 * - AdminAuthorizationFilter (Order: 0)
 * - TenantFilter (Order: 1)
 * </p>
 * <p>
 * This filter is conditionally enabled via property:
 * {@code fractalhive.keycloak.workflow.enabled=true}
 * </p>
 */
@Slf4j
@Component
@Order(-1)
@ConditionalOnProperty(
        prefix = "fractalhive.keycloak.workflow",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
@RequiredArgsConstructor
public class KeycloakWorkflowFilter extends OncePerRequestFilter {

    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String SCHEMA_NAME_ATTRIBUTE = "schemaName";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtDecoder jwtDecoder;
    private final KeycloakExceptionHandler exceptionHandler;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip authentication for public endpoints
        if (shouldSkipAuthentication(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            // Extract JWT token from Authorization header or cookies
            String token = extractToken(request);
            
            if (token == null) {
                throw new MissingTokenException();
            }

            // Validate and decode JWT token (throws exception if invalid)
            Jwt jwt = validateToken(token);

            // Extract and set roles attribute
            List<String> roles = extractRoles(jwt);
            if (roles != null && !roles.isEmpty()) {
                request.setAttribute(ROLES_ATTRIBUTE, roles);
            }

            // Extract and set schema name attribute (only if present)
            String schema = extractSchema(jwt);
            if (schema != null && !schema.trim().isEmpty()) {
                request.setAttribute(SCHEMA_NAME_ATTRIBUTE, schema.trim());
            }

            // Continue filter chain
            filterChain.doFilter(request, response);

        } catch (MissingTokenException | InvalidTokenException | TokenExpiredException e) {
            // Handle custom Keycloak exceptions
            exceptionHandler.handleFilterException(e, request, response);
        } catch (JwtException e) {
            // Handle Spring Security JWT exceptions
            log.debug("JWT validation error: {}", e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("expired") || e.getMessage().contains("Expired"))) {
                exceptionHandler.handleFilterException(
                    new TokenExpiredException("Token expired", e), request, response
                );
            } else {
                exceptionHandler.handleFilterException(
                    new InvalidTokenException("Invalid token: " + e.getMessage(), e), request, response
                );
            }
        } catch (Exception e) {
            // Handle unexpected errors
            log.error("Unexpected error processing Keycloak authentication", e);
            exceptionHandler.handleFilterException(e, request, response);
        }
    }

    /**
     * Checks if authentication should be skipped for this request.
     */
    private boolean shouldSkipAuthentication(HttpServletRequest request) {
        String path = request.getRequestURI();
        
        // Skip for public registration endpoint
        if (path.equals("/api/applications/register")) {
            return true;
        }
        
        // Skip for health check endpoints
        if (path.startsWith("/actuator/health")) {
            return true;
        }
        
        // Skip for Swagger/OpenAPI endpoints
        if (path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs")) {
            return true;
        }
        
        return false;
    }

    /**
     * Extracts JWT token from Authorization header or cookies.
     */
    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }

        // Fallback to cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (JwtCookieAuthenticationFilter.ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * Validates JWT token and returns Jwt object.
     * @throws TokenExpiredException if token is expired
     * @throws InvalidTokenException if token is invalid
     */
    private Jwt validateToken(String token) {
        try {
            return jwtDecoder.decode(token);
        } catch (JwtException e) {
            // Re-throw to be handled by exception handler
            String message = e.getMessage();
            if (message != null && (message.contains("expired") || message.contains("Expired"))) {
                throw new TokenExpiredException("Token expired", e);
            }
            throw new InvalidTokenException("Token validation failed: " + message, e);
        }
    }

    /**
     * Extracts roles from JWT token.
     * Supports both "roles" and "realm_access.roles" claims.
     */
    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        List<String> roles = new ArrayList<>();

        // Try "roles" claim first
        Object rolesClaim = jwt.getClaim("roles");
        if (rolesClaim != null) {
            if (rolesClaim instanceof List) {
                roles.addAll((List<String>) rolesClaim);
            } else if (rolesClaim instanceof String) {
                roles.add((String) rolesClaim);
            }
        }

        // Try "realm_access.roles" claim
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object realmRolesObj = realmAccess.get("roles");
            if (realmRolesObj instanceof List) {
                List<String> realmRoles = (List<String>) realmRolesObj;
                roles.addAll(realmRoles);
            }
        }

        return roles.isEmpty() ? null : roles;
    }

    /**
     * Extracts schema name from JWT token.
     */
    private String extractSchema(Jwt jwt) {
        return jwt.getClaimAsString("schema");
    }
}
