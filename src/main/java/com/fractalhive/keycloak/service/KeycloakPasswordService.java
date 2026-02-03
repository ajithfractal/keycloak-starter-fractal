package com.fractalhive.keycloak.service;

import com.fractalhive.keycloak.autoconfigure.KeycloakAuthProperties;
import com.fractalhive.keycloak.dto.PasswordResetRequest;
import com.fractalhive.keycloak.exception.KeycloakAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

/**
 * Service for Keycloak password management operations.
 */
@Service
@RequiredArgsConstructor
public class KeycloakPasswordService {

    private final WebClient webClient;
    private final KeycloakAuthProperties properties;
    private final KeycloakAuthService keycloakAuthService;
    private final KeycloakUserService userService;

    /**
     * Request password reset (sends email with reset link).
     */
    public void requestPasswordReset(String email) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            // Get user ID by email and send password reset email
            webClient
                    .put()
                    .uri(properties.getAdminUrl() + "/users/{userId}/execute-actions-email", getUserIdByEmail(email))
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(new String[]{"UPDATE_PASSWORD"}))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Password reset request failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Reset password with token (from email link).
     */
    public void resetPassword(PasswordResetRequest passwordResetRequest) {
        try {
            // Keycloak handles password reset via the token in the email link
            // This would typically be handled by the frontend redirecting to Keycloak
            // For API-based reset, we need to use the admin API
            String adminToken = keycloakAuthService.getAdminAccessToken();
            String userId = getUserIdByEmail(passwordResetRequest.email());

            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", passwordResetRequest.newPassword(),
                    "temporary", false
            );

            webClient
                    .put()
                    .uri(properties.getAdminUrl() + "/users/{userId}/reset-password", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(credential))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Password reset failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Change password for authenticated user.
     */
    public void changePassword(String userId, String newPassword) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> credential = Map.of(
                    "type", "password",
                    "value", newPassword,
                    "temporary", false
            );

            webClient
                    .put()
                    .uri(properties.getAdminUrl() + "/users/{userId}/reset-password", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(credential))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Password change failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Send verification email to user.
     */
    public void sendVerificationEmail(String userId) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            webClient
                    .put()
                    .uri(properties.getAdminUrl() + "/users/{userId}/send-verify-email", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to send verification email: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    private String getUserIdByEmail(String email) {
        // Helper method to get user ID by email
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();
            var users = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.getAdminUrl() + "/users")
                            .queryParam("email", email)
                            .queryParam("exact", true)
                            .build())
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(java.util.List.class)
                    .block();

            if (users != null && !users.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> user = (Map<String, Object>) users.get(0);
                return user.get("id").toString();
            }
            throw new KeycloakAuthException("User not found with email: " + email);
        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to get user ID: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }
}
