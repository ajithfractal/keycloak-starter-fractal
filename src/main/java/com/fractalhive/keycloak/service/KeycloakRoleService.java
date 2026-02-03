package com.fractalhive.keycloak.service;

import com.fractalhive.keycloak.autoconfigure.KeycloakAuthProperties;
import com.fractalhive.keycloak.dto.RoleRequest;
import com.fractalhive.keycloak.dto.RoleResponse;
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
import java.util.stream.Collectors;

/**
 * Service for Keycloak role management operations.
 */
@Service
@RequiredArgsConstructor
public class KeycloakRoleService {

    private final WebClient webClient;
    private final KeycloakAuthProperties properties;
    private final KeycloakAuthService keycloakAuthService;

    /**
     * Create a realm-level role.
     */
    public RoleResponse createRealmRole(RoleRequest roleRequest) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> roleRepresentation = Map.of(
                    "name", roleRequest.name(),
                    "description", roleRequest.description() != null ? roleRequest.description() : ""
            );

            webClient
                    .post()
                    .uri(properties.getAdminUrl() + "/roles")
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return getRole(roleRequest.name());

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Role creation failed. Please verify that 'fractalhive.keycloak.server-url' and " +
                    "'fractalhive.keycloak.realm' are correctly configured. Error: %s"
                            .formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Create a client-level role.
     */
    public RoleResponse createClientRole(String clientId, RoleRequest roleRequest) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> roleRepresentation = Map.of(
                    "name", roleRequest.name(),
                    "description", roleRequest.description() != null ? roleRequest.description() : ""
            );

