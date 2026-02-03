package com.fractalhive.keycloak.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Keycloak authentication.
 * <p>
 * Each Spring Boot application binds its own properties at startup.
 * All properties are instance variables, ensuring complete isolation between applications.
 * </p>
 * <p>
 * <b>Multi-Client Architecture:</b> Multiple applications can use the same Keycloak server and realm,
 * but each MUST have a unique client-id and client-secret. This allows centralized authentication
 * while maintaining application isolation.
 * </p>
 * <p>
 * <b>Example Configuration:</b>
 * <pre>
 * fractalhive.keycloak.server-url=https://keycloak.example.com
 * fractalhive.keycloak.realm=my-realm
 * fractalhive.keycloak.client-id=product-abc
 * fractalhive.keycloak.client-secret=abc-secret-123
 * fractalhive.keycloak.resource-id=product-abc
 * </pre>
 * </p>
 */
@Data
@ConfigurationProperties(prefix = "fractalhive.keycloak")
public class KeycloakAuthProperties {

    /**
     * Base Keycloak server URL.
     * <p>
     * This can be shared across multiple applications. All applications typically point to the same
     * Keycloak server instance.
     * </p>
     * <p>
     * <b>Example:</b> {@code https://keycloak.example.com} or {@code https://fhkeycloakazure.azurewebsites.net}
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.server-url}
     * </p>
     * <p>
     * <b>Required:</b> Yes
     * </p>
     */
    private String serverUrl;

    /**
     * Realm name in Keycloak.
     * <p>
     * All applications can use the same realm, or each can have its own realm depending on your
     * organization's structure. Using the same realm allows user sharing across applications.
     * </p>
     * <p>
     * <b>Example:</b> {@code fractalhive} or {@code my-company-realm}
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.realm}
     * </p>
     * <p>
     * <b>Required:</b> Yes
     * </p>
     */
    private String realm;

    /**
     * Client ID for this application.
     * <p>
     * <b>CRITICAL:</b> This MUST be unique per application. Each product/client needs its own
     * Keycloak client configured in the Keycloak Admin Console.
     * </p>
     * <p>
     * When adding a new product:
     * <ol>
     *   <li>Create a new client in Keycloak Admin Console</li>
     *   <li>Set the client ID (e.g., "product-xyz")</li>
     *   <li>Copy the client ID here</li>
     * </ol>
     * </p>
     * <p>
     * <b>Example:</b> {@code product-abc}, {@code product-xyz}, {@code admin-portal}
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.client-id}
     * </p>
     * <p>
     * <b>Required:</b> Yes
     * </p>
     */
    private String clientId;

    /**
     * Client secret for this application.
     * <p>
     * <b>CRITICAL:</b> This MUST be unique per application and matches the secret configured
     * in Keycloak for the client specified by {@code client-id}.
     * </p>
     * <p>
     * When adding a new product:
     * <ol>
     *   <li>After creating the client in Keycloak, generate or copy the client secret</li>
     *   <li>Paste the secret here</li>
     *   <li>Keep this secret secure - never commit to version control</li>
     * </ol>
     * </p>
     * <p>
     * <b>Example:</b> {@code abc-secret-123}, {@code xyz-secret-456}
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.client-secret}
     * </p>
     * <p>
     * <b>Required:</b> Yes
     * </p>
     */
    private String clientSecret;

    /**
     * Resource ID for role extraction from JWT tokens.
     * <p>
     * This is used to extract client-scoped roles from the JWT token's {@code resource_access} claim.
     * Typically, this should match your {@code client-id}.
     * </p>
     * <p>
     * If not specified, defaults to {@code client-id}.
     * </p>
     * <p>
     * <b>Example:</b> {@code product-abc} (same as client-id)
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.resource-id}
     * </p>
     * <p>
     * <b>Required:</b> No (defaults to client-id)
     * </p>
     */
    private String resourceId;

    /**
     * Cookie configuration for JWT token storage.
     * <p>
     * Configures how authentication tokens are stored in HTTP-only cookies.
     * </p>
     * <p>
     * <b>Property prefix:</b> {@code fractalhive.keycloak.cookie.*}
     * </p>
     */
    private Cookie cookie = new Cookie();

    /**
     * Admin client configuration for role/user management operations.
     * <p>
     * Used for administrative operations like creating users, assigning roles, etc.
     * If not specified, uses the main {@code client-id} and {@code client-secret}.
     * </p>
     * <p>
     * <b>Property prefix:</b> {@code fractalhive.keycloak.admin.*}
     * </p>
     * <p>
     * <b>Required:</b> No (defaults to main client credentials)
     * </p>
     */
    private Admin admin = new Admin();

