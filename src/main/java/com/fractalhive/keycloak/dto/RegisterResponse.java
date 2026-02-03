package com.fractalhive.keycloak.dto;

/**
 * Response DTO for user registration.
 */
public record RegisterResponse(
        String userId,
        String email,
        String message
) {
}
