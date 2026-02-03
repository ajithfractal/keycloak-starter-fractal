package com.fractalhive.keycloak.dto;

import java.util.List;

/**
 * Response DTO for role information.
 */
public record RoleResponse(
        String id,
        String name,
        String description,
        Boolean composite,
        List<String> subRoles
) {
}
