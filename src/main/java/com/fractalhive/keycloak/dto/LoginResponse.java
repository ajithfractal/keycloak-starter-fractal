package com.fractalhive.keycloak.dto;

/**
 * Response DTO for login and token refresh operations.
 */
public record LoginResponse(
        String accessToken,
        String refreshToken,
        Integer accessTokenExpiresIn,
        Integer refreshTokenExpiresIn
) {
}