    /**
     * Public endpoints that don't require authentication.
     * <p>
     * These endpoints are accessible without a valid JWT token. Default includes
     * authentication endpoints and Swagger UI.
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.public-endpoints}
     * </p>
     * <p>
     * <b>Example:</b> {@code /auth/login,/auth/register,/public/**}
     * </p>
     * <p>
     * <b>Required:</b> No (has sensible defaults)
     * </p>
     */
    private String[] publicEndpoints = {
            "/auth/login",
            "/auth/register",
            "/auth/refresh",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    /**
     * Cookie configuration for authentication tokens.
     */
    @Data
    public static class Cookie {
        /**
         * Enable secure cookies (HTTPS only).
         * <p>
         * Set to {@code true} in production to ensure cookies are only sent over HTTPS.
         * Set to {@code false} for local development with HTTP.
         * </p>
         * <p>
         * <b>Property:</b> {@code fractalhive.keycloak.cookie.secure}
         * </p>
         * <p>
         * <b>Default:</b> {@code true}
         * </p>
         */
        private boolean secure = true;

        /**
         * Cookie domain for cross-subdomain sharing.
         * <p>
         * If your application spans multiple subdomains (e.g., app.example.com, api.example.com),
         * set this to {@code .example.com} to share cookies across subdomains.
         * </p>
         * <p>
         * <b>Property:</b> {@code fractalhive.keycloak.cookie.domain}
         * </p>
         * <p>
         * <b>Example:</b> {@code .example.com} (note the leading dot)
         * </p>
         * <p>
         * <b>Required:</b> No
         * </p>
         */
        private String domain;

        /**
         * SameSite cookie policy.
         * <p>
         * Controls when cookies are sent with cross-site requests.
         * </p>
         * <ul>
         *   <li>{@code None} - Cookies sent with all requests (requires secure=true)</li>
         *   <li>{@code Lax} - Cookies sent with top-level navigation (default for most browsers)</li>
         *   <li>{@code Strict} - Cookies only sent for same-site requests</li>
         * </ul>
         * <p>
         * <b>Property:</b> {@code fractalhive.keycloak.cookie.same-site}
         * </p>
         * <p>
         * <b>Default:</b> {@code None}
         * </p>
         */
        private String sameSite = "None";
    }

    /**
     * Admin realm for administrative operations.
     * <p>
     * The realm used for obtaining admin access tokens. If not specified, uses the same realm as
     * the target realm ({@code realm}). This means admin authentication and operations both happen
     * in your target realm. Set this to "master" only if you specifically need master realm authentication.
     * </p>
     * <p>
     * <b>Property:</b> {@code fractalhive.keycloak.admin-realm}
     * </p>
     * <p>
     * <b>Default:</b> Same as {@code realm} (uses your target realm)
     * </p>
     * <p>
     * <b>Required:</b> No
     * </p>
     */
    private String adminRealm;

    /**
     * Admin client configuration for administrative operations.
     */
    @Data
    public static class Admin {
        /**
         * Admin client ID for role/user management.
         * <p>
         * Used for operations that require admin privileges (creating users, managing roles, etc.).
         * If not specified, uses the main {@code client-id}.
         * </p>
         * <p>
         * <b>Property:</b> {@code fractalhive.keycloak.admin.client-id}
         * </p>
         * <p>
         * <b>Example:</b> {@code admin-cli} or {@code realm-management}
         * </p>
         * <p>
         * <b>Required:</b> No (defaults to main client-id)
         * </p>
         */
        private String clientId;

        /**
         * Admin client secret.
         * <p>
         * Secret for the admin client. If not specified, uses the main {@code client-secret}.
         * </p>
         * <p>
         * <b>Property:</b> {@code fractalhive.keycloak.admin.client-secret}
         * </p>
         * <p>
         * <b>Required:</b> No (defaults to main client-secret)
         * </p>
         */
        private String clientSecret;
    }

    /**
     * Get the Keycloak authentication URL
     */
    public String getAuthUrl() {
        return serverUrl + "/realms/" + realm + "/protocol/openid-connect";
    }

    /**
     * Get the Keycloak admin API URL for the target realm.
     * <p>
     * This URL is used for admin operations on the target realm (creating users, managing roles, etc.).
     * </p>
     * 
     * @return Admin API URL for the target realm
     */
    public String getAdminUrl() {
        return serverUrl + "/admin/realms/" + realm;
    }

    /**
     * Get the Keycloak admin authentication URL.
     * <p>
     * This URL is used to obtain admin access tokens. Uses the admin realm if specified,
     * otherwise uses the target realm. Both admin authentication and operations happen in
     * the same realm.
     * </p>
     * 
     * @return Admin authentication URL using the admin realm (or target realm if not specified)
     */
    public String getAdminAuthUrl() {
        String realmForAuth = adminRealm != null && !adminRealm.isEmpty() ? adminRealm : realm;
        return serverUrl + "/realms/" + realmForAuth + "/protocol/openid-connect";
    }

    /**
     * Get the JWT issuer URI for OAuth2 Resource Server
     */
    public String getJwtIssuerUri() {
        return serverUrl + "/realms/" + realm;
    }
}
