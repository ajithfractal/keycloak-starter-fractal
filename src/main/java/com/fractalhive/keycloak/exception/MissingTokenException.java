package com.fractalhive.keycloak.exception;

/**
 * Exception thrown when JWT token is missing from the request.
 */
public class MissingTokenException extends KeycloakAuthException {

    public MissingTokenException() {
        super("Missing authorization token");
    }

    public MissingTokenException(String message) {
        super(message);
    }
}
