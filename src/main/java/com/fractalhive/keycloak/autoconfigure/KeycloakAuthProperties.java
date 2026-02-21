package com.fractalhive.keycloak.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Keycloak authentication.
 * <p>
 * Multiple applications can use the same Keycloak server and realm,
 * but each MUST have a unique client-id and client-secret.
 * </p>
 *
 * @see ConfigurationProperties
 */
@Data
@ConfigurationProperties(prefix = "fractalhive.keycloak")
public class KeycloakAuthProperties {

    /**
     * Base Keycloak server URL.
     * Property: {@code fractalhive.keycloak.server-url}
     */
    private String serverUrl;

    /**
     * Realm name in Keycloak.
     * Property: {@code fractalhive.keycloak.realm}
     */
    private String realm;

    /**
     * Client ID for this application. MUST be unique per application.
     * Property: {@code fractalhive.keycloak.client-id}
     */
    private String clientId;

    /**
     * Client secret for this application. MUST be unique per application.
     * Property: {@code fractalhive.keycloak.client-secret}
     */
    private String clientSecret;

    /**
     * Resource ID for role extraction from JWT tokens.
     * Defaults to client-id if not specified.
     * Property: {@code fractalhive.keycloak.resource-id}
     */
    private String resourceId;

    /**
     * Cookie configuration for JWT token storage.
     * Property prefix: {@code fractalhive.keycloak.cookie.*}
     */
    private Cookie cookie = new Cookie();

    /**
     * Admin client configuration for role/user management operations.
     * If not specified, uses the main client-id and client-secret.
     * Property prefix: {@code fractalhive.keycloak.admin.*}
     */
    private Admin admin = new Admin();

    /**
     * Public endpoints that don't require authentication.
     * Property: {@code fractalhive.keycloak.public-endpoints}
     */
    private String[] publicEndpoints = {
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/refresh",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    /**
     * Admin realm for administrative operations.
     * If not specified, uses the same realm as the target realm.
     * Property: {@code fractalhive.keycloak.admin-realm}
     */
    private String adminRealm;

    /**
     * Cookie configuration for authentication tokens.
     */
    @Data
    public static class Cookie {
        /**
         * Enable secure cookies (HTTPS only).
         * Default: {@code true}
         * Property: {@code fractalhive.keycloak.cookie.secure}
         */
        private boolean secure = true;

        /**
         * Cookie domain for cross-subdomain sharing.
         * Example: {@code .example.com}
         * Property: {@code fractalhive.keycloak.cookie.domain}
         */
        private String domain;

        /**
         * SameSite cookie policy (None, Lax, Strict).
         * Default: {@code None}
         * Property: {@code fractalhive.keycloak.cookie.same-site}
         */
        private String sameSite = "None";
    }

    /**
     * Admin client configuration for administrative operations.
     */
    @Data
    public static class Admin {
        /**
         * Admin client ID for role/user management.
         * Defaults to main client-id if not specified.
         * Property: {@code fractalhive.keycloak.admin.client-id}
         */
        private String clientId;

        /**
         * Admin client secret.
         * Defaults to main client-secret if not specified.
         * Property: {@code fractalhive.keycloak.admin.client-secret}
         */
        private String clientSecret;
    }

    /**
     * Get the Keycloak authentication URL.
     */
    public String getAuthUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect";
    }

    /**
     * Get the Keycloak admin API URL for the target realm.
     */
    public String getAdminUrl() {
        return serverUrl + "/admin/realms/" + realm;
    }

    /**
     * Get the Keycloak admin authentication URL.
     * Uses admin realm if specified, otherwise uses the target realm.
     */
    public String getAdminAuthUrl() {
        String realmForAuth = adminRealm != null && !adminRealm.isEmpty() ? adminRealm : realm;
        return serverUrl + "/realms/" + realmForAuth + "/protocol/openid-connect";
    }

    /**
     * Get the JWT issuer URI for OAuth2 Resource Server.
     */
    public String getJwtIssuerUri() {
        return serverUrl + "/realms/" + realm;
    }
}
