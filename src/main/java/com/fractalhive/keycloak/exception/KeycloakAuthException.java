package com.fractalhive.keycloak.exception;

/**
 * Exception for Keycloak-specific authentication and authorization errors.
 */
public class KeycloakAuthException extends RuntimeException {

    public KeycloakAuthException(String message) {
        super(message);
    }

    public KeycloakAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