            webClient
                    .post()
                    .uri(properties.getAdminUrl() + "/clients/{clientId}/roles", clientId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return getClientRole(clientId, roleRequest.name());

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Client role creation failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Get realm role details.
     */
    public RoleResponse getRole(String roleName) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> role = webClient
                    .get()
                    .uri(properties.getAdminUrl() + "/roles/{roleName}", roleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            return mapToRoleResponse(role);

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to get role: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Get client role details.
     */
    public RoleResponse getClientRole(String clientId, String roleName) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> role = webClient
                    .get()
                    .uri(properties.getAdminUrl() + "/clients/{clientId}/roles/{roleName}", clientId, roleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();

            return mapToRoleResponse(role);

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to get client role: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Update a realm role.
     */
    public RoleResponse updateRole(String roleName, RoleRequest roleRequest) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            Map<String, Object> roleRepresentation = Map.of(
                    "name", roleRequest.name(),
                    "description", roleRequest.description() != null ? roleRequest.description() : ""
            );

            webClient
                    .put()
                    .uri(properties.getAdminUrl() + "/roles/{roleName}", roleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return getRole(roleRequest.name());

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Role update failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Delete a realm role.
     */
    public void deleteRole(String roleName) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            webClient
                    .delete()
                    .uri(properties.getAdminUrl() + "/roles/{roleName}", roleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Role deletion failed: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Assign role to user.
     */
    public void assignRoleToUser(String userId, String roleName, boolean isRealmRole) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            RoleResponse role = isRealmRole 
                    ? getRole(roleName) 
                    : getClientRole(properties.getClientId(), roleName);

            List<Map<String, Object>> roleRepresentation = List.of(Map.of(
                    "id", role.id(),
                    "name", role.name()
            ));

            String endpoint = isRealmRole
                    ? properties.getAdminUrl() + "/users/{userId}/role-mappings/realm"
                    : properties.getAdminUrl() + "/users/{userId}/role-mappings/clients/{clientId}";

            if (isRealmRole) {
                webClient
                        .post()
                        .uri(endpoint, userId)
                        .headers(h -> h.setBearerAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(roleRepresentation))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } else {
                webClient
                        .post()
                        .uri(endpoint, userId, properties.getClientId())
                        .headers(h -> h.setBearerAuth(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(BodyInserters.fromValue(roleRepresentation))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            }

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to assign role to user: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Remove role from user.
     */
    public void removeRoleFromUser(String userId, String roleName, boolean isRealmRole) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            RoleResponse role = isRealmRole 
                    ? getRole(roleName) 
                    : getClientRole(properties.getClientId(), roleName);

            List<Map<String, Object>> roleRepresentation = List.of(Map.of(
                    "id", role.id(),
                    "name", role.name()
            ));

            String endpoint = isRealmRole
                    ? properties.getAdminUrl() + "/users/{userId}/role-mappings/realm"
                    : properties.getAdminUrl() + "/users/{userId}/role-mappings/clients/{clientId}";

            if (isRealmRole) {
                webClient
                        .delete()
                        .uri(endpoint, userId)
                        .headers(h -> h.setBearerAuth(adminToken))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            } else {
                webClient
                        .delete()
                        .uri(endpoint, userId, properties.getClientId())
                        .headers(h -> h.setBearerAuth(adminToken))
                        .retrieve()
                        .toBodilessEntity()
                        .block();
            }

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to remove role from user: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Get all roles for a user.
     */
    public List<RoleResponse> getUserRoles(String userId) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            List<Map<String, Object>> roles = webClient
                    .get()
                    .uri(properties.getAdminUrl() + "/users/{userId}/role-mappings/realm", userId)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .block();

            return roles != null
                    ? roles.stream().map(this::mapToRoleResponse).collect(Collectors.toList())
                    : List.of();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to get user roles: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Create a composite role (role with sub-roles).
     */
    public RoleResponse createCompositeRole(String roleName, List<String> subRoleNames) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            // First create the role
            createRealmRole(new RoleRequest(roleName, null));

            // Get sub-roles
            List<Map<String, String>> subRoles = subRoleNames.stream()
                    .map(this::getRole)
                    .map(role -> Map.of("id", role.id(), "name", role.name()))
                    .collect(Collectors.toList());

            // Make it composite
            webClient
                    .post()
                    .uri(properties.getAdminUrl() + "/roles/{roleName}/composites", roleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(subRoles))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

            return getRole(roleName);

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to create composite role: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Add sub-role to composite role.
     */
    public void addSubRole(String compositeRoleName, String subRoleName) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            RoleResponse subRole = getRole(subRoleName);
            List<Map<String, Object>> roleRepresentation = List.of(Map.of(
                    "id", subRole.id(),
                    "name", subRole.name()
            ));

            webClient
                    .post()
                    .uri(properties.getAdminUrl() + "/roles/{roleName}/composites", compositeRoleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(roleRepresentation))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to add sub-role: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    /**
     * Remove sub-role from composite role.
     */
    public void removeSubRole(String compositeRoleName, String subRoleName) {
        try {
            String adminToken = keycloakAuthService.getAdminAccessToken();

            RoleResponse subRole = getRole(subRoleName);
            List<Map<String, Object>> roleRepresentation = List.of(Map.of(
                    "id", subRole.id(),
                    "name", subRole.name()
            ));

            webClient
                    .delete()
                    .uri(properties.getAdminUrl() + "/roles/{roleName}/composites", compositeRoleName)
                    .headers(h -> h.setBearerAuth(adminToken))
                    .retrieve()
                    .toBodilessEntity()
                    .block();

        } catch (WebClientResponseException ex) {
            throw new KeycloakAuthException(
                    "Failed to remove sub-role: %s".formatted(ex.getResponseBodyAsString()),
                    ex
            );
        }
    }

    private RoleResponse mapToRoleResponse(Map<String, Object> role) {
        String id = role.get("id") != null ? role.get("id").toString() : "";
        String name = role.get("name") != null ? role.get("name").toString() : "";
        String description = role.get("description") != null ? role.get("description").toString() : "";
        Boolean composite = role.get("composite") != null ? (Boolean) role.get("composite") : false;
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> composites = (List<Map<String, Object>>) role.get("composites");
        List<String> subRoles = composites != null
                ? composites.stream()
                        .map(c -> c.get("name").toString())
                        .collect(Collectors.toList())
                : List.of();

        return new RoleResponse(id, name, description, composite, subRoles);
    }
}
