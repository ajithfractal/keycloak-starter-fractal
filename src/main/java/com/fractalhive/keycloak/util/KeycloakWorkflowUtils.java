package com.fractalhive.keycloak.util;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Collections;
import java.util.List;

/**
 * Utility class for extracting Keycloak workflow attributes from HTTP requests.
 * <p>
 * These utilities are used by workflow engine filters to access
 * roles and schema information set by KeycloakWorkflowFilter.
 * </p>
 */
public final class KeycloakWorkflowUtils {

    private static final String ROLES_ATTRIBUTE = "roles";
    private static final String SCHEMA_NAME_ATTRIBUTE = "schemaName";

    private KeycloakWorkflowUtils() {
        // Utility class - prevent instantiation
    }

    /**
     * Extracts roles from HttpServletRequest.
     * 
     * @param request HTTP servlet request
     * @return List of roles, or empty list if not found
     */
    @SuppressWarnings("unchecked")
    public static List<String> getRoles(HttpServletRequest request) {
        Object rolesObj = request.getAttribute(ROLES_ATTRIBUTE);
        
        if (rolesObj == null) {
            return Collections.emptyList();
        }
        
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        
        if (rolesObj instanceof String) {
            return List.of((String) rolesObj);
        }
        
        return Collections.emptyList();
    }

    /**
     * Extracts schema name from HttpServletRequest.
     * 
     * @param request HTTP servlet request
     * @return Schema name, or null if not found
     */
    public static String getSchema(HttpServletRequest request) {
        Object schemaObj = request.getAttribute(SCHEMA_NAME_ATTRIBUTE);
        
        if (schemaObj == null) {
            return null;
        }
        
        if (schemaObj instanceof String) {
            String schema = (String) schemaObj;
            return schema.trim().isEmpty() ? null : schema.trim();
        }
        
        return schemaObj.toString();
    }

    /**
     * Checks if user has a specific role.
     * 
     * @param request HTTP servlet request
     * @param role Role name to check (e.g., "WORKFLOW_ADMIN")
     * @return true if user has the role, false otherwise
     */
    public static boolean hasRole(HttpServletRequest request, String role) {
        if (role == null || role.trim().isEmpty()) {
            return false;
        }
        
        List<String> roles = getRoles(request);
        return roles.contains(role.trim());
    }

    /**
     * Checks if user is a workflow admin.
     * 
     * @param request HTTP servlet request
     * @return true if user has WORKFLOW_ADMIN role, false otherwise
     */
    public static boolean isWorkflowAdmin(HttpServletRequest request) {
        return hasRole(request, "WORKFLOW_ADMIN");
    }

    /**
     * Checks if user is a tenant user (has schema claim).
     * 
     * @param request HTTP servlet request
     * @return true if user has schema attribute, false otherwise
     */
    public static boolean isTenantUser(HttpServletRequest request) {
        return getSchema(request) != null;
    }
}
