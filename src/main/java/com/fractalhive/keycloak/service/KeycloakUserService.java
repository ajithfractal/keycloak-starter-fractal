package com.fractalhive.keycloak.service;

import com.fractalhive.keycloak.autoconfigure.KeycloakAuthProperties;
import com.fractalhive.keycloak.dto.RegisterRequest;
import com.fractalhive.keycloak.dto.RegisterResponse;
import com.fractalhive.keycloak.dto.UserInfoResponse;
import com.fractalhive.keycloak.exception.KeycloakAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Service for Keycloak user management operations.
 */
@Service
@RequiredArgsConstructor
public class KeycloakUserService {

    private final WebClient webClient;
    private final KeycloakAuthProperties properties;
    private final KeycloakAuthService keycloakAuthService;

    /**
     * Register a new user in Keycloak.
     */
    public RegisterResponse register(RegisterRequest registerRequest) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> userRepresentation = Map.of(
                    "username", registerRequest.email(),
                    "email", registerRequest.email(),
                    "firstName", registerRequest.firstName(),
                    "lastName", registerRequest.lastName(),
                    "enabled", true,
                    "emailVerified", false,
                    "credentials", List.of(Map.of(
                            "type", "password",
                            "value", registerRequest.password(),
                            "temporary", false
                    ))
            );

            String location = webClient
                    .post()
                    .uri(properties.getAdminUrl() + "/users")
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(userRepresentation))
                    .retrieve()
                    .toEntity(String.class)
                    .block()
                    .getHeaders()
                    .getLocation()
                    .toString();

            String userId = location.substring(location.lastIndexOf('/') + 1);

            return new RegisterResponse(
                    userId,
                    registerRequest.email(),
                    "User registered successfully"
            );

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "User registration failed. Please verify that 'fractalhive.keycloak.server-url' and " +
                    "'fractalhive.keycloak.realm' are correctly configured. Error: %s"
                            .formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Update user details.
     */
    public void updateUser(String userId, Map<String, Object> userUpdates) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            webClient
                    .put()
                    .uri(properties.getAdminUrl() + "/users/{id}", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(userUpdates))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "User update failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Get user by ID.
     */
    public UserInfoResponse getUserById(String userId) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> user = webClient
                    .get()
                    .uri(properties.getAdminUrl() + "/users/{id}", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            String email = user.get("email") != null
                    ? user.get("email").toString()
                    : null;

            String firstName = user.get("firstName") != null
                    ? user.get("firstName").toString()
                    : "";

            String lastName = user.get("lastName") != null
                    ? user.get("lastName").toString()
                    : "";

            String fullName = (firstName + " " + lastName).trim();

            return new UserInfoResponse(
                    email,
                    fullName,
                    "UNKNOWN" // roles intentionally skipped
            );

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to get user: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Get user by email.
     */
    public UserInfoResponse getUserByEmail(String email) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            List<Map<String, Object>> users = webClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(properties.getAdminUrl() + "/users")
                            .queryParam("email", email)
                            .queryParam("exact", true)
                            .build())
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .block();

            if (users == null || users.isEmpty()) {
                throw new KeycloakAuthException("User not found with email: " + email);
            }

            Map<String, Object> user = users.get(0);
            String firstName = user.get("firstName") != null
                    ? user.get("firstName").toString()
                    : "";
            String lastName = user.get("lastName") != null
                    ? user.get("lastName").toString()
                    : "";
            String fullName = (firstName + " " + lastName).trim();

            return new UserInfoResponse(
                    email,
                    fullName,
                    "UNKNOWN"
            );

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to get user by email: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Delete a user.
     */
    public void deleteUser(String userId) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            webClient
                    .delete()
                    .uri(properties.getAdminUrl() + "/users/{id}", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "User deletion failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }
}
