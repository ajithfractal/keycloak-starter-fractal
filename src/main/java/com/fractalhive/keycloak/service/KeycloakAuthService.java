package com.fractalhive.keycloak.service;

import com.fractalhive.keycloak.autoconfigure.KeycloakAuthProperties;
import com.fractalhive.keycloak.dto.LoginRequest;
import com.fractalhive.keycloak.dto.LoginResponse;
import com.fractalhive.keycloak.dto.UserInfoResponse;
import com.fractalhive.keycloak.exception.KeycloakAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Service for Keycloak authentication operations.
 */
@Service
@RequiredArgsConstructor
public class KeycloakAuthService {

    private final WebClient webClient;
    private final KeycloakAuthProperties properties;

    private String adminAccessToken;
    private Instant adminAccessTokenExpireTime;

    /**
     * Authenticate a user and return tokens.
     */
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            Map<String, Object> token = webClient
                    .post()
                    .uri(properties.getAuthUrl() + "/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("grant_type", "password")
                            .with("client_id", properties.getClientId())
                            .with("client_secret", properties.getClientSecret())
                            .with("username", loginRequest.email())
                            .with("password", loginRequest.password())
                    )
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            return getLoginResponseFromToken(token);

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Authentication failed. Please verify that 'fractalhive.keycloak.client-id' and " +
                    "'fractalhive.keycloak.client-secret' are correctly configured. Error: %s"
                            .formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Refresh access token using refresh token.
     */
    public LoginResponse refresh(String refreshToken) {
        try {
            Map<String, Object> token = webClient
                    .post()
                    .uri(properties.getAuthUrl() + "/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("grant_type", "refresh_token")
                            .with("client_id", properties.getClientId())
                            .with("client_secret", properties.getClientSecret())
                            .with("refresh_token", refreshToken)
                    )
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            return getLoginResponseFromToken(token);

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Token refresh failed. Please verify that 'fractalhive.keycloak.client-id' and " +
                    "'fractalhive.keycloak.client-secret' are correctly configured. Error: %s"
                            .formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Logout a user by invalidating the refresh token.
     */
    public void logout(String refreshToken) {
        try {
            webClient
                    .post()
                    .uri(properties.getAuthUrl() + "/logout")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters
                            .fromFormData("client_id", properties.getClientId())
                            .with("client_secret", properties.getClientSecret())
                            .with("refresh_token", refreshToken)
                    )
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Logout failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Extract user information from JWT token.
     */
    public UserInfoResponse getUserInfo(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        String role = "UNKNOWN";
        
        if (realmAccess != null) {
            List<String> roles = (List<String>) realmAccess.get("roles");
            if (roles != null && !roles.isEmpty()) {
                role = roles.get(0); // Get first role, can be customized
            }
        }

        return new UserInfoResponse(
                email != null ? email : "",
                name != null ? name : "",
                role
        );
    }

    /**
     * Get admin access token for Keycloak admin API calls.
     */
    public String getAdminAccessToken() {
        try {
            if (adminAccessToken == null || Instant.now().isAfter(adminAccessTokenExpireTime)) {
                String adminClientId = properties.getAdmin().getClientId() != null 
                        ? properties.getAdmin().getClientId() 
                        : properties.getClientId();
                String adminClientSecret = properties.getAdmin().getClientSecret() != null 
                        ? properties.getAdmin().getClientSecret() 
                        : properties.getClientSecret();

                // Use admin realm for authentication (typically "master")
                Map<String, Object> token = webClient
                        .post()
                        .uri(properties.getAdminAuthUrl() + "/token")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .body(BodyInserters
                                .fromFormData("grant_type", "client_credentials")
                                .with("client_id", adminClientId)
                                .with("client_secret", adminClientSecret)
                        )
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
                        .block();

                adminAccessToken = (String) token.get("access_token");
                Integer expiresIn = (Integer) token.get("expires_in");
                adminAccessTokenExpireTime = Instant.now().plusSeconds(expiresIn - 30);

            }
            return adminAccessToken;

        } catch (WebClientResponseException ex) {
            String adminClientId = properties.getAdmin().getClientId() != null 
                    ? properties.getAdmin().getClientId() 
                    : properties.getClientId();
            throw new KeycloakAuthException(
                    "Failed to obtain admin access token using client '%s'. " +
                    "Please verify that 'fractalhive.keycloak.client-id' and 'fractalhive.keycloak.client-secret' " +
                    "(or 'fractalhive.keycloak.admin.client-id' and 'fractalhive.keycloak.admin.client-secret') " +
                    "are correctly configured. Error: %s".formatted(adminClientId, ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    private LoginResponse getLoginResponseFromToken(Map<String, Object> token) {
        return new LoginResponse(
                token.get("access_token").toString(),
                token.get("refresh_token").toString(),
                (Integer) token.get("expires_in"),
                (Integer) token.get("refresh_expires_in")
        );
    }
}
