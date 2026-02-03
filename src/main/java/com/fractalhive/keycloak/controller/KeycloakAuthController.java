package com.fractalhive.keycloak.controller;

import com.fractalhive.keycloak.autoconfigure.KeycloakAuthProperties;
import com.fractalhive.keycloak.config.JwtCookieAuthenticationFilter;
import com.fractalhive.keycloak.dto.*;
import com.fractalhive.keycloak.exception.KeycloakAuthException;
import com.fractalhive.keycloak.service.KeycloakAuthService;
import com.fractalhive.keycloak.service.KeycloakPasswordService;
import com.fractalhive.keycloak.service.KeycloakRoleService;
import com.fractalhive.keycloak.service.KeycloakUserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Keycloak authentication and authorization endpoints.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Keycloak Authentication", description = "API endpoints for Keycloak authentication, user management, password management, and role management")
public class KeycloakAuthController {

    private final KeycloakAuthService authService;
    private final KeycloakUserService userService;
    private final KeycloakRoleService roleService;
    private final KeycloakPasswordService passwordService;
    private final KeycloakAuthProperties properties;

    // ========== Authentication Endpoints ==========

    @Operation(
            summary = "User login",
            description = "Authenticates a user with username and password. Returns JWT tokens in HTTP-only cookies."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(loginRequest);
        setAuthCookies(response, loginResponse);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Refresh access token",
            description = "Refreshes the access token using the refresh token from cookies."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getRefreshToken(request);
        if (refreshToken == null) {
            throw new KeycloakAuthException("Refresh token is not provided");
        }

        LoginResponse loginResponse = authService.refresh(refreshToken);
        setAuthCookies(response, loginResponse);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "User logout",
            description = "Logs out the current user and invalidates the refresh token. Clears authentication cookies."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = getRefreshToken(request);
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get current user info",
            description = "Returns information about the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User information retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> me(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(authService.getUserInfo(jwt));
    }

    // ========== User Management Endpoints ==========

    @Operation(
            summary = "Register new user",
            description = "Registers a new user in Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "409", description = "User already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse response = userService.register(registerRequest);
        return ResponseEntity.ok(response);
    }

    // ========== Password Management Endpoints ==========

    @Operation(
            summary = "Request password reset",
            description = "Sends a password reset email to the user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset email sent"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(
            @Parameter(description = "User email address", required = true)
            @RequestParam String email
    ) {
        passwordService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using a reset token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password reset successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid reset token")
    })
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Change password",
            description = "Changes the password for the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @Parameter(description = "New password", required = true)
            @RequestParam String newPassword,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {
        String userId = jwt.getSubject();
        passwordService.changePassword(userId, newPassword);
        return ResponseEntity.ok().build();
    }

    // ========== Role Management Endpoints ==========

