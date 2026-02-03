package com.fractalhive.keycloak.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating or updating a role.
 */
public record RoleRequest(
        @NotBlank(message = "Role name is required")
        String name,

        String description
) {
}
