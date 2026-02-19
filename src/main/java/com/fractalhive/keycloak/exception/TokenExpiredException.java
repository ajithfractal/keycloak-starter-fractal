package com.fractalhive.keycloak.exception;

/**
 * Exception thrown when JWT token has expired.
 */
public class TokenExpiredException extends KeycloakAuthException {

    public TokenExpiredException() {
        super("Token expired");
    }

    public TokenExpiredException(String message) {
        super(message);
    }

    public TokenExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
