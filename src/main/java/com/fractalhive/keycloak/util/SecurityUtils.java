package com.fractalhive.keycloak.util;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Utility class for security-related operations.
 */
public final class SecurityUtils {
	
	private static final Logger logger = LoggerFactory.getLogger(SecurityUtils.class);
    private static final String PREFERRED_USERNAME_CLAIM = "preferred_username";

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
    
    public static String getCurrentUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth == null) {
                logger.debug("No authentication found in SecurityContext, returning 'system_setted'");
                return "system_setted";
            }

            if (auth instanceof JwtAuthenticationToken jwtAuth) {
                Jwt jwt = jwtAuth.getToken();
                
                // Try preferred_username first (Keycloak standard)
                String username = jwt.getClaimAsString(PREFERRED_USERNAME_CLAIM);
                if (username != null && !username.trim().isEmpty()) {
                    return username.trim();
                }
            } else {
                // Not a JWT token, try to get name from authentication
                if (auth.getName() != null && !auth.getName().trim().isEmpty()) {
                    return auth.getName().trim();
                }
            }

            logger.warn("Could not extract username from authentication, returning 'system'");
            return "system_setted";

        } catch (Exception e) {
            logger.error("Error extracting username from SecurityContext", e);
            return "system_setted";
        }
    }
}