    @Operation(
            summary = "Get all roles",
            description = "Retrieves all realm roles from Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles retrieved successfully")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @GetMapping("/roles")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        // This would need to be implemented in KeycloakRoleService
        // For now, returning empty list
        return ResponseEntity.ok(List.of());
    }

    @Operation(
            summary = "Create role",
            description = "Creates a new realm role in Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role created successfully"),
            @ApiResponse(responseCode = "409", description = "Role already exists")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest roleRequest) {
        RoleResponse response = roleService.createRealmRole(roleRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get role by name",
            description = "Retrieves a specific realm role by name."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @GetMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponse> getRole(
            @Parameter(description = "Role name", required = true)
            @PathVariable String roleName
    ) {
        RoleResponse response = roleService.getRole(roleName);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Update role",
            description = "Updates an existing realm role in Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role updated successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @PutMapping("/roles/{roleName}")
    public ResponseEntity<RoleResponse> updateRole(
            @Parameter(description = "Role name", required = true)
            @PathVariable String roleName,
            @Valid @RequestBody RoleRequest roleRequest
    ) {
        RoleResponse response = roleService.updateRole(roleName, roleRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Delete role",
            description = "Deletes a realm role from Keycloak."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @DeleteMapping("/roles/{roleName}")
    public ResponseEntity<Void> deleteRole(
            @Parameter(description = "Role name", required = true)
            @PathVariable String roleName
    ) {
        roleService.deleteRole(roleName);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Create composite role",
            description = "Creates a composite role with sub-roles."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Composite role created successfully"),
            @ApiResponse(responseCode = "404", description = "Role or sub-role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @PostMapping("/roles/{roleName}/composite")
    public ResponseEntity<RoleResponse> createCompositeRole(
            @Parameter(description = "Role name", required = true)
            @PathVariable String roleName,
            @Parameter(description = "List of sub-role names", required = true)
            @RequestBody List<String> subRoleNames
    ) {
        RoleResponse response = roleService.createCompositeRole(roleName, subRoleNames);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Add sub-role to composite role",
            description = "Adds a sub-role to an existing composite role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-role added successfully"),
            @ApiResponse(responseCode = "404", description = "Role or sub-role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @PostMapping("/roles/{roleName}/sub-roles")
    public ResponseEntity<Void> addSubRole(
            @Parameter(description = "Composite role name", required = true)
            @PathVariable String roleName,
            @Parameter(description = "Sub-role name to add", required = true)
            @RequestParam String subRoleName
    ) {
        roleService.addSubRole(roleName, subRoleName);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Remove sub-role from composite role",
            description = "Removes a sub-role from a composite role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sub-role removed successfully"),
            @ApiResponse(responseCode = "404", description = "Role or sub-role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @DeleteMapping("/roles/{roleName}/sub-roles/{subRoleName}")
    public ResponseEntity<Void> removeSubRole(
            @Parameter(description = "Composite role name", required = true)
            @PathVariable String roleName,
            @Parameter(description = "Sub-role name to remove", required = true)
            @PathVariable String subRoleName
    ) {
        roleService.removeSubRole(roleName, subRoleName);
        return ResponseEntity.ok().build();
    }

    // ========== User Role Assignment Endpoints ==========

    @Operation(
            summary = "Assign roles to user",
            description = "Assigns one or more roles to a user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Roles assigned successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @Valid @RequestBody AssignRoleRequest request,
            @Parameter(description = "Whether the role is a realm role (default: true)")
            @RequestParam(defaultValue = "true") boolean isRealmRole
    ) {
        for (String roleName : request.roleNames()) {
            roleService.assignRoleToUser(userId, roleName, isRealmRole);
        }
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Remove role from user",
            description = "Removes a role from a user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @DeleteMapping("/users/{userId}/roles/{roleName}")
    public ResponseEntity<Void> removeRoleFromUser(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId,
            @Parameter(description = "Role name", required = true)
            @PathVariable String roleName,
            @Parameter(description = "Whether the role is a realm role (default: true)")
            @RequestParam(defaultValue = "true") boolean isRealmRole
    ) {
        roleService.removeRoleFromUser(userId, roleName, isRealmRole);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Get user roles",
            description = "Retrieves all roles assigned to a user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User roles retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @SecurityRequirement(name = "Bearer Authentication")
    @SecurityRequirement(name = "Cookie Authentication")
    @GetMapping("/users/{userId}/roles")
    public ResponseEntity<List<RoleResponse>> getUserRoles(
            @Parameter(description = "User ID", required = true)
            @PathVariable String userId
    ) {
        List<RoleResponse> roles = roleService.getUserRoles(userId);
        return ResponseEntity.ok(roles);
    }

    // ========== Helper Methods ==========

    private void setAuthCookies(HttpServletResponse response, LoginResponse loginResponse) {
        boolean secure = properties.getCookie().isSecure();
        String sameSite = properties.getCookie().getSameSite();
        String domain = properties.getCookie().getDomain();

        String accessCookie = JwtCookieAuthenticationFilter.ACCESS_TOKEN_COOKIE + "=" + loginResponse.accessToken()
                + "; Path=/"
                + "; HttpOnly"
                + (secure ? "; Secure" : "")
                + "; SameSite=" + sameSite
                + "; Max-Age=" + loginResponse.accessTokenExpiresIn()
                + (domain != null ? "; Domain=" + domain : "");

        String refreshCookie = JwtCookieAuthenticationFilter.REFRESH_TOKEN_COOKIE + "=" + loginResponse.refreshToken()
                + "; Path=/"
                + "; HttpOnly"
                + (secure ? "; Secure" : "")
                + "; SameSite=" + sameSite
                + "; Max-Age=" + loginResponse.refreshTokenExpiresIn()
                + (domain != null ? "; Domain=" + domain : "");

        response.addHeader("Set-Cookie", accessCookie);
        response.addHeader("Set-Cookie", refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        boolean secure = properties.getCookie().isSecure();
        String sameSite = properties.getCookie().getSameSite();
        String domain = properties.getCookie().getDomain();

        String accessCookie = JwtCookieAuthenticationFilter.ACCESS_TOKEN_COOKIE + "=; Path=/; Max-Age=0; HttpOnly"
                + (secure ? "; Secure" : "")
                + "; SameSite=" + sameSite
                + (domain != null ? "; Domain=" + domain : "");

        String refreshCookie = JwtCookieAuthenticationFilter.REFRESH_TOKEN_COOKIE + "=; Path=/; Max-Age=0; HttpOnly"
                + (secure ? "; Secure" : "")
                + "; SameSite=" + sameSite
                + (domain != null ? "; Domain=" + domain : "");

        response.addHeader("Set-Cookie", accessCookie);
        response.addHeader("Set-Cookie", refreshCookie);
    }

    private String getRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(JwtCookieAuthenticationFilter.REFRESH_TOKEN_COOKIE)) {
                return cookie.getValue();
            }
        }

        return null;
    }
}
