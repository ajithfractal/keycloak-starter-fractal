package com.fractalhive.keycloak.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Request DTO for assigning roles to a user.
 */
public record AssignRoleRequest(
        @NotEmpty(message = "At least one role name is required")
        List<@NotBlank(message = "Role name cannot be blank") String> roleNames
) {
}
