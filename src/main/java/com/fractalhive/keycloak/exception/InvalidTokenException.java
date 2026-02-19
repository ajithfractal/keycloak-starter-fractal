package com.fractalhive.keycloak.exception;

/**
 * Exception thrown when JWT token is invalid (malformed, wrong signature, etc.).
 */
public class InvalidTokenException extends KeycloakAuthException {

    public InvalidTokenException() {
        super("Invalid token");
    }

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
