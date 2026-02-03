package com.fractalhive.keycloak.dto;

/**
 * Response DTO for user information.
 */
public record UserInfoResponse(
        String email,
        String name,
        String role
) {
}
