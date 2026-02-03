package com.fractalhive.keycloak.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

/**
 * Utility class for security-related operations.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Get the current authenticated principal (user ID).
     */
    public static Optional<String> getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }

        return Optional.ofNullable(auth.getName());
    }

    /**
     * Get the current JWT token.
     */
    public static Optional<Jwt> getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof Jwt)) {
            return Optional.empty();
        }

        return Optional.of((Jwt) auth.getPrincipal());
    }

    /**
     * Get the current user ID from JWT.
     */
    public static Optional<String> getCurrentUserId() {
        return getJwt()
                .map(jwt -> jwt.getSubject());
    }
}
